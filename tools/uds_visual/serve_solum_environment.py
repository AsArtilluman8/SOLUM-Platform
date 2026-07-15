#!/usr/bin/env python3
"""Analyze, build, validate and serve the local P62B SOLUM environment."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_P60 = "/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH"
DEFAULT_P61 = "/mnt/shared/Download/SOLUM_UDS_P61_SCENE_TRUTH"


def run(command: list[str]) -> str:
    result = subprocess.run(command, cwd=REPO_ROOT, capture_output=True, text=True)
    if result.returncode:
        if result.stdout:
            print(result.stdout, end="", file=sys.stderr)
        if result.stderr:
            print(result.stderr, end="", file=sys.stderr)
        raise SystemExit(result.returncode)
    return result.stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--p60", default=DEFAULT_P60)
    parser.add_argument("--p61", default=DEFAULT_P61)
    parser.add_argument("--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"))
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--bind", default="127.0.0.1")
    args = parser.parse_args()
    p60, p61, output = Path(args.p60).resolve(), Path(args.p61).resolve(), Path(args.output).resolve()

    print("[1/5] Analyze", flush=True)
    required = [p60 / "EXTRACTION_GATE.json", p60 / "inventory.json", p61 / "dependencies" / "package_index.json"]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise SystemExit(f"Missing inputs: {missing}")
    package_index = json.loads((p61 / "dependencies" / "package_index.json").read_text(encoding="utf-8"))
    print(f"UDS inputs: {package_index.get('package_count', 0)} indexed packages", flush=True)

    print("[2/5] Build package", flush=True)
    built = run([
        sys.executable, str(Path(__file__).with_name("build_solum_environment.py")),
        "--p60", str(p60), "--p61", str(p61), "--output", str(output),
    ])
    if built:
        print(built, flush=True)

    print("[3/5] Copy resources", flush=True)
    package = json.loads((output / "data" / "solum_environment_package.json").read_text(encoding="utf-8"))
    print(f"Verified WAV payloads: {len(package['resources']['audio'])}; texture fallbacks: {len(package['resources']['textures'])}", flush=True)

    print("[4/5] Validate", flush=True)
    validated = run([
        sys.executable, str(Path(__file__).with_name("validate_solum_environment.py")),
        "--p60", str(p60), "--p61", str(p61), "--output", str(output), "--determinism",
    ])
    if validated:
        print(validated, flush=True)

    print("[5/5] Serve", flush=True)
    print("OPEN IN BROWSER:", flush=True)
    print(f"http://{args.bind}:{args.port}", flush=True)
    os.execv(sys.executable, [
        sys.executable, "-m", "http.server", str(args.port),
        "--bind", args.bind, "--directory", str(output),
    ])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
