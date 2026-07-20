# SOLUM SLPK / Prism / Flux / Adaptive Governor

Status: **architecture handoff + partially verified research**.

This document consolidates the SOLUM format and optimization work that was previously spread across chat handoffs and experimental reports. It is intended for Codex and future developers working on SOLUM, UDS/UDW, rendering, assets, gameplay runtime, or local ML.

It is not a claim that the whole architecture is already implemented or faster on Android.

## Evidence labels

Every implementation or report must preserve one of these labels:

| Label | Meaning |
|---|---|
| `LOCAL_CONFIRMED` | Verified by local C++/Python tests with checksum, hash, or numerical comparison |
| `SIMULATION_CONFIRMED` | Verified only by a scheduler or cost-model simulation |
| `UPSTREAM_CONFIRMED` | Capability exists in Filament, Vulkan, Android, PyTorch, ExecuTorch, llama.cpp, or another selected backend |
| `PROJECT_REQUIRED` | Must be tested against the real SOLUM repository, assets, traces, and runtime |
| `DEVICE_REQUIRED` | Must be tested on Android, especially TECNO CI8n / Mali-G57 MC2 |
| `NEGATIVE` | The method was slower or worse than the baseline in the tested scenario |

Do not convert local microbenchmarks into promises about total FPS, loading time, battery life, thermal stability, or ML tokens per second.

## Current implementation status

A first permanent implementation exists in the separate repository `AsArtilluman8/CharacterDirectorNative`, branch `foundation/solum-stage1-slpk-ui`, draft PR 4.

Implemented there:

- fixed SLPK v3 binary records;
- dense `uint32_t` resource handles;
- deterministic raw-resource writer;
- checked reader with bounds, overflow, alignment, overlap, and reference validation;
- per-chunk CRC32;
- normalized package SHA-256;
- roundtrip, corruption, truncation, and sanitizer tests;
- allocation-free frame telemetry windows;
- Flux state contracts with `OBSERVE` as the safe default;
- renderer-independent retained editor core;
- Android/JNI device harness.

Host C++ tests, ASan/UBSan tests, Android Java tests, ARM64 NDK compilation, and APK assembly have passed in CI.

The first target-device package export was exercised manually and produced repeated bit-identical SLPK files. This confirms the basic writer/export/integrity path, not streaming performance or a general runtime speedup.

A later visual-gate APK was built to pack a real GLB as a SLPK resource and load it through Filament. Its final Mali-G57 report remains `DEVICE_REQUIRED` until the generated JSON and screenshot are inspected.

Do not duplicate or silently replace this work. Inspect the branch and port only the parts that fit the current SOLUM repository.

---

# 1. Architecture

The architecture is split into four layers:

```text
SLPK      physical, versioned binary container and exact data variants
Prism     offline cooker, layout profiler, page builder, startup traces, device variants
Flux      runtime modules that remove unnecessary work without silently changing quality
Governor  adaptive controller that enables only measured, safe, profitable modules
```

Data flow:

```text
UE-derived truth / glTF / GLB / KTX2 / audio / material data / ML weights
                                |
                                v
                          Prism Cooker
 normalize -> validate -> hash -> dependencies -> dense IDs -> pages -> variants
                                |
                                v
                           SLPK package
 exact chunks + dependencies + alternatives + provenance + rollback metadata
                                |
                                v
                           Solum Runtime
 map package -> Critical Render Set -> exact baseline -> page streaming
                                |
                                v
                               Flux
 visibility / residency / uploads / shadows / poses / particles / ML cache
                                |
                                v
                       Adaptive Governor
 OBSERVE -> ACTIVE -> COOLDOWN or FALLBACK
```

Primary principle:

> Do not attempt to accelerate the entire engine with one codec or one renderer trick. Remove runtime work in advance and skip only work proven not to affect the required result.

---

# 2. Quality contracts

## 2.1 `STRICT_EXACT`

Default contract for engine truth, authoring data, integrity tests, and any optimization advertised as exact.

Allowed examples:

- raw or memory-mapped data;
- lossless LZ4 or Zstd pages;
- bit-exact chunk restoration;
- exact texture and geometry pages;
- conservative visibility with zero false negatives;
- precomputed static matrices;
- exact pose sharing using the full state key;
- partial skinning only when the visible and shadow-required result matches;
- deterministic analytic jumps for independent hidden emitters;
- exact KV or prefix cache reuse;
- exact checkpoint deltas;
- batching and sorting that do not change output.

