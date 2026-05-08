# FUTURE_ENGINE_ROADMAP

## Renderer and glTF

- P08: multi primitive static render, baseColor material slots, texture slots, FPS, debug ZIP.
- P09: PBR Material Maps Foundation: metallicRoughness, normal, AO.
- Later: alpha blending, double-sided raster state, emissive, KTX2/BasisU, mesh optimization, animation/skinning.

## Product Layer

First future product candidate: Character Generator.

Product apps should reuse the SOLUM family UI language, but product screens must not expose raw engine debug controls by default.

## Physics Later

Deferred until renderer/material foundation is stable:

- rigid bodies;
- spring bones;
- cloth;
- hair;
- soft body;
- destruction.

## Runtime Optimization Later

- render resource scheduler;
- runtime job scheduler;
- mesh/texture streaming;
- performance snapshots;
- thermal-aware quality tiers.
