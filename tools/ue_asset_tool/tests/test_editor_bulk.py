import struct
import tempfile
import unittest
from pathlib import Path

from ueassettool.binary import BinaryReader
from ueassettool.editor_bulk import (
    STORED_IN_PACKAGE_TRAILER,
    match_trailer_entry,
    parse_editor_bulk_data,
    parse_mesh_description_bulk_data,
)
from ueassettool.errors import FormatError, UnsupportedError
from ueassettool.trailer import PackageTrailer, TrailerEntry


class EditorBulkDataTests(unittest.TestCase):
    def test_exact_mesh_bulk_and_trailer_identity(self) -> None:
        bulk_id = bytes(range(1, 17))
        payload_id = bytes(range(20, 40))
        metadata = struct.pack("<I", STORED_IN_PACKAGE_TRAILER)
        metadata += bulk_id + payload_id + struct.pack("<q", 123)
        metadata += payload_id[:16] + struct.pack("<i", 1)
        trailer_entry = TrailerEntry(payload_id.hex(), 0, 50, 123, 0, 0, 0, 100)
        trailer = PackageTrailer(0, 2, 77, 50, (trailer_entry,), 127, 147)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mesh-bulk.bin"
            path.write_bytes(metadata)
            with BinaryReader(path) as reader:
                parsed = parse_mesh_description_bulk_data(reader, end=len(metadata))
        self.assertEqual(parsed.serialized_size, 68)
        self.assertTrue(parsed.guid_is_hash)
        self.assertEqual(parsed.editor_bulk.payload_content_id, payload_id.hex())
        self.assertEqual(match_trailer_entry(parsed.editor_bulk, trailer), trailer_entry)

    def test_rejects_unknown_flags_and_size_mismatch(self) -> None:
        raw = struct.pack("<I", 1 << 31) + bytes(16 + 20) + struct.pack("<q", 0)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad-bulk.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                with self.assertRaisesRegex(UnsupportedError, "unknown flags"):
                    parse_editor_bulk_data(reader)

        payload_id = bytes(range(20)).hex()
        valid = struct.pack("<I", STORED_IN_PACKAGE_TRAILER) + bytes(range(1, 17))
        valid += bytes.fromhex(payload_id) + struct.pack("<q", 5)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "size-bulk.bin"
            path.write_bytes(valid)
            with BinaryReader(path) as reader:
                parsed = parse_editor_bulk_data(reader)
        trailer = PackageTrailer(
            0, 2, 77, 1,
            (TrailerEntry(payload_id, 0, 1, 4, 0, 0, 0, 77),),
            78, 98,
        )
        with self.assertRaisesRegex(FormatError, "trailer raw size"):
            match_trailer_entry(parsed, trailer)


if __name__ == "__main__":
    unittest.main()
