import struct
import unittest
import zlib

from ueassettool.errors import FormatError
from ueassettool.media import (
    _decode_uedelta_bgra8, _decode_uedelta_texture_source, _uedelta_view_rectangles,
    _encode_source_hdr, _encode_source_npy, blake3_digest,
    _ogg_crc32, validate_hdr, validate_jpeg, validate_npy, validate_ogg,
    validate_png, validate_wav,
)


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


class MediaTests(unittest.TestCase):
    def test_multi_mip_uedelta_uses_exact_split_rectangles_and_texture_source_guid(self) -> None:
        width, height = 140, 500
        raw = bytearray()
        for mip_width, mip_height in ((140, 500), (70, 250)):
            for y in range(mip_height):
                for x in range(mip_width):
                    raw.extend(((x + y) & 255, (y * 3) & 255, (x * 5) & 255, 255))
        delta = bytearray(raw)
        offset = 0
        for mip_width, mip_height in ((140, 500), (70, 250)):
            stride = mip_width * 4
            for start_x, rect_width, start_y, rect_height in _uedelta_view_rectangles(mip_width, mip_height, 4):
                byte_x, byte_width = start_x * 4, rect_width * 4
                for local_y in range(rect_height - 1, 0, -1):
                    row = offset + (start_y + local_y) * stride + byte_x
                    previous = row - stride
                    for x in range(byte_width):
                        delta[row + x] = (raw[row + x] - raw[previous + x]) & 255
            offset += mip_width * mip_height * 4
        digest = blake3_digest(bytes(raw))[:16]
        source_id = "-".join(
            f"{int.from_bytes(digest[index:index + 4], 'little'):08x}"
            for index in range(0, 16, 4)
        )
        decoded, contract = _decode_uedelta_texture_source(
            bytes(delta), width=width, height=height, source_format="TSF_BGRA8",
            num_mips=2, num_slices=1, source_id=source_id,
        )
        self.assertEqual(decoded, bytes(raw))
        self.assertTrue(contract["texture_source_id_match"])
        self.assertGreater(len(contract["mips"][0]["split_rectangles"]), 1)

    def test_ogg_opus_pages_require_crc_sequence_and_eos(self) -> None:
        packet = b"OpusHead" + bytes((1, 2)) + struct.pack("<HIhB", 312, 48000, 0, 0)
        page = bytearray(b"OggS" + bytes((0, 0x06)))
        page += struct.pack("<QII", 0, 0x12345678, 0)
        page += b"\0\0\0\0" + bytes((1, len(packet))) + packet
        page[22:26] = struct.pack("<I", _ogg_crc32(page))
        result = validate_ogg(bytes(page))
        self.assertEqual((result["codec"], result["channels"]), ("Opus", 2))
        page[-1] ^= 1
        with self.assertRaisesRegex(FormatError, "page CRC32"):
            validate_ogg(bytes(page))

    def test_uedelta_inverse_resets_every_32_rows_and_checks_extrema(self) -> None:
        width, height = 2, 33
        raw = bytearray()
        for y in range(height):
            for x in range(width):
                raw.extend(((10 + y + x) & 255, (20 + y) & 255, (30 + x) & 255, 255))
        delta = bytearray(raw)
        stride = width * 4
        for y in range(height):
            if y % 32 == 0:
                continue
            row, previous = y * stride, (y - 1) * stride
            for x in range(stride):
                delta[row + x] = (raw[row + x] - raw[previous + x]) & 255
        color_info = {
            "items": [{
                "properties": [
                    {
                        "name": "ColorMin", "decode_status": "decoded",
                        "value": {"r": 30 / 255, "g": 20 / 255, "b": 10 / 255, "a": 1.0},
                    },
                    {
                        "name": "ColorMax", "decode_status": "decoded",
                        "value": {"r": 31 / 255, "g": 52 / 255, "b": 43 / 255, "a": 1.0},
                    },
                ]
            }]
        }
        decoded, validation = _decode_uedelta_bgra8(
            bytes(delta), width=width, height=height, source_format="TSF_BGRA8",
            num_mips=1, num_slices=1, layer_color_info=color_info,
        )
        self.assertEqual(decoded, bytes(raw))
        self.assertTrue(validation["layer_color_info_match"])
        color_info["items"][0]["properties"][0]["value"]["b"] = 0.0
        with self.assertRaisesRegex(FormatError, "does not match LayerColorInfo"):
            _decode_uedelta_bgra8(
                bytes(delta), width=width, height=height, source_format="TSF_BGRA8",
                num_mips=1, num_slices=1, layer_color_info=color_info,
            )

    def test_jpeg_sof_dimensions(self) -> None:
        sof = bytes((8,)) + struct.pack(">HHB", 1, 2, 3)
        sof += bytes((1, 0x11, 0, 2, 0x11, 1, 3, 0x11, 1))
        sos = bytes((3, 1, 0, 2, 0x11, 3, 0x11, 0, 63, 0))
        raw = b"\xff\xd8\xff\xc0" + struct.pack(">H", len(sof) + 2) + sof
        raw += b"\xff\xda" + struct.pack(">H", len(sos) + 2) + sos + b"\x01\x02\xff\xd9"
        result = validate_jpeg(raw)
        self.assertEqual((result["width"], result["height"], result["components"]), (2, 1, 3))

    def test_bgre8_uedelta_selects_unique_source_verified_reset(self) -> None:
        width, height = 8, 64
        raw = bytearray()
        for y in range(height):
            for x in range(width):
                raw.extend((205 + (x % 2), 203 + (y % 3), 207 + (y % 4), 127))
        delta = bytearray(raw)
        stride = width * 4
        for y in range(1, height):
            row, previous = y * stride, (y - 1) * stride
            for x in range(stride):
                delta[row + x] = (raw[row + x] - raw[previous + x]) & 255
        scale = 2.0 ** (127 - 136)
        color_info = {
            "items": [{"properties": [
                {"name": "ColorMin", "decode_status": "decoded", "value": {
                    "r": (207.5) * scale, "g": (203.5) * scale,
                    "b": (205.5) * scale, "a": 1.0,
                }},
                {"name": "ColorMax", "decode_status": "decoded", "value": {
                    "r": (210.5) * scale, "g": (205.5) * scale,
                    "b": (206.5) * scale, "a": 1.0,
                }},
            ]}]
        }
        decoded, validation = _decode_uedelta_bgra8(
            bytes(delta), width=width, height=height, source_format="TSF_BGRE8",
            num_mips=1, num_slices=1, layer_color_info=color_info,
        )
        self.assertEqual(decoded, bytes(raw))
        self.assertEqual(validation["predictor_reset_rows"], 64)
        hdr = _encode_source_hdr(decoded, width, height, "TSF_BGRE8")
        self.assertEqual(validate_hdr(hdr)["channels"], "RGBE")

    def test_volume_npy_layout_and_length(self) -> None:
        raw = bytes((3, 2, 1, 4, 7, 6, 5, 8))
        encoded, layout = _encode_source_npy(raw, 2, 1, 1, "TSF_BGRA8")
        validation = validate_npy(encoded)
        self.assertEqual(validation["shape"], [1, 1, 2, 4])
        self.assertEqual(layout["channel_order"], "RGBA")
        self.assertTrue(encoded.endswith(bytes((1, 2, 3, 4, 5, 6, 7, 8))))

    def test_png_crc_and_dimensions(self) -> None:
        data = b"\x89PNG\r\n\x1a\n"
        data += png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 0, 0, 0, 0))
        data += png_chunk(b"IDAT", zlib.compress(b"\0\x7f"))
        data += png_chunk(b"IEND", b"")
        result = validate_png(data)
        self.assertEqual((result["width"], result["height"]), (1, 1))
        corrupted = bytearray(data)
        corrupted[-5] ^= 1
        with self.assertRaisesRegex(FormatError, "CRC mismatch"):
            validate_png(bytes(corrupted))

    def test_pcm_wav_bounds(self) -> None:
        fmt = struct.pack("<HHIIHH", 1, 1, 44100, 88200, 2, 16)
        body = b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", 2) + b"\0\0"
        wav = b"RIFF" + struct.pack("<I", len(body) + 4) + b"WAVE" + body
        result = validate_wav(wav)
        self.assertEqual(result["sample_rate"], 44100)
        bad_rate = bytearray(wav)
        struct.pack_into("<I", bad_rate, 28, 1)
        with self.assertRaisesRegex(FormatError, "byte rate"):
            validate_wav(bytes(bad_rate))
        with self.assertRaisesRegex(FormatError, "RIFF length"):
            validate_wav(wav + b"x")


if __name__ == "__main__":
    unittest.main()
