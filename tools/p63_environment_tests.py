#!/usr/bin/env python3
from __future__ import annotations

import json
import hashlib
import importlib.util
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


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


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


def embedded_payload(gltf: dict, binary: bytes, image_name: str) -> bytes:
    image = next(item for item in gltf["images"] if item.get("name") == image_name)
    view = gltf["bufferViews"][image["bufferView"]]
    start = view.get("byteOffset", 0)
    return binary[start:start + view["byteLength"]]


def accessor_values(gltf: dict, binary: bytes, accessor_index: int) -> list[tuple[float, ...]]:
    accessor = gltf["accessors"][accessor_index]
    view = gltf["bufferViews"][accessor["bufferView"]]
    components = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[accessor["type"]]
    formats = {5123: "H", 5125: "I", 5126: "f"}
    fmt = formats[accessor["componentType"]]
    size = struct.calcsize("<" + fmt) * components
    stride = view.get("byteStride", size)
    start = view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    return [struct.unpack_from("<" + fmt * components, binary, start + index * stride)
            for index in range(accessor["count"])]


def validate_node_winding(gltf: dict, binary: bytes, node_name: str) -> None:
    node = next(item for item in gltf["nodes"] if item.get("name") == node_name)
    primitive = gltf["meshes"][node["mesh"]]["primitives"][0]
    positions = accessor_values(gltf, binary, primitive["attributes"]["POSITION"])
    normals = accessor_values(gltf, binary, primitive["attributes"]["NORMAL"])
    indices = [int(item[0]) for item in accessor_values(gltf, binary, primitive["indices"])]
    checked = 0
    for offset in range(0, len(indices), 3):
        ia, ib, ic = indices[offset:offset + 3]
        a, b, c = positions[ia], positions[ib], positions[ic]
        ab = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
        ac = (c[0] - a[0], c[1] - a[1], c[2] - a[2])
        cross = (ab[1] * ac[2] - ab[2] * ac[1], ab[2] * ac[0] - ab[0] * ac[2], ab[0] * ac[1] - ab[1] * ac[0])
        magnitude = sum(value * value for value in cross)
        if magnitude < 1e-10:
            continue
        normal = tuple((normals[ia][axis] + normals[ib][axis] + normals[ic][axis]) / 3.0 for axis in range(3))
        require(sum(cross[axis] * normal[axis] for axis in range(3)) > 0.0, f"winding matches normals: {node_name}")
        checked += 1
    require(checked > 0, f"non-degenerate triangles checked: {node_name}")


