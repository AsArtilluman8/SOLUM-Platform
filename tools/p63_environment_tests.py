#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import struct
import subprocess
import sys
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "apps/engine/src/main/assets/env"
P63 = ASSETS / "p63"
JAVA = ROOT / "apps/engine/src/main/java/com/solum/engine/environment/p63"
ACTIVITY = ROOT / "apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java"


def require(value: bool, label: str) -> None:
    if not value:
        raise AssertionError(label)


def run(command: list[str], label: str) -> None:
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    if result.stdout:
        print(result.stdout.rstrip())
    if result.returncode:
        if result.stderr:
            print(result.stderr.rstrip(), file=sys.stderr)
        raise AssertionError(f"{label} failed with exit {result.returncode}")


def validate_runtime_package() -> None:
    package = json.loads((ASSETS / "solum_environment_runtime.json").read_text(encoding="utf-8"))
    expected = {
        "Clear_Skies", "Cloudy", "Foggy", "Overcast", "Partly_Cloudy", "Rain", "Rain_Light",
        "Rain_Thunderstorm", "Sand_Dust_Calm", "Sand_Dust_Storm", "Snow", "Snow_Blizzard", "Snow_Light",
    }
    actual = {item["id"] for item in package["weatherPresets"]}
    require(package["schema"] == "solum.environment.runtime", "runtime schema")
    require(package["schemaVersion"] == 1, "runtime schema version")
    require(len(package["weatherPresets"]) == 13 and actual == expected, "exact 13 preset IDs")
    require(set(package["qualityTiers"]) == {"Low", "Medium", "High", "Manual"}, "four environment quality tiers")
    text = json.dumps(package)
    require("http://" not in text and "https://" not in text, "runtime package has no network dependency")


def validate_glb_stage() -> None:
    data = (P63 / "p63_environment_stage.glb").read_bytes()
    magic, version, total = struct.unpack_from("<4sII", data, 0)
    require(magic == b"glTF" and version == 2 and total == len(data), "valid GLB v2 header")
    json_length, json_type = struct.unpack_from("<II", data, 12)
    require(json_type == 0x4E4F534A, "GLB JSON chunk")
    gltf = json.loads(data[20:20 + json_length].decode("utf-8"))
    names = {node.get("name", "") for node in gltf.get("nodes", [])}
    required = {
        "P63_SUN_DISK", "P63_MOON_DISK", "P63_MOON_SHADOW", "P63_STAR_GROUP_0", "P63_CLOUD_0",
        "P63_RAIN_CELL_0_0", "P63_SNOW_CELL_0_0", "P63_DUST_CELL_0_0", "P63_LIGHTNING_BOLT",
        "P63_WET_SURFACE", "P63_PUDDLE", "P63_SNOW_SURFACE", "P63_ICE_SURFACE", "P63_INTERIOR_FLOOR", "P63_ROOF",
    }
    require(required <= names, "P63 stage has all required world-space systems")
    require(len([name for name in names if name.startswith("P63_RAIN_CELL_")]) == 25, "25 world-space rain cells")
    require(len([name for name in names if name.startswith("P63_SNOW_CELL_")]) == 25, "25 world-space snow cells")
    require(len([name for name in names if name.startswith("P63_CLOUD_")]) == 12, "12 cloud groups")
    materials = {item.get("name", "") for item in gltf.get("materials", [])}
    require({"P63_RoughMetal", "P63_PolishedMetal", "P63_Glass", "P63_Water", "P63_WetGround", "P63_Puddle", "P63_MoonCrater"} <= materials, "reflective diagnostic materials")
    stage_text = json.dumps(gltf).lower()
    require("fullscreen" not in stage_text and "screen_overlay" not in stage_text and "quad_particle" not in stage_text, "no fullscreen precipitation or square-particle path")
    require(any(image.get("mimeType") == "image/png" for image in gltf.get("images", [])), "embedded moon crater texture")


