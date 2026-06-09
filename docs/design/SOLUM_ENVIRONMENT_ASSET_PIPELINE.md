# SOLUM Environment Asset Pipeline

Status: P51 docs/backlog. P52 will add real assets.

## Planned Sources

- Poly Haven CC0 for HDRI/IBL.
- OpenHDRI CC0 for HDRI/IBL.
- NASA SVS star maps public domain.
- Filament `cmgen` for cubemap/IBL preprocessing.
- Sky3D/Godot references only.
- Stardroid reference later.
- Bruneton / andrewwillmott sun-sky later, not P51.

## P51 Rule

Do not download or commit HDRI/star assets in P51.

P51 only defines slots and fallback behavior:

```text
assets/env/day_ibl.ktx
assets/env/day_skybox.ktx
assets/env/sunset_ibl.ktx
assets/env/sunset_skybox.ktx
assets/env/night_ibl.ktx
assets/env/night_skybox.ktx
assets/env/stars_milkyway.ktx
```

Missing assets must keep the current IBL or neutral background active.

## Future P52

P52 should:

- pick CC0 source assets;
- document license/source URL per asset;
- run a reproducible Filament `cmgen` pipeline;
- create mobile-size KTX outputs;
- keep diagnostics honest about loaded vs fallback assets.

Avoid fake blue sphere as the main sky system. A neutral clear color is only fallback.
