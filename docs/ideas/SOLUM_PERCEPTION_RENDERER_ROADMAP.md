# SOLUM Perception Renderer Roadmap

## Назначение

Этот документ фиксирует долгосрочное направление рендера и оптимизации SOLUM Engine.

Главная идея: не рисовать всё максимально дорого. Нужно тратить GPU/CPU бюджет только там, где игрок реально заметит качество.

```text
SOLUM Perception Renderer = адаптивный мобильный рендерер, который выбирает качество по важности объекта, месту на экране, движению камеры, типу материала, видимости и бюджету кадра.
```

Это не замена текущему плану. Текущий порядок остаётся:

```text
материалы -> отражения -> стекло -> свет -> тени -> профилирование -> адаптивный рендер -> мир и взаимодействия
```

## Основные правила

1. Не всё должно рендериться с одинаковым дорогим качеством.
2. Центр экрана, важные объекты, герой, оружие, лицо, стекло, металл, вода и магия получают больше качества.
3. Периферия, дальние объекты и быстро движущиеся области могут быть дешевле.
4. Во время быстрого движения камеры качество можно временно снижать.
5. Когда камера останавливается, качество плавно повышается.
6. Нельзя резко менять качество на силуэтах, лицах, важных объектах и в центре экрана.
7. Все переходы качества должны быть плавными, без мерцания и скачков.
8. FPS важен, но стабильность кадра важнее пикового FPS.

## Ближайший план не менять

Сначала нужно закрыть визуальную базу:

```text
P24 Better Environment Reflection / Fake Cubemap Probe
P25 Glass v1 / Transparent Material Foundation
P26 Glass Polish / Tint / Fresnel / Fake Thickness
P27 Reflection Probe / Cubemap IBL Foundation
```

После этого можно добавлять блок адаптивного рендера:

```text
P28 Stable Frame Profiling Lab
P29 Material Quality Tiers
P30 Smooth Quality Transitions
P31 Importance Map v1 without ML
P32 Adaptive Reflection Tiers
P33 Fantasy Material Layer Pack
```

Номера могут сдвинуться, но принцип должен остаться: сначала измерения, потом адаптация качества.

## Уровни качества материалов

Материалы должны иметь несколько уровней качества.

```text
Tier 0: только базовый цвет, очень дешёвый свет.
Tier 1: базовый цвет + простой diffuse-свет.
Tier 2: normal map + roughness approximation.
Tier 3: PBR-like свет + отражения окружения.
Tier 4: hero material: лак, reflection, detail normal, маски, дорогие слои.
```

Выбор уровня должен зависеть от:

- положения на экране;
- центра камеры;
- расстояния;
- важности объекта;
- типа материала;
- скорости движения камеры;
- риска силуэта/края;
- бюджета кадра;
- наведения/цели игрока;
- будущей карты внимания.

Критическое правило: дешёвый и дорогой уровни должны быть похожи по яркости и базовому виду. Иначе переход будет заметен.

## Плавные переходы качества

Нельзя мгновенно переключать материал с дешёвого на дорогой.

Пример идеи:

```text
finalColor = mix(cheapColor, fullColor, qualityBlend)
```

Правила:

- сила detail normal меняется плавно;
- specular/metallic/reflection меняются плавно;
- transition обычно 200-800 мс;
- нужен cooldown/hysteresis, чтобы качество не мигало туда-сюда;
- в центре экрана качество нельзя резко снижать;
- когда камера остановилась, качество повышается постепенно.

## Importance Map

В будущем SOLUM должен иметь низкоразрешённую карту важности экрана.

Пример размера:

```text
128x72
160x90
256x144
```

Источники важности:

- центр экрана;
- object/material ID;
- hero object flag;
- расстояние;
- разрыв глубины;
- разрыв нормалей;
- контраст/яркость;
- движение;
- цель/прицел игрока;
- позже ML/attention signal из SolumDraw-like модели.

Пример смешивания:

```text
finalImportance = oldImportance * 0.6 + fastImportance * 0.3 + mlImportance * 0.1
```

ML не должен напрямую включать/выключать фичи рендера. ML только говорит, где важная область. Рендер сам решает качество.

