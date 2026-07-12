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
        if int(value["output_index"]) < 0:
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
            for item in decoded.get("properties", []):
                if not str(item.get("decode_status", "")).startswith("decoded"):
                    unsupported.append({
                        "export_index": index,
                        "object": package.object_path(index),
                        "class": export.class_name,
                        "property": item.get("name"),
                        "type": item.get("type", {}).get("display"),
                        "reason": item.get("decode_note"),
                        "provenance": _provenance(item),
                    })
            if export.class_name in MATERIAL_ROOT_CLASSES:
                roots.append({
                    "export_index": index,
                    "object": package.object_path(index),
                    "class": export.class_name,
                    "attributes": _attributes(decoded.get("properties", [])),
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
        overall_status = "UNSUPPORTED" if not nodes else "RAW_VERIFIED" if trailing_native else "VERIFIED"
        if unsupported:
            overall_status = "RAW_VERIFIED"
        return {
            "schema": "ueassettool.material-contract/v1",
            "status": overall_status,
            "source": _source(package),
            "roots": roots,
            "graph": {
                "status": "VERIFIED" if graph_verified else "UNSUPPORTED",
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
            "references": all_references,
            "dependencies": dependencies,
            "unsupported": unsupported,
            "trailing_native": trailing_native,
            "semantics": (
                "Exact serialized editor material expression graph and defaults. "
                "This is not generated HLSL and does not claim cooked shader bytecode."
            ),
        }


def export_material_contract(path: str | Path) -> dict[str, Any]:
    with UnrealPackage(path) as package:
        return MaterialContractDecoder(package).decode()
