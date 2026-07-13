from __future__ import annotations

import hashlib
import os
import platform
import resource
import shutil
import struct
import subprocess
import tempfile
import zlib
from dataclasses import dataclass
from pathlib import Path

from .errors import BoundsError, FormatError, UnsupportedError
from .hashes import blake3_digest


COMPRESSED_BUFFER_MAGIC = 0xB7756362


@dataclass(frozen=True)
class CompressedBufferHeader:
    offset: int
    crc32: int
    method: int
    compressor: int
    compression_level: int
    block_size_exponent: int
    block_count: int
    total_raw_size: int
    total_compressed_size: int
    raw_hash: str
    block_sizes: tuple[int, ...]

    @property
    def block_size(self) -> int:
        return self.total_raw_size if self.block_size_exponent == 0 else 1 << self.block_size_exponent

    @property
    def block_table_size(self) -> int:
        return 0 if self.block_size_exponent == 0 else 4 * self.block_count


def read_compressed_buffer_header(path: str | Path, offset: int) -> CompressedBufferHeader:
    source = Path(path)
    size = source.stat().st_size
    if offset < 0 or offset + 64 > size:
        raise BoundsError(f"compressed buffer header at 0x{offset:x} is outside {source}")
    with source.open("rb") as f:
        f.seek(offset)
        raw = f.read(64)
        magic, crc = struct.unpack(">II", raw[:8])
        if magic != COMPRESSED_BUFFER_MAGIC:
            raise FormatError(f"FCompressedBuffer magic 0x{magic:08x} at 0x{offset:x} is invalid")
        method, compressor, level, exponent = raw[8:12]
        block_count = int.from_bytes(raw[12:16], "big")
        total_raw = int.from_bytes(raw[16:24], "big")
        total_compressed = int.from_bytes(raw[24:32], "big")
        raw_hash = raw[32:64].hex()
        if not 0 < block_count <= 1_000_000:
            raise FormatError(f"invalid FCompressedBuffer block count {block_count}")
        uncompressed_single_block = exponent == 0
        if uncompressed_single_block:
            if method != 0 or block_count != 1 or total_compressed != 64 + total_raw:
                raise FormatError(
                    "zero-exponent FCompressedBuffer is not the exact uncompressed single-block layout"
                )
        else:
            if not 0 < exponent < 63:
                raise FormatError(f"invalid FCompressedBuffer block exponent {exponent}")
            expected_blocks = (total_raw + (1 << exponent) - 1) >> exponent
            if block_count != expected_blocks:
                raise FormatError(f"block count {block_count} != ceil(raw/block) {expected_blocks}")
        table_size = 0 if uncompressed_single_block else 4 * block_count
        if total_compressed < 64 + table_size or offset + total_compressed > size:
            raise BoundsError(
                f"compressed buffer declares {total_compressed} bytes at 0x{offset:x}, file size is 0x{size:x}"
            )
        table = f.read(table_size)
        actual_crc = zlib.crc32(raw[8:] + table) & 0xFFFFFFFF
        if actual_crc != crc:
            raise FormatError(
                f"FCompressedBuffer header CRC32 0x{actual_crc:08x} != serialized 0x{crc:08x}"
            )
        block_sizes = (
            (total_raw,)
            if uncompressed_single_block
            else tuple(int.from_bytes(table[i:i + 4], "big") for i in range(0, len(table), 4))
        )
        if any(value <= 0 for value in block_sizes):
            raise FormatError("FCompressedBuffer contains a zero-sized block")
        if 64 + table_size + sum(block_sizes) != total_compressed:
            raise FormatError("FCompressedBuffer block sizes do not sum to TotalCompressedSize")
    return CompressedBufferHeader(
        offset, crc, method, compressor, level, exponent, block_count,
        total_raw, total_compressed, raw_hash, block_sizes,
    )


def _lz4_block(data: bytes, expected_size: int) -> bytes:
    """Decode a raw LZ4 block with strict input/output bounds."""
    src = 0
    out = bytearray()
    while src < len(data):
        token = data[src]
        src += 1
        literal_len = token >> 4
        if literal_len == 15:
            while True:
                if src >= len(data):
                    raise FormatError("truncated LZ4 literal length")
                extra = data[src]
                src += 1
                literal_len += extra
                if extra != 255:
                    break
        if src + literal_len > len(data) or len(out) + literal_len > expected_size:
            raise BoundsError("LZ4 literals leave declared block boundary")
        out.extend(data[src:src + literal_len])
        src += literal_len
        if src == len(data):
            break
        if src + 2 > len(data):
            raise FormatError("truncated LZ4 match offset")
        distance = int.from_bytes(data[src:src + 2], "little")
        src += 2
        if distance == 0 or distance > len(out):
            raise FormatError(f"invalid LZ4 match distance {distance}")
        match_len = (token & 0x0F) + 4
        if (token & 0x0F) == 15:
            while True:
                if src >= len(data):
                    raise FormatError("truncated LZ4 match length")
                extra = data[src]
                src += 1
                match_len += extra
                if extra != 255:
                    break
        if len(out) + match_len > expected_size:
            raise BoundsError("LZ4 match leaves declared output boundary")
        for _ in range(match_len):
            out.append(out[-distance])
    if len(out) != expected_size:
        raise FormatError(f"LZ4 decoded {len(out)} bytes, expected {expected_size}")
    return bytes(out)


