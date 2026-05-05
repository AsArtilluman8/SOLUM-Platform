# ARCHITECTURE_RULES — архитектурные правила SOLUM

## Главная цель

SOLUM Platform — Android/Vulkan AAA-like game-dev платформа на телефоне через Termux.

SOLUM Engine — главный движок и runtime.

Будущие приложения вокруг Engine:

- Launcher
- Asset Hub
- AniStudio / Cutscene Studio
- Character Studio
- Motion Studio
- Material Studio
- VFX Studio
- Sound Studio
- World Studio
- UI Studio
- AI / Behavior Studio
- Quest / Dialogue Studio
- Mechanics Studio

Финал: launcher-managed multi-APK ecosystem.

Старт: узкий foundation, не 15 APK сразу.

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

## RULE-ARCH-001: MVP must be incomplete, not incorrect

MVP может быть неполным, но не должен быть неправильным.

Разрешено:

- ShadowSystem v1 с одной real shadow map, рассчитанный на future CSM.
- MaterialDocument v1 с 3–4 параметрами, рассчитанный на future graph.
- CloudSystem v1 с 2 параметрами, рассчитанный на расширение.
- AssetManifest v1 с минимальными полями, рассчитанный на migration.
- Vulkan Foundation v1 с triangle, рассчитанный на renderer expansion.

Запрещено:

- blob shadow вместо shadow system;
- OpenGL вместо Vulkan production path;
- low-poly/fake visual target вместо финального качества;
- кнопки `+X -X` вместо transform/gizmo tool;
- временный формат ассетов, который потом надо выбросить;
- MainActivity/code dump вместо модульной архитектуры.

## RULE-ARCH-002: No throwaway production systems

Запрещены throwaway/fake системы в production path.

Упрощение разрешено только если:

1. Использует финальную границу модуля.
2. Использует финальную data model или расширяемое подмножество.
3. Может расширяться без удаления системы.
4. Не загрязняет runtime/UI.
5. Не скрывает финальные требования качества.
6. Не создаёт ложный статус “готово”.

Debug/prototype эксперименты разрешены только если они:

- лежат в `debug_experiments/` или `prototype_scripts/`;
- отключены build flag;
- не shipped;
- не считаются feature complete.

## RULE-ARCH-003: Final target lock

Если финальная цель заявлена как Vulkan / AAA-like / mobile real-time renderer, патч не должен менять направление.

Нельзя чинить баг так, чтобы уйти от финала.

Плохой пример:

```text
Проблема: тени мерцают.
Фикс: выключить тени.
```

Это не fix, а debug workaround.

Правильный подход:

```text
Проблема: тени мерцают.
Фикс: найти причину в shadow stabilization / bias / cascade snapping / PCF / matrix jitter.
```

## RULE-ARCH-004: One patch = one vertical system layer

Патч должен закрывать цельную систему или вертикальный слой.

Хорошо:

- Diagnostics v1
- Asset Schema v1
- Vulkan Foundation v1
- Asset Hub v1
- Material Studio v1
- CSM ShadowSystem v1
- Transform Tool v1

Плохо:

- добавить одну кнопку;
- добавить один slider;
- добавить один debug label;
- 30 микропатчей ради одной функции.

Микропатчи допустимы только для build/compile/hotfix после диагностики.

## RULE-ARCH-005: Use proven repositories as references, not blind replacement

SOLUM не должен выдумывать сложные renderer/material/shadow/mechanics системы из фантазии GPT.

Перед сложной системой нужно изучать проверенные repo/docs:

- The Forge — renderer abstraction, resource lifetime, descriptor management, render graph concepts.
- Filament — PBR/material model, mobile renderer ideas.
- bgfx — rendering abstraction patterns.
- Khronos Vulkan Samples — Vulkan patterns.
- Android NDK Vulkan samples — Android-specific Vulkan foundation.
- tinygltf — glTF import.
- meshoptimizer — mesh optimization.
- KTX-Software / BasisU — KTX2/texture pipeline.
- ozz-animation — skeletal animation.
- EnTT — lightweight ECS.
- Arm/Mali guides — mobile GPU constraints.

Правильно:

```text
изучить → выделить принцип → записать решение → адаптировать маленький slice под SOLUM
```

Неправильно:

```text
слепо импортировать огромный framework → сломать Termux build → потерять SOLUM architecture
```

## RULE-ARCH-006: Module boundaries

Стартовая структура:

```text
core/
  solum-asset-core/
  solum-diagnostics-core/
  solum-ui-core/

engine-core/
  solum-vulkan-core/
  solum-render-core/
  solum-scene-core/

apps/
  engine/
  launcher/
  asset-hub/
  material-studio/
  anistudio/

tools/
```

Важно:

- `core/` лёгкий и общий.
- `engine-core/` тяжёлый, связан с Vulkan/render/scene.
- Launcher и Asset Hub не должны зависеть от Vulkan без причины.
- Shared core не должен стать god-module.

## RULE-ARCH-007: Start narrow, expand only after evidence

Финальная экосистема остаётся целью, но старт не должен строить все приложения.

Порядок:

1. Repo/docs foundation.
2. Diagnostics.
3. Asset schema.
4. Vulkan capability dump.
5. Vulkan triangle.
6. Asset Hub.
7. Material Studio.
8. Launcher.
9. Следующие tools по одному.

## RULE-ARCH-008: Compatibility and versioning

Каждый формат должен иметь:

- `schema`
- `schemaVersion`
- compatibility info
- validator
- migration plan later

Нельзя писать “просто json как получится”.

## RULE-ARCH-009: Transaction-safe writes

Нельзя напрямую перезаписывать asset/project file.

Сохранение должно идти так:

```text
serialize to temp
↓
validate temp
↓
backup current
↓
atomic replace
↓
write save_report.json
```

## RULE-ARCH-010: Android keystore is project-critical

APK signing key нельзя терять.

До первого APK нужна стратегия:

- где создать keystore;
- где хранить;
- как backup;
- один keystore для SOLUM apps или отдельные;
- что будет при потере.

См. `docs/decisions/ADR_0004_android_keystore_strategy.md`.
