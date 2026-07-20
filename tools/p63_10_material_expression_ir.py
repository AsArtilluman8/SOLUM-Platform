#!/usr/bin/env python3
"""Normalize verified UE material-expression graphs into an exact source-backed IR.

The IR is deliberately not generated HLSL. It preserves Unreal operation identity, every
serialized input/default, switch branch, output index and inter-function call so downstream work
cannot silently replace an authored graph with a visually similar formula.
"""

from __future__ import annotations

import hashlib
import json
from collections import deque
from typing import Any


OPERATION_BY_CLASS = {
    "MaterialExpressionAbs": "ABS",
    "MaterialExpressionAdd": "ADD",
    "MaterialExpressionAppendVector": "APPEND_VECTOR",
    "MaterialExpressionArccosineFast": "ACOS_FAST",
    "MaterialExpressionArctangent2Fast": "ATAN2_FAST",
    "MaterialExpressionCameraVectorWS": "CAMERA_VECTOR_WS",
    "MaterialExpressionClamp": "CLAMP",
    "MaterialExpressionCollectionParameter": "MATERIAL_PARAMETER_COLLECTION",
    "MaterialExpressionComment": "COMMENT_NON_EXECUTABLE",
    "MaterialExpressionComponentMask": "COMPONENT_MASK",
    "MaterialExpressionConstant": "CONSTANT_1",
    "MaterialExpressionConstant2Vector": "CONSTANT_2",
    "MaterialExpressionConstant3Vector": "CONSTANT_3",
    "MaterialExpressionCrossProduct": "CROSS_PRODUCT",
    "MaterialExpressionDBufferTexture": "DBUFFER_TEXTURE",
    "MaterialExpressionDistance": "DISTANCE",
    "MaterialExpressionDivide": "DIVIDE",
    "MaterialExpressionDotProduct": "DOT_PRODUCT",
    "MaterialExpressionFrac": "FRAC",
    "MaterialExpressionFunctionInput": "FUNCTION_INPUT",
    "MaterialExpressionFunctionOutput": "FUNCTION_OUTPUT",
    "MaterialExpressionIf": "IF",
    "MaterialExpressionLength": "LENGTH",
    "MaterialExpressionLinearInterpolate": "LINEAR_INTERPOLATE",
    "MaterialExpressionMaterialFunctionCall": "MATERIAL_FUNCTION_CALL",
    "MaterialExpressionMax": "MAX",
    "MaterialExpressionMultiply": "MULTIPLY",
    "MaterialExpressionNamedRerouteDeclaration": "NAMED_REROUTE_DECLARATION",
    "MaterialExpressionNamedRerouteUsage": "NAMED_REROUTE_USAGE",
    "MaterialExpressionNormalize": "NORMALIZE",
    "MaterialExpressionOneMinus": "ONE_MINUS",
    "MaterialExpressionPanner": "PANNER",
    "MaterialExpressionPower": "POWER",
    "MaterialExpressionQualitySwitch": "QUALITY_SWITCH",
    "MaterialExpressionReflectionCapturePassSwitch": "REFLECTION_CAPTURE_PASS_SWITCH",
    "MaterialExpressionReroute": "REROUTE",
    "MaterialExpressionSaturate": "SATURATE",
    "MaterialExpressionScalarParameter": "SCALAR_PARAMETER",
    "MaterialExpressionShadingPathSwitch": "SHADING_PATH_SWITCH",
    "MaterialExpressionSkyAtmosphereLightIlluminanceOnGround": "SKY_ATMOSPHERE_LIGHT_ILLUMINANCE_ON_GROUND",
    "MaterialExpressionSkyAtmosphereViewLuminance": "SKY_ATMOSPHERE_VIEW_LUMINANCE",
    "MaterialExpressionStaticBool": "STATIC_BOOL",
    "MaterialExpressionStaticSwitch": "STATIC_SWITCH",
    "MaterialExpressionStaticSwitchParameter": "STATIC_SWITCH_PARAMETER",
    "MaterialExpressionSubtract": "SUBTRACT",
    "MaterialExpressionTextureCoordinate": "TEXTURE_COORDINATE",
    "MaterialExpressionTextureObjectParameter": "TEXTURE_OBJECT_PARAMETER",
    "MaterialExpressionTextureSample": "TEXTURE_SAMPLE",
    "MaterialExpressionTextureSampleParameter2D": "TEXTURE_SAMPLE_PARAMETER_2D",
    "MaterialExpressionVectorParameter": "VECTOR_PARAMETER",
    "MaterialExpressionVertexColor": "VERTEX_COLOR",
    "MaterialExpressionVertexInterpolator": "VERTEX_INTERPOLATOR",
    "MaterialExpressionVertexNormalWS": "VERTEX_NORMAL_WS",
    "MaterialExpressionWorldPosition": "WORLD_POSITION",
}

