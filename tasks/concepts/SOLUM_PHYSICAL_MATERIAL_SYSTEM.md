# SOLUM Physical Material System

## Principle

SOLUM materials must be more than visual.

Each object should have:
- visual material;
- physical material;
- construction profile;
- runtime state.

Visual material answers:
- how does it look?

Physical material answers:
- how dense is it?
- how hard is it?
- how elastic is it?
- does it burn?
- does it absorb water?
- how does it sound?
- how does it break?

Construction profile answers:
- is it solid?
- hollow?
- shell?
- porous?
- sealed?
- open?
- filled with air/water/sand?
- wall thickness?
- displacement volume?

Runtime state answers:
- wetness
- damage
- temperature
- contamination
- cracks
- internal water fill
- integrity

## Why material alone is not enough

A wooden crate is not a solid cube of wood.
It is a hollow shell.

A barrel can be empty, filled, cracked, sealed, or leaking.

A glass cup is a thin shell, not a solid block of glass.

A boat floats because of displaced volume and hollow structure.

Therefore SOLUM must not infer physics only from the visual mesh.

## Core formula

mass = density * solidVolume

averageDensity = mass / displacementVolume

If averageDensity < fluidDensity:
- object floats

If averageDensity > fluidDensity:
- object sinks

If hollow object leaks:
- waterFill increases
- mass increases
- object can start floating and later sink

## Object profile example

Object:
- wooden_crate_01

Physical:
- material: wood_pine_dry
- sizeMeters: [1.0, 1.0, 1.0]
- construction: hollow_box
- wallThicknessM: 0.02
- solidVolumeM3: 0.08
- displacementVolumeM3: 1.0
- sealed: true
- leakRate: 0.0
- damageIntegrity: 1.0

## Physical material properties

Each physical material can have:

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
- heatConductivity
- electricalConductivity
- corrosion
- soundProfile
- breakMode
- pierceResistance
- compressionStrength
- tensileStrength
- shearStrength

## Damage / impact

impactEnergy = 0.5 * mass * velocity^2

Compare impact energy against:
- dentThreshold
- crackThreshold
- breakThreshold
- pierceThreshold
- shatterThreshold

Possible reactions:
- dent
- crack
- splinter
- chip
- shatter
- deform
- tear
- pierce
- burn
- bounce
- sink
- splash
- sound event

## Sound

Impact sound should be selected from:

- material A
- material B
- mass
- velocity
- hollowness
- wetness
- damage
- surface type

Examples:
- wood on stone = dry dull impact
- metal on stone = sharp ringing
- empty barrel = resonant hollow hit
- wet wood = muted hit
- glass = ring + crack/shatter chance
- flesh = soft body impact

## Initial material set

Start with 30-100 game physical materials:

- wood_light
- wood_heavy
- stone_soft
- stone_hard
- iron
- steel
- bronze
- glass_thin
- glass_thick
- cloth_light
- cloth_heavy
- leather
- flesh
- bone
- mud
- sand
- snow
- ice
- rubber
- water
- oil
- blood
- magic_energy

## Relationship with world reaction

Physical Material System feeds:
- water buoyancy;
- object sinking/floating;
- fracture;
- deformation;
- sound;
- VFX;
- decals;
- footsteps;
- magic impact;
- weather wetness;
- fire/burning;
- NPC/body reaction.

## Rule

Material answers: what is it made from?
Construction answers: how is it built inside?

Both are required.
