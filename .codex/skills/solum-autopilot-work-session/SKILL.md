---
name: solum-autopilot-work-session
description: Use for longer autonomous SOLUM work sessions while the user is away. Keeps scope bounded and produces report/PR-ready output.
skillPackVersion: v1
---

# SOLUM Autopilot Work Session

Use this when the user wants Codex to work longer with minimal interruptions.

## Session goal

Complete as much as safely possible inside the approved scope.

## Default mode

supervised-autopilot

Allowed:

- read project docs;
- PRE-PATCH CHECK;
- make bounded edits;
- run runner;
- fix loop up to 3 times;
- review diff;
- write report;
- prepare branch/commit/PR only if user explicitly allowed GitHub PR mode.

Forbidden by default:

- merge main;
- force push;
- reset/clean;
- delete big folders;
- install packages;
- read secrets;
- change roadmap silently;
- start unrelated large systems.

## Work cycle

1. Read required docs.
2. Print PRE-PATCH CHECK.
3. Plan steps.
4. Make edits.
5. Run runner.
6. If failed, debug with solum-systematic-debugging.
7. Review with solum-code-reviewer.
8. Verify with solum-verification-before-completion.
9. Write final RESULT.

## Stop conditions

Stop if:

- command requires secrets;
- package install is needed;
- scope would expand;
- more than 3 fix cycles failed;
- build/runtime failure needs user device interaction;
- destructive git operation is required.
