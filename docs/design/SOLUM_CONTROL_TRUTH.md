# SOLUM Control Truth

Status: design and audit requirement.

SOLUM controls must be truthful. A button, slider, or quality profile must not pretend that a renderer feature changed if the actual render state did not change.

## Core rule

```text
UI requested state != actual render state unless verified.
```

If a setting cannot be applied live, the UI must say so.

## Required states

Every control should be classified as one of:

- `works`
- `broken_ui`
- `broken_state`
- `broken_apply`
- `requested_only`
- `requires_recreate`
- `not_exposed`
- `not_verified`

## Runtime truth table

Each visible control should have this audit shape:

| Control | UI works | Requested changes | Actual applied | Visual expected | Requires recreate | Status | Next action |
|---|---|---|---|---|---|---|---|

## Requested vs actual

Diagnostics should distinguish:

```text
requestedMSAA
actualMSAA
msaaApplyStatus
requestedDynamicResolution
actualDynamicResolution
requestedTAA
actualTAA
requestedColorValue
actualColorValue
```

The same pattern should apply to Bloom, AO, SSR, shadows, fog, render scale, tone/color, and other user-facing controls.

## Quality profile truth

A quality profile is not just a label.

Low Safe must force safe settings.

Medium Mobile must force gameplay-oriented settings.

High Preview and Ultra/Screenshot may allow expensive settings, but must clearly report risk.

## Preset mismatch

If selected profile and actual state differ, diagnostics should show:

```text
Preset mismatch: true
```

Example:

```text
Selected profile: Low Safe
Expected Bloom: Off
Actual Bloom: High
Expected MSAA: 1x
Actual MSAA: 4x
Preset mismatch: true
```

## User-facing HUD

The main HUD should stay simple:

```text
FPS 48 | OK | Low | cause: none
FPS 29 | BAD | Medium | cause: Bloom, AO, MSAA
FPS 12 | BAD | SSR | cause: SSR GPU heavy
```

Developer/debug details belong in Debug, not the main user HUD.

## Why this matters

Without control truth, FPS targets and quality profiles are meaningless. SOLUM must first prove what is actually enabled before optimizing renderer quality.
