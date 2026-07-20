#!/usr/bin/env python3
"""Build the P63.10 semantic UDS contract without exposing the owned source path.

This is deliberately an evidence compiler, not a renderer implementation. It joins decoded
Blueprint defaults/metadata/graph usage, extracted asset contracts and the current SOLUM
coverage audit into deterministic machine-readable checkpoint artifacts.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable

from extract_p63_10_uds_premium_truth import TARGETS


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATASET = ROOT / "_work/private_uds_p63_10"
DEFAULT_OUTPUT = ROOT / "_work/agent_reports/p63_10_contract"
BLUEPRINT_ASSET = "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Sky"

EXTRACTION_STATUSES = ("VERIFIED", "PARTIAL", "UNKNOWN")
IMPLEMENTATION_STATUSES = (
    "VERIFIED",
    "IMPLEMENTED_NOT_VISUALLY_VERIFIED",
    "PARTIAL",
    "APPROXIMATION",
    "BLOCKED",
    "NOT_IMPLEMENTED",
)


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as stream:
        return json.load(stream)


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def property_map(properties: Iterable[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return {item["name"]: item for item in properties if item.get("name")}


def decoded_fields(item: dict[str, Any]) -> dict[str, Any]:
    return {entry["name"]: entry.get("value") for entry in item.get("properties", [])}


def nested_field(value: Any, name: str) -> Any:
    if not isinstance(value, dict):
        return None
    for item in value.get("properties", []):
        if item.get("name") == name:
            return item.get("value")
    return None


def clean_value(value: Any, depth: int = 0) -> Any:
    """Keep semantic values while removing physical paths/provenance and huge native blobs."""
    if depth > 6:
        return {"summary": "nested value omitted", "status": "PARTIAL"}
    if isinstance(value, (str, int, float, bool)) or value is None:
        if isinstance(value, str) and (value.startswith("/storage/") or value.startswith("/data/")):
            return "PRIVATE_PATH_REDACTED"
        return value
    if isinstance(value, list):
        if len(value) > 64:
            return {"itemCount": len(value), "status": "PARTIAL_ARRAY_SUMMARY"}
        return [clean_value(item, depth + 1) for item in value]
    if isinstance(value, dict):
        if "object_path" in value:
            return {"objectPath": value.get("object_path")}
        omitted = {
            "header_physical_offset",
            "physical_offset",
            "preview_hex",
            "path",
            "udsRoot",
            "dataset",
            "source_file",
        }
        return {
            key: clean_value(item, depth + 1)
            for key, item in value.items()
            if key not in omitted and not key.endswith("_physical_offset")
        }
    return str(value)


def parse_metadata(item: dict[str, Any]) -> dict[str, str]:
    fields = decoded_fields(item)
    result: dict[str, str] = {}
    metadata = fields.get("MetaDataArray")
    if isinstance(metadata, dict):
        for entry in metadata.get("items", []):
            pair = decoded_fields(entry)
            key, value = pair.get("DataKey"), pair.get("DataValue")
            if isinstance(key, str) and isinstance(value, str):
                result[key] = value
    return result


def category_text(value: Any) -> str:
    if isinstance(value, str):
        return value
    if isinstance(value, dict):
        source = value.get("source")
        if isinstance(source, str):
            return source
    return "UNSPECIFIED_IN_EXTRACTED_METADATA"


def exact_range(metadata: dict[str, str]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for source, target in (
        ("UIMin", "uiMin"),
        ("UIMax", "uiMax"),
        ("ClampMin", "clampMin"),
        ("ClampMax", "clampMax"),
    ):
        if source not in metadata:
            continue
        raw = metadata[source]
        if not raw.strip():
            continue
        try:
            result[target] = float(raw)
        except ValueError:
            result[target] = raw
    return result or {"status": "UNKNOWN", "reason": "no exact range in Blueprint metadata"}


def explicit_units(metadata: dict[str, str]) -> dict[str, str]:
    tooltip = metadata.get("Tooltip", metadata.get("tooltip", ""))
    forced = metadata.get("ForceUnits", "").strip()
    if forced:
        return {"value": forced, "evidence": "Blueprint NewVariables ForceUnits"}
    units = []
    unit_patterns = (
        (r"\b(?:in|measured in|expressed in)\s+kilometers?\b", "kilometres"),
        (r"\b(?:in|measured in|expressed in)\s+centimeters?\b", "centimetres"),
        (r"\b(?:in|measured in|expressed in)\s+meters?\b", "metres"),
        (r"\b(?:in|measured in|expressed in)\s+lux\b", "lux"),
        (r"\b(?:in|measured in|expressed in)\s+degrees?\b", "degrees"),
        (r"\b(?:in|measured in|expressed in)\s+seconds?\b", "seconds"),
        (r"\b(?:in|measured in|expressed in)\s+minutes?\b", "minutes"),
        (r"\b(?:in|measured in|expressed in)\s+hours?\b", "hours"),
        (r"\b(?:in|measured in|expressed in)\s+days?\b", "days"),
        (r"\b(?:as a|in)\s+percentage\b", "percent"),
    )
    for pattern, unit in unit_patterns:
        if re.search(pattern, tooltip, flags=re.IGNORECASE):
            units.append(unit)
    return {
        "value": ",".join(dict.fromkeys(units)) if units else "UNSPECIFIED_IN_EXTRACTED_METADATA",
        "evidence": "Blueprint NewVariables Tooltip" if units else "UNKNOWN",
    }


def explicit_coordinate_space(metadata: dict[str, str]) -> dict[str, str]:
    tooltip = metadata.get("Tooltip", metadata.get("tooltip", ""))
    spaces = []
    for pattern, space in (
        (r"\bworld[ -]space\b|\bworld coordinates?\b", "WORLD"),
        (r"\blocal[ -]space\b|\blocal coordinates?\b", "LOCAL"),
        (r"\bcamera[ -]space\b|\bview[ -]space\b", "VIEW"),
        (r"\brelative to UDS\b", "UDS_RELATIVE"),
    ):
        if re.search(pattern, tooltip, flags=re.IGNORECASE):
            spaces.append(space)
    return {
        "value": ",".join(dict.fromkeys(spaces)) if spaces else "UNSPECIFIED_IN_EXTRACTED_METADATA",
        "evidence": "Blueprint NewVariables Tooltip" if spaces else "UNKNOWN",
    }


def classify_domain(name: str, category: str) -> list[str]:
    """Engineering index only; never presented as extracted UDS behavior."""
    text = f"{name} {category}".lower()
    rules = (
        ("sun", ("sun", "solar", "dawn", "sunrise", "sunset")),
        ("moon", ("moon", "lunar")),
        ("stars", ("star", "sidereal")),
        ("clouds", ("cloud", "overcast")),
        ("weather", ("weather", "rain", "snow", "dust", "lightning", "storm", "wetness", "puddle")),
        ("atmosphere", ("atmosphere", "rayleigh", "mie", "ozone", "fog", "sky color")),
        ("lighting", ("light", "skylight", "sky light", "exposure", "shadow")),
        ("time", ("time", "date", "year", "day", "transition", "dawn", "dusk")),
        ("material", ("material", "mid", "texture", "color", "opacity")),
    )
    result = [domain for domain, tokens in rules if any(token in text for token in tokens)]
    return result or ["support"]


def graph_usage(graphs: list[dict[str, Any]]) -> tuple[dict[str, set[str]], dict[str, set[str]], list[dict[str, Any]]]:
    readers: dict[str, set[str]] = defaultdict(set)
    writers: dict[str, set[str]] = defaultdict(set)
    function_contracts = []
    for graph in graphs:
        graph_path = graph.get("graph", "UNKNOWN_GRAPH")
        graph_name = graph_path.rsplit(".", 1)[-1]
        reads, writes, calls = set(), set(), set()
        for node in graph.get("nodes", []):
            properties = node.get("properties", {})
            if node.get("class") in ("K2Node_VariableGet", "K2Node_VariableSet"):
                variable = nested_field(properties.get("VariableReference"), "MemberName")
                if isinstance(variable, str):
                    target = writes if node.get("class") == "K2Node_VariableSet" else reads
                    target.add(variable)
                    target_map = writers if node.get("class") == "K2Node_VariableSet" else readers
                    target_map[variable].add(graph_name)
            if node.get("class") == "K2Node_CallFunction":
                function = nested_field(properties.get("FunctionReference"), "MemberName")
                if isinstance(function, str):
                    calls.add(function)
        function_contracts.append(
            {
                "sourceName": graph_name,
                "sourceGraph": graph_path,
                "nodeCount": len(graph.get("nodes", [])),
                "edgeCount": len(graph.get("edges", [])),
                "reads": sorted(reads),
                "modifies": sorted(writes),
                "calls": sorted(calls),
                "extractionStatus": "VERIFIED",
            }
        )
    return readers, writers, sorted(function_contracts, key=lambda item: item["sourceName"].lower())


def parameter_status(
    default_property: dict[str, Any] | None,
    metadata: dict[str, str],
    units: dict[str, str],
    coordinate_space: dict[str, str],
) -> tuple[str, list[str]]:
    missing = []
    if default_property is None:
        missing.extend(("typed CDO default", "property type"))
    if not metadata:
        missing.append("Blueprint variable metadata")
    if units["value"].startswith("UNSPECIFIED"):
        missing.append("explicit units")
    if coordinate_space["value"].startswith("UNSPECIFIED"):
        missing.append("explicit coordinate space")
    if default_property is None and not metadata:
        return "UNKNOWN", missing
    return ("PARTIAL", missing) if missing else ("VERIFIED", [])


RUNTIME_PARAMETER_MAPPING: dict[str, dict[str, str]] = {
    "Sun Light Intensity": {
        "solumTarget": "SolumCelestialControlState.sunLightLux / Filament directional light",
        "status": "PARTIAL",
        "difference": "UDS lux default and time curve are not yet the runtime authority",
    },
    "Sun Scale": {
        "solumTarget": "SolumCelestialControlState.sunAngularSizeDegrees",
        "status": "PARTIAL",
        "difference": "UDS metadata proves degrees; current 0.96 default differs from UDS 1.2",
    },
    "Moon Scale": {
        "solumTarget": "SolumCelestialControlState.moonAngularSizeDegrees",
        "status": "PARTIAL",
        "difference": "UDS metadata proves degrees; current 1.02 default differs from UDS 0.95",
    },
    "Moon Phase": {
        "solumTarget": "SolumCelestialControlState.moonPhase",
        "status": "PARTIAL",
        "difference": "manual phase control is not the decoded 29.53-day UDS update path",
    },
    "Cloud Coverage": {
        "solumTarget": "SolumCelestialControlState.cloudCoverage",
        "status": "PARTIAL",
        "difference": "SOLUM 0..1 scalar is not yet mapped to UDS 0..10 and layer graph semantics",
    },
    "Cloud Speed": {
        "solumTarget": "SolumCelestialControlState.cloudSpeed",
        "status": "PARTIAL",
        "difference": "single scalar velocity; UDS per-layer texture velocity is not complete",
    },
    "Bottom Altitude": {
        "solumTarget": "SolumCelestialControlState.cloudHeightKm",
        "status": "PARTIAL",
        "difference": "current scalar shell is not the full independent-layer UDS model",
    },
    "Stars Intensity": {
        "solumTarget": "SolumCelestialControlState.starBrightness",
        "status": "PARTIAL",
        "difference": "visual accepted, but density/size/Milky Way controls and UDS tiling default are not complete",
    },
    "Stars Tiling": {
        "solumTarget": "p63_3_analytic_sky.mat UDS star UV projection",
        "status": "PARTIAL",
        "difference": "shader hardcodes 3.0 while the extracted UDS Blueprint default is 2.5",
    },
    "Sky Light Intensity": {
        "solumTarget": "SolumEnvironmentController IBL intensity",
        "status": "PARTIAL",
        "difference": "dynamic analytic-sky capture is not implemented",
    },
    "Time of Day": {
        "solumTarget": "SolumCelestialControlState.time / SolumTimeSystem",
        "status": "PARTIAL",
        "difference": "control is live, but exact UDS transition/date execution is not runtime authority",
    },
    "Moon Light Intensity": {
        "solumTarget": "SolumCelestialControlState.moonLightLux",
        "status": "PARTIAL",
        "difference": "control is live; exact UDS night/phase/fog multipliers are incomplete",
    },
    "Moon Material Color": {
        "solumTarget": "SolumCelestialControlState.moonTint",
        "status": "PARTIAL",
        "difference": "exact default is bound; full Moon material graph remains incomplete",
    },
    "Sun Light Color": {
        "solumTarget": "SolumCelestialControlState.sunTint",
        "status": "PARTIAL",
        "difference": "manual tint is live; exact UDS Sun_Light_Color curve is not runtime authority",
    },
    "Cloud Density": {
        "solumTarget": "SolumCelestialControlState.cloudDensity",
        "status": "PARTIAL",
        "difference": "live scalar binding; UDS multi-layer density semantics are incomplete",
    },
    "Layer Height Scale": {
        "solumTarget": "SolumCelestialControlState.cloudThicknessKm adapter",
        "status": "PARTIAL",
        "difference": "live scalar adapter; no independent layer array",
    },
    "Formation Change Speed": {
        "solumTarget": "SolumCelestialControlState.cloudEvolution",
        "status": "PARTIAL",
        "difference": "live scalar binding; full UDS movement update graph is incomplete",
    },
    "Real Time Capture": {
        "solumTarget": "SolumAnalyticSkyState.p63DynamicIbl",
        "status": "NOT_IMPLEMENTED",
        "difference": "Filament path has no dynamic analytic-sky environment capture",
    },
}


def build_parameters(
    blueprint: dict[str, Any], source_asset: str
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    exports = blueprint.get("exports", [])
    bp_export = next(item for item in exports if item.get("class") == "Blueprint")
    expected_cdo = "Default__" + source_asset.rsplit("/", 1)[-1] + "_C"
    cdo_export = next(
        item for item in exports if item.get("object", "").endswith(expected_cdo)
    )
    cdo = property_map(cdo_export.get("properties", []))
    new_variables = property_map(bp_export.get("properties", []))["NewVariables"]["value"]["items"]
    readers, writers, functions = graph_usage(blueprint.get("graph", {}).get("graphs", []))
    for function in functions:
        function["sourceAsset"] = source_asset
    parameters = []
    for item in new_variables:
        fields = decoded_fields(item)
        name = fields.get("VarName")
        if not isinstance(name, str):
            continue
        metadata = parse_metadata(item)
        default_property = cdo.get(name)
        default = default_property.get("value") if default_property else fields.get("DefaultValue")
        category = category_text(fields.get("Category"))
        units = explicit_units(metadata)
        coordinate_space = explicit_coordinate_space(metadata)
        status, missing = parameter_status(default_property, metadata, units, coordinate_space)
        writes = sorted(writers.get(name, set()))
        update_frequency: dict[str, Any]
        if writes:
            update_frequency = {
                "value": "SOURCE_GRAPH_DRIVEN",
                "writerGraphs": writes,
                "note": "exact call frequency requires execution-flow/runtime tracing",
                "status": "PARTIAL",
            }
        else:
            update_frequency = {
                "value": "NO_DECODED_WRITER_GRAPH",
                "writerGraphs": [],
                "status": "UNKNOWN",
            }
        if update_frequency["status"] == "UNKNOWN":
            missing.append("exact update frequency")
            if status == "VERIFIED":
                status = "PARTIAL"
        property_type = (
            default_property.get("type", {}).get("display", "UNKNOWN") if default_property else "UNKNOWN"
        )
        mapping = RUNTIME_PARAMETER_MAPPING.get(
            name,
            {
                "solumTarget": "UNMAPPED",
                "status": "NOT_IMPLEMENTED",
                "difference": "no audited runtime binding in Checkpoint 0",
            },
        )
        parameters.append(
            {
                "sourceId": f"{source_asset}::variable:{fields.get('VarGuid') or name}",
                "sourceName": name,
                "friendlyName": fields.get("FriendlyName") or name,
                "sourceAsset": source_asset,
                "sourceCategory": category,
                "domainIndex": classify_domain(name, category),
                "type": property_type,
                "default": clean_value(default),
                "range": exact_range(metadata),
                "units": units,
                "coordinateSpace": coordinate_space,
                "updateFrequency": update_frequency,
                "dependencies": sorted(set(readers.get(name, set())) | set(writers.get(name, set()))),
                "readBy": sorted(readers.get(name, set())),
                "modifiedBy": writes,
                "visualEffect": metadata.get("Tooltip", metadata.get("tooltip", "UNKNOWN_NO_EXACT_TOOLTIP")),
                "metadata": metadata,
                "runtimeMapping": mapping,
                "missingEvidence": missing,
                "extractionStatus": status,
            }
        )
    return sorted(parameters, key=lambda item: item["sourceName"].lower()), functions


def logical_asset_path(relative_path: str) -> str:
    return "/Game/UltraDynamicSky/" + relative_path.removesuffix(".uasset")


def build_asset_inventory(dataset: Path) -> list[dict[str, Any]]:
    gate = read_json(dataset / "P63_10_EXTRACTION_GATE.json")
    gate_records = {record["relativePath"]: record for record in gate.get("records", [])}
    inventory = []
    for target in TARGETS:
        record = gate_records.get(target.relative_path, {})
        contract_path = dataset / "contracts" / f"{target.key}.{target.contract}.json"
        media_path = (
            dataset / "media" / f"{target.key}.{target.media}.json" if target.media else None
        )
        contract_payload = read_json(contract_path) if contract_path.is_file() else {}
        media_payload = read_json(media_path) if media_path and media_path.is_file() else {}
        contract = record.get("contract") or {}
        media = record.get("media") or {}
        source = record.get("source") or contract_payload.get("source") or {}
        contract_status = contract.get("status") or contract_payload.get("status", "NOT_EXTRACTED")
        media_status = media.get("status") or media_payload.get("status") or (
            "NOT_REQUESTED" if not target.media else "NOT_EXTRACTED"
        )
        target_status = record.get("status")
        if not target_status:
            if contract_status in ("VERIFIED", "RAW_VERIFIED", "PASS") and (
                not target.media or media_status in ("VERIFIED", "RAW_VERIFIED", "PASS")
            ):
                target_status = "VERIFIED"
            elif target.required:
                target_status = "PARTIAL"
            else:
                target_status = "OPTIONAL_PARTIAL"
        item: dict[str, Any] = {
            "group": target.group,
            "logicalAsset": logical_asset_path(target.relative_path),
            "required": target.required,
            "contractKind": target.contract,
            "mediaKind": target.media,
            "sourceSize": source.get("size"),
            "sourceSha256": source.get("sha256"),
            "contractStatus": contract_status,
            "contractSha256": contract.get("sha256") or (sha256(contract_path) if contract_path.is_file() else None),
            "mediaStatus": media_status,
            "mediaOutputSize": media.get("outputSize"),
            "mediaOutputSha256": media.get("outputSha256"),
            "evidencePolicy": record.get("licenseBoundary", "PRIVATE_PAID_OWNED_SOURCE_DO_NOT_REDISTRIBUTE"),
            "status": target_status,
        }
        if target.media == "texture" and media_payload:
            texture = media_payload.get("texture", {})
            item["dimensions"] = {
                "width": texture.get("width"),
                "height": texture.get("height"),
                "slices": texture.get("num_slices"),
                "mips": texture.get("num_mips"),
                "format": texture.get("source_format"),
            }
        inventory.append(item)
    return sorted(inventory, key=lambda item: (item["group"], item["logicalAsset"]))


def build_curves(dataset: Path, inventory: list[dict[str, Any]]) -> list[dict[str, Any]]:
    curves = []
    for asset in inventory:
        if "_Curves/" not in asset["logicalAsset"]:
            continue
        relative = asset["logicalAsset"].removeprefix("/Game/UltraDynamicSky/") + ".uasset"
        contract_path = dataset / "contracts" / f"{relative.removesuffix('.uasset').replace('/', '__')}.auto.json"
        if not contract_path.is_file():
            curves.append({"sourceAsset": asset["logicalAsset"], "status": "UNKNOWN"})
            continue
        contract = read_json(contract_path)
        curves.append(
            {
                "sourceAsset": asset["logicalAsset"],
                "status": contract.get("status"),
                "channelCount": contract.get("channel_count"),
                "totalKeyCount": contract.get("total_key_count"),
                "channels": clean_value(contract.get("channels", [])),
                "issues": clean_value(contract.get("issues", [])),
            }
        )
    return curves


def build_material_contracts(dataset: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    parameters: list[dict[str, Any]] = []
    functions: list[dict[str, Any]] = []
    for target in TARGETS:
        if target.contract != "material":
            continue
        contract_path = dataset / "contracts" / f"{target.key}.material.json"
        if not contract_path.is_file():
            continue
        contract = read_json(contract_path)
        source_asset = logical_asset_path(target.relative_path)
        source_parameters = contract.get("parameters", [])
        calls = sorted(
            {
                call.get("function", {}).get("object")
                for call in contract.get("function_calls", [])
                if isinstance(call.get("function", {}).get("object"), str)
            }
        )
        outputs = [
            item.get("name") or item.get("id") or "UNNAMED_OUTPUT"
            for item in contract.get("function_outputs", [])
        ]
        root_classes = sorted(
            {root.get("class", "UNKNOWN") for root in contract.get("roots", [])}
        )
        graph = contract.get("graph", {})
        function_name = source_asset.rsplit("/", 1)[-1]
        graph_status = graph.get("status", "UNKNOWN")
        if graph_status != "NOT_APPLICABLE":
            functions.append(
                {
                    "sourceName": function_name,
                    "sourceAsset": source_asset,
                    "sourceGraph": source_asset,
                    "functionKind": "MATERIAL_GRAPH",
                    "rootClasses": root_classes,
                    "nodeCount": graph.get("node_count", 0),
                    "edgeCount": graph.get("link_count", 0),
                    "reads": sorted(
                        {item.get("name") for item in source_parameters if item.get("name")}
                    ),
                    "modifies": outputs,
                    "calls": calls,
                    "graphStatus": graph_status,
                    "extractionStatus": (
                        "VERIFIED" if contract.get("status") == "VERIFIED" else "PARTIAL"
                    ),
                }
            )
        for item in source_parameters:
            name = item.get("name") or f"UNNAMED_EXPORT_{item.get('export_index', 'UNKNOWN')}"
            collection = clean_value(item.get("collection"))
            texture = clean_value(item.get("texture"))
            dependencies = [source_asset]
            if isinstance(collection, dict) and collection.get("object"):
                dependencies.append(collection["object"])
            if isinstance(texture, dict) and texture.get("object"):
                dependencies.append(texture["object"])
            parameters.append(
                {
                    "sourceId": (
                        f"{source_asset}::expression:{item.get('export_index', 'UNKNOWN')}"
                    ),
                    "sourceName": name,
                    "friendlyName": name,
                    "sourceAsset": source_asset,
                    "sourceExpression": clean_value(item.get("object")),
                    "sourceExportIndex": item.get("export_index"),
                    "sourceCategory": item.get("group") or "MATERIAL_PARAMETER",
                    "domainIndex": classify_domain(name, source_asset),
                    "type": item.get("class", "UNKNOWN"),
                    "default": clean_value(item.get("default_value")),
                    "range": {"status": "UNKNOWN", "reason": "not decoded in material parameter contract"},
                    "units": {"value": "UNSPECIFIED_IN_EXTRACTED_METADATA", "evidence": "UNKNOWN"},
                    "coordinateSpace": {"value": "UNSPECIFIED_IN_EXTRACTED_METADATA", "evidence": "UNKNOWN"},
                    "updateFrequency": {
                        "value": "MATERIAL_UNIFORM_OR_PARAMETER_COLLECTION",
                        "status": "PARTIAL",
                    },
                    "dependencies": sorted(dependencies),
                    "readBy": [source_asset],
                    "modifiedBy": [],
                    "visualEffect": "UNKNOWN_PENDING_EXPRESSION_REACHABILITY_DECOMPILATION",
                    "metadata": {
                        "guid": item.get("guid"),
                        "sortPriority": item.get("sort_priority"),
                        "texture": texture,
                        "collection": collection,
                    },
                    "runtimeMapping": {
                        "solumTarget": "UNMAPPED",
                        "status": "NOT_IMPLEMENTED",
                        "difference": "material parameter binding has not passed expression-level mapping",
                    },
                    "missingEvidence": [
                        "exact range/units/coordinate space",
                        "writer graph and runtime binding",
                        "expression-level visual effect",
                    ],
                    "extractionStatus": "PARTIAL",
                }
            )
    return (
        sorted(parameters, key=lambda item: (item["sourceAsset"], item["sourceName"])),
        sorted(functions, key=lambda item: item["sourceAsset"]),
    )


def requirement(
    identifier: str,
    system: str,
    text: str,
    status: str,
    sources: list[str],
    evidence: str,
    missing: str,
    verification: str,
) -> dict[str, Any]:
    if status not in IMPLEMENTATION_STATUSES:
        raise ValueError(f"unsupported status {status}")
    return {
        "id": identifier,
        "system": system,
        "requirement": text,
        "status": status,
        "udsSources": sources,
        "currentEvidence": evidence,
        "missingOrDifference": missing,
        "verification": verification,
    }


def build_coverage() -> list[dict[str, Any]]:
    b = BLUEPRINT_ASSET
    shader = "apps/engine/src/main/materials/p63_3_analytic_sky.mat"
    celestial = "apps/engine/src/main/java/com/solum/engine/environment/p63/SolumCelestialCoordinateSystem.java"
    return [
        requirement("SUN-01", "sun", "world-space direction", "APPROXIMATION", [f"{b}: Approximate Real Sun Moon and Stars", f"{b}: Cache Sun and Moon Orientation"], celestial, "fixed 65-degree model is not the decoded UDS astronomy graph", "runtime vector dump plus reference dates/latitudes"),
        requirement("SUN-02", "sun", "time/date trajectory", "APPROXIMATION", [f"{b}: Approximate Real Sun Moon and Stars", f"{b}: Find Real Sunset/Sunrise Times"], celestial, "date/location/J2000/sidereal inputs not bound", "reference ephemeris cases and UDS parity dump"),
        requirement("SUN-03", "sun", "separate visual disk", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Disk"], shader, "device comparison for the current build is absent", "device noon/sunset/horizon captures"),
        requirement("SUN-04", "sun", "UDS angular size semantics", "PARTIAL", [f"{b}: parameter Sun Scale (Degrees)"], "SolumCelestialControlState.sunAngularSizeDegrees", "UDS is exactly 1.2 degrees; current default is 0.96", "default and shader-radius binding test"),
        requirement("SUN-05", "sun", "color and intensity curves", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Disk_Color", "/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Light_Color", "/Game/UltraDynamicSky/Materials/Float_Curves/Directional_Light_Intensity"], "analytic shader and directional light", "all extracted curve channels are not runtime authority", "key-by-key CPU evaluator tests"),
        requirement("SUN-06", "sun", "directional-light coupling", "PARTIAL", [f"{b}: Current Sun Light Intensity", f"{b}: Current Sun Light Color"], "SolumEnvironmentController", "direction exists but exact UDS values/curves are not complete", "runtime material/light synchronized dump"),
        requirement("SUN-07", "sun", "atmosphere coupling", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Ultra_Dynamic_Sky_Mat"], shader, "SOLUM analytic scattering is not a full UDS material mapping", "UDS/SOLUM noon and sunset comparison"),
        requirement("SUN-08", "sun", "cloud occlusion of disk", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Conservative_Density"], shader, "current raymarch path has no device confirmation", "dense cloud horizon/device capture"),
        requirement("SUN-09", "sun", "camera-rotation independence", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", [f"{b}: Cached Sun Vector"], shader, "runtime vectors are direction-based; visual proof pending", "90-degree camera turn capture and JSON"),
        requirement("MOON-01", "moon", "independent world trajectory", "NOT_IMPLEMENTED", [f"{b}: Approximate Real Sun Moon and Stars", f"{b}: Cache Sun and Moon Orientation"], celestial, "Moon is derived antipodal to Sun", "independent direction/runtime astronomy test"),
        requirement("MOON-02", "moon", "visual disk using authored textures", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Textures/Sky/Moon_Color", "/Game/UltraDynamicSky/Textures/Sky/Moon_PhaseNormal", "/Game/UltraDynamicSky/Materials/Material_Functions/Moon"], shader, "exact textures are bound; current visual polish not accepted", "night and horizon device captures"),
        requirement("MOON-03", "moon", "UDS angular size semantics", "PARTIAL", [f"{b}: parameter Moon Scale (Degrees)"], "SolumCelestialControlState.moonAngularSizeDegrees", "UDS is exactly 0.95 degrees; current default is 1.02", "default and shader-radius binding test"),
        requirement("MOON-04", "moon", "29.53-day phase and date coupling", "PARTIAL", [f"{b}: Update Lunar Phase", f"{b}: Moon Phase"], "manual phase shader control", "decoded UDS lunar update is not the runtime authority", "phase-cycle unit tests and device captures"),
        requirement("MOON-05", "moon", "orientation/yaw/pitch", "PARTIAL", [f"{b}: Moon Yaw", f"{b}: Moon Pitch"], shader, "full UDS orientation path not mapped", "cardinal camera and phase-normal orientation tests"),
        requirement("MOON-06", "moon", "color/brightness/night coupling", "PARTIAL", [f"{b}: Moon Light Intensity", f"{b}: Moon Texture Intensity (Night)", f"{b}: Moon Texture Intensity (Day)"], "analytic shader and moon directional light", "exact day/night graph and light curve are incomplete", "dusk/night/dawn runtime dump"),
        requirement("MOON-07", "moon", "cloud occlusion", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Conservative_Density"], shader, "shared raymarch occlusion exists; device proof pending", "dense cloud moon-horizon capture"),
        requirement("ATM-01", "atmosphere", "UDS atmospheric material/defaults/curves", "APPROXIMATION", ["/Game/UltraDynamicSky/Materials/Ultra_Dynamic_Sky_Mat", "/Game/UltraDynamicSky/Materials/Float_Curves/Skyatmosphere_Density"], shader, "current Rayleigh/Mie constants and coefficients are SOLUM-authored", "expression-level mapping and reference captures"),
        requirement("STAR-01", "stars", "authored star textures and UV/twinkle logic", "PARTIAL", ["/Game/UltraDynamicSky/Textures/Sky/Real_Stars", "/Game/UltraDynamicSky/Materials/Material_Functions/Stars", "/Game/UltraDynamicSky/Materials/Material_Functions/Tiling_Stars_UVs"], shader, "user accepted the visible field, but size/Milky Way uniforms are dead and UDS tiling 2.5 differs from shader 3.0", "control reachability tests plus accepted user QA"),
        requirement("CLOUD-01", "clouds", "not a screen-space mask", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Textures/3D_Clouds/FormationVolume"], shader, "3D density raymarch exists", "camera rotation/translation device sequence"),
        requirement("CLOUD-02", "clouds", "world anchoring under camera rotation", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_UVs"], shader, "view-direction shell is rotation stable but not full world-position mapping", "90-degree rotation capture"),
        requirement("CLOUD-03", "clouds", "world anchoring under camera translation", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_UVs"], shader, "shader has no camera world position input", "translated-camera capture/runtime JSON"),
        requirement("CLOUD-04", "clouds", "independent layer height/thickness/scale/density/coverage/erosion/wind/light", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Composite_Cloud_Layers", f"{b}: Cloud Layer Top and Bottom World Height"], "SolumCelestialControlState scalar cloud state", "one scalar layer and a type switch cannot represent independent layers", "active layer array runtime contract"),
        requirement("CLOUD-05", "clouds", "tileable seamless authored noise", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Textures/3D_Clouds/FormationVolume", "/Game/UltraDynamicSky/Textures/3D_Clouds/3D_Cells_32", "/Game/UltraDynamicSky/Textures/Volumetric_Clouds/Cloud_Profile"], shader, "authored volumes are bound with repeat sampling; seam QA pending", "orbit and long-wind capture"),
        requirement("CLOUD-06", "clouds", "large volumetric structures", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Conservative_Density"], shader, "raymarch exists but user QA reports small flat cloud fragments", "UDS settings parity and horizon/up captures"),
        requirement("CLOUD-07", "clouds", "density changes internal density, not alpha only", "IMPLEMENTED_NOT_VISUALLY_VERIFIED", ["/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Extinction"], shader, "density participates in Beer-Lambert integration", "controlled density sweep capture and runtime dump"),
        requirement("CLOUD-08", "clouds", "sun/moon directed lighting and internal absorption", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Material_Functions/Light_and_Dark_Cloud_Colors", "/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Extinction"], shader, "optimized light march exists but UDS graph mapping is incomplete", "sun-side/opposite-side cloud captures"),
        requirement("CLOUD-09", "clouds", "horizon cloud occlusion", "PARTIAL", [f"{b}: Cloud Layer Top and Bottom World Height"], shader, "below-horizon early return and direction shell limit full horizon behavior", "sun and moon horizon cloud captures"),
        requirement("CLOUD-10", "clouds", "architecture permits full volumetric multi-layer path", "PARTIAL", ["/Game/UltraDynamicSky/Materials/Material_Functions/Composite_Cloud_Layers"], "scalar state and single material", "runtime data model must become an array of independent layers", "architecture review after Stage 7"),
        requirement("TIME-01", "time", "UDS time-of-day/date transition functions", "APPROXIMATION", [f"{b}: Transition Time of Day", f"{b}: Tick Time Transition", f"{b}: Finish Time Transition"], "SolumCelestialSystem", "current transition/astronomy behavior is custom", "decoded graph evaluator tests"),
        requirement("AURORA-01", "aurora", "authored UDS aurora shape/material response", "APPROXIMATION", ["/Game/UltraDynamicSky/Textures/Clouds/Aurora_Clouds", "/Game/UltraDynamicSky/Materials/Material_Functions/Aurora", "/Game/UltraDynamicSky/Materials/Volumetric_Aurora"], shader, "runtime uses ParticleClouds plus custom sinusoidal curtains; exact Aurora_Clouds payload is Oodle-blocked and the graph is not ported", "night/orbit captures plus material expression fixtures"),
        requirement("AURORA-02", "aurora", "seamless tileable authored animation", "APPROXIMATION", ["/Game/UltraDynamicSky/Materials/Material_Functions/Aurora"], shader, "custom mirrored panning does not prove UDS animation or boundary parity", "long-duration UV seam capture and decoded graph evaluator"),
        requirement("VFX-RAIN-01", "weather_vfx", "UDW Niagara rain with exact sheet, wind and wetness coupling", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Particles/Rain", "/Game/UltraDynamicSky/Textures/Weather/RainSnow_Sheet", "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumFilamentEnvironmentAdapter", "world-space rain renderer is explicitly fail-closed; no Niagara semantic port", "world-space camera-move capture, particle/runtime dump and wetness timeline"),
        requirement("VFX-SNOW-01", "weather_vfx", "UDW Niagara snow with exact sheet, wind and surface accumulation", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Particles/Snow", "/Game/UltraDynamicSky/Textures/Weather/RainSnow_Sheet", "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumFilamentEnvironmentAdapter", "world-space snow renderer is explicitly fail-closed", "world-space capture, particle/runtime dump and accumulation timeline"),
        requirement("VFX-DUST-01", "weather_vfx", "UDW Niagara dust with exact alpha and wind coupling", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Particles/Dust", "/Game/UltraDynamicSky/Textures/Weather/Dust_Alpha", "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumFilamentEnvironmentAdapter", "sand/dust world-space renderer is explicitly fail-closed", "wind-direction capture and particle/runtime dump"),
        requirement("VFX-LIGHTNING-01", "weather_vfx", "UDW lightning strike/obscured visuals plus transient light", "PARTIAL", ["/Game/UltraDynamicSky/Particles/Lightning_Strike", "/Game/UltraDynamicSky/Particles/Obscured_Lightning", "/Game/UltraDynamicSky/Textures/Weather/ObscuredLightningSheet"], "SolumEnvironmentController / SolumFilamentEnvironmentAdapter", "transient light/cloud flash are live, but bolt and obscured-lightning visual renderers are fail-closed", "seeded timing fixtures and device strike/cloud captures"),
        requirement("VFX-PUDDLE-01", "weather_vfx", "UDW puddle splash/ripple visuals", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Particles/Puddle_Splash", "/Game/UltraDynamicSky/Textures/Weather/PuddleSplashParticle", "/Game/UltraDynamicSky/Materials/Material_Functions/Map_Puddle_Ripples"], "SolumFilamentEnvironmentAdapter", "puddle ripple/splash VFX is explicitly fail-closed", "rain-impact capture and ripple lifetime fixtures"),
        requirement("VFX-RAYS-01", "weather_vfx", "UDS volumetric cloud light rays driven by Sun/Moon/cloud occlusion", "PARTIAL", ["/Game/UltraDynamicSky/Particles/VolumetricCloud_LightRays", f"{b}: Volumetric Light Ray Strength and Color"], shader, "analytic ray term exists, but Niagara/material semantics and source coefficients are not closed", "sun/moon cloud-gap captures and expression fixtures"),
        requirement("SURFACE-01", "surface_weather", "UDW wetness and puddle accumulation/drying", "APPROXIMATION", ["/Game/UltraDynamicSky/Materials/Weather/Surface_Weather_Effects", "/Game/UltraDynamicSky/Materials/Material_Functions/Map_Puddle_Ripples", "/Game/UltraDynamicSky/Materials/Material_Functions/Sample_Weather_Mask_Brushes"], "SolumFilamentEnvironmentAdapter PBR scalar updates", "custom scalar PBR response is not the decoded UDS material-function chain", "material grid fixtures and rain/dry timelines"),
        requirement("SURFACE-02", "surface_weather", "UDW snow accumulation, normals, windswept response and melt", "APPROXIMATION", ["/Game/UltraDynamicSky/Materials/Weather/Surface_Weather_Effects", "/Game/UltraDynamicSky/Materials/Weather/Snow_Sparkle", "/Game/UltraDynamicSky/Textures/Weather/Rough_Snow", "/Game/UltraDynamicSky/Textures/Weather/Snow_Normal", "/Game/UltraDynamicSky/Textures/Weather/Windswept_Snow_Normal"], "SolumFilamentEnvironmentAdapter PBR scalar updates", "snow amount is live but exact masks/normals/sparkle/melt graph is not integrated", "snowfall/melt timeline and grazing-light material captures"),
        requirement("SURFACE-03", "surface_weather", "UDW ice/freeze/thaw surface behavior", "NOT_IMPLEMENTED", ["/Game/UltraDynamicSky/Materials/Weather/Surface_Weather_Effects"], "SolumFilamentEnvironmentAdapter", "no source-backed independent ice/freeze/thaw contract is runtime authority", "temperature/freeze/thaw state fixtures and material captures"),
        requirement("WIND-01", "wind", "UDW wind direction/speed/gust drives clouds, precipitation, dust, surfaces and audio", "PARTIAL", ["/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumWindState / SolumEnvironmentController", "shared wind state is live, but exact UDW readers/writers and per-system scaling are not mapped", "cross-system seeded wind sweep and runtime reachability dump"),
        requirement("AUDIO-01", "audio", "UDW weather loops, effects, crossfades and spatial/interior response", "PARTIAL", ["/Game/UltraDynamicSky/Sound/Rain/CloseRainLoop", "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumEnvironmentAudioSystem", "exact rain loop is available and crossfade/interior paths exist; remaining UDW roles/effects/loop contracts are incomplete", "loop-boundary, gain/crossfade and interior attenuation audio-state tests"),
        requirement("WEATHER-01", "weather", "UDS procedural weather dependencies and seasonal/location/date policy", "APPROXIMATION", ["/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather", f"{b}: Season Value for Weather from Date and Time"], "SolumSeasonalWeatherPolicy", "custom policy exists, but exact UDW dependency graph/defaults are not runtime authority", "deterministic season/location/date scenario tests"),
        requirement("WEATHER-02", "weather", "UDW authored weather transitions and mutually valid combinations", "APPROXIMATION", ["/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather"], "SolumEnvironmentController transition state", "current transitions are custom and do not yet reproduce decoded UDW timing/easing/dependencies", "golden transition timelines for clear/rain/snow/dust/storm"),
        requirement("IBL-01", "lighting", "dynamic sky/moon/stars IBL", "NOT_IMPLEMENTED", [f"{b}: parameter Real Time Capture", f"{b}: parameter Sky Light Intensity", f"{b}: Restart Real Time Sky Light Capture"], "prepared/user HDR IBL path", "analytic sky is not captured into dynamic IBL", "day/night surface-light probe captures"),
        requirement("QA-01", "verification", "complete canonical device capture set with runtime/camera/layers JSON", "BLOCKED", ["docs/VISUAL_QA_SPEC.md"], "user owns device visual verification", "post-checkpoint images do not yet exist", "user device: noon/sunset/night/horizons/cloud densities/rotation/translation"),
    ]


def build_control_reachability_audit() -> list[dict[str, Any]]:
    return [
        {
            "control": "starSize",
            "uniform": "stars0.w",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "uniform component is uploaded but never read by p63_3_analytic_sky.mat",
        },
        {
            "control": "milkyWayIntensity",
            "uniform": "stars1.y",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "uniform component is uploaded but never read",
        },
        {
            "control": "milkyWaySaturation",
            "uniform": "stars1.z",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "uniform component is uploaded but never read",
        },
        {
            "control": "starDensity",
            "uniform": "stars0.x",
            "shaderRead": True,
            "status": "PARTIAL",
            "finding": "currently scales radiance amplitude instead of changing catalog/sample density",
        },
        {
            "control": "Stars Tiling",
            "uniform": "none",
            "shaderRead": False,
            "status": "PARTIAL",
            "finding": "UDS default is 2.5; shader hardcodes 3.0",
        },
        {
            "control": "sunHaloSize",
            "uniform": "sun1.x",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "uploaded legacy control is not read by the clean UDS disk path",
        },
        {
            "control": "sunHaloFalloff",
            "uniform": "sun1.y",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "uploaded legacy control is not read by the clean UDS disk path",
        },
        {
            "control": "sunVisualBrightness",
            "uniform": "none in analytic sky",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "changes lighting.sunDiskBrightness, which is not consumed by the analytic sky material",
        },
        {
            "control": "sunEdgeSoftness",
            "uniform": "none",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "state value is not propagated to SolumAnalyticSkyState",
        },
        {
            "control": "moonVisualBrightness",
            "uniform": "none in analytic sky",
            "shaderRead": False,
            "status": "NOT_IMPLEMENTED",
            "finding": "changes lighting.moonDiskBrightness, which is not consumed by the analytic sky material",
        },
        {
            "control": "cloudType Cumulus/Stratocumulus/Storm",
            "uniform": "cloud3.x",
            "shaderRead": True,
            "status": "PARTIAL",
            "finding": "only Cirrus selects a distinct path; the other three modes share one density path",
        },
    ]


QA_EVIDENCE = (
    {
        "scenario": "partly-cloudy",
        "imageFile": "P63_4_partly_cloudy_20260717_193510.png",
        "imageSha256": "615f82cdc2bdb782f913de881db56ee0a9df49c6013a3424bb5a8dad326d9ff7",
        "runtimeFile": "P63_4_partly_cloudy_20260717_193510.json",
        "runtimeSha256": "2fbe9d7d176158eae21a6bce4416bd2f1f6a04ca4fe353f5c95e91bf3b32b7b5",
        "finding": "small weak cloud fragments; pre-final lower-shell settings",
        "status": "PARTIAL",
    },
    {
        "scenario": "crescent-moon-labelled",
        "imageFile": "P63_4_crescent_moon_20260717_152024.png",
        "imageSha256": "e4b85386781e5bdd327b019c2ed794f95eca4c2034a9eaa75b623d66a41756d9",
        "runtimeFile": "P63_4_crescent_moon_20260717_152024.json",
        "runtimeSha256": "ebc8fdf072ba9f28400a9fe986fab4e02826c33071e72565ec0118d1124d0df1",
        "finding": "daylight frame does not prove Moon appearance",
        "status": "NOT_VALID_FOR_MOON_VISUAL_VERIFICATION",
    },
)


def build_qa_evidence(qa_root: Path | None) -> list[dict[str, Any]]:
    """Verify user-owned QA bundles without publishing their physical directory."""
    result = []
    for expected in QA_EVIDENCE:
        entry = dict(expected)
        if qa_root is None:
            entry["hashVerificationStatus"] = "NOT_VERIFIED_IN_THIS_GENERATION"
        else:
            checks = []
            for file_key, hash_key in (
                ("imageFile", "imageSha256"),
                ("runtimeFile", "runtimeSha256"),
            ):
                path = qa_root / entry[file_key]
                checks.append(path.is_file() and sha256(path) == entry[hash_key])
            entry["hashVerificationStatus"] = (
                "VERIFIED" if all(checks) else "REJECTED_MISSING_OR_HASH_MISMATCH"
            )
        result.append(entry)
    return result


def semantic_entry(
    source: str,
    behavior: str,
    required: str,
    implementation: str,
    differences: str,
    gpu: str,
    cpu: str,
    risk: str,
    verification: str,
    prohibited: list[str],
    status: str,
) -> dict[str, Any]:
    return {
        "udsSource": source,
        "udsBehavior": behavior,
        "requiredVisualResult": required,
        "proposedSolumFilamentImplementation": implementation,
        "currentDifferences": differences,
        "estimatedGpuCost": gpu,
        "estimatedCpuCost": cpu,
        "riskOfBehaviorLoss": risk,
        "verification": verification,
        "prohibitedSimplifications": prohibited,
        "status": status,
    }


def build_semantic_mapping() -> list[dict[str, Any]]:
    b = BLUEPRINT_ASSET
    no_fake = ["screen-space billboard/mask", "unreferenced guessed constants", "single alpha texture as final system"]
    return [
        semantic_entry(f"{b}: Approximate Real Sun Moon and Stars", "UTC/date/time-of-year/J2000/sidereal celestial orientation", "stable real Sun/Moon/stars world directions", "CPU double-precision decoded graph adapter feeding Filament directions", "current fixed 65-degree orbit and antipodal Moon", "none", "low per time update", "critical", "golden vector cases across dates/locations", ["fixed daily sine orbit", "antipodal Moon"], "NOT_IMPLEMENTED"),
        semantic_entry(f"{b}: Cache Sun and Moon Orientation", "cache celestial component orientation/directions for consumers", "one authoritative direction per frame", "immutable celestial frame state shared by material and lights", "multiple current adapters need authority audit", "none", "low", "high", "runtime dump equals material/light inputs", ["independent guessed material and light vectors"], "PARTIAL"),
        semantic_entry(f"{b}: Find Real Sunset/Sunrise Times", "derive horizon crossings from date/location", "correct dawn/dusk transitions", "decoded CPU evaluator with tests", "fixed/custom dawn and dusk", "none", "low on date/location changes", "high", "known latitude/date cases", ["fixed 06:00/18:00 for real simulation"], "NOT_IMPLEMENTED"),
        semantic_entry(f"{b}: Update Lunar Phase", "advance/wrap lunar age on 29.53-day cycle and update phase", "date-consistent crescent/quarter/full Moon", "decoded phase state evaluator; shader only renders provided state", "manual phase is currently authoritative", "negligible", "low", "high", "lunar-cycle golden values and texture orientation", ["random phase", "screen-space phase mask disconnected from date"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Sun_Disk", "authored analytic Sun disk expression graph", "defined limb/edge and cloud-occludable disk", "Filament material expression port backed by extracted constants", "port exists but expression parity audit and device QA remain", "low per sky pixel", "none", "medium", "expression fixtures plus noon/horizon captures", ["replacement Sun sprite", "extra artistic rings/ghosts in the disk contract"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Disk_Color", "time/elevation dependent authored disk color", "soft UDS color change near sunset", "exact curve-key evaluator on CPU or shader LUT", "extracted keys are not yet runtime authority", "negligible", "low", "high", "key/interpolation parity tests", ["hand-picked sunset RGB"], "NOT_IMPLEMENTED"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Light_Color", "authored directional-light color curve", "surface and cloud light matches celestial state", "exact curve-key evaluator feeding Filament light/cloud material", "current artistic light color path", "negligible", "low", "high", "curve keys and synchronized runtime dump", ["unrelated sky and directional-light colors"], "NOT_IMPLEMENTED"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Float_Curves/Directional_Light_Intensity", "authored time/elevation light intensity", "day/dusk/night exposure-compatible light", "exact curve evaluator mapped to Filament units with documented conversion", "unit conversion and curve binding incomplete", "none", "low", "critical", "curve fixtures and lux/runtime probe", ["arbitrary normalized intensity"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Moon", "Moon texture, phase normal, phase contrast/dark side/glow/orientation composition", "natural phase, surface detail and controlled glow", "port every extracted material expression and bind UDS defaults", "texture and phase path exist; orientation/glow/day-night semantics incomplete", "low per sky pixel", "none", "high", "expression audit and phase grid captures", ["plain white disk", "phase texture only without graph semantics"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Ultra_Dynamic_Sky_Mat", "composite atmosphere/celestial/stars/cloud outputs", "coherent UDS sky through time/weather", "mapped Filament sky material split into bounded functions", "current analytic atmosphere is SOLUM-authored approximation", "medium sky pass", "low state updates", "critical", "material expression coverage matrix", ["visually similar generic gradient as VERIFIED"], "APPROXIMATION"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Stars", "authored star texture sampling, color and visibility response", "stable star field without pixel-dot fake", "port the exact decoded Stars expression graph and bind every live control", "exact textures are bound, but size/Milky Way controls are dead and density semantics differ", "low", "none", "medium", "expression fixtures, control reachability and accepted night QA", ["procedural point dots", "claim VERIFIED while controls are dead"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Tiling_Stars_UVs", "authored tiling-star UV projection", "stable sky-relative projection without hiding atmosphere", "port the exact decoded Tiling_Stars_UVs expression graph", "shader tiling is 3.0 while the extracted UDS default is 2.5", "low", "none", "medium", "UV fixtures and 90-degree camera captures", ["screen-space star overlay", "invented tiling constant"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_UVs", "world/layer coordinates plus velocity for cloud sampling", "cloud field stays in world when camera rotates/moves", "camera world position plus planet/layer shell intersection and per-layer wind", "current sampling is view-direction based and cannot respond to camera translation", "low", "low per frame", "critical", "rotation and translated-camera captures", ["screen UV", "view-only coordinates as world-space completion"], "NOT_IMPLEMENTED"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Cloud_Distribution", "large-scale authored formation distribution", "large separated cloud masses", "port extracted expression graph around FormationVolume", "FormationVolume is bound but distribution parity is incomplete", "medium within raymarch", "none", "critical", "density-volume slice tests and up/horizon captures", no_fake, "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Map_Cloud_Textures", "map formation and erosion fields by authored controls", "volume at macro scale with fine evolving edges", "expression-level port with independent per-layer scale/velocity", "single shared scale/velocity path", "medium", "none", "high", "UV/sample coordinate fixtures", ["one 2D cloud bitmap", "one scale for all layers"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Conservative_Density", "conservative density using authored formation/profile/erosion", "solid cloud volume with stable raymarch occupancy", "Filament sky raymarch using exact extracted volumes and decoded formula", "exact volumes are used; full formula parity not closed", "high, bounded ray steps", "none", "critical", "sample-grid CPU reference versus shader", ["alpha-only density", "tiny 256 texture rejection based only on dimensions"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Volumetric_Clouds_Extinction", "extinction/transmittance through cloud density", "depth, silvering and internal absorption", "Beer-Lambert integration with authored coefficients", "optimized integration exists; coefficients need source closure", "high, bounded light steps", "none", "high", "transmittance reference grid", ["density mapped only to final alpha"], "PARTIAL"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Composite_Cloud_Layers", "combine independent cloud layers", "simultaneous low/mid/high clouds with separate controls", "runtime CloudLayerState array and bounded per-layer material loop/passes", "current type switch selects one scalar layer", "medium-high proportional to active layers", "low", "critical", "active-layer JSON plus independent control sweeps", ["collapse all layers into one texture/state", "cloud type enum as layer system"], "NOT_IMPLEMENTED"),
        semantic_entry("/Game/UltraDynamicSky/Materials/Material_Functions/Light_and_Dark_Cloud_Colors", "directional light/dark cloud color response", "sun/moon-facing cloud color, dark interiors and sunset tint", "source-derived light evaluation per layer using active celestial light", "current light march is only a partial mapping", "medium-high", "none", "high", "front/back light captures at noon/sunset/night", ["constant gray cloud color", "global alpha darkening"], "PARTIAL"),
        semantic_entry(f"{b}: Cloud Layer Top and Bottom World Height", "derive layer world bounds from base/scale/settings", "correct horizon intersection and parallax", "CPU layer bounds plus shader shell intersection using camera world position", "one shell and below-horizon early return", "low", "low when layer state changes", "critical", "numeric bounds fixtures and horizon QA", ["lower clouds by moving a screen texture", "ignore camera altitude"], "NOT_IMPLEMENTED"),
        semantic_entry(f"{b}: Tick Time Transition", "advance the authored time transition and its eased state", "soft deterministic time transition", "decoded state-machine adapter with exact duration/ease semantics", "current custom transition does not cover the graph", "none", "low per transition tick", "high", "state-transition golden JSON timeline", ["random lerp without UDS dependencies", "instant preset replacement"], "NOT_IMPLEMENTED"),
        semantic_entry(f"{b}: Restart Real Time Sky Light Capture", "restart/update captured sky lighting when dynamic state requires it", "day/night/cloud lighting reflected on surfaces", "Filament-compatible dynamic environment capture or source-faithful bounded alternative", "only prepared/user HDR IBL currently exists", "potentially high when capture updates", "medium scheduling", "critical", "light probe/runtime exposure captures", ["constant daytime HDR at night", "claim analytic sky IBL without capture"], "NOT_IMPLEMENTED"),
    ]


def complete_semantic_mapping(functions: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], dict[str, int]]:
    curated = build_semantic_mapping()
    curated_by_key: dict[tuple[str, str], dict[str, Any]] = {}
    unmatched_curated = []
    function_keys = {(item["sourceAsset"], item["sourceName"]) for item in functions}
    for entry in curated:
        source = entry["udsSource"]
        if ": " in source:
            asset, name = source.split(": ", 1)
        else:
            asset, name = source, source.rsplit("/", 1)[-1]
        key = (asset, name)
        if key in function_keys:
            enriched = dict(entry)
            enriched.update(mappingKind="FUNCTION", sourceAsset=asset, sourceName=name)
            curated_by_key[key] = enriched
        else:
            supplemental = dict(entry)
            supplemental.update(
                mappingKind="CROSS_SYSTEM_OR_PARAMETER",
                sourceAsset=asset,
                sourceName=name,
            )
            unmatched_curated.append(supplemental)

    result = []
    curated_function_count = 0
    for function in functions:
        key = (function["sourceAsset"], function["sourceName"])
        mapped = curated_by_key.get(key)
        if mapped is not None:
            curated_function_count += 1
            mapped["decodedEvidence"] = {
                "nodeCount": function.get("nodeCount"),
                "edgeCount": function.get("edgeCount"),
                "reads": function.get("reads", []),
                "modifies": function.get("modifies", []),
                "calls": function.get("calls", []),
            }
            result.append(mapped)
            continue
        reads = function.get("reads", [])
        modifies = function.get("modifies", [])
        calls = function.get("calls", [])
        result.append(
            {
                "mappingKind": "FUNCTION",
                "sourceAsset": function["sourceAsset"],
                "sourceName": function["sourceName"],
                "udsSource": f"{function['sourceAsset']}: {function['sourceName']}",
                "udsBehavior": {
                    "status": "PARTIAL",
                    "description": "decoded graph topology; expression semantics not yet fully decompiled",
                    "nodeCount": function.get("nodeCount"),
                    "edgeCount": function.get("edgeCount"),
                    "reads": reads,
                    "modifies": modifies,
                    "calls": calls,
                },
                "requiredVisualResult": (
                    "preserve every decoded state/output dependency; exact visual effect remains UNKNOWN"
                ),
                "proposedSolumFilamentImplementation": (
                    "NOT_MAPPED: decompile this function before any dependent runtime implementation"
                ),
                "currentDifferences": "no audited one-to-one runtime implementation",
                "estimatedGpuCost": "UNKNOWN",
                "estimatedCpuCost": "UNKNOWN",
                "riskOfBehaviorLoss": "high",
                "verification": "function-specific input/output fixtures plus runtime reachability evidence",
                "prohibitedSimplifications": [
                    "claim VERIFIED from graph presence alone",
                    "replace decoded dependencies with guessed constants",
                    "drop the function because Filament has no direct analogue",
                ],
                "status": "NOT_IMPLEMENTED",
            }
        )
    result.extend(unmatched_curated)
    return result, {
        "decodedFunctionCount": len(functions),
        "functionMappingCount": sum(item["mappingKind"] == "FUNCTION" for item in result),
        "curatedFunctionCount": curated_function_count,
        "genericPendingFunctionCount": len(functions) - curated_function_count,
        "supplementalMappingCount": len(unmatched_curated),
    }


def checkpoint_markdown(
    parameters: list[dict[str, Any]],
    functions: list[dict[str, Any]],
    assets: list[dict[str, Any]],
    curves: list[dict[str, Any]],
    coverage: list[dict[str, Any]],
    blueprint_sources: list[dict[str, Any]],
    extraction_gate: dict[str, Any],
    semantic_summary: dict[str, int],
) -> str:
    extraction = Counter(item["extractionStatus"] for item in parameters)
    implementation = Counter(item["status"] for item in coverage)
    asset_statuses = Counter(item["status"] for item in assets)
    blueprint_parameter_count = sum(item.get("parameterCount", 0) for item in blueprint_sources)
    blueprint_graph_count = sum(item.get("graphCount", 0) for item in blueprint_sources)
    material_parameter_count = len(parameters) - blueprint_parameter_count
    material_graph_count = len(functions) - blueprint_graph_count
    required_failures = len(extraction_gate.get("requiredFailures", []))
    optional_failures = sum(
        1 for item in assets if not item["required"] and item["status"] != "VERIFIED"
    )
    extraction_gate_status = extraction_gate.get("status", "UNKNOWN")
    selected_targets = extraction_gate.get("selectedTargetCount", 0)
    verified_targets = extraction_gate.get("verifiedTargetCount", 0)
    return f"""# P63.10 Checkpoint 00 — UDS semantic contract

