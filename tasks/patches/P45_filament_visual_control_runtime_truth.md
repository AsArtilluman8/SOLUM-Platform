# P45 — Filament Visual Control + Runtime Truth Mega Pack

## Goal

Turn the current Filament preview into a stronger visual/render control foundation before any physics, VFX, water, terrain reaction, procedural assets, NPC, economy, or gameplay systems.

This patch is about the existing Filament visual block:
- truthful runtime diagnostics;
- working visual controls;
- post-process/color/fog/bloom;
- sun glare / lens flare if safe;
- lighting and shadow controls;
- quality presets;
- config truth;
- debug capability status.

## Current known state

Main file:

- `apps/engine/src/main/java/com/solum/engine/FilamentGlbPreviewActivity.java`

The current code already has foundations for:

- Filament as primary renderer.
- GLB/glTF preview.
- HDR/IBL loading path.
- Render Control Center.
- Tabs: Render, Color, Fog, Lighting, Lights, IBL, Shadows, Camera, Model, Material, Config, Debug.
- Modes/fields: ColorMode, FogMode, LightRig, AO, Bloom, Shadows, Refraction, TAA, SSR, Dithering, sun direction, fog density/distance/height, color contrast/saturation/temperature.
- Legacy Vulkan removed from normal app flow, but some old native code may still remain. Do not spend this patch on full Vulkan purge unless it directly blocks build.

## Hard rules

1. Filament remains the primary renderer.
2. Do not reintroduce Back to Vulkan or old Vulkan normal-flow UI.
3. SSR must remain manual-only.
4. SSR must not be enabled by Low / Medium / High / Ultra Preview.
5. SSR must show warning/status when enabled manually.
6. Do not fake support. If a Filament Java API is not exposed, show `not_exposed` or `deferred`.
7. Keep mobile/Mali-friendly defaults.
8. Prefer one strong coherent patch over small partial patches.
9. Build must pass.
10. Commit and push branch: `patch/P45-filament-visual-control-runtime-truth`.

## Scope A — Runtime truth HUD

Improve the current FPS/render HUD.

Add or verify:

- current FPS;
- frame ms;
- average frame ms;
- min frame ms;
- max frame ms;
- p95 frame ms if practical;
- worst frame ms;
- jank counter;
- slow frame counter;
- frame budget status:
  - 60 FPS budget = 16.6 ms;
  - 45 FPS budget = 22.2 ms;
  - 30 FPS budget = 33.3 ms;
- target/preset FPS vs actual measured smoothness;
- render CPU timing if available;
- reset counters button in Debug or Quality tab.

Use simple rolling windows. Do not overengineer.

## Scope B — Post-process / Color

The project already has ColorMode and Color tab foundations. Make them real and useful.

Verify/apply:

- tone mapping preset;
- exposure;
- contrast;
- saturation;
- color temperature / warm-cool feel if possible;
- neutral / product / cinematic / character / night presets;
- actual status text:
  - applied;
  - failed;
  - not_exposed;
  - deferred.

If the current ColorGrading API does not support a setting directly, do not fake it. Display truthful status.

## Scope C — Fog / Haze

The project already has FogMode, fogDensity, fogDistance, fogHeight, Fog tab foundations.

Make fog/haze controls actually affect the Filament View if available.

Add/verify:

- Fog Off;
- Soft Depth;
- Forest Haze;
- Night Mist;
- Cinematic Low;
- density;
- distance/falloff;
- height if exposed;
- visible actual status.

If Filament Java API does not expose the needed option in current dependency, show not_exposed/deferred clearly.

## Scope D — Bloom / Sun glare / Lens flare

Improve visual post-process without heavy screen-space effects.

Bloom:

- Off by default on mobile-safe presets;
- allow Soft / Medium / High manual modes;
- show actual applied status;
- avoid overly bright default values.

Sun glare / lens flare:

- Add cheap mobile-friendly fake sun glare / lens flare overlay if feasible.
- It can be simple: screen-space sprite/gradient overlay based on sun direction and camera view.
- It must be optional and off or subtle by default.
- It must not require heavy SSR/volumetric rendering.
- It should have a status string and be disabled in Low if needed.

If too risky for this patch, add clear TODO/status and do not break build.

## Scope E — Lighting and shadows

Improve existing lighting/shadow controls.

Verify/apply:

- sun intensity;
- sun azimuth/elevation;
- fill light;
- point light / spot light rig if existing code supports it;
- shadow on/off;
- shadow type/quality if exposed;
- shadow softness if exposed;
- shadow bias if exposed;
- shadow distance or map size if exposed;
- clear status for CSM/cascade controls:
  - supported;
  - not_exposed_by_current_java_api;
  - deferred.

Do not pretend cascade controls work if they do not.

## Scope F — Quality presets

Make quality presets truthful and mobile-safe.

Profiles:

- Low;
- Medium;
- High Preview;
- Ultra Preview;
- Screenshot / Experimental if practical.

Rules:

- Low/Medium/High/Ultra: SSR Off.
- Bloom Off or Soft only unless manually enabled.
- Dynamic resolution On by default.
- MSAA conservative.
- TAA only if clearly applied and not harmful.
- Dithering on if safe.
- Fog Off by default unless selected manually.

Each preset must update summary/status text so the user knows what changed.

## Scope G — Config save/load truth

Verify config status is honest.

Show:

- config schema version;
- private SharedPreferences saved/loaded;
- export config path;
- last save time;
- last load source;
- error text if failed;
- whether current settings differ from saved settings if practical.

Do not silently fail.

## Scope H — Debug supported/not exposed table

In Debug tab, show compact capability/status table for major features:

- Filament renderer active;
- GLB/glTF loaded;
- IBL loaded/fallback;
- ColorGrading;
- Fog;
- Bloom;
- AO;
- Shadows;
- Refraction;
- TAA;
- SSR;
- Dithering;
- Dynamic Resolution;
- MSAA;
- Light Rig;
- Material Inspector;
- Picking;
- CSM/cascades;
- old Vulkan normal flow removed.

Use statuses:

- supported;
- applied;
- off;
- failed;
- not_exposed;
- deferred;
- fallback.

## Out of scope

Do not implement:

- physics;
- VFX runtime;
- water system;
- craters;
- terrain deformation;
- procedural asset economy;
- NPC systems;
- inventory;
- economy;
- gameplay systems;
- full Vulkan purge unless required to compile.

## Build command

Run:

```bash
bash tools/build_native_engine.sh && ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle --no-daemon -p "$PWD" clean assembleDebug && mkdir -p /storage/emulated/0/Download/SOLUM_APK && cp apps/engine/build/outputs/apk/debug/engine-debug.apk /storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk
```

## Acceptance checklist

- App builds successfully.
- APK copied to `/storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk`.
- Filament preview still opens.
- No old Vulkan normal-flow button/path is restored.
- FPS/jank HUD is more truthful than before.
- Color/Fog/Bloom/Lighting/Shadows controls either work or clearly say not_exposed/deferred.
- SSR remains off in all normal presets including Ultra Preview.
- Debug tab clearly shows supported/not_exposed/deferred feature statuses.
- Commit pushed to branch `patch/P45-filament-visual-control-runtime-truth`.

## Report format

After implementation, report:

- branch name;
- commit SHA;
- changed files;
- build result;
- APK path;
- what works;
- what is not_exposed/deferred;
- known limitations;
- what to test manually on device.
