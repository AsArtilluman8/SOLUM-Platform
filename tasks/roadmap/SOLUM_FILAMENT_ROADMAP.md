# SOLUM Filament Roadmap

Current state:
- Filament is the primary renderer.
- gltfio handles GLB/GLTF preview.
- HDR/IBL import exists.
- Render Control Center exists.
- Old Vulkan renderer is deprecated.

Near-term patches:
- P44: Filament API Expansion + Legacy Vulkan Removal
- P45: Material Slot Inspector
- P46: Material Override + Glass Tools
- P47: Light / Shadow Rig Finalization
- P48: Scene Stage / Ground / Preview Environment
- P49: Sky / Sun / Moon / Stars / Time of Day
- P50: Post Process / Cinematic Controls
- P51: GLB Animation Support
- P52: Asset Workspace / Library
- P53: VFX Architecture Audit
- P54: SOLUM VFX Core Runtime
- P55: Filament VFX Adapter
- P56: AAA Magic VFX Pack
- P57: Weather / Atmosphere
- P58: Water Foundation
- P59: Reactive World Foundation

Important VFX decision:
- Do not build primitive throwaway particles.
- Target: SOLUM VFX Framework, Niagara-like mobile architecture.
- Filament is render adapter.
- WickedEngine GPU particles = main AAA architecture reference.
- Effekseer = audit/authoring candidate, not mandatory final runtime.
- JSON only for source/debug.
- Runtime format should become binary .solumvfxb.
