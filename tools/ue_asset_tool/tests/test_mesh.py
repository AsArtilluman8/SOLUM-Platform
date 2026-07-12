import json
import struct
import tempfile
import unittest
from pathlib import Path

from ueassettool.mesh import mesh_description_to_glb, parse_mesh_description, validate_glb
from ueassettool.trailer import FOOTER_TAG, HEADER_TAG, PACKAGE_TAG, read_package_trailer


def fstring(value: str) -> bytes:
    raw = value.encode() + b"\0"
    return struct.pack("<i", len(raw)) + raw


def bit_array(values: list[bool]) -> bytes:
    words = bytearray(((len(values) + 31) // 32) * 4)
    for index, value in enumerate(values):
        if value:
            words[index >> 3] |= 1 << (index & 7)
    return struct.pack("<ii", 1, len(values)) + words + struct.pack("<i", values.count(False))


SIZES = {0: 16, 1: 12, 2: 8, 3: 4, 4: 4}
FORMATS = {0: "4f", 1: "3f", 2: "2f", 3: "f", 4: "i"}


def attribute(name: str, type_index: int, extent: int, values: list, default, *, flags: int = 0) -> bytes:
    count = len(values) // extent
    data = fstring(name) + struct.pack("<IIiiI", type_index, extent, count, 1, extent)
    flattened = []
    for value in values:
        flattened.append(value if isinstance(value, tuple) else (value,))
    raw = b"".join(struct.pack("<" + FORMATS[type_index], *value) for value in flattened)
    data += struct.pack("<ii", SIZES[type_index], len(values)) + raw
    data += struct.pack("<" + FORMATS[type_index], *(default if isinstance(default, tuple) else (default,)))
    return data + struct.pack("<I", flags)


def name_attribute(name: str, values: list[str], default: str = "") -> bytes:
    data = fstring(name) + struct.pack("<IIiiI", 6, 1, len(values), 1, 1)
    data += struct.pack("<i", len(values)) + b"".join(fstring(value) for value in values)
    return data + fstring(default) + struct.pack("<I", 0)


def container(name: str, valid: list[bool], attributes: list[bytes]) -> bytes:
    return fstring(name) + bit_array(valid) + struct.pack("<ii", len(valid), len(attributes)) + b"".join(attributes)


def triangle_mesh_description() -> bytes:
    vertices = container("Vertices", [True] * 3, [attribute(
        "Position", 1, 1, [(0.0, 0.0, 0.0), (100.0, 0.0, 0.0), (0.0, 100.0, 0.0)], (0.0, 0.0, 0.0)
    )])
    instances = container("VertexInstances", [True] * 3, [
        attribute("VertexIndex", 4, 1, [0, 1, 2], -1),
        attribute("TextureCoordinate", 2, 1, [(0.0, 0.0), (1.0, 0.0), (0.0, 1.0)], (0.0, 0.0)),
        attribute("Normal", 1, 1, [(0.0, 0.0, 1.0)] * 3, (0.0, 0.0, 0.0)),
        attribute("Tangent", 1, 1, [(1.0, 0.0, 0.0)] * 3, (0.0, 0.0, 0.0)),
        attribute("BinormalSign", 3, 1, [1.0] * 3, 0.0),
        attribute("Color", 0, 1, [(1.0, 1.0, 1.0, 1.0)] * 3, (1.0, 1.0, 1.0, 1.0)),
    ])
    triangles = container("Triangles", [True], [
        # UE left-handed winding that agrees with the serialized +Z normal.
        attribute("VertexInstanceIndex", 4, 3, [0, 2, 1], -1, flags=16),
        attribute("PolygonGroupIndex", 4, 1, [0], -1, flags=16),
    ])
    groups = container("PolygonGroups", [True], [name_attribute("ImportedMaterialSlotName", ["TestMaterial"])])
    pieces = [vertices, instances, container("UVs", [], []), container("Edges", [], []), triangles, container("Polygons", [True], []), groups]
    return struct.pack("<i", len(pieces)) + b"".join(pieces)


class MeshTests(unittest.TestCase):
    def test_mesh_description_to_valid_glb(self) -> None:
        mesh = parse_mesh_description(triangle_mesh_description())
        glb, report = mesh_description_to_glb(mesh, source={"object_name": "Triangle", "status": "VERIFIED"})
        self.assertEqual(report["triangle_count"], 1)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "triangle.glb"
            path.write_bytes(glb)
            self.assertEqual(validate_glb(path)["status"], "VERIFIED")

    def test_package_trailer_bounds(self) -> None:
        identifier = bytes(range(20))
        payload = b"x" * 64
        header = struct.pack("<QIIQi", HEADER_TAG, 2, 77, len(payload), 1)
        entry = identifier + struct.pack("<qQQHHB", 0, len(payload), 128, 0, 0, 0)
        trailer_length = len(header) + len(entry) + len(payload) + 20
        footer = struct.pack("<QQI", FOOTER_TAG, trailer_length, PACKAGE_TAG)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "trailer.uasset"
            path.write_bytes(header + entry + payload + footer)
            result = read_package_trailer(path, offset=0)
            self.assertEqual(result.entries[0].absolute_offset, 77)
            self.assertEqual(result.entries[0].identifier, identifier.hex())


if __name__ == "__main__":
    unittest.main()
