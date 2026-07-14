#!/usr/bin/env python3
"""Build the local P62 UDS/UDW visual-truth preview without invented effects."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from collections import Counter
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
UE_TOOL_SRC = REPO_ROOT / "tools" / "ue_asset_tool" / "src"
if str(UE_TOOL_SRC) not in sys.path:
    sys.path.insert(0, str(UE_TOOL_SRC))

from ueassettool.blueprint import BlueprintGraphDecoder  # noqa: E402
from ueassettool.media import validate_wav  # noqa: E402
from ueassettool.package import UnrealPackage  # noqa: E402
from ueassettool.properties import PropertyParser  # noqa: E402


SCHEMA_VERSION = "solum.uds-visual/v1"
ROOT_PACKAGES = (
    "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Sky",
    "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather",
    "/Game/UltraDynamicSky/Blueprints/Configuration_Manager",
    "/Game/UltraDynamicSky/Blueprints/Volumetric_Cloud_Painter",
    "/Game/UltraDynamicSky/Blueprints/Mini_Controls",
    "/Game/UltraDynamicSky/Blueprints/Weather_Effects/Rain_Drip_Spline",
)
WEATHER_PREFIX = "/Game/UltraDynamicSky/Blueprints/Weather_Effects/Weather_Presets/"
VISUAL_SYSTEMS = (
    "time", "sun", "moon", "stars", "atmosphere", "clouds", "fog",
    "rain", "snow", "wind", "lightning", "wetness", "transitions", "audio",
)
ACTIVE_CONTROLS = ("time_of_day", "weather_preset", "camera", "panel", "reset", "export")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as source:
        return json.load(source)


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    path.write_text(text, encoding="utf-8")


def clean_value(value: Any) -> Any:
    """Remove host paths while preserving package/object identities and offsets."""
    if isinstance(value, dict):
        return {
            key: clean_value(item)
            for key, item in sorted(value.items())
            if key not in {"source_file", "path", "payload_source", "source_path"}
        }
    if isinstance(value, list):
        return [clean_value(item) for item in value]
    if isinstance(value, str) and value.startswith(("/mnt/", "/data/", "/storage/", "/sdcard/", "/home/")):
        return "<redacted-host-path>"
    return value


def decoded_properties(decoded: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(prop["name"]): prop
        for prop in decoded.get("properties", [])
        if str(prop.get("decode_status", "")).startswith("decoded")
    }


def decoded_fields(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {}
    return {
        str(prop["name"]): prop.get("value")
        for prop in value.get("properties", [])
        if str(prop.get("decode_status", "")).startswith("decoded")
    }


def classify_system(name: str, package: str) -> str:
    text = name.lower()
    rules = (
        ("transitions", ("transition", "lerp", "state change")),
        ("audio", ("audio", "sound", "thunder volume")),
        ("lightning", ("lightning", "thunder", "light flash")),
        ("wetness", ("wet", "dry", "moisture", "puddle", "material snow", "material dust")),
        ("rain", ("rain", "drip", "splash")),
        ("snow", ("snow", "blizzard", "frost")),
        ("wind", ("wind", "gust")),
        ("clouds", ("cloud", "wisps", "coverage")),
        ("moon", ("moon", "lunar")),
        ("stars", ("star", "twinkle")),
        ("sun", ("sun", "dawn", "dusk", "directional light")),
        ("time", ("time of day", "animate time", "day length", "time speed")),
        ("fog", ("fog",)),
        ("atmosphere", ("atmosphere", "sky light", "sky atmosphere")),
    )
    for system, tokens in rules:
        if any(token in text for token in tokens):
            return system
    if package.endswith("Rain_Drip_Spline"):
        return "rain"
    if package.endswith("Volumetric_Cloud_Painter"):
        return "clouds"
    return "configuration"


def property_evidence(
    package_name: str,
    source_sha256: str,
    object_path: str,
    export_index: int,
    prop: dict[str, Any],
) -> dict[str, Any]:
    raw = prop.get("raw", {})
    return {
        "source_package": package_name,
        "source_sha256": source_sha256,
        "source_object": object_path,
        "export_index": export_index,
        "property": prop.get("name"),
        "property_type": prop.get("type", {}).get("display"),
        "header_physical_offset": prop.get("header_physical_offset"),
        "value_physical_offset": raw.get("physical_offset"),
        "size": raw.get("size"),
        "value_sha256": raw.get("sha256"),
        "decode_status": prop.get("decode_status"),
    }


def stable_parameter_id(package_name: str, object_path: str, property_name: str) -> str:
    digest = hashlib.sha256(f"{package_name}\0{object_path}\0{property_name}".encode()).hexdigest()[:16]
    slug = "".join(char.lower() if char.isalnum() else "_" for char in property_name).strip("_")
    while "__" in slug:
        slug = slug.replace("__", "_")
    return f"uds.{slug or 'property'}.{digest}"


def cdo_contract(record: dict[str, Any]) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    source_path = Path(record["path"])
    expected_sha = record["sha256"]
    if sha256_file(source_path) != expected_sha:
        raise ValueError(f"source hash mismatch for {record['package_name']}")
    with UnrealPackage(source_path) as package:
        expected_cdo = f"Default__{record['package_name'].rsplit('/', 1)[-1]}_C"
        candidates = [
            index for index, export in enumerate(package.exports, 1)
            if export.object_name.display == expected_cdo
        ]
        if len(candidates) != 1:
            raise ValueError(f"expected one CDO in {record['package_name']}, found {len(candidates)}")
        index = candidates[0]
        decoded = PropertyParser(package).parse_export(index)
        object_path = package.object_path(index)
        parameters = []
        for name, prop in sorted(decoded_properties(decoded).items()):
            system = classify_system(name, record["package_name"])
            parameters.append({
                "id": stable_parameter_id(record["package_name"], object_path, name),
                "system": system,
                "name": name,
                "type": prop.get("type", {}).get("display"),
                "unit": infer_unit(name),
                "default": clean_value(prop.get("value")),
                "default_or_override": "class_default_object",
                "curve_or_timeline": None,
                "target": None,
                "evidence_status": "VERIFIED",
                "browser_support_status": "STATE_ONLY" if name == "Time of Day" else "UNSUPPORTED_HTML",
                "evidence": property_evidence(
                    record["package_name"], expected_sha, object_path, index, prop,
                ),
            })
        summary = {
            "package": record["package_name"],
            "source_sha256": expected_sha,
            "source_size": record["size"],
            "cdo_object": object_path,
            "cdo_export_index": index,
            "decoded_property_count": len(parameters),
            "parse_status": decoded.get("parse_status"),
            "trailing_native": clean_value(decoded.get("trailing_native")),
            "export_table": {
                "export_count": len(package.exports),
                "k2_node_count": sum((item.class_name or "").startswith("K2Node_") for item in package.exports),
                "function_export_count": sum(item.class_name == "Function" for item in package.exports),
                "timeline_export_count": sum(item.class_name == "TimelineTemplate" for item in package.exports),
            },
            "evidence_status": "PARTIAL" if decoded.get("trailing_native") else "VERIFIED",
            "reason": "decoded tagged CDO defaults; native C++ and remaining native suffix are not decompiled",
        }
        return summary, parameters


def infer_unit(name: str) -> str | None:
    text = name.lower()
    if any(token in text for token in ("duration", "period", "delay")):
        return "seconds"
    if any(token in text for token in ("direction", "pitch", "yaw", "angle")):
        return "degrees"
    if "time of day" in text or name in ("Dawn Time", "Dusk Time"):
        return "hundredths_of_hour"
    if any(token in text for token in ("height", "distance", "length", "velocity")):
        return "unreal_units"
    if any(token in text for token in ("intensity", "coverage", "density", "opacity", "scale", "frequency")):
        return "source_scalar"
    return None


def variable_metadata(record: dict[str, Any], wanted: set[str]) -> list[dict[str, Any]]:
    """Decode Blueprint NewVariables metadata, including exact UI ranges/units."""
    with UnrealPackage(record["path"]) as package:
        candidates = [
            index for index, export in enumerate(package.exports, 1)
            if export.class_name == "Blueprint" and export.is_asset
        ]
        if len(candidates) != 1:
            return []
        decoded = PropertyParser(package).parse_export(candidates[0])
        new_variables = decoded_properties(decoded).get("NewVariables")
        if not new_variables:
            return []
        result = []
        value = new_variables.get("value", {})
        for item in value.get("items", []) if isinstance(value, dict) else []:
            fields = decoded_fields(item)
            name = fields.get("VarName")
            if name not in wanted:
                continue
            metadata: dict[str, str] = {}
            meta = fields.get("MetaDataArray")
            if isinstance(meta, dict):
                for entry in meta.get("items", []):
                    pair = decoded_fields(entry)
                    if isinstance(pair.get("DataKey"), str) and isinstance(pair.get("DataValue"), str):
                        metadata[pair["DataKey"]] = pair["DataValue"]
            result.append({
                "name": name,
                "guid": fields.get("VarGuid"),
                "friendly_name": fields.get("FriendlyName"),
                "category": clean_value(fields.get("Category")),
                "default_value": fields.get("DefaultValue"),
                "metadata": dict(sorted(metadata.items())),
                "container_evidence": {
                    "source_package": record["package_name"],
                    "source_sha256": record["sha256"],
                    "source_object": package.object_path(candidates[0]),
                    "export_index": candidates[0],
                    "property": "NewVariables",
                    "header_physical_offset": new_variables.get("header_physical_offset"),
                    "value_physical_offset": new_variables.get("raw", {}).get("physical_offset"),
                    "size": new_variables.get("raw", {}).get("size"),
                    "value_sha256": new_variables.get("raw", {}).get("sha256"),
                },
                "evidence_status": "VERIFIED",
            })
        return result


def transition_node_contract(record: dict[str, Any]) -> dict[str, Any]:
    expected = {
        1653: "Global State Update - Apply Transition.K2Node_CallFunction_7",
        2976: "Global State Update - Apply Transition.K2Node_EaseFunction_1",
        5827: "Global State Update - Apply Transition.K2Node_VariableGet_16",
        5828: "Global State Update - Apply Transition.K2Node_VariableGet_17",
        7696: "Global State Update - Apply Transition.K2Node_VariableSet_3",
    }
    with UnrealPackage(record["path"]) as package:
        decoder = BlueprintGraphDecoder(package)
        nodes = []
        for index, suffix in expected.items():
            if index > len(package.exports) or not package.object_path(index).endswith(suffix):
                raise ValueError(f"transition node identity mismatch at export {index}")
            node = decoder.parse_node(index)
            pins = []
            for pin in node.get("pins", []):
                if not pin:
                    continue
                pins.append({
                    "name": pin.get("name"),
                    "direction": pin.get("direction"),
                    "type": clean_value(pin.get("type")),
                    "default_value": pin.get("default_value"),
                    "links": [
                        {"owning_node_index": link.get("owning_node_index"), "pin_id": link.get("pin_id")}
                        for link in pin.get("linked_to", []) if link
                    ],
                    "provenance": clean_value(pin.get("provenance")),
                })
            nodes.append({
                "export_index": index,
                "object": node["object"],
                "class": node["class"],
                "properties": clean_value(node.get("properties")),
                "property_provenance": clean_value(node.get("property_provenance")),
                "pins": pins,
                "pin_decode_status": node.get("pin_decode_status"),
            })
    return {
        "status": "PARTIAL",
        "source_package": record["package_name"],
        "source_sha256": record["sha256"],
        "graph": "Global State Update - Apply Transition",
        "nodes": nodes,
        "verified_operations": [
            "SafeDivide(CurrentTransitionTime, TransitionDuration)",
            "EaseInOut(A=0, B=1, BlendExp=2, ShortestPath=true)",
            "result stored as CurrentLerpAlpha",
        ],
        "browser_adapter": {
            "status": "SOURCE_VERIFIED_BROWSER_ADAPTER",
            "formula": "a<0.5 ? 0.5*(2*a)^2 : 1-0.5*(2*(1-a))^2",
            "limitation": "preset Transition Duration=-1 delegates duration selection; browser transition control stays locked",
        },
    }


def time_animation_node_contract(record: dict[str, Any]) -> dict[str, Any]:
    expected = {
        4090: "Time of Day Animation.K2Node_CallFunction_0",
        4091: "Time of Day Animation.K2Node_CallFunction_24",
        4092: "Time of Day Animation.K2Node_CallFunction_3",
        4778: "Time of Day Animation.K2Node_CommutativeAssociativeBinaryOperator_0",
        5782: "Time of Day Animation.K2Node_FunctionEntry_0",
        6588: "Time of Day Animation.K2Node_IfThenElse_1",
        6845: "Time of Day Animation.K2Node_Knot_1",
        12081: "Time of Day Animation.K2Node_VariableGet_2",
        12082: "Time of Day Animation.K2Node_VariableGet_7",
    }
    with UnrealPackage(record["path"]) as package:
        decoder = BlueprintGraphDecoder(package)
        nodes = []
        for index, suffix in expected.items():
            if index > len(package.exports) or not package.object_path(index).endswith(suffix):
                raise ValueError(f"time animation node identity mismatch at export {index}")
            node = decoder.parse_node(index)
            nodes.append({
                "export_index": index,
                "object": node["object"],
                "class": node["class"],
                "properties": clean_value(node.get("properties")),
                "property_provenance": clean_value(node.get("property_provenance")),
                "pins": [
                    {
                        "name": pin.get("name"),
                        "direction": pin.get("direction"),
                        "default_value": pin.get("default_value"),
                        "links": [
                            {"owning_node_index": link.get("owning_node_index"), "pin_id": link.get("pin_id")}
                            for link in pin.get("linked_to", []) if link
                        ],
                        "provenance": clean_value(pin.get("provenance")),
                    }
                    for pin in node.get("pins", []) if pin
                ],
                "pin_decode_status": node.get("pin_decode_status"),
            })
    return {
        "status": "PARTIAL",
        "source_package": record["package_name"],
        "source_sha256": record["sha256"],
        "graph": "Time of Day Animation",
        "nodes": nodes,
        "verified_operations": [
            "skip animation while Transitioning Time is true",
            "Amount = Tick Delta Seconds * Get Cached Float(Property=NewEnumerator136)",
            "call Increment Time of Day Forward(Amount)",
        ],
        "blocker": "cached-float enum NewEnumerator136 is not semantically resolved, and Increment Time of Day Forward remains a larger partial graph",
        "browser_support_status": "STATE_ONLY",
    }


def weather_presets(package_records: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    presets = []
    for package_name in sorted(name for name in package_records if name.startswith(WEATHER_PREFIX)):
        record = package_records[package_name]
        with UnrealPackage(record["path"]) as package:
            candidates = [
                index for index, export in enumerate(package.exports, 1)
                if export.class_name == "UDS_Weather_Settings_C" and export.is_asset
            ]
            if len(candidates) != 1:
                raise ValueError(f"expected one weather setting export in {package_name}")
            index = candidates[0]
            decoded = PropertyParser(package).parse_export(index)
            props = decoded_properties(decoded)
            values = {
                name: clean_value(prop.get("value"))
                for name, prop in sorted(props.items())
                if name not in {"Icon Texture", "Symbol Texture", "NativeClass"}
            }
            evidence = {
                name: property_evidence(
                    package_name, record["sha256"], package.object_path(index), index, prop,
                )
                for name, prop in sorted(props.items())
                if name in values
            }
            friendly = values.get("User Friendly Name")
            if isinstance(friendly, dict):
                friendly = friendly.get("source")
            presets.append({
                "id": package_name.rsplit("/", 1)[-1],
                "name": friendly or package_name.rsplit("/", 1)[-1].replace("_", " "),
                "source_package": package_name,
                "source_object": package.object_path(index),
                "source_sha256": record["sha256"],
                "export_index": index,
                "values": values,
                "evidence": evidence,
                "evidence_status": "VERIFIED",
                "browser_support_status": "STATE_ONLY",
            })
    if not presets:
        raise ValueError("no weather presets decoded")
    return presets


def curve_contracts(p60: Path) -> list[dict[str, Any]]:
    result = []
    for path in sorted((p60 / "curves").glob("*.json")):
        wrapper = read_json(path)
        contract = wrapper["contract"]
        if contract.get("status") != "VERIFIED":
            continue
        result.append({
            "id": path.stem,
            "source_package": contract.get("source", {}).get("package"),
            "source_sha256": contract.get("source", {}).get("sha256"),
            "asset": clean_value(contract.get("asset")),
            "channels": clean_value(contract.get("channels", [])),
            "channel_count": contract.get("channel_count"),
            "total_key_count": contract.get("total_key_count"),
            "evidence_status": "VERIFIED",
        })
    return result


def material_contracts(p60: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    summaries: list[dict[str, Any]] = []
    collections: list[dict[str, Any]] = []
    for path in sorted((p60 / "materials").glob("*.json")):
        wrapper = read_json(path)
        contract = wrapper["contract"]
        metrics = wrapper.get("metrics", {})
        source = contract.get("source", {})
        summaries.append({
            "id": path.stem,
            "source_package": source.get("package"),
            "source_sha256": source.get("sha256"),
            "contract_sha256": sha256_file(path),
            "status": contract.get("status"),
            "metrics": clean_value(metrics),
            "root_objects": [
                {"object": item.get("object"), "class": item.get("class"), "export_index": item.get("export_index")}
                for item in contract.get("roots", [])
            ],
            "unsupported": clean_value(contract.get("unsupported", [])),
        })
        for collection in contract.get("parameter_collections", []):
            collections.append({
                "source_package": source.get("package"),
                "source_sha256": source.get("sha256"),
                "object": collection.get("object"),
                "state_id": clean_value(collection.get("state_id")),
                "scalar_parameters": clean_value(collection.get("scalar_parameters", [])),
                "vector_parameters": clean_value(collection.get("vector_parameters", [])),
                "evidence_status": contract.get("status"),
            })
    return summaries, collections


def decoded_niagara_parameters(contract: dict[str, Any]) -> dict[str, Any] | None:
    """Select the largest decoded Niagara Parameters store, matching P60 metrics."""
    candidates: list[dict[str, Any]] = []

    def visit(value: Any, export: dict[str, Any], path: tuple[str, ...]) -> None:
        if isinstance(value, dict):
            decoded = value.get("value")
            if (
                value.get("name") == "Parameters"
                and isinstance(decoded, dict)
                and isinstance(decoded.get("items"), list)
            ):
                candidates.append({
                    "source_export": {
                        "object": export.get("object"),
                        "class": export.get("class"),
                        "export_index": export.get("export_index"),
                    },
                    "container_path": ".".join(path),
                    "container_evidence": {
                        "type": clean_value(value.get("type")),
                        "decode_status": value.get("decode_status"),
                        "header_physical_offset": value.get("header_physical_offset"),
                        "raw": clean_value(value.get("raw")),
                    },
                    "parameters": clean_value(decoded["items"]),
                })
            for key, item in value.items():
                visit(item, export, path + (str(key),))
        elif isinstance(value, list):
            for index, item in enumerate(value):
                visit(item, export, path + (str(index),))

    for index, export in enumerate(contract.get("exports", [])):
        visit(export.get("properties", []), export, ("exports", str(index), "properties"))
    if not candidates:
        return None
    selected = max(candidates, key=lambda item: len(item["parameters"]))
    selected["parameter_count"] = len(selected["parameters"])
    return selected


def niagara_contracts(p60: Path) -> list[dict[str, Any]]:
    result = []
    for path in sorted((p60 / "niagara").glob("*.json")):
        wrapper = read_json(path)
        contract = wrapper["contract"]
        source = contract.get("source", {})
        scripts = contract.get("scripts", [])
        systems = contract.get("systems", [])
        parameter_store = decoded_niagara_parameters(contract)
        expected_parameter_count = int(wrapper.get("metrics", {}).get("niagara_parameters", 0))
        decoded_parameter_count = parameter_store["parameter_count"] if parameter_store else 0
        if decoded_parameter_count != expected_parameter_count:
            raise ValueError(
                f"Niagara parameter count mismatch for {path.name}: "
                f"decoded={decoded_parameter_count}, P60={expected_parameter_count}"
            )
        result.append({
            "id": path.stem,
            "source_package": source.get("package"),
            "source_sha256": source.get("sha256"),
            "contract_sha256": sha256_file(path),
            "status": contract.get("status"),
            "coverage": clean_value(contract.get("coverage")),
            "niagara_parameter_count": decoded_parameter_count,
            "decoded_parameter_store": parameter_store,
            "systems": [
                {
                    "object": item.get("object"),
                    "status": item.get("status"),
                    "emitter_count": len(item.get("emitters", [])),
                    "parameter_store": clean_value(item.get("parameter_store")),
                }
                for item in systems
            ],
            "scripts": [
                {
                    "object": item.get("object"),
                    "status": item.get("status"),
                    "usage": item.get("usage"),
                    "graph": clean_value(item.get("graph")),
                    "inputs": clean_value(item.get("inputs", [])),
                    "outputs": clean_value(item.get("outputs", [])),
                    "operations": clean_value(item.get("operations", [])),
                    "rapid_iteration_parameters": clean_value(item.get("rapid_iteration_parameters")),
                    "vm": clean_value(item.get("vm")),
                }
                for item in scripts
            ],
        })
    return result


def audio_assets(
    p60: Path, output_assets: Path, package_records: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    result = []
    source_dir = p60 / "media" / "audio"
    for source in sorted(source_dir.glob("*.wav")):
        target_name = source.name
        target = output_assets / target_name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        validation = validate_wav(target.read_bytes())
        asset_record = read_json(p60 / "assets" / f"{source.stem}.json")
        matches = [
            item for item in package_records.values()
            if item.get("sha256") == asset_record.get("source_sha256")
        ]
        if len(matches) != 1:
            raise ValueError(f"audio source {source.stem} does not resolve uniquely in P61")
        result.append({
            "id": source.stem,
            "kind": "audio",
            "browser_path": f"assets/{target_name}",
            "source_asset_object": asset_record.get("verified_fields", {}).get("asset_object"),
            "source_package": matches[0]["package_name"],
            "source_sha256": asset_record.get("source_sha256"),
            "output_sha256": sha256_file(target),
            "size": target.stat().st_size,
            "format": "WAV",
            "format_validation": validation,
            "lossless": True,
            "payload_status": "VERIFIED",
            "event_binding_status": "UNKNOWN",
            "browser_active": False,
            "reason": "PCM payload is verified; current MetaSound frontend/event binding semantics are not decoded",
        })
    return result


def unavailable_texture_sources(
    package_records: dict[str, dict[str, Any]], parameters: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    parameter_text = {
        item["id"]: json.dumps(item.get("default"), ensure_ascii=False, sort_keys=True)
        for item in parameters
    }
    result = []
    for package_name in sorted(
        name for name in package_records if name.startswith("/Game/UltraDynamicSky/Textures/")
    ):
        record = package_records[package_name]
        references = sorted(
            parameter_id for parameter_id, text in parameter_text.items()
            if package_name in text
        )
        reason = (
            "package-specific exact TextureSource decode has no verified browser payload"
        )
        if package_name.endswith("/Moon_Color"):
            reason = (
                "Oodle backend unavailable; legacy PNG candidate size 54942 did not match "
                "serialized raw size 4202180, so it was rejected"
            )
        result.append({
            "source_package": package_name,
            "source_sha256": record["sha256"],
            "source_size": record["size"],
            "source_classes": record.get("classes", []),
            "root_parameter_references": references,
            "dependency_status": "ROOT_REFERENCED" if references else "P61_INDEXED_DEPENDENCY",
            "payload_status": "UNKNOWN",
            "browser_active": False,
            "reason": reason,
        })
    return result


def system_capabilities(
    parameters: list[dict[str, Any]], assets: list[dict[str, Any]], transition: dict[str, Any],
) -> list[dict[str, Any]]:
    counts = Counter(item["system"] for item in parameters)
    reasons = {
        "time": "range/default are verified; the complete time-to-celestial transform remains native/graph-partial",
        "sun": "serialized light values and curves exist; time-linked direction/material equivalence is incomplete",
        "moon": "serialized values and texture references exist; texture bytes and phase shader mapping are unavailable",
        "stars": "serialized values/material references exist; verified browser texture and day/cloud mask graph are unavailable",
        "atmosphere": "curves and material graph evidence exist; the Unreal SkyAtmosphere/material pipeline is not browser-equivalent",
        "clouds": "MPC/material/Niagara parameters exist; Oodle textures and complete volume material semantics are unavailable",
        "fog": "source values exist; Unreal ExponentialHeightFog mapping is not reproduced in WebGL",
        "rain": "preset, MPC and Niagara parameters exist; complete emitter/module semantics and texture payloads are unavailable",
        "snow": "preset, MPC and Niagara parameters exist; complete emitter/module semantics and texture payloads are unavailable",
        "wind": "direction/intensity/gust values and bindings exist; material/Niagara propagation is not fully executable",
        "lightning": "trigger ranges/light values exist; full trigger/VFX/thunder event chain is incomplete",
        "wetness": "MPC targets and drying durations exist; affected-material graph execution is unavailable",
        "transitions": "EaseInOut alpha path is decoded; delegated duration and full update ordering keep the control locked",
        "audio": "four exact PCM payloads exist; verified current-version MetaSound event bindings do not",
    }
    capabilities = []
    for system in VISUAL_SYSTEMS:
        static_sun = system == "sun"
        capabilities.append({
            "system": system,
            "source_found": counts[system] > 0 or system == "audio" and bool(assets),
            "values_extracted": counts[system] > 0,
            "logic_extracted": "PARTIAL" if system == "transitions" and transition.get("nodes") else "PARTIAL",
            "resources_extracted": bool(assets) if system == "audio" else False,
            "webgl_adapter": "SOURCE_VERIFIED_BROWSER_ADAPTER" if system in ("sun", "transitions") else "UNSUPPORTED_HTML",
            "visually_verified": False,
            "evidence_status": "PARTIAL",
            "browser_support_status": "SOURCE_VERIFIED_BROWSER_ADAPTER" if static_sun else "STATE_ONLY" if system in ("time", "transitions") else "UNSUPPORTED_HTML",
            "active_visual": static_sun,
            "limitations": reasons[system],
        })
    return capabilities


def controls_contract(parameters: list[dict[str, Any]], presets: list[dict[str, Any]]) -> list[dict[str, Any]]:
    time_parameter = next(
        (item for item in parameters if item["name"] == "Time of Day" and item["system"] == "time"), None,
    )
    controls = [
        {
            "id": "time_of_day", "active": time_parameter is not None, "visual_effect": False,
            "evidence_ids": [time_parameter["id"]] if time_parameter else [],
            "browser_target": "state.timeOfDay", "status": "STATE_ONLY",
            "reason": "exact source state; celestial visual binding is locked",
        },
        {
            "id": "weather_preset", "active": bool(presets), "visual_effect": False,
            "evidence_ids": [f"preset:{item['id']}" for item in presets],
            "browser_target": "state.weatherPreset", "status": "STATE_ONLY",
            "reason": "exact serialized preset targets; unsupported VFX remain locked",
        },
        {"id": "camera", "active": True, "visual_effect": True, "evidence_ids": ["browser:diagnostic-scene"], "browser_target": "camera", "status": "VERIFIED_BROWSER", "reason": "neutral diagnostic scene, not UDS content"},
        {"id": "panel", "active": True, "visual_effect": False, "evidence_ids": ["browser:ui"], "browser_target": "ui.panel", "status": "VERIFIED_BROWSER", "reason": "local UI capability"},
        {"id": "reset", "active": True, "visual_effect": False, "evidence_ids": ["browser:deterministic-reset"], "browser_target": "state.reset", "status": "VERIFIED_BROWSER", "reason": "returns to serialized defaults"},
        {"id": "export", "active": True, "visual_effect": False, "evidence_ids": ["browser:report-export"], "browser_target": "report.export", "status": "VERIFIED_BROWSER", "reason": "exports current state and truth statuses"},
    ]
    for system in ("sun", "moon", "stars", "clouds", "fog", "rain", "snow", "wind", "lightning", "wetness", "transitions", "audio"):
        controls.append({
            "id": system, "active": False, "visual_effect": False,
            "evidence_ids": [], "browser_target": None, "status": "UNSUPPORTED_HTML",
            "reason": next(item["limitations"] for item in system_capabilities(parameters, [], {} ) if item["system"] == system),
        })
    return controls


def scenarios(presets: list[dict[str, Any]], parameters: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_id = {item["id"]: item for item in presets}
    dawn = next((item["default"] for item in parameters if item["name"] == "Dawn Time"), None)
    dusk = next((item["default"] for item in parameters if item["name"] == "Dusk Time"), None)
    if not isinstance(dawn, (int, float)) or not isinstance(dusk, (int, float)):
        raise ValueError("Dawn Time/Dusk Time source defaults are required for scenarios")
    data = [
        ("noon", {"timeOfDay": 1200.0}, [], ["celestial visual transform unavailable"]),
        ("sunset", {"timeOfDay": dusk}, [], ["celestial visual transform unavailable"]),
        ("night", {"timeOfDay": 0.0}, [], ["moon/stars texture and material mapping unavailable"]),
        ("cloudy", {"weatherPreset": "Cloudy"}, ["Cloudy"], ["cloud material/volume adapter unavailable"]),
        ("rain", {"weatherPreset": "Rain"}, ["Rain"], ["Niagara/material/audio event semantics incomplete"]),
        ("snow", {"weatherPreset": "Snow"}, ["Snow"], ["Niagara/material semantics incomplete"]),
        ("lightning", {"weatherPreset": "Rain_Thunderstorm"}, ["Rain_Thunderstorm"], ["trigger/VFX/audio chain incomplete"]),
        ("clear_to_rain", {"from": "Clear_Skies", "to": "Rain"}, ["Clear_Skies", "Rain"], ["delegated transition duration prevents an active control"]),
        ("day_to_night", {"fromTime": dawn, "toTime": 0.0}, [], ["time animation/celestial transform incomplete"]),
    ]
    result = []
    for scenario_id, state, preset_ids, blockers in data:
        if any(item not in by_id for item in preset_ids):
            continue
        computed = {
            item: by_id[item]["values"] for item in preset_ids
        }
        result.append({
            "id": scenario_id,
            "input_state": state,
            "computed_parameters": computed,
            "assets": [],
            "status": "PARTIAL",
            "expected_signs": ["source state shown exactly in the accuracy panel"],
            "visual_executable": False,
            "blockers": blockers,
        })
    return result


def copy_templates(output: Path) -> list[dict[str, Any]]:
    template_root = Path(__file__).resolve().parent / "templates"
    files = []
    for source in sorted(path for path in template_root.rglob("*") if path.is_file()):
        relative = source.relative_to(template_root)
        target = output / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        files.append({
            "path": relative.as_posix(),
            "sha256": sha256_file(target),
            "size": target.stat().st_size,
        })
    return files


def build(args: argparse.Namespace) -> dict[str, Any]:
    p60, p61 = Path(args.p60).resolve(), Path(args.p61).resolve()
    output = Path(args.output).resolve()
    for required in (
        p60 / "EXTRACTION_GATE.json", p60 / "inventory.json",
        p61 / "dependencies" / "package_index.json", p61 / "MAP_GATE.json",
        p61 / "maps" / "DemoMap.json", p61 / "dependencies" / "dependency_closure.json",
    ):
        if not required.is_file():
            raise FileNotFoundError(required)
    p60_gate = read_json(p60 / "EXTRACTION_GATE.json")
    package_index = read_json(p61 / "dependencies" / "package_index.json")
    p61_gate = read_json(p61 / "MAP_GATE.json")
    if package_index.get("package_count") != 802 or package_index.get("errors"):
        raise ValueError("P61 package index is not the expected error-free 802-package source")
    records = {item["package_name"]: item for item in package_index["packages"]}
    missing_roots = [name for name in ROOT_PACKAGES if name not in records]
    if missing_roots:
        raise ValueError(f"missing visual roots: {missing_roots}")

    temp = output.with_name(output.name + ".tmp")
    if temp.exists():
        shutil.rmtree(temp)
    temp.mkdir(parents=True)
    (temp / "data").mkdir()
    (temp / "reports").mkdir()
    (temp / "assets").mkdir()
    template_files = copy_templates(temp)

    roots, parameters = [], []
    for name in ROOT_PACKAGES:
        summary, root_parameters = cdo_contract(records[name])
        roots.append(summary)
        parameters.extend(root_parameters)
    parameters.sort(key=lambda item: item["id"])
    metadata = variable_metadata(records[ROOT_PACKAGES[0]], {"Time of Day"})
    if len(metadata) != 1 or metadata[0].get("metadata", {}).get("UIMin") != "0" or metadata[0].get("metadata", {}).get("UIMax") != "2400":
        raise ValueError("Time of Day exact 0..2400 Blueprint metadata is required")
    time_animation = time_animation_node_contract(records[ROOT_PACKAGES[0]])
    transition = transition_node_contract(records[ROOT_PACKAGES[1]])
    presets = weather_presets(records)
    curves = curve_contracts(p60)
    materials, collections = material_contracts(p60)
    niagara = niagara_contracts(p60)
    niagara_parameter_count = sum(item["niagara_parameter_count"] for item in niagara)
    expected_niagara_parameter_count = int(p60_gate.get("Niagara_parameter_count", -1))
    if niagara_parameter_count != expected_niagara_parameter_count:
        raise ValueError(
            f"Niagara evidence incomplete: decoded={niagara_parameter_count}, "
            f"P60 gate={expected_niagara_parameter_count}"
        )
    assets = audio_assets(p60, temp / "assets", records)
    unavailable_textures = unavailable_texture_sources(records, parameters)
    capabilities = system_capabilities(parameters, assets, transition)
    controls = controls_contract(parameters, presets)
    scenario_contracts = scenarios(presets, parameters)

    evidence = {
        "$schema": "../../schemas/uds_visual_evidence.schema.json",
        "schema_version": SCHEMA_VERSION,
        "roots": roots,
        "parameters": parameters,
        "blueprint_variable_metadata": metadata,
        "time_animation_logic": time_animation,
        "transition_logic": transition,
        "curves": curves,
        "material_contracts": materials,
        "material_parameter_collections": collections,
        "niagara_contracts": niagara,
        "source_dataset": {
            "p60": {
                "gate_status": p60_gate.get("gate_status"),
                "package_count": p60_gate.get("total_packages"),
                "verified_count": p60_gate.get("VERIFIED_count"),
                "partial_verified_count": p60_gate.get("PARTIAL_VERIFIED_count"),
                "source_manifest_sha256": sha256_file(p60 / "inventory.json"),
            },
            "p61": {
                "package_count": package_index.get("package_count"),
                "errors": len(package_index.get("errors", [])),
                "package_index_sha256": sha256_file(p61 / "dependencies" / "package_index.json"),
                "dependency_closure_sha256": sha256_file(p61 / "dependencies" / "dependency_closure.json"),
                "map_contract_sha256": sha256_file(p61 / "maps" / "DemoMap.json"),
                "map_gate_status": p61_gate.get("gate_status") or p61_gate.get("status"),
            },
        },
    }
    asset_manifest = {
        "$schema": "../../schemas/uds_visual_asset_manifest.schema.json",
        "schema_version": SCHEMA_VERSION,
        "assets": assets,
        "texture_assets": [],
        "unavailable_texture_sources": unavailable_textures,
        "texture_blocker": "current TextureSource payloads require Oodle; legacy PNG candidates did not reproduce serialized raw size/BLAKE3",
        "proprietary_bytes_git_policy": "generated_local only; never committed",
    }
    sky_package = ROOT_PACKAGES[0]
    sun_parameter_ids = [
        item["id"] for item in parameters
        if item["evidence"]["source_package"] == sky_package
        and item["name"] in ("Sun Light Intensity", "Sun Light Color")
    ]
    weather_mpc = next(
        (item for item in collections if str(item.get("source_package", "")).endswith("/UltraDynamicWeather_Parameters")),
        None,
    )
    sun_vector = next(
        (
            item for item in (weather_mpc or {}).get("vector_parameters", [])
            if item.get("name", {}).get("value") == "Sun Vector"
        ),
        None,
    )
    require_sun_adapter = bool(len(sun_parameter_ids) == 2 and sun_vector)
    if not require_sun_adapter:
        raise ValueError("static sun adapter lacks exact intensity/color/vector evidence")
    visual_contract = {
        "$schema": "../../schemas/uds_visual_contract.schema.json",
        "schema_version": SCHEMA_VERSION,
        "source_truth": {
            "root_ids": [item["package"] for item in roots],
            "parameter_ids": [item["id"] for item in parameters],
            "weather_presets": presets,
            "curve_ids": [item["id"] for item in curves],
            "mpc_objects": [item["object"] for item in collections],
            "niagara_contract_ids": [item["id"] for item in niagara],
        },
        "browser_adapter_mapping": {
            "diagnostic_scene": {
                "status": "VERIFIED_BROWSER",
                "claim": "neutral geometry only; not DemoMap and not UDS sky",
                "clear_color": [0.018, 0.022, 0.028, 1.0],
                "directional_light": {
                    "status": "SOURCE_VERIFIED_BROWSER_ADAPTER",
                    "limitation": "serialized defaults only; no time-linked transform",
                    "evidence": {
                        "parameter_ids": sorted(sun_parameter_ids),
                        "mpc_source_package": weather_mpc["source_package"],
                        "mpc_source_sha256": weather_mpc["source_sha256"],
                        "mpc_object": weather_mpc["object"],
                        "mpc_parameter": "Sun Vector",
                        "coordinate_mapping": "Unreal (X,Y,Z) -> WebGL (X,Z,-Y)",
                        "mpc_parameter_evidence": clean_value(sun_vector),
                    },
                },
            },
            "controls": controls,
            "transition": transition["browser_adapter"],
        },
        "actual_visual_capability": capabilities,
        "missing_or_unsupported": [
            {"system": item["system"], "reason": item["limitations"]}
            for item in capabilities if not item["active_visual"]
        ],
        "scenarios": scenario_contracts,
        "map_gate": {
            "status": p61_gate.get("gate_status") or p61_gate.get("status"),
            "unchanged": True,
            "demo_map_rendered": False,
        },
    }
    capability_file = {
        "$schema": "../../schemas/uds_visual_capabilities.schema.json",
        "schema_version": SCHEMA_VERSION,
        "renderer": "WebGL2",
        "offline": True,
        "cdn": False,
        "network_requests": [
            "same-origin data/UDS_VISUAL_CONTRACT.json",
            "same-origin data/UDS_VISUAL_ASSET_MANIFEST.json",
        ],
        "mobile_ui": {
            "touch_camera": True, "camera_ground_clamp": True,
            "collapsible_panel": True, "panel_max_viewport_fraction": 0.4,
            "panel_scroll": True, "full_reset": True, "report_export": True,
        },
        "controls": controls,
        "systems": capabilities,
    }

    write_json(temp / "data" / "UDS_VISUAL_CONTRACT.json", visual_contract)
    write_json(temp / "data" / "UDS_VISUAL_ASSET_MANIFEST.json", asset_manifest)
    write_json(temp / "data" / "UDS_VISUAL_EVIDENCE.json", evidence)
    write_json(temp / "data" / "UDS_VISUAL_CAPABILITIES.json", capability_file)

    data_hashes = {
        path.name: sha256_file(path)
        for path in sorted((temp / "data").glob("*.json"))
    }
    report = {
        "$schema": "../../schemas/uds_visual_build_report.schema.json",
        "schema_version": SCHEMA_VERSION,
        "result": "BUILT",
        "roots": len(roots),
        "parameters": len(parameters),
        "weather_presets": len(presets),
        "curves": len(curves),
        "curve_keys": sum(item.get("total_key_count", 0) for item in curves),
        "material_contracts": len(materials),
        "mpc": len(collections),
        "niagara_contracts": len(niagara),
        "niagara_parameters": niagara_parameter_count,
        "audio_assets": len(assets),
        "texture_assets": 0,
        "unavailable_texture_sources": len(unavailable_textures),
        "active_controls": sorted(item["id"] for item in controls if item["active"]),
        "active_uds_visual_systems": ["sun:serialized-defaults-only"],
        "blocked_uds_visual_systems": list(VISUAL_SYSTEMS),
        "template_files": template_files,
        "data_sha256": data_hashes,
        "deterministic_build": True,
        "host_paths_persisted": False,
        "visual_claim": "pipeline/neutral diagnostic scene only; user visual equivalence is not claimed",
    }
    write_json(temp / "reports" / "UDS_VISUAL_BUILD_REPORT.json", report)
    gate = {
        "schema_version": SCHEMA_VERSION,
        "gate": "VISUAL_HTML_GATE",
        "status": "FAIL",
        "automatic_visual_equivalence_claim": False,
        "checks": [],
        "blockers": [
            "no UDS visual system has a complete evidence-backed WebGL2 adapter",
            "Oodle-backed TextureSource payloads are not decoded on this host",
            "Niagara/material/MetaSound runtime semantics are partial",
            "user visual review has not occurred",
        ],
    }
    write_json(temp / "reports" / "VISUAL_HTML_GATE.json", gate)

    if output.exists():
        shutil.rmtree(output)
    temp.replace(output)
    return report


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--p60", required=True, help="canonical P60 truth dataset")
    result.add_argument("--p61", required=True, help="P61 scene-truth dataset")
    result.add_argument(
        "--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"),
        help="generated local preview root",
    )
    return result


def main() -> int:
    args = parser().parse_args()
    report = build(args)
    print(json.dumps({
        "status": report["result"],
        "output": str(Path(args.output).resolve()),
        "roots": report["roots"],
        "parameters": report["parameters"],
        "weather_presets": report["weather_presets"],
    }, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