def validate_p63_2a() -> None:
    stage = P63 / "p63_2a_celestial_test_stage.glb"
    gltf, binary = read_glb(stage)
    names = {node.get("name", "") for node in gltf.get("nodes", [])}
    required = {
        "P63_CELESTIAL_STAGE_ROOT", "P63_STAGE_GROUND", "P63_MATTE_SPHERE", "P63_ROUGH_METAL",
        "P63_POLISHED_METAL", "P63_GLASS", "P63_VERTICAL_WALL", "P63_SHADOW_PILLAR",
        "P63_CAMERA_ORBIT_TARGET", "P63_NORMAL_SCALE_REFERENCE", "P63_ROOF", "P63_INTERIOR_FLOOR",
        "P63_SUN_HALO_OUTER", "P63_SUN_HALO", "P63_SUN_GLOW_INNER", "P63_SUN_DISK",
        "P63_MOON_HALO", "P63_MOON_GLOW_INNER", "P63_MOON_PHASE_00", "P63_MOON_PHASE_64",
        "P63_STAR_S0_G0", "P63_STAR_S4_G7", "P63_CLOUD_0", "P63_CLOUD_11",
    }
    require(required <= names, "P63.2B diagnostic and celestial nodes")
    phase_nodes = sorted(name for name in names if name.startswith("P63_MOON_PHASE_"))
    require(len(phase_nodes) == 65, "65 deterministic single-disc analytic moon phases")
    require("P63_MOON_SHADOW" not in names and "P63_MOON_DISK" not in names,
            "celestial stage has no secondary black occluder or legacy moon disc")
    prohibited = ("RAIN", "SNOW", "DUST", "PUDDLE", "LIGHTNING")
    require(not any(any(token in name for token in prohibited) for name in names), "P63.2B excludes precipitation and lightning")
    require(len([name for name in names if name.startswith("P63_STAR_S")]) == 40,
            "five non-square star size levels across eight density groups")
    require(len([name for name in names if name.startswith("P63_CLOUD_")]) == 12,
            "twelve world-space cloud layers")
    materials = {item.get("name", ""): item for item in gltf.get("materials", [])}
    require(not any(name.startswith("P63_SKY_") for name in names), "five pre-baked GLB skies removed")
    require(not any(name.startswith("P63_2A_Sky_") for name in materials), "no final two-color/pre-baked sky material")
    require("P63_2A_MoonPhaseMask" not in materials, "legacy black moon phase material removed")
    phase_materials = [materials[f"P63_2A_MoonPhase_{index:02d}"] for index in range(65)]
    require(all(not material.get("doubleSided", False) for material in phase_materials), "moon phase discs have canonical front side")
    require(all("normalTexture" not in material for material in phase_materials), "moon normal remains unbound without tangent path")
    require("emissiveTexture" in materials["P63_2A_SunDisk"] and all("emissiveTexture" in material for material in phase_materials),
            "sun and moon emissive is texture-masked and preserves disc/phase falloff")
    for node_name in ("P63_STAGE_GROUND", "P63_INTERIOR_FLOOR", "P63_MATTE_SPHERE", "P63_ROUGH_METAL",
                      "P63_POLISHED_METAL", "P63_GLASS", "P63_VERTICAL_WALL", "P63_SHADOW_PILLAR", "P63_ROOF"):
        validate_node_winding(gltf, binary, node_name)

    resource_manifest = json.loads((P63 / "P63_2A_RESOURCE_MANIFEST.json").read_text(encoding="utf-8"))
    moon = resource_manifest["selectedMoon"]
    require(moon["sha256"] == "8a8ff79b0d06946bfd09efcada50cc4a9891076b7f2a00b49fa8e182bbb6e375", "verified moon hash")
    require(moon["dimensions"] == "256x256" and moon["colorSpace"] == "sRGB" and moon["provenance"] == "UDS_VERIFIED", "moon validation/provenance")
    require(resource_manifest["selectedMoonDetail"]["runtimeUsage"] == "AUDITED_NOT_BOUND_NO_TANGENT_SAFE_PATH", "moon normal is not falsely bound")
    private_stage = ROOT / "apps/engine/src/main/assets/private_premium/p63_2a/celestial/p63_2a_celestial_test_stage_uds.glb"
    if private_stage.is_file():
        private_gltf, private_binary = read_glb(private_stage)
        payload = embedded_payload(private_gltf, private_binary, "P63_UDS_VERIFIED_MOON_COLOR")
        import hashlib
        require(hashlib.sha256(payload).hexdigest() == moon["sha256"], "exact UDS moon payload embedded")
        private_names = {item.get("name", "") for item in private_gltf["nodes"]}
        require("P63_MOON_SHADOW" not in private_names and len([name for name in private_names if name.startswith("P63_MOON_PHASE_")]) == 65,
                "UDS celestial stage uses phase derivatives with no occluder")
        private_materials = {item.get("name", ""): item for item in private_gltf["materials"]}
        moon_material = private_materials["P63_2A_MoonPhase_32"]
        texture_index = moon_material["pbrMetallicRoughness"]["baseColorTexture"]["index"]
        sampler = private_gltf["samplers"][private_gltf["textures"][texture_index]["sampler"]]
        require(sampler["magFilter"] == 9729 and sampler["minFilter"] == 9987, "moon uses linear+mipmap filtering, never nearest")
        require(moon_material.get("alphaMode") == "BLEND", "moon texture uses smooth alpha limb")
        spec = importlib.util.spec_from_file_location("p63_phase_test_generator", ROOT / "tools/generate_p63_environment_assets.py")
        require(spec is not None and spec.loader is not None, "phase generator import")
        module = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = module
        spec.loader.exec_module(module)
        source = module.decode_png_rgba(payload)
        phase_payload = embedded_payload(private_gltf, private_binary, "P63_MOON_ANALYTIC_PHASE_16")
        width, height, phase_pixels = module.decode_png_rgba(phase_payload)
        require((width, height) == source[:2] == (256, 256), "phase derivative preserves exact moon dimensions")
        source_pixels = source[2]
        def light_ratio(nx: float, ny: float) -> float:
            x = min(width - 1, max(0, round((nx + 1.0) * 0.5 * width - 0.5)))
            y = min(height - 1, max(0, round((1.0 - ny) * 0.5 * height - 0.5)))
            offset = (y * width + x) * 4
            return sum(phase_pixels[offset:offset + 3]) / max(1, sum(source_pixels[offset:offset + 3]))
        require(light_ratio(0.42, 0.65) > light_ratio(0.42, 0.0) + 0.10,
                "physically shaded crescent terminator is spherical and curved, not a knife-cut disk")

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
    coordinates = (JAVA / "SolumCelestialCoordinateSystem.java").read_text(encoding="utf-8")
    analytic_sky = (JAVA / "SolumAnalyticSky.java").read_text(encoding="utf-8")
    audio_source = (JAVA / "SolumEnvironmentAudioSystem.java").read_text(encoding="utf-8")
    require("intensity * 30000.0f * blend" not in activity, "bad indirect-light multiplier absent")
    require("float rawIntensity = intensity * blend" in activity and "clamp(rawIntensity, 0.0f, 2.0f)" in activity, "indirect-light clamp regression")
    require("controls.p63IblEnabled" in adapter and "old IBL active=" in activity, "old IBL remains active and dynamic P63 IBL is gated")
    require('slider.setTag("p63.slider." + key)' in activity and 'apply.setTag("p63.numeric.apply." + key)' in activity,
            "real slider/numeric View bindings")
    require("PREF_P63_2A_STATE" in activity and "restoreP63CelestialState" in activity, "Activity recreation persistence")
    gesture = (JAVA / "SolumCameraGestureState.java").read_text(encoding="utf-8")
    picker = (ROOT / "apps/engine/src/main/java/com/solum/engine/P63HsvColorPickerDialog.java").read_text(encoding="utf-8")
    controls = (JAVA / "SolumCelestialControlState.java").read_text(encoding="utf-8")
    require('applyP63CameraPreset("Overview")' in activity and "DEFAULT_CAMERA_ZOOM_SENSITIVITY = 0.021f" in controls,
            "diagnostic camera framing and 1.5x zoom")
    require("P63_CAMERA_MIN_DISTANCE = 2.0f" in activity and "P63_CAMERA_MAX_DISTANCE = 45.0f" in activity
            and "applyP63CameraBoundsAtGestureEnd()" in activity, "camera gesture distance and ground-plane bounds")
    require("GestureDetector" not in activity and "TWO_FINGER_PENDING" in gesture and "changesDistance()" in gesture,
            "single gesture owner separates orbit pan and pinch")
    update_loop = activity[activity.index("private void updateP63Environment"):activity.index("private void applyP63PreparedIbl")]
    require("applyP63CameraBoundsAtGestureEnd" not in update_loop, "camera manipulator is never replaced mid-gesture/per-frame")
    require("positionRelativeToCamera" in coordinates and "cameraX + bodyDirection[0] * safeRadius" in coordinates, "camera-relative celestial disks")
    require("consistentBodyAndLightDirection" in coordinates and "sunVisualDirection" in adapter, "single canonical visual/light direction")
    require("SAMPLER_CUBEMAP" in adapter and "SRGB8_A8" in adapter and "linearColor" in analytic_sky, "camera-inside analytic full sky cubemap")
    require("P63_SUN_HALO" in adapter and "P63_MOON_HALO" in adapter and "PROTOTYPE_HOOK_ONLY" in activity, "safe halo/bloom and shafts hook")
    celestial_geometry = adapter[adapter.index("private void applyCelestialGeometry"):adapter.index("private void applyCloudGeometry")]
    celestial_only = celestial_geometry[:celestial_geometry.index("if (controller.isCelestialOnlyMode())") + 1200]
    require("MOON_PHASE_NAMES" in celestial_only and "setLayerVisible" in celestial_only,
            "celestial moon renders one selected phase layer")
    generator_source = (ROOT / "tools/generate_p63_environment_assets.py").read_text(encoding="utf-8")
    phase_function = generator_source[generator_source.index("def moon_phase_png"):generator_source.index("def sun_disc_png")]
    require(not any(token in phase_function.lower() for token in ("bayer", "checker", "x %", "y %", "random.")),
            "no dither/stipple moon phase branch")
    for section in ("quick", "atmosphere", "sun", "moon", "stars", "clouds", "camera", "postfx", "debug"):
        require(f'{{"{section}",' in activity and f'"p63.tab.content." + key' in activity, f"Environment tab: {section}")
    require("P63HsvColorPickerDialog.show" in activity and "HueSaturationWheelView" in picker and "ValueBarView" in picker,
            "Sun/Moon/Stars/Clouds circular HSV picker with value control")
    require("sunEmissive" in controls and "moonEmissive" in controls and '"emissiveFactor"' in adapter,
            "sun and moon emissive remain separate from directional lux")
    require("applyStarGeometry" in adapter and "starTwinkleAmount" in controls and "starFade" in (JAVA / "SolumEnvironmentController.java").read_text(encoding="utf-8"),
            "world-space stars fade through dawn with bounded twinkle")
    require("applyCloudPreset" in controls and "updateCelestialClouds" in (JAVA / "SolumEnvironmentController.java").read_text(encoding="utf-8")
            and "P63_2B_CloudLayer" in generator_source, "layered cloud controls and presets are renderer-backed")
    require("setLooping(false)" in audio_source and "makeLoop" not in audio_source and "procedural" in audio_source, "verified playback is non-looped and non-procedural")
    android_test = ROOT / "apps/engine/src/androidTest/java/com/solum/engine/P63CelestialControlsViewTest.java"
    require(android_test.is_file() and "Activity recreation restores shared state" in android_test.read_text(encoding="utf-8")
            and "tabs switch correctly" in android_test.read_text(encoding="utf-8"), "real Android View tab/color test added")


