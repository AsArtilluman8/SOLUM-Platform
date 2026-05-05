#!/usr/bin/env python3
from __future__ import annotations
import base64, json, math, struct, sys
from pathlib import Path

ROOTS = [
    Path('/storage/emulated/0/SOLUMCreative/assets/models/imported'),
    Path('/storage/emulated/0/SOLUMCreative/assets/models'),
    Path('/storage/emulated/0/Download/SOLUMCreative/assets/models/imported'),
    Path('/storage/emulated/0/Download/SOLUMCreative/assets/models'),
]
OUT_ROOTS = [
    Path('/storage/emulated/0/SOLUMCreative/assets/models/cache'),
    Path('/storage/emulated/0/Download/SOLUMCreative/assets/models/cache'),
]

COMP_FLOAT = 5126
COMP_USHORT = 5123
COMP_UINT = 5125
COMP_UBYTE = 5121
TYPE_COUNT = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}
COMP_SIZE = {5126: 4, 5123: 2, 5125: 4, 5121: 1}

def safe_list(root: Path):
    try:
        if not root.exists() or not root.is_dir(): return []
        return sorted(list(root.glob('*.glb')) + list(root.glob('*.gltf')), key=lambda p: p.stat().st_mtime, reverse=True)
    except Exception:
        return []

def find_model():
    found = []
    for r in ROOTS:
        found.extend(safe_list(r))
    if not found: return None
    return found[0]

def parse_glb(path: Path):
    data = path.read_bytes()
    if data[:4] != b'glTF': raise ValueError('not a GLB file')
    version, length = struct.unpack_from('<II', data, 4)
    if version != 2: raise ValueError(f'unsupported GLB version {version}')
    pos = 12
    json_chunk = None
    bin_chunk = b''
    while pos + 8 <= len(data):
        clen, ctype = struct.unpack_from('<II', data, pos); pos += 8
        chunk = data[pos:pos+clen]; pos += clen
        if ctype == 0x4E4F534A: json_chunk = chunk
        elif ctype == 0x004E4942: bin_chunk = chunk
    if json_chunk is None: raise ValueError('missing JSON chunk')
    return json.loads(json_chunk.decode('utf-8')), bin_chunk

def parse_gltf(path: Path):
    doc = json.loads(path.read_text())
    buffers = []
    for b in doc.get('buffers', []):
        uri = b.get('uri', '')
        if uri.startswith('data:'):
            buffers.append(base64.b64decode(uri.split(',', 1)[1]))
        else:
            buffers.append((path.parent / uri).read_bytes())
    if not buffers: buffers = [b'']
    return doc, buffers[0]

def accessor_iter(doc, blob: bytes, accessor_index: int):
    acc = doc['accessors'][accessor_index]
    bv = doc['bufferViews'][acc['bufferView']]
    comp = acc['componentType']
    ncomp = TYPE_COUNT[acc.get('type', 'SCALAR')]
    count = acc['count']
    off = int(bv.get('byteOffset', 0)) + int(acc.get('byteOffset', 0))
    stride = int(bv.get('byteStride', COMP_SIZE[comp] * ncomp))
    if comp == COMP_FLOAT: fmt = '<' + 'f' * ncomp
    elif comp == COMP_USHORT: fmt = '<' + 'H' * ncomp
    elif comp == COMP_UINT: fmt = '<' + 'I' * ncomp
    elif comp == COMP_UBYTE: fmt = '<' + 'B' * ncomp
    else: raise ValueError(f'unsupported componentType {comp}')
    size = struct.calcsize(fmt)
    for i in range(count):
        yield struct.unpack_from(fmt, blob, off + i * stride)[:ncomp]

def make_color(material_index: int):
    palette = [
        (1.0, 0.46, 0.10), (0.12, 0.55, 1.0), (0.95, 0.95, 0.22),
        (0.35, 1.0, 0.45), (1.0, 0.25, 0.35), (0.75, 0.38, 1.0),
    ]
    return palette[material_index % len(palette)]

