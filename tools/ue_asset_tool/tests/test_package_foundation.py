import struct
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest

from ueassettool.binary import BinaryReader
from ueassettool.errors import BoundsError, FormatError
from ueassettool.package import UnrealPackage
from ueassettool.properties import PropertyParser, TypeNode


class PackageFoundationTests(unittest.TestCase):
    def _package_with_bytes(self, path: Path) -> UnrealPackage:
        package = UnrealPackage.__new__(UnrealPackage)
        package.path = path
        package.reader = BinaryReader(path)
        package.imports = [object()]
        package.exports = [object()]
        return package

    def test_dependency_package_index_bounds(self) -> None:
        with TemporaryDirectory() as directory:
            path = Path(directory) / "index.bin"
            path.write_bytes(struct.pack("<i", 2))
            package = self._package_with_bytes(path)
            try:
                with self.assertRaisesRegex(FormatError, "outside imports=1, exports=1"):
                    package._checked_dependency_index(package.reader, "test")
            finally:
                package.reader.close()

    def test_dependency_index_truncation_is_a_bounds_error(self) -> None:
        with TemporaryDirectory() as directory:
            path = Path(directory) / "truncated.bin"
            path.write_bytes(b"\x01\x00")
            package = self._package_with_bytes(path)
            try:
                with self.assertRaises(BoundsError):
                    package._checked_dependency_index(package.reader, "test")
            finally:
                package.reader.close()

    def test_preload_block_cannot_leave_table(self) -> None:
        package = UnrealPackage.__new__(UnrealPackage)
        package.preload_dependencies = [{"index": 0}]
        package.exports = [SimpleNamespace(
            first_export_dependency=0,
            serialization_before_serialization_dependencies=2,
            create_before_serialization_dependencies=0,
            serialization_before_create_dependencies=0,
            create_before_create_dependencies=0,
        )]
        with self.assertRaisesRegex(BoundsError, "exceeds table count"):
            package._resolve_preload_dependencies()

    def test_soft_object_property_uses_checked_header_index(self) -> None:
        package = SimpleNamespace(
            summary=SimpleNamespace(file_version_ue4=522, file_version_ue5=1013),
            soft_object_paths=[{
                "package": "/Game/Exact", "asset": "Asset", "sub_path": "Node",
                "object_path": "/Game/Exact.Asset:Node",
                "provenance": {"physical_offset": 40, "size": 20, "sha256": "a" * 64},
            }],
        )
        with TemporaryDirectory() as directory:
            path = Path(directory) / "soft.bin"
            path.write_bytes(struct.pack("<i", 0))
            with BinaryReader(path) as reader:
                value = PropertyParser(package)._decode_value(
                    reader, TypeNode("SoftObjectProperty"), 4, {}
                )
        self.assertEqual(value["soft_object_path_index"], 0)
        self.assertEqual(value["object_path"], "/Game/Exact.Asset:Node")

    def test_soft_object_path_struct_uses_same_header_contract(self) -> None:
        package = SimpleNamespace(
            summary=SimpleNamespace(file_version_ue4=522, file_version_ue5=1013),
            soft_object_paths=[{
                "package": "/Game/Exact", "asset": "Asset", "sub_path": "",
                "object_path": "/Game/Exact.Asset",
                "provenance": {"physical_offset": 40, "size": 20, "sha256": "a" * 64},
            }],
        )
        with TemporaryDirectory() as directory:
            path = Path(directory) / "soft_struct.bin"
            path.write_bytes(struct.pack("<i", 0))
            with BinaryReader(path) as reader:
                value = PropertyParser(package)._decode_struct(
                    reader, TypeNode("StructProperty", [TypeNode("SoftObjectPath")]), 4
                )
        self.assertEqual(value["object_path"], "/Game/Exact.Asset")


if __name__ == "__main__":
    unittest.main()
