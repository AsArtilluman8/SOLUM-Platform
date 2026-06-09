# SOLUM Physical Material System

SOLUM materials must be more than visual.

Each object should have:

- visual material;
- physical material;
- construction profile;
- runtime state.

## Core idea

```text
mass = density * solidVolume
averageDensity = mass / displacementVolume
```

If average density is lower than fluid density, the object floats. If it is higher, it sinks.

A wooden crate is not a solid cube of wood. It is usually a hollow shell. SOLUM must not infer physical behavior only from the visual mesh.

## Physical properties

- densityKgM3
- hardness
- elasticity
- toughness
- brittleness
- frictionDry
- frictionWet
- restitution
- waterAbsorption
- flammability
- soundProfile
- breakMode
