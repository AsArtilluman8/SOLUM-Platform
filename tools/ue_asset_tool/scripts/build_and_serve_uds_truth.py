#!/usr/bin/env python3
from __future__ import annotations

import argparse
import functools
import json
import os
import shutil
import subprocess
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


TOOL_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = TOOL_ROOT.parents[1]
sys.path.insert(0, str(TOOL_ROOT / "src"))

from ueassettool.dataset import build_dataset, validate_dataset, write_json  # noqa: E402
from ueassettool.schema import validate_json_file  # noqa: E402


DEFAULT_ARCHIVE = Path("/mnt/shared/Download/UE_ASSET_READER_INPUT_P59_50MB.zip")
DEFAULT_DATASET = Path("/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH")
DEFAULT_HTML = Path("/mnt/shared/Download/SOLUM_UDS_FINAL_TRUTH_HTML")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="Validate, build and serve the P59 UDS truth dataset")
    result.add_argument("--archive", type=Path, default=DEFAULT_ARCHIVE)
    result.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    result.add_argument("--html", type=Path, default=DEFAULT_HTML)
    result.add_argument("--extract", action="store_true", help="rebuild canonical extraction and gate first")
    result.add_argument("--extract-only", action="store_true", help="stop after writing EXTRACTION_GATE.json")
    result.add_argument("--check-only", action="store_true", help="validate gate, schemas and dataset without HTML/server")
    result.add_argument("--no-serve", action="store_true", help="build frontend index but do not start HTTP server")
    result.add_argument("--bind", default="127.0.0.1")
    result.add_argument("--port", type=int, default=8765)
    return result


def load_and_validate_gate(dataset: Path) -> dict:
    gate_path = dataset / "EXTRACTION_GATE.json"
    if not gate_path.is_file():
        raise RuntimeError(f"extraction gate is missing: {gate_path}")
    schema = TOOL_ROOT / "schemas" / "extraction_gate.schema.json"
    errors = validate_json_file(gate_path, schema)
    if errors:
        raise RuntimeError("EXTRACTION_GATE schema failed: " + "; ".join(errors[:20]))
    gate = json.loads(gate_path.read_text(encoding="utf-8"))
    if gate.get("gate_status") != "PASSED" or gate.get("ready_for_html") is not True:
        raise RuntimeError(f"extraction gate is {gate.get('gate_status')}; HTML is forbidden")
    failed = [item.get("name") for item in gate.get("test_results", []) if not item.get("passed")]
    if failed:
        raise RuntimeError(f"gate contains failed checks: {failed}")
    return gate


def build_frontend(dataset: Path, html: Path, gate: dict) -> Path:
    source = TOOL_ROOT / "frontend"
    required = ("index.html", "app.css", "app.js")
    missing = [name for name in required if not (source / name).is_file()]
    if missing:
        raise RuntimeError(f"frontend source is incomplete: {missing}")
    html.mkdir(parents=True, exist_ok=True)
    for name in required:
        shutil.copy2(source / name, html / name)
    inventory = json.loads((dataset / "inventory.json").read_text(encoding="utf-8"))
    index = {
        "schema_version": "solum.uds-truth-ui/v1",
        "dataset_url": f"../{dataset.name}/",
        "inventory_url": f"../{dataset.name}/inventory.json",
        "coverage_url": f"../{dataset.name}/coverage.json",
        "errors_url": f"../{dataset.name}/errors.json",
        "provenance_url": f"../{dataset.name}/provenance.json",
        "gate_url": f"../{dataset.name}/EXTRACTION_GATE.json",
        "asset_count": inventory["totals"]["assets"],
        "package_count": inventory["totals"]["packages"],
        "gate_status": gate["gate_status"],
        "runtime_ready": gate["runtime_ready"],
    }
    write_json(html / "data" / "index.json", index)
    return html / "index.html"


