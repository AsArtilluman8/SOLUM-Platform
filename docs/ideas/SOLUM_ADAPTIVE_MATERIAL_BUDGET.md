# SOLUM Adaptive Material Budget Roadmap

## Назначение

Этот документ фиксирует будущую систему адаптивного качества материалов.

Главная цель: не каждый материал должен всегда использовать самый дорогой путь. На мобильном GPU дорогой материал нужен только там, где игрок видит разницу.

```text
Adaptive Material Budget = система, которая выбирает качество материала по важности, расстоянию, экранной площади, движению и бюджету кадра.
```

## Почему это важно

SOLUM уже имеет:

- material slots;
- presets;
- gloss;
- clearcoat;
- emissive;
- alpha/cutout;
- fake IBL/reflection.

Если всё это всегда считать на всех пикселях, большая сцена может стать тяжёлой.

Нужно заранее готовить систему уровней качества.

## Уровни качества

```text
Tier 0: base color only.
Tier 1: base color + cheap light.
Tier 2: normal map + roughness approximation.
Tier 3: PBR-like lighting + environment reflection.
Tier 4: hero material with clearcoat/detail/strong reflection.
```

## Что влияет на выбор Tier

- объект в центре экрана;
- объект важен для игрока;
- объект близко;
- объект большой на экране;
- это металл/стекло/вода/магия;
- объект на силуэте;
- камера стоит или медленно движется;
- frame time позволяет поднять качество.

Качество ниже, если:

- объект далеко;
- объект маленький;
- объект на периферии;
- камера быстро движется;
- frame time высокий;
- материал простой: грязь, дальняя стена, дальний пол.

## Нельзя резко переключать качество

Плохой пример:

```text
кадр 1: cheap material
кадр 2: full material
```

Это вызовет pop/flicker.

Правильный вариант:

```text
qualityBlend 0.0 -> 1.0 за 200-800 мс
```

Смешивать:

- normal strength;
- roughness response;
- specular strength;
- reflection strength;
- clearcoat strength;
- detail layer strength.

## Hysteresis и cooldown

Чтобы материал не мигал между уровнями:

- повышать качество только если несколько кадров/секунд всё стабильно;
- снижать качество плавно;
- не менять уровень слишком часто;
- иметь cooldown после переключения.

## Debug view

Будущие debug modes:

- Material Tier View;
- Quality Blend View;
- Importance View;
- Expensive Material Heatmap;
- Reflection Tier View;
- Overdraw / Heavy Pixel View.

## Диагностика

Будущие JSON поля:

```text
adaptiveMaterialBudgetStatus
materialTierMode
materialTierSelected
materialTierBlend
materialTierTransitionStatus
materialTierHysteresisStatus
materialTierPerformanceStatus
expensiveMaterialPixelEstimate
heroMaterialCount
cheapMaterialCount
```

## Связь с Reflection Tiers

Материал и отражения должны быть связаны:

```text
Tier 0 material -> no reflection
Tier 1 material -> sky color only
Tier 2 material -> fake probe
Tier 3 material -> probe + Fresnel + roughness blur
Tier 4 material -> high quality hero reflection
```

## Связь с Impact Field

Если объект только что получил удар/магический эффект, его importance временно повышается:

```text
impact happened -> local material quality rises -> cracks/glow/decal visible -> later fades back
```

Это даст эффектный мир без постоянной высокой стоимости.

## Когда внедрять

Не внедрять до завершения базовых материалов/стекла/отражений.

Рекомендуемый порядок:

```text
1. Stable Frame Profiling Lab
2. Material Quality Tiers
3. Smooth Quality Transitions
4. Importance Map v1
5. Adaptive Reflection Tiers
6. Fantasy Material Layer Pack
```

## Чего избегать

- sudden quality switching;
- разные яркости между tiers;
- ML every frame;
- тяжёлые ветвления в fragment shader;
- создание pipeline/shader во время gameplay;
- debug branches в production path.

## Коротко

Adaptive Material Budget позволит SOLUM держать красивый вид на телефоне: дорогие материалы только там, где они реально заметны, дешёвые материалы там, где игрок не увидит разницы.
