# SOLUM Environment Asset Pipeline

Status: P53C pipeline foundation.

P51 created the `EnvironmentApi`, time-of-day model, sun/moon/stars intent, IBL/skybox slots, and fallback diagnostics. P52 adds the asset manifest, directory convention, validation tools, and safe runtime loader slots. P53 adds smooth sky/weather foundation. P53C bundles starter Filament KTX assets for `day` and `studio_debug`; `sunset` and `night` remain fallback until real assets are added.

## Asset Sources

Preferred sources:

- Poly Haven CC0 HDRIs.
- OpenHDRI CC0 HDRIs.
- NASA SVS public-domain star maps.
- ambientCG CC0 only when the selected asset license is explicit.
- Filament sample environment assets only if source and license are clear.

Do not use mixed-license or store-only packs. Do not commit large raw `.hdr` or `.exr` files. The APK target for all bundled environment assets is under 10 MB.

Normal Gradle builds must not require internet. Download and conversion scripts are manual/offline pipeline tools only.

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

Manual pipeline:

```bash
python3 tools/env_asset_fetch_filament_tools.py --version 1.71.4
python3 tools/env_asset_download.py --slot day --url <verified-cc0-hdri-url>
PATH="$PWD/_work/filament_tools/bin:$PATH" python3 tools/env_asset_pack_build.py --slot day --size 256
python3 tools/env_asset_manifest_check.py
```

`tools/env_asset_fetch_filament_tools.py` is a manual helper for the Filament tool bundle. It is not part of Gradle and must not run during normal builds. `tools/env_asset_pack_build.py` refuses to proceed if `cmgen` is missing. `toktx` is required for optional star texture conversion. If either is unavailable, do not fake KTX files; leave fallback active and record the status.

## Runtime Integration

When an environment preset is selected, the Activity maps the P51 preset to a P52 slot and checks for matching KTX assets in Android assets. If the KTX exists, it reuses the existing Filament `KTX1Loader` path. If the KTX is missing or load fails, the app keeps the current P51 fallback.

P53C loads separate KTX files:

- `*_ibl.ktx` through `KTX1Loader.createIndirectLight`;
- `*_skybox.ktx` through `KTX1Loader.createSkybox`.

If either file is missing, diagnostics report `missing_asset_fallback`. If both files exist but loading fails, diagnostics report `asset_exists_loader_failed:<error>`.

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

## P53 Result Contract

P53 may ship no real KTX assets if `cmgen`/`toktx` are unavailable. That is acceptable only when:

- manifest slots keep exact source/license fields;
- `bundled=false`;
- status is `conversion_tool_unavailable` or `missing_fallback`;
- reports include `cmgen`/`toktx` availability;
- no raw HDR/EXR is bundled into `apps/engine/src/main/assets/env`.

P53C starter bundle:

- `day`: bundled, 3668388 bytes.
- `studio_debug`: bundled, 3668388 bytes.
- provenance status: `filament_sample_asset_provenance_from_google_filament_release_needs_final_license_audit`.
- `sunset` / `night`: fallback or `conversion_tool_unavailable`.

P53B also requires:

- no Android View sun/moon disk overlay;
- `screenSpaceSunMoonOverlayStatus=disabled_screen_space_overlay_rejected`;
- `sunVisualStatus=world_space_sky_disk_not_implemented`;
- `moonVisualStatus=world_space_sky_disk_not_implemented`.
