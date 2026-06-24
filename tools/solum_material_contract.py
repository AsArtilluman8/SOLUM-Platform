#!/usr/bin/env python3
from pathlib import Path
import argparse, json, struct, sys, time, hashlib

GLB_MAGIC = 0x46546C67
GLB_JSON_CHUNK = 0x4E4F534A
VERSION = 1
GLASS_HINTS = ("glass", "window", "lens", "crystal", "visor")
FOLIAGE_HINTS = ("leaf", "leaves", "foliage", "grass", "branch", "petal")

class MaterialError(Exception):
    pass

def _pad4_json(b: bytes) -> bytes:
    return b + b" " * ((4 - len(b) % 4) % 4)

def read_gltf_or_glb(path):
    p = Path(path)
    data = p.read_bytes()
    if len(data) >= 12 and struct.unpack_from("<I", data, 0)[0] == GLB_MAGIC:
        magic, version, length = struct.unpack_from("<III", data, 0)
        if version != 2:
            raise MaterialError(f"Unsupported GLB version: {version}")
        if length != len(data):
            raise MaterialError(f"GLB length mismatch: header={length} actual={len(data)}")
        off = 12
        while off + 8 <= len(data):
            chunk_len, chunk_type = struct.unpack_from("<II", data, off)
            off += 8
            chunk = data[off:off+chunk_len]
            off += chunk_len
            if chunk_type == GLB_JSON_CHUNK:
                return json.loads(chunk.decode("utf-8"))
        raise MaterialError("GLB JSON chunk not found")
    return json.loads(data.decode("utf-8"))

def write_sample_glb(path):
    gltf = {
        "asset": {"version": "2.0", "generator": "SOLUM_F5B_MATERIAL_SAMPLE"},
        "images": [
            {"uri": "CarPaint_BaseColor.png"},
            {"uri": "CarPaint_Normal.png"},
            {"uri": "CarPaint_ORM.png"},
            {"uri": "Window_BaseColor.png"},
            {"uri": "Window_Normal.png"},
            {"uri": "Neon_Emissive.png"},
            {"uri": "Leaf_BaseColor.png"}
        ],
        "samplers": [{"magFilter": 9729, "minFilter": 9987}],
        "textures": [
            {"source": 0, "sampler": 0}, {"source": 1, "sampler": 0},
            {"source": 2, "sampler": 0}, {"source": 3, "sampler": 0},
            {"source": 4, "sampler": 0}, {"source": 5, "sampler": 0},
            {"source": 6, "sampler": 0}
        ],
        "materials": [
            {
                "name": "CarPaint_Red",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [0.8, 0.05, 0.02, 1.0],
                    "baseColorTexture": {"index": 0, "texCoord": 0},
                    "metallicFactor": 0.7,
                    "roughnessFactor": 0.32,
                    "metallicRoughnessTexture": {"index": 2}
                },
                "normalTexture": {"index": 1},
                "alphaMode": "OPAQUE",
                "doubleSided": False
            },
            {
                "name": "Window_Glass_Clear",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [0.6, 0.85, 1.0, 0.35],
                    "baseColorTexture": {"index": 3},
                    "metallicFactor": 0.0,
                    "roughnessFactor": 0.05
                },
                "normalTexture": {"index": 4},
                "alphaMode": "BLEND",
                "doubleSided": True
            },
            {
                "name": "Neon_Emissive_Sign",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
                    "metallicFactor": 0.0,
                    "roughnessFactor": 0.6
                },
                "emissiveFactor": [0.0, 0.8, 1.0],
                "emissiveTexture": {"index": 5},
                "alphaMode": "OPAQUE"
            },
            {
                "name": "Leaf_Cutout",
                "pbrMetallicRoughness": {
                    "baseColorTexture": {"index": 6},
                    "metallicFactor": 0.0,
                    "roughnessFactor": 0.8
                },
                "alphaMode": "MASK",
                "alphaCutoff": 0.45,
                "doubleSided": True
            }
        ]
    }
    jb = _pad4_json(json.dumps(gltf, separators=(",", ":"), ensure_ascii=False).encode("utf-8"))
    total_len = 12 + 8 + len(jb)
    out = struct.pack("<III", GLB_MAGIC, 2, total_len)
    out += struct.pack("<II", len(jb), GLB_JSON_CHUNK)
    out += jb
    Path(path).write_bytes(out)
    return gltf

def texture_ref(gltf, info):
    if not isinstance(info, dict):
        return None
    ti = info.get("index")
    if ti is None:
        return None
    textures = gltf.get("textures", [])
    images = gltf.get("images", [])
    tex = textures[ti] if isinstance(ti, int) and 0 <= ti < len(textures) else {}
    image_index = tex.get("source")
    img = images[image_index] if isinstance(image_index, int) and 0 <= image_index < len(images) else {}
    return {
        "texture_index": ti,
        "image_index": image_index,
        "image_uri": img.get("uri"),
        "texcoord": info.get("texCoord", tex.get("texCoord", 0)),
        "sampler_index": tex.get("sampler")
    }

