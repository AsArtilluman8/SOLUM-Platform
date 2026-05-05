#!/usr/bin/env python3
import base64
import json
import os
import struct
from pathlib import Path
from datetime import datetime, timezone

SOLUM_ROOT = Path(os.environ.get("SOLUM_ROOT", "/storage/emulated/0/SOLUMCreative"))
MODEL_ROOT = SOLUM_ROOT / "assets" / "models"
IMPORT_DIR = MODEL_ROOT / "imported"
DIAG_DIR = SOLUM_ROOT / "diagnostics" / "latest"
DIAG_DIR.mkdir(parents=True, exist_ok=True)
IMPORT_DIR.mkdir(parents=True, exist_ok=True)

EXTENSION_POLICY = {
    "KHR_texture_transform": "needed_for_95_percent_if_used",
    "KHR_materials_clearcoat": "needed_for_95_percent_if_used",
    "KHR_materials_transmission": "needed_for_95_percent_if_used",
    "KHR_materials_sheen": "needed_for_95_percent_if_used",
    "KHR_materials_ior": "needed_for_95_percent_if_used",
    "KHR_materials_specular": "needed_for_95_percent_if_used",
    "KHR_materials_emissive_strength": "nice_to_have",
    "KHR_materials_unlit": "separate_shader_path_required",
    "KHR_draco_mesh_compression": "blocked_until_decoder_dependency",
    "EXT_meshopt_compression": "blocked_until_meshoptimizer_dependency",
    "KHR_texture_basisu": "blocked_until_ktx2_basisu_dependency",
}


def read_gltf(path: Path):
    data = path.read_bytes()
    if path.suffix.lower() == ".glb":
        if len(data) < 20 or data[:4] != b"glTF":
            raise ValueError("not a GLB file")
        version, total_len = struct.unpack_from("<II", data, 4)
        if version != 2:
            raise ValueError(f"unsupported GLB version {version}")
        offset = 12
        json_chunk = None
        bin_len = 0
        while offset + 8 <= len(data):
            chunk_len, chunk_type = struct.unpack_from("<II", data, offset)
            offset += 8
            chunk = data[offset:offset + chunk_len]
            offset += chunk_len
            if chunk_type == 0x4E4F534A:
                json_chunk = chunk
            elif chunk_type == 0x004E4942:
                bin_len += len(chunk)
        if json_chunk is None:
            raise ValueError("GLB JSON chunk missing")
        return json.loads(json_chunk.decode("utf-8")), {"container": "glb", "binaryBytes": bin_len, "fileBytes": len(data)}
    else:
        return json.loads(data.decode("utf-8")), {"container": "gltf", "binaryBytes": 0, "fileBytes": len(data)}


def accessor_count(gltf, accessor_idx):
    if accessor_idx is None:
        return 0
    arr = gltf.get("accessors", [])
    if not isinstance(accessor_idx, int) or accessor_idx < 0 or accessor_idx >= len(arr):
        return 0
    return int(arr[accessor_idx].get("count", 0) or 0)


def image_info(gltf, idx):
    images = gltf.get("images", [])
    if not isinstance(idx, int) or idx < 0 or idx >= len(images):
        return {"index": idx, "valid": False}
    im = images[idx]
    return {
        "index": idx,
        "valid": True,
        "name": im.get("name", ""),
        "uri": im.get("uri", ""),
        "mimeType": im.get("mimeType", ""),
        "bufferView": im.get("bufferView", None),
    }


def texture_image_index(gltf, texture_index):
    textures = gltf.get("textures", [])
    if not isinstance(texture_index, int) or texture_index < 0 or texture_index >= len(textures):
        return None
    return textures[texture_index].get("source")


def tex_slot(gltf, material, key, nested=None):
    src = material.get(key) if nested is None else material.get(nested, {}).get(key)
    if not isinstance(src, dict):
        return {"present": False}
    ti = src.get("index")
    img_idx = texture_image_index(gltf, ti)
    return {
        "present": ti is not None,
        "textureIndex": ti,
        "texCoord": src.get("texCoord", 0),
        "scale": src.get("scale", None),
        "strength": src.get("strength", None),
        "image": image_info(gltf, img_idx) if img_idx is not None else None,
        "extensions": src.get("extensions", {}),
    }


