import struct
from pathlib import Path

import pytest

from ueassettool.errors import FormatError
from ueassettool.package import UnrealPackage


def test_rejects_non_unreal_file(tmp_path: Path) -> None:
    sample = tmp_path / "fake.uasset"
    sample.write_bytes(struct.pack("<I", 0x12345678) + b"not unreal")
    with pytest.raises(FormatError, match="bad package magic"):
        UnrealPackage(sample)
