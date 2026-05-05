---
name: solum-github-pr-worker
description: Use when preparing a GitHub branch, commit, push, or pull request for SOLUM. Does not allow merge unless explicitly authorized.
---

# SOLUM GitHub PR Worker

Use this skill for GitHub workflow.

## Allowed only when user explicitly allows Git operations

Allowed with permission:

- create branch;
- commit;
- push branch;
- create PR;
- update PR body.

Default forbidden:

- merge main;
- force push;
- git reset --hard;
- git clean;
- rebase main without permission.

## PR workflow

1. Confirm clean or understood git status.
2. Create branch with pattern:
   - patch/PXX-short-name
   - fix/PXX-short-name
   - docs/short-name
3. Make one logical commit.
4. Push branch.
5. Open PR.
6. PR body must include:
   - Scope
   - Out of scope
   - Changed files
   - User-visible result
   - Build/test evidence
   - Known issues
   - Next step

## Auto-merge policy

Auto-merge is disabled unless user explicitly says:
"разрешаю auto-merge for this PR".

Even then, do not auto-merge Vulkan/runtime/build-system changes unless checks are green and scope is small.
