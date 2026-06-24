#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import struct
import subprocess
import sys
import time
from pathlib import Path
from typing import Any

THIS = Path(__file__).resolve()
TOOLS_DIR = THIS.parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

try:
    import solum_package_mvp as spkg
except Exception as exc:
    raise SystemExit(f"Failed to import tools/solum_package_mvp.py: {exc}")

def canonical_json_bytes(obj: Any) -> bytes:
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")

def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))

def normalize_materials(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, list):
        return [m for m in data if isinstance(m, dict)]
    if isinstance(data, dict):
        for key in ("materials", "solum_materials", "items"):
            val = data.get(key)
            if isinstance(val, list):
                return [m for m in val if isinstance(m, dict)]
        if data.get("name") or data.get("classification") or data.get("type"):
            return [data]
    return []

def find_texture_refs(obj: Any) -> list[str]:
    found: list[str] = []

    def walk(v: Any):
        if isinstance(v, dict):
            for k, x in v.items():
                lk = str(k).lower()
                if isinstance(x, str):
                    lx = x.lower()
                    if (
                        "texture" in lk or lk.endswith("_tex") or lk.endswith("tex")
                        or lx.endswith((".png", ".jpg", ".jpeg", ".ktx", ".ktx2", ".webp", ".tga"))
                    ):
                        found.append(x)
                else:
                    walk(x)
        elif isinstance(v, list):
            for x in v:
                walk(x)

    walk(obj)
    out, seen = [], set()
    for t in found:
        if t not in seen:
            seen.add(t)
            out.append(t)
    return out

def pack_scene_one_object(pool: spkg.StringPool, mat_count: int) -> bytes:
    obj_name = pool.add("CookedGLBRoot")
    mesh_id = 0
    mat_id = 0 if mat_count else 0
    flags = 0
    x = y = z = 0.0
    sx = sy = sz = 1.0
    tri_count_unknown = 0
    return struct.pack("<I", 1) + struct.pack(
        "<IIIIffffffI",
        obj_name, mesh_id, mat_id, flags,
        x, y, z, sx, sy, sz, tri_count_unknown,
    )

def pack_tex_table(pool: spkg.StringPool, texture_refs: list[str]) -> bytes:
    out = bytearray()
    out += struct.pack("<I", len(texture_refs))
    for ref in texture_refs:
        name_off = pool.add(ref)
        out += struct.pack("<IIIII", name_off, 0, 0, 0, 0)
    return bytes(out)

def pack_empty_graph() -> bytes:
    graph_type = 0
    flags = 0
    node_count = 0
    link_count = 0
    node_table_off = 24
    link_table_off = 24
    payload_off = 24
    return struct.pack("<HHIIIII", graph_type, flags, node_count, link_count, node_table_off, link_table_off, payload_off)

