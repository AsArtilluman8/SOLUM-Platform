# SOLUM Codex Rules

## Project direction

SOLUM is now Filament-first.

Primary renderer:
- Filament / gltfio / Filament Android APIs.

Deprecated renderer:
- Old custom Android Native Vulkan GLB/PBR/material/glass viewer.
- Do not restore it as normal user path.
- Do not keep Back to Vulkan.
- Do not continue old Vulkan material/glass route work.

## Development rules

1. Do not guess product direction.
2. Do not create temporary throwaway systems.
3. Do not hardcode model names, material names, or test asset names.
4. Do not write model-specific hacks.
5. Do not fake unsupported features.
6. If an API is unavailable or not exposed, show honest status:
   - supported=false
   - not_exposed
   - deferred
   - fallback
7. Every patch must be useful for final SOLUM Engine.
8. Keep mobile-first constraints:
   - Android 13
   - Termux build
   - Mali-G57 class GPU
   - avoid huge assets
   - avoid heavy default settings
9. Default settings must be safe:
   - no overexposure
   - no noisy AO by default
   - no heavy shadows by default
   - no expensive SSR/TAA by default unless stable
10. UI must stay compact and usable.
11. Large patches must include diagnostics/status fields.
12. Build must succeed.
13. APK must be copied to:
   /storage/emulated/0/Download/SOLUM_APK/SOLUM_ENGINE_LATEST.apk

## Filament-specific rules

Use Filament APIs where possible:
- View options
- Renderer options
- LightManager
- RenderableManager
- TransformManager
- MaterialInstance
- ColorGrading
- FogOptions
- BloomOptions
- AmbientOcclusionOptions
- DynamicResolutionOptions
- TemporalAntiAliasingOptions if exposed
- ScreenSpaceReflectionsOptions if exposed
- Picking if exposed

Do not reimplement a renderer when Filament already provides the feature.

## Legacy Vulkan rule

Old Vulkan renderer should be removed from active app flow.
If full physical deletion breaks Gradle/native build, isolate it under legacy/deprecated path and document exactly what remains and why.
