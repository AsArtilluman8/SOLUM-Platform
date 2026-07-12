import struct
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest

from ueassettool.binary import BinaryReader
from ueassettool.properties import PropertyParser, TypeNode


class _Package:
    def __init__(self):
        self.summary = SimpleNamespace(file_version_ue4=522, file_version_ue5=1013)

    @staticmethod
    def custom_version(_guid: str, default: int = -1) -> int:
        return default


class ContainerPropertyTests(unittest.TestCase):
    def test_map_replacement_entries_consume_exactly(self) -> None:
        raw = struct.pack("<iiifif", -1, 2, 7, 1.5, 9, -2.0)
        node = TypeNode("MapProperty", [TypeNode("IntProperty"), TypeNode("FloatProperty")])
        with TemporaryDirectory() as directory:
            path = Path(directory) / "map.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package())._decode_value(reader, node, len(raw), {})
                self.assertEqual(reader.position, len(raw))
        self.assertTrue(value["replace"])
        self.assertEqual(value["count"], 2)
        self.assertEqual(value["entries"][0], {"key": 7, "value": 1.5})
        self.assertEqual(value["entries"][1], {"key": 9, "value": -2.0})


if __name__ == "__main__":
    unittest.main()