Status: **PARTIAL**. Completion/DONE is prohibited.

## Required outcome

Publish the source-backed parameter/function/asset contract, semantic mapping and honest
implementation coverage before any new render-code edits.

## UDS evidence used

- {len(blueprint_sources)} decoded UDS/UDW Blueprints: {blueprint_parameter_count} CDO/NewVariables
  parameters and {blueprint_graph_count} Kismet graphs.
- Decoded Material/Material Function contracts: {material_parameter_count} parameters and
  {material_graph_count} expression graphs.
- {len(assets)} explicitly selected extraction targets; no filename-similarity selection.
- {len(curves)} decoded authored curves with exact channel/key contracts.
- Current SOLUM runtime/shader audit and latest user QA observations.

## Contract coverage

- Parameters: {len(parameters)} ({dict(extraction)}).
- Functions: {len(functions)}.
- Semantic function mapping: {semantic_summary.get("functionMappingCount", 0)}/{semantic_summary.get("decodedFunctionCount", 0)};
  curated={semantic_summary.get("curatedFunctionCount", 0)}, pending exact decompilation={semantic_summary.get("genericPendingFunctionCount", 0)}.
- Assets: {len(assets)} ({dict(asset_statuses)}).
- Full extraction gate: **{extraction_gate_status}**, selected={selected_targets}/{len(TARGETS)},
  verified={verified_targets}/{len(TARGETS)}, required failures={required_failures}, optional failures={optional_failures}.
