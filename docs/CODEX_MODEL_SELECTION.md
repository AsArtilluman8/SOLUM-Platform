# Codex Model Selection for SOLUM

This document is mandatory when a chat, planner, or agent prepares a non-trivial Codex task for SOLUM, UDS, Cortex, rendering, assets, VFX, Android builds, or editor work.

The goal is not to use the strongest model for everything. Choose the least expensive configuration that can reliably complete the current atomic phase without creating plausible but wrong engine work.

## Required task header

Every Codex task must begin with:

```text
MODEL DECISION
- Recommended model:
- Reasoning effort:
- Why this configuration:
- Cheaper acceptable fallback:
- When to switch/restart with another model:
- Subagents/Ultra:
- Android/usage risk:
```

If available model names change, preserve the capability mapping rather than inventing a model that is not available to the user.

## Selection criteria

Choose by:

- ambiguity of the target and implementation path;
- number of systems involved;
- research and source-reading burden;
- cost of a convincing mistake;
- maturity of the architecture/contract;
- amount of deterministic repetition;
- Android memory, thermal, process, and usage limits.

## Default mapping

### Sol high/xhigh

Use for:

- architecture and subsystem boundaries;
- unknown Unreal Engine package layouts or binary parsing;
- renderer truth, frame telemetry, GPU/pass instrumentation;
- difficult root-cause analysis;
- multi-system UDS/UDW reconstruction;
- material/MPC/Niagara/audio/runtime reasoning in one phase;
- final high-risk verification of measurements, provenance, or gates.

Use `xhigh` only when the phase is genuinely open-ended, cross-system, or costly to get wrong. Use `high` for difficult but bounded analysis.

### Terra medium/high

Use for:

- implementation from a reviewed architecture and mature acceptance contract;
- ordinary refactoring and integration;
- Android/Gradle/build repair with clear evidence;
- UI wiring and editor functionality from an approved design;
- adding tests, adapters, serializers, or tools whose behavior is already defined.

Use `high` for significant code changes or debugging. Use `medium` for routine bounded work.

### Luna low/medium

Use for:

- deterministic extraction into a fixed schema;
- classification using explicit rules;
- repetitive fixtures and conversions;
- manifest/report/table generation;
- formatting and bounded documentation updates;
- bulk validation with machine-defined expected output.

Do not use Luna as the only architect or final reviewer for high-risk engine work.

## Recommended phase routing

```text
research / architecture     -> Sol high or xhigh
bounded implementation      -> Terra high or medium
bulk deterministic work     -> Luna medium
final verification / audit  -> Sol high or xhigh
```

Prefer one model per atomic phase. A clean phase boundary should include:

- an understood worktree;
- a work-log/checkpoint;
- fresh tests/evidence;
- one logical commit when commits are authorized;
- exact blockers and the next objective.

## Can Codex switch its own model?

Do not assume it can.

Interactive commands such as `/model`, `/status`, and `/ps` belong to the Codex client/TUI. The running model should not be treated as able to press those controls itself.

A Codex process can technically launch another Codex process through the shell, but that creates a separate agent and context. On Android this can:

- consume additional usage;
- race on the same files;
- duplicate work;
- increase memory and thermal pressure;
- trigger process termination such as signal 9.

Default SOLUM policy:

- one agent;
- one atomic phase;
- manual model selection at phase start;
- no nested Codex processes;
- no Ultra/subagents for memory-heavy parsing/builds unless explicitly approved;
- frequent checkpoints during long work.

## SOLUM examples

| Work | Starting recommendation |
|---|---|
| P62 UDS/UDW system truth | Sol xhigh |
| UE source/layout research | Sol xhigh |
| Honest Filament telemetry architecture | Sol xhigh |
| Implementing an approved telemetry contract | Terra high |
| Bulk UDS dependency/resource classification | Luna medium or Terra medium |
| SLPK schema architecture | Sol high/xhigh |
| Implementing stable SLPK tooling | Terra high |
| Cluster renderer research and benchmark design | Sol xhigh |
| Implementing a proven cluster/LOD algorithm | Terra high |
| VFX architecture and Niagara mapping | Sol xhigh |
| VFX module implementation from an approved IR | Terra high |
| Final review of FPS claims, parser truth, or provenance | Sol xhigh |
| Mobile editor UI from approved UX rules | Terra high |

## Evidence rule

Model choice never lowers the evidence standard.

Never claim:

- FPS or frame time;
- GPU/pass cost;
- build success;
- renderer behavior;
- parser compatibility;
- extracted asset truth;
- test completion;
- visual parity;

without fresh verification appropriate to the claim.
