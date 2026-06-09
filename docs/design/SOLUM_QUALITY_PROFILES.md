# SOLUM Quality Profiles

Status: public render quality profile design.

Quality profiles define expected render state. A profile is not just a label. Selecting a profile must force a known set of requested settings and report whether actual runtime state matches those settings.

## Profile Rules

- Low Safe must never leave expensive debug modes enabled.
- Medium Mobile must be the default practical gameplay target.
- High Preview may improve visuals but must remain mobile-aware.
- Ultra Preview may use expensive features but should warn users.
- Screenshot/Experimental may prioritize image quality over FPS.
- Every profile must expose requested versus actual state in Debug.

## Low Safe

Purpose: reliable gameplay on baseline and mid-range mobile devices.

Expected state:

- SSR: Off
- TAA: Off by default unless proven stable
- FXAA: On or low-cost mode if available
- MSAA: 1x or 2x only
- Dynamic Resolution: On
- AO: Off or Soft, never Debug Max
- Bloom: Off or Low
- Shadows: Off, Low, or Soft
- Fog: Low/simple
- Refraction: Off by default
- Sun glare/lens effects: Off or Low
- Debug visualizations: Off
- Experimental features: Off

Low Safe must not look broken or excessively blurry. It should be cheap, readable, and stable.

## Medium Mobile

Purpose: main gameplay profile for mainstream devices.

Expected state:

- SSR: Off by default
- TAA: Off by default unless proven stable
- MSAA: 2x target
- Dynamic Resolution: On
- AO: Soft or Medium
- Bloom: Low or Medium
- Shadows: Soft or Medium
- Fog: Medium/simple
- Refraction: limited and explicit
- Debug visualizations: Off

Medium should be visually better than Low while remaining a practical gameplay profile.

## High Preview

Purpose: better visual preview and gameplay on stronger devices.

Expected state:

- SSR: Off by default unless explicitly enabled with warning
- TAA: Optional
- MSAA: 2x or 4x only if actual state can be verified
- Dynamic Resolution: On or adaptive
- AO: Medium
- Bloom: Medium
- Shadows: Medium/High depending on device
- Fog: Medium/High
- Refraction: Optional, with diagnostics
- Debug visualizations: Off

High is not a PC-only mode. It must remain mobile-aware.

## Ultra Preview

Purpose: high visual quality on flagship devices or for editor preview.

Expected state:

- expensive effects allowed with warnings;
- SSR may be enabled manually, not silently forced for gameplay;
- higher bloom/AO/shadow quality allowed;
- FPS target depends on device tier;
- not guaranteed on baseline or mid-range devices.

Ultra must be clearly labeled as preview/flagship-oriented.

## Screenshot / Experimental

Purpose: maximum quality or research mode.

Expected state:

- expensive features allowed;
- FPS is secondary;
- debug and screenshot-only effects may be enabled;
- UI must clearly mark this mode as not a gameplay target.

## Requested Versus Actual

Every profile should expose at least:

```text
Selected profile: Low Safe
Expected MSAA: 1x
Actual MSAA: 1x / requires_recreate / not_verified
Expected AO: Off
Actual AO: Off
Preset mismatch: false
```

If actual state does not match expected state, Debug should report:

```text
Preset mismatch: true
MSAA expected 1x, actual 4x
Bloom expected Off, actual High
```

Main HUD may show a short warning:

```text
preset_mismatch
```

## Control Truth

A control must not pretend to work. Every button/slider should be one of:

- works;
- requested_only;
- requires_recreate;
- not_exposed;
- not_verified;
- broken_ui;
- broken_apply.

This applies to quality profile controls, MSAA, dynamic resolution, TAA, SSR, AO, bloom, shadows, fog, color grading, render scale, and material/render controls.