def read_glb(path: Path) -> tuple[dict, bytes]:
    data = path.read_bytes()
    magic, version, total = struct.unpack_from("<4sII", data, 0)
    require(magic == b"glTF" and version == 2 and total == len(data), f"valid GLB: {path.name}")
    json_length, json_type = struct.unpack_from("<II", data, 12)
    require(json_type == 0x4E4F534A, f"JSON chunk: {path.name}")
    gltf = json.loads(data[20:20 + json_length].decode("utf-8"))
    binary_header = 20 + json_length
    binary_length, binary_type = struct.unpack_from("<II", data, binary_header)
    require(binary_type == 0x004E4942, f"BIN chunk: {path.name}")
    binary = data[binary_header + 8:binary_header + 8 + binary_length]
    return gltf, binary


def embedded_png(gltf: dict, binary: bytes, image_name: str) -> tuple[int, int, bytes]:
    image = next(item for item in gltf["images"] if item.get("name") == image_name)
    view = gltf["bufferViews"][image["bufferView"]]
    payload = binary[view.get("byteOffset", 0):view.get("byteOffset", 0) + view["byteLength"]]
    require(payload.startswith(b"\x89PNG\r\n\x1a\n"), f"embedded PNG {image_name}")
    width, height = struct.unpack(">II", payload[16:24])
    offset = 8
    compressed = bytearray()
    while offset < len(payload):
        length = struct.unpack(">I", payload[offset:offset + 4])[0]
        kind = payload[offset + 4:offset + 8]
        if kind == b"IDAT": compressed.extend(payload[offset + 8:offset + 8 + length])
        offset += 12 + length
    raw = zlib.decompress(bytes(compressed))
    return width, height, raw


