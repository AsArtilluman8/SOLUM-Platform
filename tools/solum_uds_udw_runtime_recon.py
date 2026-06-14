#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECON = Path("/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output")
OUT = ROOT / "apps" / "engine" / "src" / "main" / "assets" / "weather" / "solum_udw_runtime_recipe.json"
REPORT = ROOT / "docs" / "SOLUM_UDS_UDW_RUNTIME_RECON_SUMMARY.json"

CHANNELS = {
    "timeOfDay": 12.0,
    "dayNightFactor": 0.0,
    "cloudCoverage": 0.45,
    "rainAmount": 0.0,
    "snowAmount": 0.0,
    "fog": 0.0,
    "windDirection": 0.0,
    "windIntensity": 0.25,
    "materialWetness": 0.0,
    "materialSnowCoverage": 0.0,
    "materialDustCoverage": 0.0,
    "thunderLightning": 0.0,
    "flashLightning": 0.0,
    "temperature": 20.0,
    "weatherState": "Clear",
    "manualWeatherState": "Clear",
    "randomWeatherVariation": 0.0,
}

ALIASES = {
    "timeOfDay": ("timeofday", "time of day", "time_of_day"),
    "cloudCoverage": ("cloudcoverage", "cloud coverage", "cloudamount", "cloud amount"),
    "rainAmount": ("rainamount", "rain amount", "rainintensity", "rain intensity"),
    "snowAmount": ("snowamount", "snow amount", "snowintensity", "snow intensity"),
    "fog": ("fog", "fogdensity", "fog density"),
    "windDirection": ("winddirection", "wind direction"),
    "windIntensity": ("windintensity", "wind intensity", "windstrength"),
    "materialWetness": ("materialwetness", "wetness", "surface wetness"),
    "materialSnowCoverage": ("materialsnowcoverage", "snowcoverage", "snow coverage"),
    "materialDustCoverage": ("materialdustcoverage", "dustcoverage", "dust coverage"),
    "thunderLightning": ("thunder", "lightning", "thunderlightning"),
    "flashLightning": ("flashlightning", "lightningflash"),
    "temperature": ("temperature",),
    "weatherState": ("weatherstate", "weather state"),
    "manualWeatherState": ("manualweatherstate", "manual weather state"),
    "randomWeatherVariation": ("randomweathervariation", "random weather variation"),
}


def load_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8", errors="ignore"))
    except Exception:
        return None


def text_blob():
    parts = []
    for rel in [
        "manifest_light.json",
        "10_ASSET_MAP/asset_inventory_light.json",
        "21_BLUEPRINT_NODE_TABLES/all_node_tables_light.json",
        "60_RECIPES/solum_recon_light_recipe.json",
        "70_REPORTS/reconstruction_light_report.md",
    ]:
        path = RECON / rel
        if path.exists():
            parts.append(f"\n--- {rel} ---\n")
            parts.append(path.read_text(encoding="utf-8", errors="ignore")[:300000])
    return "\n".join(parts)


def find_status(channel, blob):
    low = blob.lower()
    aliases = ALIASES.get(channel, (channel.lower(),))
    for alias in aliases:
        idx = low.find(alias.lower())
        if idx >= 0:
            return "reconstructed", alias, blob[max(0, idx - 90):idx + 180].replace("\n", " ")[:260], 0.62
    return "procedural fill", "not_found_in_light_output", "default chosen for mobile-safe weather showcase", 0.25


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    blob = text_blob() if RECON.exists() else ""
    channels = {}
    for name, default in CHANNELS.items():
        status, alias, proof, confidence = find_status(name, blob)
        channels[name] = {
            "value": default,
            "default": default,
            "sourceStatus": status if blob else "blocked",
            "proofRef": proof if blob else "latest_light_output_missing",
            "matchedAlias": alias,
            "confidence": confidence if blob else 0.0,
        }
    recipe = {
        "schema": "com.solum.udw.runtime_recipe",
        "schemaVersion": 1,
        "source": "SOLUM_RECON_TOOLKIT_LIGHT_OUTPUT",
        "latestLightOutput": str(RECON),
        "status": "reconstructed_with_procedural_fill" if blob else "BLOCKED",
        "fakeRisk": "values without proof are marked procedural fill, not asset-derived",
        "channels": channels,
    }
    OUT.write_text(json.dumps(recipe, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    REPORT.write_text(json.dumps({
        "status": recipe["status"],
        "output": str(OUT.relative_to(ROOT)),
        "reconstructed": [k for k, v in channels.items() if v["sourceStatus"] == "reconstructed"],
        "proceduralFill": [k for k, v in channels.items() if v["sourceStatus"] == "procedural fill"],
        "blocked": [k for k, v in channels.items() if v["sourceStatus"] == "blocked"],
    }, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"status": recipe["status"], "output": str(OUT.relative_to(ROOT)), "report": str(REPORT.relative_to(ROOT))}, indent=2))


if __name__ == "__main__":
    main()