def bundled_ooz_source_dir() -> Path | None:
    candidate = Path(__file__).resolve().parents[2] / "tools" / "ooz"
    return candidate if (candidate / "kraken.cpp").exists() else None


def build_bundled_ooz(*, force: bool = False) -> Path:
    source = bundled_ooz_source_dir()
    if source is None:
        raise UnsupportedError("bundled ooz source directory is not installed")
    machine = platform.machine().lower()
    if machine not in ("x86_64", "amd64", "i386", "i686"):
        raise UnsupportedError(
            f"bundled ooz uses x86 SIMD and cannot be built for {machine}; provide UEASSET_OODLE_HELPER "
            "or an Oodle binary for this platform"
        )
    target = source / "ooz-helper"
    if target.exists() and not force:
        return target
    compiler = shutil.which("g++") or shutil.which("clang++")
    if not compiler:
        raise UnsupportedError("g++/clang++ is required to build the optional ooz backend")
    command = [
        compiler, "-O2", "-std=c++17", "-msse4.1", "-fno-strict-aliasing",
        str(source / "kraken.cpp"), str(source / "bitknit.cpp"), str(source / "lzna.cpp"),
        str(source / "ooz_cli.cpp"), "-o", str(target),
    ]
    result = subprocess.run(command, capture_output=True, text=True, timeout=180)
    if result.returncode:
        raise UnsupportedError(f"ooz build failed: {result.stderr.strip()[-2000:]}")
    return target


def find_oodle_helper() -> Path:
    configured = os.environ.get("UEASSET_OODLE_HELPER")
    if configured:
        helper = Path(configured)
        if helper.is_file() and os.access(helper, os.X_OK):
            return helper
        raise UnsupportedError(f"UEASSET_OODLE_HELPER is not executable: {helper}")
    source = bundled_ooz_source_dir()
    if source and (source / "ooz-helper").is_file():
        return source / "ooz-helper"
    return build_bundled_ooz()


def _helper_limits(raw_size: int) -> None:
    # Keep a malformed stream inside a small disposable process. The upstream
    # decoder may write 64 bytes past output by design; the helper allocates it.
    memory = max(256 * 1024 * 1024, raw_size * 8 + 32 * 1024 * 1024)
    resource.setrlimit(resource.RLIMIT_AS, (memory, memory))
    resource.setrlimit(resource.RLIMIT_CPU, (30, 30))
    resource.setrlimit(resource.RLIMIT_FSIZE, (raw_size + 4096, raw_size + 4096))


def _oodle_block(helper: Path, data: bytes, raw_size: int, *, timeout: int = 45) -> bytes:
    with tempfile.TemporaryDirectory(prefix="ueasset-oodle-") as td:
        source = Path(td) / "block.oodle"
        target = Path(td) / "block.raw"
        source.write_bytes(data)
        result = subprocess.run(
            [str(helper), str(source), str(target), str(raw_size)],
            capture_output=True,
            text=True,
            timeout=timeout,
            preexec_fn=lambda: _helper_limits(raw_size),
        )
        if result.returncode:
            raise FormatError(f"Oodle helper rejected block: {result.stderr.strip()[-1000:]}")
        output = target.read_bytes()
        if len(output) != raw_size:
            raise FormatError(f"Oodle helper wrote {len(output)} bytes, expected {raw_size}")
        return output


