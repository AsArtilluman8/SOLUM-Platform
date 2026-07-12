import struct
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest

from ueassettool.binary import BinaryReader
from ueassettool.model import FNameRef
from ueassettool.properties import PropertyParser, TypeNode


class _Package:
    names = ("None", "InputPin", "Keys", "StructProperty", "RichCurveKey")

    def __init__(self, *, ue5: int = 1013):
        self.summary = SimpleNamespace(file_version_ue4=522, file_version_ue5=ue5)

    def fname(self, raw: tuple[int, int]) -> FNameRef:
        index, number = raw
        return FNameRef(index, number, self.names[index])

    @staticmethod
    def object_path(index: int) -> str:
        return "None" if index == 0 else f"Object[{index}]"

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

    def test_map_bool_value_is_one_serialized_byte(self) -> None:
        raw = struct.pack("<iii", -1, 1, 2) + b"A\0" + b"\1"
        node = TypeNode("MapProperty", [TypeNode("StrProperty"), TypeNode("BoolProperty")])
        with TemporaryDirectory() as directory:
            path = Path(directory) / "map_bool.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package())._decode_value(reader, node, len(raw), {})
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value["entries"], [{"key": "A", "value": True}])

    def test_exact_expression_and_scalar_material_inputs(self) -> None:
        expression = struct.pack("<ii", 7, 2) + struct.pack("<ii", 1, 0)
        expression += struct.pack("<iiiii", 1, 1, 0, 1, 0)
        scalar = expression + struct.pack("<if", 1, 0.75)
        with TemporaryDirectory() as directory:
            expression_path = Path(directory) / "expression.bin"
            expression_path.write_bytes(expression)
            with BinaryReader(expression_path) as reader:
                value = PropertyParser(_Package())._decode_struct(
                    reader, TypeNode("StructProperty", [TypeNode("ExpressionInput")]), len(expression)
                )
                self.assertEqual(reader.position, 36)
            scalar_path = Path(directory) / "scalar.bin"
            scalar_path.write_bytes(scalar)
            with BinaryReader(scalar_path) as reader:
                material = PropertyParser(_Package())._decode_struct(
                    reader, TypeNode("StructProperty", [TypeNode("ScalarMaterialInput")]), len(scalar)
                )
                self.assertEqual(reader.position, 44)
        self.assertEqual(value["expression"]["package_index"], 7)
        self.assertEqual(value["input_name"], "InputPin")
        self.assertEqual(value["mask"], {"enabled": 1, "r": 1, "g": 0, "b": 1, "a": 0})
        self.assertTrue(material["use_constant"])
        self.assertEqual(material["constant"], 0.75)

    def test_legacy_struct_array_inner_tag_and_rich_keys(self) -> None:
        key_a = struct.pack("<BBBffffff", 2, 0, 0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0)
        key_b = struct.pack("<BBBffffff", 0, 1, 0, 2.0, 3.0, 0.5, 0.0, 0.5, 0.0)
        inner_tag = struct.pack("<iiii", 2, 0, 3, 0)
        inner_tag += struct.pack("<ii", len(key_a + key_b), 0)
        inner_tag += struct.pack("<ii", 4, 0) + b"\0" * 16 + b"\0"
        raw = struct.pack("<i", 2) + inner_tag + key_a + key_b
        node = TypeNode("ArrayProperty", [TypeNode("StructProperty")])
        with TemporaryDirectory() as directory:
            path = Path(directory) / "legacy_array.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package(ue5=0))._decode_value(reader, node, len(raw), {})
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value["inner_tag"]["struct_name"], "RichCurveKey")
        self.assertEqual(value["inner_tag"]["header_size"], 49)
        self.assertEqual(value["items"][0]["value"], 1.0)
        self.assertEqual(value["items"][1]["time"], 2.0)


if __name__ == "__main__":
    unittest.main()
