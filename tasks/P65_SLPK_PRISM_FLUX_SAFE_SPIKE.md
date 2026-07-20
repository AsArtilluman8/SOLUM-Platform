# P65 — SLPK / Prism / Flux Safe Integration Spike

Status: **authorized research and exact-baseline task; not authorization to rewrite the renderer or enable every optimization**.

Primary architecture reference:

- `docs/runtime/SOLUM_SLPK_PRISM_FLUX_GOVERNOR.md`

Related current working plan:

- `docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md`
- `ROADMAP.md`

Reference implementation outside this repository:

- `AsArtilluman8/CharacterDirectorNative`
- branch `foundation/solum-stage1-slpk-ui`
- draft PR 4

## Goal

Determine whether the existing SLPK v3 exact container foundation can be integrated into the current SOLUM/UDS runtime without changing visual output, and establish honest telemetry before any adaptive optimization is enabled.

The task is successful even when the measured result is neutral or negative, provided the evidence is correct and the baseline is preserved.

## Non-negotiable rules

1. Inspect the current repository before changing code.
2. Current code, tests, assets, and traces outrank this document when they conflict.
3. Do not invent decoded UDS data, textures, materials, Niagara systems, meshes, or performance results.
4. Do not replace verified payloads with demo geometry, placeholders, or synthetic previews.
5. Do not claim an optimization benefit from a microbenchmark alone.
6. Keep `STRICT_EXACT`, `DISTRIBUTION_EXACT`, and `BOUNDED_PERCEPTUAL` separate.
7. The first integration must be raw/passthrough exact data. No LZ4/Zstd, paging, meshlets, visibility buffer, or perceptual quality in the same patch.
8. New Flux modules remain disabled or `OBSERVE`.
9. Preserve the current Filament path. Do not recreate Filament material compilation, glTF loading, FrameGraph, or renderer abstractions.
10. Keep a runtime feature flag and a simple baseline fallback.
11. Do not merge or enable by default before tests and target-device evidence.
12. Record negative results.

## Recommended model

Use the strongest available Codex reasoning mode for repository inspection, binary format review, native C++ integration, and benchmark design.

Do not spend a high-reasoning run on visual polish or unrelated UDS features during this spike.

## Step 0 — repository checkpoint

Before edits, report:

```text
pwd
git branch --show-current
git status --short
git log -5 --oneline
```

Create a checkpoint commit or branch only if the current worktree policy permits it. Never discard unrelated user changes.

## Step 1 — read and map existing systems

Read at minimum:

- `ROADMAP.md`;
- `docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md`;
- `docs/runtime/SOLUM_SLPK_PRISM_FLUX_GOVERNOR.md`;
- current build instructions and performance budgets;
- current asset loading, GLB/glTF, texture, material, UDS/UDW, and telemetry code;
- the CharacterDirectorNative SLPK records/tests or a local copied reference if already present.

Produce a short map:

```text
current asset entry points
current Filament loader path
current file-copy and allocation path
current cache/streaming path
current telemetry path
current UDS verified asset categories
safe SLPK integration seam
```

Do not begin implementation until the seam is identified.

## Step 2 — establish baseline telemetry

Add or verify output-neutral measurements for the selected path:

- elapsed package/file open time;
- source bytes read;
- temporary bytes allocated or copied where measurable;
- Java heap, native heap, and PSS before/after on Android;
- time to Filament model/resource acceptance;
- presented/rendered frame distinction where available;
- CPU frame p50/p95/p99;
- GPU timing only when genuinely available, otherwise `UNAVAILABLE`;
- integrity status;
- thermal status where available.

Do not label Choreographer or CPU timing as GPU time.

## Step 3 — select one exact test slice

Choose exactly one verified slice, prioritizing the smallest useful real path.

Preferred order:

1. one existing GLB plus its known material/texture dependencies;
2. a verified UDS curve/contract group plus provenance;
3. verified audio plus seek/runtime metadata;
4. another already-supported real asset group.

Do not choose cooked texture/mesh/Niagara output that the current decoder cannot verify.

Document:

- input files and hashes;
- current source-of-truth status;
- expected runtime output;
- why the slice is small enough for an isolated test.

## Step 4 — integrate SLPK exact baseline

Use the existing SLPK v3 implementation as the reference. Prefer sharing or porting records/tests over designing a conflicting format.

Required first path:

```text
verified source payload
-> raw/passthrough SLPK resource
-> deterministic writer
-> package SHA-256 and per-chunk CRC32
-> reopen with checked reader
-> extract or map exact payload
-> existing Filament/runtime consumer
```

Required properties:

- dense package-local resource handles;
- fixed versioned records;
- checked arithmetic and bounds;
- truncation rejection;
- corruption rejection;
- overlap/reference validation;
- deterministic rebuild for identical inputs;
- provenance link from source to SLPK resource;
- no hidden Java `byte[]` duplication for large payloads when mmap/file mapping is practical.

The first patch may use one package and raw chunks only.

## Step 5 — A/B test

Create two explicit paths:

```text
A current baseline loader
B SLPK exact baseline loader
```

Use identical input and scene state.

Compare:

- visual/output hash or screenshot evidence;
- load and acceptance time;
- bytes read;
- bytes copied;
- temporary memory;
- Java/native/PSS memory;
- package and APK/storage size;
- frame p50/p95/p99 after load;
- failures and fallback behavior.

Do not remove path A.

## Step 6 — optional Prism micro-spike

Only after Step 5 passes, one offline experiment may be added without changing runtime output:

- dense ID table generation;
- dependency ordering;
- a Critical Render Set reference list;
- raw AoS vs SoA scan comparison;
- precomputed draw keys or transforms for a synthetic isolated benchmark;
- codec measurement tool that reports results but does not enable compression.

Any such result must carry `LOCAL_CONFIRMED`, `PROJECT_REQUIRED`, or `DEVICE_REQUIRED` labels.

Do not add several Prism features together.

## Step 7 — Flux remains observe-only

Create no active adaptive optimization unless separately authorized.

Permitted in this task:

- Flux state enum/contracts;
- telemetry registration;
- overhead measurement;
- a report showing hypothetical enable predicates;
- baseline fallback test.

Not permitted in this task:

- active meshlet/HZB culling;
- active partial skinning;
- active shadow reuse;
- active predictive residency;
- active particle skipping;
- active ML speculative decoding;
- perceptual quality changes.

## Required tests

### Format

- deterministic roundtrip;
- empty and minimum package;
- truncated header/table/payload;
- offset overflow;
- out-of-file range;
- overlapping chunks;
- invalid resource references;
- corrupted resource and chunk tables;
- corrupted payload;
- root SHA-256 mismatch;
- per-chunk CRC mismatch;
- 4 KiB and 16 KiB alignment assumptions.

### Runtime

- baseline path still works;
- SLPK path loads the same exact payload;
- feature flag off restores baseline;
- failed SLPK validation never reaches Filament;
- no placeholder is substituted;
- cancellation or activity restart does not leak mapped buffers;
- repeated load does not grow memory indefinitely.

### Android/device report

Export one structured JSON report containing:

```text
device and build
input hashes
package hash and resource count
integrity results
baseline metrics
SLPK metrics
memory before/peak/after where available
visual evidence path
feature flags
status labels
negative results
final PASS/PARTIAL/FAIL
```

## Acceptance criteria

The spike may be marked `PASS` only when:

- the selected real asset is loaded by both paths;
- output is identical or the documented numerical/image tolerance passes;
- corruption and truncation are rejected;
- no new fake or placeholder path exists;
- baseline fallback works;
- tests pass;
- the report makes no unsupported speed claim.

A performance optimization may not be enabled by default in this task.

## Stop conditions

Stop and report rather than guessing when:

- current SOLUM records conflict with the reference SLPK layout;
- the selected UDS asset is not verified;
- Filament consumes or owns the buffer in a way that makes the proposed mapping unsafe;
- a required Android API is unavailable;
- the test requires a renderer fork not already present;
- build or tests expose unrelated repository breakage;
- the isolated patch would become a broad renderer rewrite.

## Expected deliverables

```text
implementation diff
format/runtime tests
A/B benchmark command or device button
structured JSON report schema
one sample report from available environment
WORK_LOG or equivalent update
honest summary of confirmed, required, and negative results
```

## Prompt to give Codex

```text
Прочитай полностью:
- tasks/P65_SLPK_PRISM_FLUX_SAFE_SPIKE.md
- docs/runtime/SOLUM_SLPK_PRISM_FLUX_GOVERNOR.md
- docs/SOLUM_UDS_RENDERER_WORKING_PLAN.md
- ROADMAP.md

Сначала исследуй текущий репозиторий и существующие загрузчики/телеметрию. Затем выполни только безопасный exact-baseline spike из задачи: один реальный проверенный asset slice, baseline A против SLPK B, честные тесты и отчёт. Не включай сразу Flux, meshlets, perceptual quality, compression или несколько оптимизаций. Не придумывай UDS-данные и не подменяй отсутствующий декодер заглушкой. Сохрани baseline и negative results. Перед изменениями покажи краткий план и точку интеграции; после — фактические тесты, diff и ограничения.
```
