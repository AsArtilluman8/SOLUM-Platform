#!/usr/bin/env python3
"""Build a private, provenance-first P63.10 truth set from an owned UDS source tree.

The target list is intentionally explicit. Nothing is selected by filename similarity and no
placeholder payload is generated when an Unreal package cannot be decoded exactly.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
UE_TOOL = ROOT / "tools/ue_asset_tool/src"
if str(UE_TOOL) not in sys.path:
    sys.path.insert(0, str(UE_TOOL))

from ueassettool.contracts import (  # noqa: E402
    export_auto_contract,
    export_blueprint_contract,
    export_material_contract,
    export_metasound_contract,
    export_niagara_contract,
    verify_package,
)
from ueassettool.errors import UEAssetError  # noqa: E402
from ueassettool.media import export_media  # noqa: E402
from ueassettool.package import UnrealPackage  # noqa: E402


SCHEMA = "solum.p63.10.uds-premium-extraction/v1"
DEFAULT_DATASET = ROOT / "_work/private_uds_p63_10"
GROUPS = (
    "stars",
    "celestial",
    "clouds",
    "aurora",
    "weather",
    "surface",
    "wind",
    "audio",
    "control",
)
CONTRACT_PASS_STATUSES = frozenset(("VERIFIED", "RAW_VERIFIED", "PASS"))


@dataclass(frozen=True)
class Target:
    group: str
    relative_path: str
    contract: str
    media: str | None = None
    output_suffix: str | None = None
    required: bool = True

    @property
    def key(self) -> str:
        return self.relative_path.removesuffix(".uasset").replace("/", "__")


TARGETS = (
    # Stars: exact UDS source textures plus both functions that define projection and animation.
    Target("stars", "Textures/Sky/Real_Stars.uasset", "auto", "texture", ".png"),
    Target("stars", "Textures/Sky/Tiling_Stars.uasset", "auto", "texture", ".png"),
    Target("stars", "Textures/Sky/Stars_Noise.uasset", "auto", "texture", ".png"),
    Target("stars", "Textures/Weather/ParticleClouds.uasset", "auto", "texture", ".png"),
    Target("stars", "Materials/Material_Functions/Stars.uasset", "material"),
    Target("stars", "Materials/Material_Functions/Tiling_Stars_UVs.uasset", "material"),

    # Sun/Moon body, phase, centered gradients and time-dependent UDS color/intensity curves.
    # Atmosphere LUT payloads are audited as optional inputs until their exact mobile binding is
    # proven; the analytic Sun has no body texture in UDS and must remain material-driven.
    Target("celestial", "Textures/Sky/Moon_Color.uasset", "auto", "texture", ".png"),
    Target("celestial", "Textures/Sky/Moon_PhaseNormal.uasset", "auto", "texture", ".png"),
    Target(
        "celestial",
        "Textures/Sky/Sun_Atmosphere_LUT.uasset",
        "auto",
        "texture",
        ".png",
        required=False,
    ),
    Target(
        "celestial",
        "Textures/Sky/Moon_Atmosphere_LUT.uasset",
        "auto",
        "texture",
        ".png",
        required=False,
    ),
    Target(
        "celestial",
        "Textures/Sky/Sun_Atmosphere_LUT_Volume.uasset",
        "auto",
        "texture",
        ".npy",
        required=False,
    ),
    Target(
        "celestial",
        "Textures/Sky/Moon_Atmosphere_LUT_Volume.uasset",
        "auto",
        "texture",
        ".npy",
        required=False,
    ),
    Target("celestial", "Materials/Ultra_Dynamic_Sky_Mat.uasset", "material"),
    # Direct material-function dependency closure of Ultra_Dynamic_Sky_Mat. These are semantic
    # inputs, not optional filename matches: each path is referenced by an extracted call node.
    Target("celestial", "Materials/Material_Functions/Base_Sky_Color.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Composite_Static_Clouds.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Contrast_Control.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/FlatOvercast_Texture.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Sky_Material_Ambient_Fog.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Sun_Disk.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Sun_Centered_Gradient.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Sun_Shine_Edges.uasset", "material"),
    Target(
        "celestial",
        "Materials/Material_Functions/Scale_Intensity_Around_Sun.uasset",
        "material",
    ),
    Target("celestial", "Materials/Material_Functions/Moon.uasset", "material"),
    Target("celestial", "Materials/Material_Functions/Moon_Centered_Gradient.uasset", "material"),
    Target(
        "celestial",
        "Materials/Material_Functions/Sky_Utilities/Active_Sun_or_Moon_Vector.uasset",
        "material",
    ),
    Target("celestial", "Materials/Color_Curves/Sun_Disk_Color.uasset", "auto"),
    Target("celestial", "Materials/Color_Curves/Sun_Light_Color.uasset", "auto"),
    Target("celestial", "Materials/Float_Curves/Directional_Light_Intensity.uasset", "auto"),
    Target("celestial", "Materials/Float_Curves/Shine_Intensity.uasset", "auto"),
    Target("celestial", "Materials/Float_Curves/Sun_Highlight_Intensity.uasset", "auto"),
    Target("celestial", "Materials/Float_Curves/Sun_Highlight_Radius.uasset", "auto"),
    Target("celestial", "Materials/Float_Curves/Skyatmosphere_Density.uasset", "auto"),
    # Direct non-visual inputs of Approximate Real Sun Moon and Stars. The curve supplies the
    # authored equation-of-time correction and the calendar owns month/day/leap-year tables plus
    # the winter-solstice offset. Neither may be replaced with generic astronomical constants.
    Target("celestial", "Materials/Float_Curves/Equation_of_Time.uasset", "auto"),
    Target(
        "celestial",
        "Blueprints/System/Calendars/Gregorian_Calendar.uasset",
        "auto",
    ),
    Target("celestial", "Blueprints/System/UDS_Calendar.uasset", "blueprint"),
    Target("celestial", "Materials/Float_Curves/Exposure_Compensation_Curve.uasset", "auto"),
    Target(
        "celestial",
        "Materials/Float_Curves/Exposure_Compensation_Curve_Physical.uasset",
        "auto",
    ),

    # Volumetric cloud density, mobile sheets and material-function control graph.
    Target("clouds", "Textures/3D_Clouds/3DCells_128.uasset", "auto", "texture", ".npy"),
    Target("clouds", "Textures/3D_Clouds/3D_Cells_64.uasset", "auto", "texture", ".npy"),
    Target("clouds", "Textures/3D_Clouds/3D_Cells_32.uasset", "auto", "texture", ".npy"),
    # Ultra_Dynamic_Sky CDO overrides the conservative-density function's small default
    # volume with this authored formation field. It is the large cloud-mass source; the
    # 3D_Cells textures above remain the separate high-frequency erosion field.
    Target("clouds", "Textures/3D_Clouds/FormationVolume.uasset", "auto", "texture", ".npy"),
    Target("clouds", "Textures/3D_Clouds/3DCells_128_Sheet.uasset", "auto", "texture", ".png"),
    Target("clouds", "Textures/3D_Clouds/3DCells_64_Sheet.uasset", "auto", "texture", ".png"),
    Target(
        "clouds",
        "Textures/Volumetric_Clouds/Cloud_Profile.uasset",
        "auto",
        "texture",
        ".png",
    ),
    Target("clouds", "Textures/Sky/Cloud_Wisps.uasset", "auto", "texture", ".png"),
    Target("clouds", "Materials/Volumetric_Clouds.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Cloud_Wisps.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Cloud_Layer.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Shading_Gradients.uasset", "material"),
    Target(
        "clouds",
        "Materials/Material_Functions/Scale_Radial_Gradient_Around_White.uasset",
        "material",
    ),
    Target("clouds", "Materials/Material_Functions/Cloud_Distribution.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Map_Cloud_Textures.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Light_and_Dark_Cloud_Colors.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/SC_DirectionalScattering.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Composite_Cloud_Layers.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Cloud_UVs.uasset", "material"),
    Target("clouds", "Materials/Material_Functions/Filter_Clouds.uasset", "material"),
    Target(
        "clouds",
        "Materials/Material_Functions/Volumetric_Clouds_Conservative_Density.uasset",
        "material",
    ),
    Target(
        "clouds",
        "Materials/Material_Functions/Volumetric_Clouds_Extinction.uasset",
        "material",
    ),
    Target(
        "clouds",
        "Materials/Material_Functions/UDS_VolumetricClouds_MPC.uasset",
        "material",
    ),

    # Aurora source and graph. No panning substitute is accepted as the final contract.
    Target("aurora", "Textures/Clouds/Aurora_Clouds.uasset", "auto", "texture", ".png"),
    Target("aurora", "Materials/Material_Functions/Aurora.uasset", "material"),
    Target("aurora", "Materials/Volumetric_Aurora.uasset", "material"),

    # Niagara systems and every texture explicitly needed by precipitation/lightning/surface FX.
    Target("weather", "Particles/Rain.uasset", "niagara"),
    Target("weather", "Particles/Snow.uasset", "niagara"),
    Target("weather", "Particles/Dust.uasset", "niagara"),
    Target("weather", "Particles/Lightning_Strike.uasset", "niagara"),
    Target("weather", "Particles/Obscured_Lightning.uasset", "niagara"),
    Target("weather", "Particles/Puddle_Splash.uasset", "niagara"),
    Target("weather", "Particles/VolumetricCloud_LightRays.uasset", "niagara"),
    Target("weather", "Textures/Weather/RainSnow_Sheet.uasset", "auto", "texture", ".png"),
    Target("weather", "Textures/Weather/Dust_Alpha.uasset", "auto", "texture", ".png"),
    Target(
        "weather",
        "Textures/Weather/ObscuredLightningSheet.uasset",
        "auto",
        "texture",
        ".png",
    ),
    Target(
        "weather",
        "Textures/Weather/PuddleSplashParticle.uasset",
        "auto",
        "texture",
        ".png",
    ),

    # Surface accumulation/melting inputs remain distinct from airborne Niagara.
    Target("surface", "Materials/Weather/Surface_Weather_Effects.uasset", "material"),
    # Direct dependency closure of Surface_Weather_Effects.
    Target("surface", "Materials/Material_Functions/Map_Puddle_Ripples.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Basic_SubUV_Animation.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Sample_WOV_Target.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Sample_Weather_Mask_Brushes.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Triplanar_Texture_Mapping.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Water_Level_Mask.uasset", "material"),
    Target("surface", "Materials/Material_Functions/Water_Level_Local.uasset", "material"),
    Target("surface", "Materials/Weather/Snow_Sparkle.uasset", "material"),
    Target("surface", "Textures/Weather/Rough_Snow.uasset", "auto", "texture", ".png"),
    Target("surface", "Textures/Weather/Snow_Normal.uasset", "auto", "texture", ".png"),
    Target(
        "surface",
        "Textures/Weather/Windswept_Snow_Normal.uasset",
        "auto",
        "texture",
        ".png",
    ),

    # UDW owns the wind/weather dependency graph; UDS owns sky/celestial control.
    Target("wind", "Blueprints/Ultra_Dynamic_Weather.uasset", "blueprint"),
    Target("control", "Blueprints/Ultra_Dynamic_Sky.uasset", "blueprint"),
    # Direct Blueprint dependency of Get Inverted Global Occlusion. It owns the exact
    # Current Global Occlusion behavior used by Sun/interior-light evaluation.
    Target("control", "Blueprints/System/UDS_PlayerOcclusion.uasset", "blueprint"),

    # Exact loop payload and MetaSound/graph references are resolved from this owned package.
    Target("audio", "Sound/Rain/CloseRainLoop.uasset", "auto", "audio", ".wav"),
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, ensure_ascii=False, sort_keys=False) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def validate_target_table() -> dict[str, Any]:
    errors: list[str] = []
    keys: set[str] = set()
    paths: set[str] = set()
    for target in TARGETS:
        path = PurePosixPath(target.relative_path)
        if target.group not in GROUPS:
            errors.append(f"unknown_group:{target.group}:{target.relative_path}")
        if path.is_absolute() or ".." in path.parts or path.suffix != ".uasset":
            errors.append(f"unsafe_relative_path:{target.relative_path}")
        if target.key in keys:
            errors.append(f"duplicate_key:{target.key}")
        if target.relative_path in paths:
            errors.append(f"duplicate_path:{target.relative_path}")
        keys.add(target.key)
        paths.add(target.relative_path)
        if target.media == "texture" and target.output_suffix not in (".png", ".hdr", ".npy"):
            errors.append(f"invalid_texture_suffix:{target.relative_path}")
        if target.media == "audio" and target.output_suffix != ".wav":
            errors.append(f"invalid_audio_suffix:{target.relative_path}")
        if target.contract not in ("auto", "material", "niagara", "blueprint", "metasound"):
            errors.append(f"invalid_contract:{target.relative_path}:{target.contract}")
    return {
        "status": "PASS" if not errors else "FAIL",
        "targetCount": len(TARGETS),
        "groups": {group: sum(target.group == group for target in TARGETS) for group in GROUPS},
        "errors": errors,
    }


def export_contract(target: Target, source: Path) -> dict[str, Any]:
    if target.contract == "material":
        return export_material_contract(source)
    if target.contract == "niagara":
        return export_niagara_contract(source)
    if target.contract == "blueprint":
        return export_blueprint_contract(source)
    if target.contract == "metasound":
        return export_metasound_contract(source)
    return export_auto_contract(source)


def selected_targets(
    groups: set[str], relative_paths: set[str]
) -> tuple[Target, ...]:
    return tuple(
        target
        for target in TARGETS
        if (not groups or target.group in groups)
        and (not relative_paths or target.relative_path in relative_paths)
    )


def resolve_source(root: Path, target: Target) -> Path:
    resolved_root = root.resolve()
    source = (resolved_root / target.relative_path).resolve()
    if not source.is_relative_to(resolved_root):
        raise ValueError(f"target leaves UDS root: {target.relative_path}")
    return source


def run_target(
    target: Target,
    root: Path,
    dataset: Path,
    *,
    export_media_payloads: bool,
    max_output: int,
) -> dict[str, Any]:
    source = resolve_source(root, target)
    record: dict[str, Any] = {
        "group": target.group,
        "relativePath": target.relative_path,
        "required": target.required,
        "contractKind": target.contract,
        "mediaKind": target.media,
        "status": "PENDING",
        "licenseBoundary": "PRIVATE_PAID_OWNED_SOURCE_DO_NOT_REDISTRIBUTE",
    }
    if not source.is_file():
        record.update(status="MISSING_SOURCE", reason="owned UDS package is absent")
        return record
    started = time.monotonic()
    try:
        source_size = source.stat().st_size
        source_hash = sha256(source)
        record["source"] = {
            "path": str(source),
            "size": source_size,
            "sha256": source_hash,
        }
        verify = verify_package(source)
        verify_path = dataset / "verify" / f"{target.key}.json"
        write_json(verify_path, verify)
        record["verify"] = {
            "path": str(verify_path),
            "status": verify.get("status"),
        }
        contract_path = dataset / "contracts" / f"{target.key}.{target.contract}.json"
        contract: dict[str, Any] | None = None
        if contract_path.is_file():
            cached_contract = json.loads(contract_path.read_text(encoding="utf-8"))
            if (
                cached_contract.get("status") in CONTRACT_PASS_STATUSES
                and cached_contract.get("source", {}).get("sha256") == source_hash
            ):
                contract = cached_contract
                record["reusedExactContract"] = True
        if contract is None:
            contract = export_contract(target, source)
            write_json(contract_path, contract)
        record["contract"] = {
            "path": str(contract_path),
            "status": contract.get("status"),
            "sha256": sha256(contract_path),
        }
        media_ok = True
        if target.media:
            suffix = target.output_suffix or ""
            output_path = dataset / "exports" / target.group / f"{target.key}{suffix}"
            media_path = dataset / "media" / f"{target.key}.{target.media}.json"
            media_contract: dict[str, Any] | None = None
            if output_path.is_file() and media_path.is_file():
                cached = json.loads(media_path.read_text(encoding="utf-8"))
                cached_output = cached.get("output", {})
                if (
                    cached.get("status") == "VERIFIED"
                    and cached.get("source", {}).get("sha256") == source_hash
                    and cached_output.get("sha256") == sha256(output_path)
                ):
                    media_contract = cached
                    record["reusedExactExport"] = True
            if media_contract is None and export_media_payloads:
                media_contract = export_media(
                    source,
                    output_path,
                    kind=target.media,
                    max_output=max_output,
                )
                write_json(media_path, media_contract)
            if media_contract is None:
                media_ok = False
                record["media"] = {"status": "NOT_REQUESTED"}
            else:
                media_ok = media_contract.get("status") == "VERIFIED" and output_path.is_file()
                record["media"] = {
                    "path": str(media_path),
                    "status": media_contract.get("status"),
                    "output": str(output_path),
                    "outputSize": output_path.stat().st_size if output_path.is_file() else None,
                    "outputSha256": sha256(output_path) if output_path.is_file() else None,
                }
        verify_ok = verify.get("status") == "VERIFIED"
        contract_ok = contract.get("status") in CONTRACT_PASS_STATUSES
        record["status"] = (
            "VERIFIED"
            if verify_ok and contract_ok and media_ok
            else "FAILED_GATE"
        )
    except (UEAssetError, OSError, ValueError) as error:
        record.update(
            status="EXTRACTION_ERROR",
            reason=f"{type(error).__name__}: {error}",
        )
    record["elapsedSeconds"] = round(time.monotonic() - started, 3)
    return record


def build_gate(
    root: Path,
    dataset: Path,
    targets: tuple[Target, ...],
    records: list[dict[str, Any]],
    *,
    export_media_payloads: bool,
) -> dict[str, Any]:
    required_failures = [
        record["relativePath"]
        for record in records
        if record.get("required") and record.get("status") != "VERIFIED"
    ]
    media_not_exported = [
        target.relative_path
        for target, record in zip(targets, records)
        if target.media and record.get("media", {}).get("status") == "NOT_REQUESTED"
    ]
    status = "PASS" if not required_failures and not media_not_exported else "FAIL"
    return {
        "schema": SCHEMA,
        "status": status,
        "udsRoot": str(root.resolve()),
        "dataset": str(dataset.resolve()),
        "privatePayloadPolicy": "DO_NOT_COMMIT_OR_REDISTRIBUTE",
        "exactOnly": True,
        "placeholderFallback": "FORBIDDEN",
        "mediaExportRequested": export_media_payloads,
        "selectedTargetCount": len(targets),
        "verifiedTargetCount": sum(record.get("status") == "VERIFIED" for record in records),
        "requiredFailures": required_failures,
        "mediaNotExported": media_not_exported,
        "groups": {
            group: {
                "selected": sum(target.group == group for target in targets),
                "verified": sum(
                    record.get("group") == group and record.get("status") == "VERIFIED"
                    for record in records
                ),
            }
            for group in GROUPS
            if any(target.group == group for target in targets)
        },
        "records": records,
    }


def cached_record_is_current(
    record: dict[str, Any], target: Target, root: Path, dataset: Path
) -> bool:
    source = resolve_source(root, target)
    source_record = record.get("source", {})
    contract_record = record.get("contract", {})
    if (
        record.get("relativePath") != target.relative_path
        or not source.is_file()
        or source_record.get("sha256") != sha256(source)
        or source_record.get("size") != source.stat().st_size
    ):
        return False
    contract_path_text = contract_record.get("path")
    if not contract_path_text:
        return False
    contract_path = Path(contract_path_text).resolve()
    if (
        not contract_path.is_relative_to(dataset.resolve())
        or not contract_path.is_file()
        or contract_record.get("sha256") != sha256(contract_path)
    ):
        return False
    if record.get("status") == "VERIFIED" and target.media:
        media = record.get("media", {})
        output_text = media.get("output")
        if not output_text:
            return False
        output = Path(output_text).resolve()
        if (
            media.get("status") != "VERIFIED"
            or not output.is_relative_to(dataset.resolve())
            or not output.is_file()
            or media.get("outputSha256") != sha256(output)
        ):
            return False
    return True


def compose_cached_gate(root: Path, dataset: Path) -> dict[str, Any]:
    """Rebuild the canonical inventory without loading every large contract in one process.

    Each reused record is accepted only after its owned source, in-repo contract, and any VERIFIED
    media output still match the recorded SHA-256. Failed Oodle records remain failed.
    """
    gate_paths = [dataset / "P63_10_EXTRACTION_GATE.json"]
    gate_paths.extend(sorted((dataset / "gates").glob("P63_10_EXTRACTION_GATE_*.json")))
    candidates: dict[str, list[dict[str, Any]]] = {}
    for gate_path in gate_paths:
        if not gate_path.is_file():
            continue
        gate = json.loads(gate_path.read_text(encoding="utf-8"))
        for record in gate.get("records", []):
            candidates.setdefault(record.get("relativePath", ""), []).append(record)
    records = []
    missing = []
    for target in TARGETS:
        current = next(
            (
                record
                for record in reversed(candidates.get(target.relative_path, []))
                if cached_record_is_current(record, target, root, dataset)
            ),
            None,
        )
        if current is None:
            missing.append(target.relative_path)
        else:
            records.append(current)
    if missing:
        raise ValueError(f"no current exact cached record for: {missing}")
    gate = build_gate(
        root,
        dataset,
        TARGETS,
        records,
        export_media_payloads=True,
    )
    gate["cacheComposition"] = {
        "status": "VERIFIED",
        "method": "source-contract-media-sha256",
        "recordCount": len(records),
        "failedRecordsPreserved": sum(record.get("status") != "VERIFIED" for record in records),
    }
    return gate


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--root", type=Path, help="owned Ultra Dynamic Sky content root")
    result.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    result.add_argument("--group", action="append", choices=GROUPS, default=[])
    result.add_argument(
        "--target",
        action="append",
        choices=tuple(target.relative_path for target in TARGETS),
        default=[],
        help="extract only an exact fixed-inventory relative path; repeatable",
    )
    result.add_argument("--export-media", action="store_true")
    result.add_argument("--max-output", type=int, default=2 * 1024 * 1024 * 1024)
    result.add_argument(
        "--oodle-helper",
        type=Path,
        help="explicit executable licensed Oodle block decoder for exact FCompressedBuffer payloads",
    )
    result.add_argument("--list", action="store_true", help="print the fixed target inventory")
    result.add_argument("--self-test", action="store_true", help="validate target invariants only")
    result.add_argument(
        "--compose-cache",
        action="store_true",
        help="rebuild the full gate from SHA-verified per-target records without heavy re-decode",
    )
    return result


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    table_gate = validate_target_table()
    if args.self_test:
        print(json.dumps(table_gate, ensure_ascii=False, sort_keys=True))
        return 0 if table_gate["status"] == "PASS" else 2
    if args.list:
        print(json.dumps(
            {
                "schema": SCHEMA,
                "tableGate": table_gate,
                "targets": [target.__dict__ for target in TARGETS],
            },
            indent=2,
            ensure_ascii=False,
        ))
        return 0 if table_gate["status"] == "PASS" else 2
    if table_gate["status"] != "PASS":
        print(json.dumps(table_gate, ensure_ascii=False), file=sys.stderr)
        return 2
    if args.root is None or not args.root.is_dir():
        print("P63_10_UDS_EXTRACTION=FAIL reason=--root must be an owned UDS directory", file=sys.stderr)
        return 2
    if args.max_output <= 0:
        print("P63_10_UDS_EXTRACTION=FAIL reason=--max-output must be positive", file=sys.stderr)
        return 2
    if args.oodle_helper is not None:
        helper = args.oodle_helper.resolve()
        if not helper.is_file() or not os.access(helper, os.X_OK):
            print(
                "P63_10_UDS_EXTRACTION=FAIL reason=--oodle-helper must be executable",
                file=sys.stderr,
            )
            return 2
        os.environ["UEASSET_OODLE_HELPER"] = str(helper)
    dataset = args.dataset.resolve()
    if not dataset.is_relative_to(ROOT):
        print("P63_10_UDS_EXTRACTION=FAIL reason=dataset must stay inside the repo", file=sys.stderr)
        return 2
    if args.compose_cache:
        try:
            gate = compose_cached_gate(args.root, dataset)
        except (OSError, ValueError) as error:
            print(
                f"P63_10_UDS_EXTRACTION=FAIL reason=cache composition: {error}",
                file=sys.stderr,
            )
            return 2
        write_json(dataset / "P63_10_EXTRACTION_GATE.json", gate)
        write_json(dataset / "gates/P63_10_EXTRACTION_GATE_all.json", gate)
        write_json(dataset / "TARGET_TABLE_GATE.json", table_gate)
        print(
            f"P63_10_UDS_EXTRACTION={gate['status']} "
            f"verified={gate['verifiedTargetCount']}/{gate['selectedTargetCount']} "
            "composition=VERIFIED"
        )
        return 0 if gate["status"] == "PASS" else 3
    targets = selected_targets(set(args.group), set(args.target))
    if not targets:
        print("P63_10_UDS_EXTRACTION=FAIL reason=empty target selection", file=sys.stderr)
        return 2
    records = [
        run_target(
            target,
            args.root,
            dataset,
            export_media_payloads=args.export_media,
            max_output=args.max_output,
        )
        for target in targets
    ]
    gate = build_gate(
        args.root,
        dataset,
        targets,
        records,
        export_media_payloads=args.export_media,
    )
    if args.target:
        selection_name = "target_" + hashlib.sha256(
            "\n".join(sorted(set(args.target))).encode("utf-8")
        ).hexdigest()[:16]
    else:
        selection_name = "all" if not args.group else "_".join(sorted(set(args.group)))
    selection_gate = dataset / "gates" / f"P63_10_EXTRACTION_GATE_{selection_name}.json"
    write_json(selection_gate, gate)
    if len(targets) == len(TARGETS):
        latest_gate = dataset / "P63_10_EXTRACTION_GATE.json"
        write_json(latest_gate, gate)
    write_json(dataset / "TARGET_TABLE_GATE.json", table_gate)
    print(
        f"P63_10_UDS_EXTRACTION={gate['status']} "
        f"verified={gate['verifiedTargetCount']}/{gate['selectedTargetCount']} "
        f"gate={selection_gate}"
    )
    return 0 if gate["status"] == "PASS" else 3


if __name__ == "__main__":
    raise SystemExit(main())
