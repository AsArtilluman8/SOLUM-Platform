# SOLUM Private Asset Pipeline

Status: P55 clarification over P54A safety gate.

## Purpose

SOLUM can study or use purchased local assets privately without contaminating the public repository.

Public Git contains original SOLUM code, generated SOLUM assets, or assets with clear CC0/permissive provenance only.

## Ignored private paths

Current ignored paths:

```text
private_assets/
_private_assets/
local_assets/
apps/engine/src/main/assets/private_env/
apps/engine/src/main/assets/private_weather/
apps/engine/src/main/assets/private_premium/
```

Do not commit raw Unreal/Fab/Marketplace files.

## Manifest template

Use a local ignored manifest when private assets are used:

```json
{
  "schema": "solum_private_asset_manifest",
  "schemaVersion": 1,
  "enabled": false,
  "sourcePack": "local reference only",
  "localPath": "/storage/emulated/0/Download/SOLUM_PRIVATE_ASSETS",
  "allowedUse": ["local_reference", "private_runtime_test"],
  "publicRepoSafe": true,
  "containsPaidSourceFiles": true,
  "publicFallbackRequired": true,
  "notes": "Do not commit this manifest if it reveals private paths or paid asset details."
}
```

## Runtime rule

Private assets are optional.

SOLUM public builds must run without them using public/generated fallback state.

P55 reports:

- `privateAssetsEnabled=false`;
- `paidAssetsTracked=false`;
- fallback status active when no public asset exists.

## Ultra Dynamic Sky

Ultra Dynamic Sky can inform architecture concepts such as controllers, presets, weather masks, wetness, clouds, rain, snow, and aurora behavior.

It must not be copied into SOLUM public source or assets.
