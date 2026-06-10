# SOLUM Environment Assets

Status: P53 pipeline/starter manifest.

P52 ships no external HDRI, EXR, star map, or large raw environment asset. It adds the manifest, paths, validation, and runtime loader slots so real KTX assets can be added later without changing the P51 Environment API.

P53 attempts the real starter environment pack, but this local toolchain does not expose `cmgen` or `toktx`. P53 therefore does not bundle fake KTX files. The app keeps honest fallback and reports `conversion_tool_unavailable` / `missing_asset_fallback`.

## Manifest

- Source manifest: `assets/env/ENVIRONMENT_ASSETS_MANIFEST.json`
- Bundled app manifest copy: `apps/engine/src/main/assets/env/ENVIRONMENT_ASSETS_MANIFEST.json`
- APK asset convention: `apps/engine/src/main/assets/env/`

Planned files:

```text
env/day_ibl.ktx
env/day_skybox.ktx
env/sunset_ibl.ktx
env/sunset_skybox.ktx
env/night_ibl.ktx
env/night_skybox.ktx
env/stars_milkyway.ktx
env/studio_debug_ibl.ktx
env/studio_debug_skybox.ktx
```

## Source Policy

Allowed:

- Poly Haven HDRIs with CC0 source metadata.
- OpenHDRI CC0 HDRIs when the selected asset page clearly says CC0.
- NASA SVS star maps when the page is public domain or NASA media usage allows reuse.
- Filament sample environment assets only when source and license are clear.

Rejected:

- Unity Asset Store packs.
- Sketchfab mixed-license HDRIs.
- HDRI-Hub royalty-free samples without explicit public repo and commercial compatibility.
- 4K/8K raw HDRI/EXR files in the APK.

## P53 Starter Status

No asset is bundled in P53 because `cmgen`/`toktx` are not available in this environment and no existing safe Filament sample KTX is present in the repo. The app keeps P51/P52 fallback active and reports `missing_asset_fallback` instead of crashing.

Total bundled environment asset estimate: `0` bytes.

Bundled asset list: none.

Safe source candidates remain:

- Poly Haven CC0 HDRIs for day/sunset/cloudy.
- OpenHDRI CC0 or Poly Haven CC0 for night.
- NASA SVS public-domain star map only if small and clearly documented.

Not bundled:

- raw `.hdr`;
- raw `.exr`;
- unverified royalty-free samples;
- true volumetric cloud assets.

P54 next can add dedicated cloud shadows and rain/snow VFX/performance after the P53 foundation is verified on phone.
