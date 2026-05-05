# EDITOR_TOOLING_ROADMAP_REFERENCE — lessons from old AAA recovery plan

Этот файл не является текущим roadmap SOLUM Platform. Это reference-список editor/tooling систем, которые делают движок похожим на реальный рабочий инструмент, а не на demo.

## Current SOLUM Platform roadmap remains

```text
Patch 01 — Repo Foundation
Patch 02 — Diagnostics v1 + Vulkan Capability Check
Patch 03 — Asset Schema v1 + Transaction Save
Patch 04 — Vulkan Foundation v1
Patch 05 — Asset Hub v1
Patch 06 — Material Studio v1
Patch 07 — Launcher Foundation
```

Этот файл не меняет порядок.

## Why this exists

Старый AAA recovery plan показал важный UX урок:

```text
engine/editor не ощущается реальным, если вкладки пустые,
Inspector не меняет объект,
Build button декоративная,
ассеты fallback-примитивы,
а scene hierarchy отсутствует.
```

## Editor credibility components

Future editor apps and SOLUM Engine should eventually have:

1. Project settings that actually affect project.
2. Scene hierarchy with real objects.
3. Inspector with real component editing.
4. Viewport gizmo for transform.
5. Real asset pipeline, not primitive fallback as normal path.
6. Material system with preview and assignment.
7. Lighting controls that change real renderer state.
8. Asset browser with search/filter/preview/status.
9. Grounding/placement quality.
10. Save/load with versioning/migration/recovery.
11. Build panel connected to real build flow.
12. Profiler overlay and quality profiles.
13. UI polish pass.

## No fake tabs rule

A tab is not real if it only contains placeholder text.

A tab/tool is real only when it has:

- data model;
- UI;
- user action;
- save/load or report;
- validation/error state;
- diagnostics or status where relevant.

See `docs/EDITOR_CORE_COMPONENTS.md`.

## Editor UX order for future expansion

When SOLUM Engine editor grows, prefer this order:

```text
SceneHierarchy
↓
Inspector
↓
Gizmo
↓
AssetBrowser
↓
Material assignment
↓
Lighting controls
↓
Save/Load reliability
↓
Build panel
↓
Profiler/Quality profiles
```

Do not add gameplay-heavy systems before editor foundation is credible.

## Reference lessons kept

### Scene tab

- object list;
- selection sync viewport ↔ hierarchy;
- rename/duplicate/delete/focus;
- visibility/lock;
- stable UUID.

### Inspector

- Transform;
- Render;
- Material;
- Collision;
- Placement;
- Metadata;
- collapsible sections;
- live update.

### Gizmo

- visible in viewport;
- X/Y/Z colors;
- large touch zones;
- no teleport-like drag;
- numbers only fallback.

### Asset Browser

- search;
- filters;
- preview/fallback icon;
- metadata;
- validation state;
- lazy loading.

### Build Panel

- not decorative;
- shows module/app;
- runs or links to build script;
- shows status/log/output path.

### Save/Load

- schema version;
- asset references;
- missing asset recovery;
- transaction save;
- migration report.

### Profiler / Quality Profiles

- FPS;
- frame time;
- draw calls later;
- visible object count later;
- quality mode;
- low-overhead.

## What not to import from old plan

Do not import:

- OpenGL-first direction;
- Download output policy;
- old patch numbers as current roadmap;
- gameplay-heavy expansion before foundation;
- fallback primitives as normal asset workflow.

## Future use

Before building an editor feature, check this file and `docs/UX_AND_WORKFLOW_RULES.md`.