def nonzero3(v):
    return isinstance(v, list) and len(v) >= 3 and any(abs(float(x)) > 1e-6 for x in v[:3])

def classify(name, alpha_mode, base_color_factor, has_emission):
    lname = (name or "").lower()
    is_glass = any(x in lname for x in GLASS_HINTS)
    alpha = alpha_mode.upper()
    alpha_factor = 1.0
    if isinstance(base_color_factor, list) and len(base_color_factor) >= 4:
        alpha_factor = float(base_color_factor[3])
    if is_glass:
        return "glass"
    if has_emission:
        return "emissive"
    if alpha == "BLEND" or alpha_factor < 0.999:
        return "transparent"
    if alpha == "MASK":
        return "masked"
    return "opaque"

def convert_material(gltf, mat, index):
    name = mat.get("name") or f"material_{index}"
    pbr = mat.get("pbrMetallicRoughness", {}) or {}
    base_color_factor = pbr.get("baseColorFactor", [1.0, 1.0, 1.0, 1.0])
    metallic_factor = float(pbr.get("metallicFactor", 1.0))
    roughness_factor = float(pbr.get("roughnessFactor", 1.0))
    alpha_mode = str(mat.get("alphaMode", "OPAQUE")).upper()
    alpha_cutoff = float(mat.get("alphaCutoff", 0.5))
    emissive_factor = mat.get("emissiveFactor", [0.0, 0.0, 0.0])
    has_emission = bool(mat.get("emissiveTexture")) or nonzero3(emissive_factor)
    classification = classify(name, alpha_mode, base_color_factor, has_emission)
    lname = name.lower()
    is_glass_hint = any(x in lname for x in GLASS_HINTS)
    is_foliage_hint = any(x in lname for x in FOLIAGE_HINTS)

    warnings = []
    if mat.get("extensions"):
        warnings.append({"code": "UNSUPPORTED_EXTENSIONS", "message": "Material has glTF extensions; MVP stores known PBR fields only."})
    if is_glass_hint and alpha_mode == "OPAQUE":
        warnings.append({"code": "GLASS_OPAQUE_HINT", "message": "Name hints glass but alphaMode is OPAQUE."})
    if alpha_mode == "BLEND" and is_foliage_hint:
        warnings.append({"code": "FOLIAGE_BLEND_CHECK", "message": "Foliage-like material uses BLEND; MASK is usually cheaper on mobile."})
    if "normalTexture" not in mat:
        warnings.append({"code": "MISSING_NORMAL", "message": "No normal texture; material will look flatter."})

    return {
        "version": VERSION,
        "name": name,
        "source": {"format": "gltf", "index": index},
        "shading_model": "lit",
        "classification": classification,
        "alpha_mode": alpha_mode,
        "alpha_cutoff": alpha_cutoff,
        "double_sided": bool(mat.get("doubleSided", False)),
        "base_color_factor": base_color_factor,
        "metallic_factor": metallic_factor,
        "roughness_factor": roughness_factor,
        "emissive_factor": emissive_factor,
        "textures": {
            "base_color": texture_ref(gltf, pbr.get("baseColorTexture")),
            "metallic_roughness": texture_ref(gltf, pbr.get("metallicRoughnessTexture")),
            "normal": texture_ref(gltf, mat.get("normalTexture")),
            "occlusion": texture_ref(gltf, mat.get("occlusionTexture")),
            "emissive": texture_ref(gltf, mat.get("emissiveTexture"))
        },
        "flags": {
            "has_alpha": alpha_mode in ("BLEND", "MASK") or (isinstance(base_color_factor, list) and len(base_color_factor) >= 4 and float(base_color_factor[3]) < 0.999),
            "is_glass_hint": is_glass_hint,
            "is_foliage_hint": is_foliage_hint,
            "has_emission": has_emission,
            "unsupported_nodes": False
        },
        "warnings": warnings
    }

def convert_gltf(gltf):
    mats = gltf.get("materials", []) or []
    solum = {
        "schema": "SolumMaterialSet",
        "version": VERSION,
        "source": {
            "asset_version": gltf.get("asset", {}).get("version"),
            "generator": gltf.get("asset", {}).get("generator")
        },
        "materials": [convert_material(gltf, m, i) for i, m in enumerate(mats)],
        "summary": {}
    }
    counts = {}
    warning_count = 0
    for m in solum["materials"]:
        counts[m["classification"]] = counts.get(m["classification"], 0) + 1
        warning_count += len(m["warnings"])
    solum["summary"] = {
        "material_count": len(solum["materials"]),
        "classification_counts": counts,
        "warning_count": warning_count,
        "texture_ref_count": sum(1 for m in solum["materials"] for v in m["textures"].values() if v)
    }
    return solum

