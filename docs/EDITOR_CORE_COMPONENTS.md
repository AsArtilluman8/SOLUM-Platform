# EDITOR_CORE_COMPONENTS — non-fake editor component rules

Этот файл фиксирует урок из старых editor проектов: вкладка/tool не считается существующей, если это только shell UI без реального workflow.

## LAW: No fake tabs

Вкладка/tool считается реальной только если есть:

- data model;
- видимый UI;
- user-visible action;
- save/load или report;
- validation/debug state;
- clear empty/error state.

Плохо:

```text
есть вкладка MATERIALS
↓
внутри текст “materials coming soon”
↓
нет данных, нет действия, нет сохранения
```

Хорошо:

```text
есть Material Studio v1
↓
создаёт material asset
↓
показывает preview
↓
меняет baseColor/roughness/metallic
↓
сохраняет asset_manifest + material.json
↓
validator показывает status
```

## Core editor components

Базовые компоненты, которые должны переиспользоваться между apps:

```text
SceneHierarchy
Inspector
AssetBrowser
ViewportSelection
Gizmo
ContextToolbar
BottomSheet
UndoRedo
SaveValidation
DiagnosticsPanel
Timeline later
BuildPanel later
```

## SceneHierarchy

Purpose:

- показать объекты сцены;
- selection sync viewport ↔ list;
- rename;
- duplicate;
- delete;
- focus camera;
- visibility;
- lock.

v1 can be flat list with future parentId support.

## Inspector

Purpose:

- показывает свойства выбранного объекта/ассета;
- context-aware sections;
- Basic first, Advanced hidden;
- live update.

Common sections:

```text
Transform
Render
Material
Collision
Placement
Metadata
Diagnostics
```

Inspector не должен быть длинным текстовым блоком.

## AssetBrowser

Purpose:

- scan assets;
- show cards;
- filter/search;
- show validation status;
- add/apply asset.

Card minimal:

```text
preview/fallback icon
type badge
name
schema + status
```

## ViewportSelection

Purpose:

- tap object selects it;
- selected object highlighted;
- object state syncs to inspector/hierarchy;
- deselect works reliably.

## Gizmo

Purpose:

- direct visual transform;
- X/Y/Z colors;
- move/rotate/scale modes;
- surface snap;
- compact precision fallback.

## UndoRedo

Every editor action should become a command where possible:

```text
execute
undo
redo
describe
```

Undo button must be visible.

## SaveValidation

Every save/import must produce a result:

```text
valid
invalid
warning
report path
```

## BuildPanel

Build button must not be decorative.

If Build exists, it must show:

- app/module;
- status;
- log summary;
- output path;
- next action.

If build flow is not implemented, button must be hidden or labelled clearly as unavailable.

## Component readiness checklist

Before a component is called “done”:

```text
[ ] data model exists
[ ] UI exists
[ ] user action exists
[ ] state persists or report exists
[ ] empty state exists
[ ] error state exists
[ ] diagnostics/log state exists if relevant
[ ] mobile layout follows UX rules
```

## Anti-rule

Do not create empty broad editor tabs just to look complete.

Better:

```text
1 real tool with full vertical slice
```

than:

```text
10 tabs with placeholders
```
