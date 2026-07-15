# NOTE 0055 — P62B SOLUM Native Environment

## Decision

Не переносить Unreal execution model в HTML. UDS/UDW используются как evidence-backed behavioral reference; browser runtime является самостоятельной системой SOLUM на WebGL2.

## Inputs studied

- P60 truth root: exported UDS parameters, curve keys, resource inventory and decoded WAV payloads.
- P61 scene truth root: 802 indexed packages, 13 weather preset exports and scene/environment dependency evidence.
- Предыдущие P37/P41 HTML prototypes: только как anti-regression reference; абсолютные пути, `Math.random`, WebGL1 и недоказанные approximations не перенесены.

## UDS_VERIFIED reused

- Preset values for cloud coverage, fog, rain, snow, dust, wind, lightning and wetness across all 13 states.
- Time anchors/range: Dawn `600`, Dusk `1800`, range `0..2400`, initial `960`.
- Sun, moon, stars, fog, clouds, wind, precipitation, wetness and lightning defaults stored in the compact package with source paths.
- Four compact WAV payloads: `RainHit_1`, `RainHit_2`, `RainHit_3`, `Dust_3`.
- Compact curve keys for atmosphere density, directional light intensity, sun light colour and cloud coverage. `CloudCoverage_RGB` is actively sampled by the browser mapping; curves with unknown graph input semantics remain stored but inactive.

## Derived/native boundary

- `UDS_DERIVED_MAPPING`: normalization to browser ranges, interpolation, celestial orbit anchored to verified Dawn/Dusk, material/light response mappings.
- `SOLUM_NATIVE`: atmosphere/cloud/fog shaders, deterministic precipitation/dust particles, scene diagnostics, lightning geometry, wind deformation, wet surface rendering and mobile controls.
- `UNKNOWN`: exact MetaSound event/bus binding and unavailable internal graph behavior. Automatic audio is disabled.
- `UNAVAILABLE`: browser-decodable payload for selected Oodle/legacy textures, full Niagara VM and Unreal graph execution.

## Runtime contract

Generated HTML reads one package under 250 KB plus local shaders and optional local WAV. It does not read P60/P61 reports after build, uses no CDN, and exposes nine deterministic visual scenarios for review.

## Verification boundary

Schema, provenance, 13 presets, JS logic, shader compilation, local serving and byte-identical rebuild are automated. The current Termux host has no working headless WebGL2 context; final Android-browser visual/FPS capture remains a user-device review gate and no visual parity claim is made.
