#!/usr/bin/env python3
"""Build, validate and serve the P62 UDS visual-truth preview on one command."""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def run(command: list[str]) -> None:
    result = subprocess.run(command, cwd=REPO_ROOT)
    if result.returncode:
        raise SystemExit(result.returncode)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--p60", required=True)
    parser.add_argument("--p61", required=True)
    parser.add_argument("--output", default=str(REPO_ROOT / "generated_local" / "uds_visual_preview"))
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--bind", default="127.0.0.1")
    args = parser.parse_args()
    output = Path(args.output).resolve()
    run([
        sys.executable, str(Path(__file__).with_name("build_uds_visual_truth.py")),
        "--p60", args.p60, "--p61", args.p61, "--output", str(output),
    ])
    run([
        sys.executable, str(Path(__file__).with_name("validate_uds_visual_truth.py")),
        "--p60", args.p60, "--p61", args.p61, "--output", str(output), "--determinism",
    ])
    print(f"P62 preview: http://{args.bind}:{args.port}/", flush=True)
    os.execv(
        sys.executable,
        [
            sys.executable, "-m", "http.server", str(args.port),
            "--bind", args.bind, "--directory", str(output),
        ],
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
