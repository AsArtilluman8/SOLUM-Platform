from __future__ import annotations

import hashlib
import math
from pathlib import Path
from typing import Any

from .errors import BoundsError
from .package import UnrealPackage
from .properties import PropertyParser


CURVE_CLASSES = {"CurveFloat", "CurveVector", "CurveLinearColor"}
INTERP_MODES = {0: "linear", 1: "constant", 2: "cubic", 3: "none"}
TANGENT_MODES = {0: "auto", 1: "user", 2: "break", 3: "none"}
TANGENT_WEIGHT_MODES = {0: "none", 1: "arrive", 2: "leave", 3: "both"}


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


def _decoded_fields(value: Any) -> dict[str, dict[str, Any]]:
    if not isinstance(value, dict) or not isinstance(value.get("properties"), list):
        return {}
    return {
        str(item["name"]): item
        for item in value["properties"]
        if str(item.get("decode_status", "")).startswith("decoded")
    }


def _finite(value: Any) -> bool:
    return isinstance(value, (int, float)) and math.isfinite(float(value))


def _key_provenance(source_path: Path, keys_property: dict[str, Any], index: int) -> dict[str, Any]:
    raw = keys_property["raw"]
    value = keys_property["value"]
    inner_tag = value.get("inner_tag") if isinstance(value, dict) else None
    first = int(raw["physical_offset"]) + 4
    if isinstance(inner_tag, dict):
        first += int(inner_tag["header_size"])
    offset = first + index * 27
    with source_path.open("rb") as source:
        source.seek(offset)
        payload = source.read(27)
    if len(payload) != 27:
        raise BoundsError(
            f"RichCurveKey {index} at 0x{offset:x} is truncated in {source_path}"
        )
    return {
        "source_file": str(source_path),
        "physical_offset": offset,
        "size": len(payload),
        "sha256": hashlib.sha256(payload).hexdigest(),
        "parent_property_sha256": raw["sha256"],
    }


def _rich_curve(source_path: Path, value: Any) -> dict[str, Any]:
    fields = _decoded_fields(value)
    key_property = fields.get("Keys")
    if key_property is None or not isinstance(key_property.get("value"), dict):
        return {"status": "UNSUPPORTED", "reason": "RichCurve.Keys is not exactly decoded", "keys": []}
    raw_keys = key_property["value"].get("items")
    if not isinstance(raw_keys, list):
        return {"status": "UNSUPPORTED", "reason": "RichCurve.Keys has no bounded item list", "keys": []}

    keys: list[dict[str, Any]] = []
    issues: list[str] = []
    previous_time: float | None = None
    for index, item in enumerate(raw_keys):
        if not isinstance(item, dict):
            issues.append(f"key {index} is not a decoded RichCurveKey")
            continue
        numeric_names = (
            "time", "value", "arrive_tangent", "arrive_tangent_weight",
            "leave_tangent", "leave_tangent_weight",
        )
        if any(not _finite(item.get(name)) for name in numeric_names):
            issues.append(f"key {index} contains a non-finite numeric field")
        time = item.get("time")
        if _finite(time) and previous_time is not None and float(time) < previous_time:
            issues.append(f"key {index} time is before the previous key")
        if _finite(time):
            previous_time = float(time)
        interp = int(item.get("interp_mode", -1))
        tangent = int(item.get("tangent_mode", -1))
        weight = int(item.get("tangent_weight_mode", -1))
        if interp not in INTERP_MODES:
            issues.append(f"key {index} has unknown interpolation mode {interp}")
        if tangent not in TANGENT_MODES:
            issues.append(f"key {index} has unknown tangent mode {tangent}")
        if weight not in TANGENT_WEIGHT_MODES:
            issues.append(f"key {index} has unknown tangent-weight mode {weight}")
        keys.append({
            "index": index,
            "interp_mode": {"value": interp, "name": INTERP_MODES.get(interp)},
            "tangent_mode": {"value": tangent, "name": TANGENT_MODES.get(tangent)},
            "tangent_weight_mode": {"value": weight, "name": TANGENT_WEIGHT_MODES.get(weight)},
            "time": item.get("time"),
            "value": item.get("value"),
            "arrive_tangent": item.get("arrive_tangent"),
            "arrive_tangent_weight": item.get("arrive_tangent_weight"),
            "leave_tangent": item.get("leave_tangent"),
            "leave_tangent_weight": item.get("leave_tangent_weight"),
            "provenance": _key_provenance(source_path, key_property, index),
        })

    def optional(name: str) -> Any:
        item = fields.get(name)
        return item.get("value") if item else None

    return {
        "status": "VERIFIED" if not issues else "UNSUPPORTED",
        "key_count": len(keys),
        "keys": keys,
        "pre_infinity_extrap": optional("PreInfinityExtrap"),
        "post_infinity_extrap": optional("PostInfinityExtrap"),
        "default_value": optional("DefaultValue"),
        "issues": issues,
        "keys_provenance": {
            "physical_offset": key_property["raw"]["physical_offset"],
            "size": key_property["raw"]["size"],
            "sha256": key_property["raw"]["sha256"],
            "inner_tag": key_property["value"].get("inner_tag"),
        },
    }


