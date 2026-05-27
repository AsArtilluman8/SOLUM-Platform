# SOLUM Platform Principles

Status: DRAFT architecture document.

This document is not a final implementation spec. It captures current SOLUM direction, rules, roadmap ideas, and design constraints.

Any section marked DRAFT must be discussed with Max before implementation. Codex/agents must not start implementing large systems from this document alone.

---

## 1. What SOLUM is

SOLUM is not a single demo, one-game hack, or one-model renderer.

SOLUM is intended to become a reusable Android/Mali-friendly creative and game platform.

Long-term ecosystem direction:

- SOLUM Launcher: entry point, module launcher, update/data-pack hub.
- SOLUM Engine: runtime/game engine.
- SOLUM Materials Lab: material creation, material instances, preview/testing.
- SOLUM VFX Lab: magic, particles, world impulses, effect authoring.
- SOLUM Character Lab: character, clothing, hair, skin, body/motion setup.
- SOLUM World Lab: scenes, terrain, props, procedural world tools.
- SOLUM Sound Lab: cinematic impact sound, spatial audio, haptics.
- SOLUM Asset Hub: shared assets, packs, presets, import/export.
- SOLUM Motion/Combat Lab: future movement, combat, animation feel tools.

The engine should not become a cluttered all-in-one editor. The runtime should stay clean. Deeper authoring should gradually move into focused labs/modules.

---

## 2. Core philosophy

Target:

- visually expensive;
- architecturally clean;
- mobile-cheap;
- reusable across future projects.

Main philosophy:

Do not chase physically perfect AAA simulation first. Implement lightweight, perception-based AAA illusion.

This means:

- fake reflections before real reflections;
- cheap fresnel/rim/specular tricks before heavy refraction;
- stable sorting before complex OIT;
- shader math before large fullscreen post-processing;
- believable world reaction before full physical simulation;
- mobile-first performance before PC-style feature completeness.

SOLUM should make the player believe the world is alive, heavy, reactive, and expensive-looking, without requiring full AAA hardware.

---

## 3. Mobile-first rule

Current target class:

- Android phone;
- Mali GPU;
- Termux/proot workflow;
- Vulkan/C++/Android stack;
- 45-60 FPS target before heavy effects.

Prefer:

- clear render routes;
- cheap shader variants;
- quality presets;
- simple stable passes;
- controlled diagnostics;
- data-driven materials;
- scalable features.

Avoid:

- unbounded dynamic lights;
- uncontrolled shader branching;
- heavy screen-space effects too early;
- PC-only assumptions;
- features that only work on desktop GPU;
- giant experimental patches without build/test proof.

---

## 4. Patch rules

SOLUM patches should be large enough to finish a real feature, but still realistic and testable.

Every patch should have:

- clear goal;
- preserved behavior;
- build command/result;
- test result;
- known issues;
- next step.

Avoid fake-complete patches that only add proof fields or diagnostics without improving the real route.

Status labels:

- PROTOTYPE: idea works, may break.
- FOUNDATION: architecture direction is correct.
- USABLE: usable in a test scene/project.
- POLISH: quality/UX/visual improvement phase.
- LOCKED: do not break without explicit reason.
- DEPRECATED: old path, do not continue.
- BROKEN: known broken state.

Current glass status notes:

- P30/P31 glass path: DEPRECATED. Do not continue.
- P32 clean glass path: FOUNDATION / USABLE.
- Car semantic glass after P32C4: USABLE, but weak control response on some bad/opaque GLBs.
- Glass visual polish: DRAFT / future P32D-P33.

---

## 5. Material architecture

Material system must evolve into a role-based architecture.

Correct flow:

Material data -> Material classification -> Material role -> Render route -> Shader variant -> Diagnostics -> UI/runtime controls.

Material roles should stay separated:

- OpaqueDefault
- CutoutFoliage
- Fabric
- Rubber
- Skin
- Hair
- TransparentGlass
- ChromeMetal
- CarPaint / Clearcoat
- Water
- Emissive
- VFXAdditive

Do not merge glass, chrome, water, foliage, fabric, rubber, car paint, and skin into one giant messy shader path.

Each material role should eventually have:

- classifier;
- render route;
- shader model;
- editor/lab controls;
- runtime controls;
- diagnostics;
- status label.

---

## 6. Glass rules

Current good direction:

- dedicated transparent glass route;
- glassQueue;
- separate glass pipeline;
- blend ON;
- depth test ON;
- depth write OFF;
- glass rendered after opaque/cutout;
- UI: On/Off, Opacity, Tint, Rough, Edge;
- Show Glass Geometry debug mode;
- diagnostics must reflect real native render state.

Glass classifier rule:

Not everything with window in the name is glass.

Strong glass names:

- glass
- pane
- glazing
- windshield
- windscreen
- lens
- crystal

Weak/context name:

- window

Window exclusions:

- frame
- wood
- metal
- trim
- bar
- handle
- rubber
- seal
- border
- hinge
- sill
- wall
- stone

Examples:

- WindowGlass = glass
- GlassPane = glass
- WindowPane = glass
- WindowFrame = NOT glass
- WindowMetal = NOT glass
- WindowWood = NOT glass
- WindowHandle = NOT glass

MASK/cutout foliage/fabric/leaf/petal/grass must not become glass.

This rule should be treated as a near-term classifier fix after P32C4/merge if the window model confirms frame tint/opacity regression.

---

## 7. Living world philosophy

DRAFT — concept direction, not immediate implementation.

SOLUM world should feel alive and responsive.

Not full real physics everywhere. The goal is believable reaction.

The player should feel:

- weight;
- rhythm;
- surface resistance;
- wind;
- water;
- mud;
- snow;
- impact;
- sound;
- NPC awareness;
- world memory.

Examples:

- grass bends from movement, wind, magic, impact;
- water ripples and splashes;
- mud/snow/sand leaves tracks;
- clothing becomes wet/dirty and later dries/cleans;
- dust reacts to footsteps and impacts;
- props break or move with believable impulse;
- wind affects cloth, leaves, grass, water, debris;
- terrain type affects movement feel.

Rule:

Do not simulate everything honestly. Make the player believe the world answered.

---

## 8. Combat philosophy

DRAFT — future gameplay direction, must be discussed before implementation.

Future SOLUM combat direction:

Fast readable fantasy combat with grounded physical feedback.

Avoid both extremes:

- not slow old-man combat with 3-second sword swings;
- not weightless anime chaos where every hit means nothing.

Core feel:

- fast;
- readable;
- rhythmic;
- body-aware;
- weighty through consequences, not slowness.

Potential future state system:

- Vitality: can the character continue fighting?
- Breath / Rhythm: is the body in clean rhythm?
- Balance: is the body stable?
- Focus: is the mind ready for precise technique?
- Inner Force / Qi: can the character exceed normal body limits?

Do not use boring stamina that makes the player tired after two moves.

Breath/Rhythm should reward clean input.

Balance should allow:

- stumble;
- stagger;
- lost weapon control;
- weakened block;
- fall;
- counter windows;
- different reactions for light/heavy enemies.

High level should give more options, not remove skill.

---

## 9. Magic philosophy

DRAFT — future gameplay direction, must be discussed before implementation.

Magic should have multiple modes:

- Quick Magic: fast combat gestures.
- Draw Magic: drawn spells.
- Body Magic: jumps, water run, air step, body reinforcement.
- World Magic: later, uses water/fire/stone/air/props/context.
- Ritual Magic: slower powerful magic outside fast combat.

Draw Magic concept:

- player draws a shape/gesture;
- character repeats the gesture with hand/weapon animation;
- spell preview appears;
- release or confirm gesture casts;
- unclear gesture should not hard-punish the player.

Important UX principle:

Gesture errors should degrade quality or cast a safe fallback, not randomly fire an expensive wrong spell.

Potential future formula:

Gesture = intention.
Context = material/source.
Target = direction/recipient.
Inner Force = strength.

This must not be implemented until the control/UX model is carefully designed.

---

## 10. Sound philosophy

DRAFT — future system direction.

SOLUM sound should give physical weight.

Target sound style:

- cinematic impact sound design;
- layered impact;
- LFE/sub-bass illusion;
- spatial/panned sound;
- haptic feedback;
- camera shake sync;
- material-dependent sound.

A strong impact should be layered:

- transient crack;
- body thump;
- sub/LFE illusion;
- debris/noise;
- whoosh;
- reverb tail;
- vibration/haptic pulse;
- visual shake/flash sync.

Sound must depend on material/world context:

- water;
- mud;
- stone;
- metal;
- wood;
- flesh;
- magic.

