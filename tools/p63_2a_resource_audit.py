#!/usr/bin/env python3
"""Audit local P63.2A candidates and prepare ignored premium runtime assets."""

from __future__ import annotations

import hashlib
import importlib.util
import json
import os
import shutil
import struct
import sys
import wave
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "apps/engine/src/main/assets/env/p63/P63_2A_RESOURCE_MANIFEST.json"
AUDIO_OUT = ROOT / "apps/engine/src/main/assets/env/p63/P63_2A_VERIFIED_AUDIO_MANIFEST.json"
PRIVATE = ROOT / "apps/engine/src/main/assets/private_premium/p63_2a"
VERIFIED_AUDIO = Path("/storage/emulated/0/SOLUMCreative/reports/latest/P63_1_VERIFIED_AUDIO_MANIFEST.json")
MOON_COLOR = Path("/mnt/shared/Download/SOLUM_UDS_TRUTH_SAFE_MOON_STARS_P64AD/assets/textures/moon_color.png")
MOON_NORMAL = Path("/mnt/shared/Download/SOLUM_UDS_TRUTH_SAFE_MOON_STARS_P64AD/assets/textures/moon_normal.png")
AUDIT_ROOTS = (Path("/mnt/shared/Download"), Path("/storage/emulated/0/SOLUMCreative"))
EXTENSIONS = {".png", ".jpg", ".jpeg", ".hdr", ".exr", ".ktx", ".ktx2", ".dds", ".wav", ".ogg"}
TOKENS = ("moon", "sun", "sky", "atmos", "star", "cloud", "mask", "noise", "flow", "lut",
          "thunder", "rain", "wind", "snow", "dust", "puddle", "water")
