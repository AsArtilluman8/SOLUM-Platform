# IDEA-0003: Project Memory Palace

## Raw idea

SOLUM needs a persistent memory outside any single chat.

User referenced the idea of a “memory palace”: a structured place where ideas, decisions, errors, patch history and UX pain remain available to future AI chats.

## SOLUM implementation

GitHub docs are the memory palace.

Rooms:

```text
Room 1: Current Stage
Room 2: Architecture Rules
Room 3: Agent Rules
Room 4: UX Negative Cases
Room 5: Decisions / ADR
Room 6: Error Knowledge Base
Room 7: Patch History
Room 8: Ideas
Room 9: Rendering Target
Room 10: Asset Formats
```

## Why valuable

- New chat does not start from zero.
- GPT/Claude/Codex can read project memory.
- Old mistakes are not repeated.
- Decisions have reasons.
- Patch history is traceable.
- Diagnostics point to real device state.

## Required rule

If something can repeat, save it:

- idea;
- error;
- UX pain;
- architecture decision;
- rejected solution;
- patch result;
- diagnostics lesson.

## Status

Accepted.

Implemented through `docs/PROJECT_MEMORY_INDEX.md` and related docs.
