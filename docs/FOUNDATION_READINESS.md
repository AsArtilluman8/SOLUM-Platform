# FOUNDATION_READINESS — Patch 01 continuation

Этот файл фиксирует безопасную проверку repo/build/tools foundation после agent foundation.

## Назначение

Patch 01 readiness не создаёт runtime и не меняет Vulkan path. Он проверяет, что новый агент может быстро понять состояние проекта и получить evidence перед следующим PR.

## Проверка

Основная команда остаётся единственной build/test командой агента:

```bash
bash tools/agent_build_runner.sh
```

Runner выполняет foundation preflight и пишет:

```text
_work/agent_reports/latest/SOLUM_FOUNDATION_READINESS.txt
_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt
_work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG.txt
```

## Что считается готовым

- обязательные memory docs существуют;
- `core/`, `engine-core/`, `apps/`, `tools/` существуют;
- agent workflow docs существуют;
- build/diagnostics/asset tools на месте;
- GitHub PR template на месте;
- runner честно сообщает build state: `BUILD_SUCCESS`, `BUILD_FAILED`, `NO_BUILD_SYSTEM_YET` или `NO_VALID_GRADLE_BUILD`.

## Что не входит

- Vulkan/runtime changes;
- полноценный Android app;
- Gradle redesign;
- roadmap reorder;
- asset schema changes.

## Следующий шаг

После этой проверки следующий безопасный слой выбирается по фактическому build/diagnostics evidence, а не по предположениям.