If a required exact page is unavailable, the runtime may stall or keep a previously valid exact state. It must not silently substitute degraded content.

## 2.2 `DISTRIBUTION_EXACT`

The algorithm or mathematical distribution remains valid, but floating-point execution may differ slightly.

Examples:

- correct speculative decoding;
- optimized SDPA or FlashAttention-like kernels;
- fused operations with bounded floating-point roundoff.

## 2.3 `BOUNDED_PERCEPTUAL`

Explicit opt-in quality profile. Never label it lossless or strict exact.

Examples:

- reduced peripheral shading rate;
- lower secondary reflection or GI update rate outside attention regions;
- less frequent distant secondary effects;
- ASTC, ETC2, or Basis representation when it is derived from a higher-fidelity source;
- ML quantization;
- perceptual animation, physics, VFX, or AI update budgets.

---

# 3. SLPK v3 container

## 3.1 Responsibility

SLPK is a package, provenance, dependency, variant, and streaming layer. It is not intended to replace every established payload format.

Suitable passthrough payloads include:

- GLB/glTF-derived binary data;
- KTX2/Basis/ASTC texture payloads;
- Filament `.filamat` packages;
- Opus, AAC, FLAC, MP4, or WebM where appropriate;
- reviewed JSON, CBOR, or typed binary contracts;
- ML weights in compatible layouts.

SLPK should:

- open with one or a small number of file descriptors;
- support `mmap`;
- account for 4 KiB and 16 KiB Android page configurations;
- expose a compact hot resource table;
- divide content into independent chunks or pages;
- keep exact and perceptual alternatives explicitly separated;
- store a dependency graph;
- preserve source and derived provenance;
- support A/B manifests and rollback;
- validate all bounds, sizes, references, and integrity before use;
- permit an external Exact Vault outside the APK.

## 3.2 Fixed header direction

The implemented foundation uses a fixed 256-byte `SLP3` header with versioning, counts, table offsets, file size, build ID, Exact Vault ID, and root SHA-256.

The precise record layout in code is the source of truth. Do not create a conflicting second header from this document.

The required semantic fields are:

```text
magic and version
minimum/preferred page shift
flags
resource/chunk/alternative/dependency counts
table offsets
manifest and integrity offsets
file size
build ID
Exact Vault ID
root SHA-256
reserved versioned space
```

## 3.3 Hot resource records

Hot runtime access uses a dense package-local handle:

```cpp
using ResourceHandle = uint32_t;
const SlpkResource32& resource = resourceTable[handle];
```

Names, paths, stable hashes, UE package paths, and detailed provenance remain in cold tables or manifests. They should not be required for every frame lookup.

The hot record needs at least:

- default alternative or primary chunk;
- dependency range;
- auxiliary index;
- logical raw size;
- lane;
- quality contract;
- stable type;
- flags and integrity metadata.

## 3.4 Exact alternatives

One logical resource may have several measured exact representations:

- raw mapped;
- LZ4 page;
- Zstd page;
- AoS, SoA, or AoSoA runtime layouts when the logical result is equivalent;
- resident or windowed exact ML weight layout;
- existing GPU blocks stored without recompression.

Every alternative should record:

- capability requirements;
- schema version;
- codec;
- quality contract;
- stored and raw bytes;
- integrity;
- measured decode p95;
- measured upload p95;
- temporary memory cost.

Prism keeps only Pareto-useful variants for target device profiles.

## 3.5 Lanes

Recommended logical lanes:

```text
BOOT_LANE
  Critical Render Set, UI, sky, player, first-frame pipelines

SCENE_LANE
  cells, clusters, bounds, hot records, material template IDs

GEOMETRY_LANE
  vertex/index pages, meshlet metadata, shadow caster sets

TEXTURE_LANE
  KTX2 mip/page directories and explicit quality variants

SHADER_LANE
  filamat, optional portable fallback data, usage manifests, cache metadata

ANIMATION_LANE
  clips, skeleton metadata, pose keys, bone and mesh dependencies

WEATHER_LANE
  curves, deterministic emitters, interacting emitters, UDS-derived contracts

AUDIO_VIDEO_LANE
  audio/video payloads and seek indices

MICROTEXT_LANE
  HTML, JS, CSS, JSON, dictionaries, microblocks

ML_LANE
  resident/windowed weights, LoRA pages, KV/prefix metadata, deltas

COLD_METADATA_LANE
  names, paths, debug/truth links, schemas, provenance
```

