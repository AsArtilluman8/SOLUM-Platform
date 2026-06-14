# SOLUM UDS / UDW Recon Status

Patch: `P_UDW_Weather_Showcase_Camera_Cleanup_01`

## Input

Latest light output:

- `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/manifest_light.json`
- `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/10_ASSET_MAP/asset_inventory_light.json`
- `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/21_BLUEPRINT_NODE_TABLES/all_node_tables_light.json`
- `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/60_RECIPES/solum_recon_light_recipe.json`
- `/storage/emulated/0/Download/SOLUM_RECON_TOOLKIT_KEEP/latest_light_output/70_REPORTS/reconstruction_light_report.md`

Generated runtime recipe:

- `apps/engine/src/main/assets/weather/solum_udw_runtime_recipe.json`

## Channel Status

Status source:

- `asset-derived`: direct decoded local candidate or scalar with strong proof.
- `reconstructed`: inferred from recon output names/node tables/recipe.
- `procedural fill`: mobile-safe default because light output did not prove a value.
- `blocked`: output missing or undecodable.

Current generator result:

- `reconstructed`: see `docs/SOLUM_UDS_UDW_RUNTIME_RECON_SUMMARY.json`
- `procedural fill`: see `docs/SOLUM_UDS_UDW_RUNTIME_RECON_SUMMARY.json`
- `blocked`: none while latest light output exists.

## Runtime Channels

- `timeOfDay`
- `dayNightFactor`
- `cloudCoverage`
- `rainAmount`
- `snowAmount`
- `fog`
- `windDirection`
- `windIntensity`
- `materialWetness`
- `materialSnowCoverage`
- `materialDustCoverage`
- `thunderLightning`
- `flashLightning`
- `temperature`
- `weatherState`
- `manualWeatherState`
- `randomWeatherVariation`

## Proof Limits

- No full UDS/UDW Blueprint graph clone.
- No paid raw UAsset content copied.
- Decoded PNG/WAV candidates are local private candidates and are not committed as app assets in this patch.
- Material graph reconstruction is `HOLD`.
- UAsset decode is `HOLD_UASSET_DECODE`.
