# DESIGN_TOKENS — SOLUM UI design tokens

Этот файл фиксирует минимальную дизайн-систему SOLUM.

Цель: все приложения выглядят как одна платформа, но имеют свой accent color.

## Core layout tokens

```text
statusBarHeight = 40dp
bottomNavHeight = 56dp
contextToolbarHeight = 48dp
safeZoneBottom = 32-48dp
minTouchTarget = 44dp
preferredTouchTarget = 48dp
```

## Spacing

```text
space_4  = 4dp
space_8  = 8dp
space_12 = 12dp
space_16 = 16dp
space_24 = 24dp
space_32 = 32dp
```

## Radius

```text
radius_small  = 8dp
radius_medium = 12dp
radius_large  = 16dp
radius_panel  = 18dp
radius_pill   = 999dp
```

## Bottom sheet states

```text
collapsed = 48-64dp
half      = 25-35% screen height
full      = 70-85% screen height
```

Rules:

- bottom sheet must not permanently hide viewport;
- full state scrolls internally;
- during viewport drag, sheet collapses or fades.

## Typography

Sci-fi/display font:

- only logo;
- splash headings;
- major screen title if readable.

Readable sans:

- all working UI;
- buttons;
- labels;
- inspector rows.

Monospace:

- logs;
- code-like values;
- diagnostics raw blocks.

Sizes:

```text
caption = 12sp
body    = 14sp
title   = 16sp
header  = 20sp+
```

Rules:

- Russian labels must fit.
- Avoid long uppercase labels in working UI.
- Important actions need icon + label, not icon-only.

## App accent colors

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

## Status colors

These colors are reserved for status and must not be used as app accent meaning.

```text
Success #43A047
Warning #FFA726
Error   #E53935
Pending #78909C
```

Status must always use:

```text
icon + color + label
```

Never color only.

## Asset type colors

Asset type colors identify content, not app shell.

Examples:

```text
Material  = dark burgundy
VFX       = violet
Character = emerald
Animation = lime/yellow-green
Sound     = orange
World     = earth brown/mocha
Scene     = cyan/teal
Mechanic  = steel teal
```

Rule:

```text
Asset badge = icon + type label + type color
```

## Button types

### Primary

- one primary action per screen;
- filled;
- uses app accent;
- 48dp height.

### Secondary

- outlined/ghost;
- 40-44dp height;
- max 2–3 visible.

### Context

- pill/chip style;
- appears only when context exists;
- 3–5 actions max.

### Advanced

- hidden inside More/Advanced/full bottom sheet;
- list row with arrow.

## Panel visuals

Base:

```text
dark translucent panel
subtle 1dp border
accent border only for active state
soft glow only for branding/splash or selected important control
```

During drag:

```text
opacity = 20-40% or collapsed
```

## Motion

```text
button feedback = 80-120ms
panel open/close = 180-220ms
mode switch = 120-180ms
```

Rules:

- no excessive animations during editing;
- animations must not hide live result;
- reduced motion option can come later.

## Icon rules

- App launcher icon: no small text.
- Wordmark: splash/about/loading only.
- In editor UI, logo small and secondary.
- Tool/action icons must have labels for important actions.
- Adaptive/monochrome icon variant required later.

## Do not

- Do not use glow on body text.
- Do not rely only on color.
- Do not make touch targets smaller than 44dp.
- Do not put interactive controls in Android bottom safe zone.
- Do not use decorative font for logs/body text.