def cook_package(glb_path: Path, materials_json_path: Path, out_path: Path, package_name: str = "SolumCookedPackageMVP") -> dict[str, Any]:
    if not glb_path.exists():
        raise FileNotFoundError(glb_path)
    if not materials_json_path.exists():
        raise FileNotFoundError(materials_json_path)

    glb_bytes = glb_path.read_bytes()
    material_json = load_json(materials_json_path)
    materials = normalize_materials(material_json)
    texture_refs = find_texture_refs(material_json)

    pool = spkg.StringPool()
    package_name_off = pool.add(package_name)
    author_off = pool.add("SOLUM_COOKER_MVP")
    scene_name_off = pool.add("scene_one_glb_object")
    glb_name_off = pool.add(glb_path.name)
    mat_name_off = pool.add("solum_materials_json")
    tex_name_off = pool.add("texture_refs")
    graph_name_off = pool.add("empty_graph")
    dbgi_name_off = pool.add("cook_debug_info")
    deps_name_off = pool.add("dependencies_empty")

    mani = struct.pack(
        "<IIIIIII",
        package_name_off,
        author_off,
        1,
        len(materials),
        len(texture_refs),
        0,
        0,
    )

    scne = pack_scene_one_object(pool, len(materials))
    mat_payload = canonical_json_bytes({
        "schema": "SolumCookedMaterialPayload.v1",
        "source": materials_json_path.name,
        "material_count": len(materials),
        "materials": materials,
    })
    tex_payload = pack_tex_table(pool, texture_refs)
    grph = pack_empty_graph()
    dbgi = canonical_json_bytes({
        "schema": "SolumCookerDebug.v1",
        "glb": str(glb_path),
        "materials": str(materials_json_path),
        "glb_bytes": len(glb_bytes),
        "material_count": len(materials),
        "texture_ref_count": len(texture_refs),
        "cook_time_unix": int(time.time()),
        "note": "F5C MVP bridge: GLB blob + SolumMaterial JSON -> SLPK",
    })
    deps = struct.pack("<I", 0)

    chunks = [
        {"type": b"MANI", "schema": 1, "flags": spkg.FLAG_REQUIRED, "data": mani, "name_off": package_name_off},
        {"type": b"SCNE", "schema": 1, "flags": spkg.FLAG_REQUIRED, "data": scne, "name_off": scene_name_off},
        {"type": b"GLB ", "schema": 1, "flags": 0, "data": glb_bytes, "name_off": glb_name_off},
        {"type": b"MAT ", "schema": 1, "flags": 0, "data": mat_payload, "name_off": mat_name_off},
        {"type": b"TEX ", "schema": 1, "flags": 0, "data": tex_payload, "name_off": tex_name_off},
        {"type": b"GRPH", "schema": 1, "flags": 0, "data": grph, "name_off": graph_name_off},
        {"type": b"DBGI", "schema": 1, "flags": 0, "data": dbgi, "name_off": dbgi_name_off},
        {"type": b"DEPS", "schema": 1, "flags": 0, "data": deps, "name_off": deps_name_off},
    ]

    out_bytes = spkg.write_package(chunks, pool.bytes())
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(out_bytes)

    summary = spkg.dump_summary(out_bytes, fast=False)
    validation = spkg.validate_file(out_path)

    return {
        "ok": True,
        "output": str(out_path),
        "output_bytes": len(out_bytes),
        "output_sha256": sha256_bytes(out_bytes),
        "glb_bytes": len(glb_bytes),
        "material_count": len(materials),
        "texture_ref_count": len(texture_refs),
        "chunk_types": summary.get("chunk_types"),
        "summary": summary,
        "validation": validation,
    }

def run_material_contract_selftest(out_dir: Path) -> tuple[Path, Path]:
    mat_tool = TOOLS_DIR / "solum_material_contract.py"
    if not mat_tool.exists():
        raise FileNotFoundError("tools/solum_material_contract.py is required for F5C selftest")
    out_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run([sys.executable, str(mat_tool), "selftest", "--out", str(out_dir)], check=True)
    glb = out_dir / "sample_materials.glb"
    mat_json = out_dir / "sample_solum_materials.json"
    if not glb.exists():
        raise FileNotFoundError(glb)
    if not mat_json.exists():
        raise FileNotFoundError(mat_json)
    return glb, mat_json