def export_curve_contract(path: str | Path) -> dict[str, Any]:
    asset = Path(path)
    with UnrealPackage(asset) as package:
        parser = PropertyParser(package)
        candidates = [
            (index, export)
            for index, export in enumerate(package.exports, 1)
            if export.class_name in CURVE_CLASSES and export.is_asset
        ]
        if len(candidates) != 1:
            return {
                "schema": "ueassettool.curve-contract/v1",
                "status": "UNSUPPORTED",
                "source": _source(package),
                "reason": f"expected one CurveFloat/CurveVector/CurveLinearColor asset, found {len(candidates)}",
                "channels": [],
            }
        index, export = candidates[0]
        source_path = Path(export.payload_source or asset)
        decoded = parser.parse_export(index)
        properties = decoded.get("properties", [])
        channel_names = {
            "CurveFloat": ["value"],
            "CurveVector": ["x", "y", "z"],
            "CurveLinearColor": ["r", "g", "b", "a"],
        }[str(export.class_name)]
        curve_properties = [item for item in properties if item.get("name") in ("FloatCurve", "FloatCurves")]
        channels: list[dict[str, Any]] = []
        issues: list[str] = []
        for item in curve_properties:
            array_index = int(item.get("array_index", 0))
            if array_index >= len(channel_names):
                issues.append(f"curve channel index {array_index} exceeds {export.class_name}")
                continue
            curve = _rich_curve(source_path, item.get("value"))
            channels.append({
                "index": array_index,
                "name": channel_names[array_index],
                "property": item.get("name"),
                "property_provenance": {
                    "header_physical_offset": item.get("header_physical_offset"),
                    "value_physical_offset": item.get("raw", {}).get("physical_offset"),
                    "size": item.get("size"),
                    "sha256": item.get("raw", {}).get("sha256"),
                    "decode_status": item.get("decode_status"),
                },
                **curve,
            })
            if curve["status"] != "VERIFIED":
                issues.extend(f"channel {array_index}: {reason}" for reason in curve.get("issues", []))
        channels.sort(key=lambda item: int(item["index"]))
        if len(channels) != len(channel_names):
            issues.append(f"decoded {len(channels)} of {len(channel_names)} required channels")
        status = "VERIFIED" if not issues and all(item["status"] == "VERIFIED" for item in channels) else "UNSUPPORTED"
        return {
            "schema": "ueassettool.curve-contract/v1",
            "status": status,
            "source": _source(package),
            "asset": {
                "export_index": index,
                "object": package.object_path(index),
                "class": export.class_name,
                "parse_status": decoded.get("parse_status"),
            },
            "channel_count": len(channels),
            "total_key_count": sum(int(item.get("key_count", 0)) for item in channels),
            "channels": channels,
            "issues": issues,
            "semantics": "Exact serialized FRichCurve channels and keys; no sampled or inferred values.",
        }
