# Research Summary — P63.3: Analytic Sky Pipeline

## 1. Problem

P63.2B is technically stable, but separate Moon phase meshes, star geometry, Sun shells and
transparent cloud clusters cannot produce a continuous premium sky and create visible phase,
sprite and alpha-sorting artifacts.

## 2. Current SOLUM state

Baseline: local commit `717726c62cbf1056ed00c8daa39fe82460593ae8`.
The colored Filament renderer, diagnostic stage, old IBL, camera controls, verified audio and the
`IndirectLight = clamp(intensity * blend, 0..2)` regression fix must remain intact.

Baseline systems recorded before implementation:

- CPU-generated colored sky cubemap and prepared day/sunset/night states;
- separate Sun core, inner glow and halo geometry;
- Moon texture/normal plus 65 discrete phase nodes in the diagnostic GLB;
- star-size/group geometry variants;
- 12 transparent geometry-cloud clusters;
- unchanged old IBL path and fixed `IndirectLight` clamp;
- existing nine-tab Environment UI and circular HSV picker;
- `p63_2a_celestial_test_stage.glb` diagnostic/celestial test stage.

## 3. References checked

- `google/filament@579991668ebeadceece05d79b62f21964028553f`
  - `web/examples/sky/README.md`
  - `web/examples/sky/SimulatedSkybox.js`
  - `web/examples/sky/main.js`
  - `web/examples/materials/simulated_skybox.mat`
  - `filament/src/materials/skybox.mat`
  - `filament/src/details/Skybox.cpp`
  - `filament/src/Skybox.cpp`
  - `android/filament-android/src/main/cpp/SkyBox.cpp`
- Local UDS truth roots listed in `P63_3_SKY_TRUTH_MANIFEST.json`.
- Local Epic UE source search by `SkyAtmosphere`, `VolumetricCloud`,
  `AtmosphereTransmittance`, `CloudShadow`, `AerialPerspective`, and `SunSky`.

## 4. What the references teach

### Filament

Useful principles:

- one device-domain full-screen triangle and one permanent material instance;
- reconstruct the world view direction with `getWorldFromClipMatrix()`;
- Kasten–Young optical air mass with Rayleigh/Mie/ozone extinction;
- analytic Sun disk with limb darkening and atmospheric transmittance;
- Moon sphere-normal reconstruction, continuous `N·L` phase, earthshine and texture/normal detail;
- radial/subpixel stars and spherical cloud-shell intersection inside the sky pass;
- mobile compilation through the matching `matc` release.

Rejected parts:

- web/JavaScript runtime code;
- water reflection, which evaluates the sky again;
- the sample's dynamic tone-mapping replacement;
- dynamic IBL and any new SceneView dependency;
- unbounded radiance values.

### UDS truth

Useful payloads:

- verified `moon_color.png`, `moon_normal.png`, `real_stars.png`, and `tiling_stars.png`;
- conceptual naming for cloud profiles/noise and weather controls.

Rejected parts:

- `Stars_Noise` as visible stars;
- cloud/LUT candidates whose packed channels or axes are not verified;
- any fake technical mask or claim that procedural clouds are UDS assets.

### Epic UE source

No local or authenticated Epic source tree was available. No Epic implementation was copied or
claimed as studied. Only locally extracted UDS asset/material reports were audited.

## 5. Options

- `REFERENCE_ONLY`: leaves the P63.2B placeholder architecture unchanged.
- `SMALL_SLICE`: improves only one body, but retains cross-system sorting and transition issues.
- `ADAPTER`: a SOLUM-owned state/resources/renderer layer around one Filament material.
- `DEPENDENCY`: SceneView or another sky runtime adds unnecessary lifecycle/dependency risk.
- `REJECT`: water reflection and dynamic IBL are explicitly deferred.

## 6. Recommended choice

`SMALL_SLICE + ADAPTER`: independently adapt the analytic formulas and full-screen draw structure
into SOLUM-owned Java/material code, with P63.2B preserved as automatic load/error fallback.

## 7. SOLUM adaptation

- `SolumEnvironmentState -> SolumAnalyticSkyState -> SolumAnalyticSkyRenderer`;
- one full-screen triangle, one material, one material instance, immutable textures;
- only finite uniform updates per frame;
- verified private Moon/star payloads with deterministic native fallbacks;
- Low default, Medium second cloud layer, High Experimental extra noise octave only;
- old IBL and direct directional lights remain separate from sky visuals;
- physical UI units use logarithmic sliders and larger finite exact-input ceilings.

## 8. Risks

- fragment cost on Mali-G57, especially the second cloud layer;
- visual calibration of physical units is safe/log mapped rather than device-photometer calibrated;
- shader/package load failure on a driver;
- private truth assets may be absent in a clean public checkout.

All have explicit Low/fallback/provenance handling. High is not default and is not called
volumetric or premium without device measurement.

## 9. Diagnostics/test plan

- compile with Filament 1.71.4 `matc -p mobile -a all`;
- inspect material metadata and package presence;
- static/unit contracts for finite clamps, one draw, continuous phase, no geometry main path,
  cloud/star occlusion hooks, persistence and UI tags;
- Android debug and Android-test APK builds through `tools/agent_build_runner.sh`;
- manual TECNO visual/performance scenarios from `P63_3_SKY_PRESETS.json`.

## 10. User decision required

None. The user explicitly selected the analytic adapter path and excluded water/dynamic IBL.

## License

Filament reference source is Apache-2.0. UDS payloads remain ignored local private assets and are
not committed. The adaptation source is SOLUM-owned and records upstream commit/path provenance.
