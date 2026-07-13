from __future__ import annotations

import hashlib
import math
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .errors import BoundsError, FormatError, UnsupportedError
from .model import FNameRef
from . import versions as ver


TAG_HAS_ARRAY_INDEX = 0x01
TAG_HAS_PROPERTY_GUID = 0x02
TAG_HAS_EXTENSIONS = 0x04
TAG_BINARY_OR_NATIVE = 0x08
TAG_BOOL_TRUE = 0x10
TAG_SKIPPED = 0x20
TAG_KNOWN_MASK = 0x3F
EXT_OVERRIDABLE_INFORMATION = 0x02
GUID_NIAGARA = "fcf57afa-50764283-b9a9e658-ffa02d32"
NIAGARA_VARIABLES_USE_TYPEDEF_REGISTRY = 64


@dataclass
class TypeNode:
    name: str
    parameters: list["TypeNode"] = field(default_factory=list)

    @property
    def display(self) -> str:
        if not self.parameters:
            return self.name
        return f"{self.name}<{', '.join(x.display for x in self.parameters)}>"

    def to_dict(self) -> dict[str, Any]:
        return {"name": self.name, "parameters": [x.to_dict() for x in self.parameters], "display": self.display}


def _safe_float(value: float) -> float | dict[str, str]:
    if math.isfinite(value):
        return value
    return {"non_finite": repr(value)}


