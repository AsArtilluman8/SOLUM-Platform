import struct
from pathlib import Path
from types import SimpleNamespace
from tempfile import TemporaryDirectory
import unittest

from ueassettool.binary import BinaryReader
from ueassettool.bytecode import StructScriptDecoder
from ueassettool.model import FNameRef


class _FakePackage:
    def __init__(self, path: Path):
        self.path = path
        self.imports = []
        self.exports = []
        self.summary = SimpleNamespace(
            file_version_ue4=522,
            file_version_ue5=1013,
            package_flags=0,
        )

    @staticmethod
    def custom_version(guid: str, default: int = -1) -> int:
        versions = {
            "375ec13c-06e448fb-b50084f0-262a717e": 4,
            "cffc743f-43b04480-939114df-171d2073": 37,
            "9c54d522-a8264fbe-94210746-61b482d0": 44,
            "601d1886-ac644f84-aa16d3de-0deac7d6": 170,
        }
        return versions.get(guid, default)

    @staticmethod
    def fname(raw: tuple[int, int]) -> FNameRef:
        if raw[0] != 0 or raw[1] != 0:
            raise ValueError(raw)
        return FNameRef(0, 0, "None")

    @staticmethod
    def object_path(index: int) -> str:
        return "None" if index == 0 else f"bad:{index}"


class BytecodeTests(unittest.TestCase):
    def _decode(self, raw: bytes, bytecode_size: int):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "script.bin"
            path.write_bytes(raw)
            decoder = StructScriptDecoder(_FakePackage(path))
            with BinaryReader(path) as reader:
                return decoder._script(reader, bytecode_size, len(raw))

    def test_nothing_and_end_are_exact(self) -> None:
        result = self._decode(bytes([0x0B, 0x53]), 2)
        self.assertEqual([item["token"] for item in result["expressions"]], ["EX_Nothing", "EX_EndOfScript"])
        self.assertTrue(all(result["validation"].values()))

    def test_object_reference_expands_to_vm_pointer(self) -> None:
        raw = bytes([0x20]) + struct.pack("<i", 0) + bytes([0x53])
        result = self._decode(raw, 10)
        self.assertEqual(result["serialized_script_size"], 6)
        self.assertEqual(result["bytecode_buffer_size"], 10)
        self.assertEqual(result["expressions"][0]["object"]["index"], 0)

    def test_case_preserving_script_name_is_twelve_vm_bytes(self) -> None:
        raw = bytes([0x1B]) + struct.pack("<ii", 0, 0) + bytes([0x16, 0x53])
        result = self._decode(raw, 15)
        self.assertEqual(result["expressions"][0]["function_name"]["value"], "None")
        self.assertEqual(result["expressions"][0]["bytecode_size"], 14)


if __name__ == "__main__":
    unittest.main()