## 3.6 Codec policy

Never select a codec only from the file extension.

Selection must account for:

```text
stored size / measured storage bandwidth
+ decode p95 scaled for the device
+ upload p95
+ temporary-memory penalty
+ compatibility risk
```

Initial policy from the existing experiments:

| Data | Initial candidate |
|---|---|
| Precompressed PNG, WebP, MP4, Opus, filamat, KTX2 blocks | raw passthrough |
| Hot random-access records | raw mmap |
| Latency-sensitive streaming pages | LZ4 |
| Cold structured metadata | low-level Zstd |
| Repeated microtext corpus | Zstd dictionary or microblocks |
| PCM | FLAC or block PCM depending on seek requirements |

This is a starting hypothesis. The current Android storage, CPU, and memory profile must select the real winner.

## 3.7 Integrity and updates

Before a chunk is used:

1. validate offset and size against overflow and file bounds;
2. enforce configured raw-size limits;
3. enforce a safe compression ratio;
4. decode only into a bounded buffer or arena;
5. verify fast chunk integrity;
6. verify SHA-256 at install, patch, or trust boundaries.

Atomic update direction:

```text
write new chunks
-> verify bounds and SHA-256
-> write inactive manifest B
-> fsync
-> atomically switch active manifest pointer
-> preserve manifest A as rollback target
```

---

# 4. Prism cooker

Prism moves expensive work out of runtime.

## 4.1 Pipeline

```text
import
-> verify source truth
-> normalize schema/endian
-> content hash
-> dependency graph
-> dense ResourceHandle assignment
-> quality-contract classification
-> cluster and page build
-> precompute render data
-> measure codecs and layouts
-> Pareto prune
-> build startup traces and Critical Render Set
-> write SLPK
-> verify full reconstruction
```

## 4.2 Exact Vault

The Exact Vault stores bit-exact sources and may live outside the production APK.

Purposes:

- SHA-256 content addressing;
- whole-file deduplication;
- content-defined chunk deduplication for related versions;
- restoration of any runtime package image;
- storage of UE-derived truth and extraction reports;
- patch source and developer archive.

## 4.3 Dense IDs and local palettes

External stable hashes are converted into package-local dense IDs.

Benefits:

- direct array lookup;
- smaller metadata;
- no string or hash lookup in hot loops;
- compact local palettes per world cluster.

Static world records should be split by access pattern. Selective scans should not fetch names, strings, full transforms, and rarely used properties.

## 4.4 World partition and cluster data

Recommended hierarchy:

```text
World
└── Region
    └── Cell
        └── Cluster
            └── Meshlet or object records
```

Cell and page sizes must come from real traces. A preliminary experimental target was approximately 64–128 KiB raw for some static records, but it is not a fixed production rule.

Prism may precompute:

- static matrices;
- bounds and normal cones;
- material and mesh groups;
- draw sort keys;
- instance runs;
- visibility metadata;
- texture and geometry page dependencies;
- static/dynamic shadow caster sets;
- shader usage manifests.

## 4.5 Critical Render Set

The Critical Render Set is the minimum exact set needed for the first correct frame:

- spawn-area cells;
- player mesh, materials, skeleton, and initial animation;
- sky and initial weather state;
- UI and fonts;
- first-frame shader/material variants;
- required texture mips/pages;
- mandatory shadow data;
- nearby interactive objects.

It is an ordered reference list, not duplicated payload data.

## 4.6 Startup Tape

Real launch traces should be converted into physical and logical prefetch order:

```text
BOOT -> MENU -> LEVEL_SPAWN -> FIRST_COMBAT -> WEATHER_RAIN
```

A chunk can participate in several named traces by reference. Do not create unlimited copies.

## 4.7 Materials and shaders

Cold material representation:

```text
templateId + sparse exact overrides
```

Hot representation contains only frequently scanned parameters.

