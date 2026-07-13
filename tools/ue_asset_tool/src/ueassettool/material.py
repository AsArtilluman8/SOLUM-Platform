from __future__ import annotations

from collections import Counter
from pathlib import Path
from typing import Any, Iterator

from .package import UnrealPackage
from .properties import PropertyParser


MATERIAL_ROOT_CLASSES = {
    "Material",
    "MaterialFunction",
    "MaterialFunctionInstance",
    "MaterialInstance",
    "MaterialInstanceConstant",
    "MaterialParameterCollection",
}

MATERIAL_INSTANCE_CLASSES = {"MaterialInstance", "MaterialInstanceConstant"}

MATERIAL_PARAMETER_ARRAYS = {
    "ScalarParameterValues": ("scalar", ("ParameterValue",)),
    "VectorParameterValues": ("vector", ("ParameterValue",)),
    "DoubleVectorParameterValues": ("double_vector", ("ParameterValue",)),
    "TextureParameterValues": ("texture", ("ParameterValue",)),
    "TextureCollectionParameterValues": ("texture_collection", ("ParameterValue",)),
    "RuntimeVirtualTextureParameterValues": ("runtime_virtual_texture", ("ParameterValue",)),
    "SparseVolumeTextureParameterValues": ("sparse_volume_texture", ("ParameterValue",)),
    "FontParameterValues": ("font", ("FontValue", "FontPage")),
}


def _source(package: UnrealPackage) -> dict[str, Any]:
    version = package.summary.saved_by_engine_version
    return {
        "path": str(package.path),
        "size": package.path.stat().st_size,
        "sha256": package.sha256,
        "package": package.summary.package_name,
        "engine": version.display if version else None,
        "file_version_ue4": package.summary.file_version_ue4,
        "file_version_ue5": package.summary.file_version_ue5,
    }


def _property_keys(properties: list[dict[str, Any]]) -> list[tuple[str, dict[str, Any]]]:
    counts = Counter(str(item.get("name")) for item in properties)
    result: list[tuple[str, dict[str, Any]]] = []
    for item in properties:
        name = str(item.get("name"))
        index = int(item.get("array_index", 0))
        key = f"{name}[{index}]" if counts[name] > 1 or index else name
        result.append((key, item))
    return result


def _simple_value(value: Any) -> Any:
    if isinstance(value, list):
        return [_simple_value(item) for item in value]
    if not isinstance(value, dict):
        return value
    if value.get("kind") in ("expression_input", "material_input"):
        return {key: _simple_value(item) for key, item in value.items()}
    if isinstance(value.get("properties"), list):
        return {
            "fields": {
                key: _simple_value(item.get("value"))
                if str(item.get("decode_status", "")).startswith("decoded")
                else {
                    "status": "RAW_VERIFIED",
                    "type": item.get("type", {}).get("display"),
                    "size": item.get("size"),
                    "sha256": item.get("raw", {}).get("sha256"),
                    "reason": item.get("decode_note"),
                }
                for key, item in _property_keys(value["properties"])
            },
            "terminated": value.get("terminated"),
        }
    return {key: _simple_value(item) for key, item in value.items()}