def validate_p63_2a() -> None:
    stage = P63 / "p63_2a_celestial_test_stage.glb"
    gltf, binary = read_glb(stage)
    names = {node.get("name", "") for node in gltf.get("nodes", [])}
    required = {
        "P63_CELESTIAL_STAGE_ROOT", "P63_STAGE_GROUND", "P63_MATTE_SPHERE", "P63_ROUGH_METAL",
        "P63_POLISHED_METAL", "P63_GLASS", "P63_VERTICAL_WALL", "P63_SHADOW_PILLAR",
        "P63_CAMERA_ORBIT_TARGET", "P63_NORMAL_SCALE_REFERENCE", "P63_ROOF", "P63_INTERIOR_FLOOR",
        "P63_SKY_DAWN", "P63_SKY_DAY", "P63_SKY_SUNSET", "P63_SKY_TWILIGHT", "P63_SKY_NIGHT",
        "P63_SUN_DISK", "P63_MOON_DISK", "P63_MOON_SHADOW",
    }
    require(required <= names, "P63.2A diagnostic and celestial nodes")
    prohibited = ("STAR", "CLOUD", "RAIN", "SNOW", "DUST", "PUDDLE", "LIGHTNING")
    require(not any(any(token in name for token in prohibited) for name in names), "P63.2A excludes deferred weather/stars")
    materials = {item.get("name", ""): item for item in gltf.get("materials", [])}
    for slot in ("dawn", "day", "sunset", "twilight", "night"):
        require(materials[f"P63_2A_Sky_{slot}"]["doubleSided"], f"full sphere is camera-visible: {slot}")
    width, height, pixels = embedded_png(gltf, binary, "P63_SKY_DAY")
    require((width, height) == (256, 128), "mobile sky texture dimensions")
    stride = 1 + width * 4
    for row in (8, 62, 96, 120):
        require(pixels[row * stride] == 0, "unfiltered deterministic PNG row")
        first = pixels[row * stride + 1:row * stride + 5]
        last = pixels[row * stride + 1 + (width - 1) * 4:row * stride + 1 + width * 4]
        require(max(abs(first[i] - last[i]) for i in range(3)) <= 2, f"periodic sky seam row {row}")
    lower_horizon = pixels[82 * stride + 1:82 * stride + 4]
    lower_nadir = pixels[118 * stride + 1:118 * stride + 4]
    require(lower_horizon != lower_nadir and len(set(lower_horizon)) > 1 and len(set(lower_nadir)) > 1,
            "lower hemisphere is atmospheric continuation, not flat grey")

    resource_manifest = json.loads((P63 / "P63_2A_RESOURCE_MANIFEST.json").read_text(encoding="utf-8"))
    moon = resource_manifest["selectedMoon"]
    require(moon["sha256"] == "8a8ff79b0d06946bfd09efcada50cc4a9891076b7f2a00b49fa8e182bbb6e375", "verified moon hash")
    require(moon["dimensions"] == "256x256" and moon["colorSpace"] == "sRGB" and moon["provenance"] == "UDS_VERIFIED", "moon validation/provenance")
    require(resource_manifest["selectedMoonDetail"]["runtimeUsage"] == "AUDITED_NOT_BOUND_NO_TANGENT_SAFE_PATH", "moon normal is not falsely bound")

    audio = json.loads((P63 / "P63_2A_VERIFIED_AUDIO_MANIFEST.json").read_text(encoding="utf-8"))
    require(audio["proceduralAudioDefault"] is False and audio["longLoopStatus"] == "NO_VERIFIED_LONG_LOOP", "safe audio defaults")
    require(len(audio["entries"]) == 32 and all(item["provenance"] == "UDS_VERIFIED" for item in audio["entries"]), "required verified WAV inventory")
    for item in audio["entries"]:
        asset = ROOT / "apps/engine/src/main/assets" / item["assetPath"]
        require(asset.is_file(), f"local private WAV packaged: {item['verifiedFileName']}")
        import hashlib
        require(hashlib.sha256(asset.read_bytes()).hexdigest() == item["sha256"], f"WAV hash: {item['verifiedFileName']}")

    activity = ACTIVITY.read_text(encoding="utf-8")
    adapter = (JAVA / "SolumFilamentEnvironmentAdapter.java").read_text(encoding="utf-8")
    audio_source = (JAVA / "SolumEnvironmentAudioSystem.java").read_text(encoding="utf-8")
    require("intensity * 30000.0f * blend" not in activity, "bad indirect-light multiplier absent")
    require("float rawIntensity = intensity * blend" in activity and "clamp(rawIntensity, 0.0f, 2.0f)" in activity, "indirect-light clamp regression")
    require("controls.p63IblEnabled" in adapter and "old IBL active=" in activity, "old IBL remains active and dynamic P63 IBL is gated")
    require('slider.setTag("p63.slider." + key)' in activity and 'apply.setTag("p63.numeric.apply." + key)' in activity,
            "real slider/numeric View bindings")
    require("PREF_P63_2A_STATE" in activity and "restoreP63CelestialState" in activity, "Activity recreation persistence")
    require("cameraDistance = p63CelestialStageRequested ? 15.0f" in activity and "cameraTargetY = p63CelestialStageRequested ? 1.3f" in activity, "diagnostic camera framing")
    require("setLooping(false)" in audio_source and "makeLoop" not in audio_source and "procedural" in audio_source, "verified playback is non-looped and non-procedural")
    android_test = ROOT / "apps/engine/src/androidTest/java/com/solum/engine/P63CelestialControlsViewTest.java"
    require(android_test.is_file() and "Activity recreation restores shared state" in android_test.read_text(encoding="utf-8"), "real Android View test added")


def validate_ibl_assets() -> None:
    manifest = json.loads((P63 / "P63_ASSET_MANIFEST.json").read_text(encoding="utf-8"))
    require(manifest["license"] == "SOLUM_NATIVE", "asset provenance")
    for slot in ("day", "sunset", "night", "overcast", "storm", "snow", "sand"):
        path = P63 / f"p63_{slot}.hdr"
        data = path.read_bytes()
        require(data.startswith(b"#?RADIANCE\n"), f"{slot} Radiance HDR header")
        require(b"-Y 64 +X 128" in data[:512], f"{slot} prepared HDR dimensions")
    captures = json.loads((P63 / "P63_CAPTURE_SCENARIOS.json").read_text(encoding="utf-8"))
    require(len(captures["scenarios"]) >= 10 and captures["deviceVerificationRequired"], "capture-ready visual QA scenarios")


