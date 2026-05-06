---
name: solum-build-fixer
description: Use for Termux, Ubuntu proot, Android Gradle, APK build, SDK/NDK/aapt2, and SOLUM build runner issues.
skillPackVersion: v1
---

# SOLUM Build Fixer

Use this skill for build problems.

## Device assumptions

- Development happens on Android phone through Termux and Ubuntu proot.
- Do not use /mnt/data in user commands.
- Android SDK may be at:
  - $HOME/android-sdk
  - /data/data/com.termux/files/home/android-sdk
- Termux aapt2 may be:
  - /data/data/com.termux/files/usr/bin/aapt2
- logcat route is unreliable on this device. Prefer app-written diagnostics files.

## Build process

1. Run only:
   - bash tools/agent_build_runner.sh
2. Read:
   - _work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG_SHORT.txt
   - _work/agent_reports/latest/SOLUM_AGENT_BUILD_LOG.txt
3. Detect:
   - missing Gradle skeleton;
   - invalid Gradle root;
   - empty Android SDK env;
   - missing aapt2;
   - Kotlin error;
   - NDK/C++ error;
   - shader/compiler error.
4. Fix the smallest cause.
5. Run runner again, maximum 3 cycles.

## Output policy

Use SOLUMCreative paths. Do not spam Download.

## Forbidden

- Do not install packages without explicit permission.
- Do not auto-install APK by default.
- Do not run random Gradle commands if runner exists.
- Do not use rm -rf.
