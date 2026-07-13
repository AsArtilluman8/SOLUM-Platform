from pathlib import Path
import re
import subprocess
import unittest


FRONTEND = Path(__file__).resolve().parents[1] / "frontend"


class FrontendIntegrityTests(unittest.TestCase):
    def test_all_required_sections_are_data_driven(self) -> None:
        script = (FRONTEND / "app.js").read_text(encoding="utf-8")
        for section in (
            "Overview", "Coverage", "Asset Browser", "Dependencies", "Textures", "Models",
            "Materials", "Material Functions", "MIC", "MPC", "Audio", "Curves",
            "Blueprint Graph", "Kismet Bytecode", "Niagara", "Map Actors", "Transforms",
            "UDS Runtime Reconstruction", "Errors", "Unsupported", "Provenance",
        ):
            self.assertIn(f'"{section}"', script)
        self.assertIn("state.inventory.assets", script)

    def test_no_hardcoded_asset_id_or_generated_uds_state(self) -> None:
        combined = "\n".join(
            (FRONTEND / name).read_text(encoding="utf-8")
            for name in ("index.html", "app.css", "app.js")
        )
        self.assertIsNone(re.search(r"p59-[0-9a-f]{20}-e\d+", combined))
        for marker in ("mockData", "demoData", "fakeSky", "placeholderGeometry", "silentWav"):
            self.assertNotIn(marker.lower(), combined.lower())

    def test_javascript_syntax(self) -> None:
        process = subprocess.run(
            ["node", "--check", str(FRONTEND / "app.js")],
            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )
        self.assertEqual(process.returncode, 0, process.stdout)


if __name__ == "__main__":
    unittest.main()
