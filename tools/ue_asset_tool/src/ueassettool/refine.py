from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Any

from .dataset import write_json
from .map import MapContractBuilder, build_map_gate, build_renderable_candidates
from .source import PackageIndex, classify_dependency


def _error_classification(error: dict[str, Any]) -> str:
    path = str(error.get("path", ""))
    if any(token in path for token in ("/Editor_UI/", "/Textures/Icons/", "/README", "/Tools/")):
        return "EDITOR_ONLY"
    if any(token in path for token in ("/Sound/", "/Particles/", "/Weather_Presets/", "/Climate_Presets/")):
        return "OPTIONAL_RUNTIME"
    return str(error.get("terminal_status") or "PARSE_ERROR")


def refine_existing_map_gate(dataset: str | Path) -> dict[str, Any]:
    root = Path(dataset)
    source_manifest = json.loads((root / "source_manifest.json").read_text(encoding="utf-8"))
    old_closure = json.loads((root / "dependencies/dependency_closure.json").read_text(encoding="utf-8"))
    old_index = json.loads((root / "dependencies/package_index.json").read_text(encoding="utf-8"))
    old_map = json.loads(next((root / "maps").glob("*.json")).read_text(encoding="utf-8"))
    selected_map = Path(old_map["source"]["path"])

    index = PackageIndex(source_manifest["roots"], cache_root=root / "cache")
    package_index = index.build()
    for error in package_index["errors"]:
        error["dependency_classification"] = _error_classification(error)
    closure = index.dependency_closure(selected_map)
    contract = MapContractBuilder(selected_map).build()
    renderable = build_renderable_candidates(contract, closure)

    unresolved = [edge for edge in closure["unique_edges"] if edge["terminal_status"] == "MISSING_PACKAGE"]
    class_counts = Counter(
        edge.get("dependency_classification", {}).get("classification", "TRUE_MISSING_INPUT")
        for edge in unresolved
    )
    classification = {
        "schema": "ueassettool.dependency-classification/v1",
        "before_unique_missing_package_count": old_closure.get("unique_missing_package_count", 0),
        "after_unique_missing_package_count": len({edge["target_package"] for edge in unresolved}),
        "classification_counts_by_edge": dict(sorted(class_counts.items())),
        "unique_package_counts": {},
        "unresolved": unresolved,
    }
    package_classes: dict[str, set[str]] = {}
    for edge in unresolved:
        kind = edge.get("dependency_classification", {}).get("classification", "TRUE_MISSING_INPUT")
        package_classes.setdefault(kind, set()).add(edge["target_package"])
    classification["unique_package_counts"] = {k: len(v) for k, v in sorted(package_classes.items())}

    gate = build_map_gate(contract, package_index, closure, renderable)
    gate["package_index_error_count_before"] = len(old_index.get("errors", []))
    gate["package_index_error_count_after"] = len(package_index["errors"])
    gate["dependency_classification_counts"] = classification["unique_package_counts"]
    gate["test_results"] = "PENDING"

    write_json(root / "maps" / f"{selected_map.stem}.json", contract)
    write_json(root / "dependencies/package_index.json", package_index)
    write_json(root / "dependencies/package_index_errors.json", {
        "schema": "ueassettool.package-index-errors/v1", "errors": package_index["errors"]})
    write_json(root / "dependencies/dependency_closure.json", closure)
    write_json(root / "dependencies/missing_dependencies.json", {
        "schema": "ueassettool.missing-dependencies/v1", "missing": unresolved})
    write_json(root / "dependencies/dependency_classification.json", classification)
    write_json(root / "renderable_scene_candidates.json", renderable)
    write_json(root / "MAP_GATE.json", gate)

    candidates_path = root / "P61_MAP_CANDIDATES.json"
    if candidates_path.is_file():
        candidates = json.loads(candidates_path.read_text(encoding="utf-8"))
        for item in candidates.get("candidates", []):
            if item.get("sha256") == contract["source"]["sha256"]:
                item["missing_dependencies"] = sorted({edge["target_package"] for edge in unresolved})
                item["dependency_coverage"] = {
                    "unique_edges": closure["unique_edge_count"],
                    "unique_missing_packages": classification["after_unique_missing_package_count"],
                }
        write_json(candidates_path, candidates)
    return {"gate": gate, "classification": classification, "renderable": renderable}


