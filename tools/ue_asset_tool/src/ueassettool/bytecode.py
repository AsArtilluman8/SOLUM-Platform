from __future__ import annotations

import hashlib
import struct
from pathlib import Path
from typing import Any

from .binary import BinaryReader
from .errors import BoundsError, FormatError, UnsupportedError
from .properties import PropertyParser


GUID_CORE = "375ec13c-06e448fb-b50084f0-262a717e"
GUID_FRAMEWORK = "cffc743f-43b04480-939114df-171d2073"
GUID_RELEASE = "9c54d522-a8264fbe-94210746-61b482d0"
GUID_FORTNITE_MAIN = "601d1886-ac644f84-aa16d3de-0deac7d6"

# The supplied UE 5.5 packages use FProperties and the post-UField child
# array. Older encodings are rejected instead of being inferred from bytes.
CORE_FPROPERTIES = 4
FRAMEWORK_REMOVE_UFIELD_NEXT = 29

FUNC_NET = 0x00000040
FUNCTION_FLAG_NAMES = {
    0x00000001: "Final", 0x00000002: "RequiredAPI", 0x00000004: "BlueprintAuthorityOnly",
    0x00000008: "BlueprintCosmetic", 0x00000040: "Net", 0x00000080: "NetReliable",
    0x00000100: "NetRequest", 0x00000200: "Exec", 0x00000400: "Native",
    0x00000800: "Event", 0x00001000: "NetResponse", 0x00002000: "Static",
    0x00004000: "NetMulticast", 0x00008000: "UbergraphFunction",
    0x00010000: "MulticastDelegate", 0x00020000: "Public", 0x00040000: "Private",
    0x00080000: "Protected", 0x00100000: "Delegate", 0x00200000: "NetServer",
    0x00400000: "HasOutParms", 0x00800000: "HasDefaults", 0x01000000: "NetClient",
    0x02000000: "DLLImport", 0x04000000: "BlueprintCallable",
    0x08000000: "BlueprintEvent", 0x10000000: "BlueprintPure", 0x20000000: "EditorOnly",
    0x40000000: "Const", 0x80000000: "NetValidate",
}
PKG_COOKED = 0x00000200
VER_UE4_SERIALIZE_BLUEPRINT_EVENTGRAPH_FASTCALLS_IN_UFUNCTION = 451
VER_UE4_CHANGE_SETARRAY_BYTECODE = 421
UE5_LARGE_WORLD_COORDINATES = 1004


