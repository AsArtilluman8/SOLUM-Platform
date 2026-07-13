import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from ueassettool.dataset import SCHEMA_VERSION, TERMINAL_STATUSES, sha256_file, validate_asset_record, validate_dataset
from ueassettool.schema import validate_schema


class CanonicalDatasetTests(unittest.TestCase):
    def test_asset_schema_requires_truth_fields(self) -> None:
        self.assertIn("missing source_sha256", validate_asset_record({
            "schema_version": SCHEMA_VERSION,
            "asset_id": "asset",
            "extraction_status": "VERIFIED",
        }))
        self.assertNotIn("UNSUPPORTED", TERMINAL_STATUSES)

    def test_local_json_schema_validator(self) -> None:
        schema = {
            "type": "object", "required": ["status"],
            "properties": {"status": {"enum": ["VERIFIED"]}},
        }
        self.assertEqual(validate_schema({"status": "VERIFIED"}, schema), [])
        self.assertTrue(validate_schema({"status": "fake"}, schema))

    def test_dataset_integrity_checks_output_owner_and_hash(self) -> None:
        with TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "assets").mkdir()
            output = root / "contract.json"
            output.write_text("{}\n", encoding="utf-8")
            digest = sha256_file(output)
            asset_id = "p59-a-e1"
            record = {
                "schema_version": SCHEMA_VERSION, "asset_id": asset_id,
                "package_path": "/Game/A", "source_file": "/input/A.uasset",
                "source_sha256": "a" * 64, "asset_class": "Material",
                "package_version": {}, "custom_versions": [],
                "extraction_status": "VERIFIED", "verified_fields": {},
                "raw_verified_regions": [], "unsupported_regions": [], "missing_inputs": [],
                "imports": [], "exports": [], "dependencies": {},
                "generated_outputs": [{
                    "path": str(output), "sha256": digest,
                    "owning_asset_ids": [asset_id],
                }],
                "generated_output_sha256": [digest], "provenance": {},
                "validation_results": {},
            }
            asset_path = root / "assets" / f"{asset_id}.json"
            asset_path.write_text(json.dumps(record), encoding="utf-8")
            inventory = {
                "schema_version": SCHEMA_VERSION,
                "source_archive": {},
                "totals": {"packages": 1},
                "files": [{"kind": "package", "status": "VERIFIED"}],
                "assets": [{"asset_id": asset_id, "json_path": f"assets/{asset_id}.json"}],
                "reference_roots": [],
            }
            (root / "inventory.json").write_text(json.dumps(inventory), encoding="utf-8")
            result = validate_dataset(root)
            self.assertEqual(result["status"], "VERIFIED", result["errors"])


if __name__ == "__main__":
    unittest.main()
