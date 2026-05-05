# RESEARCH_GATE_RULES — обязательная сверка с проверенными repo/docs

Перед сложной системой агент не имеет права сразу писать патч из головы.

## Где Research Gate обязателен

- Vulkan foundation
- render architecture
- shadows / CSM
- lighting / sky / atmosphere
- PBR / toon materials
- IBL / reflections
- water
- terrain / world generation
- vegetation / foliage / grass
- VFX / particles
- animation / skeletal / IK
- asset pipeline / glTF / textures
- ECS / mechanics
- diagnostics / profiling / performance
- сложный mobile editor UX

## Алгоритм

```text
пользователь просит сложный патч
↓
агент читает REPOSITORY_REFERENCE_CATALOG.md
↓
выбирает 2–5 relevant repo/docs
↓
изучает подходы
↓
пишет Research Summary
↓
даёт варианты реализации
↓
пользователь выбирает путь
↓
только потом агент пишет patch plan/code
```

## Research Summary должен содержать

1. Problem
2. Current SOLUM state
3. References checked
4. What each reference teaches
5. Options
6. Recommended choice
7. Risks
8. SOLUM adaptation
9. What not to copy
10. Diagnostics/test plan
11. User decision required

## Статусы вариантов

```text
REFERENCE_ONLY  — изучаем принцип, код не берём
SMALL_SLICE     — берём маленький фрагмент/алгоритм с адаптацией
ADAPTER         — пишем адаптер вокруг идеи/API
DEPENDENCY      — добавляем библиотеку после license/build check
REJECT          — не подходит, записать почему
```

## Dependency safety

Если вариант требует external dependency, агент обязан проверить:

- license;
- Android/Termux build feasibility;
- size/memory impact;
- maintenance risk;
- architecture conflict.

См. `docs/DEPENDENCY_AND_LICENSE_POLICY.md`.

## Когда gate можно пропустить

- typo fix;
- one-line build hotfix;
- docs-only clarification;
- mechanical rename;
- simple report formatting fix.

## Обязательный блок в ответе

```text
Research Gate
References checked:
1. ...
2. ...
3. ...

Options:
A) ...
B) ...
C) ...

Recommended: ...
Need your choice: A/B/C
```

## Анти-правило

Запрещено:

```text
“Я реализую CSM сам, примерно знаю как”
```

Правильно:

```text
“Я сверил SaschaWillems shadowmappingcascade, diharaw CSM и ARM Mali guide. Для SOLUM выбираю 2-cascade stable CSM без geometry shader, с mobile PCF 2x2.”
```
