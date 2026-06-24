#!/usr/bin/env python3
import argparse, json, os, random, struct, sys, time, tracemalloc
from pathlib import Path

HEADER_FMT = "<4sHHHHIQQQQQQ"
HEADER_SIZE = struct.calcsize(HEADER_FMT)
CHUNK_FMT = "<4sHBBIIIIII"
CHUNK_SIZE = struct.calcsize(CHUNK_FMT)

MAGIC = b"SLPK"
VERSION = 1
FLAG_HAS_STRING_POOL = 1
FLAG_REQUIRED = 1
COMP_NONE = 0
ALIGN = 64

REQUIRED_TYPES = {b"MANI", b"SCNE"}
KNOWN_SCHEMA = {
    b"MANI": 1, b"SCNE": 1, b"GLB ": 1, b"MESH": 1,
    b"MAT ": 1, b"TEX ": 1, b"AUD ": 1, b"GRPH": 1,
    b"ANIM": 1, b"DBGI": 1, b"DEPS": 1,
}

def rotl32(x, r):
    return ((x << r) & 0xffffffff) | (x >> (32 - r))

def rotl64(x, r):
    return ((x << r) & 0xffffffffffffffff) | (x >> (64 - r))

def xxh32(data: bytes, seed=0):
    P1=0x9E3779B1; P2=0x85EBCA77; P3=0xC2B2AE3D; P4=0x27D4EB2F; P5=0x165667B1
    n=len(data); i=0
    if n >= 16:
        v1=(seed+P1+P2)&0xffffffff; v2=(seed+P2)&0xffffffff; v3=seed&0xffffffff; v4=(seed-P1)&0xffffffff
        limit=n-16
        while i <= limit:
            d=struct.unpack_from("<I", data, i)[0]; i+=4; v1=(v1+d*P2)&0xffffffff; v1=rotl32(v1,13); v1=(v1*P1)&0xffffffff
            d=struct.unpack_from("<I", data, i)[0]; i+=4; v2=(v2+d*P2)&0xffffffff; v2=rotl32(v2,13); v2=(v2*P1)&0xffffffff
            d=struct.unpack_from("<I", data, i)[0]; i+=4; v3=(v3+d*P2)&0xffffffff; v3=rotl32(v3,13); v3=(v3*P1)&0xffffffff
            d=struct.unpack_from("<I", data, i)[0]; i+=4; v4=(v4+d*P2)&0xffffffff; v4=rotl32(v4,13); v4=(v4*P1)&0xffffffff
        h=(rotl32(v1,1)+rotl32(v2,7)+rotl32(v3,12)+rotl32(v4,18))&0xffffffff
    else:
        h=(seed+P5)&0xffffffff
    h=(h+n)&0xffffffff
    while i+4 <= n:
        k=struct.unpack_from("<I", data, i)[0]; i+=4
        h=(h+k*P3)&0xffffffff; h=rotl32(h,17); h=(h*P4)&0xffffffff
    while i < n:
        h=(h+data[i]*P5)&0xffffffff; i+=1
        h=rotl32(h,11); h=(h*P1)&0xffffffff
    h ^= h >> 15; h=(h*P2)&0xffffffff
    h ^= h >> 13; h=(h*P3)&0xffffffff
    h ^= h >> 16
    return h & 0xffffffff