def audit_one(path: Path):
    gltf, meta = read_gltf(path)
    meshes = gltf.get("meshes", [])
    materials = gltf.get("materials", [])
    textures = gltf.get("textures", [])
    images = gltf.get("images", [])
    nodes = gltf.get("nodes", [])
    skins = gltf.get("skins", [])
    animations = gltf.get("animations", [])
    extensions_used = gltf.get("extensionsUsed", []) or []
    extensions_required = gltf.get("extensionsRequired", []) or []

    primitive_count = 0
    vertex_count = 0
    triangle_count = 0
    primitive_reports = []
    attr_presence = {"POSITION": 0, "NORMAL": 0, "TANGENT": 0, "TEXCOORD_0": 0, "JOINTS_0": 0, "WEIGHTS_0": 0, "COLOR_0": 0}

    for mi, mesh in enumerate(meshes):
        for pi, prim in enumerate(mesh.get("primitives", []) or []):
            primitive_count += 1
            attrs = prim.get("attributes", {}) or {}
            vc = accessor_count(gltf, attrs.get("POSITION"))
            ic = accessor_count(gltf, prim.get("indices"))
            tris = (ic // 3) if ic else (vc // 3)
            vertex_count += vc
            triangle_count += tris
            for k in attr_presence:
                if k in attrs:
                    attr_presence[k] += 1
            primitive_reports.append({
                "meshIndex": mi,
                "primitiveIndex": pi,
                "mode": prim.get("mode", 4),
                "materialIndex": prim.get("material", None),
                "vertexCount": vc,
                "indexCount": ic,
                "triangleEstimate": tris,
                "attributes": sorted(attrs.keys()),
            })

    material_reports = []
    alpha_modes = {}
    for i, mat in enumerate(materials):
        pbr = mat.get("pbrMetallicRoughness", {}) or {}
        alpha = mat.get("alphaMode", "OPAQUE")
        alpha_modes[alpha] = alpha_modes.get(alpha, 0) + 1
        material_reports.append({
            "index": i,
            "name": mat.get("name", f"material_{i}"),
            "alphaMode": alpha,
            "alphaCutoff": mat.get("alphaCutoff", 0.5),
            "doubleSided": bool(mat.get("doubleSided", False)),
            "baseColorFactor": pbr.get("baseColorFactor", [1, 1, 1, 1]),
            "metallicFactor": pbr.get("metallicFactor", 1.0),
            "roughnessFactor": pbr.get("roughnessFactor", 1.0),
            "baseColorTexture": tex_slot(gltf, mat, "baseColorTexture", "pbrMetallicRoughness"),
            "metallicRoughnessTexture": tex_slot(gltf, mat, "metallicRoughnessTexture", "pbrMetallicRoughness"),
            "normalTexture": tex_slot(gltf, mat, "normalTexture"),
            "occlusionTexture": tex_slot(gltf, mat, "occlusionTexture"),
            "emissiveTexture": tex_slot(gltf, mat, "emissiveTexture"),
            "emissiveFactor": mat.get("emissiveFactor", [0, 0, 0]),
            "extensions": sorted((mat.get("extensions", {}) or {}).keys()),
        })

    unsupported = []
    extension_decisions = []
    for ext in extensions_used:
        policy = EXTENSION_POLICY.get(ext, "unknown_extension_requires_review")
        extension_decisions.append({"extension": ext, "policy": policy, "required": ext in extensions_required})
        if policy.startswith("blocked") or policy.startswith("unknown"):
            unsupported.append(ext)

    can_basic = bool(meshes) and attr_presence["POSITION"] > 0 and len(unsupported) == 0
    needs_skinning = len(skins) > 0 or attr_presence["JOINTS_0"] > 0 or attr_presence["WEIGHTS_0"] > 0
    needs_tangent_gen = attr_presence["NORMAL"] > 0 and any(m["normalTexture"].get("present") for m in material_reports) and attr_presence["TANGENT"] == 0
    needs_extension_pass = len(extensions_used) > 0

    report = {
        "schema": "solum.gltf_import_probe",
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "file": str(path),
        "fileName": path.name,
        **meta,
        "assetDecision": {
            "canRenderBasicStaticMesh": can_basic,
            "needsSkinningPass": needs_skinning,
            "needsTangentGeneration": needs_tangent_gen,
            "needsExtensionPassForAuthorLook": needs_extension_pass,
            "unsupportedExtensions": unsupported,
        },
        "counts": {
            "nodes": len(nodes),
            "meshes": len(meshes),
            "primitives": primitive_count,
            "vertices": vertex_count,
            "trianglesEstimate": triangle_count,
            "materials": len(materials),
            "textures": len(textures),
            "images": len(images),
            "skins": len(skins),
            "animations": len(animations),
        },
        "attributeCoverage": attr_presence,
        "alphaModes": alpha_modes,
        "extensionsUsed": extensions_used,
        "extensionsRequired": extensions_required,
        "extensionDecisions": extension_decisions,
        "materials": material_reports,
        "primitives": primitive_reports[:128],
        "notes": [
            "This is an import/material audit, not final rendering.",
            "For 95 percent author look, implement glTF metallic-roughness, correct sRGB/linear, tangent-space normal maps, alpha/doubleSided, IBL, PBR Neutral tone mapping, and used KHR material extensions.",
        ],
    }
    return report


def find_models():
    files = []
    for root in [IMPORT_DIR, MODEL_ROOT]:
        if root.exists():
            files.extend([p for p in root.rglob("*") if p.suffix.lower() in [".glb", ".gltf"]])
    uniq = []
    seen = set()
    for f in files:
        key = str(f.resolve())
        if key not in seen:
            uniq.append(f)
            seen.add(key)
    return sorted(uniq, key=lambda p: p.stat().st_mtime if p.exists() else 0, reverse=True)


def main():
    models = find_models()
    reports = []
    errors = []
    for m in models[:8]:
        try:
            reports.append(audit_one(m))
        except Exception as exc:
            errors.append({"file": str(m), "error": str(exc)})

    state = {
        "schema": "solum.runtime_model_state",
        "schemaVersion": 3,
        "status": "gltf_probe_ok" if reports else "waiting_for_sample_gltf_or_glb",
        "modelSearchRoots": [str(IMPORT_DIR), str(MODEL_ROOT)],
        "modelFilesFound": len(models),
        "auditedCount": len(reports),
        "errors": errors,
        "activeModel": reports[0] if reports else None,
        "allAudits": reports,
    }
    (DIAG_DIR / "runtime_model_state.json").write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8")

    active = reports[0] if reports else None
    mat_state = {
        "schema": "solum.runtime_material_state",
        "schemaVersion": 3,
        "status": "gltf_material_probe_ok" if active else "waiting_for_sample_gltf_or_glb",
        "rendererMaterialMode": "probe_only_not_rendered_yet",
        "gltfPbrReady": False,
        "selectedImporter": "cgltf_first_for_native; python_probe_for_diagnostics_now",
        "activeModelFile": active.get("fileName") if active else None,
        "materialCount": active.get("counts", {}).get("materials", 0) if active else 0,
        "materials": active.get("materials", []) if active else [],
        "requiredFor95PercentAuthorLook": [
            "glTF metallic-roughness PBR",
            "baseColor sRGB sampling",
            "normal map tangent-space decode",
            "metallicRoughness linear sampling",
            "occlusion/emissive slots",
            "alpha MASK/BLEND rules",
            "doubleSided handling",
            "IBL/environment lighting",
            "PBR Neutral tone mapping",
            "KHR extensions when used",
        ],
    }
    (DIAG_DIR / "runtime_material_state.json").write_text(json.dumps(mat_state, indent=2, ensure_ascii=False), encoding="utf-8")

    tex_state = {
        "schema": "solum.runtime_texture_state",
        "schemaVersion": 1,
        "status": "texture_slot_probe_ok" if active else "waiting_for_sample_gltf_or_glb",
        "activeModelFile": active.get("fileName") if active else None,
        "imageCount": active.get("counts", {}).get("images", 0) if active else 0,
        "textureCount": active.get("counts", {}).get("textures", 0) if active else 0,
        "textureSlotsByMaterial": [
            {
                "material": m.get("name"),
                "baseColorTexture": m.get("baseColorTexture"),
                "normalTexture": m.get("normalTexture"),
                "metallicRoughnessTexture": m.get("metallicRoughnessTexture"),
                "occlusionTexture": m.get("occlusionTexture"),
                "emissiveTexture": m.get("emissiveTexture"),
            }
            for m in (active.get("materials", []) if active else [])
        ],
    }
    (DIAG_DIR / "runtime_texture_state.json").write_text(json.dumps(tex_state, indent=2, ensure_ascii=False), encoding="utf-8")

    print("SOLUM GLTF/GLB AUDIT: OK")
    print(f"models_found={len(models)} audited={len(reports)}")
    if active:
        print(f"active={active['fileName']}")
        print("decision=" + json.dumps(active["assetDecision"], ensure_ascii=False))
    else:
        print("status=waiting_for_sample_gltf_or_glb")

if __name__ == "__main__":
    main()