NON_SEMANTIC_ATTRIBUTES = {
    "ExpressionGUID",
    "Function",
    "MaterialExpressionEditorX",
    "MaterialExpressionEditorY",
    "MaterialExpressionGuid",
    "bCollapsed",
}

MATERIAL_EXPRESSIONS_SOURCE = {
    "path": "Engine/Source/Runtime/Engine/Private/Materials/MaterialExpressions.cpp",
    "ref": "5.5",
    "sha": "7c0dc740f553f77e365299e58a533341335827c6",
}

INPUT_DEFAULT_POLICY = {
    "MaterialExpressionAdd": {
        "A": ("ConstA", 0.0),
        "B": ("ConstB", 1.0),
    },
    "MaterialExpressionMultiply": {
        "A": ("ConstA", 0.0),
        "B": ("ConstB", 1.0),
    },
    "MaterialExpressionDivide": {
        "A": ("ConstA", 1.0),
        "B": ("ConstB", 2.0),
    },
    "MaterialExpressionSubtract": {
        "A": ("ConstA", 1.0),
        "B": ("ConstB", 1.0),
    },
    "MaterialExpressionLinearInterpolate": {
        "A": ("ConstA", 0.0),
        "B": ("ConstB", 1.0),
        "Alpha": ("ConstAlpha", 0.5),
    },
    "MaterialExpressionPower": {
        "Exponent": ("ConstExponent", 2.0),
    },
    "MaterialExpressionIf": {
        "B": ("ConstB", 0.0),
        "AEqualsB": (None, "OPTIONAL_NONE"),
    },
}

REQUIRED_INPUTS = {
    "MaterialExpressionPower": ("Base",),
    "MaterialExpressionIf": ("A", "AGreaterThanB", "ALessThanB"),
}

IMPLICIT_ENGINE_PROPERTIES = {
    "MaterialExpressionIf": {"EqualsThreshold": 0.00001},
    "MaterialExpressionFunctionInput": {
        "InputName": "In",
        "InputType": "FunctionInput_Vector3",
        "PreviewValue": {"x": 0.0, "y": 0.0, "z": 0.0, "w": 0.0},
        "bUsePreviewValueAsDefault": False,
    },
    "MaterialExpressionStaticBool": {"Value": False},
    "MaterialExpressionStaticSwitchParameter": {"DefaultValue": False},
    "MaterialExpressionScalarParameter": {"DefaultValue": 0.0},
    "MaterialExpressionVectorParameter": {
        "DefaultValue": {"r": 0.0, "g": 0.0, "b": 0.0, "a": 0.0}
    },
}


def _sha256_json(value: object) -> str:
    encoded = json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _safe_value(value: Any) -> Any:
    if isinstance(value, list):
        return [_safe_value(item) for item in value]
    if not isinstance(value, dict):
        return value
    if value.get("kind") == "expression_input":
        return None
    if "object" in value and set(value).issubset({"object", "package_index", "class"}):
        return value.get("object")
    return {
        key: _safe_value(item)
        for key, item in value.items()
        if key not in {"package_index", "provenance"}
    }