Prism should generate Filament usage manifests and variant filters. It must not replace `.filamat` or recreate Filament's material compiler.

A shader capsule may contain:

```text
filamat package
+ exact usage/permutation manifest
+ optional portable fallback data
+ optional device/driver/build-keyed cache metadata
```

A cache fingerprint mismatch must reject the cache and use a portable fallback.

## 4.8 Texture and geometry pages

A page directory should record:

- logical resource and mip/meshlet range;
- dependencies;
- alignment;
- stored/raw bytes;
- upload destination;
- quality contract;
- priority;
- integrity.

Texture pages must follow KTX2 mip structure. Geometry pages must preserve the required vertex/index/meshlet dependency sets so that streaming cannot produce missing geometry.

## 4.9 ML preparation

Prism may prepare:

- resident contiguous weight layouts;
- windowed layer layouts;
- LoRA or adapter page directories;
- exact page-delta checkpoints;
- stable prefix-cache keys;
- tokenized dataset shards.

Compatibility with Safetensors, GGUF, ExecuTorch, llama.cpp, and the selected backend should be preserved before inventing a closed single-purpose ML format.

---

# 5. Flux game runtime

Each Flux module begins in `OBSERVE`. It may not change output until its benefit and safety predicates are measured.

## 5.1 Residency manager

Sets:

```text
Required   needed by the current frame or gameplay state
Predicted  prefetched with confidence and deadline
Protected  cannot be evicted while a CPU/GPU consumer may use it
Cold       eviction candidate
```

Required mechanisms:

- global budget broker;
- safety floors for player, UI, sky, and critical shaders;
- upload-bandwidth token bucket;
- deadlines;
- generation IDs;
- stale-prefetch cancellation;
- teleport emergency reserve;
- fence-aware eviction;
- simple global LRU baseline;
- 2Q only if traces prove a p95 benefit.

## 5.2 Frame and upload arenas

Avoid mass `new/delete` in frame loops.

Candidate arenas:

```text
frame CPU arena
upload staging ring
transient command arena
animation scratch arena
ML scratch and KV arenas
```

A ring segment is reusable only after the relevant GPU fence.

## 5.3 Visibility hierarchy

Safe baseline:

```text
CPU region/cell/object conservative culling
GPU experimental cluster/meshlet frustum + cone + HZB path
```

The local experiment found detailed CPU meshlet/HZB work could be much more expensive than simple object culling. Therefore the meshlet/HZB path is `DEVICE_REQUIRED` and should be GPU-oriented or disabled.

## 5.4 Meshlets are not automatically LOD

Meshlets may contain the original triangles. Exact Flux can reject only parts proven not to affect the required pass:

- outside frustum;
- conservatively back-facing;
- conservatively occluded;
- not required for a shadow pass.

In strict mode, non-resident required data causes a stall or retained exact state, not a visible hole.

## 5.5 Visibility buffer

An opaque visibility-buffer experiment may reduce repeated shading by producing depth/primitive/material IDs before final shading.

It is not a production default. Separate paths are required for:

- transparency;
- alpha-mask foliage;
- hair;
- particles;
- water;
- displacement.

Filament and Mali integration are `DEVICE_REQUIRED`.

## 5.6 Partial skinning

Candidate exact sequence:

1. identify visible and shadow-required meshlets;
2. gather unique vertex IDs and bone dependencies;
3. use subset skinning only below a measured threshold;
4. otherwise use full-skinning baseline.

The visible result must match the baseline.

## 5.7 Exact pose sharing

A pose may be shared only when the full relevant state matches:

```text
skeleton ID
clip IDs
exact times/phases
blend weights
root-motion mode
morph weights
IK state
gameplay pose modifiers
```

First measure the duplicate ratio. Disable the cache when states are mostly unique.

## 5.8 Shadows

Separate:

- static caster lists;
- dynamic caster lists;
- reusable depth data where exact-compatible;
- cascade and light state.

When the sun changes, CPU caster lists may remain reusable while depth must be redrawn. The experimental static shadow cache became slower when the light changed globally every frame; this is a recorded `NEGATIVE` case.

## 5.9 Weather and particles

The emitter compiler should classify emitters as:

- deterministic independent;
- collision/interacting;
- screen-space compatible;
- gameplay-required.

