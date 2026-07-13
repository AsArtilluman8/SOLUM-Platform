import struct
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest

from ueassettool.binary import BinaryReader
from ueassettool.model import FNameRef
from ueassettool.properties import GUID_EDITOR, GUID_NIAGARA, PropertyParser, TypeNode


class _Package:
    names = ("None", "InputPin", "Keys", "StructProperty", "RichCurveKey", "VariableName")

    def __init__(self, *, ue5: int = 1013, custom_versions: dict[str, int] | None = None):
        self.summary = SimpleNamespace(file_version_ue4=522, file_version_ue5=ue5)
        self.custom_versions = custom_versions or {}

    def fname(self, raw: tuple[int, int]) -> FNameRef:
        index, number = raw
        return FNameRef(index, number, self.names[index])

    @staticmethod
    def object_path(index: int) -> str:
        return "None" if index == 0 else f"Object[{index}]"

    def custom_version(self, guid: str, default: int = -1) -> int:
        return self.custom_versions.get(guid, default)

    @staticmethod
    def soft_object_path(index: int) -> dict[str, object]:
        if index not in (0, 1):
            raise AssertionError(index)
        return {
            "table_index": index,
            "package": None if index == 0 else "/Game/Textures/T_Test",
            "asset": None if index == 0 else "T_Test",
            "sub_path": "",
        }


class ContainerPropertyTests(unittest.TestCase):
    def test_transform3f_and_transform3d_consume_exact_serialized_widths(self) -> None:
        transform3f = struct.pack("<10f", *(float(value) for value in range(10)))
        transform3d = struct.pack("<10d", *(float(value) for value in range(10)))
        with TemporaryDirectory() as directory:
            path = Path(directory) / "transforms.bin"
            path.write_bytes(transform3f + transform3d)
            with BinaryReader(path) as reader:
                single = PropertyParser(_Package())._decode_struct(
                    reader, TypeNode("StructProperty", [TypeNode("FTransform3f")]), len(transform3f)
                )
                double = PropertyParser(_Package())._decode_struct(
                    reader, TypeNode("StructProperty", [TypeNode("FTransform3d")]),
                    len(transform3f) + len(transform3d),
                )
                self.assertEqual(reader.position, len(transform3f) + len(transform3d))
        self.assertEqual(single["serialization_variant"], "FTransform3f")
        self.assertEqual(single["rotation"]["w"], 3.0)
        self.assertEqual(double["serialization_variant"], "FTransform3d")
        self.assertEqual(double["scale"]["z"], 9.0)

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

    def test_base_text_array_item_consumes_exactly(self) -> None:
        def fstring(value: str) -> bytes:
            encoded = value.encode("utf-8") + b"\0"
            return struct.pack("<i", len(encoded)) + encoded

        item = struct.pack("<Ib", 0, 0)
        item += fstring("") + fstring("0123456789ABCDEF") + fstring("Weather")
        raw = struct.pack("<i", 1) + item
        node = TypeNode("ArrayProperty", [TypeNode("TextProperty")])
        with TemporaryDirectory() as directory:
            path = Path(directory) / "text_array.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package())._decode_value(reader, node, len(raw), {})
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value["count"], 1)
        self.assertEqual(value["items"][0], {
            "flags": 0,
            "history_type": "Base",
            "namespace": "",
            "key": "0123456789ABCDEF",
            "source": "Weather",
        })

    def test_modern_empty_text_reads_culture_invariant_marker(self) -> None:
        raw = struct.pack("<IbI", 0, -1, 0)
        with TemporaryDirectory() as directory:
            path = Path(directory) / "empty_text.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package(custom_versions={GUID_EDITOR: 31}))._decode_text(reader)
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value, {
            "flags": 0, "history_type": "None", "culture_invariant_present": False,
        })

    def test_modern_niagara_variable_native_serializer(self) -> None:
        raw = struct.pack("<ii", 5, 0)  # variable FName
        raw += struct.pack("<ii", 0, 0)  # empty tagged type definition, None terminator
        raw += struct.pack("<i", 4) + struct.pack("<f", 1.25)
        node = TypeNode("StructProperty", [TypeNode("NiagaraVariable")])
        package = _Package(custom_versions={GUID_NIAGARA: 64})
        with TemporaryDirectory() as directory:
            path = Path(directory) / "niagara_variable.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(package)._decode_struct(reader, node, len(raw))
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value["serialization"], "type-definition-registry")
        self.assertEqual(value["name"], "VariableName")
        self.assertEqual(value["data_hex"], struct.pack("<f", 1.25).hex())

    def test_deduplicated_soft_object_path_index(self) -> None:
        raw = struct.pack("<i", 1)
        node = TypeNode("SoftObjectProperty")
        with TemporaryDirectory() as directory:
            path = Path(directory) / "soft_path.bin"
            path.write_bytes(raw)
            with BinaryReader(path) as reader:
                value = PropertyParser(_Package(ue5=1013))._decode_value(reader, node, len(raw), {})
                self.assertEqual(reader.position, len(raw))
        self.assertEqual(value, {
            "table_index": 1,
            "package": "/Game/Textures/T_Test",
            "asset": "T_Test",
            "sub_path": "",
        })

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
