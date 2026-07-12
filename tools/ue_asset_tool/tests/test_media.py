import struct
import unittest
import zlib

from ueassettool.errors import FormatError
from ueassettool.media import _decode_uedelta_bgra8, validate_jpeg, validate_png, validate_wav


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


class MediaTests(unittest.TestCase):
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
        with self.assertRaisesRegex(FormatError, "RIFF length"):
            validate_wav(wav + b"x")


if __name__ == "__main__":
    unittest.main()