Only independent hidden particles may use an analytic jump based on deterministic seed and global time. Do not use it for collision, fluid, flocking, or gameplay interactions.

## 5.10 Filament transient resources

Measure Filament FrameGraph and resource allocation first. Do not write a second FrameGraph unless a real gap is demonstrated.

Solum may add:

- cross-frame project policy;
- streaming and upload arenas;
- lifetime hints;
- diagnostics and telemetry.

## 5.11 Transparency and overdraw

Still an open device-test area:

- alpha-to-coverage;
- tighter foliage geometry;
- alpha-mask depth prepass;
- tile counters;
- particle layer budgets;
- water/transparency ordering;
- temporal smoke reconstruction in perceptual mode.

Strict mode cannot discard layers that affect the final pixel.

---

# 6. Flux ML runtime

This is relevant only after identifying the actual local inference backend.

## 6.1 Backend capability audit

Check whether the chosen backend already provides:

- static or paged KV cache;
- prompt/prefix cache;
- fused QKV/MLP;
- optimized attention;
- AOT memory planning;
- speculative decoding;
- memory-mapped weights;
- quantization.

Do not duplicate mature backend features.

## 6.2 KV arena

When a backend naively grows KV with concatenation:

```text
known maximum context + one sequence -> static preallocated KV
multiple requests or branches -> paged KV
```

KV pages require ownership, valid token ranges, layer/head layout, reference counting where asynchronous, eviction/admission policy, and memory-pressure fallback.

## 6.3 Prefix cache

Candidate exact prefixes:

- system prompt;
- Cortex instructions;
- tool schemas;
- unchanged SOLUM context;
- unchanged repository files.

Cache key includes model/version, tokenizer/version, exact token IDs, adapter ID, position/rope settings, and backend/kernel compatibility.

## 6.4 Fused operations and attention

Use backend-native fused QKV/MLP and optimized attention. This is a backend integration task, not an SLPK codec.

Measure:

- time to first token;
- tokens per second;
- peak RSS/PSS;
- KV bytes per token;
- output or numerical difference;
- thermal behavior.

## 6.5 Adaptive speculative decoding

Governor may select `off`, `2`, `4`, or `8` tokens based on acceptance rate, draft cost, verification cost, context, and thermal state.

A fixed large block was slower than baseline in the simulation and must remain a recorded negative result.

## 6.6 Weight residency

```text
resident mmap lane   faster when the model fits
windowed layer lane  lower RAM, possibly lower tokens/s
```

Use windowing only under memory pressure or when the resident model does not fit.

## 6.7 Training and checkpoints

Without changing the algorithm:

- tokenize ahead of time;
- bucket sequence lengths;
- pack with a correct block-diagonal mask;
- plan activation checkpoints for the real memory budget;
- save only trainable tensors for frozen-base/LoRA training;
- use exact page deltas;
- use asynchronous checkpoint I/O only when supported;
- compile AOT outside the phone.

Quantization, low-rank approximation, or optimizer-precision changes are not strict exact.

Exact delta candidate:

```text
full  = Zstd(current page)
delta = Zstd(byte-shuffle(bit-pattern(current) XOR bit-pattern(previous)))
choose the smaller exact representation
```

Use a full page when the delta is not beneficial.

---

# 7. Adaptive Governor

## 7.1 States

```text
DISABLED
OBSERVE
ACTIVE
COOLDOWN
FALLBACK
FAULTED
```

`OBSERVE` gathers telemetry without changing output.

Activation requires a measured lower-confidence benefit larger than enable, thermal, and risk margins.

Immediate deactivation conditions:

- safety predicate failed;
- integrity failed;
- unsupported capability;
- repeated p95 regression;
- critical memory pressure;
- quality-contract violation.

## 7.2 Control loops

```text
FAST, every frame
  required pages, deadlines, uploads, fences, protected sets

MEDIUM, approximately 5–10 Hz
  cache policy, shadow invalidation, duplicate poses, queue pressure,
  speculative acceptance

SLOW, approximately 0.1–1 Hz
  thermal headroom, process memory, Vulkan budget, device profile,
  codec/layout policy
```

## 7.3 Anti-oscillation

Required:

