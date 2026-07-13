from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from typing import Any, Iterable

from .contracts import export_auto_contract, verify_package
from .errors import UEAssetError
from .media import export_media
from .mesh import export_static_mesh, validate_glb
from .package import UnrealPackage
from .schema import validate_json_file


SCHEMA_VERSION = "solum.uds-truth/v1"
TERMINAL_STATUSES = {
    "VERIFIED", "PARTIAL_VERIFIED", "RAW_VERIFIED", "MISSING_INPUT",
    "MISSING_SIDECAR", "UNSUPPORTED_VERSION", "UNSUPPORTED_FORMAT",
    "DECODE_ERROR", "INTEGRITY_ERROR",
}
FAILED_STATUSES = {
    "MISSING_INPUT", "MISSING_SIDECAR", "UNSUPPORTED_VERSION",
    "DECODE_ERROR", "INTEGRITY_ERROR",
}
REFERENCE_ROOTS = (
    "/mnt/shared/Download/SOLUM_UASSET_TRUTH_READER_V1",
    "/mnt/shared/Download/SOLUM_ASSET_LAB",
    "/mnt/shared/Download/SOLUM_UASSET_READER_REPORTS",
    "/mnt/shared/Download/SOLUM_UDS_WORK",
    "/mnt/shared/Download/SOLUM_UE_TRANSLATED",
    "/mnt/shared/Download/Ultra_Dynamic_Sky_v9.4___40_5.5-5.7_____41__EXTRACTED/Ultra Dynamic Sky v9.4 (5.5-5.7​)",
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _json_bytes(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def write_json(path: Path, value: Any) -> dict[str, Any]:
    raw = _json_bytes(value)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_bytes(raw)
    temporary.replace(path)
    return {"path": str(path), "size": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}


def _safe_zip_name(name: str) -> PurePosixPath:
    value = PurePosixPath(name)
    if value.is_absolute() or ".." in value.parts or not value.parts:
        raise ValueError(f"unsafe ZIP member {name!r}")
    return value


def prepare_archive(archive: Path, dataset: Path) -> tuple[Path, list[dict[str, Any]], dict[str, str]]:
    input_root = dataset / "_input" / "P59"
    input_root.mkdir(parents=True, exist_ok=True)
    records: list[dict[str, Any]] = []
    expected: dict[str, str] = {}
    with zipfile.ZipFile(archive) as source:
        bad = source.testzip()
        if bad:
            raise ValueError(f"ZIP CRC failure in {bad}")
        infos = source.infolist()
        if any(item.is_dir() for item in infos):
            infos = [item for item in infos if not item.is_dir()]
        for item in infos:
            relative = _safe_zip_name(item.filename)
            raw = source.read(item)
            target = input_root.joinpath(*relative.parts)
            actual = hashlib.sha256(raw).hexdigest()
            if not target.is_file() or target.stat().st_size != len(raw) or sha256_file(target) != actual:
                target.parent.mkdir(parents=True, exist_ok=True)
                temporary = target.with_name(target.name + ".tmp")
                temporary.write_bytes(raw)
                temporary.replace(target)
            records.append({
                "archive_member": item.filename,
                "absolute_path": str(target),
                "size": len(raw),
                "sha256": actual,
                "crc32": f"{item.CRC:08x}",
                "status": "VERIFIED",
                "asset_ids": [],
            })
            if item.filename == "SHA256SUMS.txt":
                for line in raw.decode("utf-8").splitlines():
                    if "  " in line:
                        digest, name = line.split("  ", 1)
                        expected[name] = digest
    for record in records:
        member = record["archive_member"]
        if member.startswith("assets/"):
            wanted = expected.get(member)
            if wanted is None or wanted != record["sha256"]:
                raise ValueError(f"SHA256SUMS mismatch or absence for {member}")
    return input_root, records, expected


def _classify_contract(contract: dict[str, Any]) -> str:
    schema = str(contract.get("schema", ""))
    return {
        "ueassettool.material-contract/v1": "materials",
        "ueassettool.blueprint-contract/v1": "blueprints",
        "ueassettool.niagara-contract/v1": "niagara",
        "ueassettool.curve-contract/v1": "curves",
    }.get(schema, "assets/contracts")


def _package_status(contract: dict[str, Any], inspection: dict[str, Any]) -> str:
    if int(inspection["integrity"]["missing_exports"]):
        return "MISSING_SIDECAR"
    value = contract.get("status")
    if value == "VERIFIED":
        return "VERIFIED"
    if value == "RAW_VERIFIED":
        return "PARTIAL_VERIFIED"
    if value == "UNSUPPORTED":
        return "RAW_VERIFIED"
    return "DECODE_ERROR"


def _walk(value: Any, path: str = "$") -> Iterable[tuple[str, Any]]:
    yield path, value
    if isinstance(value, dict):
        for key, child in value.items():
            yield from _walk(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from _walk(child, f"{path}[{index}]")


def _raw_regions(contract: dict[str, Any]) -> list[dict[str, Any]]:
    found: dict[tuple[Any, Any, Any], dict[str, Any]] = {}
    candidates: list[tuple[str, dict[str, Any]]] = []

    def collect(value: Any, path: str) -> None:
        if isinstance(value, dict):
            if str(value.get("decode_status", "")) in ("raw", "skipped"):
                raw = value.get("raw")
                if isinstance(raw, dict):
                    candidates.append((f"{path}.raw", raw))
            for key in ("trailing_native", "pin_stream", "derived_native_tail"):
                raw = value.get(key)
                if isinstance(raw, dict):
                    candidates.append((f"{path}.{key}", raw))
            for key, child in value.items():
                collect(child, f"{path}.{key}")
        elif isinstance(value, list):
            for index, child in enumerate(value):
                collect(child, f"{path}[{index}]")

    collect(contract, "$")
    for path, value in candidates:
        if not isinstance(value, dict):
            continue
        if not {"physical_offset", "size", "sha256"} <= value.keys():
            continue
        if not isinstance(value.get("physical_offset"), int) or not isinstance(value.get("size"), int):
            continue
        key = (value["physical_offset"], value["size"], value["sha256"])
        found.setdefault(key, {
            "json_path": path,
            "source_file": contract.get("source", {}).get("path"),
            "byte_offset": value["physical_offset"],
            "byte_length": value["size"],
            "sha256": value["sha256"],
            "status": "RAW_VERIFIED",
            "reason": "bounded bytes retained without a complete semantic layout",
        })
    return list(found.values())


def _unsupported_regions(contract: dict[str, Any]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()
    for path, value in _walk(contract):
        if not isinstance(value, dict):
            continue
        status = value.get("status") or value.get("validation")
        if status not in ("UNSUPPORTED", "RAW_VERIFIED"):
            continue
        reason = value.get("reason") or value.get("decode_note")
        if not reason and value.get("issues"):
            reason = "; ".join(map(str, value["issues"]))
        if not reason:
            continue
        key = (path, str(reason))
        if key in seen:
            continue
        seen.add(key)
        result.append({"json_path": path, "status": "UNSUPPORTED_FORMAT", "reason": str(reason)})
    for item in contract.get("unsupported", []):
        result.append({
            "json_path": "$.unsupported",
            "status": "UNSUPPORTED_FORMAT",
            "reason": item.get("reason") or "property layout unsupported",
            "property": item.get("property"),
            "provenance": item.get("provenance"),
        })
    return result


def _contract_metrics(contract: dict[str, Any]) -> dict[str, int]:
    graph = contract.get("graph", {})
    bytecode = contract.get("bytecode", {})
    metrics = {
        "material_graphs": int(contract.get("schema") == "ueassettool.material-contract/v1" and graph.get("status") != "NOT_APPLICABLE"),
        "material_nodes": int(graph.get("node_count", 0)) if "material" in str(contract.get("schema")) else 0,
        "material_links": int(graph.get("link_count", 0)),
        "material_parameters": len(contract.get("parameters", [])),
        "material_functions": sum(root.get("class") in ("MaterialFunction", "MaterialFunctionInstance") for root in contract.get("roots", [])),
        "mic": len(contract.get("material_instances", [])),
        "mpc": len(contract.get("parameter_collections", [])),
        "scalar_parameters": 0,
        "vector_parameters": 0,
        "texture_parameters": 0,
        "static_switch_parameters": 0,
        "curves": int(contract.get("schema") == "ueassettool.curve-contract/v1"),
        "curve_keys": int(contract.get("total_key_count", 0)),
        "blueprint_graphs": len(graph.get("graphs", [])) if "blueprint" in str(contract.get("schema")) else 0,
        "blueprint_nodes": int(graph.get("node_count", 0)) if "blueprint" in str(contract.get("schema")) else 0,
        "blueprint_pins": int(graph.get("pin_count", 0)) if "blueprint" in str(contract.get("schema")) else 0,
        "blueprint_links": int(graph.get("edge_count", 0)) if "blueprint" in str(contract.get("schema")) else 0,
        "functions": int(bytecode.get("function_count", 0)),
        "bytecode_expressions": sum(int(item.get("script", {}).get("expression_count", 0)) for item in bytecode.get("functions", [])),
        "niagara_systems": len(contract.get("systems", [])),
        "niagara_emitters": sum(int(item.get("emitter_count", 0)) for item in contract.get("systems", [])),
        "niagara_parameters": sum(int(item.get("exposed_parameter_count", 0)) for item in contract.get("systems", [])),
        "niagara_nodes": int(graph.get("node_count", 0)) if "niagara" in str(contract.get("schema")) else 0,
        "map_actors": 0,
        "verified_transforms": 0,
    }
    for instance in contract.get("material_instances", []):
        metrics["scalar_parameters"] += len(instance.get("scalar_parameters", []))
        metrics["vector_parameters"] += len(instance.get("vector_parameters", []))
        metrics["texture_parameters"] += len(instance.get("texture_parameters", []))
        metrics["static_switch_parameters"] += len(instance.get("static_switch_parameters", []))
    for collection in contract.get("parameter_collections", []):
        metrics["scalar_parameters"] += len(collection.get("scalar_parameters", []))
        metrics["vector_parameters"] += len(collection.get("vector_parameters", []))
    if "niagara" in str(contract.get("schema")):
        for _path, value in _walk(contract.get("exports", [])):
            if isinstance(value, dict) and value.get("name") == "Parameters":
                decoded = value.get("value")
                if isinstance(decoded, dict) and isinstance(decoded.get("items"), list):
                    metrics["niagara_parameters"] = max(metrics["niagara_parameters"], len(decoded["items"]))
    return metrics


def _try_media(source: Path, output_base: Path, kind: str) -> tuple[dict[str, Any] | None, list[str]]:
    suffixes = (".png", ".jpg") if kind == "texture" else (".wav", ".ogg")
    errors = []
    for suffix in suffixes:
        target = output_base.with_suffix(suffix)
        try:
            return export_media(source, target, kind=kind), errors
        except (UEAssetError, OSError, UnicodeError) as exc:
            errors.append(f"{suffix}: {type(exc).__name__}: {exc}")
    return None, errors


def _reference_match(source: Path, relative: Path) -> list[dict[str, Any]]:
    matches = []
    source_size = source.stat().st_size
    source_hash = sha256_file(source)
    for root_text in REFERENCE_ROOTS:
        root = Path(root_text)
        if not root.is_dir():
            continue
        for candidate in (root / relative, root / "assets" / relative):
            if not candidate.is_file():
                continue
            size = candidate.stat().st_size
            digest = sha256_file(candidate)
            companions = []
            for suffix in (".uexp", ".ubulk", ".uptnl"):
                sidecar = candidate.with_suffix(suffix)
                companions.append({
                    "path": str(sidecar), "present": sidecar.is_file(),
                    "size": sidecar.stat().st_size if sidecar.is_file() else None,
                    "sha256": sha256_file(sidecar) if sidecar.is_file() else None,
                })
            matches.append({
                "absolute_path": str(candidate), "size": size, "sha256": digest,
                "content_identical": size == source_size and digest == source_hash,
                "companions": companions,
            })
    return matches


def _run_check(command: list[str], cwd: Path, log: Path, name: str) -> dict[str, Any]:
    environment = os.environ.copy()
    source_root = str(cwd / "tools" / "ue_asset_tool" / "src")
    environment["PYTHONPATH"] = os.pathsep.join(
        item for item in (source_root, environment.get("PYTHONPATH", "")) if item
    )
    process = subprocess.run(
        command, cwd=cwd, env=environment, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    )
    text = process.stdout
    with log.open("a", encoding="utf-8") as target:
        target.write(f"\n$ {' '.join(command)}\n{text}")
    return {"name": name, "command": command, "exit_code": process.returncode, "passed": process.returncode == 0}


def paid_asset_policy_check(repo: Path) -> dict[str, Any]:
    forbidden = {".uasset", ".umap", ".uexp", ".ubulk", ".uptnl", ".wav", ".ogg", ".glb"}
    process = subprocess.run(
        ["git", "ls-files"], cwd=repo, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )
    violations = [name for name in process.stdout.splitlines() if Path(name).suffix.lower() in forbidden]
    return {"name": "paid_asset_policy", "passed": process.returncode == 0 and not violations, "violations": violations}


def validate_asset_record(record: dict[str, Any]) -> list[str]:
    required = (
        "schema_version", "asset_id", "package_path", "source_file", "source_sha256",
        "asset_class", "package_version", "custom_versions", "extraction_status",
        "verified_fields", "raw_verified_regions", "unsupported_regions", "missing_inputs",
        "imports", "exports", "dependencies", "generated_outputs",
        "generated_output_sha256", "provenance", "validation_results",
    )
    errors = [f"missing {name}" for name in required if name not in record]
    if record.get("extraction_status") not in TERMINAL_STATUSES:
        errors.append(f"invalid extraction_status {record.get('extraction_status')}")
    if not isinstance(record.get("asset_id"), str) or not record.get("asset_id"):
        errors.append("asset_id is empty")
    return errors


def validate_dataset(dataset: Path) -> dict[str, Any]:
    errors: list[str] = []
    inventory_path = dataset / "inventory.json"
    if not inventory_path.is_file():
        return {"status": "INTEGRITY_ERROR", "errors": ["inventory.json missing"]}
    inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    schema_dir = Path(__file__).resolve().parents[2] / "schemas"
    errors.extend(
        f"inventory schema: {item}"
        for item in validate_json_file(inventory_path, schema_dir / "inventory.schema.json")
    )
    ids: set[str] = set()
    package_files = [item for item in inventory.get("files", []) if item.get("kind") == "package"]
    for item in inventory.get("assets", []):
        asset_id = item.get("asset_id")
        path = dataset / str(item.get("json_path"))
        if asset_id in ids:
            errors.append(f"duplicate asset id {asset_id}")
        ids.add(asset_id)
        if not path.is_file():
            errors.append(f"asset JSON missing: {path}")
            continue
        record = json.loads(path.read_text(encoding="utf-8"))
        errors.extend(
            f"{asset_id} schema: {item}"
            for item in validate_json_file(path, schema_dir / "asset.schema.json")
        )
        errors.extend(f"{asset_id}: {error}" for error in validate_asset_record(record))
        if record.get("asset_id") != asset_id:
            errors.append(f"inventory/asset id mismatch for {asset_id}")
        for output in record.get("generated_outputs", []):
            output_path = Path(output["path"])
            if not output_path.is_file():
                errors.append(f"generated output missing: {output_path}")
            elif sha256_file(output_path) != output.get("sha256"):
                errors.append(f"generated output hash mismatch: {output_path}")
            if asset_id not in output.get("owning_asset_ids", []):
                errors.append(f"generated output has no owner {asset_id}: {output_path}")
    if len(package_files) != inventory.get("totals", {}).get("packages"):
        errors.append("package file total mismatch")
    if any(not item.get("status") for item in inventory.get("files", [])):
        errors.append("inventory contains a file without terminal status")
    return {
        "status": "VERIFIED" if not errors else "INTEGRITY_ERROR",
        "asset_count": len(ids), "package_count": len(package_files), "errors": errors,
    }


def _git_info(repo: Path) -> dict[str, Any]:
    def call(*args: str) -> str:
        return subprocess.run(["git", *args], cwd=repo, text=True, stdout=subprocess.PIPE, check=True).stdout.strip()
    initial = call("merge-base", "HEAD", "origin/main")
    commits = call("log", "--format=%H %s", f"{initial}..HEAD").splitlines()
    return {"initial_commit": initial, "current_commit": call("rev-parse", "HEAD"), "created_commits": commits}


def build_dataset(archive: Path, dataset: Path, repo: Path) -> dict[str, Any]:
    started = datetime.now(timezone.utc).isoformat()
    dataset.mkdir(parents=True, exist_ok=True)
    for relative in (
        "assets/contracts", "materials", "blueprints", "niagara", "curves", "maps",
        "media/textures", "media/audio", "media/models", "reports",
    ):
        (dataset / relative).mkdir(parents=True, exist_ok=True)
    input_root, file_records, expected = prepare_archive(archive, dataset)
    archive_sha256 = sha256_file(archive)
    by_member = {item["archive_member"]: item for item in file_records}
    package_paths = sorted((input_root / "assets").rglob("*.uasset")) + sorted((input_root / "assets").rglob("*.umap"))
    assets: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    provenance_assets: list[dict[str, Any]] = []
    class_counts: Counter[str] = Counter()
    status_counts: Counter[str] = Counter()
    metrics: Counter[str] = Counter()
    output_counts: Counter[str] = Counter()

    for ordinal, source in enumerate(package_paths, 1):
        relative = source.relative_to(input_root / "assets")
        member = f"assets/{relative.as_posix()}"
        member_record = by_member[member]
        member_record["kind"] = "package"
        try:
            with UnrealPackage(source) as package:
                inspection = package.inspect_dict()
                top = [(index, export) for index, export in enumerate(package.exports, 1) if export.is_asset]
                if not top:
                    top = [(1, package.exports[0])] if package.exports else []
                top_paths = {index: package.object_path(index) for index, _export in top}
                asset_ids = [f"p59-{package.sha256[:20]}-e{index}" for index, _export in top]
                package_identity = {
                    "package_name": package.summary.package_name,
                    "file_version_ue4": package.summary.file_version_ue4,
                    "file_version_ue5": package.summary.file_version_ue5,
                    "licensee_version": package.summary.file_version_licensee,
                    "saved_hash": package.summary.saved_hash,
                }
                foundations = {
                    "summary": inspection["summary"],
                    "names": inspection["names"],
                    "soft_object_paths": inspection["soft_object_paths"],
                    "imports": inspection["imports"],
                    "exports": inspection["exports"],
                    "soft_package_references": inspection["soft_package_references"],
                    "depends_map": inspection["depends_map"],
                    "preload_dependencies": inspection["preload_dependencies"],
                }
            contract = export_auto_contract(source)
            verification = verify_package(source)
            contract_metrics = _contract_metrics(contract)
            metrics.update(contract_metrics)
            raw_regions = _raw_regions(contract)
            unsupported = _unsupported_regions(contract)
            status = _package_status(contract, inspection)
            contract_category = _classify_contract(contract)
            contract_path = dataset / contract_category / f"p59-{member_record['sha256'][:20]}.json"
            contract_wrapper = {
                "schema_version": SCHEMA_VERSION,
                "owning_asset_ids": asset_ids,
                "source_file": str(source),
                "source_sha256": member_record["sha256"],
                "contract": contract,
                "metrics": contract_metrics,
            }
            contract_output = write_json(contract_path, contract_wrapper)
            contract_output.update({"kind": "contract", "owning_asset_ids": asset_ids, "status": "VERIFIED"})
            for asset_id, (export_index, export) in zip(asset_ids, top):
                generated = [contract_output]
                validation_results: dict[str, Any] = {
                    "package": verification,
                    "contract_status": contract.get("status"),
                    "contract_metrics": contract_metrics,
                }
                asset_unsupported = list(unsupported)
                if export.class_name == "Texture2D":
                    manifest, attempts = _try_media(source, dataset / "media/textures" / asset_id, "texture")
                    if manifest:
                        out = manifest["output"]
                        generated.append({**out, "kind": "texture", "owning_asset_ids": [asset_id], "status": "VERIFIED"})
                        validation_results["texture"] = manifest
                        output_counts["textures"] += 1
                    else:
                        asset_unsupported.append({"status": "UNSUPPORTED_FORMAT", "region": "TextureSource", "reason": "; ".join(attempts)})
                elif export.class_name in ("VolumeTexture", "TextureCube"):
                    asset_unsupported.append({
                        "status": "UNSUPPORTED_FORMAT", "region": "TextureSource",
                        "reason": f"{export.class_name} semantic-preserving export is not implemented",
                    })
                elif export.class_name == "SoundWave":
                    manifest, attempts = _try_media(source, dataset / "media/audio" / asset_id, "audio")
                    if manifest:
                        out = manifest["output"]
                        generated.append({**out, "kind": "audio", "owning_asset_ids": [asset_id], "status": "VERIFIED"})
                        validation_results["audio"] = manifest
                        output_counts["audio"] += 1
                    else:
                        asset_unsupported.append({"status": "UNSUPPORTED_FORMAT", "region": "SoundWave.RawData", "reason": "; ".join(attempts)})
                elif export.class_name == "StaticMesh":
                    target = dataset / "media/models" / f"{asset_id}.glb"
                    try:
                        manifest = export_static_mesh(source, target)
                        glb = validate_glb(target)
                        generated.append({"path": str(target), "size": target.stat().st_size, "sha256": sha256_file(target), "kind": "model", "owning_asset_ids": [asset_id], "status": "VERIFIED"})
                        validation_results["model"] = {**manifest, "glb_validation": glb}
                        output_counts["models"] += 1
                    except (UEAssetError, OSError, UnicodeError) as exc:
                        asset_unsupported.append({"status": "UNSUPPORTED_FORMAT", "region": "StaticMesh", "reason": f"{type(exc).__name__}: {exc}"})
                if status == "VERIFIED" and (raw_regions or asset_unsupported):
                    asset_status = "PARTIAL_VERIFIED"
                else:
                    asset_status = status
                record = {
                    "schema_version": SCHEMA_VERSION,
                    "asset_id": asset_id,
                    "package_path": package_identity["package_name"],
                    "source_file": str(source),
                    "source_sha256": member_record["sha256"],
                    "asset_class": export.class_name,
                    "package_version": package_identity,
                    "custom_versions": inspection["summary"]["custom_versions"],
                    "extraction_status": asset_status,
                    "verified_fields": {
                        "package_foundation": foundations,
                        "asset_export_index": export_index,
                        "asset_object": top_paths[export_index],
                        "contract_schema": contract.get("schema"),
                        "contract_metrics": contract_metrics,
                    },
                    "raw_verified_regions": raw_regions,
                    "unsupported_regions": asset_unsupported,
                    "missing_inputs": [item for item in verification.get("companions", []) if item.get("present") is False and Path(item["path"]).suffix == ".uexp" and int(inspection["integrity"]["missing_exports"])],
                    "imports": foundations["imports"],
                    "exports": foundations["exports"],
                    "dependencies": {
                        "imports": contract.get("dependencies", []),
                        "depends_map": foundations["depends_map"][export_index - 1] if export_index <= len(foundations["depends_map"]) else None,
                        "preload": foundations["exports"][export_index - 1].get("preload_dependencies", {}),
                        "soft_object_paths": foundations["soft_object_paths"],
                        "soft_package_references": foundations["soft_package_references"],
                    },
                    "generated_outputs": generated,
                    "generated_output_sha256": [item["sha256"] for item in generated],
                    "provenance": {
                        "source_archive": str(archive),
                        "source_archive_sha256": archive_sha256,
                        "archive_member": member,
                        "archive_member_sha256": expected.get(member),
                        "export_payload": {
                            "source_file": export.payload_source,
                            "byte_offset": export.payload_physical_offset,
                            "byte_length": export.serial_size,
                            "sha256": export.payload_sha256,
                        },
                        "reference_matches": _reference_match(source, relative),
                    },
                    "validation_results": validation_results,
                }
                schema_errors = validate_asset_record(record)
                if schema_errors:
                    record["extraction_status"] = "INTEGRITY_ERROR"
                    record["validation_results"]["schema_errors"] = schema_errors
                asset_json = dataset / "assets" / f"{asset_id}.json"
                write_json(asset_json, record)
                assets.append({
                    "asset_id": asset_id, "asset_class": export.class_name,
                    "package_path": package_identity["package_name"], "source_file": str(source),
                    "source_sha256": member_record["sha256"],
                    "extraction_status": record["extraction_status"],
                    "json_path": str(asset_json.relative_to(dataset)),
                    "contract_path": str(contract_path.relative_to(dataset)),
                })
                class_counts[str(export.class_name)] += 1
                status_counts[record["extraction_status"]] += 1
                member_record["asset_ids"].append(asset_id)
                provenance_assets.append({
                    "asset_id": asset_id, "source_file": str(source),
                    "source_sha256": member_record["sha256"],
                    "generated_outputs": generated,
                })
                for issue in asset_unsupported:
                    errors.append({"asset_id": asset_id, "source_file": str(source), **issue})
            member_record.update({
                "status": status, "package_identity": package_identity,
                "sidecars": verification["companions"],
            })
        except Exception as exc:
            member_record.update({"kind": "package", "status": "DECODE_ERROR", "error": f"{type(exc).__name__}: {exc}"})
            errors.append({"source_file": str(source), "status": "DECODE_ERROR", "reason": f"{type(exc).__name__}: {exc}"})

    for item in file_records:
        item.setdefault("kind", "manifest" if item["archive_member"].endswith(".json") else "checksum" if item["archive_member"].endswith(".txt") else "other")
    reference_summaries = []
    for value in REFERENCE_ROOTS:
        path = Path(value)
        if path.is_dir():
            file_count = sum(1 for item in path.rglob("*") if item.is_file())
            reference_summaries.append({"path": str(path), "present": True, "file_count": file_count})
        else:
            reference_summaries.append({"path": str(path), "present": False, "file_count": 0})
    inventory = {
        "schema_version": SCHEMA_VERSION,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "source_archive": {"path": str(archive), "size": archive.stat().st_size, "sha256": archive_sha256},
        "totals": {"input_files": len(file_records), "packages": len(package_paths), "assets": len(assets)},
        "files": file_records,
        "assets": assets,
        "reference_roots": reference_summaries,
    }
    write_json(dataset / "inventory.json", inventory)
    coverage = {
        "schema_version": SCHEMA_VERSION,
        "counts_by_asset_class": dict(class_counts.most_common()),
        "counts_by_status": {name: status_counts.get(name, 0) for name in sorted(TERMINAL_STATUSES)},
        "metrics": dict(metrics),
        "generated_outputs": dict(output_counts),
        "unsupported_asset_count": sum(bool(json.loads((dataset / item["json_path"]).read_text(encoding="utf-8"))["unsupported_regions"]) for item in assets),
    }
    write_json(dataset / "coverage.json", coverage)
    write_json(dataset / "errors.json", {"schema_version": SCHEMA_VERSION, "count": len(errors), "errors": errors})
    write_json(dataset / "provenance.json", {"schema_version": SCHEMA_VERSION, "assets": provenance_assets})

    reports = dataset / "reports"
    test_log = reports / "test_results.log"
    test_log.write_text("SOLUM UDS truth validation\n", encoding="utf-8")
    checks = [
        _run_check([sys.executable, "-m", "compileall", "-q", "tools/ue_asset_tool/src", "tools/ue_asset_tool/tests"], repo, test_log, "compileall"),
        _run_check([sys.executable, "-m", "unittest", "discover", "-s", "tools/ue_asset_tool/tests", "-v"], repo, test_log, "unit_and_regression_tests"),
        _run_check(["git", "diff", "--check"], repo, test_log, "git_diff_check"),
        paid_asset_policy_check(repo),
    ]
    integrity = validate_dataset(dataset)
    checks.append({"name": "html_dataset_integrity", "passed": integrity["status"] == "VERIFIED", "details": integrity})
    package_statuses = Counter(item.get("status") for item in file_records if item.get("kind") == "package")
    blockers = [
        {"asset_id": item.get("asset_id"), "source_file": item.get("source_file"), "status": item.get("status"), "reason": item.get("reason"), "region": item.get("region"), "json_path": item.get("json_path")}
        for item in errors
    ]
    ready = (
        len(file_records) == 343
        and len(package_paths) == 341
        and all(item.get("status") in TERMINAL_STATUSES for item in file_records)
        and len(assets) > 0
        and all(item.get("asset_ids") for item in file_records if item.get("kind") == "package")
        and all(item.get("extraction_status") in TERMINAL_STATUSES for item in assets)
        and all(item.get("passed") for item in checks)
        and integrity["status"] == "VERIFIED"
    )
    gate = {
        "schema_version": SCHEMA_VERSION,
        "gate_status": "PASSED" if ready else "FAILED",
        "ready_for_html": ready,
        "total_input_files": len(file_records),
        "total_packages": len(package_paths),
        "complete_packages": package_statuses.get("VERIFIED", 0),
        "partial_packages": package_statuses.get("PARTIAL_VERIFIED", 0) + package_statuses.get("RAW_VERIFIED", 0),
        "failed_packages": sum(package_statuses.get(item, 0) for item in FAILED_STATUSES),
        "counts_by_asset_class": dict(class_counts.most_common()),
        "VERIFIED_count": status_counts.get("VERIFIED", 0),
        "PARTIAL_VERIFIED_count": status_counts.get("PARTIAL_VERIFIED", 0),
        "RAW_VERIFIED_count": status_counts.get("RAW_VERIFIED", 0),
        "missing_input_count": status_counts.get("MISSING_INPUT", 0),
        "missing_sidecar_count": status_counts.get("MISSING_SIDECAR", 0),
        "unsupported_count": coverage["unsupported_asset_count"],
        "decode_error_count": status_counts.get("DECODE_ERROR", 0),
        "integrity_error_count": status_counts.get("INTEGRITY_ERROR", 0),
        "texture_outputs": output_counts.get("textures", 0),
        "model_outputs": output_counts.get("models", 0),
        "audio_outputs": output_counts.get("audio", 0),
        "Material_graph_count": metrics.get("material_graphs", 0),
        "Material_Function_count": metrics.get("material_functions", 0),
        "MIC_count": metrics.get("mic", 0),
        "MPC_count": metrics.get("mpc", 0),
        "Curve_count": metrics.get("curves", 0),
        "Curve_key_count": metrics.get("curve_keys", 0),
        "Blueprint_graph_count": metrics.get("blueprint_graphs", 0),
        "function_count": metrics.get("functions", 0),
        "bytecode_expression_count": metrics.get("bytecode_expressions", 0),
        "Niagara_system_count": metrics.get("niagara_systems", 0),
        "Niagara_emitter_count": metrics.get("niagara_emitters", 0),
        "Niagara_parameter_count": metrics.get("niagara_parameters", 0),
        "map_actor_count": metrics.get("map_actors", 0),
        "verified_transform_count": metrics.get("verified_transforms", 0),
        "test_results": checks,
        "unresolved_blockers": blockers,
        "started_at": started,
        "completed_at": datetime.now(timezone.utc).isoformat(),
    }
    write_json(dataset / "EXTRACTION_GATE.json", gate)
    git = _git_info(repo)
    report = f"""# SOLUM UDS Final Truth — P59

- Initial commit: `{git['initial_commit']}`
- Current commit: `{git['current_commit']}`
- Created commits: {len(git['created_commits'])}
- Input files: {len(file_records)}
- Packages: {len(package_paths)}
- Assets: {len(assets)}
- Gate: **{gate['gate_status']}**
- Dataset size at report time: {sum(item.stat().st_size for item in dataset.rglob('*') if item.is_file())} bytes

## Extraction depth

- Package structure: exact summary/name/soft-path/import/export/depends maps; preload table is empty in P59 and version-gated.
- Textures: only exact editor TextureSource layouts; unsupported volume/cube/platform payloads remain listed.
- Models: exact editor FMeshDescription only; cooked LOD resources are unsupported.
- Sound: only owning SoundWave bounded WAV/Ogg payloads; no synthetic audio.
- Curves: exact FRichCurve channels and keys.
- Materials / Functions: serialized expression graph, inputs, parameters and references; not HLSL.
- MIC / MPC: exact tagged parent, overrides and defaults with provenance.
- Blueprint graph: serialized nodes, pins and reciprocal links.
- Kismet bytecode: exact bounded EX_* stream; not C++.
- Executable Blueprint semantics: incomplete where native calls are not mapped.
- Niagara: graph/data contract only — not exactly executable.
- Maps / transforms: P59 contains no `.umap`; no actor placement is inferred.
- Full UDS runtime: **NOT YET EXECUTABLE — VERIFIED GRAPH/DATA CONTRACT ONLY**.

## Counts

```json
{json.dumps({'classes': dict(class_counts), 'statuses': dict(status_counts), 'metrics': dict(metrics), 'outputs': dict(output_counts)}, ensure_ascii=False, indent=2)}
```

## Unsupported and required inputs

All blockers are enumerated in `errors.json` and `EXTRACTION_GATE.json`. Missing sidecars are recorded per package. Further exact decoding requires the owning `.uexp`/`.ubulk`/`.uptnl` where declared, a licensed Oodle decoder for Oodle-compressed payloads, and version-matched complete cooked package sets for cooked-only mesh/texture layouts.

## Run

`python3 tools/ue_asset_tool/scripts/build_and_serve_uds_truth.py`

Stop with `Ctrl-C` in the serving terminal.
"""
    (dataset / "FINAL_REPORT.md").write_text(report, encoding="utf-8")
    return gate