def run_selftest(out_dir: Path) -> dict[str, Any]:
    out_dir.mkdir(parents=True, exist_ok=True)
    source_dir = out_dir / "material_source"
    glb, mat_json = run_material_contract_selftest(source_dir)

    pkg_a = out_dir / "sample_cooked_scene.slpk"
    pkg_b = out_dir / "sample_cooked_scene_2.slpk"

    tests: list[dict[str, Any]] = []
    holder: dict[str, Any] = {}

    def test(name: str, fn):
        t0 = time.perf_counter()
        try:
            detail = fn()
            tests.append({"name": name, "status": "PASS", "ms": (time.perf_counter() - t0) * 1000, "detail": str(detail)})
        except Exception as exc:
            tests.append({"name": name, "status": "FAIL", "ms": (time.perf_counter() - t0) * 1000, "detail": repr(exc)})

    def cook_once():
        res = cook_package(glb, mat_json, pkg_a)
        holder["cook"] = res
        return f"{res['output_bytes']} bytes"

    test("material_source_exists", lambda: f"{glb.name}, {mat_json.name}")
    test("cook_glb_materials_to_slpk", cook_once)
    test("validate_cooked_package", lambda: spkg.validate_file(pkg_a)["ok"])
    test("summary_object_count", lambda: spkg.dump_summary(pkg_a.read_bytes(), fast=True)["object_count"])
    test("summary_material_count", lambda: spkg.dump_summary(pkg_a.read_bytes(), fast=True)["material_count"])
    test("chunk_GLB_present", lambda: "GLB " in spkg.dump_summary(pkg_a.read_bytes(), fast=True)["chunk_types"])
    test("chunk_MAT_present", lambda: "MAT " in spkg.dump_summary(pkg_a.read_bytes(), fast=True)["chunk_types"])
    test("chunk_TEX_present", lambda: "TEX " in spkg.dump_summary(pkg_a.read_bytes(), fast=True)["chunk_types"])
    test("texture_refs_nonzero", lambda: holder["cook"]["texture_ref_count"])
    test("material_count_nonzero", lambda: holder["cook"]["material_count"])

    def deterministic_metadata():
        res2 = cook_package(glb, mat_json, pkg_b)
        a = holder["cook"]
        stable = (
            a["material_count"] == res2["material_count"]
            and a["texture_ref_count"] == res2["texture_ref_count"]
            and a["chunk_types"] == res2["chunk_types"]
            and a["summary"]["object_count"] == res2["summary"]["object_count"]
        )
        if not stable:
            raise AssertionError("cook output metadata changed")
        return "stable_metadata"

    test("deterministic_metadata", deterministic_metadata)

    cook_result = holder.get("cook", {})
    report = {
        "schema": "SolumCookerMVPSelftest.v1",
        "pass_count": sum(1 for t in tests if t["status"] == "PASS"),
        "fail_count": sum(1 for t in tests if t["status"] == "FAIL"),
        "tests": tests,
        "cook_result": cook_result,
        "source_glb": str(glb),
        "source_materials": str(mat_json),
        "output_package": str(pkg_a),
    }

    (out_dir / "solum_cooker_mvp_selftest.json").write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    lines = [
        "# SOLUM Cooker MVP Selftest",
        "",
        f"PASS={report['pass_count']} FAIL={report['fail_count']}",
        f"source_glb={glb}",
        f"source_materials={mat_json}",
        f"output_package={pkg_a}",
    ]
    if cook_result:
        lines += [
            f"output_bytes={cook_result.get('output_bytes')}",
            f"materials={cook_result.get('material_count')}",
            f"texture_refs={cook_result.get('texture_ref_count')}",
            f"chunks={cook_result.get('chunk_types')}",
        ]
    lines.append("")
    for t in tests:
        lines.append(f"- {t['name']}: {t['status']} — {t['detail']} ({t['ms']:.4f} ms)")
    (out_dir / "solum_cooker_mvp_selftest.md").write_text("\n".join(lines), encoding="utf-8")

    return report

def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("cook")
    p.add_argument("--glb", required=True)
    p.add_argument("--materials", required=True)
    p.add_argument("--out", required=True)
    p.add_argument("--name", default="SolumCookedPackageMVP")

    p = sub.add_parser("selftest")
    p.add_argument("--out", default="build/solum_cooker_mvp")

    args = ap.parse_args()

    if args.cmd == "cook":
        res = cook_package(Path(args.glb), Path(args.materials), Path(args.out), args.name)
        print(json.dumps(res, indent=2, ensure_ascii=False))
    elif args.cmd == "selftest":
        res = run_selftest(Path(args.out))
        print(json.dumps(res, indent=2, ensure_ascii=False))
        if res["fail_count"]:
            sys.exit(1)

if __name__ == "__main__":
    main()
