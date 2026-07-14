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
    export_auto_contract, export_blueprint_contract, export_curve_contract,
    export_material_contract, export_metasound_contract, export_niagara_contract,
    verify_package,
)
from .media import export_media
from .map import MapContractBuilder, build_map_gate, inventory_maps
from .source import PackageIndex, prepare_source_roots
from .refine import refine_existing_map_gate, refresh_refinement_reports


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
        if kind == "texture":
            media.add_argument(
                "--recovered-source",
                help="independently recovered raw TextureSource; accepted only on exact serialized BLAKE3 match",
            )
    maps = sub.add_parser(
        "inventory-maps",
        help="inventory .umap candidates with exact package-table evidence",
    )
    maps.add_argument("roots", nargs="+", help=".umap files or roots to search")
    maps.add_argument("-o", "--output", help="write candidate contract JSON")
    export_map = sub.add_parser(
        "export-map",
        help="build an exact UWorld/ULevel actor and transform contract",
    )
    source = export_map.add_mutually_exclusive_group()
    source.add_argument("--source-manifest", help="JSON source-root/file manifest")
    export_map.add_argument("--source-root", action="append", default=[], help="dependency root or ZIP source; repeatable")
    export_map.add_argument("--map", dest="map_path", required=True, help="direct selected .umap source")
    export_map.add_argument("--dataset", required=True, help="P61 dataset/cache output root")
    export_map.add_argument("-o", "--output", help="map contract path (default: <dataset>/maps/<name>.json)")
    export_map.add_argument(
        "--dependency-closure", action="store_true",
        help="also write exact package-reference closure and missing dependencies",
    )
    export_map.add_argument("--incremental", action="store_true", help="reuse hash-matching ZIP cache files")
    refine = sub.add_parser("refine-map-gate", help="refine saved P61 resolver outputs without exporting a map")
    refine.add_argument("--dataset", required=True)
    reports = sub.add_parser("refresh-map-reports", help="refresh reports from completed resolver outputs")
    reports.add_argument("--dataset", required=True)
    for command, help_text in (
        ("export-blueprint", "export Blueprint properties and graph contract"),
        ("export-niagara", "export Niagara scripts, parameters, dependencies and graph contract"),
        ("export-metasound", "export MetaSound property/reference contract"),
        ("export-material", "export exact Material/Function/Instance/Parameter Collection contract"),
        ("export-curve", "export exact CurveFloat/Vector/LinearColor keys"),
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
        if args.command == "inventory-maps":
            _json_dump(inventory_maps(args.roots, args.output), None if args.output else None)
            return 0
        if args.command == "export-map":
            source_manifest = prepare_source_roots(
                source_root=args.source_root,
                source_manifest=args.source_manifest,
                map_path=args.map_path,
                dataset=args.dataset,
                incremental=args.incremental,
            )
            direct = [Path(item) for item in source_manifest["direct_files"] if Path(item).suffix.lower() == ".umap"]
            candidates = inventory_maps(direct or source_manifest["roots"])
            selected = candidates.get("selected_map")
            if not isinstance(selected, str):
                raise UEAssetError("map source does not resolve to one exact UWorld/ULevel candidate")
            contract = MapContractBuilder(selected).build()
            dataset = Path(args.dataset)
            output = Path(args.output) if args.output else dataset / "maps" / f"{Path(selected).stem}.json"
            _json_dump(contract, str(output))
            result: dict[str, object] = {
                "schema": "ueassettool.export-map/v1",
                "source_manifest": source_manifest,
                "map_candidates": candidates,
                "map_contract": str(output),
                "map_sha256": contract["source"]["sha256"],
            }
            if args.dependency_closure:
                index = PackageIndex(source_manifest["roots"], cache_root=dataset / "cache")
                package_index = index.build()
                closure = index.dependency_closure(selected)
                package_index_path = dataset / "dependencies" / "package_index.json"
                closure_path = dataset / "dependencies" / "dependency_closure.json"
                missing_path = dataset / "dependencies" / "missing_dependencies.json"
                _json_dump(package_index, str(package_index_path))
                _json_dump(closure, str(closure_path))
                _json_dump({"schema": "ueassettool.missing-dependencies/v1", "missing": closure["missing"]}, str(missing_path))
                _json_dump(build_map_gate(contract, package_index, closure), str(dataset / "MAP_GATE.json"))
                result["dependency_closure"] = str(closure_path)
                result["missing_dependencies"] = str(missing_path)
            _json_dump(result, None)
            return 0
        if args.command == "refine-map-gate":
            _json_dump(refine_existing_map_gate(args.dataset), None)
            return 0
        if args.command == "refresh-map-reports":
            _json_dump(refresh_refinement_reports(args.dataset), None)
            return 0
        if args.command in ("export-texture", "export-audio"):
            kind = "texture" if args.command == "export-texture" else "audio"
            _json_dump(
                export_media(
                    args.asset, args.output, kind=kind, max_output=args.max_output,
                    recovered_source=getattr(args, "recovered_source", None),
                ),
                args.manifest,
            )
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
        if args.command == "export-material":
            _json_dump(export_material_contract(args.asset), args.output)
            return 0
        if args.command == "export-curve":
            data = export_curve_contract(args.asset)
            _json_dump(data, args.output)
            return 0 if data["status"] == "VERIFIED" else 3
        if args.command == "contract":
            _json_dump(export_auto_contract(args.asset), args.output)
            return 0
    except (UEAssetError, OSError) as exc:
        print(f"uex: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 2
    return 1