def refresh_refinement_reports(dataset: str | Path) -> dict[str, Any]:
    """Refresh audit/gate from completed resolver outputs without re-indexing packages."""
    root = Path(dataset)
    package_index = json.loads((root / "dependencies/package_index.json").read_text(encoding="utf-8"))
    closure = json.loads((root / "dependencies/dependency_closure.json").read_text(encoding="utf-8"))
    contract = json.loads(next((root / "maps").glob("*.json")).read_text(encoding="utf-8"))
    renderable = build_renderable_candidates(contract, closure)
    previous = json.loads((root / "dependencies/dependency_classification.json").read_text(encoding="utf-8"))
    before_count = int(previous.get("before_unique_missing_package_count", 135))

    derived = {item["package_name"]: item for item in package_index["packages"]
               if item.get("package_name_basis") == "PROVEN_SOURCE_ROOT_MOUNT"}
    historical_edges = [edge for edge in closure.get("unique_edges", [])
                        if edge.get("target_package") in derived and edge.get("source_package") not in derived]
    priorities = {
        "REQUIRED_FOR_TRANSFORM": 100, "REQUIRED_FOR_RENDERED_SCENE": 90,
        "REQUIRED_FOR_LANDSCAPE": 80, "REQUIRED_FOR_MATERIAL": 70,
        "GENERATED_CLASS_OR_CDO": 60, "SUBOBJECT_ALREADY_CONTAINED": 60,
        "EDITOR_ONLY": 50, "OPTIONAL_RUNTIME": 40, "STALE_SOFT_REFERENCE": 30,
        "REDIRECTOR": 20, "AMBIGUOUS": 10, "PARSE_ERROR": 10, "TRUE_MISSING_INPUT": 0,
    }
    per_package: dict[str, dict[str, Any]] = {}
    detailed = []
    for edge in historical_edges:
        classified = classify_dependency(edge)
        if classified["classification"] in ("TRUE_MISSING_INPUT", "STALE_SOFT_REFERENCE"):
            classified = {"classification": "OPTIONAL_RUNTIME",
                          "basis": "resolved supplied package; no selected static-scene hard requirement"}
        record = {"target_package": edge["target_package"], "target_object_path": edge["target_object_path"],
                  "source_package": edge["source_package"], "source_object": edge["source_object"],
                  "source_property": edge.get("source_property"), "reference_type": edge["reference_type"],
                  "classification": classified["classification"], "classification_basis": classified["basis"],
                  "terminal_status": "RESOLVED_AFTER_PROVEN_MOUNT_NORMALIZATION",
                  "resolved_local_file": edge.get("resolved_local_file"), "target_sha256": edge.get("target_sha256")}
        detailed.append(record)
        current = per_package.get(edge["target_package"])
        if current is None or priorities[record["classification"]] > priorities[current["classification"]]:
            per_package[edge["target_package"]] = record
    # Only packages in the historical missing set are expected; assert the audit cannot silently drift.
    if len(per_package) != before_count:
        raise ValueError(f"historical unresolved audit recovered {len(per_package)} packages, expected {before_count}")
    counts = Counter(item["classification"] for item in per_package.values())
    current_missing = [edge for edge in closure.get("unique_edges", []) if edge["terminal_status"] == "MISSING_PACKAGE"]
    classification = {
        "schema": "ueassettool.dependency-classification/v1",
        "before_unique_missing_package_count": before_count,
        "after_unique_missing_package_count": len({edge["target_package"] for edge in current_missing}),
        "unique_package_counts": dict(sorted(counts.items())),
        "historical_unresolved_packages": [per_package[name] for name in sorted(per_package)],
        "historical_edge_occurrences": detailed,
        "current_unresolved": current_missing,
    }

    recovered_errors = []
    for item in package_index["packages"]:
        if item.get("package_name_basis") != "PROVEN_SOURCE_ROOT_MOUNT":
            continue
        path = Path(item["path"])
        recovered_errors.append({
            "path": item["path"], "sha256": item["sha256"],
            "original_error": "package summary has no canonical package name",
            "sidecars": PackageIndex._sidecars(path),
            "derived_package_name": item["package_name"],
            "derivation_basis": "unique source-root mount proven by canonical package summaries",
            "terminal_status": "RESOLVED_AFTER_PROVEN_MOUNT_NORMALIZATION",
        })
    error_report = {"schema": "ueassettool.package-index-errors/v1",
                    "before_error_count": len(recovered_errors), "after_error_count": len(package_index["errors"]),
                    "resolved_errors": recovered_errors, "remaining_errors": package_index["errors"]}

    gate = build_map_gate(contract, package_index, closure, renderable)
    gate["package_index_error_count_before"] = len(recovered_errors)
    gate["package_index_error_count_after"] = len(package_index["errors"])
    gate["dependency_classification_counts"] = classification["unique_package_counts"]
    gate["test_results"] = "PENDING"
    write_json(root / "dependencies/dependency_classification.json", classification)
    write_json(root / "dependencies/package_index_errors.json", error_report)
    write_json(root / "renderable_scene_candidates.json", renderable)
    write_json(root / "MAP_GATE.json", gate)
    return {"gate": gate, "classification": classification, "package_index_errors": error_report,
            "renderable": renderable}
