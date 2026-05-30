# P44 Filament API Expansion + Legacy Vulkan Removal

## Branch

patch/P44-filament-api-expansion-legacy-removal

## Base

main

## Goal

Make one large useful patch:

1. Remove old custom Vulkan renderer from active project/app flow.
2. Expand Filament Render Control Center with most available high-impact Filament API features.
3. Add honest status for unsupported/not exposed features.
4. Keep Filament as the only normal renderer.
5. Keep build stable.
6. Preserve mobile-safe defaults.

This patch should replace several small UI/API patches.

## Context

Current renderer direction:
- Filament is primary.
- Old Vulkan GLB/PBR/material/glass renderer is deprecated.
- P40-P43 added Import Model, Import IBL, Render Control Center, config, camera/model controls, AO, Bloom, Shadows, Refraction, MSAA/FXAA, dynamic resolution, skybox, IBL rotation, light presets.

Current issue:
- Filament still does not expose all useful API controls in UI.
- Old Vulkan renderer still exists and pollutes the repo/app architecture.
- We need to use ready Filament APIs instead of custom renderer work.

## Hard rules

1. Do not restore old Vulkan renderer.
2. Do not keep Back to Vulkan.
3. Do not keep old Vulkan material/glass/debug UI in normal user flow.
4. Do not hardcode test model names.
5. Do not fake features.
6. If a Filament API is not available from Java/Kotlin, show not_exposed.
7. If a feature is too expensive, keep it Off by default but expose it under High/Ultra/Advanced.
8. No VFX/water/world physics in this patch.
9. No model-specific hacks.
10. Build must pass.

## Part A — Legacy Vulkan removal / quarantine

Remove old Vulkan renderer from active flow.

Required:
- Launcher must stay Filament.
- Main normal app flow must open Filament.
- Remove or disable old Vulkan viewer path.
- Remove old normal UI references to:
  - old Vulkan render UI
  - old Glass Route Test
  - old Material Lab Test
  - old glass sliders
  - old Vulkan debug buttons
  - Back to Vulkan
- If MainActivity cannot be deleted because build/import/storage depends on it, convert it to explicit legacy/deprecated hidden code path.
- Add status/docs:
  Legacy Vulkan = removed_from_normal_flow / deprecated / build_required_only if anything remains.
- Do not break Gradle/native build.

Expected:
- User sees only Filament renderer in normal launch.
- No old Vulkan renderer button.

## Part B — Filament API surface audit inside code

Inspect available classes in current dependency:
- com.google.android.filament.View
- Renderer
- ColorGrading
- LightManager
- RenderableManager
- TransformManager
- MaterialInstance
- Skybox
- IndirectLight
- ModelViewer
- Manipulator

For every high-impact feature:
- implement if safely exposed;
- otherwise show not_exposed in Debug.

## Part C — Render / Quality controls

Add or improve UI/status for:

- FXAA On/Off
- MSAA samples 1x / 2x / 4x
- Dynamic Resolution On/Off
- Dynamic Resolution min scale / max scale if exposed
- Render scale
- Dithering On/Off
- TAA On/Off if exposed
- TAA quality/feedback/filterWidth if exposed
- SSR On/Off if exposed
- SSR quality/options if exposed
- Guard Band if exposed
- Render quality LOW/MEDIUM/HIGH/ULTRA
- actual applied status for each field

Defaults:
- Medium quality.
- FXAA On if stable.
- MSAA 2x if stable.
- Dynamic Resolution On if stable.
- TAA Off by default even if exposed.
- SSR Off by default even if exposed.
- Dithering On if stable.

Debug fields:
- fxaaSupported
- fxaaEnabled
- msaaSupported
- msaaSamples
- dynamicResolutionSupported
- dynamicResolutionEnabled
- dynamicMinScale
- dynamicMaxScale
- taaSupported
- taaEnabled
- ssrSupported
- ssrEnabled
- ditheringStatus
- renderScale
- qualityProfile

## Part D — Color grading / tone mapping

Add Filament ColorGrading controls if exposed.

UI:
- Color Mode:
  - Neutral
  - PBR Neutral
  - Filmic
  - Cinematic
  - Product
  - Character
  - Night
- Tone mapper selector if exposed.
- Exposure remains in Lighting.
- Contrast if exposed.
- Saturation if exposed.
- Temperature/warm-cool if exposed.
- Highlight protection / white point if exposed.
- Reset Color Grading.

Defaults:
- Neutral/PBR-safe.
- No overbright cinematic default.
- No aggressive saturation.

Debug:
- colorGradingSupported
- toneMapper
- contrast
- saturation
- temperature
- colorGradingStatus

If ColorGrading API is limited, show partial/not_exposed.

## Part E — Fog / Atmosphere basic

Add basic Filament FogOptions if exposed.

UI:
- Fog Off/On
- Fog preset:
  - Off
  - Soft Depth
  - Forest Haze
  - Night Mist
  - Cinematic Low
- Fog density
- Fog distance
- Fog height
- Fog color if easy
- Fog from IBL if exposed

Defaults:
- Fog Off.

Debug:
- fogSupported
- fogEnabled
- fogDensity
- fogDistance
- fogHeight
- fogStatus

## Part F — Bloom polish

Improve Bloom controls.

UI:
- Bloom Off / Low / Medium / High
- Bloom strength if exposed
- Bloom threshold if exposed
- Bloom radius if exposed
- Bloom quality if exposed

