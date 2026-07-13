import hashlib
import json
from collections import Counter
from pathlib import Path
import unittest
import zipfile

from ueassettool.contracts import export_material_contract
from ueassettool.package import UnrealPackage


ARCHIVE = Path("/mnt/shared/Download/UE_ASSET_READER_INPUT_P59_50MB.zip")
ARCHIVE_SHA256 = "94b16e87d71724e44197c46637d9c417faa3e2ea0feb8d0bdd98ddd9576b197e"
EXTRACTED = Path("/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH/_input/P59/assets")


@unittest.skipUnless(ARCHIVE.is_file() and EXTRACTED.is_dir(), "private P59 corpus is not locally available")
class P59RegressionTests(unittest.TestCase):
    def test_archive_inventory_and_package_foundation(self) -> None:
        digest = hashlib.sha256(ARCHIVE.read_bytes()).hexdigest()
        self.assertEqual(digest, ARCHIVE_SHA256)
        with zipfile.ZipFile(ARCHIVE) as source:
            self.assertIsNone(source.testzip())
            self.assertEqual(len([item for item in source.infolist() if not item.is_dir()]), 343)
            manifest = json.loads(source.read("COLLECTION_MANIFEST.json"))
            self.assertEqual(manifest["selected_files"], 341)
        classes = Counter()
        soft_paths = dependencies = preload = 0
        packages = list(EXTRACTED.rglob("*.uasset"))
        self.assertEqual(len(packages), 341)
        for path in packages:
            with UnrealPackage(path) as package:
                soft_paths += len(package.soft_object_paths)
                dependencies += sum(len(item["dependencies"]) for item in package.depends_map)
                preload += len(package.preload_dependencies)
                classes.update(
                    item.class_name for item in package.exports if item.is_asset and item.class_name
                )
        self.assertEqual(soft_paths, 209)
        self.assertEqual(dependencies, 30086)
        self.assertEqual(preload, 0)
        self.assertEqual(classes["MaterialFunction"], 61)
        self.assertEqual(classes["MaterialInstanceConstant"], 59)
        self.assertEqual(classes["MaterialParameterCollection"], 3)

    def test_mic_and_mpc_are_exact_non_graph_contracts(self) -> None:
        mic_path = next(EXTRACTED.rglob("Global_Volumetric_Fog_C.uasset"))
        mic = export_material_contract(mic_path)
        self.assertEqual(mic["graph"]["status"], "NOT_APPLICABLE")
        self.assertEqual(len(mic["material_instances"]), 1)
        self.assertGreaterEqual(len(mic["material_instances"][0]["scalar_parameters"]), 1)
        mpc_path = next(EXTRACTED.rglob("UDS_VolumetricClouds_MPC.uasset"))
        mpc = export_material_contract(mpc_path)
        self.assertEqual(mpc["status"], "VERIFIED")
        self.assertEqual(len(mpc["parameter_collections"]), 1)
        self.assertEqual(len(mpc["parameter_collections"][0]["scalar_parameters"]), 50)
        self.assertEqual(len(mpc["parameter_collections"][0]["vector_parameters"]), 10)


if __name__ == "__main__":
    unittest.main()