def xxh64(data: bytes, seed=0):
    P1=11400714785074694791; P2=14029467366897019727; P3=1609587929392839161
    P4=9650029242287828579; P5=2870177450012600261
    mask=0xffffffffffffffff
    n=len(data); i=0
    def rnd(acc, inp):
        acc=(acc+inp*P2)&mask; acc=rotl64(acc,31); acc=(acc*P1)&mask; return acc
    def merge(acc, val):
        val=rnd(0, val); acc ^= val; acc=(acc*P1+P4)&mask; return acc
    if n >= 32:
        v1=(seed+P1+P2)&mask; v2=(seed+P2)&mask; v3=seed&mask; v4=(seed-P1)&mask
        limit=n-32
        while i <= limit:
            v1=rnd(v1, struct.unpack_from("<Q", data, i)[0]); i+=8
            v2=rnd(v2, struct.unpack_from("<Q", data, i)[0]); i+=8
            v3=rnd(v3, struct.unpack_from("<Q", data, i)[0]); i+=8
            v4=rnd(v4, struct.unpack_from("<Q", data, i)[0]); i+=8
        h=(rotl64(v1,1)+rotl64(v2,7)+rotl64(v3,12)+rotl64(v4,18))&mask
        h=merge(h,v1); h=merge(h,v2); h=merge(h,v3); h=merge(h,v4)
    else:
        h=(seed+P5)&mask
    h=(h+n)&mask
    while i+8 <= n:
        k=rnd(0, struct.unpack_from("<Q", data, i)[0]); i+=8
        h ^= k; h=(rotl64(h,27)*P1+P4)&mask
    if i+4 <= n:
        h ^= struct.unpack_from("<I", data, i)[0]*P1; i+=4
        h=(rotl64(h,23)*P2+P3)&mask
    while i < n:
        h ^= data[i]*P5; i+=1
        h=(rotl64(h,11)*P1)&mask
    h ^= h >> 33; h=(h*P2)&mask
    h ^= h >> 29; h=(h*P3)&mask
    h ^= h >> 32
    return h & mask

def align(n, a=ALIGN):
    return (n + a - 1) // a * a

class SolumPackageError(Exception):
    def __init__(self, code, msg):
        super().__init__(f"{code}: {msg}")
        self.code = code
        self.msg = msg

class StringPool:
    def __init__(self):
        self.buf = bytearray(b"\x00")
        self.map = {"": 0}
    def add(self, s: str):
        if s in self.map:
            return self.map[s]
        off = len(self.buf)
        self.buf += s.encode("utf-8") + b"\x00"
        self.map[s] = off
        return off
    def bytes(self):
        return bytes(self.buf)

def read_cstr(pool: bytes, off: int):
    if off < 0 or off >= len(pool):
        return ""
    end = pool.find(b"\x00", off)
    if end < 0:
        end = len(pool)
    return pool[off:end].decode("utf-8", errors="replace")

