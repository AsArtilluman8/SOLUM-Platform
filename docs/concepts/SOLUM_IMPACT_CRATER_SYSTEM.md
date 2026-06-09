# SOLUM Impact Crater System

Goal: create believable terrain impact and crater events without full voxel destruction.

A crater should not appear as an instant texture swap. It should be born through an event: charge, flash, shockwave, impulse, foliage dissolve, dust, debris, terrain/material change, decal, smoke, and final collision update.

## Event phases

1. Charge: light, sparks, ground glow, grass bends.
2. Impact: flash, shockwave, camera shake, impulse, dust, sound.
3. Dissolve: grass and small objects glow/fade/remove.
4. Crater reveal: terrain patch or crater mesh, decal, smoke.
5. Settle: dust fades, final collision activates.

## Technical options

- Small impact: decal, normal/parallax, dust.
- Medium impact: crater mesh overlay, material mask change, foliage dissolve.
- Large impact: terrain height patch, debris, stronger VFX, persistence event.

## Gameplay safety

During transition, the center should be temporarily unsafe or locked through shockwave, heat, magic field, or debris so the player does not step into unfinished geometry.
