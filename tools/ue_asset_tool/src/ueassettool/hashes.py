from __future__ import annotations

import struct

try:
    from blake3 import blake3 as _native_blake3
except ImportError:  # pragma: no cover - covered by the pure-Python vector tests
    _native_blake3 = None


_IV = (
    0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A,
    0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19,
)
_PERMUTATION = (2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8)
_CHUNK_START = 1
_CHUNK_END = 2
_PARENT = 4
_ROOT = 8
_MASK32 = 0xFFFFFFFF


def _rotate_right(value: int, count: int) -> int:
    return ((value >> count) | (value << (32 - count))) & _MASK32


def _g(state: list[int], a: int, b: int, c: int, d: int, x: int, y: int) -> None:
    state[a] = (state[a] + state[b] + x) & _MASK32
    state[d] = _rotate_right(state[d] ^ state[a], 16)
    state[c] = (state[c] + state[d]) & _MASK32
    state[b] = _rotate_right(state[b] ^ state[c], 12)
    state[a] = (state[a] + state[b] + y) & _MASK32
    state[d] = _rotate_right(state[d] ^ state[a], 8)
    state[c] = (state[c] + state[d]) & _MASK32
    state[b] = _rotate_right(state[b] ^ state[c], 7)


def _round(state: list[int], message: list[int]) -> None:
    _g(state, 0, 4, 8, 12, message[0], message[1])
    _g(state, 1, 5, 9, 13, message[2], message[3])
    _g(state, 2, 6, 10, 14, message[4], message[5])
    _g(state, 3, 7, 11, 15, message[6], message[7])
    _g(state, 0, 5, 10, 15, message[8], message[9])
    _g(state, 1, 6, 11, 12, message[10], message[11])
    _g(state, 2, 7, 8, 13, message[12], message[13])
    _g(state, 3, 4, 9, 14, message[14], message[15])


def _compress(
    chaining_value: tuple[int, ...] | list[int],
    block_words: tuple[int, ...] | list[int],
    counter: int,
    block_length: int,
    flags: int,
) -> list[int]:
    state = list(chaining_value) + list(_IV[:4]) + [
        counter & _MASK32, (counter >> 32) & _MASK32, block_length, flags,
    ]
    message = list(block_words)
    for round_index in range(7):
        _round(state, message)
        if round_index != 6:
            message = [message[index] for index in _PERMUTATION]
    return [state[index] ^ state[index + 8] for index in range(8)] + [
        state[index + 8] ^ chaining_value[index] for index in range(8)
    ]


def _words(block: bytes) -> tuple[int, ...]:
    return struct.unpack("<16I", block.ljust(64, b"\0"))


class _Output:
    def __init__(
        self,
        input_chaining_value: tuple[int, ...] | list[int],
        block_words: tuple[int, ...] | list[int],
        counter: int,
        block_length: int,
        flags: int,
    ):
        self.input_chaining_value = tuple(input_chaining_value)
        self.block_words = tuple(block_words)
        self.counter = counter
        self.block_length = block_length
        self.flags = flags

    def chaining_value(self) -> tuple[int, ...]:
        return tuple(_compress(
            self.input_chaining_value, self.block_words, self.counter,
            self.block_length, self.flags,
        )[:8])

    def root_bytes(self, length: int) -> bytes:
        output = bytearray()
        output_block_counter = 0
        while len(output) < length:
            words = _compress(
                self.input_chaining_value, self.block_words, output_block_counter,
                self.block_length, self.flags | _ROOT,
            )
            output.extend(struct.pack("<16I", *words))
            output_block_counter += 1
        return bytes(output[:length])


def _chunk_output(chunk: bytes, counter: int) -> _Output:
    chaining_value: tuple[int, ...] = _IV
    block_count = max(1, (len(chunk) + 63) // 64)
    for block_index in range(block_count):
        block = chunk[block_index * 64:(block_index + 1) * 64]
        flags = (_CHUNK_START if block_index == 0 else 0)
        if block_index == block_count - 1:
            return _Output(
                chaining_value, _words(block), counter, len(block), flags | _CHUNK_END,
            )
        chaining_value = tuple(_compress(
            chaining_value, _words(block), counter, len(block), flags,
        )[:8])
    raise AssertionError("unreachable")


def _parent_output(left: tuple[int, ...], right: tuple[int, ...]) -> _Output:
    return _Output(_IV, left + right, 0, 64, _PARENT)


def blake3_digest(data: bytes, length: int = 32) -> bytes:
    """Return an unkeyed BLAKE3 digest, with a strict portable fallback."""
    if length < 0:
        raise ValueError("negative BLAKE3 output length")
    if _native_blake3 is not None:
        return _native_blake3(data).digest(length=length)
    chunk_count = max(1, (len(data) + 1023) // 1024)
    stack: list[tuple[int, ...]] = []
    for chunk_index in range(chunk_count - 1):
        chunk = data[chunk_index * 1024:(chunk_index + 1) * 1024]
        chaining_value = _chunk_output(chunk, chunk_index).chaining_value()
        total_chunks = chunk_index + 1
        while total_chunks & 1 == 0:
            chaining_value = _parent_output(stack.pop(), chaining_value).chaining_value()
            total_chunks >>= 1
        stack.append(chaining_value)
    last_index = chunk_count - 1
    output = _chunk_output(data[last_index * 1024:(last_index + 1) * 1024], last_index)
    for left in reversed(stack):
        output = _parent_output(left, output.chaining_value())
    return output.root_bytes(length)
