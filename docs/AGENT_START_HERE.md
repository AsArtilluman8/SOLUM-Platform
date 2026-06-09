# Agent Start Here

This file preserves agent-oriented guidance. The public README is user-facing.

## Before proposing patches

Read:

1. README.md
2. ROADMAP.md
3. docs/PROJECT_STATUS.md
4. docs/GETTING_STARTED_ANDROID_TERMUX.md
5. docs/OPENAI_CODEX_FOR_OSS_APPLICATION.md
6. tasks/roadmap/ if present
7. tasks/patches/ if present

## Working rules

- Keep SOLUM mobile-first.
- Prefer large coherent patches over fake broad patches.
- Filament is the primary renderer direction.
- Do not reintroduce old Vulkan as normal renderer path.
- SSR is manual-only and must not be default, even in Ultra.
- Keep APK output in /storage/emulated/0/Download/SOLUM_APK/.
- Do not use /mnt/data paths in user-facing Termux commands.
