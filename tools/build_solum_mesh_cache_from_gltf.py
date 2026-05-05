#!/usr/bin/env python3
from __future__ import annotations
import base64
import json
import struct
import sys
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
DIAG_DIR = Path('/storage/emulated/0/SOLUMCreative/diagnostics/latest')

COMP_FLOAT = 5126
COMP_USHORT = 5123
COMP_UINT = 5125
COMP_UBYTE = 5121
TYPE_COUNT = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}
COMP_SIZE = {COMP_FLOAT: 4, COMP_USHORT: 2, COMP_UINT: 4, COMP_UBYTE: 1}


def safe_list(root: Path):
    try:
        if not root.exists() or not root.is_dir():
            return []
        files = list(root.glob('*.glb')) + list(root.glob('*.gltf'))
        return sorted(files, key=lambda p: p.stat().st_mtime, reverse=True)
    except PermissionError:
        return []
    except Exception:
        return []


def find_model():
    found = []
    for r in ROOTS:
        found.extend(safe_list(r))
    return found[0] if found else None


def parse_glb(path: Path):
    data = path.read_bytes()
    if len(data) < 20 or data[:4] != b'glTF':
        raise ValueError('not a GLB file')
    version, _length = struct.unpack_from('<II', data, 4)
    if version != 2:
        raise ValueError(f'unsupported GLB version {version}')
    pos = 12
    json_chunk = None
    bin_chunk = b''
    while pos + 8 <= len(data):
        clen, ctype = struct.unpack_from('<II', data, pos)
        pos += 8
        chunk = data[pos:pos + clen]
        pos += clen
        if ctype == 0x4E4F534A:
            json_chunk = chunk
        elif ctype == 0x004E4942:
            bin_chunk = chunk
    if json_chunk is None:
        raise ValueError('missing GLB JSON chunk')
    return json.loads(json_chunk.decode('utf-8')), bin_chunk


def parse_gltf(path: Path):
    doc = json.loads(path.read_text(encoding='utf-8'))
    buffers = []
    for b in doc.get('buffers', []):
        uri = b.get('uri', '')
        if uri.startswith('data:'):
            buffers.append(base64.b64decode(uri.split(',', 1)[1]))
        elif uri:
            buffers.append((path.parent / uri).read_bytes())
    if not buffers:
        buffers = [b'']
    return doc, buffers[0]


def accessor_iter(doc, blob: bytes, accessor_index: int):
    acc = doc['accessors'][accessor_index]
    bv = doc['bufferViews'][acc['bufferView']]
    comp = acc['componentType']
    ncomp = TYPE_COUNT[acc.get('type', 'SCALAR')]
    count = int(acc['count'])
    off = int(bv.get('byteOffset', 0)) + int(acc.get('byteOffset', 0))
    stride = int(bv.get('byteStride', COMP_SIZE[comp] * ncomp))
    if comp == COMP_FLOAT:
        fmt = '<' + 'f' * ncomp
    elif comp == COMP_USHORT:
        fmt = '<' + 'H' * ncomp
    elif comp == COMP_UINT:
        fmt = '<' + 'I' * ncomp
    elif comp == COMP_UBYTE:
        fmt = '<' + 'B' * ncomp
    else:
        raise ValueError(f'unsupported componentType {comp}')
    for i in range(count):
        yield struct.unpack_from(fmt, blob, off + i * stride)[:ncomp]


def make_color(material_index: int):
    palette = [
        (1.0, 0.46, 0.10),
        (0.12, 0.55, 1.0),
        (0.95, 0.95, 0.22),
        (0.35, 1.0, 0.45),
        (1.0, 0.25, 0.35),
        (0.75, 0.38, 1.0),
    ]
    return palette[material_index % len(palette)]


def normalize_positions(vertices):
    if not vertices:
        return vertices
    xs = [v[0] for v in vertices]
    ys = [v[1] for v in vertices]
    zs = [v[2] for v in vertices]
    cx = (min(xs) + max(xs)) * 0.5
    cy = (min(ys) + max(ys)) * 0.5
    cz = (min(zs) + max(zs)) * 0.5
    span = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs), 1e-5)
    scale = 1.65 / span
    return [((x - cx) * scale, (y - cy) * scale, (z - cz) * scale) for x, y, z in vertices]