class PropertyParser:
    """Tagged FProperty reader with byte-level provenance.

    A value is marked ``decoded`` only when the type decoder consumes exactly
    the declared property size. Unknown native structures remain raw and carry
    offset, length, SHA-256 and a short preview.
    """

    def __init__(self, package: Any):
        self.package = package
        self.summary = package.summary

    def _fname(self, r: BinaryReader) -> FNameRef:
        return self.package.fname(r.fname_raw())

    def _read_type_node(self, r: BinaryReader, *, depth: int = 0) -> TypeNode:
        if depth > 32:
            raise FormatError("property type tree exceeds 32 levels")
        name = self._fname(r).display
        count = r.i32()
        if not 0 <= count <= 64:
            raise FormatError(f"invalid inner type count {count} for {name} at 0x{r.position - 4:x}")
        return TypeNode(name, [self._read_type_node(r, depth=depth + 1) for _ in range(count)])

    def _legacy_type(self, r: BinaryReader, type_name: str) -> tuple[TypeNode, dict[str, Any]]:
        meta: dict[str, Any] = {}
        params: list[TypeNode] = []
        if type_name == "StructProperty":
            struct_name = self._fname(r).display
            params.append(TypeNode(struct_name))
            meta["struct_name"] = struct_name
            if self.summary.file_version_ue4 >= ver.UE4_STRUCT_GUID_IN_PROPERTY_TAG:
                meta["struct_guid"] = r.guid()
        elif type_name == "BoolProperty":
            meta["bool"] = r.flag8()
        elif type_name in ("ByteProperty", "EnumProperty"):
            enum_name = self._fname(r).display
            params.append(TypeNode(enum_name))
            meta["enum_name"] = enum_name
        elif type_name == "ArrayProperty" and self.summary.file_version_ue4 >= ver.UE4_ARRAY_PROPERTY_INNER_TAGS:
            inner = self._fname(r).display
            params.append(TypeNode(inner))
            meta["inner_type"] = inner
        elif type_name in ("SetProperty", "OptionalProperty") and self.summary.file_version_ue4 >= ver.UE4_PROPERTY_TAG_SET_MAP_SUPPORT:
            inner = self._fname(r).display
            params.append(TypeNode(inner))
            meta["inner_type"] = inner
        elif type_name == "MapProperty" and self.summary.file_version_ue4 >= ver.UE4_PROPERTY_TAG_SET_MAP_SUPPORT:
            key = self._fname(r).display
            value = self._fname(r).display
            params.extend((TypeNode(key), TypeNode(value)))
            meta.update(inner_type=key, value_type=value)
        return TypeNode(type_name, params), meta

    def _raw_info(self, r: BinaryReader, start: int, end: int) -> dict[str, Any]:
        old = r.position
        r.seek(start)
        raw = r.read(end - start)
        r.seek(old)
        return {
            "physical_offset": start,
            "size": end - start,
            "sha256": hashlib.sha256(raw).hexdigest(),
            "preview_hex": raw[:48].hex(" "),
        }

    def _read_property_tag(self, r: BinaryReader, object_end: int) -> dict[str, Any] | None:
        header_start = r.position
        name = self._fname(r)
        if name.text == "None":
            if name.number != 0:
                raise FormatError(f"None terminator has nonzero FName number at 0x{header_start:x}")
            return None

        meta: dict[str, Any] = {}
        modern = self.summary.file_version_ue5 >= ver.UE5_PROPERTY_TAG_COMPLETE_TYPE_NAME
        if modern:
            type_node = self._read_type_node(r)
            size = r.i32()
            flags = r.u8()
            if flags & ~TAG_KNOWN_MASK:
                raise UnsupportedError(f"unknown FPropertyTag flags 0x{flags:02x}")
            array_index = r.i32() if flags & TAG_HAS_ARRAY_INDEX else 0
            property_guid = r.guid() if flags & TAG_HAS_PROPERTY_GUID else None
            if flags & TAG_HAS_EXTENSIONS:
                extensions = r.u8()
                meta["extensions"] = extensions
                if extensions & EXT_OVERRIDABLE_INFORMATION:
                    meta["override_operation"] = r.u8()
                    meta["experimental_overridable_logic"] = r.boolean32()
                if extensions & ~0x03:
                    meta["unknown_extension_bits"] = extensions & ~0x03
            if type_node.name == "BoolProperty":
                meta["bool"] = bool(flags & TAG_BOOL_TRUE)
        else:
            type_name = self._fname(r).display
            size = r.i32()
            array_index = r.i32()
            flags = 0
            type_node, type_meta = self._legacy_type(r, type_name)
            meta.update(type_meta)
            property_guid = None
            if self.summary.file_version_ue4 >= ver.UE4_PROPERTY_GUID_IN_PROPERTY_TAG:
                has_guid = r.flag8()
                property_guid = r.guid() if has_guid else None
            if self.summary.file_version_ue5 >= ver.UE5_PROPERTY_TAG_EXTENSION:
                extensions = r.u8()
                meta["extensions"] = extensions
                if extensions & EXT_OVERRIDABLE_INFORMATION:
                    meta["override_operation"] = r.u8()
                    meta["experimental_overridable_logic"] = r.boolean32()

        if size < 0:
            raise FormatError(f"negative property size {size} for {name.display}")
        value_start = r.position
        value_end = value_start + size
        if value_end > object_end:
            raise BoundsError(
                f"property {name.display} declares 0x{size:x} bytes at 0x{value_start:x}, "
                f"past object end 0x{object_end:x}"
            )
        raw = self._raw_info(r, value_start, value_end)
        status = "raw"
        value: Any = None
        error: str | None = None
        if flags & TAG_SKIPPED:
            status, error = "skipped", "serialization was explicitly skipped by UE"
        else:
            try:
                value = self._decode_value(r, type_node, value_end, meta)
                if r.position != value_end:
                    raise UnsupportedError(
                        f"decoder consumed {r.position - value_start} of {size} declared bytes"
                    )
                status = "decoded_native" if flags & TAG_BINARY_OR_NATIVE else "decoded"
            except (BoundsError, FormatError, UnsupportedError, UnicodeError) as exc:
                error = str(exc)
                if flags & TAG_BINARY_OR_NATIVE:
                    error = f"binary/native property not in verified decoder set: {error}"
                r.seek(value_end)
        r.seek(value_end)
        result: dict[str, Any] = {
            "name": name.display,
            "type": type_node.to_dict(),
            "array_index": array_index,
            "size": size,
            "flags": flags,
            "property_guid": property_guid,
            "header_physical_offset": header_start,
            "value": value,
            "decode_status": status,
            "raw": raw,
        }
        if meta:
            result["tag_meta"] = meta
        if error:
            result["decode_note"] = error
        return result

    def _read_nested_properties(self, r: BinaryReader, end: int) -> dict[str, Any]:
        nested = self._read_property_list(r, end)
        if not nested["terminated"]:
            raise UnsupportedError("struct fallback did not contain a None terminator")
        if r.position != end:
            raise UnsupportedError(f"struct fallback has {end - r.position} trailing bytes")
        return nested

    def _struct_name(self, node: TypeNode) -> str | None:
        return node.parameters[0].name if node.parameters else None

    def _decode_expression_input(self, r: BinaryReader) -> dict[str, Any]:
        """Decode the native FExpressionInput layout from MaterialShared.cpp.

        UE serializes the referenced expression, output index, input FName and
        five int32 component-mask fields.  The resulting 36-byte structure is
        the authoritative edge record for editor material graphs.
        """
        expression_index = r.i32()
        return {
            "kind": "expression_input",
            "expression": {
                "package_index": expression_index,
                "object": self.package.object_path(expression_index),
            },
            "output_index": r.i32(),
            "input_name": self._fname(r).display,
            "mask": {
                "enabled": r.i32(),
                "r": r.i32(),
                "g": r.i32(),
                "b": r.i32(),
                "a": r.i32(),
            },
        }

    def _decode_material_input(self, r: BinaryReader, name: str) -> dict[str, Any]:
        """Decode FMaterialInput descendants using their native serializers."""
        result = self._decode_expression_input(r)
        result["kind"] = "material_input"
        if name == "MaterialAttributesInput":
            return result

        result["use_constant"] = r.boolean32()
        if name == "ColorMaterialInput":
            b, g, red, a = r.u8(), r.u8(), r.u8(), r.u8()
            result["constant"] = {"r": red, "g": g, "b": b, "a": a}
        elif name == "ScalarMaterialInput":
            result["constant"] = _safe_float(r.f32())
        elif name == "VectorMaterialInput":
            result["constant"] = {k: _safe_float(r.f32()) for k in ("x", "y", "z")}
        elif name == "Vector2MaterialInput":
            result["constant"] = {k: _safe_float(r.f32()) for k in ("x", "y")}
        elif name in ("ShadingModelMaterialInput", "SubstrateMaterialInput"):
            result["constant"] = r.u32()
        else:  # pragma: no cover - guarded by the caller's exact name set
            raise UnsupportedError(f"material input struct {name} is not verified")
        return result

    def _read_legacy_array_struct_tag(self, r: BinaryReader, end: int) -> tuple[TypeNode, dict[str, Any]]:
        """Read the inner FPropertyTag written for pre-complete-type arrays.

        FArrayProperty::SerializeItem writes this tag after the element count
        for arrays of structs from VER_UE4_INNER_ARRAY_TAG_INFO until UE5's
        complete property type names.  Its declared size covers all elements.
        """
        start = r.position
        property_name = self._fname(r).display
        type_name = self._fname(r).display
        size = r.i32()
        array_index = r.i32()
        if type_name != "StructProperty":
            raise UnsupportedError(f"legacy array inner tag is {type_name}, expected StructProperty")
        struct_name = self._fname(r).display
        struct_guid = (
            r.guid() if self.summary.file_version_ue4 >= ver.UE4_STRUCT_GUID_IN_PROPERTY_TAG else None
        )
        property_guid = None
        if self.summary.file_version_ue4 >= ver.UE4_PROPERTY_GUID_IN_PROPERTY_TAG:
            has_guid = r.flag8()
            property_guid = r.guid() if has_guid else None
        extensions: dict[str, Any] | None = None
        if self.summary.file_version_ue5 >= ver.UE5_PROPERTY_TAG_EXTENSION:
            extension_flags = r.u8()
            if extension_flags & ~0x03:
                raise UnsupportedError(f"unknown legacy inner-tag extensions 0x{extension_flags:02x}")
            extensions = {"flags": extension_flags}
            if extension_flags & EXT_OVERRIDABLE_INFORMATION:
                extensions["override_operation"] = r.u8()
                extensions["experimental_overridable_logic"] = r.boolean32()
        if size < 0 or r.position + size > end:
            raise BoundsError(
                f"legacy array inner tag declares {size} bytes at 0x{r.position:x}, boundary 0x{end:x}"
            )
        metadata: dict[str, Any] = {
            "physical_offset": start,
            "header_size": r.position - start,
            "property_name": property_name,
            "type": type_name,
            "struct_name": struct_name,
            "struct_guid": struct_guid,
            "array_index": array_index,
            "declared_elements_size": size,
            "property_guid": property_guid,
        }
        if extensions is not None:
            metadata["extensions"] = extensions
        return TypeNode("StructProperty", [TypeNode(struct_name)]), metadata

    def _decode_struct(self, r: BinaryReader, node: TypeNode, end: int) -> Any:
        name = self._struct_name(node)
        size = end - r.position
        if name == "ExpressionInput" and size == 36:
            return self._decode_expression_input(r)
        material_input_sizes = {
            "MaterialAttributesInput": 36,
            "ColorMaterialInput": 44,
            "ScalarMaterialInput": 44,
            "VectorMaterialInput": 52,
            "Vector2MaterialInput": 48,
            "ShadingModelMaterialInput": 44,
            "SubstrateMaterialInput": 44,
        }
        if name in material_input_sizes and size == material_input_sizes[name]:
            return self._decode_material_input(r, name)
        if name in ("Guid", "FGuid") and size == 16:
            return r.guid()
        if name in ("LinearColor", "Float16Color") and size == 16:
            return {k: _safe_float(r.f32()) for k in ("r", "g", "b", "a")}
        if name == "Color" and size == 4:
            b, g, red, a = r.u8(), r.u8(), r.u8(), r.u8()
            return {"r": red, "g": g, "b": b, "a": a}
        if name in ("Vector2f", "FloatPoint") and size == 8:
            return {"x": _safe_float(r.f32()), "y": _safe_float(r.f32())}
        if name in ("Vector2D", "Vector2d") and size == 16:
            return {"x": _safe_float(r.f64()), "y": _safe_float(r.f64())}
        if name in ("Vector3f", "Vector", "Rotator") and size == 12:
            labels = ("pitch", "yaw", "roll") if name == "Rotator" else ("x", "y", "z")
            return {k: _safe_float(r.f32()) for k in labels}
        if name in ("Vector", "Vector3d", "Rotator") and size == 24:
            labels = ("pitch", "yaw", "roll") if name == "Rotator" else ("x", "y", "z")
            return {k: _safe_float(r.f64()) for k in labels}
        if name in ("Vector4f", "Quat4f", "Quat") and size == 16:
            return {k: _safe_float(r.f32()) for k in ("x", "y", "z", "w")}
        if name in ("Vector4", "Vector4d", "Quat", "Quat4d") and size == 32:
            return {k: _safe_float(r.f64()) for k in ("x", "y", "z", "w")}
        if name in ("IntPoint", "IntVector2") and size == 8:
            return {"x": r.i32(), "y": r.i32()}
        if name in ("IntVector", "IntVector3") and size == 12:
            return {"x": r.i32(), "y": r.i32(), "z": r.i32()}
        if name == "RichCurveKey" and size == 27:
            return {
                "interp_mode": r.u8(),
                "tangent_mode": r.u8(),
                "tangent_weight_mode": r.u8(),
                "time": _safe_float(r.f32()),
                "value": _safe_float(r.f32()),
                "arrive_tangent": _safe_float(r.f32()),
                "arrive_tangent_weight": _safe_float(r.f32()),
                "leave_tangent": _safe_float(r.f32()),
                "leave_tangent_weight": _safe_float(r.f32()),
            }
        if name in ("RichCurve", "RuntimeFloatCurve"):
            return self._read_nested_properties(r, end)

        # Many UE structs use tagged fallback serialization. Only accept it if
        # the complete nested stream, including None, consumes the declaration.
        start = r.position
        try:
            return self._read_nested_properties(r, end)
        except (BoundsError, FormatError, UnsupportedError):
            r.seek(start)
            raise UnsupportedError(f"native/unknown struct {name or '<unspecified>'} ({size} bytes)")

    def _decode_fixed_item(self, r: BinaryReader, node: TypeNode, end: int, remaining_items: int) -> Any:
        name = node.name
        if name == "BoolProperty":
            return r.flag8()
        if name in ("Int8Property",):
            return r.i8()
        if name in ("ByteProperty", "UInt8Property"):
            return r.u8()
        if name == "Int16Property":
            return r.i16()
        if name == "UInt16Property":
            return r.u16()
        if name == "IntProperty":
            return r.i32()
        if name == "UInt32Property":
            return r.u32()
        if name == "Int64Property":
            return r.i64()
        if name == "UInt64Property":
            return r.u64()
        if name == "FloatProperty":
            return _safe_float(r.f32())
        if name == "DoubleProperty":
            return _safe_float(r.f64())
        if name == "NameProperty":
            return self._fname(r).display
        if name in (
            "ObjectProperty", "ObjectPtrProperty", "ClassProperty", "ClassPtrProperty", "InterfaceProperty",
            "WeakObjectProperty", "LazyObjectProperty",
        ):
            index = r.i32()
            return {"package_index": index, "object": self.package.object_path(index)}
        if name in ("StrProperty", "StringProperty"):
            return r.fstring()
        if name == "StructProperty":
            struct_name = self._struct_name(node)
            fixed = {"Guid": 16, "FGuid": 16, "LinearColor": 16, "Color": 4, "Vector2f": 8,
                     "Vector2D": 16, "Vector2d": 16, "Vector3f": 12, "Vector3d": 24,
                     "RichCurveKey": 27, "IntPoint": 8, "IntVector": 12,
                     "ExpressionInput": 36, "MaterialAttributesInput": 36,
                     "ColorMaterialInput": 44, "ScalarMaterialInput": 44,
                     "VectorMaterialInput": 52, "Vector2MaterialInput": 48,
                     "ShadingModelMaterialInput": 44, "SubstrateMaterialInput": 44}
            if struct_name not in fixed:
                raise UnsupportedError(f"array element struct {struct_name} has no verified boundary")
            item_end = r.position + fixed[struct_name]
            if item_end > end:
                raise BoundsError(f"array struct {struct_name} leaves property boundary")
            return self._decode_struct(r, node, item_end)
        raise UnsupportedError(f"array item type {node.display} is not verified")

    def _decode_container_item(self, r: BinaryReader, node: TypeNode, end: int, label: str) -> Any:
        if node.name == "StructProperty":
            struct_name = self._struct_name(node)
            if struct_name in ("NiagaraVariable", "NiagaraVariableBase", "NiagaraVariableWithOffset"):
                if self.package.custom_version(GUID_NIAGARA) < NIAGARA_VARIABLES_USE_TYPEDEF_REGISTRY:
                    raise UnsupportedError(
                        f"{struct_name} predates VariablesUseTypeDefRegistry and needs its legacy decoder"
                    )
                variable_name = self._fname(r).display
                type_definition = self._read_property_list(r, end)
                if not type_definition["terminated"]:
                    raise UnsupportedError(f"{struct_name} type definition lacks a None terminator")
                result: dict[str, Any] = {
                    "name": variable_name,
                    "type_definition": type_definition,
                }
                if struct_name == "NiagaraVariable":
                    count = r.count("Niagara variable data", maximum=1 << 30)
                    data = r.read(count)
                    result["data_size"] = count
                    result["data_hex"] = data.hex()
                    result["data_sha256"] = hashlib.sha256(data).hexdigest()
                elif struct_name == "NiagaraVariableWithOffset":
                    result["offset"] = r.i32()
                return result
            if struct_name in {
                "Guid", "FGuid", "LinearColor", "Color", "Vector2f", "Vector2D", "Vector2d",
                "Vector3f", "Vector3d", "RichCurveKey", "IntPoint", "IntVector",
                "ExpressionInput", "MaterialAttributesInput", "ColorMaterialInput",
                "ScalarMaterialInput", "VectorMaterialInput", "Vector2MaterialInput",
                "ShadingModelMaterialInput", "SubstrateMaterialInput",
            }:
                return self._decode_fixed_item(r, node, end, 1)
            item = self._read_property_list(r, end)
            if not item["terminated"]:
                raise UnsupportedError(f"{label} struct value lacks a None terminator")
            return item
        return self._decode_fixed_item(r, node, end, 1)

    def _decode_value(self, r: BinaryReader, node: TypeNode, end: int, meta: dict[str, Any]) -> Any:
        name = node.name
        size = end - r.position
        if name == "BoolProperty":
            if size != 0:
                raise UnsupportedError(f"BoolProperty unexpectedly has {size} value bytes")
            return bool(meta.get("bool", False))
        scalar_sizes = {
            "Int8Property": (1, r.i8), "ByteProperty": (1, r.u8), "UInt8Property": (1, r.u8),
            "Int16Property": (2, r.i16), "UInt16Property": (2, r.u16),
            "IntProperty": (4, r.i32), "UInt32Property": (4, r.u32),
            "Int64Property": (8, r.i64), "UInt64Property": (8, r.u64),
        }
        if name in scalar_sizes:
            expected, fn = scalar_sizes[name]
            if size == 8 and name == "ByteProperty" and node.parameters:
                return self._fname(r).display
            if size != expected:
                raise UnsupportedError(f"{name} size {size}, expected {expected}")
            return fn()
        if name == "FloatProperty":
            if size != 4:
                raise UnsupportedError(f"FloatProperty size {size}, expected 4")
            return _safe_float(r.f32())
        if name == "DoubleProperty":
            if size != 8:
                raise UnsupportedError(f"DoubleProperty size {size}, expected 8")
            return _safe_float(r.f64())
        if name == "NameProperty":
            if size != 8:
                raise UnsupportedError(f"NameProperty size {size}, expected 8")
            return self._fname(r).display
        if name in ("StrProperty", "StringProperty"):
            return r.fstring()
        if name in (
            "ObjectProperty", "ObjectPtrProperty", "ClassProperty", "ClassPtrProperty", "InterfaceProperty",
            "WeakObjectProperty", "LazyObjectProperty",
        ):
            if size != 4:
                raise UnsupportedError(f"{name} size {size}, expected FPackageIndex (4)")
            index = r.i32()
            return {"package_index": index, "object": self.package.object_path(index)}
        if name in ("SoftObjectProperty", "SoftClassProperty"):
            soft_paths = getattr(self.package, "soft_object_paths", [])
            if soft_paths:
                if size != 4:
                    raise UnsupportedError(
                        f"{name} uses a header soft-path table but value size is {size}, expected 4"
                    )
                index = r.i32()
                if not 0 <= index < len(soft_paths):
                    raise FormatError(
                        f"{name} index {index} outside header soft-path table 0..{len(soft_paths) - 1}"
                    )
                path = soft_paths[index]
                return {
                    "soft_object_path_index": index,
                    "package": path["package"],
                    "asset": path["asset"],
                    "sub_path": path["sub_path"],
                    "object_path": path["object_path"],
                    "header_provenance": path["provenance"],
                }
            if self.summary.file_version_ue5 >= ver.UE5_FSOFTOBJECTPATH_REMOVE_ASSET_PATH_FNAMES:
                package_name = self._fname(r).display
                asset_name = self._fname(r).display
                sub_path = r.fstring()
                return {"package": package_name, "asset": asset_name, "sub_path": sub_path}
            asset_path = self._fname(r).display
            return {"asset_path": asset_path, "sub_path": r.fstring()}
        if name == "StructProperty":
            return self._decode_struct(r, node, end)
        if name == "ArrayProperty":
            count = r.i32()
            if not 0 <= count <= 10_000_000:
                raise FormatError(f"invalid array count {count}")
            if not node.parameters:
                raise UnsupportedError("ArrayProperty lacks its inner type")
            inner = node.parameters[0]
            inner_tag: dict[str, Any] | None = None
            if (
                inner.name == "StructProperty"
                and self._struct_name(inner) is None
                and self.summary.file_version_ue5 < ver.UE5_PROPERTY_TAG_COMPLETE_TYPE_NAME
                and self.summary.file_version_ue4 >= ver.UE4_ARRAY_PROPERTY_INNER_TAGS
            ):
                inner, inner_tag = self._read_legacy_array_struct_tag(r, end)
            values = []
            # Enum-backed TArray<TEnumAsByte<...>> values are serialized as
            # FName even though the legacy tag only records ByteProperty.
            if inner.name == "ByteProperty" and end - r.position == count * 8:
                result = {"count": count, "items": [self._fname(r).display for _ in range(count)]}
                if inner_tag is not None:
                    result["inner_tag"] = inner_tag
                return result
            for i in range(count):
                if inner.name == "StructProperty":
                    values.append(self._decode_container_item(r, inner, end, f"array struct element {i}"))
                else:
                    values.append(self._decode_fixed_item(r, inner, end, count - i))
            result = {"count": count, "items": values}
            if inner_tag is not None:
                consumed = r.position - (inner_tag["physical_offset"] + inner_tag["header_size"])
                if consumed != inner_tag["declared_elements_size"]:
                    raise UnsupportedError(
                        f"legacy array elements consumed {consumed} of "
                        f"{inner_tag['declared_elements_size']} declared bytes"
                    )
                result["inner_tag"] = inner_tag
            return result
        if name == "SetProperty":
            if not node.parameters:
                raise UnsupportedError("SetProperty lacks its element type")
            element = node.parameters[0]
            removed_count = r.i32()
            if not 0 <= removed_count <= 10_000_000:
                raise FormatError(f"invalid SetProperty removed count {removed_count}")
            removed = [
                self._decode_container_item(r, element, end, f"set removed element {i}")
                for i in range(removed_count)
            ]
            count = r.i32()
            if not 0 <= count <= 10_000_000:
                raise FormatError(f"invalid SetProperty element count {count}")
            items = [
                self._decode_container_item(r, element, end, f"set element {i}")
                for i in range(count)
            ]
            return {"removed_count": removed_count, "removed": removed, "count": count, "items": items}
        if name == "MapProperty":
            if len(node.parameters) != 2:
                raise UnsupportedError("MapProperty lacks key/value types")
            key_node, value_node = node.parameters
            removed_count = r.i32()
            replace = removed_count == -1
            if not replace and not 0 <= removed_count <= 10_000_000:
                raise FormatError(f"invalid MapProperty removed count {removed_count}")
            removed = [] if replace else [
                self._decode_container_item(r, key_node, end, f"map removed key {i}")
                for i in range(removed_count)
            ]
            count = r.i32()
            if not 0 <= count <= 10_000_000:
                raise FormatError(f"invalid MapProperty entry count {count}")
            entries = []
            for i in range(count):
                entries.append({
                    "key": self._decode_container_item(r, key_node, end, f"map key {i}"),
                    "value": self._decode_container_item(r, value_node, end, f"map value {i}"),
                })
            return {
                "replace": replace, "removed_count": 0 if replace else removed_count,
                "removed": removed, "count": count, "entries": entries,
            }
        if name in ("EnumProperty",):
            if size == 1:
                return r.u8()
            if size == 2:
                return r.u16()
            if size == 4:
                return r.u32()
            if size == 8:
                return self._fname(r).display
            raise UnsupportedError(f"EnumProperty size {size} is not verified")
        if name == "DelegateProperty" and size == 12:
            index = r.i32()
            return {"object": self.package.object_path(index), "function": self._fname(r).display}
        if name == "MulticastDelegateProperty":
            count = r.i32()
            if count < 0 or 4 + count * 12 != size:
                raise UnsupportedError("multicast delegate layout/size mismatch")
            return {
                "count": count,
                "bindings": [
                    {"object": self.package.object_path(r.i32()), "function": self._fname(r).display}
                    for _ in range(count)
                ],
            }
        raise UnsupportedError(f"property type {node.display} has no verified decoder")

    def _read_property_list(self, r: BinaryReader, end: int) -> dict[str, Any]:
        properties: list[dict[str, Any]] = []
        terminated = False
        while r.position < end:
            before = r.position
            tag = self._read_property_tag(r, end)
            if tag is None:
                terminated = True
                break
            properties.append(tag)
            if r.position <= before:
                raise FormatError("property parser made no forward progress")
        return {"properties": properties, "terminated": terminated}

    def parse_export(self, export_index: int) -> dict[str, Any]:
        if not 1 <= export_index <= len(self.package.exports):
            raise FormatError(f"export index {export_index} outside 1..{len(self.package.exports)}")
        export = self.package.exports[export_index - 1]
        base = {
            "export_index": export_index,
            "object": self.package.object_path(export_index),
            "class": export.class_name,
            "logical_offset": export.serial_offset,
            "size": export.serial_size,
            "availability": export.payload_availability,
        }
        if export.payload_availability not in ("available", "empty"):
            return {**base, "parse_status": "unavailable", "properties": []}
        if export.serial_size == 0:
            return {**base, "parse_status": "decoded", "properties": [], "trailing_native": None}

        source = Path(export.payload_source or self.package.path)
        physical = int(export.payload_physical_offset or 0)
        with BinaryReader(source) as r:
            r.seek(physical)
            end = physical + export.serial_size
            with r.bounded(end):
                object_prefix: dict[str, Any] | None = None
                if self.summary.file_version_ue5 >= ver.UE5_PROPERTY_TAG_EXTENSION:
                    prefix_offset = r.position
                    operation = r.u8()
                    if operation > 7:
                        return {
                            **base,
                            "parse_status": "native_or_unknown",
                            "properties": [],
                            "raw": self._raw_info(r, physical, end),
                            "decode_note": f"invalid UObject overridable prefix {operation}",
                        }
                    object_prefix = {"physical_offset": prefix_offset, "override_operation": operation}
                try:
                    parsed = self._read_property_list(r, end)
                except (BoundsError, FormatError, UnsupportedError) as exc:
                    return {
                        **base,
                        "parse_status": "native_or_unknown",
                        "object_prefix": object_prefix,
                        "properties": [],
                        "raw": self._raw_info(r, physical, end),
                        "decode_note": str(exc),
                    }
                object_guid = None
                object_guid_marker = None
                # UObject::Serialize writes a 32-bit has-guid marker after
                # tagged properties for every non-CDO UE4/UE5 object.
                if parsed["terminated"] and not (export.object_flags & 0x10) and r.position + 4 <= end:
                    marker_at = r.position
                    try:
                        has_guid = r.boolean32()
                        object_guid_marker = {"physical_offset": marker_at, "has_guid": has_guid}
                        if has_guid:
                            if r.position + 16 > end:
                                raise BoundsError("UObject GUID crosses export boundary")
                            object_guid = r.guid()
                    except (BoundsError, FormatError):
                        r.seek(marker_at)
                        object_guid_marker = None
                trailing = self._raw_info(r, r.position, end) if r.position < end else None
                status = "decoded_properties" if parsed["terminated"] else "unterminated_or_native"
                return {
                    **base,
                    "parse_status": status,
                    "object_prefix": object_prefix,
                    "properties": parsed["properties"],
                    "property_terminator_found": parsed["terminated"],
                    "object_guid_marker": object_guid_marker,
                    "object_guid": object_guid,
                    "trailing_native": trailing,
                }
