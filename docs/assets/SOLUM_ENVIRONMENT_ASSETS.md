# SOLUM Environment Assets

Status: P53C starter KTX manifest.

P52 ships no external HDRI, EXR, star map, or large raw environment asset. It adds the manifest, paths, validation, and runtime loader slots so real KTX assets can be added later without changing the P51 Environment API.

P53C bundles a small starter Filament KTX set for `day` and `studio_debug`.

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

## P53C Starter Status

Bundled:

- `env/day_ibl.ktx` — 2095456 bytes.
- `env/day_skybox.ktx` — 1572932 bytes.
- `env/studio_debug_ibl.ktx` — 2095456 bytes.
- `env/studio_debug_skybox.ktx` — 1572932 bytes.

Total bundled environment asset estimate: `7336776` bytes.

Source/provenance:

- Filament sample environment `lightroom_14b`.
- Copied from local downloaded Filament tools package path `_work/filament_tools/filament/bin/assets/ibl/lightroom_14b`.
- Conservative status: `filament_sample_asset_provenance_from_google_filament_release_needs_final_license_audit`.

Do not call these assets CC0. Exact sample asset license must be audited before public/commercial claim.

Safe source candidates remain:

- Poly Haven CC0 HDRIs for day/sunset/cloudy.
- OpenHDRI CC0 or Poly Haven CC0 for night.
- NASA SVS public-domain star map only if small and clearly documented.

Not bundled:

- raw `.hdr`;
- raw `.exr`;
- unverified royalty-free samples;
- true volumetric cloud assets.

Fallback remains for:

- `sunset`;
- `night`;
- optional `cloudy`;
- optional stars.

P54 next can add dedicated cloud shadows and rain/snow VFX/performance after the P53 foundation is verified on phone.

One-command tool fetch attempt:

```bash
python3 tools/env_asset_fetch_filament_tools.py --version 1.71.4
```

If that succeeds, run:

```bash
PATH="$PWD/_work/filament_tools/bin:$PATH" python3 tools/env_asset_pack_build.py --slot day --slot sunset --slot night --size 256
```