def build_mesh(doc, blob: bytes):
    out_positions = []
    out_colors = []
    prims = []
    for mesh_i, mesh in enumerate(doc.get('meshes', [])):
        for prim_i, prim in enumerate(mesh.get('primitives', [])):
            attrs = prim.get('attributes', {})
            mode = int(prim.get('mode', 4))
            if mode != 4 or 'POSITION' not in attrs:
                continue
            pos_list = [tuple(x[:3]) for x in accessor_iter(doc, blob, attrs['POSITION'])]
            idx_acc = prim.get('indices')
            if idx_acc is not None:
                indices = [int(x[0]) for x in accessor_iter(doc, blob, idx_acc)]
            else:
                indices = list(range(len(pos_list)))
            tri_count = len(indices) // 3
            mat_i = int(prim.get('material', 0))
            color = make_color(mat_i)
            before = len(out_positions)
            for idx in indices[:tri_count * 3]:
                if 0 <= idx < len(pos_list):
                    out_positions.append(pos_list[idx])
                    out_colors.append(color)
            prims.append({
                'mesh': mesh_i,
                'primitive': prim_i,
                'material': mat_i,
                'vertices': len(out_positions) - before,
                'triangles': tri_count,
            })
    return normalize_positions(out_positions), out_colors, prims


def writable_dirs():
    out = []
    for d in OUT_ROOTS:
        try:
            d.mkdir(parents=True, exist_ok=True)
            probe = d / '.write_probe'
            probe.write_text('ok')
            probe.unlink(missing_ok=True)
            out.append(d)
        except Exception:
            pass
    return out


def write_cache(model: Path, positions, colors, prims):
    dirs = writable_dirs()
    if not dirs:
        raise RuntimeError('no writable SOLUM mesh cache dir')
    payload = bytearray()
    payload += b'SOLMESH1'
    payload += struct.pack('<II', len(positions), 0)
    for (x, y, z), (r, g, b) in zip(positions, colors):
        payload += struct.pack('<ffffff', x, y, z, r, g, b)
    manifest = {
        'schema': 'solum.active_mesh_cache',
        'schemaVersion': 2,
        'status': 'mesh_cache_ready',
        'sourceModel': str(model),
        'fileName': model.name,
        'format': 'SOLMESH1',
        'vertexLayout': 'Vertex3D_px_py_pz_r_g_b_float32',
        'vertexCount': len(positions),
        'triangleCount': len(positions) // 3,
        'primitiveCount': len(prims),
        'primitives': prims,
        'note': 'P14A renders real GLB mesh geometry with debug material colors; textures/PBR are P15+',
    }
    for d in dirs:
        (d / 'active_mesh_v1.bin').write_bytes(payload)
        (d / 'active_mesh_manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    DIAG_DIR.mkdir(parents=True, exist_ok=True)
    (DIAG_DIR / 'runtime_mesh_cache_state.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    return dirs[0] / 'active_mesh_v1.bin', manifest


def main():
    model = find_model()
    DIAG_DIR.mkdir(parents=True, exist_ok=True)
    if not model:
        state = {
            'schema': 'solum.active_mesh_cache',
            'schemaVersion': 2,
            'status': 'no_model_found',
            'searchRoots': [str(r) for r in ROOTS],
        }
        (DIAG_DIR / 'runtime_mesh_cache_state.json').write_text(json.dumps(state, indent=2), encoding='utf-8')
        print('SOLUM MESH CACHE: NO_MODEL')
        return 2
    if model.suffix.lower() == '.glb':
        doc, blob = parse_glb(model)
    else:
        doc, blob = parse_gltf(model)
    positions, colors, prims = build_mesh(doc, blob)
    if len(positions) < 3:
        raise RuntimeError('no triangle vertices extracted from model')
    mesh_path, manifest = write_cache(model, positions, colors, prims)
    print(f"SOLUM MESH CACHE: OK model={model.name} vertices={manifest['vertexCount']} triangles={manifest['triangleCount']} cache={mesh_path}")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