def make_sample_chunks(n_objects=500, n_materials=64, n_textures=40, n_graph_nodes=256, n_graph_links=480, seed=42):
    rng = random.Random(seed)
    sp = StringPool()
    package_name = sp.add("Solum MVP Test Scene")
    author = sp.add("SOLUM_F5A_REFERENCE")
    mani = struct.pack("<IIIIIII", package_name, author, n_objects, n_materials, n_textures, n_graph_nodes, n_graph_links)

    mat_names = [sp.add(f"mat_{i % 32:03d}") for i in range(32)]
    tex_names = [sp.add(f"tex_{i % 24:03d}_bc.ktx2") for i in range(24)]

    scne = bytearray()
    scne += struct.pack("<I", n_objects)
    total_tris = 0
    for i in range(n_objects):
        name_off = sp.add(f"obj_{i:05d}") if i < 40 else sp.add(f"inst_{i % 128:03d}")
        mesh_id = rng.randrange(80)
        mat_id = rng.randrange(32)
        flags = 0
        x, y, z = rng.uniform(-80, 80), rng.uniform(-6, 6), rng.uniform(-80, 80)
        sx, sy, sz = rng.uniform(0.8, 1.2), rng.uniform(0.8, 1.2), rng.uniform(0.8, 1.2)
        tris = rng.randrange(400, 24000)
        total_tris += tris
        scne += struct.pack("<IIIIffffffI", name_off, mesh_id, mat_id, flags, x, y, z, sx, sy, sz, tris)

    mat = bytearray()
    mat += struct.pack("<I", n_materials)
    for i in range(n_materials):
        name_off = mat_names[i % len(mat_names)]
        base = i % n_textures
        normal = (i + 1) % n_textures
        flags = 1 if i % 13 == 0 else 0
        rough = 0.25 + (i % 50) / 100.0
        metal = 1.0 if i % 17 == 0 else 0.0
        mat += struct.pack("<IIIIff", name_off, base, normal, flags, rough, metal)

    tex = bytearray()
    tex += struct.pack("<I", n_textures)
    total_tex_bytes = 0
    for i in range(n_textures):
        name_off = tex_names[i % len(tex_names)]
        w = [512, 1024, 2048][i % 3]
        h = w
        fmt = 2
        bytes_est = w * h // 2
        total_tex_bytes += bytes_est
        tex += struct.pack("<IIIII", name_off, w, h, fmt, bytes_est)

    payload = bytearray()
    nodes = bytearray()
    links = bytearray()
    out_links = [[] for _ in range(n_graph_nodes)]
    for j in range(n_graph_links):
        a = rng.randrange(n_graph_nodes)
        b = rng.randrange(n_graph_nodes)
        out_links[a].append((j, b))
        links += struct.pack("<IHIH", a, rng.randrange(4), b, rng.randrange(4))
    for i in range(n_graph_nodes):
        node_name = sp.add(f"node_{i % 96:03d}")
        node_type = i % 32
        flags = 0
        payload_off = len(payload)
        node_payload = struct.pack("<Iff", i, rng.random(), rng.random()) + bytes([node_type]) * (4 + (i % 19))
        payload += node_payload
        first = out_links[i][0][0] if out_links[i] else 0xffffffff
        count = len(out_links[i])
        nodes += struct.pack("<IHHIIIHH", node_name, node_type, flags, payload_off, len(node_payload), first, count, 0)

    graph_type = 4
    grph_header = struct.pack("<HHIIIII", graph_type, 0, n_graph_nodes, n_graph_links, 24, 24 + len(nodes), 24 + len(nodes) + len(links))
    grph = grph_header + bytes(nodes) + bytes(links) + bytes(payload)

    glb = b"glTF" + b"\x02\x00\x00\x00" + os.urandom(4096)
    dbgi = json.dumps({"source": "synthetic", "note": "debug chunk, not runtime source"}).encode("utf-8")
    deps = struct.pack("<I", 0)

    chunks = [
        {"type": b"MANI", "schema": 1, "flags": FLAG_REQUIRED, "data": mani, "name_off": package_name},
        {"type": b"SCNE", "schema": 1, "flags": FLAG_REQUIRED, "data": bytes(scne), "name_off": sp.add("scene_objects")},
        {"type": b"GLB ", "schema": 1, "flags": 0, "data": glb, "name_off": sp.add("bridge_model.glb")},
        {"type": b"MAT ", "schema": 1, "flags": 0, "data": bytes(mat), "name_off": sp.add("materials")},
        {"type": b"TEX ", "schema": 1, "flags": 0, "data": bytes(tex), "name_off": sp.add("textures")},
        {"type": b"GRPH", "schema": 1, "flags": 0, "data": grph, "name_off": sp.add("uds_weather_graph_mock")},
        {"type": b"DBGI", "schema": 1, "flags": 0, "data": dbgi, "name_off": sp.add("debug_info")},
        {"type": b"DEPS", "schema": 1, "flags": 0, "data": deps, "name_off": sp.add("dependencies_empty")},
    ]
    expected = {
        "object_count": n_objects,
        "material_count": n_materials,
        "texture_count": n_textures,
        "graph_node_count": n_graph_nodes,
        "graph_link_count": n_graph_links,
        "total_triangle_count": total_tris,
        "texture_memory_bytes_est": total_tex_bytes,
        "string_pool_bytes": len(sp.bytes()),
    }
    return chunks, sp.bytes(), expected

def write_package(chunks, string_pool):
    n = len(chunks)
    data_start = align(HEADER_SIZE + n * CHUNK_SIZE)
    offset = data_start
    entries = []
    payloads = bytearray()
    for ch in chunks:
        pad = offset - (data_start + len(payloads))
        if pad:
            payloads += b"\x00" * pad
        data = ch["data"]
        entries.append({
            **ch,
            "offset": offset,
            "compressed_size": len(data),
            "uncompressed_size": len(data),
            "hash32": xxh32(data),
            "compression": COMP_NONE,
        })
        payloads += data
        offset = align(offset + len(data))
    string_pool_offset = align(data_start + len(payloads))
    payloads += b"\x00" * (string_pool_offset - (data_start + len(payloads)))
    payloads += string_pool
    file_size = string_pool_offset + len(string_pool)

    table = bytearray()
    for e in entries:
        table += struct.pack(CHUNK_FMT, e["type"], e["schema"], e["compression"], e["flags"], e["offset"],
                             e["compressed_size"], e["uncompressed_size"], e["hash32"], e.get("name_off", 0), 0)

    body = bytes(table) + b"\x00" * (data_start - HEADER_SIZE - len(table)) + bytes(payloads)
    header = struct.pack(HEADER_FMT, MAGIC, HEADER_SIZE, VERSION, CHUNK_SIZE, FLAG_HAS_STRING_POOL, n,
                         HEADER_SIZE, string_pool_offset, len(string_pool), file_size, xxh64(body), 0)
    return header + body