Goal:

Not just hear the hit. Feel the hit.

---

## 11. NPC and dialogue philosophy

DRAFT — future system direction.

Avoid dummy NPCs.

NPCs should have believable awareness:

- see;
- hear;
- notice traces;
- become suspicious;
- search;
- fear;
- attack;
- run;
- remember.

Better than old reputation:

Social Memory System.

NPCs should remember events, not just numbers:

- saw player help;
- saw player steal;
- heard rumor;
- player used forbidden magic;
- player came wet/bloody/dirty;
- player destroyed something nearby;
- player spared/killed someone;
- player caused a public incident.

Possible memory levels:

- Level 0: crowd NPC, reacts only to current state.
- Level 1: local memory, remembers 1-3 nearby events.
- Level 2: important NPC, personal memory and relationship.
- Level 3: story NPC, long memory and quest consequences.

Dialogue should be:

- short;
- reactive;
- characterful;
- sometimes humorous;
- tied to player actions;
- not boring exposition.

World rule:

Sound reacts. NPCs react. Dialogue reacts. Quests react. The world remembers.

---

## 12. Launcher and modules

DRAFT — platform direction, not current implementation.

Preferred early path:

- one SOLUM Launcher APK;
- internal modules first;
- separate APKs later only if needed;
- data packs/assets/presets should update through Launcher UI where possible.

Potential modules:

- Engine;
- Materials Lab;
- VFX Lab;
- Character Lab;
- World Lab;
- Sound Lab;
- Motion/Combat Lab;
- Asset Hub.

APK auto-update outside Google Play has Android limitations. Launcher can manage downloads and open installation UI, but fully silent app updates are not the default path for normal Android apps.

Data packs should be preferred for content/presets:

- .solmat
- .solvfx
- .solchar
- .solworld
- .solscene
- .solsound

---

## 13. Near roadmap

Current/near direction:

1. Merge P32 clean glass route into main.
2. Fix remaining glass classifier issues if confirmed:
   - WindowFrame / WindowMetal / WindowWood must not become glass.
3. Material Role Split Foundation.
4. Material Instance format / future Materials Lab foundation.
5. Chrome / Metal / CarPaint clean routes.
6. Engine UI cleanup.
7. FPS / Performance panel.
8. Lighting polish + cascaded shadows.
9. Sound Feel prototype.
10. NPC Awareness / Social Memory prototype.
11. VFX / Water / World Reaction later.
12. Combat / Magic / Rhythm systems much later.

Do not jump to world physics/VFX/combat before render/material/FPS/light foundation is stable.

---

## 14. Agent/Codex rule

This document is a direction map.

Codex/agents may use it for context, but must not implement DRAFT systems unless the user explicitly asks for that specific system.

Before implementing any large DRAFT block, ask/confirm scope in the task text.

Safe to implement without new concept discussion only when the task is precise, narrow, and tied to the current roadmap gate.
---

# SOLUM Platform Roadmap Notes — Glass, Materials, World Reaction, Living World

Status: concept, roadmap, and design memory.  
These notes are not all final implementation rules. Before any major block is implemented, it must be reviewed with the project owner, sliced into a safe patch, tested on Android/Termux, and validated against FPS, UI workflow, and runtime stability.

This document is intended to prevent loss of the larger SOLUM vision while near-term work continues on glass, materials, FPS, UI, lighting, shadows, and renderer foundation.

---

## 1. Current Foundation Status

SOLUM Platform is currently building the shared foundation for future SOLUM systems:

- Vulkan render core
- GLB / mesh / material route
- glass route
- material diagnostics
- runtime UI controls
- Android / Termux build workflow
- future Labs ecosystem
- future game/runtime systems

The goal is not only one game scene. The goal is a reusable foundation that can later support:

- SOLUM Engine
- SOLUM Launcher
- Material Studio
- VFX Studio
- Character Studio
- Motion Studio
- World Studio
- Sound Studio
- Asset Hub
- future gameplay prototypes

The runtime must stay clean and lightweight. Heavy experimentation, training, tuning, authoring, and ML can happen in separate labs/tools, then export compact runtime data.

Core direction:

```text
Labs create and tune.
Runtime consumes clean data.
Engine stays fast.
```

---

## 2. Near-Term Patch Roadmap

This is the current likely direction after P32 merge. The exact order may change if testing exposes blockers.