REQUIRED_AUDIO = (
    "CloseThunder_1", "CloseThunder_2", "CloseThunder_3", "CloseThunder_4", "CloseThunder_5", "CloseThunder_6",
    "LightRain_1", "LightRain_2", "MediumRain_1", "MediumRain_2", "RainHit_1", "RainHit_2", "RainHit_3",
    "Wind_Whistling", "Snow_Compress_1", "Snow_Compress_2", "Snow_Compress_3", "Snow_Compress_4",
    "Snow_Compress_5", "Snow_Compress_6", "Snow_Movement", "Dust_1", "Dust_2", "Dust_3", "Dust_4",
    "Dust_5", "Dust_6", "Puddle_01", "Puddle_02", "Puddle_03", "Puddle_04", "Water_Movement",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def image_dimensions(path: Path) -> tuple[int | None, int | None]:
    try:
        with path.open("rb") as stream:
            header = stream.read(32)
            if header.startswith(b"\x89PNG\r\n\x1a\n"):
                return struct.unpack(">II", header[16:24])
            if header[:2] == b"\xff\xd8":
                stream.seek(2)
                while True:
                    marker = stream.read(2)
                    if len(marker) != 2 or marker[0] != 0xFF:
                        break
                    length_raw = stream.read(2)
                    if len(length_raw) != 2:
                        break
                    length = struct.unpack(">H", length_raw)[0]
                    if marker[1] in {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}:
                        payload = stream.read(5)
                        return struct.unpack(">H", payload[3:5])[0], struct.unpack(">H", payload[1:3])[0]
                    stream.seek(max(0, length - 2), os.SEEK_CUR)
    except (OSError, struct.error):
        pass
    return None, None


def wav_metadata(path: Path) -> dict[str, object]:
    try:
        with wave.open(str(path), "rb") as wav:
            frames = wav.getnframes()
            rate = wav.getframerate()
            return {"durationSeconds": round(frames / max(1, rate), 6), "channels": wav.getnchannels(),
                    "sampleRate": rate, "bitsPerSample": wav.getsampwidth() * 8}
    except (OSError, wave.Error):
        return {"durationSeconds": None, "channels": None, "sampleRate": None, "bitsPerSample": None}


def classify(path: Path, digest: str, verified_by_hash: dict[str, dict[str, object]]) -> dict[str, object]:
    lower = path.name.lower()
    suffix = path.suffix.lower().lstrip(".")
    item: dict[str, object] = {"exactPath": str(path), "sha256": digest, "format": suffix}
    if path.suffix.lower() == ".wav":
        item.update(wav_metadata(path))
    else:
        width, height = image_dimensions(path)
        item.update({"width": width, "height": height})

    verified = verified_by_hash.get(digest)
    if verified:
        item.update({"semanticRole": verified.get("recommendedRole", "UNKNOWN"),
                     "confidence": verified.get("semanticConfidence", "HIGH"), "provenance": "UDS_VERIFIED",
                     "colorSpace": "N/A", "runtimeSuitability": bool(verified.get("formatVerified", False))})
    elif digest == "8a8ff79b0d06946bfd09efcada50cc4a9891076b7f2a00b49fa8e182bbb6e375":
        item.update({"semanticRole": "MOON_ALBEDO", "confidence": "HIGH", "provenance": "UDS_VERIFIED",
                     "colorSpace": "sRGB", "runtimeSuitability": True})
    elif digest == "1b0e0306afc8626bdf1e06f809c6f3fe01b3cb997234ca2653a5ef89af9a9998":
        item.update({"semanticRole": "MOON_PHASE_NORMAL_DETAIL", "confidence": "HIGH", "provenance": "UDS_VERIFIED",
                     "colorSpace": "LINEAR", "runtimeSuitability": True})
    else:
        technical = any(token in lower for token in ("mask", "noise", "flow", "lut", "normal", "packed"))
        role = "UNKNOWN_TECHNICAL_ASSET" if technical else "UNASSIGNED_" + next((t.upper() for t in TOKENS if t in lower), "ASSET")
        item.update({"semanticRole": role, "confidence": "LOW", "provenance": "UDS_DERIVED",
                     "colorSpace": "UNKNOWN", "runtimeSuitability": suffix in {"png", "jpg", "jpeg", "hdr", "ktx", "ktx2", "wav", "ogg"}})
    return item


def load_verified_audio() -> tuple[list[dict[str, object]], dict[str, dict[str, object]]]:
    if not VERIFIED_AUDIO.is_file():
        raise FileNotFoundError(f"verified audio manifest missing: {VERIFIED_AUDIO}")
    source = json.loads(VERIFIED_AUDIO.read_text(encoding="utf-8"))
    entries = source.get("entries", [])
    return entries, {str(entry.get("sha256")): entry for entry in entries if entry.get("sha256")}


def choose_audio(entries: list[dict[str, object]]) -> list[dict[str, object]]:
    selected: list[dict[str, object]] = []
    target = PRIVATE / "audio"
    target.mkdir(parents=True, exist_ok=True)
    for requested in REQUIRED_AUDIO:
        match = next((entry for entry in entries if any(requested.lower() in Path(path).name.lower()
                                                        for path in entry.get("exactFilePaths", []))), None)
        if match is None:
            selected.append({"verifiedFileName": requested, "status": "UNAVAILABLE", "provenance": "UNAVAILABLE"})
            continue
        source_path = next((Path(path) for path in match.get("exactFilePaths", [])
                            if "/13_solum_runtime_bundle/" in path and Path(path).is_file()), None)
        if source_path is None:
            source_path = next((Path(path) for path in match.get("exactFilePaths", []) if Path(path).is_file()), None)
        if source_path is None:
            selected.append({"verifiedFileName": requested, "status": "UNAVAILABLE", "provenance": "UNAVAILABLE"})
            continue
        asset_name = f"{requested}.wav"
        shutil.copy2(source_path, target / asset_name)
        selected.append({
            "verifiedFileName": requested, "exactSourcePath": str(source_path), "sha256": match["sha256"],
            "format": match.get("format"), "durationSeconds": match.get("durationSeconds"),
            "sampleRate": match.get("sampleRate"), "channels": match.get("channels"),
            "provenance": "UDS_VERIFIED", "semanticConfidence": match.get("semanticConfidence"),
            "semanticRole": match.get("recommendedRole"),
            "assetPath": f"private_premium/p63_2a/audio/{asset_name}", "status": "PACKAGED_LOCAL_PRIVATE",
        })
    return selected


def build_private_stage() -> None:
    if not MOON_COLOR.is_file() or not MOON_NORMAL.is_file():
        return
    target = PRIVATE / "celestial"
    target.mkdir(parents=True, exist_ok=True)
    shutil.copy2(MOON_COLOR, target / "Moon_Color.png")
    shutil.copy2(MOON_NORMAL, target / "Moon_PhaseNormal.png")
    module_path = ROOT / "tools/generate_p63_environment_assets.py"
    spec = importlib.util.spec_from_file_location("p63_generator", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError("cannot load P63 generator")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    payload = module.generate_celestial_glb(MOON_COLOR.read_bytes(), "P63_UDS_VERIFIED_MOON_COLOR")
    (target / "p63_2a_celestial_test_stage_uds.glb").write_bytes(payload)


def main() -> None:
    entries, verified_by_hash = load_verified_audio()
    candidates: set[Path] = set()
    for root in AUDIT_ROOTS:
        if not root.is_dir():
            continue
        for base, _, files in os.walk(root):
            for name in files:
                path = Path(base) / name
                lower = name.lower()
                if path.suffix.lower() in EXTENSIONS and any(token in lower for token in TOKENS):
                    candidates.add(path)
    audited = []
    for path in sorted(candidates, key=lambda value: str(value).lower()):
        try:
            audited.append(classify(path, sha256(path), verified_by_hash))
        except OSError as error:
            audited.append({"exactPath": str(path), "format": path.suffix.lower().lstrip("."), "sha256": None,
                            "semanticRole": "UNKNOWN", "confidence": "LOW", "runtimeSuitability": False,
                            "provenance": "UNKNOWN", "error": error.__class__.__name__})
    selected_audio = choose_audio(entries)
    build_private_stage()
    manifest = {
        "schema": "solum.p63_2a.resource_manifest", "schemaVersion": 1,
        "auditRoots": [str(path) for path in AUDIT_ROOTS], "fileCount": len(audited),
        "selectionPolicy": "technical masks remain unassigned; no generated LUT/flow/packed masks",
        "selectedMoon": {"exactPath": str(MOON_COLOR), "sha256": sha256(MOON_COLOR), "format": "png",
                         "dimensions": "256x256", "colorSpace": "sRGB", "semanticRole": "MOON_ALBEDO",
                         "confidence": "HIGH", "runtimeSuitability": True, "provenance": "UDS_VERIFIED",
                         "runtimeAsset": "private_premium/p63_2a/celestial/p63_2a_celestial_test_stage_uds.glb"},
        "selectedMoonDetail": {"exactPath": str(MOON_NORMAL), "sha256": sha256(MOON_NORMAL), "format": "png",
                               "dimensions": "256x256", "colorSpace": "LINEAR", "semanticRole": "MOON_PHASE_NORMAL_DETAIL",
                               "confidence": "HIGH", "runtimeSuitability": True, "provenance": "UDS_VERIFIED",
                               "runtimeUsage": "AUDITED_NOT_BOUND_NO_TANGENT_SAFE_PATH"},
        "verifiedAudioManifest": str(VERIFIED_AUDIO), "verifiedAudio": selected_audio, "files": audited,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    AUDIO_OUT.write_text(json.dumps({
        "schema": "solum.p63_2a.verified_audio_runtime", "schemaVersion": 1,
        "proceduralAudioDefault": False, "longLoopStatus": "NO_VERIFIED_LONG_LOOP",
        "sourceManifest": str(VERIFIED_AUDIO), "entries": selected_audio,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": "OK", "audited": len(audited), "audio": len(selected_audio),
                      "manifest": str(OUT), "private": str(PRIVATE)}))


if __name__ == "__main__":
    main()