def validate_p63_3() -> None:
    material_path = ROOT / "apps/engine/src/main/materials/p63_3_analytic_sky.mat"
    package_path = P63 / "analytic_sky_mobile.filamat"
    renderer_path = JAVA / "SolumAnalyticSkyRenderer.java"
    resources_path = JAVA / "SolumAnalyticSkyResources.java"
    state_path = JAVA / "SolumAnalyticSkyState.java"
    controls_path = JAVA / "SolumCelestialControlState.java"
    controller_path = JAVA / "SolumEnvironmentController.java"
    adapter_path = JAVA / "SolumFilamentEnvironmentAdapter.java"
    for path in (material_path, package_path, renderer_path, resources_path, state_path, controls_path):
        require(path.is_file() and path.stat().st_size > 0, f"P63.3 runtime file: {path.name}")

    material = material_path.read_text(encoding="utf-8")
    renderer = renderer_path.read_text(encoding="utf-8")
    resources = resources_path.read_text(encoding="utf-8")
    state = state_path.read_text(encoding="utf-8")
    controls = controls_path.read_text(encoding="utf-8")
    controller = controller_path.read_text(encoding="utf-8")
    adapter = adapter_path.read_text(encoding="utf-8")
    activity = ACTIVITY.read_text(encoding="utf-8")

    require("vertexDomain : device" in material and "getWorldFromClipMatrix()" in material,
            "device-domain full-screen analytic sky")
    require("analyticAtmosphere" in material and "airMass" in material and "rayleighPhase" in material
            and "hgPhase" in material and "ozoneScale" in material, "Rayleigh/Mie/ozone/optical-air-mass material")
    require("analyticSun" in material and "sunDiscLuminanceNits" in controls and "sunLightLux" in controls,
            "analytic Sun visual luminance is separate from direct lux")
    require("limb" in material and "fwidth(distance)" in material and "sunHaloFalloff" in controls,
            "Sun limb AA and finite halo controls")
    require("safeVisualLuminance" in material and "log2(1.0 + max(0.0, nits))" in material
            and "vec3(64.0)" in material, "finite HDR display mapping prevents white-frame overflow")
    material_helpers = (JAVA / "SolumAnalyticSkyMaterial.java").read_text(encoding="utf-8")
    require("SUN_LUMINANCE_SAFETY_MAX_NITS = 1_000_000.0f" in material_helpers
            and "Float.isNaN(value) || Float.isInfinite(value)" in controls, "large finite Sun ceiling")

    require("moonToSunDirection" in state and "moonToSunDirection" in controller,
            "continuous Moon direction is state/uniform driven")
    require("normalPerpendicular" in material and "smoothstep(-terminatorWidth, terminatorWidth, nDotL)" in material
            and "moonEarthshine" in controls, "spherical Moon normal, smooth terminator and earthshine")
    require("materialParams_moonAlbedo" in material and "materialParams_moonNormal" in material,
            "verified Moon albedo and normal are sampled")
    require("analytic_continuous_uniform" in adapter and "hideLegacyCelestialGeometry" in adapter,
            "analytic Moon has no phase-node recreation or secondary occluder")

    require("analyticStars" in material and "proceduralStar" in material and "fwidth(direction.x)" in material,
            "radial subpixel stars with deterministic fallback")
    require("materialParams_realStars" in material and "materialParams_tilingStars" in material
            and "1.0 - smoothstep(-0.18, 0.055, lightDirection.y)" in material,
            "UDS stars and continuous day/night fade")
    require("(1.0 - cloudOpacity) * (1.0 - moonMask)" in material,
            "star cloud and Moon occlusion hooks")

    require("cloudShellLayer" in material and "raySphere" in material and "baseCloud" in material
            and "highCloud" in material, "camera-centered base/cirrus spherical cloud layers")
    require("1.0 - exp(-density" in material and "hgPhase(cosine, 0.88)" in material
            and "sunTransmittance" in material, "Beer-Lambert and forward-scattering cloud lighting")
    require("materialParams.cloudArtTint" in material and "lighting * materialParams.cloud2.z" in material,
            "Cloud Art Tint is a multiplier after physical lighting")
    require("High Experimental" in controls and "quality > 1.5" in material and "raymarch" not in material.lower(),
            "Low/Medium/High shell tiers without default raymarch")

    require(renderer.count("new Material.Builder()") == 1 and renderer.count("new RenderableManager.Builder(1)") == 1,
            "one permanent material and one sky renderable")
    uniform_method = renderer[renderer.index("private void applyUniforms"):renderer.index("private void setActive")]
    require("uniformUpdateCount++" in renderer and "materialRebuildCount" in renderer
            and "new Texture.Builder" not in uniform_method, "uniform-only hot path without texture/material rebuild")
    require("legacy_fallback_material_unavailable" in renderer and "legacyCelestialFallback" in adapter,
            "automatic legacy fallback on material failure")
    require("state.clouds.visibleGroups = controls.cloudsEnabled ? 12 : 0" in adapter,
            "material failure restores legacy cloud fallback budget")
    require("for (String[] sizeNames : STAR_VARIANT_NAMES)" in adapter and "for (String name : CLOUD_NAMES)" in adapter,
            "legacy star/cloud geometry is explicitly hidden in analytic mode")

    manifest = json.loads((P63 / "P63_3_SKY_TRUTH_MANIFEST.json").read_text(encoding="utf-8"))
    allowed = {"UDS_VERIFIED", "UDS_DERIVED_MAPPING", "FILAMENT_ADAPTED", "SOLUM_NATIVE", "UNKNOWN", "UNAVAILABLE"}
    require(manifest["upstream"]["commit"] == "579991668ebeadceece05d79b62f21964028553f", "pinned Filament upstream")
    require(all(item["provenance"] in allowed for item in manifest["resources"]), "manifest provenance vocabulary")
    for item in manifest["resources"]:
        resource_path = item.get("path")
        expected_hash = item.get("sha256")
        if resource_path and resource_path.startswith("apps/") and expected_hash:
            require(sha256(ROOT / resource_path) == expected_hash, f"manifest hash: {resource_path}")
    hashes = {item.get("sha256") for item in manifest["resources"]}
    require({"8a8ff79b0d06946bfd09efcada50cc4a9891076b7f2a00b49fa8e182bbb6e375",
             "1b0e0306afc8626bdf1e06f809c6f3fe01b3cb997234ca2653a5ef89af9a9998",
             "feb52ae23909cd4a9faf1f9384d1661711c5dedc07dde31e4dda444e7745f69c",
             "841f09169dc0e955a580c0faef8b1a62372d06e5340b701b757a64f51242e8c5"} <= hashes,
            "verified Moon/star hashes")
    require("MOON_ALBEDO_PATHS" in resources and "starTextureAvailable = 1.0f" in resources,
            "verified private resources bind with procedural fallback")

    presets = json.loads((P63 / "P63_3_SKY_PRESETS.json").read_text(encoding="utf-8"))["presets"]
    require(len(presets) == 14 and all({"inputs", "expectedVisibleResult", "quality", "provenance", "cameraPreset"} <= item.keys()
                                       for item in presets), "14 complete sky verification presets")
    require('"atmosphere", "Atmosphere"' in activity and '"p63.log.slider." + key' in activity
            and "numeric safety=0.." in activity, "Atmosphere tab and log/exact physical controls")
    for key in ("sun", "moon", "stars", "clouds"):
        require(f'showP63ColorPicker("{key}")' in activity, f"circular color picker: {key}")
    require("active sky renderer=" in activity and " · rebuild=" in activity
            and "uniform updates=" in activity and "dynamic IBL=" in activity,
            "analytic debug counters and IBL truth")
    for flag in ("analyticSky", "analyticSun", "analyticMoon", "analyticStars", "analyticClouds", "legacyCelestialFallback"):
        require(flag in controls and flag in activity, f"runtime feature flag: {flag}")

    require("intensity * 30000.0f * blend" not in activity, "forbidden IndirectLight multiplier absent")
    require("float rawIntensity = intensity * blend" in activity and "clamp(rawIntensity, 0.0f, 2.0f)" in activity,
            "IndirectLight 0..2 clamp unchanged")
    require("getWaterColor" not in material and "waterControl" not in material
            and "dynamicIbl" not in material,
            "water and dynamic IBL excluded from material")


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
        "SolumAnalyticSkyRenderer.java", "SolumAnalyticSkyResources.java",
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
    validate_p63_3()
    validate_ibl_assets()
    validate_ui_preservation()
    validate_runtime_wiring()
    compile_and_run_core()
    run_legacy_environment_regression()
    print("P63_ENVIRONMENT_TESTS=PASS p63_3=true presets=14 analyticSky=true legacyFallback=true oldIbl=true dynamicIbl=false")


if __name__ == "__main__":
    main()