def validate_frontend(dataset: Path, html: Path, gate: dict) -> dict:
    errors = []
    inventory = json.loads((dataset / "inventory.json").read_text(encoding="utf-8"))
    index = json.loads((html / "data" / "index.json").read_text(encoding="utf-8"))
    if index.get("asset_count") != inventory.get("totals", {}).get("assets"):
        errors.append("frontend/inventory asset count mismatch")
    if index.get("package_count") != gate.get("total_packages"):
        errors.append("frontend/gate package count mismatch")
    forbidden = ("mockData", "demoData", "fakeSky", "placeholderGeometry", "silentWav")
    for name in ("index.html", "app.css", "app.js"):
        text = (html / name).read_text(encoding="utf-8")
        for marker in forbidden:
            if marker.lower() in text.lower():
                errors.append(f"forbidden frontend marker {marker} in {name}")
        if "p59-" in text:
            errors.append(f"hardcoded P59 asset id in {name}")
    node = shutil.which("node")
    if node:
        process = subprocess.run([node, "--check", str(html / "app.js")], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if process.returncode:
            errors.append("JavaScript syntax: " + process.stdout.strip())
    return {"name": "frontend_dataset_integrity", "passed": not errors, "errors": errors}


class LogHandler(SimpleHTTPRequestHandler):
    log_path: Path

    def log_message(self, format: str, *args: object) -> None:
        with self.log_path.open("a", encoding="utf-8") as target:
            target.write(f"{self.log_date_time_string()} {self.client_address[0]} {format % args}\n")


def main() -> int:
    args = parser().parse_args()
    if args.extract:
        gate = build_dataset(args.archive.resolve(), args.dataset.resolve(), REPO_ROOT)
        print(f"Extraction gate: {gate['gate_status']}", flush=True)
        if args.extract_only:
            return 0 if gate["gate_status"] == "PASSED" else 3
    integrity = validate_dataset(args.dataset.resolve())
    if integrity["status"] != "VERIFIED":
        raise RuntimeError("dataset integrity failed: " + "; ".join(integrity["errors"][:20]))
    gate = load_and_validate_gate(args.dataset.resolve())
    if args.check_only:
        print(json.dumps({"gate": gate["gate_status"], "integrity": integrity}, ensure_ascii=False, indent=2))
        return 0
    index_path = build_frontend(args.dataset.resolve(), args.html.resolve(), gate)
    frontend_check = validate_frontend(args.dataset.resolve(), args.html.resolve(), gate)
    if not frontend_check["passed"]:
        raise RuntimeError("frontend integrity failed: " + "; ".join(frontend_check["errors"]))
    gate["test_results"] = [
        item for item in gate.get("test_results", []) if item.get("name") != frontend_check["name"]
    ] + [frontend_check]
    write_json(args.dataset.resolve() / "EXTRACTION_GATE.json", gate)
    inventory = json.loads((args.dataset / "inventory.json").read_text(encoding="utf-8"))
    log_path = args.dataset / "reports" / "server.log"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.touch(exist_ok=True)
    url = f"http://{args.bind}:{args.port}/{args.html.name}/"
    print(f"URL: {url}")
    print(f"Dataset: {args.dataset.resolve()}")
    print(f"Assets: {inventory['totals']['assets']}  Packages: {inventory['totals']['packages']}")
    print(f"Gate: {gate['gate_status']}")
    print(f"Frontend: {index_path}")
    print(f"Log: {log_path}")
    print(f"Stop: kill {os.getpid()}  (or Ctrl-C)", flush=True)
    if args.no_serve:
        return 0
    handler = functools.partial(LogHandler, directory=str(args.html.resolve().parent))
    handler.func.log_path = log_path
    server = ThreadingHTTPServer((args.bind, args.port), handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, RuntimeError, ValueError) as exc:
        print(f"uds-truth: {type(exc).__name__}: {exc}", file=sys.stderr)
        raise SystemExit(2)
