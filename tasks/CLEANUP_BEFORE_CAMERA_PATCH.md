# Cleanup plan before camera patch

Date: 2026-06-16

This is a non-destructive cleanup plan. It should be followed before the next camera/gizmo patch.

## Goals

- Reduce Codex confusion.
- Avoid mixing unrelated native/weather/glass work with camera fixes.
- Preserve the Blueprint pin-link milestone.
- Keep Download artifacts out of patch context unless explicitly needed.

## Current local risk areas from audit

### Dirty tracked files

Latest local audit showed modified native files under `engine-core/solum-vulkan-core/`:

```text
shaders/glass.frag.glsl
src/generated/solum_glass_frag_spv.h
src/solum/render_lab.hpp
src/solum/renderer_core.hpp
src/solum/renderer_types.hpp
src/solum/runtime_diagnostics.hpp
src/solum_engine.cpp
```

These are unrelated to a camera-only patch and must not be bundled into it.

### Untracked files/directories

```text
assets/weather/
tools/solum_blueprint_pinlink_extractor.py
```

`tools/solum_blueprint_pinlink_extractor.py` is valuable and should be preserved. `assets/weather/` needs classification before any commit.

## Recommended local steps

### Step 1: safety snapshot

Create a zip snapshot containing status, diffs, key files, and file lists before cleanup.

### Step 2: classify worktree

Classify files into:

1. keep as important WIP;
2. archive-only;
3. safe to ignore;
4. candidate for revert after user confirmation;
5. candidate for commit after review.

### Step 3: isolate camera patch

Before camera patch, ensure `git diff -- apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java` is the only production-code diff related to camera.

### Step 4: keep Codex narrow

For camera task, explicitly point Codex to:

```text
tasks/ACTIVE_CODEX_HANDOFF.md
apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java
```

And explicitly forbid:

```text
/storage/emulated/0/Download
engine-core/solum-vulkan-core
assets/weather
UDS/UDW/weather
glass/material pipeline
```

## Do not do without user confirmation

- `git reset --hard`
- `git clean`
- deleting files
- committing unrelated dirty native files
- pushing branches
- creating PRs
- merging main

## Camera patch acceptance criteria

The next camera patch is acceptable only if:

- `git diff` shows real changes in `FilamentGlbPreviewActivity.java` or clearly justified helper files;
- one camera owner is proven in code;
- UI touch does not move the scene;
- touch move does not persist settings or refresh full UI each event;
- debug status contains camera owner and active gesture state;
- build succeeds;
- runtime test instructions are specific.

## Performance patch acceptance criteria

Performance patch is separate from camera patch. It is acceptable only if:

- LOW/BAD disable TAA and MSAA 4x;
- HUD shows actual TAA/MSAA separately from profile label;
- user can see why FPS is BAD;
- no weather/glass changes are mixed in.