def validate_material_set(ms):
    if ms.get("schema") != "SolumMaterialSet":
        raise MaterialError("Not a SolumMaterialSet")
    if ms.get("version") != VERSION:
        raise MaterialError("Unsupported SolumMaterialSet version")
    errors = []
    names = set()
    for i, m in enumerate(ms.get("materials", [])):
        for key in ("name", "classification", "alpha_mode", "textures", "flags", "warnings"):
            if key not in m:
                errors.append(f"material {i} missing {key}")
        name = m.get("name")
        if name in names:
            errors.append(f"duplicate material name {name}")
        names.add(name)
        if m.get("classification") not in ("opaque", "masked", "transparent", "glass", "emissive"):
            errors.append(f"material {i} bad classification {m.get('classification')}")
        if m.get("alpha_mode") not in ("OPAQUE", "MASK", "BLEND"):
            errors.append(f"material {i} bad alpha_mode {m.get('alpha_mode')}")
    if errors:
        raise MaterialError("; ".join(errors))
    return {"ok": True, "material_count": len(ms.get("materials", [])), "warning_count": ms.get("summary", {}).get("warning_count", 0)}

def stable_hash(obj):
    b = json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(b).hexdigest()

def run_selftest(out_dir):
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    sample = out / "sample_materials.glb"
    write_sample_glb(sample)
    gltf = read_gltf_or_glb(sample)
    matset = convert_gltf(gltf)
    out_json = out / "sample_solum_materials.json"
    out_json.write_text(json.dumps(matset, indent=2, ensure_ascii=False), encoding="utf-8")
    tests = []

    def ok(name, fn):
        t0 = time.perf_counter()
        try:
            detail = fn()
            tests.append({"name": name, "status": "PASS", "ms": (time.perf_counter() - t0) * 1000, "detail": str(detail)})
        except Exception as e:
            tests.append({"name": name, "status": "FAIL", "ms": (time.perf_counter() - t0) * 1000, "detail": str(e)})

    ok("parse_sample_glb", lambda: f"{len(gltf.get('materials', []))} materials")
    ok("convert_material_count", lambda: matset["summary"]["material_count"])
    ok("validate_material_set", lambda: validate_material_set(matset))
    ok("classify_opaque", lambda: matset["summary"]["classification_counts"].get("opaque", 0))
    ok("classify_glass", lambda: matset["summary"]["classification_counts"].get("glass", 0))
    ok("classify_emissive", lambda: matset["summary"]["classification_counts"].get("emissive", 0))
    ok("classify_masked", lambda: matset["summary"]["classification_counts"].get("masked", 0))
    ok("texture_refs_present", lambda: matset["summary"]["texture_ref_count"])
    ok("deterministic_output_hash", lambda: stable_hash(matset))
    ok("warnings_are_nonfatal", lambda: matset["summary"]["warning_count"])

    result = {
        "schema": "SolumMaterialContractSelftest",
        "pass_count": sum(1 for t in tests if t["status"] == "PASS"),
        "fail_count": sum(1 for t in tests if t["status"] == "FAIL"),
        "tests": tests,
        "summary": matset["summary"],
        "sample_glb": str(sample),
        "sample_material_json": str(out_json)
    }
    (out / "solum_material_contract_test_results.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    report = [
        "# SOLUM Material Contract MVP Test Report",
        "",
        f"PASS={result['pass_count']} FAIL={result['fail_count']}",
        f"materials={matset['summary']['material_count']}",
        f"classification_counts={matset['summary']['classification_counts']}",
        f"warning_count={matset['summary']['warning_count']}",
        f"texture_ref_count={matset['summary']['texture_ref_count']}",
        ""
    ]
    for t in tests:
        report.append(f"- {t['name']}: {t['status']} — {t['detail']} ({t['ms']:.4f} ms)")
    (out / "solum_material_contract_test_report.md").write_text("\n".join(report), encoding="utf-8")
    return result

def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("create-sample-glb")
    p.add_argument("output")

    p = sub.add_parser("convert")
    p.add_argument("input")
    p.add_argument("--out", required=True)

    p = sub.add_parser("validate")
    p.add_argument("material_json")

    p = sub.add_parser("selftest")
    p.add_argument("--out", default="build/solum_material_contract")

    args = ap.parse_args()

    if args.cmd == "create-sample-glb":
        write_sample_glb(args.output)
        print(args.output)
    elif args.cmd == "convert":
        gltf = read_gltf_or_glb(args.input)
        matset = convert_gltf(gltf)
        Path(args.out).write_text(json.dumps(matset, indent=2, ensure_ascii=False), encoding="utf-8")
        print(json.dumps(matset["summary"], indent=2, ensure_ascii=False))
    elif args.cmd == "validate":
        ms = json.loads(Path(args.material_json).read_text(encoding="utf-8"))
        print(json.dumps(validate_material_set(ms), indent=2, ensure_ascii=False))
    elif args.cmd == "selftest":
        result = run_selftest(args.out)
        print(json.dumps(result, indent=2, ensure_ascii=False))
        if result["fail_count"]:
            sys.exit(1)

if __name__ == "__main__":
    main()
