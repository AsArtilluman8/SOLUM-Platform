from __future__ import annotations

import hashlib
import json
import math
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .errors import BoundsError, FormatError, UnsupportedError
from .package import UnrealPackage
from .properties import PropertyParser
from .trailer import PackageTrailer, TrailerEntry, load_local_payload, read_package_trailer


MAX_ELEMENTS = 50_000_000
TYPE_NAMES = ("Vector4f", "Vector3f", "Vector2f", "float", "int32", "bool", "FName", "FTransform")
TYPE_SIZES = (16, 12, 8, 4, 4, 1, None, None)


class BufferReader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    @property
    def remaining(self) -> int:
        return len(self.data) - self.pos

    def read(self, size: int) -> bytes:
        if size < 0 or self.pos + size > len(self.data):
            raise BoundsError(f"mesh read {size} at 0x{self.pos:x} leaves payload 0x{len(self.data):x}")
        value = self.data[self.pos:self.pos + size]
        self.pos += size
        return value

    def unpack(self, fmt: str) -> tuple[Any, ...]:
        size = struct.calcsize("<" + fmt)
        return struct.unpack("<" + fmt, self.read(size))

    def i32(self) -> int:
        return self.unpack("i")[0]

    def u32(self) -> int:
        return self.unpack("I")[0]

    def count(self, label: str, maximum: int = MAX_ELEMENTS) -> int:
        start = self.pos
        value = self.i32()
        if not 0 <= value <= maximum:
            raise FormatError(f"invalid {label} count {value} at mesh payload 0x{start:x}")
        return value

    def fstring(self) -> str:
        start = self.pos
        count = self.i32()
        if count == 0:
            return ""
        if abs(count) > 1_000_000:
            raise FormatError(f"implausible mesh FString length {count} at 0x{start:x}")
        if count > 0:
            raw = self.read(count)
            if not raw.endswith(b"\0"):
                raise FormatError(f"mesh FString lacks terminator at 0x{start:x}")
            return raw[:-1].decode("utf-8", "strict")
        raw = self.read(-count * 2)
        if not raw.endswith(b"\0\0"):
            raise FormatError(f"mesh UTF-16 FString lacks terminator at 0x{start:x}")
        return raw[:-2].decode("utf-16-le", "strict")


@dataclass
class MeshAttribute:
    name: str
    type_index: int
    extent: int
    num_elements: int
    channels: list[list[Any]]
    default: Any
    flags: int

    @property
    def type_name(self) -> str:
        return TYPE_NAMES[self.type_index]


@dataclass
class MeshElementContainer:
    name: str
    valid: list[bool]
    num_holes: int
    num_elements: int
    attributes: dict[str, MeshAttribute]

    @property
    def valid_ids(self) -> list[int]:
        return [index for index, value in enumerate(self.valid) if value]


@dataclass
class MeshDescription:
    containers: dict[str, MeshElementContainer]
    source_size: int
    source_sha256: str

    def attribute(self, container: str, name: str) -> MeshAttribute:
        try:
            return self.containers[container].attributes[name]
        except KeyError as exc:
            # Some older MeshDescription payloads contain a historical trailing
            # space in the reserved Position FName. Preserve it in reports, but
            # permit canonical lookup only when the stripped name is unique.
            candidates = [value for key, value in self.containers[container].attributes.items() if key.rstrip() == name]
            if len(candidates) == 1:
                return candidates[0]
            raise FormatError(f"required mesh attribute {container}.{name} is absent") from exc

    def summary(self) -> dict[str, object]:
        return {
            "source_size": self.source_size,
            "source_sha256": self.source_sha256,
            "containers": {
                name: {
                    "array_size": len(container.valid),
                    "valid_elements": len(container.valid_ids),
                    "holes": container.num_holes,
                    "attributes": {
                        attr_name: {
                            "type": attr.type_name,
                            "extent": attr.extent,
                            "elements": attr.num_elements,
                            "channels": len(attr.channels),
                            "flags": attr.flags,
                        }
                        for attr_name, attr in container.attributes.items()
                    },
                }
                for name, container in self.containers.items()
            },
        }


