# UX_AND_WORKFLOW_RULES — SOLUM UI/UX и workflow

SOLUM UI должен быть профессиональным mobile-first editor UI, а не desktop interface, сжатый под телефон.

Главная формула:

```text
smart auto → direct visual control → compact precision → advanced override
```

Пользователь должен управлять смыслом, а не воевать с координатами и кнопками.

## LAW 01: Viewport is sacred

Главное место экрана — результат:

- 3D viewport;
- material preview;
- animation preview;
- VFX preview;
- scene preview;
- timeline playback.

Правила:

- viewport занимает минимум 60% экрана в portrait editor mode;
- постоянная левая панель шире 20% экрана на телефоне запрещена;
- центр объекта нельзя закрывать постоянной панелью;
- debug overlays выключены по умолчанию;
- статусная строка компактная.

Допустимо:

- узкий vertical tool rail 48–56dp;
- bottom sheet;
- floating context toolbar;
- landscape/tablet split view, если viewport остаётся не меньше 60%.

## LAW 02: One screen = one focus

Один экран/режим имеет один главный фокус.

Примеры:

```text
Material mode → material controls visible, animation/transform hidden
Animation mode → clips/timeline visible, material/transform hidden
World Sculpt mode → brush controls visible, asset/material controls hidden
```

Запрещено показывать transform, state machine, clips, timeline, material и lighting одновременно на телефоне.

## LAW 03: Progressive disclosure

Показывать сложность слоями:

```text
Basic → Pro → Advanced → Debug
```

Basic: 3–5 главных параметров.

Advanced: только после свайпа вверх / More / Advanced.

Debug: только в Debug mode.

## LAW 04: Smart auto first

Инструмент сначала делает хороший результат автоматически.

Пример transform:

- surface snap включён по умолчанию;
- объект не проваливается под пол;
- overlap показывает warning, но не всегда запрещает;
- пользователь может отключить snap и сделать manual override.

Автоматика должна помогать, а не быть тюрьмой.

## LAW 05: Touch before numbers

Главный способ редактирования — touch/drag/gizmo.

Числа — дополнение для точности.

Правильно:

```text
object selected
↓
on-object gizmo appears
↓
drag axis or plane
↓
live object movement
↓
compact numeric scrub available
```

Неправильно:

```text
+X -X +Y -Y +Z -Z как главный transform tool
```

## LAW 06: Panel collapses during drag

Когда пользователь начал drag в viewport:

- bottom sheet сворачивается или становится прозрачной;
- floating panels не закрывают объект;
- после отпускания возвращаются.

## LAW 07: Color + icon + label

Нельзя передавать смысл только цветом.

Всегда:

```text
icon + color + label
```

Особенно для:

- asset type;
- validation status;
- warning/error;
- compatibility state.

## LAW 08: Status bar visible by default

Статусная строка видна по умолчанию:

- FPS;
- CPU/GPU ms если есть;
- Renderer: Vulkan;
- Quality;
- temperature optional.

Можно скрывать только в явном Preview/Cinematic mode.

Один tap должен возвращать status UI.

## LAW 09: Undo is one tap

Undo/redo должны быть видимыми и быстрыми.

- Undo кнопка не прячется в deep menu.
- Long press Undo позже может открыть action history.
- Shake gesture не использовать как основной undo.

## LAW 10: Russian labels fit

UI проектируется под русский текст.

Русский часто длиннее английского на 20–40%.

Правила:

- не обрезать важные labels;
- использовать icon + short label;
- не злоупотреблять uppercase/spaced letters в рабочем UI;
- sci-fi font только для логотипов/главных заголовков;
- body text должен быть readable sans.

## LAW 11: Debug is a mode

Debug не должен засорять обычный UI.

Обычный режим:

- маленькая status bar;
- рабочие инструменты;
- без debug-кнопок поверх сцены.

Debug mode:

- FPS graph;
- render flags;
- Vulkan info;
- shader/material state;
- export diagnostics.

## LAW 12: No duplicate controls

Один параметр — один владелец.

Пример:

```text
Brush Radius owner = Brush Bottom Sheet
Viewport показывает визуальный круг кисти
Floating mini panel может показывать read-only значение или quick scrub, но не второй полноценный slider
```

## LAW 13: Safe zone bottom

Нижняя зона экрана должна учитывать Android navigation bar.

Интерактивные элементы нельзя ставить в unsafe bottom area.

Минимум safe zone: 32–48dp.

## Layout pattern

Базовый layout для editor apps:

```text
Top Status Bar
↓
Viewport / Preview
↓
Floating Context Toolbar
↓
Bottom Sheet Inspector
↓
Bottom Navigation
↓
Android Safe Zone
```

## Bottom sheet states

Каждая основная панель имеет 3 состояния:

```text
collapsed → half → full
```

- Collapsed: 48–64dp, ручка + короткий статус.
- Half: 25–35% высоты экрана, 3–5 главных параметров.
- Full: 70–85% высоты экрана, advanced + scroll.

Алгоритм:

```text
пользователь выбрал объект
↓
появилась compact panel
↓
видны 3–5 главных действий
↓
свайп вверх открывает больше параметров
↓
ещё свайп вверх открывает advanced
↓
начал drag
↓
панель прозрачная/свёрнутая
↓
отпустил
↓
панель возвращается
```

## Buttons

Типы кнопок:

### Primary

- 1 на экран.
- Filled, accent color.
- Пример: Save, Export, Apply.

### Secondary

- 2–3 на экран.
- Outlined/ghost.
- Пример: Reset, Compare, Duplicate.

### Context

- 3–5, только при выборе объекта/ассета.
- Pill buttons или icon+label chips.
- Пример: Move, Rotate, Scale, Snap, Material.

### Advanced

- Скрыто по умолчанию.
- Внутри Full bottom sheet или отдельного screen.
- Пример: Node Graph, Shader Debug, Import Settings.

## Bottom navigation

Максимум 4–5 вкладок.

Нельзя делать 8 вкладок одновременно.

Пример:

```text
Scene | Assets | Create | Debug | More
```

## Transform / Gizmo UX

Правильный минимум:

1. Tap object → select.
2. On-object gizmo appears.
3. Move/Rotate/Scale modes.
4. Axis colors:
   - X = red
   - Y = green
   - Z = blue
5. Surface snap by default.
6. Object does not fall through floor.
7. Overlap states:
   - green = valid
   - yellow = overlap allowed
   - red = invalid/collision problem
8. Camera-aware axis/plane movement.
9. Compact precision controls:

```text
Pos X 1.25 <>  Y 0.00 <>  Z -3.40 <>
Rot X 0°   <>  Y 45°  <>  Z 0°    <>
Scale 1.00 <>
```

`<>` = scrub control.

Algorithm:

```text
hold <> + drag right → value increases
hold <> + drag left → value decreases
tap value → exact numeric input
```

## Gesture map

### Viewport gestures

```text
1 finger tap empty → deselect
1 finger tap object → select
1 finger drag empty → rotate camera/orbit
1 finger drag gizmo → edit object/tool
2 finger pinch → zoom
2 finger drag → pan camera
2 finger twist → optional roll/orbit, can be disabled
long press object → context actions
long press empty → placement options
double tap object → focus camera on object
double tap empty → reset/focus scene
```

### Bottom sheet gestures

```text
swipe up → collapsed → half → full
swipe down → full → half → collapsed
tap handle → collapsed/half toggle
```

## Scroll rules

- Viewport не скроллится, только camera/tool gestures.
- Inspector скроллится вертикально только в full bottom sheet.
- Asset grid скроллится вертикально.
- Timeline скроллится горизонтально.
- Top status bar не скроллится.
- Bottom nav не скроллится.
- Node graph позже имеет отдельный pan/zoom контекст.

## Color system

Разделить:

### App accent

Цвет приложения/брендинга/активной навигации.

### Asset type color

Цвет типа ассета. Всегда с icon + label.

### Status color

Зарезервированные цвета состояния. Всегда с icon + label.

Предложенная палитра:

```text
Launcher         #00BFFF  cyan / electric blue
Engine           #00E5CC  teal-cyan
Asset Hub        #FFB300  amber/gold
AniStudio        #E91E8C  magenta/hot pink
Character Studio #00C853  emerald green
Motion Studio    #AEEA00  yellow-lime
Material Studio  #B71C1C  dark burgundy/crimson
VFX Studio       #7C4DFF  deep violet
Sound Studio     #FF6D00  deep orange
World Studio     #8D6E63  earth brown/mocha
Diagnostics      #FF8F00  amber-orange
AI Studio        #1565C0  deep blue
Quest/Dialogue   #4527A0  deep indigo
Mechanics        #00695C  steel teal
```

Status colors:

```text
Success #43A047
Warning #FFA726
Error   #E53935
Pending #78909C
```

## UI v1 required components

Минимум для первого UI:

1. Status Bar.
2. Bottom Navigation.
3. Asset Card.
4. Bottom Sheet.
5. Inspector Row.
6. Basic Gizmo.
7. Context Toolbar.
8. Undo/Redo buttons.
9. Design Tokens file.

## Отложить

- Node Graph UI.
- Context Ring Menu.
- Blur panels.
- State Machine editor.
- Timeline keyframe editor.
- Multi-window/split view.
- Light mode.
- Reduced motion settings.
- Accessibility mode.

## UI запреты для агентов

Запрещено:

- постоянная левая панель шире 20% экрана на телефоне;
- больше 5 вкладок в bottom nav;
- `+X -X +Y -Y +Z -Z` как основной transform tool;
- показывать больше 5 параметров без progressive disclosure;
- дублировать один параметр в двух местах;
- использовать только цвет для статуса;
- использовать app accent color для error/warning;
- debug-кнопки в production UI;
- touch target меньше 44dp;
- перекрывать центр viewport;
- node graph в v1 Studio app;
- важные кнопки только иконкой без label;
- обрезать русский текст;
- использовать Windows/Linux paths в UI (`C:\`, `/mnt/data`).