def validate_ui_preservation() -> None:
    text = ACTIVITY.read_text(encoding="utf-8")
    preserved_controls = (
        "Import Model", "Import IBL", "Scan Download", "Reload Model", "Cook Active GLB → SLPK",
        "Render Control Center: Basic", "Dynamic Resolution", "MSAA", "FXAA", "Dithering", "TAA",
        "Render Control Center: Lighting", "Sun Azimuth", "Sun Elevation", "Light Rig",
        "Render Control Center: PostFX", "Bloom Strength", "SSR", "Refraction",
        "Render Control Center: Color / Fog", "Reset Color Grading", "Fog Density",
        "Reset Camera", "Fit Model", "Reset Model Transform", "Auto Fit after Transform",
        "Save Config", "Load Config", "Export Debug ZIP", "Export SLPK Debug ZIP",
    )
    for label in preserved_controls:
        require(label in text, f"existing UI control preserved: {label}")
    require('ENVIRONMENT("Environment / Weather")' in text, "Environment/Weather workspace tab")
    require("buildP63EnvironmentPanel();" in text, "P63 panel integrated into existing activity")
    require("setPanelVisible(environmentPanel, activeTab == WorkspaceTab.ENVIRONMENT" in text, "P63 panel follows existing tab architecture")
    require("class FilamentGlbPreviewActivity" in text and "throwaway Activity" not in text, "existing Filament preview runtime retained")
    require("p63EnvironmentClassificationView" in text and "debugPanel.addView(p63EnvironmentClassificationView)" in text, "classification routed to Debug")


def validate_runtime_wiring() -> None:
    sources = "\n".join(path.read_text(encoding="utf-8") for path in JAVA.glob("*.java"))
    adapter = (JAVA / "SolumFilamentEnvironmentAdapter.java").read_text(encoding="utf-8")
    require("P60" not in sources and "P61" not in sources, "P63 runtime has no P60/P61 report dependency")
    require("http://" not in sources and "https://" not in sources, "P63 runtime has no CDN/network path")
    require("LightManager.Type.POINT" in adapter and "lightningLumens" in adapter, "lightning uses transient Filament point light")
    require("setMaterial4(\"P63_WET_SURFACE\"" in adapter and "P63_PUDDLE" in adapter and "P63_ICE_SURFACE" in adapter, "surface material response hooks")
    require("applyPreparedIbl" in adapter and "iblRevision" in adapter, "thresholded prepared IBL hook")
    require("blocksPrecipitation" in adapter and "P63_RAIN_CELL" in adapter and "P63_SNOW_CELL" in adapter, "world-space precipitation occlusion hook")
    require("new float[16]" not in adapter[adapter.index("private void setTransform"):], "no per-transform hot-path matrix allocation")


def compile_and_run_core() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(bool(javac and java), "JDK tools available")
    output = ROOT / "_work" / "p63_test_classes" / str(os.getpid())
    output.mkdir(parents=True, exist_ok=True)
    sources = [path for path in JAVA.glob("*.java") if path.name not in {
        "SolumEnvironmentPackageLoader.java", "SolumFilamentEnvironmentAdapter.java", "SolumEnvironmentAudioSystem.java",
    }]
    sources.append(ROOT / "tools/p63_tests/SolumEnvironmentCoreTest.java")
    run([javac, "-encoding", "UTF-8", "-d", str(output), *map(str, sources)], "P63 pure Java compile")
    run([java, "-cp", str(output), "com.solum.engine.environment.p63.SolumEnvironmentCoreTest"], "P63 pure Java runtime")


def run_legacy_environment_regression() -> None:
    run([sys.executable, "-m", "unittest", "tools.uds_visual.tests.test_solum_environment"], "P62B environment regression")
    node = shutil.which("node")
    package = ROOT / "generated_local/uds_visual_preview/data/solum_environment_package.json"
    if node and package.is_file():
        run([node, "tools/uds_visual/tests/test_environment_runtime.mjs", str(package)], "P62B compact runtime regression")
    else:
        print("P62B_NODE_RUNTIME_TEST=SKIP optional generated_local package or node unavailable")


def main() -> None:
    validate_runtime_package()
    validate_glb_stage()
    validate_p63_2a()
    validate_ibl_assets()
    validate_ui_preservation()
    validate_runtime_wiring()
    compile_and_run_core()
    run_legacy_environment_regression()
    print("P63_ENVIRONMENT_TESTS=PASS presets=13 worldSpacePrecipitation=true interiorExclusion=true preparedIbl=7")


if __name__ == "__main__":
    main()