def _semantic_attributes(node: dict[str, Any]) -> dict[str, Any]:
    result = {}
    for key, value in node.get("attributes", {}).items():
        if key in NON_SEMANTIC_ATTRIBUTES:
            continue
        if isinstance(value, dict) and value.get("kind") == "expression_input":
            continue
        if key.startswith("FunctionInputs") or key in {"FunctionInputs", "FunctionOutputs", "Outputs"}:
            continue
        result[key] = _safe_value(value)
    return result


def _source_index(reference: Any) -> int | None:
    if not isinstance(reference, dict):
        return None
    value = reference.get("package_index")
    return value if isinstance(value, int) and value > 0 else None


def _normalized_input(item: dict[str, Any], node_indexes: set[int]) -> dict[str, Any]:
    source_index = _source_index(item.get("expression")) if item.get("connected") else None
    return {
        "slot": item.get("slot"),
        "connected": bool(item.get("connected")),
        "sourceNode": source_index,
        "sourceOutput": item.get("output_index"),
        "sourceResolved": source_index in node_indexes if source_index is not None else not item.get("connected"),
        "serializedInputName": item.get("serialized_input_name"),
        "mask": item.get("mask"),
        "useConstant": item.get("use_constant"),
        "constant": item.get("constant"),
    }


def _complete_input_defaults(
    node_class: str,
    attributes: dict[str, Any],
    inputs: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], list[str]]:
    by_slot = {item["slot"]: item for item in inputs}
    unresolved = []
    for slot, (property_name, engine_default) in INPUT_DEFAULT_POLICY.get(node_class, {}).items():
        item = by_slot.get(slot)
        if item is None:
            item = {
                "slot": slot,
                "connected": False,
                "sourceNode": None,
                "sourceOutput": None,
                "sourceResolved": True,
                "serializedInputName": None,
                "mask": None,
                "useConstant": None,
                "constant": None,
            }
            inputs.append(item)
            by_slot[slot] = item
        if item["connected"]:
            item["valueResolved"] = item["sourceResolved"]
            continue
        if property_name and property_name in attributes:
            item["resolvedDefault"] = attributes[property_name]
            item["defaultSource"] = f"serialized {property_name}"
        else:
            item["resolvedDefault"] = engine_default
            item["defaultSource"] = (
                f"UE 5.5 constructor default: {MATERIAL_EXPRESSIONS_SOURCE['path']}@{MATERIAL_EXPRESSIONS_SOURCE['sha']}"
            )
        item["valueResolved"] = True
    for slot in REQUIRED_INPUTS.get(node_class, ()):
        item = by_slot.get(slot)
        if item is None or not item.get("connected") or not item.get("sourceResolved"):
            unresolved.append(slot)
    for item in inputs:
        item.setdefault("valueResolved", item["sourceResolved"] if item["connected"] else None)
    return inputs, unresolved


def _call_outputs(attributes: dict[str, Any]) -> list[dict[str, Any]]:
    container = attributes.get("FunctionOutputs") or attributes.get("Outputs") or {}
    items = container.get("items", []) if isinstance(container, dict) else []
    outputs = []
    for index, item in enumerate(items):
        fields = item.get("fields", {}) if isinstance(item, dict) else {}
        output = fields.get("Output", fields)
        if isinstance(output, dict) and "fields" in output:
            output = output["fields"]
        outputs.append(
            {
                "index": index,
                "id": fields.get("ExpressionOutputId"),
                "name": output.get("OutputName") if isinstance(output, dict) else None,
            }
        )
    return outputs


def _call_input_ids(attributes: dict[str, Any]) -> list[dict[str, Any]]:
    container = attributes.get("FunctionInputs", {})
    items = container.get("items", []) if isinstance(container, dict) else []
    result = []
    for index, item in enumerate(items):
        fields = item.get("fields", {}) if isinstance(item, dict) else {}
        result.append({"index": index, "id": fields.get("ExpressionInputId")})
    return result


def _target_function(attributes: dict[str, Any]) -> str | None:
    value = attributes.get("MaterialFunction")
    if not isinstance(value, dict):
        return None
    target = value.get("object")
    if not isinstance(target, str):
        return None
    return target.rsplit(".", 1)[0]