- hysteresis;
- minimum dwell time;
- cooldown;
- maximum switches per minute;
- confidence intervals;
- regression budget;
- stable baseline;
- telemetry-only phase before activation.

Governor owns state and budgets. It must not absorb renderer, animation, VFX, or ML implementation details.

---

# 8. Filament boundary

Do not duplicate:

- Filament material compiler and `.filamat`;
- material variant filtering;
- FrameGraph lifetime tracking;
- clustered/froxel lighting;
- normal renderable culling;
- glTF loading;
- Vulkan/OpenGL backend abstraction.

SOLUM/Prism/Flux may add:

- packaging and provenance;
- world cells and clusters;
- page residency;
- usage manifests;
- dirty-shadow policy;
- pose and particle scheduling;
- telemetry and Governor;
- project-level resource priorities.

Potential fork/backend experiments, each behind a separate feature flag:

- GPU meshlet compaction;
- indirect draw path;
- visibility buffer;
- custom page-table bindings;
- driver pipeline binaries.

None may be merged as default without a separate spike and device A/B test.

---

# 9. Experimental results already recorded

These are test-polygon results, not production SOLUM numbers.

## 9.1 Format and layout

| Experiment | Recorded result | Status and limit |
|---|---:|---|
| 50k JSON records vs raw mapped records | 70.01 ms vs 0.0168 ms | `LOCAL_CONFIRMED`, parse-vs-map microbenchmark |
| 50k AoS vs SoA traversal | 0.414 ms vs 0.217 ms | `LOCAL_CONFIRMED` |
| 1M sequential AoS vs SoA scan | 1.960 ms vs 0.575 ms | `LOCAL_CONFIRMED` |
| 1M runtime TRS builds vs prebuilt matrix copies | 8.042 ms vs 3.066 ms | `LOCAL_CONFIRMED` |
| 1M runtime draw-key sort vs pre-sorted scan | 63.224 ms vs 0.366 ms | `LOCAL_CONFIRMED`, not total render speed |
| LZ4 vs Zstd-1 raw payload decode | 0.957 ms vs 2.737 ms | LZ4 lower latency, Zstd smaller in that test |

## 9.2 Prism, startup, and memory

| Experiment | Recorded result | Limit |
|---|---:|---|
| ZIP Deflate startup vs profiled pack | 40.06 ms vs 19.61 ms | x86 cold-like process |
| Profiled pack vs loose files | 25.89 ms vs 19.61 ms | storage/device dependent |
| Runtime static-map activation vs precomputed data | 43.42 ms vs 4.20 ms | synthetic data |
| Dense ID vs `uint64` hash lookup | 21.7× in microbenchmark | hot lookup only |
| Hot static record | 64 bytes to 24 bytes | real scene layout required |
| Windowed ML weight model | about 128 MiB to about 4 MiB per layer | may lower tokens/s |
| Correlated exact checkpoint delta | 62.02 MB to 31.56 MB | random delta only about 1.08× |
| Material templates + sparse overrides | 12.21 MiB to 1.54 MiB | synthetic 100k materials |

## 9.3 Game runtime

| Experiment | Recorded result | Limit |
|---|---:|---|
| Conservative meshlet submission | 5.34× fewer submitted groups | detailed CPU culling was expensive |
| CPU object culling vs CPU meshlet/HZB | 0.0078 ms vs 1.705 ms | meshlet/HZB should be GPU/device-gated |
| Visibility-buffer opaque prototype | 2.73× time, 3.48× fewer shades | not Filament/Mali |
| Exact visible-page copy | 15.1× faster, 6.52× fewer active bytes | synthetic residency |
| Partial skinning | 3.76× | 64.8k visible of 240k vertices |
| Upload ring arena | 1.61× | real Filament copies unknown |
| Static shadow cache | 10.13× with stable light | 20% slower when light changed globally |
| Exact repeated pose sharing | 6.13× | slower with unique poses |
| Independent hidden particles | 5.49× | not valid for interactions |
| Predictive residency | up to 14.71× fewer stall-pages | extra upload; teleport benefit much lower |
| Transient aliasing cost model | median 1.87× lower reservation | Filament may already do it |

## 9.4 ML and Governor

