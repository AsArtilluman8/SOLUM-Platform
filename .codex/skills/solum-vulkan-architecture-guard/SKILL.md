---
name: solum-vulkan-architecture-guard
description: Use for Vulkan, rendering, materials, shadows, diagnostics, engine architecture, and runtime direction checks.
skillPackVersion: v1
---

# SOLUM Vulkan Architecture Guard

Use this skill for render/engine decisions.

## Final target

SOLUM Engine target is Android Vulkan AAA-like mobile renderer.

## Allowed simplification

MVP can be incomplete but not incorrect.

Allowed:

- small Vulkan foundation;
- triangle/clear screen as first Vulkan proof;
- minimal material schema designed to expand;
- diagnostics-first approach.

Forbidden:

- OpenGL production path instead of Vulkan target;
- Canvas/bitmap fake renderer;
- blob shadows as production shadow system;
- low-poly target drift;
- hiding Vulkan errors;
- random MainActivity god-object growth;
- resource lifetime chaos.

## For complex render work

Require research gate:

- proven repo/docs references;
- Android/Vulkan constraints;
- Mali/mobile constraints;
- resource lifetime plan;
- diagnostics plan.

## Review questions

1. Does this preserve Vulkan target?
2. Is it extendable without deletion?
3. Are resources owned by correct layer/thread?
4. Is diagnostics evidence planned?
5. Is performance risk noted?
