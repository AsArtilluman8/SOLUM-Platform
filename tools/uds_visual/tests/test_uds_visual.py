from __future__ import annotations

import argparse
import importlib.util
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
TOOL = ROOT / "tools" / "uds_visual"


def load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


builder = load("p62_builder", TOOL / "build_uds_visual_truth.py")
validator = load("p62_validator", TOOL / "validate_uds_visual_truth.py")


class UdsVisualTests(unittest.TestCase):
    def test_stable_parameter_id_and_host_path_redaction(self) -> None:
        first = builder.stable_parameter_id("/Game/P", "/Game/P.CDO", "Time of Day")
        second = builder.stable_parameter_id("/Game/P", "/Game/P.CDO", "Time of Day")
        self.assertEqual(first, second)
        self.assertRegex(first, r"^uds\.time_of_day\.[0-9a-f]{16}$")
        cleaned = builder.clean_value({
            "source_file": "/mnt/private/source.uasset",
            "nested": {"path": "/data/private/file", "object_path": "/Game/UDS.Asset"},
            "loose": "/storage/private/file",
        })
        self.assertNotIn("source_file", cleaned)
        self.assertNotIn("path", cleaned["nested"])
        self.assertEqual(cleaned["nested"]["object_path"], "/Game/UDS.Asset")
        self.assertEqual(cleaned["loose"], "<redacted-host-path>")

    def test_schema_subset_rejects_missing_and_wrong_const(self) -> None:
        schema = {
            "type": "object", "required": ["version", "items"],
            "properties": {
                "version": {"const": "v1"},
                "items": {"type": "array", "minItems": 1},
            },
        }
        self.assertEqual(validator.schema_errors({"version": "v1", "items": [1]}, schema), [])
        errors = validator.schema_errors({"version": "v2", "items": []}, schema)
        self.assertTrue(any("const" in item for item in errors))
        self.assertTrue(any("minItems" not in item and "items <" in item for item in errors))

    def test_templates_have_no_weather_synthesis_or_external_network(self) -> None:
        files = [path for path in (TOOL / "templates").rglob("*") if path.is_file()]
        text = "\n".join(path.read_text(encoding="utf-8") for path in files)
        for token in ("Math.random", "random(", "fbm(", "proceduralRain", "proceduralSnow"):
            self.assertNotIn(token, text)
        self.assertNotIn("https://", text)
        self.assertNotIn("http://", text)
        self.assertIn("getContext('webgl2'", text)
        self.assertIn("height: 40vh", text)

    def test_javascript_modules_pass_available_parser(self) -> None:
        node = shutil.which("node")
        if not node:
            self.skipTest("node unavailable")
        for path in (TOOL / "templates" / "js").glob("*.js"):
            result = subprocess.run([node, "--check", str(path)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_builder_fails_on_missing_truth_inputs(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            args = argparse.Namespace(
                p60=str(Path(directory) / "missing-p60"),
                p61=str(Path(directory) / "missing-p61"),
                output=str(Path(directory) / "output"),
            )
            with self.assertRaises(FileNotFoundError):
                builder.build(args)

    def test_legacy_niagara_parameter_store_preserves_exact_data(self) -> None:
        small = {"name": "Parameters", "value": {"items": [{"name": "small"}]}}
        exact = {
            "name": "Parameters",
            "decode_status": "decoded_native",
            "header_physical_offset": 41,
            "raw": {"physical_offset": 64, "size": 4, "sha256": "a" * 64},
            "value": {"items": [
                {"name": "Wind", "data_size": 4, "data_hex": "0000803f", "data_sha256": "b" * 64},
                {"name": "Wet", "data_size": 4, "data_hex": "00000000", "data_sha256": "c" * 64},
            ]},
        }
        contract = {"exports": [{
            "object": "/Game/Test.NPC", "class": "NiagaraParameterCollection", "export_index": 2,
            "properties": [small, {"name": "Nested", "value": {"properties": [exact]}}],
        }]}
        result = builder.decoded_niagara_parameters(contract)
        self.assertIsNotNone(result)
        self.assertEqual(result["parameter_count"], 2)
        self.assertEqual(result["parameters"][0]["data_hex"], "0000803f")
        self.assertEqual(result["source_export"]["export_index"], 2)

    def test_active_controls_are_evidence_backed_and_weather_vfx_locked(self) -> None:
        parameters = [{"id": "p.time", "name": "Time of Day", "system": "time"}]
        presets = [{"id": "Clear_Skies"}]
        controls = builder.controls_contract(parameters, presets)
        for item in controls:
            if item["active"]:
                self.assertTrue(item["evidence_ids"])
        locked = {item["id"]: item for item in controls}
        for name in ("sun", "moon", "stars", "clouds", "rain", "snow", "lightning", "audio"):
            self.assertFalse(locked[name]["active"])


if __name__ == "__main__":
    unittest.main()