def parse_header(data):
    if len(data) < HEADER_SIZE:
        raise SolumPackageError("ERR_TRUNCATED", "file shorter than header")
    magic, header_size, version, entry_size, flags, n_chunks, table_off, sp_off, sp_size, file_size, content_hash, reserved = struct.unpack_from(HEADER_FMT, data, 0)
    if magic != MAGIC:
        raise SolumPackageError("ERR_BAD_MAGIC", f"magic={magic!r}")
    if header_size != HEADER_SIZE:
        raise SolumPackageError("ERR_BAD_HEADER_SIZE", str(header_size))
    if version > VERSION:
        raise SolumPackageError("ERR_UNSUPPORTED_VERSION", str(version))
    if entry_size != CHUNK_SIZE:
        raise SolumPackageError("ERR_BAD_CHUNK_ENTRY_SIZE", str(entry_size))
    if file_size != len(data):
        raise SolumPackageError("ERR_FILE_SIZE_MISMATCH", f"header={file_size} actual={len(data)}")
    if table_off != HEADER_SIZE:
        raise SolumPackageError("ERR_BAD_TABLE_OFFSET", str(table_off))
    return {"n_chunks": n_chunks, "table_off": table_off, "string_pool_offset": sp_off, "string_pool_size": sp_size, "file_size": file_size, "content_hash64": content_hash}

def read_entries(data, header, validate_hash=True):
    table_end = header["table_off"] + header["n_chunks"] * CHUNK_SIZE
    if table_end > len(data):
        raise SolumPackageError("ERR_TRUNCATED", "chunk table out of file")
    if validate_hash and xxh64(data[header["table_off"]:]) != header["content_hash64"]:
        raise SolumPackageError("ERR_CONTENT_HASH_MISMATCH", "content_hash64 mismatch")
    entries = []
    seen_required = set()
    for i in range(header["n_chunks"]):
        off = header["table_off"] + i * CHUNK_SIZE
        typ, schema, comp, flags, payload_off, csize, usize, h32, name_off, reserved = struct.unpack_from(CHUNK_FMT, data, off)
        if payload_off % ALIGN != 0:
            raise SolumPackageError("ERR_BAD_ALIGNMENT", f"{typ!r} offset={payload_off}")
        if payload_off + csize > len(data):
            raise SolumPackageError("ERR_CHUNK_OOB", f"{typ!r} offset={payload_off} size={csize}")
        if comp != COMP_NONE:
            raise SolumPackageError("ERR_UNSUPPORTED_COMPRESSION", f"{typ!r} comp={comp}")
        known = KNOWN_SCHEMA.get(typ)
        if known is None:
            if flags & FLAG_REQUIRED:
                raise SolumPackageError("ERR_UNKNOWN_REQUIRED_CHUNK", repr(typ))
            entries.append({"type": typ, "schema": schema, "flags": flags, "offset": payload_off, "size": csize, "name_off": name_off, "unknown": True})
            continue
        if schema > known and (flags & FLAG_REQUIRED):
            raise SolumPackageError("ERR_UNSUPPORTED_SCHEMA", f"{typ!r} schema={schema}")
        payload = data[payload_off:payload_off + csize]
        if validate_hash and xxh32(payload) != h32:
            raise SolumPackageError("ERR_HASH_MISMATCH", f"{typ.decode(errors='replace')} hash mismatch")
        if flags & FLAG_REQUIRED:
            seen_required.add(typ)
        entries.append({"type": typ, "schema": schema, "flags": flags, "offset": payload_off, "size": csize, "hash32": h32, "name_off": name_off, "unknown": False})
    missing = REQUIRED_TYPES - seen_required
    if missing:
        raise SolumPackageError("ERR_MISSING_CHUNK", ",".join(t.decode().strip() for t in missing))
    sp_off, sp_size = header["string_pool_offset"], header["string_pool_size"]
    if sp_off + sp_size > len(data):
        raise SolumPackageError("ERR_STRING_POOL_OOB", "string pool out of file")
    return entries, data[sp_off:sp_off + sp_size]

