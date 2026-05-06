---
name: solum-verification-before-completion
description: Use before claiming a task is done in SOLUM. Requires evidence, logs, diff, and known issues.
skillPackVersion: v1
---

# SOLUM Verification Before Completion

Use this skill before final answer.

## Completion rule

Never say done, fixed, working, complete, or ready without evidence.

## Required evidence

At least one of:

- build runner result;
- diagnostics/report ZIP;
- runtime state file;
- generated report;
- git diff summary;
- explicit user confirmation.

## Checklist

1. List changed files.
2. Show check command used.
3. Show result:
   - BUILD_SUCCESS;
   - BUILD_FAILED;
   - NO_BUILD_SYSTEM_YET;
   - NO_VALID_GRADLE_BUILD;
   - diagnostics collected;
   - report generated.
4. List output paths.
5. List known issues.
6. State next step.

## Forbidden

- No vague "should work".
- No pretending runtime was tested if only build was tested.
- No fake evidence.
- No hiding failed checks.
