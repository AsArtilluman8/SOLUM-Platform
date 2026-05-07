# RENDER_BACKEND_DECISION — P02C

## Current backend

Current backend:

```text
direct Vulkan fallback
```

Meaning:

- SOLUM Engine owns the Android/Vulkan runtime path.
- Existing Vulkan code remains the active runtime foundation.
- Diagnostics must report actual engine state, not companion-only evidence.

## The Forge

The Forge status:

```text
reference / adapter target
```

Use The Forge for:

- renderer abstraction ideas;
- resource lifetime patterns;
- descriptor management principles;
- barrier/synchronization references;
- future render graph concepts.

Do not:

- import The Forge wholesale;
- replace SOLUM architecture with The Forge architecture;
- add The Forge as a dependency without a separate license/build/Termux decision.

## Decision

For P02C:

```text
REFERENCE_ONLY now, possible ADAPTER later.
```

The current patch adds Render Lab state/config only. It does not claim cube, depth, shadow, material or import rendering is implemented.

## Next real Vulkan step

Next real Vulkan step:

```text
P03 real Vulkan cube + camera + depth
```

P03 must prove:

- real cube geometry;
- camera/projection;
- depth attachment;
- diagnostics state;
- build success;
- runtime evidence.
