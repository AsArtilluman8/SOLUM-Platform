# SOLUM Environment Asset Pipeline

Status: P52 pipeline foundation.

P51 created the `EnvironmentApi`, time-of-day model, sun/moon/stars intent, IBL/skybox slots, and fallback diagnostics. P52 adds the asset manifest, directory convention, validation tools, and safe runtime loader slots. P52 does not add weather, volumetric clouds, Bruneton/Hosek runtime atmosphere, or a procedural sky shader.

## Asset Sources

Preferred sources:

- Poly Haven CC0 HDRIs.
- OpenHDRI CC0 HDRIs.
- NASA SVS public-domain star maps.
- ambientCG CC0 only when the selected asset license is explicit.
- Filament sample environment assets only if source and license are clear.

Do not use mixed-license or store-only packs. Do not commit large raw `.hdr` or `.exr` files. The APK target for all bundled environment assets is under 10 MB.

## Directory Convention

Source manifest:

```text
assets/env/ENVIRONMENT_ASSETS_MANIFEST.json
```

APK assets:

```text
apps/engine/src/main/assets/env/
```

Planned runtime paths:

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

## Conversion

Use a Filament `cmgen` version that matches the Android Filament runtime. Current runtime dependency is declared in `apps/engine/build.gradle`; P52 was built against Filament `1.71.4`.

Example shape:

```bash
cmgen --format=ktx --size=256 --extract-blur=0.1 --deploy=apps/engine/src/main/assets/env source.hdr
```

If `toktx` is used for star textures, keep the texture small and mobile-friendly:

```bash
toktx --t2 --genmipmap apps/engine/src/main/assets/env/stars_milkyway.ktx source_star_map.png
```

Do not run download/conversion during normal build. Use:

```bash
python3 tools/env_asset_manifest_check.py
```

## Runtime Integration

When an environment preset is selected, the Activity maps the P51 preset to a P52 slot and checks for matching KTX assets in Android assets. If the KTX exists, it reuses the existing Filament `KTX1Loader` path. If the KTX is missing or load fails, the app keeps the current P51 fallback.

Diagnostics must report:

- `activeEnvironmentPreset`
- `activeIblAssetStatus`
- `activeSkyboxAssetStatus`
- `activeStarsAssetStatus`
- `fallbackActive`
- `lastAssetLoadError`
- `assetLicenseStatus`
- `totalEnvAssetSizeEstimate`

P52B should add a tiny verified KTX bundle after `cmgen`/`toktx` availability and license provenance are proven.