def normalize_positions(vertices):
    if not vertices: return vertices
    xs = [v[0] for v in vertices]; ys = [v[1] for v in vertices]; zs = [v[2] for v in vertices]
    cx = (min(xs)+max(xs))*0.5; cy = (min(ys)+max(ys))*0.5; cz = (min(zs)+max(zs))*0.5
    span = max(max(xs)-min(xs), max(ys)-min(ys), max(zs)-min(zs), 1e-5)
    scale = 1.65 / span
    return [((x-cx)*scale, (y-cy)*scale, (z-cz)*scale) for x,y,z in vertices]

def build_mesh(doc, blob: bytes):
    out_positions = []
    out_colors = []
    prims = []
    for mi, mesh in enumerate(doc.get('meshes', [])):
        for pi, prim in enumerate(mesh.get('primitives', [])):
            attrs = prim.get('attributes', {})
            if prim.get('mode', 4) != 4 or 'POSITION' not in attrs:
                continue
            pos_list = [tuple(x[:3]) for x in accessor_iter(doc, blob, attrs['POSITION'])]
            idx_acc = prim.get('indices')
            if idx_acc is not None:
                indices = [int(x[0]) for x in accessor_iter(doc, blob, idx_acc)]
            else:
                indices = list(range(len(pos_list)))
            tri_count = len(indices)//3
            mat = int(prim.get('material', 0))
            color = make_color(mat)
            base_start = len(out_positions)
            for idx in indices[:tri_count*3]:
                if 0 <= idx < len(pos_list):
                    out_positions.append(pos_list[idx])
                    out_colors.append(color)
            prims.append({'mesh': mi, 'primitive': pi, 'material': mat, 'vertices': len(out_positions)-base_start, 'triangles': tri_count})
    out_positions = normalize_positions(out_positions)
    return out_positions, out_colors, prims

def write_cache(model: Path, positions, colors, prims):
    cache_dirs = []
    for d in OUT_ROOTS:
        try:
            d.mkdir(parents=True, exist_ok=True)
            test = d / '.write_probe'
            test.write_text('ok')
            test.unlink(missing_ok=True)
            cache_dirs.append(d)
        except Exception:
            pass
    if not cache_dirs: raise RuntimeError('no writable cache dir')
    payload = bytearray()
    payload += b'SOLMESH1'
    payload += struct.pack('<II', len(positions), 0)
    for (x,y,z), (r,g,b) in zip(positions, colors):
        payload += struct.pack('<ffffff', x, y, z, r, g, b)
    manifest = {
        'schema': 'solum.active_mesh_cache', 'schemaVersion': 1,
        'status': 'mesh_cache_ready', 'sourceModel': str(model),
        'fileName': model.name, 'vertexLayout': 'Vertex3D_px_py_pz_r_g_b_float32',
        'vertexCount': len(positions), 'triangleCount': len(positions)//3,
        'primitiveCount': len(prims), 'primitives': prims,
        'note': 'P14 renders real GLB mesh geometry with debug material colors; textures/PBR are P15+'.
    }
    for d in cache_dirs:
        (d / 'active_mesh_v1.bin').write_bytes(payload)
        (d / 'active_mesh_manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    return cache_dirs[0] / 'active_mesh_v1.bin', manifest

def main():
    model = find_model()
    diag = Path('/storage/emulated/0/SOLUMCreative/diagnostics/latest')
    diag.mkdir(parents=True, exist_ok=True)
    if not model:
        state = {'schema':'solum.active_mesh_cache','schemaVersion':1,'status':'no_model_found','searchRoots':[str(r) for r in ROOTS]}
        (diag/'runtime_mesh_cache_state.json').write_text(json.dumps(state, indent=2))
        print('SOLUM MESH CACHE: NO_MODEL')
        return 2
    if model.suffix.lower() == '.glb': doc, blob = parse_glb(model)
    else: doc, blob = parse_gltf(model)
    positions, colors, prims = build_mesh(doc, blob)
    if len(positions) < 3: raise RuntimeError('no triangle vertices extracted')
    mesh_path, manifest = write_cache(model, positions, colors, prims)
    (diag/'runtime_mesh_cache_state.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(f'SOLUM MESH CACHE: OK model={model.name} vertices={len(positions)} triangles={len(positions)//3} cache={mesh_path}')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
