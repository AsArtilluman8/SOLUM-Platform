# ADR-0005: Use proven repositories as references, not blind dependencies

## Status

Accepted.

## Decision

SOLUM should not invent complex rendering/material/shadow/animation/mechanics systems from scratch when proven open-source architecture exists.

However, SOLUM must not blindly replace itself with a whole external engine/framework.

## Why

Previous experience showed that inventing shadows/materials/rendering only from AI guesses causes:

- visual bugs;
- wrong architecture;
- performance regressions;
- many patch cycles;
- fake progress.

Using proven repositories reduces guesswork.

## Correct process

For a hard system:

```text
identify problem
↓
find proven repo/docs
↓
read relevant implementation
↓
extract architecture principle
↓
decide: reference / small slice / adapter / dependency / reject
↓
record decision
↓
implement SOLUM-compatible version
↓
add diagnostics/test
```

## Priority references

### Vulkan foundation

- Android NDK Vulkan samples.
- Khronos Vulkan Samples.

### Renderer architecture

- The Forge.
- Filament.
- bgfx.
- GPUOpen/Cauldron concepts.

### Asset pipeline

- tinygltf.
- meshoptimizer.
- KTX-Software / BasisU.

### Animation

- ozz-animation.

### ECS / mechanics

- EnTT.

### Mobile GPU

- Arm Mobile Studio docs.
- Mali performance guides.

## The Forge rule

The Forge is not a replacement for SOLUM.

Use it to study:

- resource lifetime;
- descriptor management;
- render graph/frame graph;
- barriers;
- renderer abstraction.

Do not import huge parts without adapter/build proof.

## Required documentation

When external reference influences a system, write:

- what was studied;
- what principle was adopted;
- what was rejected;
- why it fits Android/Vulkan/Termux;
- what diagnostics/tests prove the implementation.
