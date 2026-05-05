# INPUT_AND_GESTURE_ARCHITECTURE — input ownership rules

Этот файл фиксирует урок из старого SOLUM Engine v2: camera/touch баги появляются, когда несколько классов одновременно владеют одним gesture state.

## Главная проблема, которую запрещаем

Плохо:

```text
Activity владеет camera state
и одновременно View владеет camera state
и overlay dispatch тоже двигает camera
```

Результат:

```text
камера работает до picker
↓
после picker не работает
↓
жесты из Android picker влияют на сцену
↓
модель появляется или обновляется только со второго раза
```

## LAW: One gesture state = one owner

Каждый gesture state имеет одного владельца.

Примеры:

```text
CameraGestureOwner = один модуль
GizmoGestureOwner = один модуль
BottomSheetGestureOwner = один модуль
TimelineGestureOwner = один модуль
NodeGraphGestureOwner = один модуль
```

Нельзя, чтобы Activity и View одновременно изменяли camera yaw/pitch/radius.

## InputRouter

Все touch события проходят через InputRouter.

Алгоритм:

```text
touch event пришёл
↓
InputRouter определяет зону
↓
zone = viewport / gizmo / panel / bottom nav / timeline / system picker
↓
событие получает только один owner
↓
owner возвращает handled/not handled
↓
остальные системы получают только результат, не raw gesture
```

## Zones

```text
ViewportZone      — camera orbit/pan/zoom, object select
GizmoZone         — transform/rotate/scale selected object
BottomSheetZone   — panel expand/collapse/scroll
BottomNavZone     — navigation only
TimelineZone      — scrub/playhead/keyframes
NodeGraphZone     — graph pan/zoom/node drag later
SystemPickerZone  — Android picker; must not control camera
```

## Conflict resolution

- Если палец начал движение в viewport — это viewport/gizmo gesture.
- Если палец начал движение в bottom sheet handle — это panel gesture.
- Если палец начал в timeline — это timeline gesture.
- Нет overlap-зоны, где camera и panel одновременно считают gesture своим.

## Android picker rule

Android ACTION_OPEN_DOCUMENT / picker не должен пропускать жесты в camera/input state.

Flow:

```text
open picker
↓
InputRouter enters externalPickerMode
↓
viewport gestures disabled/suspended
↓
picker returns result/cancel
↓
clear touch state
↓
resume viewport gestures
```

## State reset after external UI

После picker/dialog/permission screen:

```text
reset active pointer id
reset pinch distance
reset drag mode
clear gesture owner
sync camera state once
```

## Camera owner decision

Каждый editor app должен явно выбрать:

### Option A — Activity owns camera

Activity хранит camera state, View только показывает surface/viewport.

### Option B — View owns camera

View хранит camera state, Activity только shell/picker/navigation.

Запрещено смешивать A и B.

## Diagnostics

Input diagnostics should report:

```json
{
  "activeGestureOwner": "ViewportCamera",
  "activeZone": "ViewportZone",
  "externalPickerMode": false,
  "cameraOwner": "StageView",
  "lastGesture": "drag_empty_rotate_camera"
}
```

## UI rule

Не чинить camera/input баги кнопками движения камеры.

Плохой fix:

```text
drag не работает → добавить кнопки поворота камеры
```

Правильный fix:

```text
найти owner conflict → убрать лишний handler → восстановить gesture ownership
```
