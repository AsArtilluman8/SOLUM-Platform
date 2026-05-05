#!/usr/bin/env python3
import json
import os
import struct
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

SOLUM_ROOT = Path(os.environ.get("SOLUM_ROOT", "/storage/emulated/0/SOLUMCreative"))
DIAG_DIR = SOLUM_ROOT / "diagnostics" / "latest"
MODEL_ROOTS = [
    SOLUM_ROOT / "assets" / "models",
    SOLUM_ROOT / "assets" / "gltf",
    SOLUM_ROOT / "assets" / "samples",
]
EXPLICIT_SAMPLE = os.environ.get("SOLUM_GLTF_SAMPLE", "").strip()


def now() -> str:
    return time.strftime("%Y-%m-%d %H:%M:%S %z")


def write_json(name: str, data: Dict[str, Any]) -> None:
    DIAG_DIR.mkdir(parents=True, exist_ok=True)
    (DIAG_DIR / name).write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def find_sample() -> Optional[Path]:
    if EXPLICIT_SAMPLE:
        p = Path(EXPLICIT_SAMPLE)
        if p.exists() and p.suffix.lower() in (".gltf", ".glb"):
            return p
    candidates: List[Path] = []
    for root in MODEL_ROOTS:
        if root.exists():
            candidates.extend(root.rglob("*.gltf"))
            candidates.extend(root.rglob("*.glb"))
    candidates = sorted(candidates, key=lambda p: (p.name.lower(), str(p)))
    return candidates[0] if candidates else None


def read_gltf(path: Path) -> Tuple[Optional[Dict[str, Any]], List[str]]:
    warnings: List[str] = []
    try:
        if path.suffix.lower() == ".gltf":
            return json.loads(path.read_text(encoding="utf-8")), warnings
        raw = path.read_bytes()
        if len(raw) < 20:
            return None, ["GLB too small"]
        magic, version, total_len = struct.unpack_from("<III", raw, 0)
        if magic != 0x46546C67:  # glTF
            return None, ["Invalid GLB magic"]
        if version != 2:
            warnings.append(f"GLB version is {version}, expected 2")
        offset = 12
        json_chunk = None
        while offset + 8 <= len(raw):
            chunk_len, chunk_type = struct.unpack_from("<II", raw, offset)
            offset += 8
            chunk = raw[offset:offset + chunk_len]
            offset += chunk_len
            if chunk_type == 0x4E4F534A:  # JSON
                json_chunk = chunk.decode("utf-8").rstrip(" \t\r\n\x00")
                break
        if not json_chunk:
            return None, warnings + ["GLB JSON chunk not found"]
        return json.loads(json_chunk), warnings
    except Exception as e:
        return None, [f"parse_failed: {type(e).__name__}: {e}"]


def image_uri(gltf: Dict[str, Any], index: Optional[int]) -> Optional[str]:
    if index is None:
        return None
    images = gltf.get("images") or []
    if not isinstance(index, int) or index < 0 or index >= len(images):
        return None
    img = images[index] or {}
    if "uri" in img:
        return str(img.get("uri"))
    if "bufferView" in img:
        mime = img.get("mimeType", "unknown")
        return f"bufferView:{img.get('bufferView')}:{mime}"
    return None


