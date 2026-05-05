# GAMEPLAY_MECHANICS_LIBRARY_REFERENCE — future ARPG mechanics knowledge base

Этот файл не является текущим implementation plan. Это справочник будущих mechanics для Action RPG, чтобы позже не придумывать базовые системы заново.

## Current status

Do not implement Mechanics API now.

Current startup remains:

```text
core → diagnostics → asset schema → Vulkan capability check → Vulkan foundation → Asset Hub → Material Studio
```

Mechanics are documented for future planning.

## Goal

Eventually SOLUM can expose reusable mechanics modules:

```text
DashAbility
LockOnSystem
AttackCombo
HitboxSystem
DamageSystem
Inventory
QuestSystem
DialogueSystem
AIStateMachine
```

These should be configurable and overrideable per project, like a simplified blueprint/ability library.

## Movement and camera

Future mechanics:

- third-person camera;
- lock-on target system;
- dodge/roll with i-frames;
- sprint with stamina;
- jump;
- ledge grab;
- swimming;
- climbing.

## Combat

Future mechanics:

- light/heavy attack;
- combo chain;
- block;
- parry;
- stamina cost;
- hitbox detection;
- damage types;
- critical hits;
- status effects: burn, freeze, poison, stun.

## RPG systems

Future mechanics:

- character stats;
- XP/level up;
- skill tree;
- inventory grid;
- equipment slots;
- loot rarity;
- crafting;
- vendors.

## World and quests

Future mechanics:

- quest system;
- dialogue system;
- fast travel;
- day/night cycle;
- weather;
- map/fog of war;
- save system.

## AI and NPC

Future mechanics:

- patrol AI;
- alert state;
- aggro/return;
- faction system;
- dialogue AI;
- behavior tree/state machine.

## Future Mechanics API shape

Example:

```text
Entity
↓
AbilityComponent
↓
Ability: Dash / Attack / Block / Spell
↓
parameters + animation + VFX + sound + cooldown
```

Dash example fields:

```text
distance
duration
cooldown
staminaCost
iFrameWindow
collisionMode
animationClip
vfxTrail
sound
```

## Rule

Document mechanics early, implement late.

Do not build mechanics before:

- engine runtime foundation;
- asset schema;
- renderer foundation;
- editor core components;
- basic scene/entity model.