### P32D1 — Glass Per-Material Diagnostics

Goal: diagnose why normal standalone glass responds to opacity/tint, but car window glass responds weakly or not live.

Required output:

- per-material glass diagnostics
- selected candidate info
- car glass material info if detectable
- applied opacity/tint values
- shader alpha/tint values
- queue/pass info
- exact reason if UI values are not applied

No blind visual fix. Diagnostics first.

### P32D2 — Car Glass Live Opacity/Tint Fix

Goal: fix the exact route found by P32D1.

Acceptance:

- car glass opacity 0.2 / 0.6 / 1.0 visibly differs
- tint presets visibly differ
- update is live without moving camera
- normal standalone glass still works
- Show Glass Geometry still works
- Glass On/Off still works

### P32D3 — Glass Quality Hardening

Goal: make glass visually better without heavy refraction.

Possible features:

- thin glass mode
- solid / thick glass mode
- rim / edge highlight
- fake reflection / specular
- safer double-sided / backface handling
- better sorting diagnostics
- mobile-friendly only

Do not add expensive SSR/refraction yet.

### P32D4 — Glass UI / Debug Cleanup

Goal: clean Glass Lab UI after route is stable.

Rules:

- user-facing controls stay simple
- debug/proof details move under Debug
- Glass Route Test becomes readable
- no noisy labels in normal mode

### P33 — Material Route Separation Foundation

Goal: stop mixing unrelated materials in one path.

Separate material routes conceptually:

- Opaque PBR
- Glass
- Metal / Chrome
- Mirror / Reflection special case
- Fabric / Hair future
- Foliage / Cutout future
- Emission future
- Decal future

Do not implement everything at once. First make architecture clean.

### P34 — Metal / Chrome / Glossy Polish

Goal: improve hard-surface materials after routes are separated.

Focus:

- metalness / roughness response
- chrome-like highlight
- glossy paint
- better specular
- simple mobile reflection illusion
- diagnostics per material route

### P35 — Mirror / High Reflection Special Case

Goal: add limited expensive-looking reflection only for rare special surfaces.

Rules:

- not general SSR everywhere
- special-case mirror / royal glass / hero surface
- controlled quality
- mobile fallback
- debug cost visible

### P36 — UI Cleanup Pass

Goal: organize the editor/lab UI.

Direction:

- top tabs or compact categories
- Scene / Materials / Glass / Lighting / Debug / Performance
- hide proof/debug from normal workflow
- reduce clutter
- keep touch-friendly controls

### P37 — FPS / Performance Pass

Goal: make renderer measurable and stable.

Track:

- FPS
- frame time
- draw count
- material route counts
- glass count
- debug overhead
- quality presets
- Mali-friendly defaults

### P38 — Lighting / Shadow Polish

Goal: after material routes are stable, continue light/shadow quality.

Focus:

- sun / ambient balance
- specular readability
- cascaded shadow polish
- local light future
- mobile-safe cinematic look

### P39 — World Reaction / VFX Prototype Planning

Goal: prepare later integration of reaction physics concepts.

Do not implement full world reaction yet. First define runtime data, impulse events, and test scenes.

---

## 3. Material System Direction

SOLUM materials must not become one mixed shader path where glass, metal, fabric, foliage, mirror, and opaque objects fight each other.

Each material route should eventually have:

- detection rules
- runtime flags
- shader path
- diagnostics
- UI controls
- fallback behavior
- mobile quality level

Glass must not classify all alpha materials as glass. Foliage, grass, fabric, petals, leaves, mask/cutout objects must stay out of glass route unless explicitly assigned.

Semantic material names can help detect glass / window / lens, but must not override every alpha material blindly.

Material route goal:

```text
Clear route.
Clear debug.
Clear fallback.
No hidden material mixing.
```

---

## 4. SOLUM Feeling: What the Player Should Feel

SOLUM should not chase perfect AAA simulation first. The priority is believable response.

The player should feel:

- glass is glass, not just a transparent texture
- metal is heavy and reflective, not just gray
- water has resistance and movement
- mud pulls the body down
- snow compresses under weight
- grass bends and remembers steps briefly
- magic has force, shape, sound, and consequence
- enemies fall because of direction, mass, balance, and surface, not because a generic hit animation played
- NPCs and crowds feel alive without simulating every person fully

The target is:

```text
believable response
clear cause and effect
mobile-friendly illusion
good diagnostics
upgrade path later
```

Every future system should pass these questions:

1. Does the player feel it?
2. Does it explain itself visually?
3. Does it avoid mobile FPS collapse?
4. Can it be debugged?
5. Does it add gameplay feeling, not only visual noise?
6. Can it be reduced to a cheaper quality tier?

---

## 5. Reaction Physics Future Direction

SOLUM world reaction should be based on a shared impulse system:

```text
event creates impulse
surface reacts
body reacts
VFX reacts
sound reacts
NPC reacts
```

Core future concept:

```text
WorldImpulse:
- position
- direction
- force
- radius
- duration
- type
- surface
- source
```

This should be "directed believable reaction", not full simulation.

### Surface identity rules

Each surface should react by material identity:

- water: ripples, wake, splash, foam, wetness
- mud: deep footprints, sticky droplets, wet marks
- swamp: slow sinking, bubbles, viscous recovery
- sand: dry depression, dust, soft sliding
- snow: compressed hole, powder, soft landing
- grass: bend field, temporary trail, recovery
- stone: chips/cracks only on strong impact
- wood: splinters/props reaction

Important rule:

```text
No rocks from water.
No dry dust from pure water.
No boulders from snow/grass unless earth magic or stone source exists.
```

### Surface feeling target

Water should not be only circles on a plane. It should feel like shallow water:

- foot enters water -> local depression, small splash, ripple
- walking -> V-shaped wake and wet legs
- strong impact -> splash crown, droplets, mist, foam
- lightning on water -> electric ripple / glow
- fire on water -> steam, not rocks

Mud should feel sticky:

- foot sinks
- step delays
- dark wet footprint
- sticky droplets when foot lifts
- falling creates mud mark on body

Swamp should feel worse than mud:

- deeper sink
- slower movement
- bubbles
- slow recovery of the hole
- heavy, sticky falling and getting up

Sand should feel dry:

- shallow depression
- low dust
- sliding foot
- no wet shine

Snow should feel soft and deep:

- compressed footprint
- powder around step
- soft landing puff
- snow marks on boots/knees

Grass should feel alive:

- bends by foot direction
- bends by wind/impulse wave
- leaves temporary trail
- slowly recovers

---

## 6. Character Reaction / Animation Future Direction

Characters should not use one reaction per ability.

Reaction must depend on:

- force
- distance
- direction
- body mass
- balance
- current surface
- stance
- hit side
- magic type

Future reaction library:

- micro flinch
- cover face
- brace
- foot adjust
- step back
- side stagger
- spin stagger
- slip recover
- knee buckle
- fall back
- fall forward
- side fall
- trip fall
- knee collapse
- airborne launch
- slide fall
- lightning spasm
- heat recoil
- water slip
- wind push
- slash reaction

SOLUM should prefer hybrid/fake active ragdoll:

```text
animation drives body
physics adds weight, delay, inertia, landing
recovery returns character to control
```

No full ragdoll sack behavior unless explicitly needed.

### Reaction examples

Weak impulse:

- flinch
- cover face
- brace
- foot adjustment

Medium impulse:

- step back
- side stagger
- spin stagger
- slip recover
- knee buckle

Strong impulse:

- fall back if hit from front/chest
- fall forward if hit from behind
- side fall if hit from side
- trip fall if legs/surface are compromised
- airborne launch only for strong upward/earth/explosion impulse

Magic-specific:

- Storm Lance should stun/spasm more than throw
- Azure Tide should slip, push, wet, and disturb water
- Verdant Pulse should push cloth/grass/leaves/trees
- Ember Rupture should cause heat recoil, smoke, sparks
- Earth Break should crack ground and launch debris only from valid surfaces
- Crimson Slash should create directional stagger/spin, not generic explosion

---

## 7. Magic / VFX Future Direction

Magic must not be only different colors.

Each school/element should have its own silhouette, timing, force profile, sound, and surface reaction.

Examples:

- Crimson Slash: narrow cutting arc, side stagger, water split line
- Azure Tide: water sheet/wave, wetness, slip
- Arcane Nova: layered radial force pulse
- Storm Lance: branching lightning, stun/spasm, electric water ripple
- Verdant Pulse: wind/nature wave, grass/tree/leaves response
- Ember Rupture: heat, smoke, sparks, recoil, steam on water
- Earth Break: cracks, stone chunks only from valid surfaces

