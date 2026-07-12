import struct
from pathlib import Path

import unittest

from ueassettool.errors import FormatError
from ueassettool.package import UnrealPackage


class PackageSmokeTests(unittest.TestCase):
    def test_rejects_non_unreal_file(self) -> None:
        from tempfile import TemporaryDirectory
        with TemporaryDirectory() as directory:
            sample = Path(directory) / "fake.uasset"
            sample.write_bytes(struct.pack("<I", 0x12345678) + b"not unreal")
            with self.assertRaisesRegex(FormatError, "bad package magic"):
                UnrealPackage(sample)


if __name__ == "__main__":
    unittest.main()