def read_package(data, validate_hash=True):
    h = parse_header(data)
    e, p = read_entries(data, h, validate_hash)
    return h, e, p

def dump_summary(data, fast=False):
    header = parse_header(data)
    entries, pool = read_entries(data, header, validate_hash=not fast)
    chunks = {e["type"]: e for e in entries if not e.get("unknown")}

    def payload(typ):
        e = chunks[typ]
        return data[e["offset"]:e["offset"] + e["size"]]

    mani = payload(b"MANI")
    name_off, author_off, obj_count, mat_count, tex_count, node_count, link_count = struct.unpack_from("<IIIIIII", mani, 0)

    base = {
        "package_name": read_cstr(pool, name_off),
        "author": read_cstr(pool, author_off),
        "object_count": obj_count,
        "material_count": mat_count,
        "texture_count": tex_count,
        "graph_node_count": node_count,
        "graph_link_count": link_count,
        "chunk_count": header["n_chunks"],
        "string_pool_bytes": header["string_pool_size"],
        "file_size": header["file_size"],
        "chunk_types": [e["type"].decode(errors="replace") for e in entries],
    }

    if fast:
        base["mode"] = "fast_header_manifest_only"
        base["graph_type"] = None
        if b"GRPH" in chunks:
            grph = payload(b"GRPH")
            if len(grph) >= 24:
                graph_type, _, n_nodes, n_links, node_off, link_off, payload_off = struct.unpack_from("<HHIIIII", grph, 0)
                base["graph_type"] = graph_type
        base["total_triangle_count"] = None
        base["texture_memory_bytes_est"] = None
        return base

    scne = payload(b"SCNE")
    n_obj = struct.unpack_from("<I", scne, 0)[0]
    total_tris = 0
    rec_size = struct.calcsize("<IIIIffffffI")
    for i in range(n_obj):
        total_tris += struct.unpack_from("<I", scne, 4 + i * rec_size + rec_size - 4)[0]

    tex = payload(b"TEX ")
    n_tex = struct.unpack_from("<I", tex, 0)[0]
    tex_rec = struct.calcsize("<IIIII")
    tex_bytes = 0
    for i in range(n_tex):
        tex_bytes += struct.unpack_from("<I", tex, 4 + i * tex_rec + 16)[0]

    graph_type = None
    if b"GRPH" in chunks:
        grph = payload(b"GRPH")
        if len(grph) >= 24:
            graph_type, _, n_nodes, n_links, node_off, link_off, payload_off = struct.unpack_from("<HHIIIII", grph, 0)

    base["mode"] = "full_summary"
    base["graph_type"] = graph_type
    base["total_triangle_count"] = total_tris
    base["texture_memory_bytes_est"] = tex_bytes
    return base


def create_sample(path):
    chunks, pool, expected = make_sample_chunks()
    Path(path).write_bytes(write_package(chunks, pool))
    return expected

def validate_file(path):
    data = Path(path).read_bytes()
    h, e, p = read_package(data, True)
    return {"ok": True, "chunk_count": len(e), "chunk_types": [x["type"].decode(errors="replace") for x in e], "string_pool_bytes": len(p)}

def patch_header(data, **kwargs):
    vals = list(struct.unpack_from(HEADER_FMT, data, 0))
    names = ["magic", "header_size", "version", "entry_size", "flags", "n_chunks", "table_off", "sp_off", "sp_size", "file_size", "content_hash", "reserved"]
    for k, v in kwargs.items():
        vals[names.index(k)] = v
    ba = bytearray(data)
    ba[:HEADER_SIZE] = struct.pack(HEADER_FMT, *vals)
    return bytes(ba)