EXPR_NAMES: dict[int, str] = {
    0x00: "EX_LocalVariable", 0x01: "EX_InstanceVariable", 0x02: "EX_DefaultVariable",
    0x04: "EX_Return", 0x06: "EX_Jump", 0x07: "EX_JumpIfNot", 0x09: "EX_Assert",
    0x0B: "EX_Nothing", 0x0C: "EX_NothingInt32", 0x0F: "EX_Let",
    0x11: "EX_BitFieldConst", 0x12: "EX_ClassContext", 0x13: "EX_MetaCast",
    0x14: "EX_LetBool", 0x15: "EX_EndParmValue", 0x16: "EX_EndFunctionParms",
    0x17: "EX_Self", 0x18: "EX_Skip", 0x19: "EX_Context",
    0x1A: "EX_Context_FailSilent", 0x1B: "EX_VirtualFunction", 0x1C: "EX_FinalFunction",
    0x1D: "EX_IntConst", 0x1E: "EX_FloatConst", 0x1F: "EX_StringConst",
    0x20: "EX_ObjectConst", 0x21: "EX_NameConst", 0x22: "EX_RotationConst",
    0x23: "EX_VectorConst", 0x24: "EX_ByteConst", 0x25: "EX_IntZero",
    0x26: "EX_IntOne", 0x27: "EX_True", 0x28: "EX_False", 0x29: "EX_TextConst",
    0x2A: "EX_NoObject", 0x2B: "EX_TransformConst", 0x2C: "EX_IntConstByte",
    0x2D: "EX_NoInterface", 0x2E: "EX_DynamicCast", 0x2F: "EX_StructConst",
    0x30: "EX_EndStructConst", 0x31: "EX_SetArray", 0x32: "EX_EndArray",
    0x33: "EX_PropertyConst", 0x34: "EX_UnicodeStringConst", 0x35: "EX_Int64Const",
    0x36: "EX_UInt64Const", 0x37: "EX_DoubleConst", 0x38: "EX_Cast",
    0x39: "EX_SetSet", 0x3A: "EX_EndSet", 0x3B: "EX_SetMap", 0x3C: "EX_EndMap",
    0x3D: "EX_SetConst", 0x3E: "EX_EndSetConst", 0x3F: "EX_MapConst",
    0x40: "EX_EndMapConst", 0x41: "EX_Vector3fConst", 0x42: "EX_StructMemberContext",
    0x43: "EX_LetMulticastDelegate", 0x44: "EX_LetDelegate",
    0x45: "EX_LocalVirtualFunction", 0x46: "EX_LocalFinalFunction",
    0x48: "EX_LocalOutVariable", 0x4A: "EX_DeprecatedOp4A", 0x4B: "EX_InstanceDelegate",
    0x4C: "EX_PushExecutionFlow", 0x4D: "EX_PopExecutionFlow", 0x4E: "EX_ComputedJump",
    0x4F: "EX_PopExecutionFlowIfNot", 0x50: "EX_Breakpoint", 0x51: "EX_InterfaceContext",
    0x52: "EX_ObjToInterfaceCast", 0x53: "EX_EndOfScript", 0x54: "EX_CrossInterfaceCast",
    0x55: "EX_InterfaceToObjCast", 0x5A: "EX_WireTracepoint", 0x5B: "EX_SkipOffsetConst",
    0x5C: "EX_AddMulticastDelegate", 0x5D: "EX_ClearMulticastDelegate", 0x5E: "EX_Tracepoint",
    0x5F: "EX_LetObj", 0x60: "EX_LetWeakObjPtr", 0x61: "EX_BindDelegate",
    0x62: "EX_RemoveMulticastDelegate", 0x63: "EX_CallMulticastDelegate",
    0x64: "EX_LetValueOnPersistentFrame", 0x65: "EX_ArrayConst", 0x66: "EX_EndArrayConst",
    0x67: "EX_SoftObjectConst", 0x68: "EX_CallMath", 0x69: "EX_SwitchValue",
    0x6A: "EX_InstrumentationEvent", 0x6B: "EX_ArrayGetByRef",
    0x6C: "EX_ClassSparseDataVariable", 0x6D: "EX_FieldPathConst",
    0x70: "EX_AutoRtfmTransact", 0x71: "EX_AutoRtfmStopTransact",
    0x72: "EX_AutoRtfmAbortIfNot",
}

TERMINATOR_TOKENS = {
    0x15, 0x16, 0x30, 0x32, 0x3A, 0x3C, 0x3E, 0x40, 0x53, 0x66, 0x71,
}

PLAIN_PROPERTY_TYPES = {
    "Int8Property", "Int16Property", "IntProperty", "Int64Property",
    "UInt16Property", "UInt32Property", "UInt64Property", "FloatProperty", "DoubleProperty",
    "NameProperty", "StrProperty", "StringProperty", "TextProperty",
}
OBJECT_PROPERTY_TYPES = {
    "ObjectProperty", "ObjectPtrProperty", "WeakObjectProperty", "LazyObjectProperty",
    "SoftObjectProperty", "AssetObjectProperty",
}
CLASS_PROPERTY_TYPES = {"ClassProperty", "ClassPtrProperty", "SoftClassProperty", "AssetClassProperty"}
DELEGATE_PROPERTY_TYPES = {
    "DelegateProperty", "MulticastDelegateProperty", "MulticastInlineDelegateProperty",
    "MulticastSparseDelegateProperty",
}