| Experiment | Recorded result | Limit |
|---|---:|---|
| KV-cache generation prototype | 7.12× | small PyTorch model |
| Prefix cache | 2.87× | repeated exact prefix |
| Fused QKV | 1.98× | relevant operation only |
| Controlled speculative decoding | 2.16× | artificially high agreement |
| Adaptive speculative simulation | 1.79× | fixed-8 was 0.86× |
| Training bucketing | 1.18× wall time, about 2.1× lower peak test RSS | small CPU workload |
| Static KV append vs concatenate | about 28× append-path improvement | mature backend may already solve it |
| Paged KV append | about 22× append-path improvement | same caveat |
| SDPA vs manual attention | 2.49×, max diff 2.38e-7 | not bit-exact, backend-dependent |
| Adaptive Governor vs all-off model | 1.91× lower modeled work | simulation |
| Governor overhead | 0.00044 ms/frame for 32 modules | x86 microbenchmark |
| Single-bit corruption detection | 1000/1000 detected | local integrity test |

No combination or multiplication of these factors is allowed.

---

# 10. Recorded negative results and fallbacks

| Method | Harmful condition | Required fallback |
|---|---|---|
| LZ4/Zstd on incompressible or precompressed payload | no size benefit and added CPU | raw passthrough |
| CPU meshlet/HZB | culling overhead exceeds saved GPU work | object/cell culling |
| Static shadow depth reuse | moving sun invalidates all tiles | redraw depth, optionally reuse caster lists |
| Pose sharing | animation states are unique | calculate individually |
| Aggressive prefetch | teleport, oscillation, overfetch | required-only plus emergency reserve |
| Fixed large speculative block | low acceptance | adaptive off/2/4/8 |
| Windowed weights | model fits and storage becomes bottleneck | resident mmap |
| Complex cache policy | trace shows no benefit | simple global LRU |
| Runtime compilation on weak CPU | unacceptable cold compile | AOT outside device |
| Recompress MP4/KTX2/Opus/filamat | no useful size gain | passthrough |
| Custom FrameGraph | Filament already solves lifetime | use Filament FrameGraph |

Negative results must remain in documentation and regression tests.

---

# 11. Safe integration with UDS/UDW

UDS work should not be blocked while the full runtime format is unfinished.

The first useful integration is a bounded exact spike:

1. inspect current SOLUM asset loaders, caches, render loop, and Filament fork;
2. add telemetry without changing output;
3. select one verified UDS resource group, such as curves plus their provenance, or one GLB plus its material/texture dependencies;
4. package it as raw/passthrough SLPK resources using the existing v3 implementation as reference;
5. reopen and verify every chunk;
6. load the exact payload through the current runtime path;
7. compare current baseline against SLPK exact baseline;
8. record size, cold/warm load, Java/native/PSS memory, bytes copied, and output hash or screenshot diff;
9. do not enable compression, paging, meshlets, perceptual quality, or Governor in the same patch.

Suggested UDS resource categories for later separate tests:

- time/weather contracts and curves;
- material and Material Function reconstruction data;
- MPC parameter sets;
- verified textures and LUTs;
- audio and seek data;
- Niagara truth and SOLUM VFX intermediate data;
- map placement and dependencies;
- cooked meshes, skeletons, and animations when the decoder is verified.

UE-derived source truth, extracted payload, derived runtime asset, and SLPK record must remain linked by provenance and hash.

---

# 12. Implementation order

## Phase 0 — documentation and flags

- keep this document in the SOLUM repository;
- define compile/runtime flags;
- define quality contracts;
- keep new optimization modules disabled or observe-only.

## Phase 1 — honest telemetry

Collect without changing output:

- presented and rendered FPS;
- CPU/GPU p50, p95, p99;
- first frame and map activation;
- RSS/PSS and native/Java memory;
- Vulkan budget where available;
- bytes read, decoded, copied, and uploaded;
- page misses, stalls, and overfetch;
- shader/pipeline stalls;
- shadow invalidation ratio;
- pose duplicate ratio;
- particle counts by class;
- thermal state;
- ML TTFT, tok/s, KV bytes, cache hits, and acceptance when relevant.

## Phase 2 — SLPK exact baseline

- port or share the existing v3 records and validator;
- raw chunks first;
- one package open and mmap path;
- dense handles;
- roundtrip, corruption, truncation, overlap, and bounds tests;
- A/B manifest and rollback design.