def decompress_compressed_buffer(
    path: str | Path,
    offset: int,
    *,
    max_output: int = 2 * 1024 * 1024 * 1024,
) -> tuple[CompressedBufferHeader, bytes]:
    header = read_compressed_buffer_header(path, offset)
    if header.total_raw_size > max_output:
        raise BoundsError(
            f"refusing {header.total_raw_size} output bytes; increase --max-output explicitly"
        )
    data_start = offset + 64 + header.block_table_size
    output = bytearray()
    helper = find_oodle_helper() if header.method == 3 else None
    with Path(path).open("rb") as f:
        f.seek(data_start)
        remaining = header.total_raw_size
        for compressed_size in header.block_sizes:
            compressed = f.read(compressed_size)
            if len(compressed) != compressed_size:
                raise BoundsError("short compressed block read")
            raw_size = min(header.block_size, remaining)
            if header.method == 0:
                if len(compressed) != raw_size:
                    raise FormatError("uncompressed FCompressedBuffer block size mismatch")
                block = compressed
            elif header.method == 3:
                block = _oodle_block(helper, compressed, raw_size)  # type: ignore[arg-type]
            elif header.method == 4:
                block = _lz4_block(compressed, raw_size)
            else:
                raise UnsupportedError(f"FCompressedBuffer compression method {header.method}")
            output.extend(block)
            remaining -= raw_size
    if remaining != 0 or len(output) != header.total_raw_size:
        raise FormatError("decompressed FCompressedBuffer length invariant failed")
    actual_hash = blake3_digest(bytes(output)).hex()
    if actual_hash != header.raw_hash:
        raise FormatError(
            f"FCompressedBuffer BLAKE3 {actual_hash} != serialized RawHash {header.raw_hash}"
        )
    return header, bytes(output)


def decompress_legacy_chunked_zlib(
    path: str | Path,
    offset: int,
    stored_size: int,
    expected_size: int,
    *,
    max_output: int = 2 * 1024 * 1024 * 1024,
) -> tuple[dict[str, object], bytes]:
    """Decode UE4 ``SerializeCompressed`` chunked-zlib with exact bounds."""
    if expected_size < 0 or expected_size > max_output:
        raise BoundsError(
            f"refusing {expected_size} legacy-zlib output bytes; increase --max-output explicitly"
        )
    source = Path(path)
    if offset < 0 or stored_size < 32 or offset + stored_size > source.stat().st_size:
        raise BoundsError("legacy chunked-zlib range leaves its payload file")
    with source.open("rb") as handle:
        handle.seek(offset)
        header = handle.read(32)
        tag, reserved = struct.unpack_from("<II", header)
        chunk_size, total_compressed, total_raw = struct.unpack_from("<QQQ", header, 8)
        if tag != 0x9E2A83C1 or reserved != 0:
            raise FormatError("legacy chunked-zlib package tag/reserved field is invalid")
        if not chunk_size or total_raw != expected_size:
            raise FormatError(
                f"legacy chunked-zlib raw size {total_raw} != bulk element count {expected_size}"
            )
        chunk_count = (total_raw + chunk_size - 1) // chunk_size
        if not 0 < chunk_count <= 1_000_000:
            raise FormatError(f"legacy chunked-zlib chunk count {chunk_count} is invalid")
        table = handle.read(16 * chunk_count)
        if len(table) != 16 * chunk_count:
            raise BoundsError("legacy chunked-zlib table is truncated")
        chunks = [struct.unpack_from("<QQ", table, index * 16) for index in range(chunk_count)]
        if sum(item[0] for item in chunks) != total_compressed:
            raise FormatError("legacy chunked-zlib compressed chunk sizes do not sum to total")
        if sum(item[1] for item in chunks) != total_raw:
            raise FormatError("legacy chunked-zlib raw chunk sizes do not sum to total")
        header_size = 32 + len(table)
        if header_size + total_compressed != stored_size:
            raise FormatError(
                f"legacy chunked-zlib header+chunks {header_size + total_compressed} != bulk size {stored_size}"
            )
        output = bytearray()
        compressed_sha256 = hashlib.sha256()
        for compressed_size, raw_size in chunks:
            compressed = handle.read(compressed_size)
            if len(compressed) != compressed_size:
                raise BoundsError("legacy chunked-zlib chunk is truncated")
            compressed_sha256.update(compressed)
            decoder = zlib.decompressobj()
            raw = decoder.decompress(compressed, raw_size + 1)
            raw += decoder.flush()
            if decoder.unused_data or decoder.unconsumed_tail or not decoder.eof:
                raise FormatError("legacy chunked-zlib stream has trailing or unconsumed data")
            if len(raw) != raw_size:
                raise FormatError(f"legacy zlib chunk decoded {len(raw)} bytes, expected {raw_size}")
            output.extend(raw)
    if len(output) != expected_size:
        raise FormatError(f"legacy zlib output {len(output)} != expected {expected_size}")
    return {
        "format": "UE4 SerializeCompressed zlib",
        "offset": offset,
        "stored_size": stored_size,
        "chunk_size": chunk_size,
        "chunk_count": chunk_count,
        "total_compressed_size": total_compressed,
        "total_raw_size": total_raw,
        "compressed_chunks_sha256": compressed_sha256.hexdigest(),
        "raw_sha256": hashlib.sha256(output).hexdigest(),
    }, bytes(output)


def write_verified_output(path: str | Path, data: bytes) -> dict[str, object]:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    return {"path": str(target), "size": len(data), "sha256": hashlib.sha256(data).hexdigest()}
