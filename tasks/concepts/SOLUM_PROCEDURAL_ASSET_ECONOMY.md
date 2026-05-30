# SOLUM Procedural Asset Economy

## Principle

SOLUM is not an asset warehouse.
SOLUM is a variation factory.

The target is not:
- thousands of marketplace assets;
- duplicated 4K textures;
- huge folders copied into APK;
- every object having unique materials.

The target is:
- small high-quality base library;
- master materials;
- masks;
- modular meshes;
- procedural variation;
- seed-based generation;
- runtime instance variation;
- strong compression.

## Target asset philosophy

Instead of:

- 1000 houses
- 500 trees
- 300 rocks
- 200 NPCs
- 200 water/glass/mud materials

Use:

- 20-40 building modules
- 20-50 props
- 20-40 nature meshes
- 10-30 NPC body/head/hair/clothes modules
- 100-200 strong texture sets
- 20-40 master materials
- thousands of procedural variants

## Master materials

Required master materials:

- M_Glass_Master
- M_Liquid_Master
- M_Wood_Master
- M_Stone_Master
- M_Metal_Master
- M_Cloth_Master
- M_Skin_Master
- M_Foliage_Master
- M_DirtMud_Master
- M_Snow_Master
- M_Sand_Master
- M_Magic_VFX_Master

## Material presets

A master material becomes many presets.

Example: M_Glass_Master can become:
- clean window
- dirty window
- cup
- bottle
- glasses lens
- ice
- stained glass
- magic crystal

Example: M_Liquid_Master can become:
- puddle
- swamp
- river
- lake
- ocean
- blood
- wine
- poison
- oil
- magic liquid

## Masks

Masks are critical.

Required masks:
- dirt
- wetness
- moss
- edge wear
- scratches
- cracks
- burn
- blood
- snow
- sand dust
- curvature
- height
- ambient occlusion
- color variation

One mesh + one material + different masks = many believable variants.

## Modular buildings

Buildings should be generated from parts:

- walls
- doors
- windows
- roofs
- corners
- stairs
- balconies
- beams
- trims
- chimneys
- signs
- damage pieces
- moss/dirt masks

Generation parameters:
- seed
- style
- district
- floors
- roof type
- window layout
- damage
- dirt
- wetness
- moss
- props around building

## Modular NPCs

NPC variation should come from:

- body base
- head variants
- hair variants
- beard variants
- skin tones
- clothes layers
- armor parts
- belts/bags
- weapons
- height scale
- body proportions
- walk style
- idle animation
- voice set
- behavior profile
- personality seed

## Asset intake rule

Every imported asset must answer:

Can this become:
- reusable module?
- material preset?
- texture source?
- mask source?
- animation source?
- procedural variation source?

If not, it is suspicious.

## APK rules

Use:
- KTX2
- BasisU
- ASTC
- ORM packing
- texture atlases
- shared materials
- shared skeletons
- meshopt compression
- LOD
- asset deduplication

Avoid:
- duplicated 4K textures;
- raw marketplace folders;
- each prop having its own full texture set;
- each NPC having its own skeleton;
- unused assets in APK.

## Technical systems needed

SOLUM Asset System:
- Master Materials
- Material Presets
- Texture Library
- Mesh Library
- Modular Building Generator
- NPC Modular Generator
- Prop Variation System
- Runtime Instance Variation
- Mask Painter
- LOD Generator
- Collision Generator
- Texture Compression
- Asset Manifest System

## Rule

Use ready assets as raw material, not as final architecture.
