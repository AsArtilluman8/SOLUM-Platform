#!/usr/bin/env python3
"""Validate the compact P62B SOLUM environment HTML and deterministic package."""

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
from typing import Any, Callable

import validate_uds_visual_truth as legacy_validation


REPO_ROOT = Path(__file__).resolve().parents[2]
PROVENANCE = {"UDS_VERIFIED", "UDS_DERIVED_MAPPING", "SOLUM_NATIVE", "UNKNOWN", "UNAVAILABLE"}
REQUIRED_PRESETS = {
    "Clear_Skies", "Cloudy", "Foggy", "Overcast", "Partly_Cloudy", "Rain",
    "Rain_Light", "Rain_Thunderstorm", "Sand_Dust_Calm", "Sand_Dust_Storm",
    "Snow", "Snow_Blizzard", "Snow_Light",
}
REQUIRED_MODULES = {
    "SolumEnvironmentState", "SolumTimeSystem", "SolumCelestialSystem",
    "SolumAtmosphereSystem", "SolumCloudSystem", "SolumFogSystem",
    "SolumWeatherController", "SolumPrecipitationSystem", "SolumWindSystem",
    "SolumLightningSystem", "SolumWetnessSystem", "SolumEnvironmentAudioSystem",
    "SolumEnvironmentLightingState",
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
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


class Checks:
    def __init__(self) -> None:
        self.items: list[dict[str, str]] = []

    def run(self, check_id: str, callback: Callable[[], str]) -> None:
        try:
            self.items.append({"id": check_id, "status": "PASS", "detail": callback() or "ok"})
        except Exception as exc:  # aggregate all validation failures
            self.items.append({"id": check_id, "status": "FAIL", "detail": str(exc)})

    @property
    def passed(self) -> bool:
        return all(item["status"] == "PASS" for item in self.items)


def generated_files(root: Path) -> list[Path]:
    return sorted(path for path in root.rglob("*") if path.is_file())


def validate(args: argparse.Namespace) -> tuple[dict[str, Any], bool]:
    root = Path(args.output).resolve()
    package_path = root / "data" / "solum_environment_package.json"
    checks = Checks()
    loaded: dict[str, Any] = {}

    def required_outputs() -> str:
        required = [
            root / "index.html", root / "css" / "app.css",
            root / "js" / "app.js", root / "js" / "renderer.js", root / "js" / "environment-core.js",
            root / "shaders" / "sky.vert", root / "shaders" / "sky.frag",
            root / "shaders" / "scene.vert", root / "shaders" / "scene.frag",
            root / "shaders" / "particle.vert", root / "shaders" / "particle.frag",
            root / "shaders" / "line.vert", root / "shaders" / "line.frag",
            package_path, root / "reports" / "SOLUM_ENVIRONMENT_BUILD_REPORT.json",
        ]
        missing = [path.relative_to(root).as_posix() for path in required if not path.is_file()]
        require(not missing, f"missing outputs: {missing}")
        loaded["package"] = read_json(package_path)
        loaded["report"] = read_json(root / "reports" / "SOLUM_ENVIRONMENT_BUILD_REPORT.json")
        return f"{len(required)} required files"

    checks.run("required_outputs", required_outputs)

    def schema_validation() -> str:
        require(loaded, "package not loaded")
        schema = read_json(REPO_ROOT / "schemas" / "solum_environment_package.schema.json")
        errors = legacy_validation.schema_errors(loaded["package"], schema)
        require(not errors, "; ".join(errors[:20]))
        require(package_path.stat().st_size < 250_000, f"compact package too large: {package_path.stat().st_size}")
        return f"schema valid; {package_path.stat().st_size} bytes"

    checks.run("compact_package_schema", schema_validation)

    def preset_completeness() -> str:
        presets = loaded["package"]["weatherPresets"]
        found = {item["id"] for item in presets}
        require(found == REQUIRED_PRESETS, f"preset set differs: missing={sorted(REQUIRED_PRESETS-found)}, extra={sorted(found-REQUIRED_PRESETS)}")
        required_runtime = {
            "cloudCoverage", "cloudDensity", "cloudProfileLow", "cloudProfileMid", "cloudProfileHigh",
            "fogDensity", "rain", "snow", "dust",
            "windDirectionDeg", "windSpeed", "windGust", "lightningPotential",
            "lightningEnabled", "wetnessTarget", "humidity", "atmosphereHaze",
            "lightingScale", "ambientScale", "exposure", "audioAutomatic",
        }
        for item in presets:
            require(item["status"] == "UDS_VERIFIED", f"preset source not verified: {item['id']}")
            require(required_runtime <= set(item["runtime"]), f"runtime fields missing: {item['id']}")
            require(set(item["runtime"]) == set(item["runtimeProvenance"]), f"runtime provenance mismatch: {item['id']}")
            for field, provenance in item["runtimeProvenance"].items():
                require(provenance["status"] in PROVENANCE, f"invalid provenance {item['id']}.{field}")
            for exact in item["exactUdsValues"].values():
                require(exact["status"] == "UDS_VERIFIED", f"exact preset value status changed: {item['id']}")
        lightning_presets = {item["id"] for item in presets if item["runtime"]["lightningEnabled"] >= 0.5}
        require(lightning_presets == {"Rain_Thunderstorm"}, f"lightning enabled outside storm preset: {sorted(lightning_presets)}")
        return "13 exact source presets with complete runtime state"

    checks.run("weather_preset_and_provenance", preset_completeness)

    def module_completeness() -> str:
        modules = loaded["package"]["modules"]
        found = {item["name"] for item in modules}
        require(found == REQUIRED_MODULES, f"module set differs: {sorted(REQUIRED_MODULES-found)}")
        require(all(item["status"] in PROVENANCE for item in modules), "module provenance invalid")
        tiers=loaded["package"]["qualityTiers"]
        require([tiers[name]["renderLongEdgeMax"] for name in ("Low", "Medium", "High")] == [1280, 1600, 1920], "render long-edge caps differ")
        return "13 independent environment modules"

    checks.run("module_architecture", module_completeness)

    def verified_runtime_usage() -> str:
        package = loaded["package"]
        active_curves = [item for item in package["celestial"]["curves"] if item.get("browserActive")]
        require(len(active_curves) == 1 and active_curves[0]["sourcePackage"].endswith("/CloudCoverage_RGB"), "active verified cloud curve differs")
        require(active_curves[0]["browserMappingStatus"] == "UDS_DERIVED_MAPPING", "cloud curve mapping provenance differs")
        core = (root / "js" / "environment-core.js").read_text(encoding="utf-8")
        renderer = (root / "js" / "renderer.js").read_text(encoding="utf-8")
        shader = (root / "shaders" / "sky.frag").read_text(encoding="utf-8")
        for token in ("starsIntensity", "starsSpeed", "twinkleAmount", "twinkleSpeed", "sunDiskIntensity", "moonScale", "sunColor", "moonColor"):
            require(token in core, f"verified celestial runtime binding absent: {token}")
        for token in ("uCloudProfile", "uStarsIntensity", "uStarsSpeed", "uTwinkleAmount", "uTwinkleSpeed", "uSunDiskIntensity", "uMoonScale"):
            require(token in renderer and token in shader, f"renderer/shader truth binding absent: {token}")
        for token in ("rainSpawnSource", "snowSpawnSource", "rainWindVelocity", "snowWindVelocity", "rainAlpha", "snowAlpha", "rainScale", "snowScale"):
            require(token in package["precipitation"], f"verified precipitation field absent: {token}")
            require(token in core, f"verified precipitation runtime binding absent: {token}")
        require(package["lightning"]["thunderBinding"]["status"] == "UNKNOWN", "unknown thunder binding was promoted")
        require(package["audio"]["automatic"]["value"] is False, "unknown automatic audio binding was enabled")
        require("AudioContext" not in core, "non-WAV procedural audio was added")
        return "verified celestial values and CloudCoverage_RGB active; unknown thunder audio remains disabled"

    checks.run("verified_runtime_usage", verified_runtime_usage)

    def resources() -> str:
        package = loaded["package"]
        for item in package["resources"]["audio"]:
            path = root / item["path"]
            require(path.is_file(), f"audio missing: {item['path']}")
            require(path.stat().st_size == item["size"], f"audio size differs: {item['id']}")
            require(sha256_file(path) == item["sha256"], f"audio hash differs: {item['id']}")
            require(item["payloadStatus"] == "UDS_VERIFIED" and item["bindingStatus"] == "UNKNOWN" and not item["automatic"], f"audio truth policy invalid: {item['id']}")
        require(package["resources"]["native"], "native fallbacks absent")
        require(all(item["status"] == "UNAVAILABLE" and not item["browserActive"] for item in package["resources"]["textures"]), "unavailable texture activated")
        browser_text = "\n".join((root / "js" / name).read_text(encoding="utf-8") for name in ("app.js", "renderer.js", "environment-core.js"))
        require("new Image" not in browser_text, "runtime attempts unavailable texture load")
        return f"{len(package['resources']['audio'])} verified WAV; procedural missing-asset fallback active"

    checks.run("resource_manifest_and_missing_asset_fallback", resources)

    def runtime_dependency() -> str:
        app = (root / "js" / "app.js").read_text(encoding="utf-8")
        require("solum_environment_package.json" in app, "compact package is not loaded")
        forbidden = ("UDS_VISUAL_EVIDENCE", "UDS_VISUAL_CONTRACT", "UDS_VISUAL_CAPABILITIES", "dependency_closure.json", "DemoMap.json")
        hits = [token for token in forbidden if token in app]
        require(not hits, f"runtime references P60/P61 reports: {hits}")
        require(loaded["report"]["runtimeInputFiles"] == ["data/solum_environment_package.json"], "build report runtime inputs differ")
        require(not loaded["package"]["sourceSummary"]["runtimeReadsP60P61"], "package claims P60/P61 runtime reads")
        return "HTML loads one compact package and local shaders/audio only"

    checks.run("no_p60_p61_runtime_dependency", runtime_dependency)

    def offline_no_leakage() -> str:
        forbidden_paths = ("/mnt/", "/data/data/", "/storage/", "/sdcard/", "/home/")
        forbidden_network = ("https://", "http://", "WebSocket(", "EventSource(", "cdn.")
        for path in generated_files(root):
            if path.suffix.lower() not in (".json", ".html", ".js", ".css", ".vert", ".frag", ".txt"):
                continue
            text = path.read_text(encoding="utf-8")
            path_hits = [token for token in forbidden_paths if token in text]
            network_hits = [token for token in forbidden_network if token in text]
            require(not path_hits, f"absolute path leak in {path.relative_to(root)}: {path_hits}")
            require(not network_hits, f"external network token in {path.relative_to(root)}: {network_hits}")
        return "offline; no CDN/network or absolute host paths"

    checks.run("offline_no_network_no_path_leakage", offline_no_leakage)

    def mobile_ui() -> str:
        css = (root / "css" / "app.css").read_text(encoding="utf-8")
        html = (root / "index.html").read_text(encoding="utf-8")
        require("max-height: 40dvh" in css and "height: 38dvh" in css, "panel 40% limit absent")
        for control in ("weather", "time", "quality", "clouds", "fog", "rain", "snow", "wind", "lightning", "wetness", "export-report"):
            require(f'id="{control}"' in html, f"mobile control absent: {control}")
        require("pointerdown" in (root / "js" / "renderer.js").read_text(encoding="utf-8"), "pointer input absent")
        return "collapsible <=40% panel, touch orbit/pan/pinch and requested controls"

    checks.run("mobile_ui_contract", mobile_ui)

    def javascript() -> str:
        node = shutil.which("node")
        require(node is not None, "node unavailable")
        paths = sorted((root / "js").glob("*.js"))
        for path in paths:
            result = subprocess.run([node, "--check", str(path)], capture_output=True, text=True, timeout=30)
            require(result.returncode == 0, f"{path.name}: {result.stderr.strip()}")
        runtime_test = subprocess.run(
            [node, "--experimental-default-type=module", str(REPO_ROOT / "tools" / "uds_visual" / "tests" / "test_environment_runtime.mjs"), str(package_path)],
            capture_output=True, text=True, timeout=60,
        )
        require(runtime_test.returncode == 0, runtime_test.stderr.strip() or runtime_test.stdout.strip())
        return f"node syntax {len(paths)} modules; {runtime_test.stdout.strip()}"

    checks.run("javascript_and_environment_logic", javascript)

    def shaders() -> str:
        compiler = shutil.which("glslangValidator")
        require(compiler is not None, "glslangValidator unavailable")
        paths = sorted((root / "shaders").glob("*.vert")) + sorted((root / "shaders").glob("*.frag"))
        require(paths, "no shaders found")
        for path in paths:
            result = subprocess.run([compiler, str(path)], capture_output=True, text=True, timeout=30)
            require(result.returncode == 0, f"{path.name}: {result.stderr.strip() or result.stdout.strip()}")
        return f"glslang compiled {len(paths)} shaders"

    checks.run("shader_compilation", shaders)

    def scenarios() -> str:
        items = loaded["package"]["scenarios"]
        required = {"noon", "sunset", "night", "cloudy", "rain", "snow", "storm", "clear_to_rain", "day_to_night"}
        require({item["id"] for item in items} == required, "scenario set differs")
        for item in items:
            require(item.get("camera") and item.get("finalRuntime") and item.get("expectedVisualSigns"), f"scenario incomplete: {item['id']}")
        return "9 fixed visual scenarios"

    checks.run("visual_scenarios", scenarios)

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
            for relative in ("/", "/data/solum_environment_package.json", "/shaders/sky.frag", "/js/environment-core.js"):
                with urllib.request.urlopen(base + relative, timeout=5) as response:
                    body = response.read()
                    require(response.status == 200 and body, f"server smoke failed: {relative}")
        finally:
            server.shutdown(); server.server_close(); thread.join(timeout=3)
        return "index, compact package, shader and environment module served"

    checks.run("local_server_smoke", local_server)

    def deterministic() -> str:
        if not args.determinism:
            return "not requested"
        require(args.p60 and args.p61, "--p60 and --p61 required")
        with tempfile.TemporaryDirectory(prefix="solum-p62b-rebuild-") as directory:
            target = Path(directory) / "preview"
            command = [
                sys.executable, str(Path(__file__).with_name("build_solum_environment.py")),
                "--p60", args.p60, "--p61", args.p61, "--output", str(target),
            ]
            result = subprocess.run(command, capture_output=True, text=True, timeout=900)
            require(result.returncode == 0, result.stderr.strip()[-3000:])
            excluded = {"reports/SOLUM_ENVIRONMENT_VALIDATION.json"}
            original = {path.relative_to(root).as_posix(): sha256_file(path) for path in generated_files(root) if path.relative_to(root).as_posix() not in excluded}
            rebuilt = {path.relative_to(target).as_posix(): sha256_file(path) for path in generated_files(target) if path.relative_to(target).as_posix() not in excluded}
            changed = sorted(key for key in original.keys() & rebuilt.keys() if original[key] != rebuilt[key])
            require(original == rebuilt, f"determinism differs: changed={changed}, original-only={sorted(original.keys()-rebuilt.keys())}, rebuilt-only={sorted(rebuilt.keys()-original.keys())}")
        return f"{len(original)} byte-identical files"

    checks.run("deterministic_rebuild", deterministic)

    validation = {
        "schema": "solum.environment.validation",
        "schemaVersion": 1,
        "status": "PASS" if checks.passed else "FAIL",
        "checks": checks.items,
        "visualParityClaim": False,
        "runtimeP60P61Dependency": False,
        "filamentGate": "NOT_RUN",
        "androidGate": "NOT_RUN",
    }
    (root / "reports").mkdir(exist_ok=True)
    write_json(root / "reports" / "SOLUM_ENVIRONMENT_VALIDATION.json", validation)
    return validation, checks.passed


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"))
    result.add_argument("--p60")
    result.add_argument("--p61")
    result.add_argument("--determinism", action="store_true")
    return result


def main() -> int:
    args = parser().parse_args()
    report, passed = validate(args)
    print(json.dumps({
        "status": report["status"],
        "passed": sum(item["status"] == "PASS" for item in report["checks"]),
        "failed": sum(item["status"] == "FAIL" for item in report["checks"]),
    }, ensure_ascii=False, sort_keys=True))
    return 0 if passed else 2


if __name__ == "__main__":
    raise SystemExit(main())