Do not add LZ4/Zstd alternatives before the raw baseline is measured.

## Phase 3 — Prism exact variants

- codec and layout profiler;
- Pareto pruning;
- LZ4 and Zstd alternatives;
- Startup Tape and Critical Render Set;
- material templates;
- page directories;
- shader usage manifest.

## Phase 4 — low-risk Flux modules

One patch and one feature flag at a time:

1. frame/upload arenas;
2. deadline scheduler and stale cancellation;
3. exact residency required/protected sets;
4. exact pose sharing;
5. independent hidden-particle jump;
6. dirty-shadow policy;
7. ML backend audit and prefix policy.

Each starts in `OBSERVE`.

## Phase 5 — Governor

- state machine;
- thresholds, confidence, hysteresis, cooldown;
- budget broker;
- regression guard;
- persisted device profile;
- baseline fallback.

## Phase 6 — device-gated GPU experiments

Separate branches and reports:

- GPU meshlet/HZB;
- indirect draw;
- geometry pages;
- texture pages;
- visibility buffer;
- pipeline cache.

## Phase 7 — real ML backend

- identify backend;
- audit existing KV, prefix, fusion, and mmap support;
- add only missing functions;
- compare resident and windowed weights;
- test adaptive speculative decoding;
- add training/checkpoint work only when local training is actually in scope.

---

# 13. Acceptance and merge rules

For each strict module:

- identical output checksum or documented numerical tolerance;
- zero false-negative visibility;
- zero missing required pages;
- no accepted corruption;
- p95 not worse than baseline after warmup;
- memory budget respected;
- automatic fallback tested;
- runtime flag can disable the module.

A module may become enabled by default only when:

```text
median improvement >= 5%
AND p95/p99 remain within the agreed regression budget
AND no quality or integrity failure occurs
AND cold, warm, and thermal traces do not crash
AND benefit repeats on the target device
```

Risky GPU modules should require a larger margin because of driver variance.

---

# 14. Target-device test gate

Target device:

- TECNO CI8n;
- Android 13;
- Mali-G57 MC2;
- 1080×2352;
- current SOLUM/Filament build.

Required build separation:

```text
A current baseline
B SLPK/Prism exact only
C B plus one Flux module
D C plus Governor
E one separate GPU experiment
```

Required traces:

- at least 5 cold starts;
- at least 30 warm starts for stable startup statistics;
- identical camera replay;
- representative open, enclosed, weather, and combat scenes when available;
- fast camera turn and teleport;
- day/night and rain/snow traces;
- 10–20 minute thermal run.

Metrics:

- time to first frame and map activation;
- CPU/GPU p50, p95, p99;
- jank and stutter count;
- RSS/PSS and GPU budget where available;
- bytes read, decoded, and uploaded;
- package/APK/storage size;
- shader stalls;
- battery and thermal state;
- screenshot or image diff;
- ML TTFT, tokens/s, and peak memory where relevant.

---

# 15. What is and is not established

## Established locally or in the foundation implementation

- fixed mapped data can avoid generic parsing and allocation overhead;
- SoA can improve selective scans;
- precomputed static data can remove runtime transforms and sorting;
- dense IDs avoid hot string/hash lookup;
- codec selection must be measured per payload and device;
- exact material-template dedup can reduce metadata;
- upload arenas can reduce allocation overhead;
- pose sharing helps only when exact states repeat;
- analytic hidden-particle jumps apply only to independent emitters;
- aggressive fixed strategies can be slower than baseline;
- Governor requires a baseline and automatic disable path;
- SLPK v3 writer, reader, integrity, corruption rejection, telemetry contracts, and Android JNI build exist in the CharacterDirectorNative foundation branch.

## Still requires project or device evidence

- total SOLUM FPS improvement;
- real Mali benefit from meshlet/HZB or indirect draws;
- visibility-buffer viability on the tile-based GPU;
- full Filament integration cost;
- real texture/geometry page upload latency;
- pipeline-cache compatibility and benefit;
- thermal stability;
- production UDS SLPK size;
- real local Qwen/Codex tokens/s;
- any claim of a combined 5×–20× engine speedup.

The correct result of a spike may be `NEGATIVE`. That is useful evidence and must not be hidden.
