#!/usr/bin/env python3
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECON = Path("/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output")
TOOLKIT = Path("/data/data/com.termux/files/home/SOLUM_RECON_TOOLKIT_MASTER/solum_ue_recon_toolkit_light_v1.py")
OUT_JSON = ROOT / "docs" / "SOLUM_WEATHER_FAST_INVENTORY.json"
OUT_MD = ROOT / "docs" / "SOLUM_WEATHER_FAST_INVENTORY.md"

SCAN_ROOTS = [
    ROOT / "apps" / "engine" / "src" / "main" / "java",
    ROOT / "apps" / "engine" / "src" / "main" / "assets",
    ROOT / "engine-core",
    ROOT / "assets",
]
EXTS = {".java", ".kt", ".kts", ".cpp", ".hpp", ".h", ".c", ".glsl", ".frag", ".vert", ".json", ".png", ".jpg", ".jpeg", ".wav", ".ogg", ".txt", ".md"}
WEATHER_TOKENS = ("weather", "uds", "udw", "sky", "cloud", "rain", "snow", "fog", "wind", "lightning", "thunder", "moon", "sun", "stars")
LEGACY_TOKENS = ("weatheralpha", "weather_v43", "weather_v44", "weather_v45", "opengl", "gles")
FILAMENT_TOKENS = ("filament", "modelviewer", "environmentcontroller", "weatherruntimeparameters", "weathervfxrecipe")


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def collect_files():
    weather = []
    legacy = []
    filament = []
    for base in SCAN_ROOTS:
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in EXTS:
                continue
            low = rel(path).lower()
            if any(t in low for t in WEATHER_TOKENS):
                weather.append(rel(path))
            if any(t in low for t in LEGACY_TOKENS):
                legacy.append(rel(path))
            if any(t in low for t in FILAMENT_TOKENS):
                filament.append(rel(path))
    return sorted(weather), sorted(legacy), sorted(filament)


def recon_presence():
    key_files = [
        "manifest_light.json",
        "10_ASSET_MAP/asset_inventory_light.json",
        "21_BLUEPRINT_NODE_TABLES/all_node_tables_light.json",
        "60_RECIPES/solum_recon_light_recipe.json",
        "70_REPORTS/reconstruction_light_report.md",
    ]
    return {
        "toolkit_script": str(TOOLKIT),
        "toolkit_script_exists": TOOLKIT.exists(),
        "latest_output": str(RECON),
        "latest_output_exists": RECON.exists(),
        "key_files": {name: (RECON / name).exists() for name in key_files},
    }


def unreal_access():
    try:
        result = subprocess.run(
            ["gh", "repo", "view", "EpicGames/UnrealEngine", "--json", "nameWithOwner,visibility,defaultBranchRef"],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=15,
        )
        if result.returncode != 0:
            return {"status": "BLOCKED", "error": result.stderr.strip()[:500]}
        data = json.loads(result.stdout)
        return {
            "status": "OK",
            "nameWithOwner": data.get("nameWithOwner"),
            "visibility": data.get("visibility"),
            "defaultBranch": (data.get("defaultBranchRef") or {}).get("name"),
        }
    except Exception as exc:
        return {"status": "BLOCKED", "error": str(exc)}


def main():
    weather, legacy, filament = collect_files()
    summary = {
        "schema": "com.solum.weather.fast_inventory",
        "schemaVersion": 1,
        "weatherRelatedCount": len(weather),
        "legacyCandidateCount": len(legacy),
        "currentFilamentCandidateCount": len(filament),
        "weatherRelatedFiles": weather,
        "legacyCandidates": legacy,
        "currentFilamentRuntimeCandidates": filament,
        "reconToolkit": recon_presence(),
        "unrealAccess": unreal_access(),
        "reports": {
            "json": rel(OUT_JSON),
            "markdown": rel(OUT_MD),
        },
    }
    OUT_JSON.write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    lines = [
        "# SOLUM Weather Fast Inventory",
        "",
        f"- weatherRelatedCount: {len(weather)}",
        f"- legacyCandidateCount: {len(legacy)}",
        f"- currentFilamentCandidateCount: {len(filament)}",
        f"- reconLatestOutput: {summary['reconToolkit']['latest_output_exists']}",
        f"- unrealAccess: {summary['unrealAccess'].get('status')}",
        "",
        "## Legacy Candidates",
        *[f"- `{item}`" for item in legacy[:80]],
        "",
        "## Current Filament Runtime Candidates",
        *[f"- `{item}`" for item in filament[:80]],
    ]
    OUT_MD.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({
        "weatherRelatedCount": len(weather),
        "legacyCandidateCount": len(legacy),
        "currentFilamentCandidateCount": len(filament),
        "reconLatestOutput": summary["reconToolkit"]["latest_output_exists"],
        "unrealAccess": summary["unrealAccess"].get("status"),
        "json": rel(OUT_JSON),
        "markdown": rel(OUT_MD),
    }, indent=2))


if __name__ == "__main__":
    main()
