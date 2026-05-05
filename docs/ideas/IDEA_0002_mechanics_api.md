# IDEA-0002: SOLUM Mechanics API

## Raw idea

Game developers repeatedly rebuild the same mechanics: dash, lock-on, attacks, loot, skills, quests, stamina, hit reactions, enemy AI patterns.

SOLUM should eventually provide reusable mechanics modules similar to a small gameplay ability/blueprint-like library.

## Goal

Instead of hardcoding mechanics in Player code, SOLUM should have configurable mechanics:

```text
character
↓
AbilityComponent
↓
DashAbility / AttackCombo / LockOn / SkillCast
↓
parameters + animation + VFX + audio + cooldown + diagnostics
```

## Example mechanic

Dash:

- distance;
- duration;
- cooldown;
- stamina cost;
- invulnerability window;
- collision mode;
- VFX trail;
- animation clip;
- sound.

## Local override

Base mechanic must remain clean.

Game can define:

```text
shadow_dash extends combat.dash
```

and override only local values.

## Why valuable

- reduces repeated work;
- gives consistent UX;
- helps focus on game feel, story, world and atmosphere;
- lets mechanics be tested and profiled.

## Status

Document now, implement later.

Do not build Mechanics API before:

- Engine foundation;
- asset schema;
- render foundation;
- first tools;
- basic gameplay/runtime.