- Curves: {len(curves)}.
- Mandatory implementation requirements: {len(coverage)} ({dict(implementation)}).

Every parameter contains source name/asset/type/default/range/units/coordinate space/update
evidence/dependencies/readers/writers/visual effect/extraction status. `UNKNOWN` and `PARTIAL`
are preserved; absent evidence is not replaced with guessed values.

## Files produced

- `P63_10_UDS_SYSTEM_CONTRACT.json`
- `P63_10_UDS_COVERAGE_MATRIX.json`
- `P63_10_UDS_SEMANTIC_MAPPING.json`
- `CHECKPOINT_00_CONTRACT.md`

## Code/functions implemented

No render behavior was changed in this checkpoint. The evidence compiler is
`tools/build_p63_10_uds_system_contract.py`.

## Missing / approximations / blockers

- Every selected package and its raw contract was inspected, but {len(assets) - verified_targets}
  texture payloads remain undecoded; the exact Oodle backend is unavailable. Required failures are
  {required_failures}; no visually similar or filename-only payload is accepted as recovery.
- {semantic_summary.get("genericPendingFunctionCount", 0)} decoded function entries still require
  function-specific semantic decompilation; graph presence alone is not treated as implementation.
- Sun trajectory is an APPROXIMATION; exact UDS astronomy is not runtime authority.
- Moon trajectory is NOT_IMPLEMENTED independently; phase/orientation remain PARTIAL.
- Atmosphere is an APPROXIMATION against the UDS material graph.
- Clouds are not world-position anchored under camera translation and do not have independent
  runtime layers.
