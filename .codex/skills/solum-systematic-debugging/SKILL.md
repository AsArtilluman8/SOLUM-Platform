---
name: solum-systematic-debugging
description: Use when diagnosing build, runtime, logic, Vulkan, Android, Termux, Gradle, or tool failures in SOLUM.
---

# SOLUM Systematic Debugging

Use this skill when something fails.

## Core rule

Do not guess. Diagnose first.

## Process

1. State the symptom.
2. Find the evidence source:
   - build log;
   - runner short log;
   - diagnostics ZIP;
   - crash/runtime state file;
   - screenshot/report if visual.
3. Extract the first meaningful error.
4. Create 1 to 3 hypotheses.
5. Pick the smallest safe fix.
6. Change only files needed for that hypothesis.
7. Run the allowed runner.
8. If failed, update the hypothesis and repeat up to 3 times.
9. If still failed, stop and report.

## Forbidden

- Do not rewrite large systems to fix one error.
- Do not hide errors.
- Do not say fixed without evidence.
- Do not replace Vulkan target with fake/OpenGL/Canvas path.
- Do not delete files as a debugging shortcut.

## Output

Always report:

- Symptom
- Evidence
- Hypothesis
- Change
- Check result
- Remaining risk
