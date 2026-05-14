# SOLUM Impact Field System Roadmap

## Назначение

Этот документ фиксирует будущую систему реакции мира в SOLUM Engine и будущей игре.

Цель: одна общая event-система для ветра, шагов, ударов мечом, магии, взрывов, воды, травы, волос, листвы, трещин, кратеров, пыли, камеры, звука и восстановления мира.

```text
Impact Field = временное событие в мире, на которое разные системы реагируют по-своему.
```

Это будущий gameplay/world layer. Он не должен ломать текущий план:

```text
материалы -> отражения -> стекло -> свет -> тени -> profiling/adaptive systems -> world interaction
```

## Главная идея

Вместо отдельной системы для каждой реакции SOLUM создаёт impact events:

```text
position
radius
direction
strength
type
lifetime
falloff
timestamp
```

Разные подсистемы читают эти события и реагируют дешёвым способом.

## Типы impact events

Начальный набор:

```text
wind
footstep
slash
projectile
explosion
magic
shockwave
water_hit
terrain_hit
tree_hit
```

## Что может реагировать

Будущие системы:

- трава сгибается;
- волосы качаются;
- листья дрожат;
- ветки трясутся;
- вода получает ripple;
- пыль появляется;
- стены/пол получают cracks/burn/slash decals;
- земля получает crater overlay;
- debris chunks появляются на короткое время;
- камера получает shake;
- звук запускается;
- магия даёт glow/flash.

## Трава

Не симулировать каждую травинку.

Использовать:

- near: grass cards/blade clusters;
- mid: card clusters/billboards;
- far: density texture / ground material noise.

Vertex shader может считать:

```text
grassBend = wind + localImpact + recovery
```

Impact fields должны затухать, чтобы трава постепенно вставала назад.

## Волосы

Не делать full strand simulation на телефоне.

Использовать hair cards, сгруппированные в locks.

Уровни качества:

```text
Tier 0: static cards
Tier 1: vertex shader sway
Tier 2: spring bones for large locks
Tier 3: fake collision + wind/magic/attack impulses
```

Волосы реагируют на:

- движение персонажа;
- wind fields;
- magic impact fields;
- attack impulse;
- camera/animation velocity.

## Деревья и листва

Представление дерева:

- trunk mesh;
- branch meshes/cards;
- leaf cards/clusters/billboards.

Реакции:

- лёгкий удар: листья/ветки трясутся;
- средний удар: bark crack/decal;
- сильный удар: pre-fractured chunk detaches;
- очень сильный удар: swap to broken version.

Полный real-time tree fracture не делать для обычных объектов. Только pre-fractured hero objects.

## Разрушения и кратеры

Использовать staged destruction, не full physics везде.

```text
Level 1: visual decal: crack, burn, slash, dirt.
Level 2: normal/parallax fake: damage normal/height illusion.
Level 3: crater overlay mesh: raised rim, stones, dust.
Level 4: pre-fractured object: hidden chunks become visible.
Level 5: real dynamic deformation only for specific hero cases.
```

## Восстановление мира

Fantasy-мир может восстанавливаться:

- crater появляется мгновенно;
- dust fades;
- cracks glow, потом темнеют;
- crater overlay fades за 20-60 секунд;
- grass rises back;
- debris despawns;
- broken objects reset when far away / zone reload.

## Пример сильного магического удара

Одно impact event может вызвать:

```text
magic flash
grass wave outward
leaf shake
dust cloud
crater overlay
cracks glowing for 2 seconds
small debris burst
camera shake
sound cue
slow restoration
```

## Правила безопасности для телефона

- Ограничить количество активных impacts.
- Маленький active impact buffer.
- Не выделять память каждый кадр.
- Не спавнить бесконечные частицы.
- Не использовать full rigid-body physics для каждого куска.
- Не деформировать весь terrain mesh по умолчанию.
- Сначала использовать decals/overlay meshes.

## Будущий порядок патчей

После renderer/material/lighting foundation:

```text
Impact Field v1: data model + debug overlay + simple radial field
Grass Reaction v1: grass bend from wind/impact
Water Ripple Hook v1: ripple from impact
Damage Decal v1: crack/burn/slash marks
Crater Overlay v1: local mesh/decal with fade
Pre-Fractured Hero Object v1: tree/wall chunks
World Restoration v1: fade/reset damage over time
```

## Чего не делать рано

Избегать рано:

- full real-time terrain boolean destruction;
- full tree fracture simulation;
- full cloth/hair physics;
- unlimited debris physics;
- heavy GPU particles before profiling;
- impact fields depending on heavy scene queries.

## Коротко

Impact Field System позволит будущему миру SOLUM реагировать на меч, магию, ветер, шаги, взрывы и восстановление через одну общую дешёвую систему событий.
