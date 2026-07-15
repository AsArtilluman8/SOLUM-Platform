# NOTE 0056 — P63.2B Celestial Polish

## Decision

P63.2B расширяет существующий Filament stage без нового renderer и без screen-space weather overlays.

## Existing references used

- `moon_phase_png` — normal/light dot на сфере для закруглённого терминатора; увеличено число фаз и добавлены limb shading/earthshine.
- `SolumStarCatalog` и `star_group` — deterministic world-space field; runtime использует density groups, size variants и dawn fade.
- `cloud_cluster` — геометрические layered clouds без квадратных sprites.
- Filament `View.BloomOptions` — threshold bloom для emissive Sun core; старый tone mapping и IBL не заменяются.
- UDS audit — только controller/layer/preset concepts; private UDS source/assets не копируются в public path.

## Rejected

- чёрный moon occluder;
- fullscreen star/cloud overlay;
- volumetric raymarch clouds на этом этапе;
- новый dependency или shader/runtime fork;
- unverified normal-map binding для Moon.

## Mobile guard

- максимум восемь активных star groups из пяти mutually-exclusive size variants;
- двенадцать cloud groups;
- material/twinkle updates throttled текущим adapter interval;
- Filament bloom ограничен четырьмя уровнями и 256 resolution;
- rain/snow/lightning/audio logic не меняется.

## Verification boundary

Static/core/generator/Android build tests и APK build доказывают интеграцию. Финальный внешний вид, alpha sorting и FPS требуют ручного запуска APK на устройстве.
