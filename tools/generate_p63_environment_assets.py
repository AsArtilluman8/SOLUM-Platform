#!/usr/bin/env python3
"""Generate deterministic, SOLUM-owned P63 Filament assets without external dependencies."""

from __future__ import annotations

import json
import math
import random
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "apps" / "engine" / "src" / "main" / "assets" / "env" / "p63"
SEED = 1597463007


def align4(data: bytearray) -> None:
    while len(data) % 4:
        data.append(0)


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    body = kind + payload
    return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)


def moon_png(size: int = 128) -> bytes:
    rng = random.Random(SEED ^ 0x4D4F4F4E)
    craters = [(rng.uniform(-0.75, 0.75), rng.uniform(-0.75, 0.75), rng.uniform(0.035, 0.19), rng.uniform(0.08, 0.28)) for _ in range(38)]
    rows = bytearray()
    for y in range(size):
        rows.append(0)
        fy = (y + 0.5) / size * 2.0 - 1.0
        for x in range(size):
            fx = (x + 0.5) / size * 2.0 - 1.0
            radius = math.hypot(fx, fy)
            if radius > 1.0:
                rows.extend((0, 0, 0, 0))
                continue
            sphere = math.sqrt(max(0.0, 1.0 - radius * radius))
            value = 0.54 + sphere * 0.28 + 0.035 * math.sin(fx * 27.0 + fy * 13.0)
            for cx, cy, cr, strength in craters:
                distance = math.hypot(fx - cx, fy - cy)
                if distance < cr:
                    bowl = 1.0 - distance / cr
                    rim = math.exp(-((distance / cr - 0.86) / 0.09) ** 2)
                    value += rim * strength * 0.65 - bowl * strength
            value *= 0.82 + 0.18 * max(0.0, 0.45 * fx + sphere)
            base = max(0, min(255, int(value * 255)))
            rows.extend((base, max(0, base - 5), max(0, base - 13), 255))
    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + png_chunk(b"IEND", b"")


def halo_png(size: int = 64) -> bytes:
    """Smooth analytic halo alpha; deliberately contains no stipple/dither/checker branch."""
    rows = bytearray()
    for y in range(size):
        rows.append(0)
        fy = (y + 0.5) / size * 2.0 - 1.0
        for x in range(size):
            fx = (x + 0.5) / size * 2.0 - 1.0
            radius = math.hypot(fx, fy)
            alpha = max(0.0, min(1.0, 1.0 - radius))
            alpha = alpha * alpha * (3.0 - 2.0 * alpha)
            rows.extend((255, 244, 214, round(alpha * 255.0)))
    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + png_chunk(b"IEND", b"")


def encode_rgba_png(width: int, height: int, pixels: bytes) -> bytes:
    require_size = width * height * 4
    if len(pixels) != require_size:
        raise ValueError(f"rgba_size_mismatch_{len(pixels)}_{require_size}")
    rows = bytearray()
    stride = width * 4
    for y in range(height):
        rows.append(0)
        rows.extend(pixels[y * stride:(y + 1) * stride])
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + png_chunk(b"IEND", b"")


def decode_png_rgba(payload: bytes) -> tuple[int, int, bytes]:
    """Decode the audited 8-bit non-interlaced RGB/RGBA source without external packages."""
    if not payload.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("moon_source_not_png")
    width = height = color_type = bit_depth = interlace = 0
    compressed = bytearray()
    offset = 8
    while offset + 12 <= len(payload):
        length = struct.unpack_from(">I", payload, offset)[0]
        kind = payload[offset + 4:offset + 8]
        data = payload[offset + 8:offset + 8 + length]
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", data)
        elif kind == b"IDAT":
            compressed.extend(data)
        elif kind == b"IEND":
            break
        offset += length + 12
    if bit_depth != 8 or color_type not in (2, 6) or interlace != 0:
        raise ValueError(f"moon_png_unsupported_{bit_depth}_{color_type}_{interlace}")
    channels = 3 if color_type == 2 else 4
    stride = width * channels
    raw = zlib.decompress(bytes(compressed))
    previous = bytearray(stride)
    decoded = bytearray()
    source_offset = 0
    for _ in range(height):
        filter_type = raw[source_offset]
        source_offset += 1
        scan = bytearray(raw[source_offset:source_offset + stride])
        source_offset += stride
        for x in range(stride):
            left = scan[x - channels] if x >= channels else 0
            above = previous[x]
            upper_left = previous[x - channels] if x >= channels else 0
            if filter_type == 1:
                scan[x] = (scan[x] + left) & 255
            elif filter_type == 2:
                scan[x] = (scan[x] + above) & 255
            elif filter_type == 3:
                scan[x] = (scan[x] + ((left + above) >> 1)) & 255
            elif filter_type == 4:
                p = left + above - upper_left
                pa, pb, pc = abs(p - left), abs(p - above), abs(p - upper_left)
                predictor = left if pa <= pb and pa <= pc else (above if pb <= pc else upper_left)
                scan[x] = (scan[x] + predictor) & 255
            elif filter_type != 0:
                raise ValueError(f"moon_png_filter_{filter_type}")
        for x in range(width):
            base = x * channels
            decoded.extend((scan[base], scan[base + 1], scan[base + 2], scan[base + 3] if channels == 4 else 255))
        previous = scan
    return width, height, bytes(decoded)


