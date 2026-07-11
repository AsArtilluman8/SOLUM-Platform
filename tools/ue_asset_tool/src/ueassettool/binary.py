from __future__ import annotations

import os
import struct
from contextlib import contextmanager
from pathlib import Path
from typing import BinaryIO, Iterator

from .errors import BoundsError, FormatError


class BinaryReader:
    """Little-endian bounded reader with offsets in every error."""

    def __init__(self, path: str | os.PathLike[str]):
        self.path = Path(path)
        self._file: BinaryIO = self.path.open("rb")
        self.size = self.path.stat().st_size
        self._limit = self.size

    def close(self) -> None:
        self._file.close()

    def __enter__(self) -> "BinaryReader":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    @property
    def position(self) -> int:
        return self._file.tell()

    def seek(self, offset: int, whence: int = 0) -> int:
        if whence == 0 and not 0 <= offset <= self._limit:
            raise BoundsError(f"seek 0x{offset:x} outside 0..0x{self._limit:x} in {self.path}")
        pos = self._file.seek(offset, whence)
        if not 0 <= pos <= self._limit:
            raise BoundsError(f"seek resolved to 0x{pos:x} outside boundary 0x{self._limit:x}")
        return pos

    def read(self, size: int) -> bytes:
        if size < 0:
            raise BoundsError(f"negative read length {size} at 0x{self.position:x}")
        end = self.position + size
        if end > self._limit:
            raise BoundsError(
                f"read 0x{size:x} bytes at 0x{self.position:x} crosses boundary 0x{self._limit:x}"
            )
        data = self._file.read(size)
        if len(data) != size:
            raise BoundsError(f"short read at 0x{self.position - len(data):x}: wanted {size}, got {len(data)}")
        return data

    def _unpack(self, fmt: str) -> int | float:
        return struct.unpack("<" + fmt, self.read(struct.calcsize("<" + fmt)))[0]

    def u8(self) -> int:
        return int(self._unpack("B"))

    def i8(self) -> int:
        return int(self._unpack("b"))

    def u16(self) -> int:
        return int(self._unpack("H"))

    def i16(self) -> int:
        return int(self._unpack("h"))

    def u32(self) -> int:
        return int(self._unpack("I"))

    def i32(self) -> int:
        return int(self._unpack("i"))

    def u64(self) -> int:
        return int(self._unpack("Q"))

    def i64(self) -> int:
        return int(self._unpack("q"))

    def f32(self) -> float:
        return float(self._unpack("f"))

    def f64(self) -> float:
        return float(self._unpack("d"))

    def boolean32(self) -> bool:
        offset = self.position
        value = self.i32()
        if value not in (0, 1):
            raise FormatError(f"invalid UE bool32 {value} at 0x{offset:x}")
        return bool(value)

    def flag8(self) -> bool:
        offset = self.position
        value = self.u8()
        if value not in (0, 1):
            raise FormatError(f"invalid UE flag8 {value} at 0x{offset:x}")
        return bool(value)

    def guid(self) -> str:
        a, b, c, d = struct.unpack("<IIII", self.read(16))
        return f"{a:08x}-{b:08x}-{c:08x}-{d:08x}"

    def fname_raw(self) -> tuple[int, int]:
        return self.i32(), self.i32()

    def fstring(self, *, max_units: int = 16_777_216) -> str:
        offset = self.position
        count = self.i32()
        if count == 0:
            return ""
        units = abs(count)
        if units > max_units:
            raise FormatError(f"FString length {count} is implausible at 0x{offset:x}")
        if count > 0:
            raw = self.read(count)
            if not raw.endswith(b"\x00"):
                raise FormatError(f"ANSI FString lacks terminator at 0x{offset:x}")
            try:
                return raw[:-1].decode("utf-8")
            except UnicodeDecodeError:
                return raw[:-1].decode("latin-1")
        raw = self.read(units * 2)
        if not raw.endswith(b"\x00\x00"):
            raise FormatError(f"UTF-16 FString lacks terminator at 0x{offset:x}")
        try:
            return raw[:-2].decode("utf-16-le")
        except UnicodeDecodeError as exc:
            raise FormatError(f"bad UTF-16 FString at 0x{offset:x}: {exc}") from exc

    def count(self, label: str, *, maximum: int = 10_000_000) -> int:
        offset = self.position
        value = self.i32()
        if not 0 <= value <= maximum:
            raise FormatError(f"invalid {label} count {value} at 0x{offset:x}")
        return value

    @contextmanager
    def bounded(self, end: int) -> Iterator[None]:
        old = self._limit
        if not self.position <= end <= old:
            raise BoundsError(f"invalid boundary 0x{end:x} at 0x{self.position:x} (outer 0x{old:x})")
        self._limit = end
        try:
            yield
        finally:
            self._limit = old