def _attributes(properties: list[dict[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, item in _property_keys(properties):
        if str(item.get("decode_status", "")).startswith("decoded"):
            result[key] = _simple_value(item.get("value"))
        else:
            result[key] = {
                "status": "RAW_VERIFIED",
                "type": item.get("type", {}).get("display"),
                "size": item.get("size"),
                "sha256": item.get("raw", {}).get("sha256"),
                "reason": item.get("decode_note"),
            }
    return result


def _provenance(item: dict[str, Any]) -> dict[str, Any]:
    raw = item.get("raw") or {}
    return {
        "property": item.get("name"),
        "array_index": item.get("array_index", 0),
        "type": item.get("type", {}).get("display"),
        "header_physical_offset": item.get("header_physical_offset"),
        "value_physical_offset": raw.get("physical_offset"),
        "size": item.get("size"),
        "sha256": raw.get("sha256"),
        "decode_status": item.get("decode_status"),
    }


def _iter_expression_inputs(
    value: Any,
    path: str,
    provenance: dict[str, Any] | None,
) -> Iterator[tuple[str, dict[str, Any], dict[str, Any] | None]]:
    if not isinstance(value, dict):
        return
    if value.get("kind") in ("expression_input", "material_input"):
        yield path, value, provenance
        return
    properties = value.get("properties")
    if isinstance(properties, list):
        for key, item in _property_keys(properties):
            if str(item.get("decode_status", "")).startswith("decoded"):
                nested = f"{path}.{key}" if path else key
                yield from _iter_expression_inputs(item.get("value"), nested, _provenance(item))
        return
    items = value.get("items")
    if isinstance(items, list):
        for index, item in enumerate(items):
            yield from _iter_expression_inputs(item, f"{path}[{index}]", provenance)
    entries = value.get("entries")
    if isinstance(entries, list):
        for index, entry in enumerate(entries):
            yield from _iter_expression_inputs(entry.get("key"), f"{path}[{index}].key", provenance)
            yield from _iter_expression_inputs(entry.get("value"), f"{path}[{index}].value", provenance)


def _iter_object_references(value: Any, path: str) -> Iterator[tuple[str, dict[str, Any]]]:
    if not isinstance(value, dict):
        return
    if value.get("kind") in ("expression_input", "material_input"):
        return
    if "package_index" in value and "object" in value:
        yield path, value
        return
    properties = value.get("properties")
    if isinstance(properties, list):
        for key, item in _property_keys(properties):
            if str(item.get("decode_status", "")).startswith("decoded"):
                nested = f"{path}.{key}" if path else key
                yield from _iter_object_references(item.get("value"), nested)
        return
    items = value.get("items")
    if isinstance(items, list):
        for index, item in enumerate(items):
            yield from _iter_object_references(item, f"{path}[{index}]")
    entries = value.get("entries")
    if isinstance(entries, list):
        for index, entry in enumerate(entries):
            yield from _iter_object_references(entry.get("key"), f"{path}[{index}].key")
            yield from _iter_object_references(entry.get("value"), f"{path}[{index}].value")


def _reference_class(package: UnrealPackage, index: int) -> str | None:
    if index < 0:
        at = -index - 1
        return package.imports[at].class_name.display if 0 <= at < len(package.imports) else None
    if index > 0:
        at = index - 1
        return package.exports[at].class_name if 0 <= at < len(package.exports) else None
    return None


def _decoded_fields(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict) or not isinstance(value.get("properties"), list):
        return {}
    return {
        key: item.get("value")
        for key, item in _property_keys(value["properties"])
        if str(item.get("decode_status", "")).startswith("decoded")
    }


def _field_properties(value: Any) -> dict[str, dict[str, Any]]:
    if not isinstance(value, dict) or not isinstance(value.get("properties"), list):
        return {}
    return {key: item for key, item in _property_keys(value["properties"])}


def _decoded_property(item: dict[str, Any] | None) -> Any:
    if item is None or not str(item.get("decode_status", "")).startswith("decoded"):
        return None
    return item.get("value")


def _guid_valid(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    parts = value.split("-")
    return len(parts) == 4 and all(len(part) == 8 and all(c in "0123456789abcdefABCDEF" for c in part) for part in parts)


def _finite_serialized_value(value: Any) -> bool:
    if isinstance(value, list):
        return all(_finite_serialized_value(item) for item in value)
    if not isinstance(value, dict):
        return True
    if "non_finite" in value:
        return False
    return all(_finite_serialized_value(item) for item in value.values())


def _item_provenance(
    container: dict[str, Any], index: int, fields: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    return {
        "container": _provenance(container),
        "element_index": index,
        "fields": {name: _provenance(item) for name, item in fields.items()},
    }


def _parameter_info(fields: dict[str, dict[str, Any]]) -> tuple[dict[str, Any], list[str]]:
    issues: list[str] = []
    info_property = fields.get("ParameterInfo")
    info_fields = _field_properties(_decoded_property(info_property))
    name = _decoded_property(info_fields.get("Name"))
    association = _decoded_property(info_fields.get("Association"))
    index = _decoded_property(info_fields.get("Index"))
    if not isinstance(name, str) or not name:
        issues.append("ParameterInfo.Name is missing or not a non-empty FName")
    if not isinstance(association, (str, int)):
        issues.append("ParameterInfo.Association is missing or not decoded")
    if not isinstance(index, int):
        issues.append("ParameterInfo.Index is missing or not int32")
    return {
        "name": name,
        "association": association,
        "index": index,
        "provenance": _provenance(info_property) if info_property else None,
        "field_provenance": {key: _provenance(item) for key, item in info_fields.items()},
    }, issues


def _iter_nested_unsupported(value: Any, path: str = "") -> Iterator[tuple[str, dict[str, Any]]]:
    if not isinstance(value, dict):
        return
    properties = value.get("properties")
    if isinstance(properties, list):
        for key, item in _property_keys(properties):
            nested = f"{path}.{key}" if path else key
            if not str(item.get("decode_status", "")).startswith("decoded"):
                yield nested, item
            else:
                yield from _iter_nested_unsupported(item.get("value"), nested)
    items = value.get("items")
    if isinstance(items, list):
        for index, item in enumerate(items):
            yield from _iter_nested_unsupported(item, f"{path}[{index}]")
    entries = value.get("entries")
    if isinstance(entries, list):
        for index, entry in enumerate(entries):
            yield from _iter_nested_unsupported(entry.get("key"), f"{path}[{index}].key")
            yield from _iter_nested_unsupported(entry.get("value"), f"{path}[{index}].value")


class MaterialContractDecoder:
    """Build an exact editor material graph contract from serialized inputs."""

    def __init__(self, package: UnrealPackage):
        self.package = package
        self.parser = PropertyParser(package)

    def _decoded_export(self, index: int) -> dict[str, Any]:
        return self.parser.parse_export(index)

    def _input_record(
        self,
        owner_index: int,
        owner_object: str,
        slot: str,
        value: dict[str, Any],
        provenance: dict[str, Any] | None,
        *,
        kind: str,
    ) -> tuple[dict[str, Any], dict[str, Any] | None]:
        expression = value["expression"]
        target_index = int(expression["package_index"])
        target_class = _reference_class(self.package, target_index)
        connected = target_index != 0
        issues: list[str] = []
        if connected and not (1 <= target_index <= len(self.package.exports)):
            issues.append("expression reference is not a local export")
        if connected and not str(target_class or "").startswith("MaterialExpression"):
            issues.append(f"target class is {target_class or '<unresolved>'}, not MaterialExpression")
        if connected and int(value["output_index"]) < 0:
            issues.append("negative output index")
        mask = value.get("mask", {})
        if any(mask.get(name) not in (0, 1) for name in ("enabled", "r", "g", "b", "a")):
            issues.append("component mask contains a value outside 0/1")

        item = {
            "slot": slot,
            "kind": value.get("kind"),
            "connected": connected,
            "expression": expression,
            "target_class": target_class,
            "output_index": value["output_index"],
            "serialized_input_name": value["input_name"],
            "mask": mask,
            "use_constant": value.get("use_constant"),
            "constant": value.get("constant"),
            "provenance": provenance,
            "validation": "VERIFIED" if not issues else "UNSUPPORTED",
            "issues": issues,
        }
        link = None
        if connected:
            link = {
                "kind": kind,
                "from_export": target_index,
                "from_object": expression["object"],
                "from_output_index": value["output_index"],
                "to_export": owner_index,
                "to_object": owner_object,
                "to_input": slot,
                "mask": mask,
                "validation": "VERIFIED" if not issues else "UNSUPPORTED",
                "issues": issues,
                "provenance": provenance,
            }
        return item, link

    def _decode_native_tail(
        self, index: int, decoded: dict[str, Any], properties: dict[str, dict[str, Any]],
    ) -> dict[str, Any] | None:
        tail = decoded.get("trailing_native")
        if not tail:
            return None
        export = self.package.exports[index - 1]
        size = int(tail.get("size", -1))
        preview = str(tail.get("preview_hex", ""))
        try:
            raw = bytes.fromhex(preview)
        except ValueError:
            raw = b""
        if len(raw) != size or size % 4:
            return {"status": "RAW_VERIFIED", "class": export.class_name, **tail}

        labels: list[str] = []
        if export.class_name in MATERIAL_INSTANCE_CLASSES:
            # UMaterialInterface::Serialize and UMaterialInstance::Serialize
            # both gained cached-data bool32 values in UE5.  An instance with
            # a static permutation then appends SerializeInlineShaderMaps'
            # int32 resource count.  Editor saves in this corpus contain no
            # cached data or inline shader resources.
            if self.package.summary.file_version_ue5:
                labels.extend(("saved_cached_expression_data", "saved_cached_instance_data"))
            if _decoded_property(properties.get("bHasStaticPermutationResource")) is True:
                labels.append("inline_shader_map_count")
        elif export.class_name == "Material":
            version = self.package.summary.saved_by_engine_version
            if self.package.summary.file_version_ue5 and version and version.major == 5 and version.minor >= 5:
                labels.extend((
                    "saved_cached_expression_data",
                    "inline_shader_map_count",
                    "force_nanite_usage",
                ))
            elif self.package.summary.file_version_ue5 and version and version.major == 5 and version.minor <= 1:
                labels.extend((
                    "inline_shader_map_count",
                    "saved_cached_expression_data_deprecated",
                ))
            elif not self.package.summary.file_version_ue5:
                labels.append("inline_shader_map_count")
            else:
                return {
                    "status": "RAW_VERIFIED", "class": export.class_name,
                    "reason": "material native layout is not pinned for this saved engine version",
                    **tail,
                }
        elif export.class_name in ("MaterialEditorOnlyData", "MaterialInstanceEditorOnlyData"):
            # UMaterialInterfaceEditorOnlyData::Serialize always appends this
            # bool32 after its tagged properties.
            labels.append("saved_cached_expression_editor_data")
        else:
            return {"status": "RAW_VERIFIED", "class": export.class_name, **tail}

        if len(labels) * 4 != size:
            return {
                "status": "RAW_VERIFIED", "class": export.class_name,
                "reason": f"expected {len(labels) * 4} native bytes from verified source path, found {size}",
                **tail,
            }
        fields = []
        issues: list[str] = []
        for position, label in enumerate(labels):
            value = int.from_bytes(raw[position * 4:position * 4 + 4], "little", signed=True)
            if label.endswith("count"):
                if value != 0:
                    issues.append(f"{label}={value} requires shader-map payload decoding")
            elif value not in (0, 1):
                issues.append(f"{label} bool32 is {value}, expected 0 or 1")
            elif value:
                issues.append(f"{label}=true requires cached tagged-struct decoding")
            fields.append({
                "name": label,
                "value": bool(value) if not label.endswith("count") else value,
                "physical_offset": int(tail["physical_offset"]) + position * 4,
                "size": 4,
            })
        return {
            "status": "VERIFIED" if not issues else "RAW_VERIFIED",
            "class": export.class_name,
            "physical_offset": tail.get("physical_offset"),
            "size": size,
            "sha256": tail.get("sha256"),
            "fields": fields,
            "issues": issues,
            "source_layouts": [
                "UMaterialInterface::Serialize",
                "UMaterialInstance::Serialize" if export.class_name in MATERIAL_INSTANCE_CLASSES
                else "UMaterial::Serialize" if export.class_name == "Material"
                else "UMaterialInterfaceEditorOnlyData::Serialize",
                *( ["SerializeInlineShaderMaps"] if "inline_shader_map_count" in labels else [] ),
            ],
        }

    def _material_instance_contract(
        self, root_index: int, decoded_by_index: dict[int, dict[str, Any]],
    ) -> dict[str, Any]:
        package = self.package
        decoded = decoded_by_index[root_index]
        properties = _field_properties(decoded)
        issues: list[str] = []
        parent_property = properties.get("Parent")
        parent = _decoded_property(parent_property)
        if not isinstance(parent, dict) or not isinstance(parent.get("package_index"), int):
            issues.append("Parent object reference is missing or not decoded")

        parameters: list[dict[str, Any]] = []
        identities: Counter[tuple[Any, ...]] = Counter()
        for property_name, (kind, value_fields) in MATERIAL_PARAMETER_ARRAYS.items():
            container = properties.get(property_name)
            if container is None:
                continue
            value = _decoded_property(container)
            if not isinstance(value, dict) or not isinstance(value.get("items"), list):
                issues.append(f"{property_name} is not a decoded array")
                continue
            if value.get("count") != len(value["items"]):
                issues.append(f"{property_name} count does not match decoded items")
            for element_index, item in enumerate(value["items"]):
                fields = _field_properties(item)
                info, item_issues = _parameter_info(fields)
                values = {name: _simple_value(_decoded_property(fields.get(name))) for name in value_fields}
                if any(fields.get(name) is None for name in value_fields):
                    item_issues.append(f"missing value field(s): {', '.join(value_fields)}")
                if any(
                    fields.get(name) is not None
                    and not str(fields[name].get("decode_status", "")).startswith("decoded")
                    for name in value_fields
                ):
                    item_issues.append("one or more parameter values are not decoded")
                if not _finite_serialized_value(values):
                    item_issues.append("parameter contains a non-finite numeric value")
                expression_guid = _decoded_property(fields.get("ExpressionGUID"))
                if expression_guid is not None and not _guid_valid(expression_guid):
                    item_issues.append("ExpressionGUID is not a serialized FGuid")
                identity = (kind, info["association"], info["index"], info["name"])
                identities[identity] += 1
                parameters.append({
                    "kind": kind,
                    **{key: info[key] for key in ("name", "association", "index")},
                    "value": values[value_fields[0]] if len(value_fields) == 1 else values,
                    "expression_guid": expression_guid,
                    "status": "VERIFIED" if not item_issues else "UNSUPPORTED",
                    "issues": item_issues,
                    "provenance": _item_provenance(container, element_index, fields),
                })
        duplicates = [
            {"kind": key[0], "association": key[1], "index": key[2], "name": key[3], "count": count}
            for key, count in identities.items() if count > 1
        ]
        if duplicates:
            issues.append("duplicate dynamic parameter identities")

        static_parameters: list[dict[str, Any]] = []
        static_sources: list[dict[str, Any]] = []
        source_values: list[tuple[int, str, dict[str, Any], dict[str, Any]]] = []
        for property_name in ("StaticParametersRuntime", "StaticParameters"):
            prop = properties.get(property_name)
            struct = _decoded_property(prop)
            if prop is not None and isinstance(struct, dict):
                source_values.append((root_index, property_name, prop, struct))

        editor_ref = _decoded_property(properties.get("EditorOnlyData"))
        editor_indices = {
            int(editor_ref["package_index"])
            for _ in (0,)
            if isinstance(editor_ref, dict) and int(editor_ref.get("package_index", 0)) > 0
        }
        editor_indices.update(
            index for index, export in enumerate(package.exports, 1)
            if "MaterialInstanceEditorOnlyData" in str(export.class_name or "")
            and export.outer_index == root_index
        )
        for editor_index in sorted(editor_indices):
            editor = decoded_by_index.get(editor_index)
            if not editor:
                issues.append(f"EditorOnlyData export {editor_index} is not available")
                continue
            editor_properties = _field_properties(editor)
            prop = editor_properties.get("StaticParameters")
            struct = _decoded_property(prop)
            if prop is not None and isinstance(struct, dict):
                source_values.append((editor_index, "StaticParameters", prop, struct))

        static_identities: Counter[tuple[Any, ...]] = Counter()
        for source_index, source_name, source_property, struct in source_values:
            struct_fields = _field_properties(struct)
            static_sources.append({
                "export_index": source_index,
                "object": package.object_path(source_index),
                "property": source_name,
                "provenance": _provenance(source_property),
            })
            for array_name, container in struct_fields.items():
                array_value = _decoded_property(container)
                if not isinstance(array_value, dict) or not isinstance(array_value.get("items"), list):
                    issues.append(f"{source_name}.{array_name} is not a decoded array")
                    continue
                for element_index, item in enumerate(array_value["items"]):
                    fields = _field_properties(item)
                    info, item_issues = _parameter_info(fields)
                    expression_guid = _decoded_property(fields.get("ExpressionGUID"))
                    if expression_guid is not None and not _guid_valid(expression_guid):
                        item_issues.append("ExpressionGUID is not a serialized FGuid")
                    override = _decoded_property(fields.get("bOverride"))
                    if override is not None and not isinstance(override, bool):
                        item_issues.append("bOverride is not bool")
                    serialized_value = {
                        name: _simple_value(_decoded_property(prop))
                        for name, prop in fields.items()
                        if name not in ("ParameterInfo", "ExpressionGUID", "bOverride")
                    }
                    if not _finite_serialized_value(serialized_value):
                        item_issues.append("static parameter contains a non-finite numeric value")
                    kind = {
                        "StaticSwitchParameters": "static_switch",
                        "StaticComponentMaskParameters": "static_component_mask",
                        "TerrainLayerWeightParameters": "terrain_layer_weight",
                        "MaterialLayersParameters": "material_layers",
                    }.get(array_name, array_name)
                    identity = (kind, info["association"], info["index"], info["name"])
                    static_identities[identity] += 1
                    static_parameters.append({
                        "kind": kind,
                        **{key: info[key] for key in ("name", "association", "index")},
                        "value": serialized_value,
                        "override": override,
                        "expression_guid": expression_guid,
                        "status": "VERIFIED" if not item_issues else "UNSUPPORTED",
                        "issues": item_issues,
                        "source_export": source_index,
                        "source_property": source_name,
                        "provenance": _item_provenance(container, element_index, fields),
                    })
        static_duplicates = [
            {"kind": key[0], "association": key[1], "index": key[2], "name": key[3], "count": count}
            for key, count in static_identities.items() if count > 1
        ]
        if static_duplicates:
            issues.append("duplicate static parameter identities")

        base_property = properties.get("BasePropertyOverrides")
        base_fields = _field_properties(_decoded_property(base_property))
        base_overrides = {
            "serialized_fields": {
                name: _simple_value(_decoded_property(item)) for name, item in base_fields.items()
            },
            "override_flags": {
                name: _decoded_property(item) for name, item in base_fields.items()
                if name.startswith("bOverride_")
            },
            "provenance": {
                "container": _provenance(base_property) if base_property else None,
                "fields": {name: _provenance(item) for name, item in base_fields.items()},
            },
            "semantics": "Serialized fields are reported exactly; a value is active only when its matching bOverride_* flag says so.",
        }

        native = self._decode_native_tail(root_index, decoded, properties)
        editor_native = []
        for editor_index in sorted(editor_indices):
            editor = decoded_by_index.get(editor_index)
            if editor:
                record = self._decode_native_tail(editor_index, editor, _field_properties(editor))
                if record:
                    editor_native.append({"export_index": editor_index, **record})
        native_records = ([{"export_index": root_index, **native}] if native else []) + editor_native
        if any(item["status"] != "VERIFIED" for item in native_records):
            issues.append("one or more native serialization suffixes remain raw")
        if any(item["status"] != "VERIFIED" for item in parameters + static_parameters):
            issues.append("one or more parameter records failed validation")
        return {
            "status": "VERIFIED" if not issues else "RAW_VERIFIED",
            "export_index": root_index,
            "object": package.object_path(root_index),
            "class": package.exports[root_index - 1].class_name,
            "parent": parent,
            "parent_provenance": _provenance(parent_property) if parent_property else None,
            "parameter_state_id": _decoded_property(properties.get("ParameterStateId")),
            "dynamic_parameters": parameters,
            "dynamic_parameter_count": len(parameters),
            "scalar_parameters": [item for item in parameters if item["kind"] == "scalar"],
            "vector_parameters": [item for item in parameters if item["kind"] == "vector"],
            "texture_parameters": [item for item in parameters if item["kind"] == "texture"],
            "duplicate_dynamic_parameters": duplicates,
            "static_parameters": static_parameters,
            "static_parameter_count": len(static_parameters),
            "static_switch_parameters": [
                item for item in static_parameters if item["kind"] == "static_switch"
            ],
            "duplicate_static_parameters": static_duplicates,
            "static_sources": static_sources,
            "base_property_overrides": base_overrides,
            "native_serialization": native_records,
            "issues": issues,
            "semantics": "Exact serialized local overrides; effective inherited values require resolving the parent material chain.",
        }

    def _parameter_collection_contract(
        self, root_index: int, decoded: dict[str, Any],
    ) -> dict[str, Any]:
        properties = _field_properties(decoded)
        issues: list[str] = []
        state_id = _decoded_property(properties.get("StateId"))
        if not _guid_valid(state_id):
            issues.append("StateId is not a serialized FGuid")
        parameters: list[dict[str, Any]] = []
        names: Counter[str] = Counter()
        ids: Counter[str] = Counter()
        for property_name, kind in (("ScalarParameters", "scalar"), ("VectorParameters", "vector")):
            container = properties.get(property_name)
            value = _decoded_property(container)
            if container is None:
                continue
            if not isinstance(value, dict) or not isinstance(value.get("items"), list):
                issues.append(f"{property_name} is not a decoded array")
                continue
            if value.get("count") != len(value["items"]):
                issues.append(f"{property_name} count does not match decoded items")
            for element_index, item in enumerate(value["items"]):
                fields = _field_properties(item)
                item_issues: list[str] = []
                name = _decoded_property(fields.get("ParameterName"))
                identifier = _decoded_property(fields.get("Id"))
                default = _decoded_property(fields.get("DefaultValue"))
                if not isinstance(name, str) or not name:
                    item_issues.append("ParameterName is missing or empty")
                if not _guid_valid(identifier):
                    item_issues.append("Id is not a serialized FGuid")
                if fields.get("DefaultValue") is None:
                    item_issues.append("DefaultValue is missing")
                elif not str(fields["DefaultValue"].get("decode_status", "")).startswith("decoded"):
                    item_issues.append("DefaultValue is not decoded")
                if not _finite_serialized_value(default):
                    item_issues.append("DefaultValue contains a non-finite number")
                if isinstance(name, str):
                    names[name] += 1
                if isinstance(identifier, str):
                    ids[identifier] += 1
                parameters.append({
                    "kind": kind,
                    "name": name,
                    "id": identifier,
                    "default_value": _simple_value(default),
                    "status": "VERIFIED" if not item_issues else "UNSUPPORTED",
                    "issues": item_issues,
                    "provenance": _item_provenance(container, element_index, fields),
                })
        duplicate_names = sorted(name for name, count in names.items() if count > 1)
        duplicate_ids = sorted(identifier for identifier, count in ids.items() if count > 1)
        if duplicate_names:
            issues.append("duplicate collection parameter names")
        if duplicate_ids:
            issues.append("duplicate collection parameter IDs")
        if any(item["status"] != "VERIFIED" for item in parameters):
            issues.append("one or more collection parameters failed validation")
        return {
            "status": "VERIFIED" if not issues else "RAW_VERIFIED",
            "export_index": root_index,
            "object": self.package.object_path(root_index),
            "state_id": state_id,
            "parameters": parameters,
            "parameter_count": len(parameters),
            "scalar_parameters": [item for item in parameters if item["kind"] == "scalar"],
            "vector_parameters": [item for item in parameters if item["kind"] == "vector"],
            "scalar_parameter_count": sum(item["kind"] == "scalar" for item in parameters),
            "vector_parameter_count": sum(item["kind"] == "vector" for item in parameters),
            "duplicate_names": duplicate_names,
            "duplicate_ids": duplicate_ids,
            "issues": issues,
            "semantics": "Exact serialized names, stable IDs and defaults; runtime values are supplied by the owning world.",
        }

    def decode(self) -> dict[str, Any]:
        package = self.package
        expression_indices = [
            index
            for index, export in enumerate(package.exports, 1)
            if str(export.class_name or "").startswith("MaterialExpression")
        ]
        roots: list[dict[str, Any]] = []
        editor_data: list[tuple[int, dict[str, Any]]] = []
        nodes: list[dict[str, Any]] = []
        links: list[dict[str, Any]] = []
        reference_edges: list[dict[str, Any]] = []
        unsupported: list[dict[str, Any]] = []
        trailing_native: list[dict[str, Any]] = []
        all_references: list[dict[str, Any]] = []
        material_outputs: list[dict[str, Any]] = []

        decoded_by_index: dict[int, dict[str, Any]] = {}
        relevant_indices = [
            index
            for index, export in enumerate(package.exports, 1)
            if (
                index in expression_indices
                or export.class_name in MATERIAL_ROOT_CLASSES
                or "MaterialEditorOnlyData" in str(export.class_name or "")
                or "MaterialFunctionEditorOnlyData" in str(export.class_name or "")
                or "MaterialInstanceEditorOnlyData" in str(export.class_name or "")
            )
        ]
        for index in relevant_indices:
            export = package.exports[index - 1]
            decoded = self._decoded_export(index)
            decoded_by_index[index] = decoded
            if decoded.get("trailing_native"):
                trailing_native.append({
                    "export_index": index,
                    "object": package.object_path(index),
                    "class": export.class_name,
                    **decoded["trailing_native"],
                })
            for key, item in _property_keys(decoded.get("properties", [])):
                failures = []
                if not str(item.get("decode_status", "")).startswith("decoded"):
                    failures.append((key, item))
                else:
                    failures.extend(_iter_nested_unsupported(item.get("value"), key))
                for property_path, failed in failures:
                    unsupported.append({
                        "export_index": index,
                        "object": package.object_path(index),
                        "class": export.class_name,
                        "property": property_path,
                        "type": failed.get("type", {}).get("display"),
                        "reason": failed.get("decode_note"),
                        "provenance": _provenance(failed),
                    })
            if export.class_name in MATERIAL_ROOT_CLASSES:
                root_properties = decoded.get("properties", [])
                roots.append({
                    "export_index": index,
                    "object": package.object_path(index),
                    "class": export.class_name,
                    "attributes": _attributes(root_properties),
                    "attribute_provenance": {
                        key: _provenance(item) for key, item in _property_keys(root_properties)
                    },
                    "parse_status": decoded.get("parse_status"),
                })
            if "EditorOnlyData" in str(export.class_name or ""):
                editor_data.append((index, decoded))

        for index in expression_indices:
            export = package.exports[index - 1]
            decoded = decoded_by_index[index]
            properties = decoded.get("properties", [])
            inputs: list[dict[str, Any]] = []
            references: list[dict[str, Any]] = []
            for key, item in _property_keys(properties):
                if not str(item.get("decode_status", "")).startswith("decoded"):
                    continue
                value = item.get("value")
                for slot, input_value, input_provenance in _iter_expression_inputs(
                    value, key, _provenance(item)
                ):
                    record, link = self._input_record(
                        index, package.object_path(index), slot, input_value, input_provenance,
                        kind="expression",
                    )
                    inputs.append(record)
                    if link:
                        links.append(link)
                for role, reference in _iter_object_references(value, key):
                    target_index = int(reference["package_index"])
                    ref = {
                        "role": role,
                        **reference,
                        "class": _reference_class(package, target_index),
                    }
                    references.append(ref)
                    all_references.append({"owner_export": index, **ref})
                    if target_index in expression_indices:
                        reference_edges.append({
                            "kind": "object_reference",
                            "from_export": target_index,
                            "from_object": reference["object"],
                            "to_export": index,
                            "to_object": package.object_path(index),
                            "role": role,
                            "validation": "VERIFIED",
                        })
            attrs = _attributes(properties)
            # MaterialExpressionGuid identifies a graph node. ExpressionGUID is
            # a parameter/function identity and is intentionally shared by
            # multiple nodes, so it must never be used for node uniqueness.
            guid = attrs.get("MaterialExpressionGuid")
            nodes.append({
                "export_index": index,
                "object": package.object_path(index),
                "name": export.object_name.display,
                "class": export.class_name,
                "guid": guid,
                "editor_position": {
                    "x": attrs.get("MaterialExpressionEditorX"),
                    "y": attrs.get("MaterialExpressionEditorY"),
                },
                "description": attrs.get("Desc") or attrs.get("Text"),
                "attributes": attrs,
                "attribute_provenance": {
                    key: _provenance(item) for key, item in _property_keys(properties)
                },
                "inputs": inputs,
                "references": references,
                "parse_status": decoded.get("parse_status"),
            })

        expression_collection: list[dict[str, Any]] = []
        collection_sources: list[dict[str, Any]] = []
        collection_available = False

        # UE5 moved editor expressions/comments from the Material(Function)
        # object into FMaterialExpressionCollection.  Accept both exact
        # versioned locations and validate the combined membership set.
        for index in relevant_indices:
            decoded = decoded_by_index[index]
            for _key, item in _property_keys(decoded.get("properties", [])):
                if not str(item.get("decode_status", "")).startswith("decoded"):
                    continue
                name = str(item.get("name"))
                value = item.get("value")
                if name == "ExpressionCollection":
                    fields = _decoded_fields(value)
                    for field_name in ("Expressions", "EditorComments"):
                        refs = fields.get(field_name)
                        if isinstance(refs, dict) and isinstance(refs.get("items"), list):
                            collection_available = True
                            expression_collection.extend(refs["items"])
                            collection_sources.append({
                                "export_index": index,
                                "property": f"ExpressionCollection.{field_name}",
                                "count": len(refs["items"]),
                            })
                elif name in ("FunctionExpressions", "FunctionEditorComments", "Expressions", "EditorComments"):
                    if isinstance(value, dict) and isinstance(value.get("items"), list):
                        collection_available = True
                        expression_collection.extend(value["items"])
                        collection_sources.append({
                            "export_index": index, "property": name, "count": len(value["items"]),
                        })

        for index, decoded in editor_data:
            export = package.exports[index - 1]
            for key, item in _property_keys(decoded.get("properties", [])):
                if not str(item.get("decode_status", "")).startswith("decoded"):
                    continue
                value = item.get("value")
                for slot, input_value, input_provenance in _iter_expression_inputs(
                    value, key, _provenance(item)
                ):
                    record, link = self._input_record(
                        index, package.object_path(index), slot, input_value, input_provenance,
                        kind="material_output",
                    )
                    material_outputs.append(record)
                    if link:
                        links.append(link)
                for role, reference in _iter_object_references(value, key):
                    all_references.append({
                        "owner_export": index,
                        "role": role,
                        **reference,
                        "class": _reference_class(package, int(reference["package_index"])),
                    })

        collection_indices = [int(item["package_index"]) for item in expression_collection]
        expression_set = set(expression_indices)
        collection_set = set(collection_indices)
        collection_unknown = sorted(collection_set - expression_set)
        collection_missing = sorted(expression_set - collection_set)
        collection_duplicates = sorted(
            index for index, count in Counter(collection_indices).items() if count > 1
        )

        guids = [str(node["guid"]) for node in nodes if node.get("guid")]
        duplicate_guids = sorted(guid for guid, count in Counter(guids).items() if count > 1)
        invalid_links = [item for item in links if item["validation"] != "VERIFIED"]
        input_count = sum(len(node["inputs"]) for node in nodes) + len(material_outputs)
        connected_input_count = len(links)
        graph_applicable = bool(expression_indices) or any(
            root["class"] in ("Material", "MaterialFunction", "MaterialFunctionInstance")
            for root in roots
        )
        graph_verified = (
            bool(nodes)
            and not unsupported
            and not invalid_links
            and not collection_unknown
            and not collection_missing
            and not collection_duplicates
            and (not collection_available or bool(expression_collection))
        )

        parameters = []
        function_inputs = []
        function_outputs = []
        function_calls = []
        for node in nodes:
            attrs = node["attributes"]
            if "Parameter" in str(node["class"]) or "ParameterName" in attrs:
                parameters.append({
                    "export_index": node["export_index"],
                    "object": node["object"],
                    "class": node["class"],
                    "name": attrs.get("ParameterName") or node["name"],
                    "guid": attrs.get("ExpressionGUID") or node.get("guid"),
                    "group": attrs.get("Group"),
                    "sort_priority": attrs.get("SortPriority"),
                    "default_value": attrs.get("DefaultValue"),
                    "texture": attrs.get("Texture"),
                    "collection": attrs.get("Collection"),
                })
            if node["class"] == "MaterialExpressionFunctionInput":
                function_inputs.append({
                    "export_index": node["export_index"], "object": node["object"],
                    "name": attrs.get("InputName"), "id": attrs.get("Id"),
                    "input_type": attrs.get("InputType"), "preview_value": attrs.get("PreviewValue"),
                    "description": attrs.get("Description") or attrs.get("Desc"),
                })
            elif node["class"] == "MaterialExpressionFunctionOutput":
                function_outputs.append({
                    "export_index": node["export_index"], "object": node["object"],
                    "name": attrs.get("OutputName"), "id": attrs.get("Id"),
                    "description": attrs.get("Description") or attrs.get("Desc"),
                })
            elif node["class"] == "MaterialExpressionMaterialFunctionCall":
                function_calls.append({
                    "export_index": node["export_index"], "object": node["object"],
                    "function": attrs.get("MaterialFunction"),
                    "inputs": attrs.get("FunctionInputs"),
                    "outputs": attrs.get("FunctionOutputs") or attrs.get("Outputs"),
                })

        dependencies = [
            {
                "import_index": -index,
                "object": package.object_path(-index),
                "class_package": item.class_package.display,
                "class": item.class_name.display,
                "optional": item.optional,
            }
            for index, item in enumerate(package.imports, 1)
        ]
        instance_contracts = [
            self._material_instance_contract(index, decoded_by_index)
            for index in relevant_indices
            if package.exports[index - 1].class_name in MATERIAL_INSTANCE_CLASSES
        ]
        collection_contracts = [
            self._parameter_collection_contract(index, decoded_by_index[index])
            for index in relevant_indices
            if package.exports[index - 1].class_name == "MaterialParameterCollection"
        ]
        native_serialization = []
        for item in trailing_native:
            export_index = int(item["export_index"])
            decoded = decoded_by_index[export_index]
            record = self._decode_native_tail(export_index, decoded, _field_properties(decoded))
            if record:
                native_serialization.append({"export_index": export_index, **record})
        native_verified_indices = {
            int(item["export_index"])
            for item in native_serialization
            if item["status"] == "VERIFIED"
        }
        unresolved_trailing_native = [
            item for item in trailing_native if int(item["export_index"]) not in native_verified_indices
        ]
        if instance_contracts:
            asset_kind = "material_instance"
            overall_status = (
                "VERIFIED"
                if all(item["status"] == "VERIFIED" for item in instance_contracts)
                and not unsupported and not unresolved_trailing_native
                else "RAW_VERIFIED"
            )
        elif collection_contracts:
            asset_kind = "material_parameter_collection"
            overall_status = (
                "VERIFIED"
                if all(item["status"] == "VERIFIED" for item in collection_contracts)
                and not unsupported and not unresolved_trailing_native
                else "RAW_VERIFIED"
            )
        else:
            asset_kind = "material_graph"
            overall_status = "UNSUPPORTED" if not nodes else "VERIFIED"
            if unsupported or unresolved_trailing_native or not graph_verified:
                overall_status = "RAW_VERIFIED"
        return {
            "schema": "ueassettool.material-contract/v1",
            "status": overall_status,
            "asset_kind": asset_kind,
            "source": _source(package),
            "roots": roots,
            "graph": {
                "status": "VERIFIED" if graph_verified else "UNSUPPORTED" if graph_applicable else "NOT_APPLICABLE",
                "node_count": len(nodes),
                "input_count": input_count,
                "connected_input_count": connected_input_count,
                "link_count": len(links),
                "reference_edge_count": len(reference_edges),
                "expression_collection_count": len(expression_collection),
                "expression_collection_sources": collection_sources,
                "expression_collection_missing": collection_missing,
                "expression_collection_unknown": collection_unknown,
                "expression_collection_duplicates": collection_duplicates,
                "duplicate_expression_guids": duplicate_guids,
                "invalid_links": invalid_links,
                "nodes": nodes,
                "links": links,
                "reference_edges": reference_edges,
                "material_outputs": material_outputs,
            },
            "parameters": parameters,
            "function_inputs": function_inputs,
            "function_outputs": function_outputs,
            "function_calls": function_calls,
            "material_instances": instance_contracts,
            "parameter_collections": collection_contracts,
            "material_parameter_collections": collection_contracts,
            "references": all_references,
            "dependencies": dependencies,
            "unsupported": unsupported,
            "native_serialization": native_serialization,
            "trailing_native": unresolved_trailing_native,
            "semantics": (
                "Exact serialized editor material expression graph and defaults. "
                "This is not generated HLSL and does not claim cooked shader bytecode."
            ),
        }


def export_material_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        return MaterialContractDecoder(package).decode()
