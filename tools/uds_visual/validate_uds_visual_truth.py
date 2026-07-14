#!/usr/bin/env python3
"""Validate P62 generated UDS visual truth and write VISUAL_HTML_GATE.json."""

from __future__ import annotations

import argparse
import hashlib
import http.server
import json
import shutil
import socketserver
import subprocess
import sys
import tempfile
import threading
import urllib.request
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
UE_TOOL_SRC = REPO_ROOT / "tools" / "ue_asset_tool" / "src"
if str(UE_TOOL_SRC) not in sys.path:
    sys.path.insert(0, str(UE_TOOL_SRC))

from ueassettool.media import validate_wav  # noqa: E402


SCHEMA_VERSION = "solum.uds-visual/v1"
DATA_FILES = (
    "UDS_VISUAL_CONTRACT.json", "UDS_VISUAL_ASSET_MANIFEST.json",
    "UDS_VISUAL_EVIDENCE.json", "UDS_VISUAL_CAPABILITIES.json",
)
SCHEMA_MAP = {
    "UDS_VISUAL_CONTRACT.json": "uds_visual_contract.schema.json",
    "UDS_VISUAL_ASSET_MANIFEST.json": "uds_visual_asset_manifest.schema.json",
    "UDS_VISUAL_EVIDENCE.json": "uds_visual_evidence.schema.json",
    "UDS_VISUAL_CAPABILITIES.json": "uds_visual_capabilities.schema.json",
    "UDS_VISUAL_BUILD_REPORT.json": "uds_visual_build_report.schema.json",
}


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
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def schema_errors(value: Any, schema: dict[str, Any], pointer: str = "$") -> list[str]:
    """Validate the strict subset used by the committed P62 schemas."""
    errors: list[str] = []
    expected = schema.get("type")
    types = expected if isinstance(expected, list) else [expected] if expected else []
    type_map = {
        "object": dict, "array": list, "string": str, "integer": int,
        "number": (int, float), "boolean": bool, "null": type(None),
    }
    if types and not any(
        isinstance(value, type_map[item]) and not (item in ("integer", "number") and isinstance(value, bool))
        for item in types
    ):
        return [f"{pointer}: expected {types}, got {type(value).__name__}"]
    if "const" in schema and value != schema["const"]:
        errors.append(f"{pointer}: {value!r} != const {schema['const']!r}")
    if isinstance(value, dict):
        for key in schema.get("required", []):
            if key not in value:
                errors.append(f"{pointer}: missing required key {key}")
        properties = schema.get("properties", {})
        if schema.get("additionalProperties") is False:
            for key in value:
                if key not in properties:
                    errors.append(f"{pointer}: unexpected key {key}")
        for key, child in properties.items():
            if key in value:
                errors.extend(schema_errors(value[key], child, f"{pointer}.{key}"))
    if isinstance(value, list):
        if len(value) < schema.get("minItems", 0):
            errors.append(f"{pointer}: {len(value)} items < {schema['minItems']}")
        child = schema.get("items")
        if child:
            for index, item in enumerate(value):
                errors.extend(schema_errors(item, child, f"{pointer}[{index}]"))
    if isinstance(value, int) and not isinstance(value, bool) and "minimum" in schema and value < schema["minimum"]:
        errors.append(f"{pointer}: {value} < {schema['minimum']}")
    return errors


class Checks:
    def __init__(self) -> None:
        self.items: list[dict[str, Any]] = []

    def run(self, check_id: str, callback: Any) -> None:
        try:
            detail = callback()
            self.items.append({"id": check_id, "status": "PASS", "detail": detail or "ok"})
        except Exception as exc:  # validation aggregation is intentional
            self.items.append({"id": check_id, "status": "FAIL", "detail": str(exc)})

    @property
    def passed(self) -> bool:
        return all(item["status"] == "PASS" for item in self.items)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def generated_files(root: Path) -> list[Path]:
    return sorted(path for path in root.rglob("*") if path.is_file())


