# PERFORMANCE_BUDGETS — FPS, profiling and performance rules

SOLUM target is AAA-like mobile graphics, but mobile performance must be protected from the start.

## Target FPS tiers

```text
Tier A: 60 FPS target → 16.6ms frame budget
Tier B: 45 FPS target → 22.2ms frame budget
Tier C: 30 FPS fallback → 33.3ms frame budget
```

Default early target:

```text
stable 45-60 FPS on target device where possible
```

## Frame budget meaning

At 60 FPS, total frame must fit in 16.6ms:

```text
CPU update
+ render submit
+ GPU work
+ UI overlay
+ diagnostics overhead
<= 16.6ms
```

## Diagnostics overhead rule

Diagnostics must not become the performance bug.

Rules:

- Heavy metrics are not sampled every frame.
- Expensive reports run on user action or low-frequency timer.
- FPS counter uses lightweight rolling average.
- GPU timing availability must be detected, not assumed.
- If timing is CPU approximation, report must say so.
- Diagnostics overhead target should be below 1ms average in normal mode.

## Performance snapshots

Each large patch should create a performance snapshot when runtime exists.

Minimum fields:

```json
{
  "timestamp": "ISO-8601",
  "patch": "PXX",
  "commit": "...",
  "device": "...",
  "scene": "...",
  "fpsAvg": 0,
  "fpsMin": 0,
  "frameMsAvg": 0,
  "cpuMsApprox": 0,
  "gpuMsApprox": null,
  "ramMb": 0,
  "thermalState": "unknown",
  "enabledFeatures": [],
  "notes": ""
}
```

## Regression rule

If FPS drops after a patch:

```text
compare previous snapshot
↓
list changed systems/files
↓
identify likely cost area
↓
do targeted diagnostics
↓
fix actual cause
```

Do not randomly disable systems to claim improvement.

## Render feature budget examples

Early budgets are placeholders and must be refined by diagnostics.

```text
Base renderer / clear / triangle: near-zero baseline
Material preview v1: low cost, one object
Asset Hub UI: no Vulkan dependency by default
Diagnostics normal mode: low overhead
Debug heavy mode: allowed to cost more, but marked clearly
```

Future systems need explicit budgets:

- shadow pass;
- CSM cascades;
- material shader complexity;
- VFX overdraw;
- terrain/foliage;
- water/post-process;
- UI overlay.

## Mobile GPU rules

For Mali/tile-based GPUs:

- avoid unnecessary bandwidth;
- avoid expensive full-screen debug branches;
- avoid overdraw-heavy UI/VFX;
- avoid monolithic shaders with many runtime debug branches;
- prefer feature flags/specialization/variants where appropriate;
- keep render passes mobile-friendly with correct load/store choices.

## Asset budgets v1 placeholders

To be refined later:

```text
Texture sizes: prefer mobile-compressed formats later
Meshes: optimize with meshoptimizer later
Materials: track texture sample count and shader cost tier
VFX: track particles/overdraw/lifetime
Characters: track bones/material count/draw calls later
```

## Definition of done for optimization patch

Optimization patch must include:

- before measurement;
- after measurement;
- changed systems;
- evidence path;
- known tradeoffs;
- no fake quality regression hidden as optimization.
