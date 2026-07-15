from __future__ import annotations

import importlib.util
import json
import shutil
import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
TOOL = ROOT / "tools" / "uds_visual"


def load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    sys.path.insert(0, str(path.parent))
    try:
        spec.loader.exec_module(module)
    finally:
        sys.path.pop(0)
    return module


builder = load("p62b_builder", TOOL / "build_solum_environment.py")


class SolumEnvironmentTests(unittest.TestCase):
    def shared(self) -> dict[str, float]:
        return {
            "cloudHeight": 15.0,
            "baseFogDensity": 0.0055,
            "baseFogFalloff": 0.065,
            "foggyDensityContribution": 0.1,
            "foggyFogFalloff": 0.015,
            "dustyFogFalloff": 0.03,
            "windDirection": 180.0,
            "windGust": 0.45,
        }

    def test_verified_weather_values_map_to_runtime(self) -> None:
        preset = {
            "id": "Rain_Thunderstorm",
            "values": {
                "Cloud Coverage": 8.0, "Fog": 6.5, "Rain": 10.0,
                "Wind Intensity": 10.0, "Thunder/Lightning": 10.0,
                "Material Wetness": 1.0,
            },
        }
        runtime, provenance = builder.preset_runtime(preset, self.shared())
        self.assertEqual(runtime["cloudCoverage"], 0.8)
        self.assertEqual(runtime["rain"], 1.0)
        self.assertEqual(runtime["lightningEnabled"], 1.0)
        self.assertEqual(runtime["windDirectionDeg"], 180.0)
        self.assertEqual(provenance["rain"]["status"], "UDS_DERIVED_MAPPING")
        self.assertEqual(set(runtime), set(provenance))

    def test_non_storm_lightning_is_not_enabled(self) -> None:
        preset = {"id": "Rain", "values": {"Thunder/Lightning": 4.0, "Rain": 7.0, "Wind Intensity": 3.0}}
        runtime, _provenance = builder.preset_runtime(preset, self.shared())
        self.assertEqual(runtime["lightningPotential"], 0.4)
        self.assertEqual(runtime["lightningEnabled"], 0.0)

    def test_verified_cloud_curve_drives_cloud_profile(self) -> None:
        curve = {"channels": [
            {"name": "x", "keys": [{"time": 0.0, "value": 1.0}, {"time": 10.0, "value": 0.0}]},
            {"name": "y", "keys": [{"time": 0.0, "value": 0.0}, {"time": 5.0, "value": 1.0}, {"time": 10.0, "value": 0.0}]},
            {"name": "z", "keys": [{"time": 0.0, "value": 0.0}, {"time": 10.0, "value": 1.0}]},
        ]}
        runtime, provenance = builder.preset_runtime(
            {"id": "Cloudy", "values": {"Cloud Coverage": 5.0}}, self.shared(), curve,
        )
        self.assertEqual(runtime["cloudProfileLow"], 0.5)
        self.assertEqual(runtime["cloudProfileMid"], 1.0)
        self.assertEqual(runtime["cloudProfileHigh"], 0.5)
        self.assertEqual(provenance["cloudProfileMid"]["status"], "UDS_DERIVED_MAPPING")

    def test_missing_exact_fields_use_native_fallback(self) -> None:
        preset = {"id": "Clear_Skies", "values": {"Wind Intensity": 2.0}}
        runtime, provenance = builder.preset_runtime(preset, self.shared())
        self.assertEqual(runtime["cloudCoverage"], 0.08)
        self.assertEqual(provenance["cloudCoverage"]["status"], "SOLUM_NATIVE")
        self.assertEqual(provenance["audioAutomatic"]["status"], "UNKNOWN")

    def test_nested_float_range_values_are_compacted(self) -> None:
        value = {"properties": [
            {"name": "LowerBound", "value": {"properties": [{"name": "Value", "value": 1.75}]}},
            {"name": "UpperBound", "value": {"properties": [{"name": "Value", "value": 2.2}]}},
        ]}
        self.assertEqual(builder.range_values(value), [1.75, 2.2])

    def test_templates_load_only_compact_package_and_local_files(self) -> None:
        root = TOOL / "environment_templates"
        text = "\n".join(path.read_text(encoding="utf-8") for path in root.rglob("*") if path.is_file())
        self.assertIn("data/solum_environment_package.json", text)
        for token in ("UDS_VISUAL_EVIDENCE", "UDS_VISUAL_CONTRACT", "https://", "http://"):
            self.assertNotIn(token, text)
        self.assertIn("getContext('webgl2'", text)
        self.assertIn("max-height: 40dvh", text)

    def test_javascript_modules_pass_parser(self) -> None:
        node = shutil.which("node")
        if not node:
            self.skipTest("node unavailable")
        for path in (TOOL / "environment_templates" / "js").glob("*.js"):
            result = subprocess.run([node, "--check", str(path)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_one_command_launcher_has_required_stages(self) -> None:
        text = (TOOL / "serve_solum_environment.py").read_text(encoding="utf-8")
        for stage in ("[1/5] Analyze", "[2/5] Build package", "[3/5] Copy resources", "[4/5] Validate", "[5/5] Serve", "OPEN IN BROWSER:"):
            self.assertIn(stage, text)

    def test_schema_locks_package_identity(self) -> None:
        schema = json.loads((ROOT / "schemas" / "solum_environment_package.schema.json").read_text(encoding="utf-8"))
        self.assertEqual(schema["properties"]["schema"]["const"], "solum.environment.package")
        self.assertEqual(schema["properties"]["schemaVersion"]["const"], 1)


if __name__ == "__main__":
    unittest.main()