def smoothstep(edge0: float, edge1: float, value: float) -> float:
    t = max(0.0, min(1.0, (value - edge0) / max(1e-6, edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def moon_phase_png(source: tuple[int, int, bytes], phase: float) -> bytes:
    """Curved spherical terminator baked from the exact color texture; no dither or occluder."""
    width, height, pixels = source
    phase = max(0.0, min(1.0, phase))
    phase_angle = math.acos(phase * 2.0 - 1.0)
    light_x = math.sin(phase_angle)
    light_z = math.cos(phase_angle)
    edge = 3.0 / max(width, height)
    out = bytearray(width * height * 4)
    for y in range(height):
        fy = 1.0 - (y + 0.5) / height * 2.0
        for x in range(width):
            fx = (x + 0.5) / width * 2.0 - 1.0
            radius2 = fx * fx + fy * fy
            target = (y * width + x) * 4
            if radius2 >= 1.0:
                out[target:target + 4] = b"\x00\x00\x00\x00"
                continue
            sphere_z = math.sqrt(max(0.0, 1.0 - radius2))
            light = fx * light_x + sphere_z * light_z
            illumination = smoothstep(-edge, edge, light)
            # Preserve a restrained textured earthshine floor instead of a black phase object.
            shaded = 0.035 + illumination * 0.965
            limb_alpha = 1.0 - smoothstep(1.0 - edge * 1.8, 1.0, math.sqrt(radius2))
            source_index = target
            out[target] = round(pixels[source_index] * shaded)
            out[target + 1] = round(pixels[source_index + 1] * shaded)
            out[target + 2] = round(pixels[source_index + 2] * shaded)
            out[target + 3] = round(pixels[source_index + 3] * limb_alpha)
    return encode_rgba_png(width, height, bytes(out))


def sun_disc_png(size: int = 128) -> bytes:
    """SOLUM-native bright core with continuous soft falloff and no fullscreen flare."""
    pixels = bytearray(size * size * 4)
    for y in range(size):
        fy = (y + 0.5) / size * 2.0 - 1.0
        for x in range(size):
            fx = (x + 0.5) / size * 2.0 - 1.0
            radius = math.hypot(fx, fy)
            alpha = 1.0 - smoothstep(0.82, 1.0, radius)
            core = 1.0 - smoothstep(0.0, 0.82, radius)
            index = (y * size + x) * 4
            pixels[index:index + 4] = bytes((255, round(224 + core * 31), round(168 + core * 87), round(alpha * 255)))
    return encode_rgba_png(size, size, bytes(pixels))


def sky_png(slot: str, width: int = 256, height: int = 128) -> bytes:
    """Deterministic full-sphere mobile atmosphere; azimuth is periodic at the seam."""
    settings = {
        "dawn": ((0.015, 0.035, 0.11), (0.82, 0.32, 0.12), 0.66, 0.22, -0.06),
        "day": ((0.06, 0.30, 0.78), (0.58, 0.74, 0.92), 0.36, 0.08, 0.38),
        "sunset": ((0.025, 0.055, 0.16), (0.98, 0.27, 0.055), 0.78, 0.34, -0.02),
        "twilight": ((0.018, 0.035, 0.105), (0.38, 0.14, 0.22), 0.54, 0.20, -0.12),
        "night": ((0.004, 0.009, 0.032), (0.018, 0.035, 0.085), 0.20, 0.04, -0.42),
    }
    zenith, horizon, rayleigh, mie, sun_elevation = settings[slot]
    rows = bytearray()
    for y in range(height):
        rows.append(0)
        v = (y + 0.5) / height
        elevation = math.pi * 0.5 - v * math.pi
        sin_elevation = math.sin(elevation)
        altitude = max(0.0, min(1.0, (sin_elevation + 0.10) / 0.90))
        horizon_band = math.exp(-abs(sin_elevation) * 7.5)
        lower_continuation = max(0.0, -sin_elevation)
        for x in range(width):
            azimuth = ((x + 0.5) / width) * math.tau - math.pi
            cos_gamma = (math.cos(elevation) * math.cos(sun_elevation) * math.cos(azimuth)
                         + math.sin(elevation) * math.sin(sun_elevation))
            cos_gamma = max(-1.0, min(1.0, cos_gamma))
            rayleigh_phase = 0.75 * (1.0 + cos_gamma * cos_gamma)
            g = 0.76
            mie_phase = (1.0 - g * g) / max(0.035, (1.0 + g * g - 2.0 * g * cos_gamma) ** 1.5)
            sun_glow = math.exp((cos_gamma - 1.0) * 42.0)
            base = [horizon[i] * (1.0 - altitude) + zenith[i] * altitude for i in range(3)]
            scatter = rayleigh * rayleigh_phase * 0.055 + mie * mie_phase * 0.012 + sun_glow * mie * 0.44
            warm = (1.0, 0.55, 0.25) if slot in ("dawn", "sunset") else (0.72, 0.84, 1.0)
            # The lower hemisphere remains atmospheric instead of becoming a flat grey cap.
            lower_haze = lower_continuation * (0.30 if slot != "night" else 0.10)
            rgb = [base[i] + scatter * warm[i] + horizon_band * horizon[i] * 0.12 for i in range(3)]
            if lower_continuation > 0.0:
                rgb = [rgb[i] * (1.0 - lower_haze) + horizon[i] * lower_haze for i in range(3)]
            rows.extend(max(0, min(255, round((max(0.0, min(1.0, c))) ** (1.0 / 2.2) * 255.0))) for c in rgb)
            rows.append(255)
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", header) + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + png_chunk(b"IEND", b"")


@dataclass
class Mesh:
    positions: list[tuple[float, float, float]]
    normals: list[tuple[float, float, float]]
    uvs: list[tuple[float, float]]
    indices: list[int]


def merge(parts: list[tuple[Mesh, tuple[float, float, float], tuple[float, float, float]]]) -> Mesh:
    pos: list[tuple[float, float, float]] = []
    normals: list[tuple[float, float, float]] = []
    uvs: list[tuple[float, float]] = []
    indices: list[int] = []
    for mesh, offset, scale in parts:
        base = len(pos)
        ox, oy, oz = offset
        sx, sy, sz = scale
        pos.extend((x * sx + ox, y * sy + oy, z * sz + oz) for x, y, z in mesh.positions)
        normals.extend(mesh.normals)
        uvs.extend(mesh.uvs)
        indices.extend(base + index for index in mesh.indices)
    return Mesh(pos, normals, uvs, indices)


def box() -> Mesh:
    positions: list[tuple[float, float, float]] = []
    normals: list[tuple[float, float, float]] = []
    uvs: list[tuple[float, float]] = []
    indices: list[int] = []
    faces = [((0, 0, 1), [(-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1)]),
             ((0, 0, -1), [(1, -1, -1), (-1, -1, -1), (-1, 1, -1), (1, 1, -1)]),
             ((1, 0, 0), [(1, -1, 1), (1, -1, -1), (1, 1, -1), (1, 1, 1)]),
             ((-1, 0, 0), [(-1, -1, -1), (-1, -1, 1), (-1, 1, 1), (-1, 1, -1)]),
             ((0, 1, 0), [(-1, 1, 1), (1, 1, 1), (1, 1, -1), (-1, 1, -1)]),
             ((0, -1, 0), [(-1, -1, -1), (1, -1, -1), (1, -1, 1), (-1, -1, 1)])]
    for normal, vertices in faces:
        base = len(positions); positions.extend(vertices); normals.extend([normal] * 4); uvs.extend([(0, 0), (1, 0), (1, 1), (0, 1)])
        indices.extend((base, base + 1, base + 2, base, base + 2, base + 3))
    return Mesh(positions, normals, uvs, indices)


def plane() -> Mesh:
    return Mesh([(-1, 0, -1), (1, 0, -1), (1, 0, 1), (-1, 0, 1)], [(0, 1, 0)] * 4, [(0, 0), (1, 0), (1, 1), (0, 1)], [0, 1, 2, 0, 2, 3])


def plane_up() -> Mesh:
    """+Y normals with matching counter-clockwise winding when viewed from above."""
    return Mesh([(-1, 0, -1), (1, 0, -1), (1, 0, 1), (-1, 0, 1)], [(0, 1, 0)] * 4,
                [(0, 0), (1, 0), (1, 1), (0, 1)], [0, 2, 1, 0, 3, 2])


def disk(segments: int = 32) -> Mesh:
    positions = [(0.0, 0.0, 0.0)]
    normals = [(0.0, 0.0, 1.0)]
    uvs = [(0.5, 0.5)]
    for i in range(segments + 1):
        angle = i / segments * math.tau
        x, y = math.cos(angle), math.sin(angle)
        positions.append((x, y, 0.0)); normals.append((0.0, 0.0, 1.0)); uvs.append((x * 0.5 + 0.5, 1.0 - (y * 0.5 + 0.5)))
    indices: list[int] = []
    for i in range(segments): indices.extend((0, i + 1, i + 2))
    return Mesh(positions, normals, uvs, indices)


def sphere(segments: int = 10, rings: int = 6, outward_winding: bool = False) -> Mesh:
    positions: list[tuple[float, float, float]] = []
    normals: list[tuple[float, float, float]] = []
    uvs: list[tuple[float, float]] = []
    for ring in range(rings + 1):
        v = ring / rings; phi = v * math.pi
        for segment in range(segments + 1):
            u = segment / segments; theta = u * math.tau
            normal = (math.sin(phi) * math.cos(theta), math.cos(phi), math.sin(phi) * math.sin(theta))
            positions.append(normal); normals.append(normal); uvs.append((u, v))
    indices: list[int] = []
    stride = segments + 1
    for ring in range(rings):
        for segment in range(segments):
            a = ring * stride + segment; b = a + stride
            if outward_winding:
                indices.extend((a, a + 1, b, a + 1, b + 1, b))
            else:
                indices.extend((a, b, a + 1, a + 1, b, b + 1))
    return Mesh(positions, normals, uvs, indices)


def octahedron() -> Mesh:
    p = [(0, 1, 0), (1, 0, 0), (0, 0, 1), (-1, 0, 0), (0, 0, -1), (0, -1, 0)]
    faces = [(0,1,2),(0,2,3),(0,3,4),(0,4,1),(5,2,1),(5,3,2),(5,4,3),(5,1,4)]
    positions: list[tuple[float,float,float]]=[]; normals: list[tuple[float,float,float]]=[]; uvs: list[tuple[float,float]]=[]; indices: list[int]=[]
    for face in faces:
        base=len(positions); a,b,c=(p[i] for i in face); positions.extend((a,b,c));
        ux,uy,uz=(b[i]-a[i] for i in range(3)); vx,vy,vz=(c[i]-a[i] for i in range(3)); n=(uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx); length=math.sqrt(sum(v*v for v in n)) or 1; n=tuple(v/length for v in n)
        normals.extend((n,n,n));uvs.extend(((0.5,0),(1,1),(0,1)));indices.extend((base,base+1,base+2))
    return Mesh(positions,normals,uvs,indices)


def ring(segments: int = 32) -> Mesh:
    positions=[];normals=[];uvs=[];indices=[]
    for i in range(segments+1):
        angle=i/segments*math.tau;c,s=math.cos(angle),math.sin(angle)
        positions.extend(((c,0,s),(c*0.82,0,s*0.82)));normals.extend(((0,1,0),(0,1,0)));uvs.extend(((i/segments,0),(i/segments,1)))
    for i in range(segments):
        a=i*2;indices.extend((a,a+1,a+2,a+2,a+1,a+3))
    return Mesh(positions,normals,uvs,indices)


def rain_group(seed: int) -> Mesh:
    rng=random.Random(seed);parts=[];needle=box()
    for i in range(18):
        layer=i%3; radius=(0.006,0.011,0.018)[layer]; length=rng.uniform(0.32,0.75)*(1+layer*0.45)
        parts.append((needle,(rng.uniform(-1.35,1.35),rng.uniform(-6,6),rng.uniform(-1.35,1.35)),(radius,length,radius)))
    return merge(parts)


def snow_group(seed: int) -> Mesh:
    rng=random.Random(seed);parts=[];flake=octahedron()
    for i in range(15):
        layer=i%3; size=(0.025,0.045,0.075)[layer]*rng.uniform(0.75,1.3)
        parts.append((flake,(rng.uniform(-1.4,1.4),rng.uniform(-6,6),rng.uniform(-1.4,1.4)),(size,size*rng.uniform(0.35,0.75),size)))
    return merge(parts)


def dust_group(seed: int) -> Mesh:
    rng=random.Random(seed);parts=[];grain=octahedron()
    for _ in range(14):
        size=rng.uniform(0.012,0.038);parts.append((grain,(rng.uniform(-1.4,1.4),rng.uniform(-3,3),rng.uniform(-1.4,1.4)),(size,size,size)))
    return merge(parts)


def cloud_cluster(seed: int) -> Mesh:
    rng=random.Random(seed);parts=[];ball=sphere(8,5)
    for _ in range(9):
        size=rng.uniform(0.65,1.45);parts.append((ball,(rng.uniform(-2.2,2.2),rng.uniform(-0.35,0.45),rng.uniform(-1.0,1.0)),(size*1.45,size*0.55,size)))
    return merge(parts)


def star_group(seed: int, group: int, count: int = 72) -> Mesh:
    rng=random.Random(seed);parts=[];star=octahedron()
    for index in range(count):
        u=rng.random();v=rng.random();w=rng.random();az=u*math.tau;band=abs(v-0.5)*2; elevation=0.12+(1-band**1.8)*1.25+(w-0.5)*0.2;radius=34
        cos=math.cos(elevation);position=(math.sin(az)*cos*radius,math.sin(elevation)*radius,-math.cos(az)*cos*radius)
        size=0.026+rng.random()**4*0.12
        if index%3==group:parts.append((star,position,(size,size,size)))
    return merge(parts)


def lightning_mesh() -> Mesh:
    rng=random.Random(SEED ^ 0xB017);parts=[];segment=box();x=z=0.0
    for index in range(14):
        y=12.0-index*0.9
        if index:x+=rng.uniform(-0.55,0.55);z+=rng.uniform(-0.3,0.3)
        parts.append((segment,(x,y,z),(0.025,0.48,0.025)))
        if index in (5,8,10):parts.append((segment,(x+rng.uniform(-0.4,0.4),y-0.4,z+rng.uniform(-0.2,0.2)),(0.014,0.42,0.014)))
    return merge(parts)


class GlbBuilder:
    def __init__(self) -> None:
        self.binary=bytearray();self.buffer_views=[];self.accessors=[];self.meshes=[];self.nodes=[];self.materials=[];self.images=[];self.textures=[];self.samplers=[]

    def material(self,name:str,color:tuple[float,float,float,float],metallic:float,roughness:float,*,unlit=False,emissive=(0,0,0),alpha="OPAQUE",texture=None,emissive_texture=None,double_sided=False,force_single_sided=False)->int:
        pbr={"baseColorFactor":list(color),"metallicFactor":metallic,"roughnessFactor":roughness}
        if texture is not None:pbr["baseColorTexture"]={"index":texture}
        item={"name":name,"pbrMetallicRoughness":pbr,"emissiveFactor":list(emissive),"doubleSided":False if force_single_sided else double_sided or alpha!="OPAQUE"}
        if emissive_texture is not None:item["emissiveTexture"]={"index":emissive_texture}
        if alpha!="OPAQUE":item["alphaMode"]=alpha;item["alphaCutoff"]=0.08
        if unlit:item["extensions"]={"KHR_materials_unlit":{}}
        self.materials.append(item);return len(self.materials)-1

    def texture_png(self,payload:bytes,name:str="P63_Procedural_Moon_Craters",wrap:int=33071)->int:
        align4(self.binary);start=len(self.binary);self.binary.extend(payload);view=len(self.buffer_views);self.buffer_views.append({"buffer":0,"byteOffset":start,"byteLength":len(payload)})
        image=len(self.images);self.images.append({"bufferView":view,"mimeType":"image/png","name":name});sampler=len(self.samplers);self.samplers.append({"magFilter":9729,"minFilter":9987,"wrapS":wrap,"wrapT":33071});self.textures.append({"sampler":sampler,"source":image});return len(self.textures)-1

    def _accessor(self,values:list[tuple],fmt:str,component_type:int,kind:str,target:int)->int:
        align4(self.binary);start=len(self.binary)
        flat=[value for row in values for value in row] if values and isinstance(values[0],tuple) else list(values)
        self.binary.extend(struct.pack("<"+fmt*len(flat),*flat));view=len(self.buffer_views);self.buffer_views.append({"buffer":0,"byteOffset":start,"byteLength":len(self.binary)-start,"target":target})
        item={"bufferView":view,"componentType":component_type,"count":len(values),"type":kind}
        if kind=="VEC3":item["min"]=[min(v[i] for v in values) for i in range(3)];item["max"]=[max(v[i] for v in values) for i in range(3)]
        self.accessors.append(item);return len(self.accessors)-1

    def mesh(self,name:str,mesh:Mesh,material:int)->int:
        pos=self._accessor(mesh.positions,"f",5126,"VEC3",34962);normal=self._accessor(mesh.normals,"f",5126,"VEC3",34962);uv=self._accessor(mesh.uvs,"f",5126,"VEC2",34962)
        index_rows=[(v,) for v in mesh.indices];idx=self._accessor(index_rows,"H",5123,"SCALAR",34963)
        self.meshes.append({"name":name,"primitives":[{"attributes":{"POSITION":pos,"NORMAL":normal,"TEXCOORD_0":uv},"indices":idx,"material":material}]});return len(self.meshes)-1

    def node(self,name:str,mesh:int,translation=(0,0,0),scale=(1,1,1))->int:
        self.nodes.append({"name":name,"mesh":mesh,"translation":list(translation),"scale":list(scale)});return len(self.nodes)-1

    def build(self)->bytes:
        align4(self.binary);doc={"asset":{"version":"2.0","generator":"SOLUM P63 native asset generator"},"extensionsUsed":["KHR_materials_unlit"],"scene":0,"scenes":[{"name":"P63_Environment_Stage","nodes":list(range(len(self.nodes)))}],"nodes":self.nodes,"meshes":self.meshes,"materials":self.materials,"accessors":self.accessors,"bufferViews":self.buffer_views,"buffers":[{"byteLength":len(self.binary)}]}
        if self.images:doc.update(images=self.images,textures=self.textures,samplers=self.samplers)
        encoded=json.dumps(doc,separators=(",",":"),ensure_ascii=True).encode();encoded+=b" "*((4-len(encoded)%4)%4);binary=bytes(self.binary)
        return struct.pack("<4sII",b"glTF",2,12+8+len(encoded)+8+len(binary))+struct.pack("<I4s",len(encoded),b"JSON")+encoded+struct.pack("<I4s",len(binary),b"BIN\0")+binary


def generate_glb() -> bytes:
    b=GlbBuilder();moon_texture=b.texture_png(moon_png())
    concrete=b.material("P63_Concrete",(0.32,0.34,0.35,1),0,0.82);rough_metal=b.material("P63_RoughMetal",(0.28,0.31,0.34,1),1,0.48);polished=b.material("P63_PolishedMetal",(0.62,0.66,0.72,1),1,0.08)
    glass=b.material("P63_Glass",(0.30,0.62,0.78,0.28),0,0.06,alpha="BLEND");water=b.material("P63_Water",(0.05,0.19,0.28,0.62),0,0.04,alpha="BLEND");wet=b.material("P63_WetGround",(0.085,0.09,0.095,1),0.05,0.16)
    puddle=b.material("P63_Puddle",(0.035,0.09,0.13,0.72),0,0.025,alpha="BLEND");snow=b.material("P63_SnowSurface",(0.86,0.91,0.97,1),0,0.78);ice=b.material("P63_Ice",(0.42,0.68,0.78,0.58),0,0.09,alpha="BLEND")
    sun=b.material("P63_SunDisk",(1,0.78,0.34,1),0,0,unlit=True,emissive=(1,0.48,0.08));moon=b.material("P63_MoonCrater",(0.72,0.78,0.9,1),0,0.66,unlit=True,emissive=(0.14,0.18,0.28),texture=moon_texture);moon_shadow=b.material("P63_MoonPhaseShadow",(0.005,0.008,0.018,0.97),0,1,unlit=True,alpha="BLEND")
    star_mats=[b.material("P63_StarWarm",(1,0.72,0.46,1),0,0,unlit=True,emissive=(1,0.42,0.18)),b.material("P63_StarWhite",(0.9,0.94,1,1),0,0,unlit=True,emissive=(0.72,0.8,1)),b.material("P63_StarBlue",(0.55,0.72,1,1),0,0,unlit=True,emissive=(0.32,0.52,1))]
    cloud=b.material("P63_Cloud",(0.72,0.76,0.81,0.72),0,0.92,alpha="BLEND");rain=b.material("P63_Rain",(0.42,0.62,0.82,0.64),0,0.04,alpha="BLEND");snowflake=b.material("P63_SnowFlake",(0.92,0.96,1,0.86),0,0.62,alpha="BLEND");dust=b.material("P63_Dust",(0.62,0.38,0.18,0.42),0,0.92,alpha="BLEND")
    ripple=b.material("P63_Ripple",(0.4,0.68,0.9,0.58),0,0.05,unlit=True,alpha="BLEND");bolt=b.material("P63_LightningBolt",(0.68,0.78,1,1),0,0,unlit=True,emissive=(1,1,1));cloth=b.material("P63_FlagCloth",(0.12,0.62,0.55,1),0,0.72)
    cube_mesh={m:b.mesh("box_"+str(m),box(),m) for m in (concrete,rough_metal,polished,glass,wet,snow,ice,cloth)};plane_mesh={m:b.mesh("plane_"+str(m),plane(),m) for m in (concrete,water,wet,puddle,snow,ice,cloth)};sphere_mesh={m:b.mesh("sphere_"+str(m),sphere(),m) for m in (rough_metal,polished,glass)}
    b.node("P63_STAGE_GROUND",plane_mesh[concrete],(0,0,0),(11,1,9));b.node("P63_MATTE_STONE",cube_mesh[concrete],(-6,0.65,-2),(1.1,0.65,1.1));b.node("P63_ROUGH_METAL",sphere_mesh[rough_metal],(-3,1,-2),(1,1,1));b.node("P63_POLISHED_METAL",sphere_mesh[polished],(-0.3,1,-2),(1,1,1));b.node("P63_GLASS",sphere_mesh[glass],(-4.5,1,1.3),(1,1,1));b.node("P63_WATER",plane_mesh[water],(-1.0,0.035,2.0),(2.0,1,1.4));b.node("P63_WET_SURFACE",plane_mesh[wet],(-4.8,0.025,4.8),(2.2,1,1.5));b.node("P63_PUDDLE",plane_mesh[puddle],(-0.2,0.045,5.0),(2.0,1,1.2));b.node("P63_SNOW_SURFACE",plane_mesh[snow],(3.2,0.035,5.0),(1.6,1,1.25));b.node("P63_ICE_SURFACE",plane_mesh[ice],(6.5,0.045,5.0),(1.5,1,1.25))
    # Roofed room with an open doorway; its dimensions match runtime exclusion/roof mask.
    b.node("P63_INTERIOR_FLOOR",plane_mesh[concrete],(4.5,0.02,0.5),(2.5,1,2.5));b.node("P63_ROOF",cube_mesh[concrete],(4.5,4.0,0.5),(2.6,0.12,2.6));b.node("P63_ROOM_BACK",cube_mesh[concrete],(4.5,2.0,-2.0),(2.6,2.0,0.12));b.node("P63_ROOM_LEFT",cube_mesh[concrete],(2.0,2.0,0.5),(0.12,2.0,2.5));b.node("P63_ROOM_RIGHT",cube_mesh[concrete],(7.0,2.0,0.5),(0.12,2.0,2.5));b.node("P63_DOOR_LEFT",cube_mesh[concrete],(2.8,2.0,3.0),(0.8,2.0,0.12));b.node("P63_DOOR_RIGHT",cube_mesh[concrete],(6.2,2.0,3.0),(0.8,2.0,0.12));b.node("P63_DOOR_TOP",cube_mesh[concrete],(4.5,3.55,3.0),(0.9,0.45,0.12))
    for i,x in enumerate((-8,-6,-4,-2,0,2,4,6,8)):b.node(f"P63_FOG_POLE_{i}",cube_mesh[rough_metal],(x,1.8,-6.5),(0.12,1.8,0.12))
    b.node("P63_FLAG_POLE",cube_mesh[rough_metal],(-8,2.5,3.5),(0.06,2.5,0.06));b.node("P63_FLAG",plane_mesh[cloth],(-7.25,3.8,3.5),(0.75,1,0.55))
    sun_mesh=b.mesh("sun_disk",disk(40),sun);moon_mesh=b.mesh("moon_disk",disk(40),moon);shadow_mesh=b.mesh("moon_shadow",disk(40),moon_shadow)
    b.node("P63_SUN_DISK",sun_mesh,(0,14,-25),(1.15,1.15,1.15));b.node("P63_MOON_DISK",moon_mesh,(0,12,-24),(1.0,1.0,1.0));b.node("P63_MOON_SHADOW",shadow_mesh,(0,12,-23.96),(1.0,1.0,1.0))
    for group,material in enumerate(star_mats):b.node(f"P63_STAR_GROUP_{group}",b.mesh(f"star_group_{group}",star_group(SEED^0x53544152,group),material))
    cloud_mesh=b.mesh("cloud_cluster",cloud_cluster(SEED^0x434C4F55),cloud)
    cloud_positions=[(-9,10,-8),(-5,11,-11),(-1,9,-9),(4,10,-12),(9,11,-8),(-8,12,-16),(-3,10,-17),(2,12,-16),(7,9,-17),(-6,8,-5),(0,11,-5),(7,10,-4)]
    for i,position in enumerate(cloud_positions):b.node(f"P63_CLOUD_{i}",cloud_mesh,position,(1,1,1))
    rain_meshes=[b.mesh(f"rain_group_{i}",rain_group(SEED+i),rain) for i in range(3)];snow_meshes=[b.mesh(f"snow_group_{i}",snow_group(SEED+100+i),snowflake) for i in range(3)];dust_meshes=[b.mesh(f"dust_group_{i}",dust_group(SEED+200+i),dust) for i in range(3)]
    for z in range(5):
        for x in range(5):
            index=z*5+x;position=((x-2)*3.0,6.0,(z-2)*3.0)
            b.node(f"P63_RAIN_CELL_{x}_{z}",rain_meshes[index%3],position);b.node(f"P63_SNOW_CELL_{x}_{z}",snow_meshes[index%3],position);b.node(f"P63_DUST_CELL_{x}_{z}",dust_meshes[index%3],position)
    ring_mesh=b.mesh("ripple_ring",ring(),ripple);impact_positions=[(-7,0.055,-1),(-5,0.055,4),(-2,0.055,1),(0,0.055,5),(2,0.055,-3),(7.5,0.055,-3),(-8,0.055,6),(1,0.055,7)]
    for i,position in enumerate(impact_positions):b.node(f"P63_RIPPLE_{i}",ring_mesh,position,(0.35,1,0.35))
    b.node("P63_LIGHTNING_BOLT",b.mesh("lightning_bolt",lightning_mesh(),bolt),(0,0,-8),(1,1,1))
    return b.build()


def generate_celestial_glb(moon_payload: bytes | None = None, moon_name: str = "P63_SOLUM_NATIVE_MOON") -> bytes:
    """Small explicit P63.2A stage. It contains no weather, stars, or dynamic IBL assets."""
    b = GlbBuilder()
    source_moon_payload = moon_payload or moon_png(256)
    # Embed the exact audited source unchanged for provenance. Runtime phase nodes use only
    # deterministic color derivatives of this payload, never a secondary black occluder.
    b.texture_png(source_moon_payload, moon_name)
    decoded_moon = decode_png_rgba(source_moon_payload)
    sun_texture = b.texture_png(sun_disc_png(), "P63_SOLUM_NATIVE_SUN_CORE")
    halo_texture = b.texture_png(halo_png(), "P63_SMOOTH_CELESTIAL_HALO")
    concrete = b.material("P63_2A_Matte", (0.34, 0.35, 0.36, 1), 0, 0.86)
    rough_metal = b.material("P63_2A_RoughMetal", (0.30, 0.33, 0.37, 1), 1, 0.48)
    polished = b.material("P63_2A_PolishedMetal", (0.64, 0.68, 0.74, 1), 1, 0.07)
    glass = b.material("P63_2A_Glass", (0.25, 0.58, 0.74, 0.27), 0, 0.06, alpha="BLEND")
    sun = b.material("P63_2A_SunDisk", (1, 0.78, 0.34, 1), 0, 0, unlit=False,
                     emissive=(0.72, 0.32, 0.04), alpha="BLEND", texture=sun_texture,
                     emissive_texture=sun_texture, force_single_sided=True)
    sun_halo = b.material("P63_2A_SunHalo", (1, 0.82, 0.50, 0.28), 0, 1, unlit=True,
                          alpha="BLEND", texture=halo_texture, force_single_sided=True)
    moon_halo = b.material("P63_2A_MoonHalo", (0.58, 0.68, 0.92, 0.16), 0, 1, unlit=True,
                           alpha="BLEND", texture=halo_texture, force_single_sided=True)

    box_mesh = b.mesh("p63_2a_box", box(), concrete)
    ground_mesh = b.mesh("p63_2a_ground", plane_up(), concrete)
    matte_sphere = b.mesh("p63_2a_matte_sphere", sphere(24, 14, outward_winding=True), concrete)
    rough_sphere = b.mesh("p63_2a_rough_sphere", sphere(24, 14, outward_winding=True), rough_metal)
    polished_sphere = b.mesh("p63_2a_polished_sphere", sphere(24, 14, outward_winding=True), polished)
    glass_sphere = b.mesh("p63_2a_glass_sphere", sphere(24, 14, outward_winding=True), glass)
    b.node("P63_CELESTIAL_STAGE_ROOT", box_mesh, (0, -0.3, -2), (0.05, 0.05, 0.05))
    b.node("P63_STAGE_GROUND", ground_mesh, (0, 0, -2), (14, 1, 10))
    b.node("P63_MATTE_SPHERE", matte_sphere, (-6.0, 1.1, -2.5), (1.1, 1.1, 1.1))
    b.node("P63_ROUGH_METAL", rough_sphere, (-2.0, 1.1, -2.5), (1.1, 1.1, 1.1))
    b.node("P63_POLISHED_METAL", polished_sphere, (2.0, 1.1, -2.5), (1.1, 1.1, 1.1))
    b.node("P63_GLASS", glass_sphere, (6.0, 1.1, -2.5), (1.1, 1.1, 1.1))
    b.node("P63_SHADOW_PILLAR", box_mesh, (-7.5, 2.2, 2.2), (0.45, 2.2, 0.45))
    b.node("P63_VERTICAL_WALL", box_mesh, (8.0, 2.2, 1.5), (0.24, 2.2, 3.2))
    b.node("P63_INTERIOR_FLOOR", ground_mesh, (4.6, 0.025, 5.5), (2.5, 1, 2.0))
    b.node("P63_ROOF", box_mesh, (4.6, 3.6, 5.5), (2.6, 0.12, 2.1))
    b.node("P63_ROOF_LEFT", box_mesh, (2.05, 1.8, 5.5), (0.12, 1.8, 2.1))
    b.node("P63_ROOF_BACK", box_mesh, (4.6, 1.8, 7.55), (2.6, 1.8, 0.12))
    b.node("P63_NORMAL_SCALE_REFERENCE", box_mesh, (-4.6, 0.55, 4.7), (0.55, 0.55, 0.55))
    b.node("P63_CAMERA_ORBIT_TARGET", matte_sphere, (0, 1.2, -2), (0.06, 0.06, 0.06))

    sun_mesh = b.mesh("p63_2a_sun_disk", disk(96), sun)
    sun_halo_mesh = b.mesh("p63_2a_sun_halo", disk(96), sun_halo)
    moon_halo_mesh = b.mesh("p63_2a_moon_halo", disk(96), moon_halo)
    b.node("P63_SUN_HALO", sun_halo_mesh, (0, 14, -25), (0.3, 0.3, 0.3))
    b.node("P63_SUN_DISK", sun_mesh, (0, 14, -25), (0.13, 0.13, 0.13))
    b.node("P63_MOON_HALO", moon_halo_mesh, (0, 12, -24), (0.3, 0.3, 0.3))
    for index in range(33):
        phase = index / 32.0
        phase_texture = b.texture_png(moon_phase_png(decoded_moon, phase), f"P63_MOON_ANALYTIC_PHASE_{index:02d}")
        phase_material = b.material(f"P63_2A_MoonPhase_{index:02d}", (1, 1, 1, 1), 0, 0.62,
                                    unlit=False, emissive=(0.12, 0.14, 0.18), alpha="BLEND",
                                    texture=phase_texture, emissive_texture=phase_texture, force_single_sided=True)
        phase_mesh = b.mesh(f"p63_2a_moon_phase_{index:02d}", disk(96), phase_material)
        b.node(f"P63_MOON_PHASE_{index:02d}", phase_mesh, (0, 12, -24), (0.001, 0.001, 0.001))
    return b.build()


def rgbe(red: float, green: float, blue: float) -> tuple[int, int, int, int]:
    value=max(red,green,blue)
    if value<1e-32:return 0,0,0,0
    mantissa,exponent=math.frexp(value);scale=mantissa*256.0/value
    return min(255,int(red*scale)),min(255,int(green*scale)),min(255,int(blue*scale)),exponent+128


def encode_channel(values: list[int]) -> bytes:
    out=bytearray();offset=0
    while offset<len(values):
        count=min(128,len(values)-offset);out.append(count);out.extend(values[offset:offset+count]);offset+=count
    return bytes(out)


def hdr_bytes(slot: str, width: int = 128, height: int = 64) -> bytes:
    settings={"day":((0.0,0.72,-0.68),(0.16,0.42,0.9),(0.72,0.79,0.9),5.5,0.12),"sunset":((-0.72,0.18,-0.67),(0.08,0.12,0.31),(1.0,0.25,0.055),7.0,0.28),"night":((0.2,-0.3,-0.93),(0.006,0.012,0.05),(0.04,0.08,0.18),0.35,0.05),"overcast":((0.0,0.55,-0.83),(0.18,0.22,0.28),(0.42,0.46,0.51),1.3,0.42),"storm":((-0.3,0.32,-0.9),(0.025,0.035,0.055),(0.13,0.16,0.22),0.7,0.62),"snow":((0.15,0.5,-0.85),(0.34,0.43,0.58),(0.82,0.88,0.96),2.1,0.34),"sand":((-0.5,0.32,-0.8),(0.28,0.12,0.035),(0.78,0.39,0.12),2.4,0.58)}
    sun,zenith,horizon,sun_power,mie=settings[slot];pixels=[]
    for y in range(height):
        theta=(y+0.5)/height*math.pi;dy=math.cos(theta);ring=math.sin(theta);row=[]
        for x in range(width):
            phi=(x+0.5)/width*math.tau;direction=(math.sin(phi)*ring,dy,-math.cos(phi)*ring);elevation=max(0,min(1,(dy+0.08)/0.78));base=[horizon[i]*(1-elevation)+zenith[i]*elevation for i in range(3)];mu=max(-1,min(1,sum(direction[i]*sun[i] for i in range(3))));rayleigh=0.0597*(1+mu*mu);g=0.76;hg=(1-g*g)/max(0.02,(1+g*g-2*g*mu)**1.5);disc=math.exp((mu-1)*900);scatter=rayleigh*0.7+mie*hg*0.035+disc*sun_power
            if dy<0:base=[0.035,0.04,0.045]
            tint=(1.0,0.78,0.48) if slot in ("sunset","sand") else (0.76,0.86,1.0);row.append(rgbe(*(base[i]+scatter*tint[i] for i in range(3))))
        pixels.append(row)
    out=bytearray(b"#?RADIANCE\n# SOLUM P63 physically-inspired prepared IBL\nFORMAT=32-bit_rle_rgbe\n\n"+f"-Y {height} +X {width}\n".encode())
    for row in pixels:
        out.extend((2,2,width>>8,width&255))
        for channel in range(4):out.extend(encode_channel([pixel[channel] for pixel in row]))
    return bytes(out)


def main() -> None:
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/"p63_environment_stage.glb").write_bytes(generate_glb())
    (OUT/"p63_2a_celestial_test_stage.glb").write_bytes(generate_celestial_glb())
    for slot in ("day","sunset","night","overcast","storm","snow","sand"):(OUT/f"p63_{slot}.hdr").write_bytes(hdr_bytes(slot))
    manifest={"schema":"solum.environment.generated-assets","schemaVersion":1,"generator":"tools/generate_p63_environment_assets.py","seed":SEED,"license":"SOLUM_NATIVE","files":sorted(path.name for path in OUT.iterdir() if path.is_file())}
    (OUT/"P63_ASSET_MANIFEST.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":"OK","output":str(OUT),"files":manifest["files"]}))


if __name__ == "__main__":main()