def _value(reader: BufferReader, type_index: int, *, bulk: bool = False) -> Any:
    if type_index == 0:
        return reader.unpack("4f")
    if type_index == 1:
        return reader.unpack("3f")
    if type_index == 2:
        return reader.unpack("2f")
    if type_index == 3:
        return reader.unpack("f")[0]
    if type_index == 4:
        return reader.i32()
    if type_index == 5:
        # TArray<bool>::BulkSerialize stores one byte per value, while FArchive's
        # standalone bool operator is the historical uint32 representation.
        value = reader.read(1)[0] if bulk else reader.u32()
        if value not in (0, 1):
            raise FormatError(f"invalid mesh bool {value} at 0x{reader.pos - 1:x}")
        return bool(value)
    if type_index == 6:
        return reader.fstring()
    if type_index == 7:
        # UE5 FTransform: rotation quaternion, translation and scale (double precision).
        return {"rotation": reader.unpack("4d"), "translation": reader.unpack("3d"), "scale": reader.unpack("3d")}
    raise UnsupportedError(f"mesh attribute type index {type_index}")


def _array(reader: BufferReader, type_index: int, expected: int) -> list[Any]:
    if type_index not in range(len(TYPE_NAMES)):
        raise UnsupportedError(f"mesh attribute type index {type_index}")
    if TYPE_SIZES[type_index] is not None:
        serialized_size = reader.i32()
        count = reader.count("mesh attribute value", MAX_ELEMENTS * 16)
        expected_size = TYPE_SIZES[type_index]
        if serialized_size != expected_size:
            raise FormatError(
                f"bulk mesh element size {serialized_size} != {TYPE_NAMES[type_index]} size {expected_size}"
            )
    else:
        count = reader.count("mesh attribute value", MAX_ELEMENTS * 16)
    if count != expected:
        raise FormatError(f"mesh attribute array count {count} != elements*extent {expected}")
    return [_value(reader, type_index, bulk=True) for _ in range(count)]


def _attribute(reader: BufferReader, name: str, parent_elements: int) -> MeshAttribute:
    type_index = reader.u32()
    extent = reader.u32()
    if type_index >= len(TYPE_NAMES):
        raise UnsupportedError(f"attribute {name} has unknown type index {type_index}")
    if extent == 0:
        raise UnsupportedError(f"unbounded mesh attribute {name} is not yet supported")
    num_elements = reader.count(f"{name} element")
    if num_elements != parent_elements:
        raise FormatError(f"attribute {name} elements {num_elements} != container {parent_elements}")
    channel_count = reader.count(f"{name} channel", 1024)
    channels: list[list[Any]] = []
    for _ in range(channel_count):
        channel_extent = reader.u32()
        if channel_extent != extent:
            raise FormatError(f"attribute {name} channel extent {channel_extent} != entry extent {extent}")
        channels.append(_array(reader, type_index, num_elements * extent))
    default = _value(reader, type_index)
    flags = reader.u32()
    return MeshAttribute(name, type_index, extent, num_elements, channels, default, flags)