def validate(args: argparse.Namespace) -> tuple[dict[str, Any], bool]:
    root = Path(args.output).resolve()
    checks = Checks()
    loaded: dict[str, Any] = {}

    def load_required() -> str:
        required = [
            root / "index.html", root / "js" / "app.js", root / "js" / "renderer.js",
            root / "shaders" / "scene.vert", root / "shaders" / "scene.frag",
            root / "reports" / "UDS_VISUAL_BUILD_REPORT.json",
            *(root / "data" / name for name in DATA_FILES),
        ]
        missing = [path.relative_to(root).as_posix() for path in required if not path.is_file()]
        require(not missing, f"missing output files: {missing}")
        for path in required:
            if path.suffix == ".json":
                loaded[path.name] = read_json(path)
        return f"{len(required)} required files"

    checks.run("required_outputs", load_required)

    def schemas() -> str:
        require(loaded, "required JSON was not loaded")
        total = 0
        for name, schema_name in SCHEMA_MAP.items():
            value = loaded[name]
            schema = read_json(REPO_ROOT / "schemas" / schema_name)
            errors = schema_errors(value, schema)
            require(not errors, "; ".join(errors[:12]))
            total += 1
        return f"{total} schemas"

    checks.run("schema_validation", schemas)

    def stable_sorting() -> str:
        evidence = loaded["UDS_VISUAL_EVIDENCE.json"]
        contract = loaded["UDS_VISUAL_CONTRACT.json"]
        assets = loaded["UDS_VISUAL_ASSET_MANIFEST.json"]
        parameter_ids = [item["id"] for item in evidence["parameters"]]
        require(parameter_ids == sorted(parameter_ids), "parameters are not stably sorted")
        preset_ids = [item["id"] for item in contract["source_truth"]["weather_presets"]]
        require(preset_ids == sorted(preset_ids), "weather presets are not stably sorted")
        asset_ids = [item["id"] for item in assets["assets"]]
        require(asset_ids == sorted(asset_ids), "assets are not stably sorted")
        require(len(parameter_ids) == len(set(parameter_ids)), "duplicate parameter id")
        return f"{len(parameter_ids)} parameters, {len(preset_ids)} presets"

    checks.run("stable_sorting", stable_sorting)

    def evidence_integrity() -> str:
        evidence = loaded["UDS_VISUAL_EVIDENCE.json"]
        contract = loaded["UDS_VISUAL_CONTRACT.json"]
        capabilities = loaded["UDS_VISUAL_CAPABILITIES.json"]
        ids = {item["id"] for item in evidence["parameters"]}
        ids.update(f"preset:{item['id']}" for item in contract["source_truth"]["weather_presets"])
        ids.update({"browser:diagnostic-scene", "browser:ui", "browser:deterministic-reset", "browser:report-export"})
        require(set(contract["source_truth"]["parameter_ids"]) == {item["id"] for item in evidence["parameters"]}, "source_truth parameter references differ")
        for control in capabilities["controls"]:
            missing = [item for item in control["evidence_ids"] if item not in ids]
            require(not missing, f"control {control['id']} has missing evidence {missing}")
        root_ids = {item["package"] for item in evidence["roots"]}
        require(set(contract["source_truth"]["root_ids"]) == root_ids, "root references differ")
        return f"{len(ids)} evidence ids"

    checks.run("evidence_referential_integrity", evidence_integrity)

    def assets() -> str:
        manifest = loaded["UDS_VISUAL_ASSET_MANIFEST.json"]
        for item in manifest["assets"]:
            path = root / item["browser_path"]
            require(path.is_file(), f"missing asset {item['browser_path']}")
            require(path.stat().st_size == item["size"], f"size mismatch {item['id']}")
            require(sha256_file(path) == item["output_sha256"], f"hash mismatch {item['id']}")
            if item["format"] == "WAV":
                current = validate_wav(path.read_bytes())
                require(current == item["format_validation"], f"WAV validation changed {item['id']}")
        parameter_ids = {item["id"] for item in loaded["UDS_VISUAL_EVIDENCE.json"]["parameters"]}
        for item in manifest["unavailable_texture_sources"]:
            require(len(item["source_sha256"]) == 64, f"texture source hash invalid: {item['source_package']}")
            require(not item["browser_active"], f"unavailable texture is active: {item['source_package']}")
            require(set(item["root_parameter_references"]) <= parameter_ids, f"texture reference evidence unresolved: {item['source_package']}")
        return f"{len(manifest['assets'])} extracted assets, {len(manifest['unavailable_texture_sources'])} blocked texture sources"

    checks.run("asset_hash_and_format", assets)

    def no_host_paths() -> str:
        forbidden = ("/mnt/", "/data/data/", "/storage/", "/sdcard/", "/home/")
        for path in generated_files(root):
            if path.suffix.lower() not in (".json", ".html", ".js", ".css", ".vert", ".frag", ".txt"):
                continue
            text = path.read_text(encoding="utf-8")
            hits = [token for token in forbidden if token in text]
            require(not hits, f"host path leaked in {path.relative_to(root)}: {hits}")
        return "no host filesystem paths"

    checks.run("no_absolute_path_leakage", no_host_paths)

    def no_decorative_branches() -> str:
        forbidden = (
            "Math.random", "random(", "fbm(", "hash(", "proceduralCloud",
            "proceduralRain", "proceduralSnow", "inventedSky",
        )
        for path in generated_files(root):
            if path.suffix.lower() not in (".js", ".vert", ".frag"):
                continue
            text = path.read_text(encoding="utf-8")
            hits = [token for token in forbidden if token in text]
            require(not hits, f"decorative branch tokens in {path.relative_to(root)}: {hits}")
        return "no random/noise/weather synthesis branches"

    checks.run("no_decorative_visual_branch", no_decorative_branches)

    def control_truth() -> str:
        controls = loaded["UDS_VISUAL_CAPABILITIES.json"]["controls"]
        for item in controls:
            if item["active"]:
                require(item["evidence_ids"], f"active control without evidence: {item['id']}")
                require(item["status"] in ("STATE_ONLY", "VERIFIED_BROWSER"), f"active control has invalid status: {item['id']}")
            if item.get("visual_effect") and item["id"] not in ("camera",):
                require(False, f"unsupported active visual effect: {item['id']}")
        return f"{sum(item['active'] for item in controls)} active controls"

    checks.run("active_controls_have_evidence", control_truth)

    def preset_completeness() -> str:
        presets = loaded["UDS_VISUAL_CONTRACT.json"]["source_truth"]["weather_presets"]
        required = {
            "Clear_Skies", "Cloudy", "Foggy", "Overcast", "Partly_Cloudy", "Rain",
            "Rain_Light", "Rain_Thunderstorm", "Sand_Dust_Calm", "Sand_Dust_Storm",
            "Snow", "Snow_Blizzard", "Snow_Light",
        }
        found = {item["id"] for item in presets}
        require(required == found, f"weather preset set differs: missing={sorted(required-found)}, extra={sorted(found-required)}")
        for item in presets:
            require(item["evidence_status"] == "VERIFIED", f"preset not verified: {item['id']}")
            require(item["values"], f"empty preset: {item['id']}")
            require(set(item["values"]) == set(item["evidence"]), f"preset evidence mismatch: {item['id']}")
        return f"{len(presets)} exact presets"

    checks.run("weather_preset_completeness", preset_completeness)

    def curve_binding() -> str:
        evidence = loaded["UDS_VISUAL_EVIDENCE.json"]
        curves = evidence["curves"]
        require(curves, "no curves")
        for curve in curves:
            require(curve["evidence_status"] == "VERIFIED", f"curve not verified: {curve['id']}")
            require(sum(channel["key_count"] for channel in curve["channels"]) == curve["total_key_count"], f"curve key count mismatch: {curve['id']}")
            for channel in curve["channels"]:
                indices = [item["index"] for item in channel["keys"]]
                require(indices == list(range(len(indices))), f"curve key indices unstable: {curve['id']}")
        visual_curve_controls = [item for item in loaded["UDS_VISUAL_CAPABILITIES.json"]["controls"] if item["active"] and item["id"] in {"sun","moon","stars","clouds","fog","rain","snow","wind"}]
        require(not visual_curve_controls, "curve-driven control active without a complete browser binding")
        return f"{len(curves)} curves / {sum(item['total_key_count'] for item in curves)} keys; browser bindings locked"

    checks.run("curve_timeline_binding", curve_binding)

    def niagara_parameters() -> str:
        contracts = loaded["UDS_VISUAL_EVIDENCE.json"]["niagara_contracts"]
        require(contracts, "no Niagara contracts")
        parameter_count = 0
        for item in contracts:
            require(len(item["contract_sha256"]) == 64, f"invalid Niagara contract hash: {item['id']}")
            store = item["decoded_parameter_store"]
            count = item["niagara_parameter_count"]
            require(count == (store["parameter_count"] if store else 0), f"Niagara store count mismatch: {item['id']}")
            require(count == (len(store["parameters"]) if store else 0), f"Niagara item count mismatch: {item['id']}")
            if store:
                raw = store["container_evidence"]["raw"]
                require(raw["size"] > 0 and len(raw["sha256"]) == 64, f"invalid Niagara container evidence: {item['id']}")
                for parameter in store["parameters"]:
                    require(parameter.get("name"), f"unnamed Niagara parameter: {item['id']}")
                    data = bytes.fromhex(parameter["data_hex"])
                    require(len(data) == parameter["data_size"], f"Niagara data size mismatch: {parameter['name']}")
                    require(hashlib.sha256(data).hexdigest() == parameter["data_sha256"], f"Niagara data hash mismatch: {parameter['name']}")
            parameter_count += count
            for script in item["scripts"]:
                require(script["status"] in ("VERIFIED", "RAW_VERIFIED"), f"unsupported Niagara script normalized as usable: {script['object']}")
        require(parameter_count == 137, f"expected 137 decoded Niagara parameters, got {parameter_count}")
        controls = {item["id"]: item for item in loaded["UDS_VISUAL_CAPABILITIES.json"]["controls"]}
        require(not controls["rain"]["active"] and not controls["snow"]["active"] and not controls["lightning"]["active"], "particle VFX active with partial Niagara semantics")
        return f"{len(contracts)} Niagara contracts / {parameter_count} exact parameters; VFX controls locked"

    checks.run("niagara_parameter_validation", niagara_parameters)

    def audio_binding() -> str:
        items = loaded["UDS_VISUAL_ASSET_MANIFEST.json"]["assets"]
        for item in items:
            if item["browser_active"]:
                require(item["event_binding_status"] == "VERIFIED", f"active audio lacks event binding: {item['id']}")
            else:
                require(item["event_binding_status"] in ("UNKNOWN", "PARTIAL"), f"inactive audio status unexpected: {item['id']}")
        audio = next(item for item in loaded["UDS_VISUAL_CAPABILITIES.json"]["controls"] if item["id"] == "audio")
        require(not audio["active"], "audio control must remain locked without event binding")
        return f"{len(items)} exact payloads, zero active bindings"

    checks.run("audio_binding_validation", audio_binding)

    def browser_manifest() -> str:
        caps = loaded["UDS_VISUAL_CAPABILITIES.json"]
        contract = loaded["UDS_VISUAL_CONTRACT.json"]
        evidence = loaded["UDS_VISUAL_EVIDENCE.json"]
        require(caps["renderer"] == "WebGL2" and caps["offline"] and not caps["cdn"], "browser capability flags invalid")
        require(caps["mobile_ui"]["panel_max_viewport_fraction"] <= 0.4, "panel exceeds 40% viewport")
        require(caps["mobile_ui"]["touch_camera"] and caps["mobile_ui"]["camera_ground_clamp"], "touch camera requirements absent")
        require(contract["map_gate"]["unchanged"] and not contract["map_gate"]["demo_map_rendered"], "MAP_GATE/DemoMap policy violated")
        sun = next(item for item in caps["systems"] if item["system"] == "sun")
        require(sun["active_visual"] and sun["webgl_adapter"] == "SOURCE_VERIFIED_BROWSER_ADAPTER", "static source-backed sun adapter is not declared")
        light = contract["browser_adapter_mapping"]["diagnostic_scene"]["directional_light"]
        parameter_ids = {item["id"] for item in evidence["parameters"]}
        require(set(light["evidence"]["parameter_ids"]) <= parameter_ids, "sun adapter parameter evidence is unresolved")
        require(light["evidence"]["mpc_parameter"] == "Sun Vector", "sun adapter MPC vector binding differs")
        texts = "\n".join(path.read_text(encoding="utf-8") for path in generated_files(root) if path.suffix.lower() in (".html", ".js", ".css", ".vert", ".frag"))
        require("https://" not in texts and "http://" not in texts and "cdn" not in texts.lower(), "external URL/CDN token in browser files")
        return "WebGL2, offline, touch-first, static source-backed sun, MAP_GATE unchanged"

    checks.run("browser_manifest_validation", browser_manifest)

    def js_syntax() -> str:
        node = shutil.which("node")
        require(node is not None, "node is unavailable")
        for path in (root / "js" / "app.js", root / "js" / "renderer.js"):
            result = subprocess.run([node, "--check", str(path)], capture_output=True, text=True, timeout=20)
            require(result.returncode == 0, f"{path.name}: {result.stderr.strip()}")
        return "node --check: 2 modules"

    checks.run("html_js_syntax", js_syntax)

    def local_server() -> str:
        class Quiet(http.server.SimpleHTTPRequestHandler):
            def log_message(self, _format: str, *_args: Any) -> None:
                return

        handler = lambda *handler_args, **kwargs: Quiet(*handler_args, directory=str(root), **kwargs)
        server = socketserver.TCPServer(("127.0.0.1", 0), handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            base = f"http://127.0.0.1:{server.server_address[1]}"
            for relative in ("/", "/data/UDS_VISUAL_CONTRACT.json", "/shaders/scene.vert"):
                with urllib.request.urlopen(base + relative, timeout=5) as response:
                    body = response.read()
                    require(response.status == 200 and body, f"server smoke failed: {relative}")
        finally:
            server.shutdown(); server.server_close(); thread.join(timeout=3)
        return "index, contract and shader served over localhost"

    checks.run("local_server_smoke", local_server)

    def deterministic() -> str:
        if not args.determinism:
            return "not requested"
        require(args.p60 and args.p61, "--p60 and --p61 required for deterministic rebuild")
        with tempfile.TemporaryDirectory(prefix="solum-p62-rebuild-") as directory:
            target = Path(directory) / "preview"
            command = [
                sys.executable, str(Path(__file__).with_name("build_uds_visual_truth.py")),
                "--p60", args.p60, "--p61", args.p61, "--output", str(target),
            ]
            result = subprocess.run(command, capture_output=True, text=True, timeout=900)
            require(result.returncode == 0, f"rebuild failed: {result.stderr.strip()[-3000:]}")
            validator_owned = {
                "reports/VISUAL_HTML_GATE.json", "reports/UDS_VISUAL_VALIDATION.json",
            }
            original = {
                path.relative_to(root).as_posix(): sha256_file(path)
                for path in generated_files(root)
                if path.relative_to(root).as_posix() not in validator_owned
            }
            rebuilt = {
                path.relative_to(target).as_posix(): sha256_file(path)
                for path in generated_files(target)
                if path.relative_to(target).as_posix() not in validator_owned
            }
            require(original == rebuilt, f"rebuild differs: only-original={sorted(original.keys()-rebuilt.keys())}, only-rebuilt={sorted(rebuilt.keys()-original.keys())}, changed={sorted(key for key in original.keys() & rebuilt.keys() if original[key] != rebuilt[key])}")
        return f"{len(original)} byte-identical files"

    checks.run("deterministic_rebuild", deterministic)

    semantic_blockers = [
        "no UDS visual system has a complete evidence-backed WebGL2 adapter",
        "Oodle-backed TextureSource payloads are not decoded on this host",
        "Niagara/material/MetaSound runtime semantics are partial",
        "user visual review has not occurred",
    ]
    gate = {
        "schema_version": SCHEMA_VERSION,
        "gate": "VISUAL_HTML_GATE",
        "status": "FAIL",
        "technical_validation": "PASS" if checks.passed else "FAIL",
        "automatic_visual_equivalence_claim": False,
        "checks": checks.items,
        "blockers": semantic_blockers + (["one or more technical checks failed"] if not checks.passed else []),
        "map_gate_unchanged": True,
        "runtime_gate": "NOT_RUN",
        "filament_gate": "NOT_RUN",
    }
    reports = root / "reports"
    reports.mkdir(exist_ok=True)
    write_json(reports / "VISUAL_HTML_GATE.json", gate)
    write_json(reports / "UDS_VISUAL_VALIDATION.json", {
        "schema_version": SCHEMA_VERSION,
        "status": gate["technical_validation"],
        "checks": checks.items,
        "visual_gate": gate["status"],
    })
    return gate, checks.passed


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"))
    result.add_argument("--p60")
    result.add_argument("--p61")
    result.add_argument("--determinism", action="store_true")
    return result


def main() -> int:
    args = parser().parse_args()
    gate, passed = validate(args)
    print(json.dumps({
        "technical_validation": gate["technical_validation"],
        "visual_html_gate": gate["status"],
        "passed": sum(item["status"] == "PASS" for item in gate["checks"]),
        "failed": sum(item["status"] == "FAIL" for item in gate["checks"]),
    }, ensure_ascii=False, sort_keys=True))
    return 0 if passed else 2


if __name__ == "__main__":
    raise SystemExit(main())
