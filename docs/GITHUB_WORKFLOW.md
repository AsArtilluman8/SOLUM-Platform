# GITHUB_WORKFLOW — branch, commit, PR rules

GitHub is project memory and code source of truth.

## Default workflow for large changes

For large docs/code patches:

```text
create branch
↓
create one batch commit
↓
open PR
↓
review diff
↓
merge after approval
```

Do not create many separate commits/files directly in main for one logical patch.

## Branch naming

```text
patch/P02-diagnostics-vulkan-caps
patch/P03-asset-schema
patch/P04-vulkan-foundation
fix/P02-build-log-parser
research/csm-reference-plan
```

## Commit naming

```text
P02: add diagnostics and Vulkan capability check
P03: add asset schema and validator foundation
Fix P02: correct Termux aapt2 detection
Docs: update UX panel state rules
```

## PR body template

```markdown
# Patch PXX — Title

## Scope
What this PR changes.

## Out of scope
What this PR intentionally does not change.

## Changed modules/files
- ...

## User-visible result
What user should see.

## Build/test evidence
- build log path
- diagnostics path
- runtime proof if available

## Known issues
- ...

## Next step
- ...
```

## When direct update is allowed

Direct `main` file update is allowed only for:

- typo;
- one small docs clarification;
- urgent rule fix;
- repository bootstrapping if repo is empty and user approves.

## When PR is required

PR is required for:

- multiple files;
- code changes;
- build system changes;
- docs foundation updates;
- architecture decisions;
- diagnostics changes;
- any patch that affects future workflow.

## Review checklist

Before merge:

```text
[ ] scope is clear
[ ] no throwaway/fake system
[ ] docs updated if decision changed
[ ] diagnostics/build evidence included if runtime/build change
[ ] user-visible result described
[ ] known issues listed
[ ] next step listed
```

## Patch complete definition

A patch is complete only when:

- implementation exists;
- build status is known;
- runtime status is known if applicable;
- diagnostics/report path is provided if applicable;
- docs/memory updated;
- known issues documented.