def parse_mesh_description(data: bytes) -> MeshDescription:
    reader = BufferReader(data)
    container_count = reader.count("mesh element container", 64)
    containers: dict[str, MeshElementContainer] = {}
    for _ in range(container_count):
        name = reader.fstring()
        if not name or name in containers:
            raise FormatError(f"invalid/duplicate mesh container {name!r}")
        bit_array_marker = reader.i32()
        if bit_array_marker != 1:
            raise UnsupportedError(f"TBitArray serialization marker {bit_array_marker} for {name}")
        bit_count = reader.count(f"{name} bit", MAX_ELEMENTS)
        words = (bit_count + 31) // 32
        packed = reader.read(words * 4)
        valid = [bool(packed[i >> 3] & (1 << (i & 7))) for i in range(bit_count)]
        # UE masks unused high bits to zero or one depending on the TBitArray allocator; ignore only outside NumBits.
        holes = reader.count(f"{name} hole", MAX_ELEMENTS)
        actual_holes = valid.count(False)
        if holes != actual_holes:
            raise FormatError(f"{name} NumHoles {holes} != zero bits {actual_holes}")
        num_elements = reader.count(f"{name} attribute element", MAX_ELEMENTS)
        if num_elements != bit_count:
            raise FormatError(f"{name} attribute array size {num_elements} != bit array size {bit_count}")
        attribute_count = reader.count(f"{name} attribute", 4096)
        attributes: dict[str, MeshAttribute] = {}
        for _ in range(attribute_count):
            attr_name = reader.fstring()
            if not attr_name or attr_name in attributes:
                raise FormatError(f"invalid/duplicate {name} attribute {attr_name!r}")
            attributes[attr_name] = _attribute(reader, attr_name, num_elements)
        containers[name] = MeshElementContainer(name, valid, holes, num_elements, attributes)
    if reader.remaining:
        raise FormatError(f"MeshDescription left {reader.remaining} unconsumed bytes at 0x{reader.pos:x}")
    required = {"Vertices", "VertexInstances", "Triangles", "PolygonGroups"}
    missing = required - containers.keys()
    if missing:
        raise FormatError(f"MeshDescription missing containers: {sorted(missing)}")
    return MeshDescription(containers, len(data), hashlib.sha256(data).hexdigest())


def _tuples(attribute: MeshAttribute, channel: int = 0) -> list[Any]:
    if not 0 <= channel < len(attribute.channels):
        raise FormatError(f"attribute {attribute.name} has no channel {channel}")
    flat = attribute.channels[channel]
    if attribute.extent == 1:
        return flat
    return [tuple(flat[i:i + attribute.extent]) for i in range(0, len(flat), attribute.extent)]


def _vec_ue_to_gltf(value: tuple[float, float, float], *, position: bool) -> tuple[float, float, float]:
    scale = 0.01 if position else 1.0
    return value[1] * scale, value[2] * scale, -value[0] * scale


class GlbBuilder:
    def __init__(self):
        self.data = bytearray()
        self.views: list[dict[str, int]] = []
        self.accessors: list[dict[str, Any]] = []

    def add(self, raw: bytes, *, component: int, count: int, kind: str,
            target: int | None = None, minimum: list[float] | None = None,
            maximum: list[float] | None = None) -> int:
        while len(self.data) % 4:
            self.data.append(0)
        offset = len(self.data)
        self.data.extend(raw)
        view: dict[str, int] = {"buffer": 0, "byteOffset": offset, "byteLength": len(raw)}
        if target is not None:
            view["target"] = target
        view_index = len(self.views)
        self.views.append(view)
        accessor: dict[str, Any] = {
            "bufferView": view_index, "byteOffset": 0, "componentType": component,
            "count": count, "type": kind,
        }
        if minimum is not None:
            accessor["min"] = minimum
        if maximum is not None:
            accessor["max"] = maximum
        index = len(self.accessors)
        self.accessors.append(accessor)
        return index


