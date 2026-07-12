from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from . import __version__
from .errors import UEAssetError
from .package import UnrealPackage
from .properties import PropertyParser
from .compression import build_bundled_ooz, decompress_compressed_buffer, write_verified_output
from .blueprint import BlueprintGraphDecoder
from .bytecode import StructScriptDecoder
from .extract import extract_verified
from .mesh import export_static_mesh, validate_glb
from .contracts import (
    export_auto_contract, export_blueprint_contract, export_metasound_contract,
    export_niagara_contract, verify_package,
)
from .media import export_media


def _json_dump(data: object, path: str | None) -> None:
    text = json.dumps(data, ensure_ascii=False, indent=2, sort_keys=False) + "\n"
    if path:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_name(target.name + ".tmp")
        temporary.write_text(text, encoding="utf-8")
        temporary.replace(target)
    else:
        sys.stdout.write(text)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="uex", description="Strict Unreal package inspector/extractor")
    parser.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    sub = parser.add_subparsers(dest="command", required=True)
    inspect = sub.add_parser("inspect", help="verify header/name/import/export tables and emit JSON")
    inspect.add_argument("asset", help=".uasset or .umap path")
    inspect.add_argument("-o", "--output", help="write JSON to this path")
    inspect.add_argument("--compact", action="store_true", help="omit the full name table")
    dump = sub.add_parser("dump", help="decode tagged properties with raw provenance for unsupported bytes")
    dump.add_argument("asset", help=".uasset or .umap path")
    dump.add_argument("-o", "--output", help="write JSON to this path")
    selection = dump.add_mutually_exclusive_group(required=True)
    selection.add_argument("--export", type=int, action="append", help="1-based export index; repeatable")
    selection.add_argument("--assets-only", action="store_true", help="parse exports marked as top-level assets")
    selection.add_argument("--class", dest="class_name", help="parse every export with this exact class name")
    selection.add_argument("--all", action="store_true", help="parse every available export")
    bulk = sub.add_parser("bulk-decompress", help="strictly decode an FCompressedBuffer at a known offset")
    bulk.add_argument("asset")
    bulk.add_argument("--offset", required=True, type=lambda x: int(x, 0))
    bulk.add_argument("-o", "--output", required=True)
    bulk.add_argument("--max-output", type=int, default=2 * 1024 * 1024 * 1024)
    doctor = sub.add_parser("doctor", help="check/build optional native backends")
    doctor.add_argument("--build-oodle", action="store_true")
    graph = sub.add_parser("graph", help="reconstruct Blueprint/Niagara editor node and pin connections")
    graph.add_argument("asset")
    graph.add_argument("-o", "--output", help="write graph contract JSON")
    graph.add_argument("--include-niagara", action="store_true")
    bytecode = sub.add_parser("bytecode", help="decode and validate serialized UFunction Kismet bytecode")
    bytecode.add_argument("asset")
    bytecode.add_argument("-o", "--output", help="write exact bytecode contract JSON")
    extract = sub.add_parser("extract", help="automatically extract strictly verified embedded payloads")
    extract.add_argument("asset")
    extract.add_argument("-d", "--output-dir", required=True)
    extract.add_argument("-o", "--manifest", help="write extraction manifest JSON")
    extract.add_argument("--max-output", type=int, default=2 * 1024 * 1024 * 1024)
    mesh = sub.add_parser("export-mesh", help="export a structurally verified StaticMesh to GLB")
    mesh.add_argument("asset")
    mesh.add_argument("-o", "--output", required=True)
    mesh.add_argument("--manifest", help="write mesh/export verification JSON")
    mesh.add_argument("--max-output", type=int, default=2 * 1024 * 1024 * 1024)
    glb = sub.add_parser("validate-glb", help="validate GLB structure, chunk and buffer bounds")
    glb.add_argument("glb")
    verify = sub.add_parser("verify", help="verify package ranges, companions and package trailer")
    verify.add_argument("asset")
    verify.add_argument("-o", "--output")
    for command, kind in (("export-texture", "texture"), ("export-audio", "audio")):
        media = sub.add_parser(command, help=f"export one strictly verified {kind} payload")
        media.add_argument("asset")
        media.add_argument("-o", "--output", required=True)
        media.add_argument("--manifest")
        media.add_argument("--max-output", type=int, default=2 * 1024 * 1024 * 1024)
    for command, help_text in (
        ("export-blueprint", "export Blueprint properties and graph contract"),
        ("export-niagara", "export Niagara parameters, curves and graph contract"),
        ("export-metasound", "export MetaSound property/reference contract"),
        ("contract", "auto-detect and export the strongest truthful asset contract"),
    ):
        contract = sub.add_parser(command, help=help_text)
        contract.add_argument("asset")
        contract.add_argument("-o", "--output")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.command == "inspect":
            with UnrealPackage(args.asset) as package:
                data = package.inspect_dict()
                if args.compact:
                    data.pop("names", None)
                _json_dump(data, args.output)
                return 0
        if args.command == "dump":
            with UnrealPackage(args.asset) as package:
                if args.export:
                    indices = args.export
                elif args.assets_only:
                    indices = [i for i, item in enumerate(package.exports, 1) if item.is_asset]
                elif args.class_name:
                    indices = [i for i, item in enumerate(package.exports, 1) if item.class_name == args.class_name]
                else:
                    indices = list(range(1, len(package.exports) + 1))
                decoder = PropertyParser(package)
                data = {
                    "schema": "ueassettool.properties/v1",
                    "source": {"path": str(package.path), "sha256": package.sha256},
                    "exports": [decoder.parse_export(i) for i in indices],
                }
                _json_dump(data, args.output)
                return 0
        if args.command == "bulk-decompress":
            header, raw = decompress_compressed_buffer(args.asset, args.offset, max_output=args.max_output)
            result = write_verified_output(args.output, raw)
            _json_dump({
                "schema": "ueassettool.bulk/v1",
                "header": header.__dict__,
                "output": result,
            }, None)
            return 0
        if args.command == "doctor":
            result: dict[str, object] = {"python": sys.version.split()[0]}
            if args.build_oodle:
                result["oodle_helper"] = str(build_bundled_ooz())
            _json_dump(result, None)
            return 0
        if args.command == "graph":
            with UnrealPackage(args.asset) as package:
                data = BlueprintGraphDecoder(package).decode(include_niagara=args.include_niagara)
                data["source_sha256"] = package.sha256
                _json_dump(data, args.output)
                return 0
        if args.command == "bytecode":
            with UnrealPackage(args.asset) as package:
                data = StructScriptDecoder(package).decode_functions()
                data["source"] = {"path": str(package.path), "sha256": package.sha256}
                _json_dump(data, args.output)
                return 0 if data["status"] == "VERIFIED" else 3
        if args.command == "extract":
            data = extract_verified(args.asset, args.output_dir, max_output=args.max_output)
            _json_dump(data, args.manifest)
            return 0
        if args.command == "export-mesh":
            data = export_static_mesh(args.asset, args.output, max_output=args.max_output)
            data["glb_validation"] = validate_glb(args.output)
            _json_dump(data, args.manifest)
            return 0
        if args.command == "validate-glb":
            _json_dump(validate_glb(args.glb), None)
            return 0
        if args.command == "verify":
            _json_dump(verify_package(args.asset), args.output)
            return 0
        if args.command in ("export-texture", "export-audio"):
            kind = "texture" if args.command == "export-texture" else "audio"
            _json_dump(export_media(args.asset, args.output, kind=kind, max_output=args.max_output), args.manifest)
            return 0
        if args.command == "export-blueprint":
            _json_dump(export_blueprint_contract(args.asset), args.output)
            return 0
        if args.command == "export-niagara":
            _json_dump(export_niagara_contract(args.asset), args.output)
            return 0
        if args.command == "export-metasound":
            _json_dump(export_metasound_contract(args.asset), args.output)
            return 0
        if args.command == "contract":
            _json_dump(export_auto_contract(args.asset), args.output)
            return 0
    except (UEAssetError, OSError) as exc:
        print(f"uex: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 2
    return 1