def _semantic_property(node: dict[str, Any], name: str) -> tuple[Any, str | None]:
    attributes = node.get("semanticAttributes", {})
    if name in attributes:
        return attributes[name], "serialized target FunctionInput property"
    implicit = attributes.get("implicitEngineProperties", {}).get("values", {})
    if name in implicit:
        return implicit[name], "UE 5.5 target FunctionInput engine default"
    return None, None


def resolve_inter_function_defaults(
    programs: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """Close call inputs through the callee's exact FunctionInput preview contract.

    UE permits an unconnected UMaterialExpressionMaterialFunctionCall input only when the target
    UMaterialExpressionFunctionInput explicitly enables its preview value as the default. The
    binding remains a cross-program expression reference; it is not constant-folded here.
    """
    by_asset = {program["sourceAsset"]: program for program in programs}
    for program in programs:
        unresolved_bindings = []
        for call in (node for node in program["nodes"] if node["operation"] == "MATERIAL_FUNCTION_CALL"):
            bindings = []
            target = by_asset.get(call.get("calledFunction"))
            target_inputs = {}
            if target:
                target_inputs = {
                    node.get("semanticAttributes", {}).get("Id"): node
                    for node in target["nodes"]
                    if node["operation"] == "FUNCTION_INPUT"
                    and node.get("semanticAttributes", {}).get("Id")
                }
            actual_by_index = {}
            for item in call.get("inputs", []):
                slot = item.get("slot") or ""
                if slot.startswith("FunctionInputs[") and slot.endswith("].Input"):
                    try:
                        actual_by_index[int(slot[len("FunctionInputs[") : slot.index("]")])] = item
                    except ValueError:
                        pass
            for call_input in call.get("callInputs", []):
                index = call_input.get("index")
                input_id = call_input.get("id")
                actual = actual_by_index.get(index)
                binding = {
                    "callInputIndex": index,
                    "callInputId": input_id,
                    "callInputName": actual.get("serializedInputName") if actual else None,
                    "targetAsset": call.get("calledFunction"),
                }
                if actual and actual.get("connected"):
                    binding.update(
                        {
                            "status": (
                                "VERIFIED_EXPLICIT_CALLER_EXPRESSION"
                                if actual.get("sourceResolved")
                                else "UNRESOLVED_CALLER_EXPRESSION"
                            ),
                            "sourceNode": actual.get("sourceNode"),
                            "sourceOutput": actual.get("sourceOutput"),
                        }
                    )
                    actual["valueResolved"] = bool(actual.get("sourceResolved"))
                else:
                    target_input = target_inputs.get(input_id)
                    use_preview, use_preview_source = (
                        _semantic_property(target_input, "bUsePreviewValueAsDefault")
                        if target_input
                        else (None, None)
                    )
                    preview = next(
                        (
                            item
                            for item in target_input.get("inputs", [])
                            if item.get("slot") == "Preview"
                        ),
                        None,
                    ) if target_input else None
                    if use_preview is True and preview and preview.get("connected") and preview.get("sourceResolved"):
                        resolved_default = {
                            "kind": "TARGET_FUNCTION_PREVIEW_EXPRESSION",
                            "targetAsset": target["sourceAsset"],
                            "targetFunctionInputNode": target_input["sourceNode"],
                            "targetPreviewSourceNode": preview["sourceNode"],
                            "targetPreviewSourceOutput": preview["sourceOutput"],
                        }
                        binding.update(
                            {
                                "status": "VERIFIED_TARGET_PREVIEW_EXPRESSION",
                                "targetFunctionInputNode": target_input["sourceNode"],
                                "targetPreviewSourceNode": preview["sourceNode"],
                                "targetPreviewSourceOutput": preview["sourceOutput"],
                                "usePreviewSource": use_preview_source,
                            }
                        )
                    elif use_preview is True and (preview is None or not preview.get("connected")):
                        preview_value, preview_value_source = _semantic_property(
                            target_input, "PreviewValue"
                        )
                        if preview_value_source:
                            resolved_default = {
                                "kind": "TARGET_FUNCTION_PREVIEW_VALUE",
                                "targetAsset": target["sourceAsset"],
                                "targetFunctionInputNode": target_input["sourceNode"],
                                "value": preview_value,
                            }
                            binding.update(
                                {
                                    "status": "VERIFIED_TARGET_PREVIEW_VALUE",
                                    "targetFunctionInputNode": target_input["sourceNode"],
                                    "targetPreviewValue": preview_value,
                                    "previewValueSource": preview_value_source,
                                    "usePreviewSource": use_preview_source,
                                }
                            )
                        else:
                            resolved_default = None
                            binding["status"] = "UNRESOLVED_TARGET_PREVIEW_VALUE"
                    else:
                        resolved_default = None
                        binding["status"] = "UNRESOLVED_TARGET_FUNCTION_INPUT"
                    if actual is not None:
                        actual["valueResolved"] = resolved_default is not None
                        if resolved_default is not None:
                            actual["resolvedDefault"] = resolved_default
                            actual["defaultSource"] = binding["status"]
                if not binding["status"].startswith("VERIFIED_"):
                    unresolved_bindings.append(
                        {
                            "callNode": call["sourceNode"],
                            **binding,
                        }
                    )
                bindings.append(binding)
            call["resolvedCallBindings"] = bindings
        program["unresolvedCallBindings"] = unresolved_bindings
        if unresolved_bindings:
            program["status"] = "PARTIAL"
        program.pop("programSha256", None)
        program["programSha256"] = _sha256_json(program)
    return programs


def build_expression_program(source_asset: str, contract: dict[str, Any]) -> dict[str, Any]:
    graph = contract.get("graph", {})
    raw_nodes = graph.get("nodes", [])
    indexes = {item.get("export_index") for item in raw_nodes if isinstance(item.get("export_index"), int)}
    unknown_classes = sorted({item.get("class") for item in raw_nodes} - set(OPERATION_BY_CLASS))
    nodes = []
    unresolved_inputs = []
    unresolved_engine_defaults = []
    calls = []
    for item in raw_nodes:
        export_index = item.get("export_index")
        inputs = [_normalized_input(value, indexes) for value in item.get("inputs", [])]
        attributes = item.get("attributes", {})
        if item.get("class") == "MaterialExpressionNamedRerouteUsage":
            declaration = _source_index(attributes.get("Declaration"))
            inputs.append(
                {
                    "slot": "Declaration",
                    "connected": declaration is not None,
                    "sourceNode": declaration,
                    "sourceOutput": 0,
                    "sourceResolved": declaration in indexes if declaration is not None else False,
                    "serializedInputName": None,
                    "mask": None,
                    "useConstant": None,
                    "constant": None,
                }
            )
        inputs, missing_required = _complete_input_defaults(
            item.get("class", ""), attributes, inputs
        )
        unresolved_engine_defaults.extend(
            {"node": export_index, "slot": slot}
            for slot in missing_required
        )
        unresolved_inputs.extend(
            {"node": export_index, "slot": value["slot"], "sourceNode": value["sourceNode"]}
            for value in inputs
            if value["connected"] and not value["sourceResolved"]
        )
        semantic_attributes = _semantic_attributes(item)
        implicit_properties = {
            key: value
            for key, value in IMPLICIT_ENGINE_PROPERTIES.get(item.get("class", ""), {}).items()
            if key not in attributes
        }
        if implicit_properties:
            semantic_attributes["implicitEngineProperties"] = {
                "values": implicit_properties,
                "source": MATERIAL_EXPRESSIONS_SOURCE,
            }
        node = {
            "sourceNode": export_index,
            "sourceName": item.get("name"),
            "sourceClass": item.get("class"),
            "operation": OPERATION_BY_CLASS.get(item.get("class")),
            "semanticAttributes": semantic_attributes,
            "inputs": inputs,
            "parseStatus": item.get("parse_status"),
        }
        if item.get("class") == "MaterialExpressionMaterialFunctionCall":
            node["calledFunction"] = _target_function(attributes)
            node["callInputs"] = _call_input_ids(attributes)
            node["callOutputs"] = _call_outputs(attributes)
            calls.append(node["calledFunction"])
        nodes.append(node)

    by_index = {item["sourceNode"]: item for item in nodes}
    outputs = []
    roots = []
    for node in nodes:
        if node["sourceClass"] != "MaterialExpressionFunctionOutput":
            continue
        source = next((value for value in node["inputs"] if value["slot"] == "A"), None)
        attributes = node["semanticAttributes"]
        outputs.append(
            {
                "sourceNode": node["sourceNode"],
                "id": attributes.get("Id"),
                "name": attributes.get("OutputName") or "Result",
                "description": attributes.get("Description") or attributes.get("Desc"),
                "expression": source,
            }
        )
        if source and source.get("sourceNode") is not None:
            roots.append(source["sourceNode"])

    reachable: set[int] = set()
    queue = deque(roots)
    while queue:
        index = queue.popleft()
        if index in reachable or index not in by_index:
            continue
        reachable.add(index)
        queue.extend(
            value["sourceNode"]
            for value in by_index[index]["inputs"]
            if value.get("sourceNode") is not None
        )
    executable = {
        item["sourceNode"]
        for item in nodes
        if item["sourceClass"] != "MaterialExpressionComment"
    }
    output_nodes = {item["sourceNode"] for item in outputs}
    dead_nodes = sorted(executable - reachable - output_nodes)
    unresolved_outputs = [
        item["sourceNode"]
        for item in outputs
        if not item.get("expression")
        or not item["expression"].get("connected")
        or not item["expression"].get("sourceResolved")
    ]
    graph_issues = {
        "invalidLinks": graph.get("invalid_links", []),
        "missingExpressionCollections": graph.get("expression_collection_missing", []),
        "unknownExpressionCollections": graph.get("expression_collection_unknown", []),
        "duplicateExpressions": graph.get("expression_collection_duplicates", []),
        "duplicateExpressionGuids": graph.get("duplicate_expression_guids", []),
    }
    called = sorted({value for value in calls if value})
    blocking_graph_issues = {
        key: value
        for key, value in graph_issues.items()
        if key != "duplicateExpressionGuids" and value
    }
    verified = (
        contract.get("status") == "VERIFIED"
        and graph.get("status") == "VERIFIED"
        and not unknown_classes
        and not unresolved_inputs
        and not unresolved_engine_defaults
        and not unresolved_outputs
        and not blocking_graph_issues
    )
    program = {
        "schema": "solum.p63.10.ue-material-expression-program/v1",
        "sourceAsset": source_asset,
        "status": "VERIFIED" if verified else "PARTIAL",
        "semanticLevel": "EXACT_NORMALIZED_UE_EXPRESSION_DAG_NOT_GENERATED_HLSL",
        "ueSemanticsSource": {
            "engine": contract.get("source", {}).get("engine"),
            "materialExpressionApi": "Engine/Source/Runtime/Engine/Public/Materials/MaterialExpression*.h",
            "interpretation": "operation identity is preserved from serialized UMaterialExpression class",
        },
        "nodes": nodes,
        "outputs": outputs,
        "calledFunctions": called,
        "reachableExpressionNodes": sorted(reachable),
        "deadOrPreviewExpressionNodes": dead_nodes,
        "unknownExpressionClasses": unknown_classes,
        "unresolvedInputs": unresolved_inputs,
        "unresolvedEngineDefaults": unresolved_engine_defaults,
        "unresolvedOutputs": unresolved_outputs,
        "graphIssues": graph_issues,
        "blockingGraphIssues": blocking_graph_issues,
        "nonBlockingSourceWarnings": {
            "duplicateExpressionGuids": graph_issues["duplicateExpressionGuids"],
            "interpretation": "serialized export indexes remain unique; duplicated editor GUIDs do not break expression links",
        },
    }
    program["programSha256"] = _sha256_json(program)
    return program
