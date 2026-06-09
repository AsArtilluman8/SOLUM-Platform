# SOLUM Testing Guide

Status: public testing guide.

Testing should separate user-facing behavior from developer diagnostics.

## Required report fields

Every test report should include:

- branch;
- commit SHA;
- APK/build source;
- device tier if known;
- quality profile;
- loaded model/scene;
- FPS/HUD result;
- visible smoothness notes;
- controls tested;
- screenshots or short video when useful;
- crash logs if applicable.

Do not include private account data or secrets.

## Renderer smoke test

Check:

- app launches;
- default scene/model renders;
- camera moves;
- model import still works;
- Render/Color/Fog/Shadows/Debug panels open;
- no crash while switching profiles.

## Performance truth test

Check:

- main HUD does not use Java callback FPS as the primary FPS;
- Debug separates Java callback FPS from estimated visible FPS;
- FrameMetrics values appear when available;
- timing disagreement is reported if sources diverge strongly;
- expensive features are listed as causes when enabled.

## Control truth test

Check each visible control:

- UI interaction works;
- requested state changes;
- actual state changes or honestly reports requested-only/requires-recreate/not-exposed;
- label updates;
- Debug updates;
- config save/load does not restore stale expensive values.

## Quality profile test

Low Safe should be gameplay-safe and should not keep expensive debug settings enabled.

Medium Mobile should remain gameplay-oriented.

High Preview and Ultra/Screenshot may use more expensive features but must report risk and should not pretend to be guaranteed gameplay modes.

## Crash report template

```text
Branch:
Commit:
APK source:
Action before crash:
Panel/control:
Model/scene:
Log excerpt:
Expected:
Actual:
```