def patch_chunk(data, index, **kwargs):
    off = HEADER_SIZE + index * CHUNK_SIZE
    vals = list(struct.unpack_from(CHUNK_FMT, data, off))
    names = ["type", "schema", "compression", "flags", "offset", "csize", "usize", "hash32", "name_off", "reserved"]
    for k, v in kwargs.items():
        vals[names.index(k)] = v
    ba = bytearray(data)
    ba[off:off + CHUNK_SIZE] = struct.pack(CHUNK_FMT, *vals)
    hvals = list(struct.unpack_from(HEADER_FMT, ba, 0))
    hvals[10] = xxh64(bytes(ba[HEADER_SIZE:]))
    ba[:HEADER_SIZE] = struct.pack(HEADER_FMT, *hvals)
    return bytes(ba)

def update_header_content_hash(data):
    ba = bytearray(data)
    hvals = list(struct.unpack_from(HEADER_FMT, ba, 0))
    hvals[10] = xxh64(bytes(ba[HEADER_SIZE:]))
    ba[:HEADER_SIZE] = struct.pack(HEADER_FMT, *hvals)
    return bytes(ba)

def run_selftest(out_dir):
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    sample = out / "sample_scene.slpk"
    expected = create_sample(sample)
    data = sample.read_bytes()
    summary = dump_summary(data)
    (out / "sample_scene_dump.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False))
    _, entries, pool = read_package(data)
    idx = {e["type"]: i for i, e in enumerate(entries) if not e.get("unknown")}
    scne_i = idx[b"SCNE"]
    scne_e = entries[scne_i]

    def expect_ok(name, func):
        t0 = time.perf_counter()
        try:
            detail = func()
            return {"name": name, "status": "PASS", "ms": (time.perf_counter() - t0) * 1000, "detail": str(detail)}
        except Exception as e:
            return {"name": name, "status": "FAIL", "ms": (time.perf_counter() - t0) * 1000, "detail": str(e)}

    def expect_error(name, code, blob):
        t0 = time.perf_counter()
        try:
            read_package(blob)
            return {"name": name, "status": "FAIL", "ms": (time.perf_counter() - t0) * 1000, "detail": f"expected {code}, got OK"}
        except SolumPackageError as e:
            return {"name": name, "status": "PASS" if e.code == code else "FAIL", "ms": (time.perf_counter() - t0) * 1000, "detail": e.code}

    bad_magic = b"BAD!" + data[4:]
    truncated = data[:-17]
    missing_scne = patch_chunk(data, scne_i, type=b"XXXX", flags=0)
    oob = patch_chunk(data, scne_i, offset=align(len(data) + 1024))
    bad_alignment = patch_chunk(data, scne_i, offset=scne_e["offset"] + 1)
    bad_payload = bytearray(data)
    bad_payload[scne_e["offset"] + 12] ^= 0x23
    hash_mismatch = update_header_content_hash(bytes(bad_payload))
    unsupported_version = patch_header(data, version=99)
    unsupported_schema = patch_chunk(data, scne_i, schema=99, flags=FLAG_REQUIRED)

    chunks, string_pool, _ = make_sample_chunks()
    extra_chunks = chunks + [{"type": b"ZZZZ", "schema": 1, "flags": 0, "data": b"optional payload", "name_off": 0}]
    unknown_optional = write_package(extra_chunks, string_pool)

    tests = []
    tests.append(expect_ok("valid_package_opens", lambda: f"{dump_summary(data)['object_count']} objects"))
    tests.append(expect_error("bad_magic", "ERR_BAD_MAGIC", bad_magic))
    tests.append(expect_error("truncated_file", "ERR_FILE_SIZE_MISMATCH", truncated))
    tests.append(expect_error("missing_required_SCNE", "ERR_MISSING_CHUNK", missing_scne))
    tests.append(expect_error("chunk_out_of_bounds", "ERR_CHUNK_OOB", oob))
    tests.append(expect_error("bad_alignment", "ERR_BAD_ALIGNMENT", bad_alignment))
    tests.append(expect_error("chunk_hash_mismatch", "ERR_HASH_MISMATCH", hash_mismatch))
    tests.append(expect_error("unsupported_container_version", "ERR_UNSUPPORTED_VERSION", unsupported_version))
    tests.append(expect_ok("unknown_optional_chunk_skips", lambda: f"{len(read_package(unknown_optional)[1])} chunks"))
    tests.append(expect_error("unsupported_required_schema", "ERR_UNSUPPORTED_SCHEMA", unsupported_schema))
    tests.append(expect_ok("dump_summary_equals_expected", lambda: "ok" if summary["object_count"] == expected["object_count"] else "mismatch"))
    tests.append(expect_ok("string_pool_dedup_small", lambda: f"{len(pool)} bytes"))
    tests.append(expect_ok("graph_payload_roundtrip_counts", lambda: f"{summary['graph_node_count']} nodes/{summary['graph_link_count']} links"))

    tracemalloc.start()
    t0 = time.perf_counter()
    for _ in range(500):
        dump_summary(data, fast=True)
    avg = (time.perf_counter() - t0) * 1000 / 500
    cur, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    tests.append({"name": "quick_summary_read_speed_python_reference", "status": "PASS" if avg < 1.0 else "WARN", "ms": avg, "detail": f"avg_ms={avg:.4f} peak_kb={peak/1024:.1f}"})

    result = {"format": "SOLUM Package MVP Reference v1", "header_size": HEADER_SIZE, "chunk_entry_size": CHUNK_SIZE,
              "alignment": ALIGN, "sample_package": str(sample), "summary": summary, "expected": expected, "tests": tests,
              "pass_count": sum(1 for t in tests if t["status"] == "PASS"),
              "warn_count": sum(1 for t in tests if t["status"] == "WARN"),
              "fail_count": sum(1 for t in tests if t["status"] == "FAIL")}
    (out / "solum_package_mvp_test_results.json").write_text(json.dumps(result, indent=2, ensure_ascii=False))
    report = ["# SOLUM Package MVP Test Report", "", f"PASS={result['pass_count']} WARN={result['warn_count']} FAIL={result['fail_count']}",
              f"sample_package={sample}", f"objects={summary['object_count']} materials={summary['material_count']} textures={summary['texture_count']}",
              f"graph={summary['graph_node_count']} nodes / {summary['graph_link_count']} links", f"triangles={summary['total_triangle_count']}", ""]
    for t in tests:
        report.append(f"- {t['name']}: {t['status']} — {t['detail']} ({t['ms']:.4f} ms)")
    (out / "solum_package_mvp_test_report.md").write_text("\n".join(report))
    return result

def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("create-sample"); p.add_argument("output")
    p = sub.add_parser("validate"); p.add_argument("package")
    p = sub.add_parser("summary"); p.add_argument("package"); p.add_argument("--fast", action="store_true")
    p = sub.add_parser("dump"); p.add_argument("package"); p.add_argument("output_json")
    p = sub.add_parser("selftest"); p.add_argument("--out", default="build/solum_package_mvp")
    args = ap.parse_args()

    if args.cmd == "create-sample":
        print(json.dumps({"created": args.output, "expected": create_sample(args.output)}, indent=2))
    elif args.cmd == "validate":
        try:
            print(json.dumps(validate_file(args.package), indent=2))
        except SolumPackageError as e:
            print(json.dumps({"ok": False, "code": e.code, "message": e.msg}, indent=2))
            sys.exit(2)
    elif args.cmd == "summary":
        print(json.dumps(dump_summary(Path(args.package).read_bytes(), fast=args.fast), indent=2, ensure_ascii=False))
    elif args.cmd == "dump":
        Path(args.output_json).write_text(json.dumps(dump_summary(Path(args.package).read_bytes()), indent=2, ensure_ascii=False))
        print(args.output_json)
    elif args.cmd == "selftest":
        result = run_selftest(args.out)
        print(json.dumps(result, indent=2, ensure_ascii=False))
        if result["fail_count"]:
            sys.exit(1)

if __name__ == "__main__":
    main()
