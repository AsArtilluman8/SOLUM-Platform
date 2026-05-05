# UX-0005: Bad mobile editor UI / button soup

## Problem

Previous UI attempts often replaced real mobile editor tools with technical controls:

- buttons `+X -X +Y -Y +Z -Z` instead of gizmo;
- large permanent panels covering viewport;
- debug buttons always visible;
- too many parameters at once;
- live preview not updating;
- controls duplicated in multiple places.

## Why bad

- user fights UI instead of creating;
- viewport becomes secondary;
- phone screen is too small for desktop-style panels;
- touch precision becomes painful;
- tool feels like debug panel, not professional editor.

## Rule

SOLUM tools must follow:

```text
smart auto → direct visual control → compact precision → advanced override
```

## Correct direction

- Viewport remains central.
- Bottom sheet instead of wide permanent left panel.
- Context toolbar appears only when needed.
- Gizmo on object for transform.
- Compact scrub controls for numbers.
- Advanced hidden until requested.
- Debug is a separate mode.

## Forbidden

- permanent wide left panel on phone portrait;
- more than 5 bottom tabs;
- debug buttons in production UI;
- node graph in v1 tool;
- important buttons without labels;
- color-only states;
- tiny touch targets.
