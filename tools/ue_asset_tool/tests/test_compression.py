import hashlib
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

from ueassettool.compression import (
    COMPRESSED_BUFFER_MAGIC,
    decompress_compressed_buffer,
    decompress_legacy_chunked_zlib,
)
from ueassettool.errors import FormatError
from ueassettool.hashes import blake3_digest


class CompressionTests(unittest.TestCase):
    def test_uncompressed_zero_exponent_compressed_buffer(self) -> None:
        raw = b"official-layout-uncompressed-payload" * 3
        header = struct.pack(">II", COMPRESSED_BUFFER_MAGIC, 0)
        header += bytes((0, 0, 0, 0))
        header += struct.pack(">IQQ", 1, len(raw), 64 + len(raw))
        header += blake3_digest(raw)
        self.assertEqual(len(header), 64)
        crc = zlib.crc32(header[8:]) & 0xFFFFFFFF
        header = header[:4] + struct.pack(">I", crc) + header[8:]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "buffer.bin"
            path.write_bytes(header + raw)
            parsed, decoded = decompress_compressed_buffer(path, 0)
        self.assertEqual(decoded, raw)
        self.assertEqual(parsed.block_size_exponent, 0)
        self.assertEqual(parsed.block_table_size, 0)
        self.assertEqual(parsed.raw_hash, blake3_digest(raw).hex())

        corrupted = bytearray(header + raw)
        corrupted[40] ^= 1
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad-crc.bin"
            path.write_bytes(corrupted)
            with self.assertRaisesRegex(FormatError, "header CRC32"):
                decompress_compressed_buffer(path, 0)

    def test_legacy_serialize_compressed_zlib_chunks(self) -> None:
        chunks = [b"A" * 31, bytes(range(31)), b"last-chunk"]
        compressed = [zlib.compress(item) for item in chunks]
        raw = b"".join(chunks)
        table = b"".join(
            struct.pack("<QQ", len(encoded), len(decoded))
            for encoded, decoded in zip(compressed, chunks)
        )
        header = struct.pack(
            "<IIQQQ", 0x9E2A83C1, 0, 31,
            sum(map(len, compressed)), len(raw),
        )
        stored = header + table + b"".join(compressed)
        prefix = b"prefix"
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "legacy.bin"
            path.write_bytes(prefix + stored)
            manifest, decoded = decompress_legacy_chunked_zlib(
                path, len(prefix), len(stored), len(raw),
            )
        self.assertEqual(decoded, raw)
        self.assertEqual(manifest["chunk_count"], 3)
        self.assertEqual(manifest["raw_sha256"], hashlib.sha256(raw).hexdigest())


if __name__ == "__main__":
    unittest.main()