class StructScriptDecoder:
    """UE UStruct/UFunction serializer and Kismet bytecode decoder.

    This follows UE 5.5's UStruct::Serialize, FProperty serializers and
    ScriptSerialization.h. A function is returned as decoded only when the
    serialized field list, script storage, in-memory bytecode size and
    UFunction footer all consume their declared boundaries exactly.
    """

    def __init__(self, package: Any):
        self.package = package
        self.properties = PropertyParser(package)
        self.core = package.custom_version(GUID_CORE)
        self.framework = package.custom_version(GUID_FRAMEWORK)
        self.release = package.custom_version(GUID_RELEASE)
        self.fortnite = package.custom_version(GUID_FORTNITE_MAIN)
        self._expression_count = 0
        self._bytecode_size = 0
        self._storage_end = 0
        self._code = 0
        self._jump_offsets: list[dict[str, int]] = []

    def _fname(self, r: BinaryReader) -> dict[str, Any]:
        value = self.package.fname(r.fname_raw())
        return {"index": value.index, "number": value.number, "value": value.display}

    def _package_index(self, r: BinaryReader) -> dict[str, Any]:
        at = r.position
        index = r.i32()
        if index < -len(self.package.imports) or index > len(self.package.exports):
            raise FormatError(f"FPackageIndex {index} at 0x{at:x} is outside package maps")
        return {"index": index, "object": self.package.object_path(index)}

    def _metadata(self, r: BinaryReader) -> dict[str, str]:
        count = r.count("FField metadata", maximum=1_000_000)
        result: dict[str, str] = {}
        for _ in range(count):
            key = self._fname(r)["value"]
            if key in result:
                raise FormatError(f"duplicate FField metadata key {key!r}")
            result[key] = r.fstring()
        return result

    def _property_base(self, r: BinaryReader, type_name: str) -> dict[str, Any]:
        start = r.position
        result: dict[str, Any] = {
            "type": type_name,
            "name": self._fname(r)["value"],
            "field_flags": r.u32(),
        }
        if not (self.package.summary.package_flags & PKG_COOKED):
            has_metadata = r.boolean32()
            result["metadata"] = self._metadata(r) if has_metadata else {}
        array_dim = r.i32()
        element_size = r.i32()
        if not 0 <= array_dim <= 1_000_000:
            raise FormatError(f"invalid {type_name} ArrayDim {array_dim} at 0x{start:x}")
        if not 0 <= element_size <= 1 << 30:
            raise FormatError(f"invalid {type_name} ElementSize {element_size} at 0x{start:x}")
        result.update(
            array_dim=array_dim,
            element_size=element_size,
            property_flags=r.u64(),
            rep_index=r.u16(),
            rep_notify=self._fname(r)["value"],
            blueprint_replication_condition=r.u8(),
        )
        return result

    def _single_field(self, r: BinaryReader) -> dict[str, Any] | None:
        type_ref = self._fname(r)
        if type_ref["value"] == "None":
            return None
        return self._serialized_property(r, type_ref["value"])

    def _serialized_property(self, r: BinaryReader, type_name: str) -> dict[str, Any]:
        start = r.position
        result = self._property_base(r, type_name)
        if type_name in PLAIN_PROPERTY_TYPES:
            pass
        elif type_name == "BoolProperty":
            result["bool_layout"] = {
                "field_size": r.u8(), "byte_offset": r.u8(), "byte_mask": r.u8(),
                "field_mask": r.u8(), "bool_size": r.u8(), "native_bool": r.u8(),
            }
        elif type_name == "ByteProperty":
            result["enum"] = self._package_index(r)
        elif type_name == "EnumProperty":
            result["enum"] = self._package_index(r)
            result["underlying"] = self._single_field(r)
            if result["underlying"] is None:
                raise FormatError("EnumProperty has null underlying field")
        elif type_name == "ArrayProperty":
            result["inner"] = self._single_field(r)
            if result["inner"] is None:
                raise FormatError("ArrayProperty has null inner field")
        elif type_name == "SetProperty":
            result["element"] = self._single_field(r)
            if result["element"] is None:
                raise FormatError("SetProperty has null element field")
        elif type_name == "MapProperty":
            result["key"] = self._single_field(r)
            result["value"] = self._single_field(r)
            if result["key"] is None or result["value"] is None:
                raise FormatError("MapProperty has null key/value field")
        elif type_name == "OptionalProperty":
            result["value"] = self._single_field(r)
            if result["value"] is None:
                raise FormatError("OptionalProperty has null value field")
        elif type_name in OBJECT_PROPERTY_TYPES:
            result["property_class"] = self._package_index(r)
        elif type_name in CLASS_PROPERTY_TYPES:
            result["property_class"] = self._package_index(r)
            result["meta_class"] = self._package_index(r)
        elif type_name == "InterfaceProperty":
            result["interface_class"] = self._package_index(r)
        elif type_name == "StructProperty":
            result["struct"] = self._package_index(r)
        elif type_name in DELEGATE_PROPERTY_TYPES:
            result["signature_function"] = self._package_index(r)
        else:
            raise UnsupportedError(f"serialized FProperty class {type_name} is not implemented")
        result["serialized_size"] = r.position - start
        return result

    def _struct_prefix(self, r: BinaryReader) -> dict[str, Any]:
        start = r.position
        result: dict[str, Any] = {"super_struct": self._package_index(r)}
        if self.framework < FRAMEWORK_REMOVE_UFIELD_NEXT:
            result["first_child"] = self._package_index(r)
            result["children"] = []
        else:
            count = r.count("UStruct child", maximum=1_000_000)
            result["children"] = [self._package_index(r) for _ in range(count)]
        if self.core < CORE_FPROPERTIES:
            raise UnsupportedError(
                f"Core custom version {self.core} predates verified FProperties serialization"
            )
        count = r.count("UStruct FProperty", maximum=1_000_000)
        fields: list[dict[str, Any]] = []
        for _ in range(count):
            type_name = self._fname(r)["value"]
            if type_name == "None":
                raise FormatError("null property class in UStruct field array")
            fields.append(self._serialized_property(r, type_name))
        result["properties"] = fields
        result["serialized_size"] = r.position - start
        return result

    def _storage_read(self, r: BinaryReader, size: int) -> bytes:
        if r.position + size > self._storage_end:
            raise BoundsError(
                f"bytecode storage read of {size} bytes at 0x{r.position:x} crosses 0x{self._storage_end:x}"
            )
        return r.read(size)

    def _xfer(self, r: BinaryReader, fmt: str) -> int | float:
        size = struct.calcsize("<" + fmt)
        raw = self._storage_read(r, size)
        self._code += size
        if self._code > self._bytecode_size:
            raise BoundsError(f"in-memory bytecode offset {self._code} exceeds {self._bytecode_size}")
        return struct.unpack("<" + fmt, raw)[0]

    def _script_name(self, r: BinaryReader) -> dict[str, Any]:
        raw = self._storage_read(r, 8)
        index, number = struct.unpack("<ii", raw)
        # UE 5.5 editor packages are produced with case-preserving FNames;
        # FScriptName is three uint32 values in the loaded VM buffer while the
        # persistent FName remains the usual two-int32 package reference.
        self._code += 12
        return self.package.fname((index, number)).__dict__ | {
            "value": self.package.fname((index, number)).display
        }

    def _object_pointer(self, r: BinaryReader) -> dict[str, Any]:
        # Persistent package archives store UObject pointers as FPackageIndex;
        # the loaded VM buffer stores a 64-bit ScriptPointerType.
        raw = self._storage_read(r, 4)
        index = struct.unpack("<i", raw)[0]
        if index < -len(self.package.imports) or index > len(self.package.exports):
            raise FormatError(f"bytecode object FPackageIndex {index} is outside package maps")
        self._code += 8
        return {"index": index, "object": self.package.object_path(index)}

    def _field_pointer(self, r: BinaryReader) -> dict[str, Any]:
        # FPropertyProxyArchive serializes every FField* as TFieldPath<FField>:
        # TArray<FName> Path followed by the owner UStruct in current packages.
        raw_count = self._storage_read(r, 4)
        count = struct.unpack("<i", raw_count)[0]
        if not 0 <= count <= 1_000_000:
            raise FormatError(f"invalid bytecode FFieldPath component count {count}")
        path = []
        for _ in range(count):
            raw = self._storage_read(r, 8)
            path.append(self.package.fname(struct.unpack("<ii", raw)).display)
        # UE 5.5 always has FFieldPathOwnerSerialization. The version enum
        # value was not included in the supplied source subset, so older
        # packages are explicitly outside this decoder's verified range.
        if self.release < 44 and self.fortnite < 170:
            raise UnsupportedError(
                "FFieldPath owner layout is not verified for this package custom-version set"
            )
        owner_raw = self._storage_read(r, 4)
        owner_index = struct.unpack("<i", owner_raw)[0]
        if owner_index < -len(self.package.imports) or owner_index > len(self.package.exports):
            raise FormatError(f"FFieldPath owner FPackageIndex {owner_index} is outside package maps")
        self._code += 8
        return {"path": path, "owner_index": owner_index, "owner": self.package.object_path(owner_index)}

    def _ansi_string(self, r: BinaryReader) -> str:
        raw = bytearray()
        while True:
            value = int(self._xfer(r, "B"))
            if value == 0:
                break
            raw.append(value)
            if len(raw) > 16_777_216:
                raise FormatError("bytecode ANSI string exceeds limit")
        try:
            return bytes(raw).decode("utf-8")
        except UnicodeDecodeError:
            return bytes(raw).decode("latin-1")

    def _unicode_string(self, r: BinaryReader) -> str:
        raw = bytearray()
        units = 0
        while True:
            value = int(self._xfer(r, "H"))
            if value == 0:
                break
            raw += struct.pack("<H", value)
            units += 1
            if units > 16_777_216:
                raise FormatError("bytecode UTF-16 string exceeds limit")
        try:
            return bytes(raw).decode("utf-16-le")
        except UnicodeDecodeError as exc:
            raise FormatError(f"invalid bytecode UTF-16 string: {exc}") from exc

    def _jump(self, r: BinaryReader, label: str) -> int:
        value = int(self._xfer(r, "I"))
        self._jump_offsets.append({"kind": label, "from": self._code - 4, "target": value})
        return value

    def _expr_list(self, r: BinaryReader, terminator: int, depth: int) -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = []
        while True:
            expr = self._expr(r, depth + 1)
            result.append(expr)
            if expr["opcode"] == terminator:
                return result

    def _expr(self, r: BinaryReader, depth: int = 0) -> dict[str, Any]:
        if depth > 512:
            raise FormatError("bytecode expression recursion exceeds 512")
        self._expression_count += 1
        if self._expression_count > 10_000_000:
            raise FormatError("bytecode expression count exceeds limit")
        storage_start, code_start = r.position, self._code
        opcode = int(self._xfer(r, "B"))
        if opcode not in EXPR_NAMES:
            raise UnsupportedError(
                f"unknown Kismet opcode 0x{opcode:02x} at storage 0x{storage_start:x}, bytecode {code_start}"
            )
        node: dict[str, Any] = {
            "opcode": opcode, "token": EXPR_NAMES[opcode],
            "storage_offset": storage_start, "bytecode_offset": code_start,
        }

        if opcode == 0x38:  # Cast
            node["cast_token"] = int(self._xfer(r, "B"))
            node["expression"] = self._expr(r, depth + 1)
        elif opcode in (0x52, 0x54, 0x55, 0x13, 0x2E):
            node["class"] = self._object_pointer(r)
            node["expression"] = self._expr(r, depth + 1)
        elif opcode == 0x0F:  # Let falls through to two expressions
            node["property"] = self._field_pointer(r)
            node["variable"] = self._expr(r, depth + 1)
            node["assignment"] = self._expr(r, depth + 1)
        elif opcode in (0x5F, 0x60, 0x14, 0x44, 0x43):
            node["variable"] = self._expr(r, depth + 1)
            node["assignment"] = self._expr(r, depth + 1)
        elif opcode == 0x64:
            node["property"] = self._field_pointer(r)
            node["assignment"] = self._expr(r, depth + 1)
        elif opcode == 0x42:
            node["member"] = self._field_pointer(r)
            node["struct"] = self._expr(r, depth + 1)
        elif opcode in (0x06, 0x4C, 0x5B):
            node["target"] = self._jump(r, node["token"])
        elif opcode == 0x4E:
            node["target_expression"] = self._expr(r, depth + 1)
        elif opcode in (0x00, 0x01, 0x02, 0x48, 0x6C, 0x33):
            node["property"] = self._field_pointer(r)
        elif opcode in (0x51, 0x04, 0x4F, 0x70, 0x72):
            if opcode == 0x70:
                node["transaction_id"] = int(self._xfer(r, "i"))
                node["target"] = self._jump(r, node["token"])
                node["body"] = self._expr_list(r, 0x71, depth + 1)
            else:
                node["expression"] = self._expr(r, depth + 1)
        elif opcode == 0x0C:
            node["value"] = int(self._xfer(r, "i"))
        elif opcode in (
            0x0B, 0x53, 0x16, 0x30, 0x32, 0x66, 0x3A, 0x3C, 0x3E, 0x40,
            0x25, 0x26, 0x27, 0x28, 0x2A, 0x2D, 0x17, 0x15, 0x4D, 0x4A,
            0x5A, 0x5E, 0x50,
        ):
            pass
        elif opcode == 0x6A:
            raise UnsupportedError(
                "EX_InstrumentationEvent carries non-persistent VM-only bytes; exact reconstruction is unavailable"
            )
        elif opcode in (0x1C, 0x46, 0x68, 0x63):
            node["function"] = self._object_pointer(r)
            node["parameters"] = self._expr_list(r, 0x16, depth + 1)
        elif opcode in (0x1B, 0x45):
            node["function_name"] = self._script_name(r)
            node["parameters"] = self._expr_list(r, 0x16, depth + 1)
        elif opcode in (0x12, 0x19, 0x1A):
            node["object"] = self._expr(r, depth + 1)
            node["null_skip"] = self._jump(r, node["token"])
            node["rvalue_property"] = self._field_pointer(r)
            node["context"] = self._expr(r, depth + 1)
        elif opcode in (0x5C, 0x62):
            node["multicast_delegate"] = self._expr(r, depth + 1)
            node["delegate"] = self._expr(r, depth + 1)
        elif opcode == 0x5D:
            node["multicast_delegate"] = self._expr(r, depth + 1)
        elif opcode == 0x1D:
            node["value"] = int(self._xfer(r, "i"))
        elif opcode == 0x35:
            node["value"] = int(self._xfer(r, "q"))
        elif opcode == 0x36:
            node["value"] = int(self._xfer(r, "Q"))
        elif opcode == 0x1E:
            node["value"] = float(self._xfer(r, "f"))
        elif opcode == 0x37:
            node["value"] = float(self._xfer(r, "d"))
        elif opcode == 0x1F:
            node["value"] = self._ansi_string(r)
        elif opcode == 0x34:
            node["value"] = self._unicode_string(r)
        elif opcode == 0x29:
            literal_type = int(self._xfer(r, "B"))
            node["literal_type"] = literal_type
            counts = {0: 0, 1: 3, 2: 1, 3: 1}
            if literal_type in counts:
                node["parts"] = [self._expr(r, depth + 1) for _ in range(counts[literal_type])]
            elif literal_type == 4:
                node["string_table"] = self._object_pointer(r)
                node["parts"] = [self._expr(r, depth + 1), self._expr(r, depth + 1)]
            else:
                raise UnsupportedError(f"unknown EBlueprintTextLiteralType {literal_type}")
        elif opcode == 0x20:
            node["object"] = self._object_pointer(r)
        elif opcode in (0x67, 0x6D):
            node["expression"] = self._expr(r, depth + 1)
        elif opcode == 0x21:
            node["value"] = self._script_name(r)
        elif opcode == 0x22:
            fmt = "d" if self.package.summary.file_version_ue5 >= UE5_LARGE_WORLD_COORDINATES else "f"
            node["value"] = [float(self._xfer(r, fmt)) for _ in range(3)]
        elif opcode == 0x23:
            fmt = "d" if self.package.summary.file_version_ue5 >= UE5_LARGE_WORLD_COORDINATES else "f"
            node["value"] = [float(self._xfer(r, fmt)) for _ in range(3)]
        elif opcode == 0x41:
            node["value"] = [float(self._xfer(r, "f")) for _ in range(3)]
        elif opcode == 0x2B:
            fmt = "d" if self.package.summary.file_version_ue5 >= UE5_LARGE_WORLD_COORDINATES else "f"
            node["rotation"] = [float(self._xfer(r, fmt)) for _ in range(4)]
            node["translation"] = [float(self._xfer(r, fmt)) for _ in range(3)]
            node["scale"] = [float(self._xfer(r, fmt)) for _ in range(3)]
        elif opcode == 0x2F:
            node["struct"] = self._object_pointer(r)
            node["serialized_size"] = int(self._xfer(r, "i"))
            node["values"] = self._expr_list(r, 0x30, depth + 1)
        elif opcode == 0x31:
            if self.package.summary.file_version_ue4 < VER_UE4_CHANGE_SETARRAY_BYTECODE:
                node["inner_property"] = self._field_pointer(r)
            else:
                node["target"] = self._expr(r, depth + 1)
            node["values"] = self._expr_list(r, 0x32, depth + 1)
        elif opcode in (0x39, 0x3B):
            node["target"] = self._expr(r, depth + 1)
            node["element_count"] = int(self._xfer(r, "i"))
            terminator = 0x3A if opcode == 0x39 else 0x3C
            node["values"] = self._expr_list(r, terminator, depth + 1)
        elif opcode in (0x65, 0x3D):
            node["inner_property"] = self._field_pointer(r)
            node["element_count"] = int(self._xfer(r, "i"))
            terminator = 0x66 if opcode == 0x65 else 0x3E
            node["values"] = self._expr_list(r, terminator, depth + 1)
        elif opcode == 0x3F:
            node["key_property"] = self._field_pointer(r)
            node["value_property"] = self._field_pointer(r)
            node["element_count"] = int(self._xfer(r, "i"))
            node["values"] = self._expr_list(r, 0x40, depth + 1)
        elif opcode == 0x11:
            node["property"] = self._field_pointer(r)
            node["value"] = int(self._xfer(r, "B"))
        elif opcode in (0x24, 0x2C):
            node["value"] = int(self._xfer(r, "B"))
        elif opcode == 0x07:
            node["target"] = self._jump(r, node["token"])
            node["condition"] = self._expr(r, depth + 1)
        elif opcode == 0x09:
            node["line"] = int(self._xfer(r, "H"))
            node["debug_mode"] = int(self._xfer(r, "B"))
            node["condition"] = self._expr(r, depth + 1)
        elif opcode == 0x18:
            node["skip"] = self._jump(r, node["token"])
            node["expression"] = self._expr(r, depth + 1)
        elif opcode == 0x4B:
            node["function_name"] = self._script_name(r)
        elif opcode == 0x61:
            node["function_name"] = self._script_name(r)
            node["delegate"] = self._expr(r, depth + 1)
            node["object"] = self._expr(r, depth + 1)
        elif opcode == 0x69:
            case_count = int(self._xfer(r, "H"))
            node["case_count"] = case_count
            node["end_target"] = self._jump(r, "EX_SwitchValue.end")
            node["index"] = self._expr(r, depth + 1)
            cases = []
            for _ in range(case_count):
                cases.append({
                    "value": self._expr(r, depth + 1),
                    "next_target": self._jump(r, "EX_SwitchValue.next"),
                    "result": self._expr(r, depth + 1),
                })
            node["cases"] = cases
            node["default"] = self._expr(r, depth + 1)
        elif opcode == 0x6B:
            node["array"] = self._expr(r, depth + 1)
            node["index"] = self._expr(r, depth + 1)
        elif opcode == 0x71:
            node["transaction_id"] = int(self._xfer(r, "i"))
            node["stop_mode"] = int(self._xfer(r, "b"))
        else:
            raise UnsupportedError(f"Kismet opcode {node['token']} is recognized but not implemented")

        node["storage_size"] = r.position - storage_start
        node["bytecode_size"] = self._code - code_start
        return node

    def _script(self, r: BinaryReader, bytecode_size: int, storage_size: int) -> dict[str, Any]:
        if bytecode_size < 0 or storage_size < 0:
            raise FormatError(f"negative UStruct script sizes {bytecode_size}/{storage_size}")
        if bytecode_size > 1 << 30 or storage_size > 1 << 30:
            raise FormatError(f"implausible UStruct script sizes {bytecode_size}/{storage_size}")
        storage_start = r.position
        storage_end = storage_start + storage_size
        if storage_end > r.limit:  # bounded export; no seek or read is performed here
            raise BoundsError(f"UStruct script ends at 0x{storage_end:x}, after export boundary 0x{r.limit:x}")
        self._expression_count = 0
        self._bytecode_size = bytecode_size
        self._storage_end = storage_end
        self._code = 0
        self._jump_offsets = []
        expressions: list[dict[str, Any]] = []
        while self._code < bytecode_size:
            expressions.append(self._expr(r))
        if self._code != bytecode_size:
            raise FormatError(f"bytecode size mismatch: declared {bytecode_size}, decoded {self._code}")
        if r.position != storage_end:
            raise FormatError(
                f"serialized script size mismatch: declared {storage_size}, consumed {r.position - storage_start}"
            )
        if bytecode_size and (not expressions or expressions[-1]["opcode"] != 0x53):
            raise FormatError("non-empty UStruct script does not end with EX_EndOfScript")
        invalid_jumps = [item for item in self._jump_offsets if not 0 <= item["target"] <= bytecode_size]
        if invalid_jumps:
            raise FormatError(f"{len(invalid_jumps)} bytecode jump targets are outside 0..{bytecode_size}")
        return {
            "bytecode_buffer_size": bytecode_size,
            "serialized_script_size": storage_size,
            "storage_physical_offset": storage_start,
            "storage_sha256": self._sha_range(r.path, storage_start, storage_size),
            "expression_count": self._expression_count,
            "expressions": expressions,
            "jump_offsets": self._jump_offsets,
            "validation": {
                "bytecode_size_exact": True,
                "serialized_storage_size_exact": True,
                "end_of_script": not bytecode_size or expressions[-1]["opcode"] == 0x53,
                "jump_targets_in_range": True,
            },
        }

    @staticmethod
    def _sha_range(path: Path, offset: int, size: int) -> str:
        digest = hashlib.sha256()
        with path.open("rb") as source:
            source.seek(offset)
            remaining = size
            while remaining:
                chunk = source.read(min(remaining, 1024 * 1024))
                if not chunk:
                    raise BoundsError(f"short read hashing {path} at 0x{offset:x}")
                digest.update(chunk)
                remaining -= len(chunk)
        return digest.hexdigest()

    def parse_function(self, export_index: int) -> dict[str, Any]:
        if not 1 <= export_index <= len(self.package.exports):
            raise FormatError(f"export index {export_index} outside package")
        export = self.package.exports[export_index - 1]
        base = {
            "export_index": export_index,
            "object": self.package.object_path(export_index),
            "name": export.object_name.display,
            "class": export.class_name,
        }
        if export.class_name != "Function":
            return {**base, "status": "UNSUPPORTED", "reason": "export class is not Function"}
        if export.payload_availability != "available":
            return {**base, "status": "MISSING_INPUT", "reason": export.payload_availability}
        tagged = self.properties.parse_export(export_index)
        trailing = tagged.get("trailing_native")
        if not trailing:
            return {**base, "status": "UNSUPPORTED", "reason": "no UStruct native stream"}
        source = Path(export.payload_source or self.package.path)
        start = int(trailing["physical_offset"])
        end = int(export.payload_physical_offset or 0) + export.serial_size
        try:
            with BinaryReader(source) as r:
                r.seek(start)
                with r.bounded(end):
                    prefix = self._struct_prefix(r)
                    script_header_offset = r.position
                    bytecode_size, storage_size = r.i32(), r.i32()
                    script = self._script(r, bytecode_size, storage_size)
                    footer_offset = r.position
                    function_flags = r.u32()
                    rep_offset = r.i16() if function_flags & FUNC_NET else None
                    event_graph_function = event_graph_call_offset = None
                    if self.package.summary.file_version_ue4 >= VER_UE4_SERIALIZE_BLUEPRINT_EVENTGRAPH_FASTCALLS_IN_UFUNCTION:
                        event_graph_function = self._package_index(r)
                        event_graph_call_offset = r.i32()
                    if r.position != end:
                        raise FormatError(f"UFunction footer leaves {end - r.position} bytes")
            return {
                **base,
                "status": "VERIFIED",
                "struct_prefix": prefix,
                "script_header_physical_offset": script_header_offset,
                "script": script,
                "function_footer": {
                    "physical_offset": footer_offset,
                    "function_flags": function_flags,
                    "function_flag_names": [
                        name for bit, name in FUNCTION_FLAG_NAMES.items() if function_flags & bit
                    ],
                    "rep_offset": rep_offset,
                    "event_graph_function": event_graph_function,
                    "event_graph_call_offset": event_graph_call_offset,
                },
                "provenance": {
                    "payload_source": str(source),
                    "export_physical_offset": export.payload_physical_offset,
                    "export_size": export.serial_size,
                    "tagged_property_end": start,
                    "native_stream_end": end,
                    "export_map_script_serialization_range": {
                        "start": export.script_serialization_start_offset,
                        "end": export.script_serialization_end_offset,
                        "semantics": "tagged-property serialization bounds; not bytecode offsets",
                    },
                },
            }
        except (BoundsError, FormatError, UnsupportedError, UnicodeError) as exc:
            return {
                **base,
                "status": "UNSUPPORTED",
                "reason": str(exc),
                "raw_native_stream": trailing,
            }

    def decode_functions(self) -> dict[str, Any]:
        indices = [i for i, item in enumerate(self.package.exports, 1) if item.class_name == "Function"]
        functions = [self.parse_function(index) for index in indices]
        verified = sum(item["status"] == "VERIFIED" for item in functions)
        missing = sum(item["status"] == "MISSING_INPUT" for item in functions)
        return {
            "schema": "ueassettool.blueprint-bytecode/v1",
            "status": "VERIFIED" if verified == len(functions) else "MISSING_INPUT" if missing else "PARTIAL",
            "function_count": len(functions),
            "verified_function_count": verified,
            "unsupported_function_count": len(functions) - verified - missing,
            "missing_function_count": missing,
            "functions": functions,
            "semantics": "Exact serialized Kismet expressions and UFunction metadata; not decompiled C++.",
        }