def texture_info(gltf: Dict[str, Any], info: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    if not isinstance(info, dict) or "index" not in info:
        return {"present": False}
    textures = gltf.get("textures") or []
    idx = info.get("index")
    result: Dict[str, Any] = {"present": True, "textureIndex": idx, "texCoord": info.get("texCoord", 0)}
    if isinstance(idx, int) and 0 <= idx < len(textures):
        tex = textures[idx] or {}
        result["sourceImage"] = tex.get("source")
        result["sampler"] = tex.get("sampler")
        result["uri"] = image_uri(gltf, tex.get("source"))
    else:
        result["warning"] = "texture index out of range"
    return result


def analyze(path: Path, gltf: Dict[str, Any], parse_warnings: List[str]) -> Tuple[Dict[str, Any], Dict[str, Any], Dict[str, Any]]:
    meshes = gltf.get("meshes") or []
    nodes = gltf.get("nodes") or []
    scenes = gltf.get("scenes") or []
    materials = gltf.get("materials") or []
    images = gltf.get("images") or []
    textures = gltf.get("textures") or []
    samplers = gltf.get("samplers") or []

    primitive_count = 0
    accessor_indices = set()
    warnings = list(parse_warnings)
    for mesh in meshes:
        for prim in mesh.get("primitives", []) or []:
            primitive_count += 1
            attrs = prim.get("attributes") or {}
            for key in ("POSITION", "NORMAL", "TANGENT", "TEXCOORD_0", "TEXCOORD_1", "COLOR_0"):
                if key in attrs:
                    accessor_indices.add(attrs[key])
            if "indices" in prim:
                accessor_indices.add(prim.get("indices"))
            if "material" not in prim:
                warnings.append("primitive_without_material")
            if "NORMAL" not in attrs:
                warnings.append("primitive_missing_NORMAL")
            if "TEXCOORD_0" not in attrs:
                warnings.append("primitive_missing_TEXCOORD_0")

    material_reports = []
    texture_slot_summary = {
        "baseColor": 0,
        "normal": 0,
        "metallicRoughness": 0,
        "occlusion": 0,
        "emissive": 0,
    }

    for mi, mat in enumerate(materials):
        pbr = mat.get("pbrMetallicRoughness") or {}
        base = texture_info(gltf, pbr.get("baseColorTexture"))
        mr = texture_info(gltf, pbr.get("metallicRoughnessTexture"))
        normal = texture_info(gltf, mat.get("normalTexture"))
        occ = texture_info(gltf, mat.get("occlusionTexture"))
        emi = texture_info(gltf, mat.get("emissiveTexture"))
        if base.get("present"): texture_slot_summary["baseColor"] += 1
        if mr.get("present"): texture_slot_summary["metallicRoughness"] += 1
        if normal.get("present"): texture_slot_summary["normal"] += 1
        if occ.get("present"): texture_slot_summary["occlusion"] += 1
        if emi.get("present"): texture_slot_summary["emissive"] += 1
        alpha_mode = mat.get("alphaMode", "OPAQUE")
        if alpha_mode not in ("OPAQUE", "MASK", "BLEND"):
            warnings.append(f"material_{mi}_unknown_alphaMode_{alpha_mode}")
        material_reports.append({
            "index": mi,
            "name": mat.get("name", f"material_{mi}"),
            "workflow": "glTF_2_0_metallic_roughness",
            "alphaMode": alpha_mode,
            "alphaCutoff": mat.get("alphaCutoff", 0.5),
            "doubleSided": bool(mat.get("doubleSided", False)),
            "baseColorFactor": pbr.get("baseColorFactor", [1, 1, 1, 1]),
            "metallicFactor": pbr.get("metallicFactor", 1.0),
            "roughnessFactor": pbr.get("roughnessFactor", 1.0),
            "emissiveFactor": mat.get("emissiveFactor", [0, 0, 0]),
            "textures": {
                "baseColor": base,
                "normal": normal,
                "metallicRoughness": mr,
                "occlusion": occ,
                "emissive": emi,
            },
            "colorSpaceRules": {
                "baseColor": "sRGB_to_linear_before_lighting",
                "emissive": "sRGB_to_linear_before_lighting",
                "normal": "linear_non_color",
                "metallicRoughness": "linear_non_color",
                "occlusion": "linear_non_color",
            }
        })

    model_state = {
        "schema": "solum.runtime_model_state",
        "schemaVersion": 2,
        "status": "gltf_probe_ok",
        "time": now(),
        "source": str(path),
        "format": path.suffix.lower().lstrip("."),
        "assetKind": "glTF_2_0_probe",
        "meshCount": len(meshes),
        "nodeCount": len(nodes),
        "sceneCount": len(scenes),
        "primitiveCount": primitive_count,
        "materialCount": len(materials),
        "imageCount": len(images),
        "textureCount": len(textures),
        "samplerCount": len(samplers),
        "accessorReferencesSeen": len(accessor_indices),
        "warnings": sorted(set(warnings)),
        "renderReadiness": {
            "canProbeMaterials": True,
            "canUploadMeshNow": False,
            "reason": "P13 is import/material diagnostics only; GPU upload comes in P14/P15"
        }
    }

    material_state = {
        "schema": "solum.runtime_material_state",
        "schemaVersion": 2,
        "status": "gltf_material_probe_ok",
        "time": now(),
        "source": str(path),
        "materialModel": "glTF_2_0_metallic_roughness",
        "implementationStage": "probe_only_not_pbr_shader_yet",
        "materialCount": len(materials),
        "materials": material_reports,
        "requiredNextBeforePBR": [
            "descriptor_set_layout_for_textures",
            "sampler_policy",
            "image_upload_path",
            "tangent_space_validation_for_normal_maps",
            "baseColor_sRGB_decode",
            "metallicRoughness_linear_sampling",
            "alphaMode_pipeline_policy"
        ]
    }

    texture_state = {
        "schema": "solum.runtime_texture_state",
        "schemaVersion": 1,
        "status": "gltf_texture_slots_probed",
        "time": now(),
        "source": str(path),
        "imageCount": len(images),
        "textureCount": len(textures),
        "samplerCount": len(samplers),
        "slotSummary": texture_slot_summary,
        "images": [
            {
                "index": i,
                "name": img.get("name", f"image_{i}"),
                "uri": img.get("uri"),
                "bufferView": img.get("bufferView"),
                "mimeType": img.get("mimeType"),
            }
            for i, img in enumerate(images)
        ],
        "note": "Texture image decoding/upload is intentionally not implemented in P13. This file verifies glTF slots before PBR shader work."
    }
    return model_state, material_state, texture_state


def write_no_sample() -> None:
    base = {
        "time": now(),
        "status": "waiting_for_sample_gltf_or_glb",
        "expectedPaths": [str(p) for p in MODEL_ROOTS],
        "explicitEnv": "SOLUM_GLTF_SAMPLE=/path/to/model.glb",
        "message": "Put a .gltf or .glb under SOLUMCreative/assets/models, then rerun tools/gltf_import_probe.py"
    }
    write_json("runtime_model_state.json", {"schema": "solum.runtime_model_state", "schemaVersion": 2, **base})
    write_json("runtime_material_state.json", {
        "schema": "solum.runtime_material_state",
        "schemaVersion": 2,
        **base,
        "implementationStage": "probe_ready_no_sample",
        "materialModel": "glTF_2_0_metallic_roughness",
        "notFakeMaterial": True
    })
    write_json("runtime_texture_state.json", {"schema": "solum.runtime_texture_state", "schemaVersion": 1, **base})


def main() -> int:
    sample = find_sample()
    if not sample:
        write_no_sample()
        print("SOLUM GLTF PROBE: WAITING_FOR_SAMPLE")
        print("Put .gltf/.glb into /storage/emulated/0/SOLUMCreative/assets/models")
        return 0
    gltf, warnings = read_gltf(sample)
    if gltf is None:
        write_json("runtime_model_state.json", {
            "schema": "solum.runtime_model_state",
            "schemaVersion": 2,
            "status": "gltf_probe_failed",
            "time": now(),
            "source": str(sample),
            "warnings": warnings
        })
        print("SOLUM GLTF PROBE: FAILED")
        return 2
    model_state, material_state, texture_state = analyze(sample, gltf, warnings)
    write_json("runtime_model_state.json", model_state)
    write_json("runtime_material_state.json", material_state)
    write_json("runtime_texture_state.json", texture_state)
    print("SOLUM GLTF PROBE: OK")
    print(f"source={sample}")
    print(f"materials={material_state['materialCount']} textures={texture_state['textureCount']} images={texture_state['imageCount']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