Rule:

```text
If color is removed, the player should still recognize the magic by shape and reaction.
```

VFX should contain:

- silhouette
- timing
- light pulse
- camera response
- particles only where they make sense
- surface-specific reaction
- sound profile later

VFX should not be:

- same circle with different color
- generic particle spam
- rocks from impossible surfaces
- huge effect without gameplay cause

---

## 8. Progression / Body / School Direction

Future gameplay should not rely only on typical stat tables.

Potential direction:

```text
Body + Origin + Fate Mark + School + Weapon + Craft + Player Skill
```

Body should affect how techniques feel:

- light body: speed, agility, easier displacement, faster recovery
- heavy body: mass, stability, heavier impact, slower recovery
- trained body: better control, rhythm, balance

School should not erase body identity. It should reinterpret it.

Example:

- light body + Stone School = technical stability, precise counters, not brute force
- heavy body + Wind School = short powerful mobility, not feather-like movement
- blacksmith origin + light body = precise weapon balancing, hidden mechanisms, magic channels
- blacksmith origin + heavy body = heavy weapon forging, armor, guard pressure

Controlled randomness should create identity, not forced rerolls.

Rules:

- no dead classes
- no useless origins
- no pure stat-only backgrounds
- no mandatory rerolling for best start
- rare path means unusual/hard/dangerous, not automatically stronger

---

## 9. NPC / Animal Behavior Future Direction

SOLUM may use offline ML/training tools for animals, NPC instincts, movement, and animation research.

Runtime rule:

```text
Heavy ML training stays outside the game runtime.
Game runtime consumes lightweight exported behavior packages.
```

Possible exported package:

- behavior JSON
- instinct profile
- state machine
- movement curves
- procedural animation rules
- skeleton map
- animation clips
- reaction tags
- debug/explain report

This allows experiments with cats, animals, NPCs, crowd behavior, and movement without shipping heavy ML in the game.

This is especially important for:

- animal movement
- pet / creature behavior
- NPC instincts
- crowd motion
- non-scripted idle behavior
- animation selection
- future Character Studio / Motion Studio

---

## 10. Crowd Audio Future Direction

Crowd audio should use a layered “broom” model:

```text
far crowd = shared bed
near important people = selected focus voices
```

Architecture:

```text
Crowd Ambience Manager
- global crowd bed
- zone emitters
- focus voice selector
- NPC voice pool
- occlusion / muffling
- distance low-pass
- ducking for nearby speech
```

Budget example:

- 1–3 global crowd loops
- 2–8 zone emitters
- 2–4 active NPC focus voices
- 1 important dialogue voice

Audio islands:

- fish stall
- forge
- cloth stall
- tavern
- port
- gate
- market square

The player should feel that many NPCs are alive, while the engine only plays a small controlled number of real voices.

Important sound behavior:

- far crowd is blended murmur
- nearby NPCs temporarily separate from the murmur
- distance reduces high frequencies
- occlusion muffles behind walls/tents
- close speech ducks crowd bed slightly
- important dialogue gets highest priority

---

## 11. Launcher / Labs Ecosystem Direction

Long-term SOLUM may have one Launcher APK or central app that connects tools/labs.

Possible apps/labs:

- SOLUM Engine
- Material Studio
- VFX Studio
- Character Studio
- Motion Studio
- World Studio
- Sound Studio
- Asset Hub

Launcher direction:

- one entry point
- manage app/lab versions
- open/install/update local APKs where Android allows
- no hidden auto-update assumptions outside platform rules
- keep engine runtime separate from heavy authoring tools

Labs should export runtime-compatible data. They must not become beautiful but incompatible editors.

Rule:

```text
What looks correct in Lab must export predictably to Engine.
```

---

## 12. Scope Control Rule

SOLUM has a large vision, but patches must stay grounded.

Before implementing any future system:

1. define the smallest test scene
2. define the visible feeling
3. define debug proof
4. define mobile performance risk
5. define fallback quality
6. define what is explicitly not included

Do not add major future systems directly into the renderer foundation without a clear slice.

Near-term priority remains:

```text
glass -> material route separation -> metal/chrome/gloss -> mirror special case -> UI cleanup -> FPS -> lighting/shadows -> later world reaction/VFX/gameplay
```
