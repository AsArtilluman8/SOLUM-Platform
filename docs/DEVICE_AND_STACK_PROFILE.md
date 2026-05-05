# DEVICE_AND_STACK_PROFILE — target device and stack

Этот файл фиксирует рабочую среду SOLUM. Значения нужно проверять diagnostics, потому что окружение может меняться.

## Target device

Primary development device:

```text
Phone: Tecno CAMON 19 Pro / Tecno CI8n
Android: Android 13 class
GPU: Mali-G57 MC2 class
CPU: MediaTek Helio G96 / mt6781 class
RAM: ~7–8 GB class
Development environment: Termux on phone
```

## Render target

```text
Primary renderer: Vulkan
Target Vulkan capability: Vulkan 1.1 class where available
Fallback production renderer: none unless explicit future ADR
```

OpenGL references from old docs are not current SOLUM Platform direction.

## Core stack

```text
C++          → Vulkan core, renderer, engine runtime
GLSL         → shaders, SPIR-V pipeline
Kotlin       → Android shell, lifecycle, permissions, native surface, UI shell where needed
Python       → diagnostics, reports, validators, offline tools, asset preprocessing
HTML/CSS/JS  → diagnostics report/dashboard
Markdown     → project memory and specifications
```

## Storage constraint rule

Phone storage is limited. Build and diagnostics must avoid uncontrolled output growth.

Rules:

- use `SOLUMCreative/latest` and `SOLUMCreative/archive`;
- do not write random APK/ZIP/TXT/PY files into Download root;
- diagnostics archive should be useful but not unbounded;
- large dependencies/assets require explicit reason.

## Performance direction

```text
60 FPS ideal
45 FPS acceptable mobile editor/preview target
30 FPS only fallback for heavy debug/preview modes
```

Runtime proof comes from diagnostics on actual device.

## Device-specific caution

Mali-G57 is tile-based and bandwidth-sensitive.

Before heavy render features, check:

- render pass load/store ops;
- texture bandwidth;
- shader ALU/texture cost;
- overdraw;
- diagnostics overhead;
- thermal throttling.

## Rule

This file describes expected device/stack. Actual values must come from:

```text
SOLUM_LATEST_DIAGNOSTICS.zip
vulkan_caps.json
env_info.json
device_info.json
```