Defaults:
- Bloom Off.
- No overexposure.

Debug:
- bloomSupported
- bloomEnabled
- bloomMode
- bloomStrength
- bloomThreshold
- bloomStatus

## Part G — AO polish

Improve AO controls.

UI:
- AO Off / Soft / Medium / Strong / Debug
- AO strength
- AO radius
- AO quality
- AO power/bias/bilateral threshold if exposed

Defaults:
- AO Off or Soft.
- Avoid noisy default.

Debug:
- aoSupported
- aoEnabled
- aoMode
- aoStrength
- aoRadius
- aoQuality
- aoStatus
- aoNoiseWarning for Strong/Debug.

## Part H — Shadows polish

Improve shadow controls using Filament APIs only.

UI:
- Shadows Off / Soft Low / Medium / Sharp Inspect
- Shadow type if exposed
- VSM/DPCF options if exposed
- Soft shadow options if exposed
- Shadow map/status if exposed
- Cast shadows per renderable if accessible
- Receive shadows per renderable if accessible
- Contact shadows if exposed
- Bias/normal bias if exposed

Defaults:
- Shadows Off or Soft Low only.
- No harsh black default.

Debug:
- shadowsSupported
- shadowsEnabled
- shadowMode
- shadowType
- vsmOptionsStatus
- softShadowOptionsStatus
- castReceiveStatus
- contactShadowsStatus
- shadowBiasStatus
- csmStatus

## Part I — Lights expansion

Add initial light list/controls if safe.

UI:
- Sun light controls already exist.
- Add Point Light preset if LightManager supports it.
- Add Spot Light preset if LightManager supports it.
- Add simple Light Rig:
  - Studio Key
  - Rim Light
  - Product Light
  - Night Lamp
  - Magic Preview Light
- Controls:
  - light enabled
  - intensity
  - color/temperature if simple
  - position/direction if simple
  - falloff for point/spot if exposed
  - spot cone if exposed

Defaults:
- Additional lights Off.
- No FPS-heavy default.

Debug:
- lightCount
- pointLightSupported
- spotLightSupported
- activeLightRig
- additionalLightsEnabled

## Part J — Picking / object-material selection

If View.pick is exposed and practical:

Implement:
- tap on model selects renderable/object if possible.
- show selected entity/renderable ID.
- show selected material slot if possible.
- highlight or status only; no complex gizmo.

If not practical:
- show pickingStatus=not_exposed/deferred.

Debug:
- pickingSupported
- selectedRenderable
- selectedMaterialIndex
- pickDepth
- pickStatus

## Part K — Material inspector expansion

Do not make full editor yet, but improve read-only inspector.

Show if accessible:
- material count
- selected material index
- material package/name
- alpha mode
- baseColor parameter if accessible
- metallic/roughness if accessible
- normal texture present
- AO texture present
- emissive texture present
- clearcoat/sheeen/transmission/ior/volume if accessible
- double sided/culling if accessible
- material API status

If not accessible:
- limited_by_gltfio_java_api.

No destructive overrides in this patch.

## Part L — Config upgrade

Update config schema.

Save/load:
- new render/quality/color/fog/bloom/AO/shadow/SSR/TAA/light settings.
- old config should load safely.
- missing fields use safe defaults.
- private backup + Download JSON export.
- visible save/load success/failure.

Config path:
- /storage/emulated/0/Download/SOLUM/config/filament_render_config.json

Debug:
- configVersion
- lastSaveStatus
- lastLoadStatus
- privateSaved
- downloadExportSaved
- lastConfigError

## Part M — UI requirements

Keep compact mobile UI.

Required:
- tabs remain usable.
- panel collapse/expand remains.
- every slider shows current value.
- every button shows current state.
- top HUD remains short.
- Debug can be verbose.
- Do not block viewport by default.

Tabs can be:
- Assets
- Render
- Color
- Lighting
- IBL
- Shadows
- Camera
- Model
- Material
- Debug
- Config

## Part N — Build

Run:

git diff --check

bash tools/build_native_engine.sh && ANDROID_HOME=/data/data/com.termux/files/home/android-sdk ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk gradle --no-daemon -p "$PWD" clean assembleDebug

mkdir -p /storage/emulated/0/Download/SOLUM_APK
cp apps/engine/build/outputs/apk/debug/engine-debug.apk /storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk

## Commit

Commit message:
P44 expand Filament API controls and remove legacy Vulkan renderer path

Push:
origin patch/P44-filament-api-expansion-legacy-removal

## Final report must include

- changed files
- legacy Vulkan removal/quarantine status
- Filament features implemented
- features not_exposed
- default values
- config schema update
- build result
- APK path
- branch
- commit SHA
- known limitations

## Acceptance checklist

1. Build succeeds.
2. APK copied.
3. Filament remains default launcher.
4. No old Vulkan normal UI path.
5. No Back to Vulkan.
6. Import Model works.
7. Import IBL works.
8. Config save/load works.
9. Quality controls show actual state.
10. TAA/SSR either work or show not_exposed.
11. Color grading works or shows partial/not_exposed.
12. Fog works or shows not_exposed.
13. AO still works and safe default is not noisy.
14. Shadows still work or show honest not_exposed.
15. Bloom default does not overexpose.
16. Light rig controls do not tank FPS by default.
17. Material inspector still shows useful read-only info.
18. UI remains compact/collapsible.
19. No model-specific hacks.
20. No fake features.
