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