def mesh_description_to_glb(mesh: MeshDescription, *, source: dict[str, Any]) -> tuple[bytes, dict[str, Any]]:
    vertices = mesh.containers["Vertices"]
    instances = mesh.containers["VertexInstances"]
    triangles = mesh.containers["Triangles"]
    positions = _tuples(mesh.attribute("Vertices", "Position"))
    vertex_ids = _tuples(mesh.attribute("VertexInstances", "VertexIndex"))
    triangle_instances = _tuples(mesh.attribute("Triangles", "VertexInstanceIndex"))
    groups = _tuples(mesh.attribute("Triangles", "PolygonGroupIndex"))
    normals = _tuples(mesh.attribute("VertexInstances", "Normal"))
    tangents = _tuples(mesh.attribute("VertexInstances", "Tangent"))
    signs = _tuples(mesh.attribute("VertexInstances", "BinormalSign"))
    uv_attr = mesh.attribute("VertexInstances", "TextureCoordinate")
    colors = _tuples(mesh.attribute("VertexInstances", "Color"))

    expected_attributes = (
        (mesh.attribute("Vertices", "Position"), 1, 1),
        (mesh.attribute("VertexInstances", "VertexIndex"), 4, 1),
        (mesh.attribute("VertexInstances", "Normal"), 1, 1),
        (mesh.attribute("VertexInstances", "Tangent"), 1, 1),
        (mesh.attribute("VertexInstances", "BinormalSign"), 3, 1),
        (mesh.attribute("VertexInstances", "TextureCoordinate"), 2, 1),
        (mesh.attribute("VertexInstances", "Color"), 0, 1),
        (mesh.attribute("Triangles", "VertexInstanceIndex"), 4, 3),
        (mesh.attribute("Triangles", "PolygonGroupIndex"), 4, 1),
    )
    for attribute, type_index, extent in expected_attributes:
        if attribute.type_index != type_index or attribute.extent != extent:
            raise FormatError(
                f"attribute {attribute.name} is {attribute.type_name}[{attribute.extent}], "
                f"expected {TYPE_NAMES[type_index]}[{extent}]"
            )

    valid_vertices = set(vertices.valid_ids)
    valid_instances = set(instances.valid_ids)
    valid_triangles = triangles.valid_ids
    if any(vertex_ids[vi] not in valid_vertices for vi in valid_instances):
        raise FormatError("VertexInstance.VertexIndex references an invalid vertex")
    if len(normals) != len(instances.valid) or len(tangents) != len(instances.valid):
        raise FormatError("vertex-instance normal/tangent count mismatch")

    used_instances = sorted({vi for tid in valid_triangles for vi in triangle_instances[tid]})
    if any(vi not in valid_instances for vi in used_instances):
        raise FormatError("Triangle.VertexInstanceIndex references an invalid vertex instance")
    remap = {old: new for new, old in enumerate(used_instances)}
    out_positions = [_vec_ue_to_gltf(positions[vertex_ids[vi]], position=True) for vi in used_instances]
    out_normals = [_vec_ue_to_gltf(normals[vi], position=False) for vi in used_instances]
    out_tangents = [(*_vec_ue_to_gltf(tangents[vi], position=False), -float(signs[vi])) for vi in used_instances]
    out_uvs = [(float(v[0]), 1.0 - float(v[1])) for v in _tuples(uv_attr, 0)]
    out_uvs = [out_uvs[vi] for vi in used_instances]
    out_colors = [tuple(float(x) for x in colors[vi]) for vi in used_instances]
    valid_groups = set(mesh.containers["PolygonGroups"].valid_ids)
    if any(int(groups[tid]) not in valid_groups for tid in valid_triangles):
        raise FormatError("Triangle.PolygonGroupIndex references an invalid polygon group")
    if not all(all(math.isfinite(x) for x in uv) for uv in out_uvs):
        raise FormatError("mesh contains non-finite UV values")
    if not all(len(color) == 4 and all(math.isfinite(x) for x in color) for color in out_colors):
        raise FormatError("mesh contains invalid vertex colors")

    # The axis reflection converts UE's left-handed winding into glTF's
    # right-handed winding. Preserve the serialized triangle order; reversing
    # it again would make every supplied normal point behind its face.
    by_group: dict[int, list[int]] = {}
    for tid in valid_triangles:
        tri = triangle_instances[tid]
        if len(tri) != 3:
            raise FormatError(f"triangle {tid} has extent {len(tri)}, expected 3")
        group = int(groups[tid])
        by_group.setdefault(group, []).extend((remap[tri[0]], remap[tri[1]], remap[tri[2]]))

    if not out_positions or not by_group:
        raise FormatError("mesh has no verified geometry")
    for vector in out_positions + out_normals + [v[:3] for v in out_tangents]:
        if not all(math.isfinite(x) for x in vector):
            raise FormatError("mesh contains non-finite vertex data")
    mins = [min(v[i] for v in out_positions) for i in range(3)]
    maxs = [max(v[i] for v in out_positions) for i in range(3)]

    normal_lengths = [math.sqrt(sum(x * x for x in value)) for value in out_normals]
    tangent_lengths = [math.sqrt(sum(x * x for x in value[:3])) for value in out_tangents]
    tangent_normal_dot = [abs(sum(n * t for n, t in zip(normal, tangent[:3]))) for normal, tangent in zip(out_normals, out_tangents)]
    if not all(0.99 <= value <= 1.01 for value in normal_lengths):
        raise FormatError("mesh contains non-unit vertex-instance normals")
    has_tangents = all(0.99 <= value <= 1.01 for value in tangent_lengths)
    if not has_tangents and not all(value <= 1e-8 for value in tangent_lengths):
        raise FormatError("mesh contains a mixture of invalid/non-unit vertex-instance tangents")
    if has_tangents and max(tangent_normal_dot, default=0.0) > 1e-3:
        raise FormatError("mesh tangent is not orthogonal to normal")

    def subtract(a: tuple[float, float, float], b: tuple[float, float, float]) -> tuple[float, float, float]:
        return tuple(x - y for x, y in zip(a, b))  # type: ignore[return-value]

    def cross(a: tuple[float, float, float], b: tuple[float, float, float]) -> tuple[float, float, float]:
        return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

    alignments: list[float] = []
    for indices in by_group.values():
        for start in range(0, len(indices), 3):
            ia, ib, ic = indices[start:start + 3]
            face = cross(subtract(out_positions[ib], out_positions[ia]), subtract(out_positions[ic], out_positions[ia]))
            average = tuple((out_normals[ia][axis] + out_normals[ib][axis] + out_normals[ic][axis]) / 3.0 for axis in range(3))
            alignment = sum(x * y for x, y in zip(face, average))
            if not math.isfinite(alignment) or alignment <= 0.0:
                raise FormatError("triangle winding disagrees with serialized normals")
            alignments.append(alignment)

    builder = GlbBuilder()
    pack = lambda fmt, values: b"".join(struct.pack("<" + fmt, *v) for v in values)
    position_accessor = builder.add(pack("3f", out_positions), component=5126, count=len(out_positions), kind="VEC3", target=34962, minimum=mins, maximum=maxs)
    normal_accessor = builder.add(pack("3f", out_normals), component=5126, count=len(out_normals), kind="VEC3", target=34962)
    tangent_accessor = builder.add(pack("4f", out_tangents), component=5126, count=len(out_tangents), kind="VEC4", target=34962) if has_tangents else None
    uv_accessor = builder.add(pack("2f", out_uvs), component=5126, count=len(out_uvs), kind="VEC2", target=34962)
    color_accessor = builder.add(pack("4f", out_colors), component=5126, count=len(out_colors), kind="VEC4", target=34962)

    polygon_group_names: list[Any] | None = None
    group_container = mesh.containers["PolygonGroups"]
    for candidate in ("ImportedMaterialSlotName", "MaterialSlotName"):
        if candidate in group_container.attributes:
            polygon_group_names = _tuples(group_container.attributes[candidate])
            break
    materials: list[dict[str, Any]] = []
    material_for_group: dict[int, int] = {}
    primitives: list[dict[str, Any]] = []
    index_component = 5123 if len(out_positions) <= 65535 else 5125
    index_fmt = "H" if index_component == 5123 else "I"
    for group, indices in sorted(by_group.items()):
        if any(index < 0 or index >= len(out_positions) for index in indices):
            raise FormatError(f"polygon group {group} contains out-of-range index")
        index_accessor = builder.add(
            b"".join(struct.pack("<" + index_fmt, value) for value in indices),
            component=index_component, count=len(indices), kind="SCALAR", target=34963,
            minimum=[min(indices)], maximum=[max(indices)],
        )
        name = str(polygon_group_names[group]) if polygon_group_names and group < len(polygon_group_names) else f"PolygonGroup_{group}"
        material_for_group[group] = len(materials)
        materials.append({"name": name or f"PolygonGroup_{group}", "extras": {"uePolygonGroup": group}})
        attributes = {"POSITION": position_accessor, "NORMAL": normal_accessor, "TEXCOORD_0": uv_accessor, "COLOR_0": color_accessor}
        if tangent_accessor is not None:
            attributes["TANGENT"] = tangent_accessor
        primitives.append({
            "attributes": attributes,
            "indices": index_accessor, "material": material_for_group[group], "mode": 4,
            "extras": {"uePolygonGroup": group, "triangleCount": len(indices) // 3},
        })

    document = {
        "asset": {"version": "2.0", "generator": "SOLUM UE Asset Truth Reader"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0, "name": source.get("object_name", "StaticMesh")}],
        "meshes": [{"name": source.get("object_name", "StaticMesh"), "primitives": primitives}],
        "materials": materials,
        "buffers": [{"byteLength": len(builder.data)}],
        "bufferViews": builder.views,
        "accessors": builder.accessors,
        "extras": {"ueAssetTruth": source, "coordinateConversion": "(UE.Y, UE.Z, -UE.X) * 0.01; serialized winding preserved"},
    }
    json_chunk = json.dumps(document, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    json_chunk += b" " * ((-len(json_chunk)) % 4)
    bin_chunk = bytes(builder.data) + b"\0" * ((-len(builder.data)) % 4)
    total = 12 + 8 + len(json_chunk) + 8 + len(bin_chunk)
    glb = struct.pack("<4sII", b"glTF", 2, total)
    glb += struct.pack("<I4s", len(json_chunk), b"JSON") + json_chunk
    glb += struct.pack("<I4s", len(bin_chunk), b"BIN\0") + bin_chunk
    report = {
        "vertex_count": len(out_positions), "triangle_count": sum(len(v) for v in by_group.values()) // 3,
        "primitive_count": len(primitives), "bounds_min": mins, "bounds_max": maxs,
        "normal_length_range": [min(normal_lengths), max(normal_lengths)],
        "tangent_status": "serialized" if has_tangents else "omitted_serialized_zero_vectors",
        "tangent_length_range": [min(tangent_lengths), max(tangent_lengths)],
        "max_abs_normal_tangent_dot": max(tangent_normal_dot, default=0.0) if has_tangents else None,
        "face_normal_alignment_min": min(alignments),
        "glb_size": len(glb), "glb_sha256": hashlib.sha256(glb).hexdigest(),
    }
    return glb, report


def _matching_mesh_payload(package: UnrealPackage, trailer: PackageTrailer, *, max_output: int) -> tuple[TrailerEntry, bytes, dict[str, Any]]:
    entries = {bytes.fromhex(entry.identifier): entry for entry in trailer.entries}
    parser = PropertyParser(package)
    matches: list[tuple[TrailerEntry, dict[str, Any]]] = []
    for index, export in enumerate(package.exports, 1):
        if export.class_name != "StaticMeshDescriptionBulkData":
            continue
        decoded = parser.parse_export(index)
        trailing = decoded.get("trailing_native")
        if not trailing:
            continue
        offset = int(trailing["physical_offset"])
        size = int(trailing["size"])
        package.reader.seek(offset)
        raw = package.reader.read(size)
        for identifier, entry in entries.items():
            if identifier in raw:
                matches.append((entry, {"export_index": index, "object": decoded["object"], "class": decoded["class"], "metadata_offset": offset, "metadata_size": size}))
    unique = {(entry.identifier, meta["export_index"]): (entry, meta) for entry, meta in matches}
    if len(unique) != 1:
        raise FormatError(f"expected exactly one mesh bulk/trailer identity match, found {len(unique)}")
    entry, meta = next(iter(unique.values()))
    return entry, load_local_payload(package.path, entry, max_output=max_output), meta


def export_static_mesh(asset: str | Path, output: str | Path, *, max_output: int = 2 * 1024 * 1024 * 1024) -> dict[str, Any]:
    with UnrealPackage(asset) as package:
        static_meshes = [(i, e) for i, e in enumerate(package.exports, 1) if e.class_name == "StaticMesh" and e.is_asset]
        if len(static_meshes) != 1:
            raise FormatError(f"expected one top-level StaticMesh export, found {len(static_meshes)}")
        export_index, export = static_meshes[0]
        if package.summary.payload_toc_offset < 0:
            raise UnsupportedError("StaticMesh has no package trailer; cooked render-data path is not implemented yet")
        trailer = read_package_trailer(package.path, offset=package.summary.payload_toc_offset)
        entry, payload, bulk_meta = _matching_mesh_payload(package, trailer, max_output=max_output)
        mesh = parse_mesh_description(payload)
        source = {
            "status": "VERIFIED", "path": str(package.path), "sha256": package.sha256,
            "engine": package.summary.saved_by_engine_version.display if package.summary.saved_by_engine_version else None,
            "file_version_ue4": package.summary.file_version_ue4, "file_version_ue5": package.summary.file_version_ue5,
            "export_index": export_index, "object_name": export.object_name.display, "class": export.class_name,
            "payload_identifier": entry.identifier, "payload_offset": entry.absolute_offset,
            "payload_compressed_size": entry.compressed_size, "payload_raw_size": entry.raw_size,
            "mesh_bulk_export": bulk_meta,
        }
        glb, geometry = mesh_description_to_glb(mesh, source=source)
        target = Path(output)
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_name(target.name + ".tmp")
        temporary.write_bytes(glb)
        temporary.replace(target)
        return {
            "schema": "ueassettool.mesh-export/v1", "status": "VERIFIED", "source": source,
            "trailer": trailer.to_dict(), "mesh_description": mesh.summary(),
            "geometry": geometry, "output": str(target),
        }


def validate_glb(path: str | Path) -> dict[str, Any]:
    raw = Path(path).read_bytes()
    if len(raw) < 20:
        raise FormatError("GLB is too small")
    magic, version, total = struct.unpack_from("<4sII", raw)
    if magic != b"glTF" or version != 2 or total != len(raw):
        raise FormatError("invalid GLB header/declared length")
    position = 12
    chunks: list[tuple[str, int]] = []
    document = None
    while position < len(raw):
        if position + 8 > len(raw):
            raise BoundsError("truncated GLB chunk header")
        size, kind = struct.unpack_from("<I4s", raw, position)
        position += 8
        if position + size > len(raw):
            raise BoundsError("GLB chunk exceeds declared file length")
        payload = raw[position:position + size]
        position += size
        chunks.append((kind.decode("ascii", "replace"), size))
        if kind == b"JSON":
            document = json.loads(payload.decode("utf-8"))
    if position != len(raw) or document is None:
        raise FormatError("GLB chunks do not consume file or JSON chunk is absent")
    bin_size = next((size for kind, size in chunks if kind == "BIN\x00"), None)
    if bin_size is None:
        raise FormatError("GLB BIN chunk is absent")
    for view in document.get("bufferViews", []):
        if view.get("byteOffset", 0) + view["byteLength"] > bin_size:
            raise BoundsError("GLB bufferView exceeds BIN chunk")
    return {"status": "VERIFIED", "size": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "chunks": chunks, "meshes": len(document.get("meshes", [])), "accessors": len(document.get("accessors", []))}