## Temporal Refinement

Когда камера быстро двигается:

- снижать material tier;
- снижать качество отражений;
- уменьшать плотность частиц;
- не обновлять дорогие эффекты;
- скрывать упрощения движением, пылью, ветром, магией.

Когда камера замедляется или стоит:

- постепенно повышать material tier;
- обновлять reflection probes;
- позже уточнять тени/AO/detail normal;
- улучшать статичные области за несколько кадров.

## Visual Masking

Использовать эффекты, чтобы скрыть подгрузку/переключение качества:

- пыль при приземлении;
- ветер при беге/полёте;
- motion blur при резком повороте;
- camera shake от удара;
- магическая вспышка;
- дым/туман при входе в область;
- анимация UI;
- дверь/проход/карабканье/пролезание.

Пока глаз отвлечён, можно обновить LOD, reflection, decals, cache.

## Stable Frame Mode

SOLUM должен измерять не только FPS.

Нужно отслеживать:

- CPU frame ms;
- GPU frame ms, если доступно;
- present time;
- 1% low / 99th percentile frame time;
- jank count;
- min/max frame time;
- thermal state, если доступно.

Будущий режим:

```text
Target: 60 / 45 / 30 FPS
Headroom: 15-25% GPU запаса
Если frame time растёт: снижать качество плавно
Если всё стабильно несколько секунд: повышать качество плавно
Всегда писать profiling report/debug ZIP
```

## Rendering Vacuum Test

Контролируемый тестовый режим:

- нет runtime allocations;
- нет загрузки файлов в кадре;
- нет создания shader/pipeline в кадре;
- нет лишних логов;
- повторяемая камера;
- стабильная сцена;
- diagnostics пишутся после теста, не каждый кадр.

## Reflection Tiers

Отражения должны иметь уровни качества:

```text
Tier 0: нет отражения.
Tier 1: цвет неба/окружения.
Tier 2: probe/fake cubemap.
Tier 3: probe + Fresnel + roughness blur + highlights.
Tier 4: half-res SSR + probe fallback.
Tier 5: low-res planar reflection только для hero surface.
```

Примеры:

- старое деревенское стекло: Tier 1-2;
- fantasy glass: Tier 2-3;
- noble glass: Tier 3-4;
- magical mirror / polished hero floor: Tier 5 только если важно.

## Fantasy Material Layer Pack

Будущие дешёвые слои материалов:

- Fresnel;
- dirt/dust;
- scratches;
- roughness variation;
- edge wear;
- wetness;
- fake reflection probe;
- detail normal;
- emissive magic;
- distance/importance quality fade.

Идея packed mask:

```text
R = dirt / dust
G = scratches
B = roughness variation
A = thickness / opacity / wetness, зависит от материала
```

## Связь с волосами, травой, деревьями

Perception renderer должен помочь будущему миру:

- волосы через hair cards, не full strand physics;
- трава через clusters/cards, не физика каждой травинки;
- деревья через trunk mesh + branch/leaf cards;
- wind/impact/magic fields двигают vertex shader;
- near/mid/far vegetation используют разные уровни качества.

## Будущие эксперименты

Только после profiling foundation:

1. Adaptive Material Budget prototype.
2. Importance Map v1 без ML.
3. Temporal Refinement + Visual Masking.
4. Adaptive Reflection Tiers.
5. Fantasy Material Layer Pack.
6. Impact Field System v1.
7. Cluster/Meshlet/Surfel visibility experiment.
8. Visibility Buffer Lite experiment.

## Чего избегать рано

Не делать рано:

- full ray tracing;
- full strand hair physics;
- full real-time terrain boolean destruction;
- full voxel world replacement;
- 4K internal gameplay rendering;
- per-point visibility для миллионов точек;
- perfect mirror/refraction everywhere;
- ML every frame;
- sudden quality switching;
- runtime shader/pipeline creation during gameplay;
- heavy debug branches in fragment shader.

## Коротко

SOLUM должен стать адаптивным мобильным рендерером: качество материалов, отражений, эффектов, разрушений и детализации должно зависеть от важности, движения, видимости и бюджета кадра.