- Dynamic analytic-sky IBL and exact weather transitions are NOT_IMPLEMENTED.
- Canonical render screenshots/runtime-camera-layer bundles are BLOCKED on user-device QA.

## Tests for this checkpoint

- JSON parse/schema/status checks.
- Private absolute-path leak scan.
- Contract generator self-test.
- Independent read-only review is required before Stage 1 edits.

## Why this checkpoint is not complete system parity

It establishes traceability and prevents the previous build-success/visible-sky state from being
misreported as semantic completion. Visual parity remains subject to staged implementation and
explicit user acceptance.
"""


def build(dataset: Path, output: Path, qa_root: Path | None = None) -> dict[str, Any]:
    blueprint_specs = (
        (
            dataset / "contracts/Blueprints__Ultra_Dynamic_Sky.blueprint.json",
            BLUEPRINT_ASSET,
        ),
        (
            dataset / "contracts/Blueprints__Ultra_Dynamic_Weather.blueprint.json",
            "/Game/UltraDynamicSky/Blueprints/Ultra_Dynamic_Weather",
        ),
        (
            dataset
            / "contracts/Blueprints__System__UDS_PlayerOcclusion.blueprint.json",
            "/Game/UltraDynamicSky/Blueprints/System/UDS_PlayerOcclusion",
        ),
    )
    blueprint_sources = []
    parameters: list[dict[str, Any]] = []
    functions: list[dict[str, Any]] = []
    for blueprint_path, source_asset in blueprint_specs:
        if not blueprint_path.is_file():
            blueprint_sources.append(
                {"asset": source_asset, "status": "NOT_EXTRACTED", "completionEligible": False}
            )
            continue
        blueprint = read_json(blueprint_path)
        if blueprint.get("status") != "RAW_VERIFIED":
            raise ValueError(
                f"Blueprint contract is not RAW_VERIFIED: {source_asset}: {blueprint.get('status')}"
            )
        source_parameters, source_functions = build_parameters(blueprint, source_asset)
        parameters.extend(source_parameters)
        functions.extend(source_functions)
        blueprint_sources.append(
            {
                "asset": source_asset,
                "status": blueprint.get("status"),
                "sha256": blueprint.get("source", {}).get("sha256"),
                "exportCount": len(blueprint.get("exports", [])),
                "graphCount": len(blueprint.get("graph", {}).get("graphs", [])),
                "parameterCount": len(source_parameters),
            }
        )
    if not parameters:
        raise FileNotFoundError("no decoded UDS/UDW Blueprint contract")
    material_parameters, material_functions = build_material_contracts(dataset)
    parameters.extend(material_parameters)
    functions.extend(material_functions)
    parameters.sort(key=lambda item: (item["sourceAsset"], item["sourceName"].lower()))
    functions.sort(key=lambda item: (item["sourceAsset"], item["sourceName"].lower()))
    assets = build_asset_inventory(dataset)
    curves = build_curves(dataset, assets)
    coverage = build_coverage()
    semantic_mapping, semantic_summary = complete_semantic_mapping(functions)
    qa_evidence = build_qa_evidence(qa_root)
    extraction_gate = read_json(dataset / "P63_10_EXTRACTION_GATE.json")
    full_gate = (
        extraction_gate.get("selectedTargetCount") == len(TARGETS)
        and extraction_gate.get("status") == "PASS"
    )
    completion_blockers = [
        "parameters remain PARTIAL/UNKNOWN",
        "mandatory requirements remain non-VERIFIED",
        "canonical device capture set is absent",
        "independent reviewer and user visual acceptance are pending",
    ]
    if not full_gate:
        completion_blockers.append(
            f"full extraction gate not PASS: selected={extraction_gate.get('selectedTargetCount')}/{len(TARGETS)} status={extraction_gate.get('status')}"
        )
    if any(source["status"] == "NOT_EXTRACTED" for source in blueprint_sources):
        completion_blockers.append("UDS or UDW Blueprint contract is not extracted")

    contract = {
        "schema": "solum.p63.10.uds-system-contract/v1",
        "status": "PARTIAL",
        "completionEligible": False,
        "completionBlockers": completion_blockers,
        "sourcePolicy": {
            "authorityOrder": [
                "owned extracted UDS assets",
                "Blueprint/Kismet graphs",
                "Material and Material Function graphs",
                "defaults/metadata/curves/dependencies",
                "user UDS screenshots and QA",
            ],
            "privatePayload": "PRIVATE_PAID_OWNED_SOURCE_DO_NOT_REDISTRIBUTE",
            "guessedValues": "PROHIBITED_AS_VERIFIED",
        },
        "statusVocabulary": {
            "extraction": list(EXTRACTION_STATUSES),
            "implementation": list(IMPLEMENTATION_STATUSES),
        },
        "source": {
            "blueprints": blueprint_sources,
            "extractionGate": {
                "status": extraction_gate.get("status"),
                "selectedTargetCount": extraction_gate.get("selectedTargetCount"),
                "expectedTargetCount": len(TARGETS),
                "verifiedTargetCount": extraction_gate.get("verifiedTargetCount"),
                "requiredFailures": extraction_gate.get("requiredFailures", []),
                "fullGate": full_gate,
            },
        },
        "parameters": parameters,
        "functions": functions,
        "curves": curves,
        "assets": assets,
    }
    coverage_counts = Counter(item["status"] for item in coverage)
    verified_coverage = coverage_counts.get("VERIFIED", 0)
    matrix = {
        "schema": "solum.p63.10.uds-coverage-matrix/v1",
        "status": "PARTIAL",
        "completionEligible": False,
        "summary": {
            "counts": dict(coverage_counts),
            "totalRequirementCount": len(coverage),
            "verifiedRequirementCount": verified_coverage,
            "verifiedCoveragePercent": round(verified_coverage * 100.0 / max(1, len(coverage)), 3),
        },
        "requirements": coverage,
        "runtimeControlReachability": build_control_reachability_audit(),
        "qaEvidence": qa_evidence,
        "canonicalCaptures": {
            "status": "BLOCKED",
            "owner": "USER_DEVICE_VISUAL_QA",
            "requiredScenarios": [
                "noon",
                "sunset",
                "night",
                "sun-at-horizon",
                "moon-at-horizon",
                "clear",
                "medium-cloud",
                "dense-cloud",
                "camera-up",
                "camera-horizon",
                "camera-rotated-90-degrees",
                "camera-translated-in-world",
            ],
            "requiredBundle": [
                "image",
                "runtime parameters JSON",
                "camera transform",
                "time/weather state",
                "active cloud layer list",
            ],
        },
    }
    mapping = {
        "schema": "solum.p63.10.uds-semantic-mapping/v1",
        "status": "PARTIAL",
        "completionEligible": False,
        "summary": semantic_summary,
        "entries": semantic_mapping,
    }

    write_json(output / "P63_10_UDS_SYSTEM_CONTRACT.json", contract)
    write_json(output / "P63_10_UDS_COVERAGE_MATRIX.json", matrix)
    write_json(output / "P63_10_UDS_SEMANTIC_MAPPING.json", mapping)
    (output / "CHECKPOINT_00_CONTRACT.md").write_text(
        checkpoint_markdown(
            parameters,
            functions,
            assets,
            curves,
            coverage,
            blueprint_sources,
            extraction_gate,
            semantic_summary,
        ),
        encoding="utf-8",
    )
    return {
        "parameters": len(parameters),
        "functions": len(functions),
        "assets": len(assets),
        "curves": len(curves),
        "coverage": len(coverage),
        "parameterStatuses": dict(Counter(item["extractionStatus"] for item in parameters)),
        "coverageStatuses": dict(Counter(item["status"] for item in coverage)),
        "semanticMapping": semantic_summary,
        "qaEvidenceStatuses": dict(
            Counter(item["hashVerificationStatus"] for item in qa_evidence)
        ),
        "fullExtractionGate": full_gate,
    }


def self_test(output: Path, summary: dict[str, Any]) -> None:
    if summary["parameters"] < 800:
        raise AssertionError("Blueprint parameter coverage unexpectedly low")
    if summary["functions"] < 500:
        raise AssertionError("Blueprint graph coverage unexpectedly low")
    if summary["assets"] < 60:
        raise AssertionError("asset inventory unexpectedly low")
    if summary["coverage"] < 30:
        raise AssertionError("mandatory coverage matrix unexpectedly small")
    if summary["semanticMapping"]["functionMappingCount"] != summary["functions"]:
        raise AssertionError("semantic mapping does not cover every decoded function")
    for name in (
        "P63_10_UDS_SYSTEM_CONTRACT.json",
        "P63_10_UDS_COVERAGE_MATRIX.json",
        "P63_10_UDS_SEMANTIC_MAPPING.json",
    ):
        payload = read_json(output / name)
        if payload.get("status") != "PARTIAL" or payload.get("completionEligible") is not False:
            raise AssertionError(f"{name} must remain explicitly non-complete")
    rendered = "\n".join(path.read_text(encoding="utf-8") for path in output.iterdir() if path.is_file())
    forbidden = ("/storage/emulated/", "/data/data/com.termux/", "udsRoot")
    leaks = [token for token in forbidden if token in rendered]
    if leaks:
        raise AssertionError(f"private physical path leak: {leaks}")
    contract = read_json(output / "P63_10_UDS_SYSTEM_CONTRACT.json")
    matrix = read_json(output / "P63_10_UDS_COVERAGE_MATRIX.json")
    function_keys = {
        (item["sourceAsset"], item["sourceName"]) for item in contract["functions"]
    }
    function_assets = {asset for asset, _ in function_keys}
    parameter_keys = {
        (item["sourceAsset"], item["sourceName"]) for item in contract["parameters"]
    }
    logical_assets = {item["logicalAsset"] for item in contract["assets"]}
    parameter_ids = [item.get("sourceId") for item in contract["parameters"]]
    duplicate_parameter_ids = [
        source_id
        for source_id, count in Counter(parameter_ids).items()
        if source_id is None or count != 1
    ]
    if duplicate_parameter_ids:
        raise AssertionError(
            f"parameter expression/variable identities are not unique: {duplicate_parameter_ids[:20]}"
        )
    missing_material_dependencies = []
    for function in contract["functions"]:
        if function.get("functionKind") != "MATERIAL_GRAPH":
            continue
        for call in function.get("calls", []):
            if not call.startswith("/Game/UltraDynamicSky/"):
                continue
            called_asset = call.rsplit(".", 1)[0] if "." in call else call
            if called_asset not in function_assets:
                missing_material_dependencies.append(
                    (function["sourceAsset"], called_asset)
                )
    if missing_material_dependencies:
        raise AssertionError(
            "material dependency closure is incomplete: "
            f"{sorted(set(missing_material_dependencies))}"
        )
    missing_sources = []
    for requirement_item in matrix["requirements"]:
        for source in requirement_item["udsSources"]:
            if source.startswith("docs/"):
                valid = (ROOT / source).is_file()
            elif ": " in source:
                asset, source_name = source.split(": ", 1)
                if source_name.startswith("parameter "):
                    source_name = source_name.removeprefix("parameter ")
                    source_name = re.sub(r" \([^()]+\)$", "", source_name)
                    valid = (asset, source_name) in parameter_keys
                else:
                    valid = (asset, source_name) in function_keys or (
                        asset, source_name
                    ) in parameter_keys
            else:
                valid = source in logical_assets or source in function_assets
            if not valid:
                missing_sources.append((requirement_item["id"], source))
    if missing_sources:
        raise AssertionError(f"coverage uses nonexistent UDS sources: {missing_sources}")
    rejected_qa = [
        item["scenario"]
        for item in matrix["qaEvidence"]
        if item["hashVerificationStatus"] == "REJECTED_MISSING_OR_HASH_MISMATCH"
    ]
    if rejected_qa:
        raise AssertionError(f"QA evidence missing or hash mismatch: {rejected_qa}")
    falsely_verified = [
        item["sourceName"]
        for item in contract["parameters"]
        if item["extractionStatus"] == "VERIFIED"
        and item["updateFrequency"].get("status") == "UNKNOWN"
    ]
    if falsely_verified:
        raise AssertionError(f"VERIFIED parameters have unknown update frequency: {falsely_verified}")
    packaged_manifest_root = ROOT / "apps/engine/src/main/assets/env/p63"
    packaged_manifests = {
        path: path.read_text(encoding="utf-8")
        for path in packaged_manifest_root.glob("*.json")
    }
    manifest = packaged_manifests[
        packaged_manifest_root / "P63_3_SKY_TRUTH_MANIFEST.json"
    ]
    manifest_forbidden = (
        "/mnt/",
        "/data/data/com.termux/",
        "/storage/emulated/0/Download/SOLUM_ASSET_LAB",
        "_work/private_uds_p63_10",
    )
    manifest_leaks = [
        (path.name, token)
        for path, payload in packaged_manifests.items()
        for token in manifest_forbidden
        if token in payload
    ]
    if manifest_leaks:
        raise AssertionError(f"APK manifest contains private physical path: {manifest_leaks}")
    false_exact_manifest_tokens = (
        "UDS_EXACT_TILING_STARS_TEXTURE",
        "FILAMENT_VOLUME_RAYMARCH_UDS_EXACT",
        "VERIFIED_MILKY_WAY_TEXTURE",
        "VERIFIED_ATMOSPHERE_TRANSMITTANCE_MULTISCATTER_AERIAL_LUTS",
    )
    false_exact_manifest = [
        token for token in false_exact_manifest_tokens if token in manifest
    ]
    if false_exact_manifest:
        raise AssertionError(
            f"APK manifest overclaims incomplete runtime semantics: {false_exact_manifest}"
        )
    resource_labels = (
        ROOT
        / "apps/engine/src/main/java/com/solum/engine/environment/p63/"
        "SolumAnalyticSkyResources.java"
    ).read_text(encoding="utf-8")
    false_exact_runtime_tokens = (
        "+STARS_NOISE+TILING_STARS_UVS\"",
        "+CLOUD_PROFILE+VOLUMETRIC_CLOUDS_EXTINCTION\"",
    )
    false_exact_runtime = [
        token for token in false_exact_runtime_tokens if token in resource_labels
    ]
    if false_exact_runtime:
        raise AssertionError(
            f"runtime diagnostics overclaim incomplete mappings: {false_exact_runtime}"
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--qa-root",
        type=Path,
        help="optional user-owned QA directory; only hashes/statuses enter generated reports",
    )
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    summary = build(args.dataset, args.output, args.qa_root)
    if args.self_test:
        self_test(args.output, summary)
    print(json.dumps({"status": "PASS", **summary}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
