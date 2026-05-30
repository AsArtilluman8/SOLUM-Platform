# SOLUM Patch Workflow

Before implementing a patch:

1. Read tasks/CODEX_RULES.md.
2. Read the specific task file under tasks/patches/.
3. Stay inside the task scope.
4. Do not improvise new product direction.
5. Implement build-safe changes.
6. Run git diff --check.
7. Run the full build command.
8. Copy APK to Download.
9. Commit and push the branch.
10. Final report must include:
   - files changed
   - what was implemented
   - what is supported
   - what is not_exposed/deferred
   - build result
   - APK path
   - branch
   - commit SHA
   - known limitations
