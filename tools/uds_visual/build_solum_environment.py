#!/usr/bin/env python3
"""Build the compact P62B SOLUM native environment package from local UDS truth."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path
from typing import Any

import build_uds_visual_truth as uds_truth


REPO_ROOT = Path(__file__).resolve().parents[2]
PACKAGE_SCHEMA = "solum.environment.package"
PACKAGE_VERSION = 1
PACKAGE_ID = "solum.native-environment.p62b"
PROVENANCE = ("UDS_VERIFIED", "UDS_DERIVED_MAPPING", "SOLUM_NATIVE", "UNKNOWN", "UNAVAILABLE")
REQUIRED_PRESETS = (
    "Clear_Skies", "Cloudy", "Foggy", "Overcast", "Partly_Cloudy", "Rain",
    "Rain_Light", "Rain_Thunderstorm", "Sand_Dust_Calm", "Sand_Dust_Storm",
    "Snow", "Snow_Blizzard", "Snow_Light",
)
RUNTIME_FIELDS = (
    "cloudCoverage", "cloudDensity", "cloudHeight", "cloudThickness",
    "cloudProfileLow", "cloudProfileMid", "cloudProfileHigh",
    "fogDensity", "fogHeightFalloff", "rain", "snow", "dust",
    "windDirectionDeg", "windSpeed", "windGust", "windTurbulence",
    "lightningPotential", "lightningEnabled", "wetnessTarget", "humidity",
    "atmosphereHaze", "atmosphereAbsorption", "lightingScale", "ambientScale",
    "exposure", "audioAutomatic",
)
CURVE_PACKAGES = {
    "/Game/UltraDynamicSky/Materials/Float_Curves/Skyatmosphere_Density",
    "/Game/UltraDynamicSky/Materials/Float_Curves/Directional_Light_Intensity",
    "/Game/UltraDynamicSky/Materials/Color_Curves/Sun_Light_Color",
    "/Game/UltraDynamicSky/Blueprints/Weather_Effects/System/CloudCoverage_RGB",
}
PARAMETER_NAMES = {
    "Base Cloud Height", "Base Fog Density", "Base Height Fog Falloff",
    "Base Wetness when Clear", "Base Wetness when Raining", "Cloud Direction",
    "Cloud Speed", "Cloudy Density Contribution", "Close Thunder Delay Per KM",
    "Dawn Time", "Day Length", "Directional Light Scattering Curve", "Dusk Time",
    "Dusty Height Fog Falloff", "Foggy Density Contribution", "Foggy Height Fog Falloff",
    "Lightning Effect Tint Color", "Lightning Flash Duration", "Lightning Flash Frequency",
    "Lightning Flash Light Intensity", "Lightning Flash Light Source Color",
    "Lightning Flash Spawn Period", "Lightning Flash Timing Randomization",
    "Material Water Roughness", "Max Material Wetness", "Moon Light Color",
    "Moon Light Intensity", "Moon Scale", "Puddle Coverage", "Puddle Sharpness",
    "Rain Drops Alpha", "Rain Drops Scale", "Rain Particle Spawn Count",
    "Rain Velocity Randomization", "Rain Wind Velocity", "Snow Flakes Alpha",
    "Snow Flakes Scale", "Snow Particle Spawn Count", "Snow Velocity Randomization",
    "Snow Wind Velocity", "Stars Intensity", "Stars Speed", "Sun Disk Intensity",
    "Sun Light Color", "Sun Light Intensity", "Time of Day", "Time Speed",
    "Transition Duration", "Transition Easing Exponent", "Twinkle Amount", "Twinkle Speed",
    "Wetness Coverage Duration", "Wetness Dry Duration", "Wetness Dry Speed in Sunlight",
    "Wetness Dry Speed without Sunlight", "Wind Direction", "Wind Gust Intensity",
    "Wind Gust Speed",
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
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def owned_build_directory(path: Path) -> bool:
    generated_root = (REPO_ROOT / "generated_local").resolve()
    try:
        path.resolve().relative_to(generated_root)
        return True
    except ValueError:
        marker = path / "reports" / "SOLUM_ENVIRONMENT_BUILD_REPORT.json"
        if not marker.is_file():
            return False
        try:
            return read_json(marker).get("packageId") == PACKAGE_ID
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            return False


def remove_owned_build_directory(path: Path) -> None:
    if not owned_build_directory(path):
        raise ValueError(f"refusing to replace unowned output directory: {path}")
    shutil.rmtree(path)


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


def compact_evidence(value: dict[str, Any] | None) -> dict[str, Any]:
    value = value or {}
    return {
        key: value.get(key)
        for key in (
            "source_package", "source_sha256", "source_object", "export_index",
            "property", "value_sha256", "decode_status",
        )
        if value.get(key) is not None
    }


def exact_parameter(parameters: list[dict[str, Any]], name: str) -> dict[str, Any]:
    matches = [item for item in parameters if item["name"] == name]
    if not matches:
        raise ValueError(f"required UDS parameter is absent: {name}")
    item = matches[0]
    return {
        "value": item["default"],
        "status": "UDS_VERIFIED",
        "unit": item.get("unit"),
        "evidence": compact_evidence(item.get("evidence")),
    }


def range_values(value: Any) -> list[float]:
    found: list[float] = []

    def visit(node: Any, parent_name: str | None = None) -> None:
        if isinstance(node, dict):
            if node.get("name") == "Value" and isinstance(node.get("value"), (int, float)):
                found.append(float(node["value"]))
            name = node.get("name") if isinstance(node.get("name"), str) else parent_name
            for child in node.values():
                visit(child, name)
        elif isinstance(node, list):
            for child in node:
                visit(child, parent_name)

    visit(value)
    unique: list[float] = []
    for item in found:
        if item not in unique:
            unique.append(item)
    return unique[:2]


def compact_curves(curves: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result = []
    for curve in curves:
        if curve.get("source_package") not in CURVE_PACKAGES:
            continue
        browser_active = curve.get("source_package", "").endswith("/CloudCoverage_RGB")
        result.append({
            "id": curve["id"],
            "sourcePackage": curve.get("source_package"),
            "sourceSha256": curve.get("source_sha256"),
            "status": "UDS_VERIFIED",
            "browserActive": browser_active,
            "browserMappingStatus": "UDS_DERIVED_MAPPING" if browser_active else "UNKNOWN",
            "channels": [
                {
                    "name": channel.get("name"),
                    "keys": [
                        {
                            "time": key.get("time"),
                            "value": key.get("value"),
                            "interp": key.get("interp_mode", {}).get("name"),
                            "arriveTangent": key.get("arrive_tangent"),
                            "leaveTangent": key.get("leave_tangent"),
                        }
                        for key in channel.get("keys", [])
                    ],
                }
                for channel in curve.get("channels", [])
            ],
        })
    return result


def sample_linear_curve(curve: dict[str, Any], input_value: float) -> dict[str, float]:
    result: dict[str, float] = {}
    for channel in curve.get("channels", []):
        keys = channel.get("keys", [])
        if not keys:
            continue
        if input_value <= float(keys[0]["time"]):
            result[channel["name"]] = float(keys[0]["value"])
            continue
        if input_value >= float(keys[-1]["time"]):
            result[channel["name"]] = float(keys[-1]["value"])
            continue
        for left, right in zip(keys, keys[1:]):
            low, high = float(left["time"]), float(right["time"])
            if low <= input_value <= high:
                alpha = (input_value - low) / max(1e-9, high - low)
                result[channel["name"]] = float(left["value"]) + (float(right["value"]) - float(left["value"])) * alpha
                break
    return result


def preset_runtime(
    preset: dict[str, Any], shared: dict[str, float], cloud_curve: dict[str, Any] | None = None,
) -> tuple[dict[str, Any], dict[str, Any]]:
    values = preset["values"]

    def source_number(name: str) -> float | None:
        value = values.get(name)
        return float(value) if isinstance(value, (int, float)) else None

    cloud_source = source_number("Cloud Coverage")
    fog_source = source_number("Fog")
    rain_source = source_number("Rain")
    snow_source = source_number("Snow")
    dust_source = source_number("Dust")
    wind_source = source_number("Wind Intensity")
    lightning_source = source_number("Thunder/Lightning")
    wetness_source = source_number("Material Wetness")
    snow_cover = source_number("Material Snow Coverage")
    dust_cover = source_number("Material Dust Coverage")

    cloud = clamp(cloud_source / 10.0) if cloud_source is not None else 0.08
    cloud_profile = (
        sample_linear_curve(cloud_curve, cloud_source if cloud_source is not None else cloud * 10.0)
        if cloud_curve else {}
    )
    fog = clamp(fog_source / 10.0) if fog_source is not None else 0.0
    rain = clamp((rain_source or 0.0) / 10.0)
    snow = clamp((snow_source or 0.0) / 10.0)
    dust = clamp((dust_source or 0.0) / 10.0)
    wind = clamp((wind_source or 0.0) / 10.0)
    lightning = clamp((lightning_source or 0.0) / 10.0)
    lightning_enabled = 1.0 if preset["id"] == "Rain_Thunderstorm" else 0.0
    if wetness_source is None:
        wetness = clamp(rain * 0.82 + fog * 0.18 + snow * 0.12)
    else:
        wetness = clamp(wetness_source)
    humidity = clamp(0.22 + fog * 0.62 + rain * 0.72 + snow * 0.32 - dust * 0.25)
    lighting = clamp(1.0 - cloud * 0.52 - fog * 0.12 - dust * 0.28, 0.18, 1.0)
    runtime = {
        "cloudCoverage": cloud,
        "cloudDensity": clamp(0.28 + cloud * 0.72),
        "cloudHeight": shared["cloudHeight"],
        "cloudThickness": clamp(0.22 + cloud * 0.68),
        "cloudProfileLow": clamp(cloud_profile.get("x", 1.0 - cloud)),
        "cloudProfileMid": clamp(cloud_profile.get("y", 4.0 * cloud * (1.0 - cloud))),
        "cloudProfileHigh": clamp(cloud_profile.get("z", cloud)),
        "fogDensity": shared["baseFogDensity"] + fog * shared["foggyDensityContribution"],
        "fogHeightFalloff": (
            shared["dustyFogFalloff"] if dust > 0.0
            else shared["baseFogFalloff"] * (1.0 - fog) + shared["foggyFogFalloff"] * fog
        ),
        "rain": rain,
        "snow": snow,
        "dust": dust,
        "windDirectionDeg": shared["windDirection"],
        "windSpeed": wind,
        "windGust": clamp(shared["windGust"] * (0.35 + wind * 0.9)),
        "windTurbulence": clamp(0.08 + wind * 0.78 + dust * 0.18),
        "lightningPotential": lightning,
        "lightningEnabled": lightning_enabled,
        "wetnessTarget": wetness,
        "humidity": humidity,
        "atmosphereHaze": clamp(fog * 0.72 + dust * 0.88 + humidity * 0.12),
        "atmosphereAbsorption": clamp(dust * 0.82 + cloud * 0.16),
        "lightingScale": lighting,
        "ambientScale": clamp(0.3 + lighting * 0.7 + cloud * 0.08),
        "exposure": clamp(0.72 + lighting * 0.34, 0.72, 1.06),
        "audioAutomatic": 0.0,
    }
    runtime["audioProfile"] = (
        "rain_hits_manual" if rain > 0.0 else "dust_hit_manual" if dust > 0.0 else "none"
    )
    runtime["surfaceSnowTarget"] = clamp(snow_cover or snow * 0.65)
    runtime["surfaceDustTarget"] = clamp(dust_cover or dust * 0.7)

    provenance: dict[str, Any] = {}
    sources = {
        "cloudCoverage": "Cloud Coverage", "cloudDensity": "Cloud Coverage",
        "cloudHeight": None, "cloudThickness": "Cloud Coverage",
        "cloudProfileLow": "Cloud Coverage", "cloudProfileMid": "Cloud Coverage",
        "cloudProfileHigh": "Cloud Coverage", "fogDensity": "Fog",
        "fogHeightFalloff": "Fog", "rain": "Rain", "snow": "Snow", "dust": "Dust",
        "windDirectionDeg": None, "windSpeed": "Wind Intensity", "windGust": "Wind Intensity",
        "windTurbulence": "Wind Intensity", "lightningPotential": "Thunder/Lightning",
        "lightningEnabled": "Thunder/Lightning", "wetnessTarget": "Material Wetness",
        "humidity": None, "atmosphereHaze": None, "atmosphereAbsorption": None,
        "lightingScale": None, "ambientScale": None, "exposure": None,
        "audioAutomatic": None,
    }
    for field in RUNTIME_FIELDS:
        source_name = sources[field]
        has_source = source_name is not None and source_number(source_name) is not None
        if field in ("cloudHeight", "windDirectionDeg"):
            has_source = True
        provenance[field] = {
            "status": "UDS_DERIVED_MAPPING" if has_source else "SOLUM_NATIVE",
            "sourceField": source_name,
            "mapping": "normalized/mobile adapter; not Unreal runtime parity",
        }
    provenance["audioAutomatic"] = {
        "status": "UNKNOWN",
        "sourceField": None,
        "mapping": "disabled because current MetaSound event binding is unknown",
    }
    provenance["surfaceSnowTarget"] = {
        "status": "UDS_DERIVED_MAPPING" if snow_cover is not None else "SOLUM_NATIVE",
        "sourceField": "Material Snow Coverage" if snow_cover is not None else None,
        "mapping": "surface diagnostic adapter",
    }
    provenance["surfaceDustTarget"] = {
        "status": "UDS_DERIVED_MAPPING" if dust_cover is not None else "SOLUM_NATIVE",
        "sourceField": "Material Dust Coverage" if dust_cover is not None else None,
        "mapping": "surface diagnostic adapter",
    }
    provenance["audioProfile"] = {
        "status": "UNKNOWN",
        "sourceField": None,
        "mapping": "manual payload audition only",
    }
    return runtime, provenance


def exact_preset_values(preset: dict[str, Any]) -> dict[str, Any]:
    result = {}
    for name, value in sorted(preset["values"].items()):
        if name == "User Friendly Name":
            continue
        result[name] = {
            "value": value,
            "status": "UDS_VERIFIED",
            "evidence": compact_evidence(preset["evidence"].get(name)),
        }
    return result


def copy_templates(output: Path) -> list[dict[str, Any]]:
    root = Path(__file__).resolve().parent / "environment_templates"
    files = []
    for source in sorted(path for path in root.rglob("*") if path.is_file()):
        relative = source.relative_to(root)
        target = output / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        files.append({"path": relative.as_posix(), "sha256": sha256_file(target), "size": target.stat().st_size})
    return files


def scenario_contracts(presets: list[dict[str, Any]], dawn: float, dusk: float) -> list[dict[str, Any]]:
    by_id = {item["id"]: item for item in presets}
    data = (
        ("noon", "Полдень", 1200.0, "Clear_Skies", 0.0, "short shadows, bright atmosphere"),
        ("sunset", "Закат", dusk, "Partly_Cloudy", 0.0, "warm horizon, low sun disk"),
        ("night", "Ночь", 0.0, "Clear_Skies", 0.0, "moon, rotating stars, night lighting"),
        ("cloudy", "Облачно", 1400.0, "Cloudy", 1.5, "dense moving clouds, dimmed light"),
        ("rain", "Дождь", 1430.0, "Rain", 2.0, "rain streaks, fog, accumulating wet ground"),
        ("snow", "Снег", 1000.0, "Snow", 2.0, "wind-driven flakes and snow surface response"),
        ("storm", "Гроза", 1600.0, "Rain_Thunderstorm", 1.5, "rain, cloud lightning and deterministic bolt"),
        ("clear_to_rain", "Ясно → дождь", 1300.0, "Rain", 6.0, "continuous cloud, fog, rain and wetness blend"),
        ("day_to_night", "День → ночь", 0.0, "Partly_Cloudy", 8.0, "continuous celestial and atmosphere change"),
    )
    scenarios = []
    for scenario_id, label, time_value, preset_id, duration, expected in data:
        initial = {"timeOfDay": dawn if scenario_id == "day_to_night" else time_value, "weatherPreset": "Clear_Skies"}
        scenarios.append({
            "id": scenario_id,
            "label": label,
            "input": initial,
            "target": {"timeOfDay": time_value, "weatherPreset": preset_id, "transitionSeconds": duration},
            "finalRuntime": by_id[preset_id]["runtime"],
            "provenance": ["UDS_VERIFIED", "UDS_DERIVED_MAPPING", "SOLUM_NATIVE"],
            "expectedVisualSigns": expected,
            "camera": {"yaw": 0.72, "pitch": 0.34, "distance": 13.5, "target": [0.0, 1.2, 0.0]},
        })
    return scenarios


def build(args: argparse.Namespace) -> dict[str, Any]:
    p60 = Path(args.p60).resolve()
    p61 = Path(args.p61).resolve()
    output = Path(args.output).resolve()
    for required in (
        p60 / "EXTRACTION_GATE.json", p60 / "inventory.json",
        p61 / "dependencies" / "package_index.json",
        p61 / "dependencies" / "dependency_closure.json",
    ):
        if not required.is_file():
            raise FileNotFoundError(required)

    package_index = read_json(p61 / "dependencies" / "package_index.json")
    if package_index.get("package_count") != 802 or package_index.get("errors"):
        raise ValueError("P61 package index is not the expected error-free 802-package source")
    records = {item["package_name"]: item for item in package_index["packages"]}
    for package_name in uds_truth.ROOT_PACKAGES[:2]:
        if package_name not in records:
            raise ValueError(f"required package missing: {package_name}")

    parameters: list[dict[str, Any]] = []
    for package_name in uds_truth.ROOT_PACKAGES[:2]:
        _summary, decoded = uds_truth.cdo_contract(records[package_name])
        parameters.extend(decoded)
    parameters = [item for item in parameters if item["name"] in PARAMETER_NAMES]
    exact = {name: exact_parameter(parameters, name) for name in sorted(PARAMETER_NAMES)}
    metadata = uds_truth.variable_metadata(records[uds_truth.ROOT_PACKAGES[0]], {"Time of Day"})
    if len(metadata) != 1 or metadata[0].get("metadata", {}).get("UIMin") != "0" or metadata[0].get("metadata", {}).get("UIMax") != "2400":
        raise ValueError("verified Time of Day range 0..2400 is absent")

    source_presets = uds_truth.weather_presets(records)
    if tuple(item["id"] for item in source_presets) != REQUIRED_PRESETS:
        raise ValueError("verified UDS preset set differs from the required 13 states")
    curves = compact_curves(uds_truth.curve_contracts(p60))
    if len(curves) != 4:
        raise ValueError(f"expected four compact verified curves, got {len(curves)}")
    cloud_curve = next((item for item in curves if item["browserActive"]), None)
    if cloud_curve is None:
        raise ValueError("verified CloudCoverage_RGB curve is absent")
    shared = {
        "cloudHeight": float(exact["Base Cloud Height"]["value"]),
        "baseFogDensity": float(exact["Base Fog Density"]["value"]),
        "baseFogFalloff": float(exact["Base Height Fog Falloff"]["value"]),
        "foggyDensityContribution": float(exact["Foggy Density Contribution"]["value"]),
        "foggyFogFalloff": float(exact["Foggy Height Fog Falloff"]["value"]),
        "dustyFogFalloff": float(exact["Dusty Height Fog Falloff"]["value"]),
        "windDirection": float(exact["Wind Direction"]["value"]),
        "windGust": float(exact["Wind Gust Intensity"]["value"]),
    }
    presets = []
    for source in source_presets:
        runtime, runtime_provenance = preset_runtime(source, shared, cloud_curve)
        presets.append({
            "id": source["id"],
            "name": source["name"],
            "status": "UDS_VERIFIED",
            "sourcePackage": source["source_package"],
            "sourceSha256": source["source_sha256"],
            "exactUdsValues": exact_preset_values(source),
            "runtime": runtime,
            "runtimeProvenance": runtime_provenance,
        })

    temp = output.with_name(output.name + ".tmp")
    if temp.exists():
        remove_owned_build_directory(temp)
    temp.mkdir(parents=True)
    (temp / "data").mkdir()
    (temp / "reports").mkdir()
    (temp / "assets").mkdir()
    template_files = copy_templates(temp)
    audio_assets = uds_truth.audio_assets(p60, temp / "assets", records)
    selected_textures = []
    texture_tokens = (
        "Moon_Color", "Moon_PhaseNormal", "Real_Stars", "Stars_Noise",
        "CloudsAlpha", "ParticleClouds", "3D_Cells", "ObscuredLightning",
    )
    for item in uds_truth.unavailable_texture_sources(records, parameters):
        if any(token in item["source_package"] for token in texture_tokens):
            selected_textures.append({
                "sourcePackage": item["source_package"],
                "sourceSha256": item["source_sha256"],
                "status": "UNAVAILABLE",
                "browserActive": False,
                "reason": item["reason"],
            })

    dawn = float(exact["Dawn Time"]["value"])
    dusk = float(exact["Dusk Time"]["value"])
    flash_duration = range_values(exact["Lightning Flash Duration"]["value"])
    if len(flash_duration) != 2:
        raise ValueError("verified lightning duration range was not decoded")
    modules = [
        {"name": "SolumEnvironmentState", "status": "SOLUM_NATIVE"},
        {"name": "SolumTimeSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumCelestialSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumAtmosphereSystem", "status": "SOLUM_NATIVE"},
        {"name": "SolumCloudSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumFogSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumWeatherController", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumPrecipitationSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumWindSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumLightningSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumWetnessSystem", "status": "UDS_DERIVED_MAPPING"},
        {"name": "SolumEnvironmentAudioSystem", "status": "UNKNOWN"},
        {"name": "SolumEnvironmentLightingState", "status": "SOLUM_NATIVE"},
    ]
    package = {
        "schema": PACKAGE_SCHEMA,
        "schemaVersion": PACKAGE_VERSION,
        "packageId": PACKAGE_ID,
        "renderer": "WebGL2",
        "provenanceLegend": {
            "UDS_VERIFIED": "exact decoded UDS value or payload",
            "UDS_DERIVED_MAPPING": "SOLUM adapter driven by verified UDS data",
            "SOLUM_NATIVE": "independent SOLUM implementation; not UDS parity",
            "UNKNOWN": "semantics or binding are not established",
            "UNAVAILABLE": "required payload is unavailable on this host",
        },
        "modules": modules,
        "time": {
            "range": {"value": [0.0, 2400.0], "status": "UDS_VERIFIED", "evidence": metadata[0]["container_evidence"]},
            "initial": exact["Time of Day"],
            "speed": exact["Time Speed"],
            "dayLength": exact["Day Length"],
            "dawn": exact["Dawn Time"],
            "dusk": exact["Dusk Time"],
            "previewDaySeconds": {"value": 240.0, "status": "SOLUM_NATIVE"},
            "wrap": {"value": "modulo_2400", "status": "SOLUM_NATIVE"},
        },
        "celestial": {
            "sunIntensity": exact["Sun Light Intensity"],
            "sunColor": exact["Sun Light Color"],
            "sunDiskIntensity": exact["Sun Disk Intensity"],
            "moonIntensity": exact["Moon Light Intensity"],
            "moonColor": exact["Moon Light Color"],
            "moonScale": exact["Moon Scale"],
            "starsIntensity": exact["Stars Intensity"],
            "starsSpeed": exact["Stars Speed"],
            "twinkleAmount": exact["Twinkle Amount"],
            "twinkleSpeed": exact["Twinkle Speed"],
            "orbitMapping": {"value": "dawn/dusk anchored solar arc; moon opposite sun", "status": "UDS_DERIVED_MAPPING"},
            "moonPhase": {"value": 0.62, "status": "SOLUM_NATIVE"},
            "curves": curves,
        },
        "atmosphere": {
            "model": {"value": "mobile Rayleigh/Mie approximation", "status": "SOLUM_NATIVE"},
            "verifiedDensityCurve": {"value": "Skyatmosphere_Density", "status": "UDS_VERIFIED"},
            "unrealParity": {"value": False, "status": "UNKNOWN"},
        },
        "clouds": {
            "baseHeight": exact["Base Cloud Height"],
            "direction": exact["Cloud Direction"],
            "speed": exact["Cloud Speed"],
            "noise": {"value": "seeded shader FBM", "status": "SOLUM_NATIVE"},
            "texturePayload": {"value": None, "status": "UNAVAILABLE"},
        },
        "fog": {
            "baseDensity": exact["Base Fog Density"],
            "baseHeightFalloff": exact["Base Height Fog Falloff"],
            "foggyContribution": exact["Foggy Density Contribution"],
            "distanceHeightAdapter": {"value": True, "status": "UDS_DERIVED_MAPPING"},
        },
        "precipitation": {
            "rainSpawnSource": exact["Rain Particle Spawn Count"],
            "snowSpawnSource": exact["Snow Particle Spawn Count"],
            "rainWindVelocity": exact["Rain Wind Velocity"],
            "snowWindVelocity": exact["Snow Wind Velocity"],
            "rainAlpha": exact["Rain Drops Alpha"],
            "rainScale": exact["Rain Drops Scale"],
            "rainVelocityRandomization": exact["Rain Velocity Randomization"],
            "snowAlpha": exact["Snow Flakes Alpha"],
            "snowScale": exact["Snow Flakes Scale"],
            "snowVelocityRandomization": exact["Snow Velocity Randomization"],
            "adapter": {"value": "deterministic WebGL2 point particles", "status": "UDS_DERIVED_MAPPING"},
            "niagaraVm": {"value": False, "status": "UNAVAILABLE"},
        },
        "wind": {
            "direction": exact["Wind Direction"],
            "gustIntensity": exact["Wind Gust Intensity"],
            "gustSpeed": exact["Wind Gust Speed"],
            "propagation": {"value": ["clouds", "rain", "snow", "dust", "flags", "vegetation"], "status": "SOLUM_NATIVE"},
        },
        "lightning": {
            "seed": {"value": 1597463007, "status": "SOLUM_NATIVE"},
            "flashDurationRange": {"value": flash_duration, "status": "UDS_VERIFIED", "evidence": exact["Lightning Flash Duration"]["evidence"]},
            "frequency": exact["Lightning Flash Frequency"],
            "spawnPeriod": exact["Lightning Flash Spawn Period"],
            "timingRandomization": exact["Lightning Flash Timing Randomization"],
            "lightIntensity": exact["Lightning Flash Light Intensity"],
            "lightColor": exact["Lightning Flash Light Source Color"],
            "thunderDelayPerKm": exact["Close Thunder Delay Per KM"],
            "thunderBinding": {"value": None, "status": "UNKNOWN"},
        },
        "wetness": {
            "coverageDuration": exact["Wetness Coverage Duration"],
            "dryDuration": exact["Wetness Dry Duration"],
            "drySpeedSun": exact["Wetness Dry Speed in Sunlight"],
            "drySpeedShade": exact["Wetness Dry Speed without Sunlight"],
            "waterRoughness": exact["Material Water Roughness"],
            "puddleCoverage": exact["Puddle Coverage"],
            "dynamicAdapter": {"value": "rain accumulation and sunlight/shade drying", "status": "UDS_DERIVED_MAPPING"},
        },
        "audio": {
            "automatic": {"value": False, "status": "UNKNOWN"},
            "manualAudition": {"value": True, "status": "SOLUM_NATIVE"},
            "reason": "verified WAV payloads exist, but current MetaSound event bindings are unknown",
        },
        "lighting": {
            "model": {"value": "sun/moon directional plus atmosphere ambient", "status": "SOLUM_NATIVE"},
            "weatherMapping": {"value": "cloud/fog/dust attenuation", "status": "UDS_DERIVED_MAPPING"},
        },
        "qualityTiers": {
            "Low": {"cloudSteps": 3, "particleLimit": 480, "pixelRatioMax": 1.25, "renderLongEdgeMax": 1280, "status": "SOLUM_NATIVE"},
            "Medium": {"cloudSteps": 5, "particleLimit": 1100, "pixelRatioMax": 1.6, "renderLongEdgeMax": 1600, "status": "SOLUM_NATIVE"},
            "High": {"cloudSteps": 8, "particleLimit": 2400, "pixelRatioMax": 2.0, "renderLongEdgeMax": 1920, "status": "SOLUM_NATIVE"},
        },
        "weatherPresets": presets,
        "scenarios": scenario_contracts(presets, dawn, dusk),
        "resources": {
            "audio": [
                {
                    "id": item["id"], "path": item["browser_path"], "sha256": item["output_sha256"],
                    "size": item["size"], "sourcePackage": item["source_package"],
                    "payloadStatus": "UDS_VERIFIED", "bindingStatus": "UNKNOWN", "automatic": False,
                }
                for item in audio_assets
            ],
            "textures": selected_textures,
            "native": [
                {"id": "procedural-stars", "status": "SOLUM_NATIVE", "seed": 1337},
                {"id": "procedural-cloud-noise", "status": "SOLUM_NATIVE", "seed": 424242},
                {"id": "procedural-precipitation", "status": "SOLUM_NATIVE", "seed": 9001},
            ],
        },
        "sourceSummary": {
            "p60InventorySha256": sha256_file(p60 / "inventory.json"),
            "p61PackageIndexSha256": sha256_file(p61 / "dependencies" / "package_index.json"),
            "weatherPresetCount": len(presets),
            "verifiedCurveCount": len(curves),
            "verifiedAudioPayloadCount": len(audio_assets),
            "runtimeReadsP60P61": False,
            "unrealSystemsIntentionallyNotExecuted": ["Blueprint VM", "Niagara VM", "Material Graph", "MetaSound"],
        },
    }
    write_json(temp / "data" / "solum_environment_package.json", package)
    report = {
        "schema": "solum.environment.build-report",
        "schemaVersion": 1,
        "result": "BUILT",
        "packageId": PACKAGE_ID,
        "packageSha256": sha256_file(temp / "data" / "solum_environment_package.json"),
        "weatherPresets": len(presets),
        "templateFiles": template_files,
        "verifiedAudioPayloads": len(audio_assets),
        "unavailableTextures": len(selected_textures),
        "runtimeInputFiles": ["data/solum_environment_package.json"],
        "deterministic": True,
    }
    write_json(temp / "reports" / "SOLUM_ENVIRONMENT_BUILD_REPORT.json", report)
    if output.exists():
        remove_owned_build_directory(output)
    temp.replace(output)
    return report


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--p60", required=True)
    result.add_argument("--p61", required=True)
    result.add_argument("--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"))
    return result


def main() -> int:
    args = parser().parse_args()
    report = build(args)
    print(json.dumps({
        "status": report["result"],
        "output": str(Path(args.output).resolve()),
        "weather_presets": report["weatherPresets"],
        "package_sha256": report["packageSha256"],
    }, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
