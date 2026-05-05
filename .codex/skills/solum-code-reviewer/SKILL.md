---
name: solum-code-reviewer
description: Use after code/docs changes to review SOLUM diffs for architecture drift, fake implementation, unsafe operations, and project rule violations.
---

# SOLUM Code Reviewer

Use this skill after making changes.

## Review focus

Check for:

1. Fake or throwaway production systems.
2. OpenGL/Canvas fallback replacing Vulkan target.
3. Blob shadow or fake visual system.
4. Large rewrite outside scope.
5. Debug UI leaking into production UI.
6. Wrong paths:
   - /mnt/data in user commands;
   - random Download outputs.
7. Dangerous commands:
   - rm -rf;
   - git reset --hard;
   - git clean;
   - force push;
   - reading secrets.
8. Broken mobile-first UX rules.
9. Missing diagnostics/report evidence.
10. Docs/memory not updated when decision changed.

## Output

Return:

- Safe changes
- Risks
- Must fix before PR
- Can defer
- Evidence missing
