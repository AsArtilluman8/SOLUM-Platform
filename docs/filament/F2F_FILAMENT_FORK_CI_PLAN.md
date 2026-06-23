# F2F Filament Fork CI Build Proof Plan

## 1. Summary

This is a docs-only plan for proving that a separate Filament fork can build the Android artifacts needed by SOLUM.

The plan does not modify SOLUM rendering, Android app code, Gradle build scripts, gameplay, UI, sky, UDS, VFX, or materials. It also does not replace the current Maven dependencies in SOLUM.

Recommended target:

- repository: `AsArtilluman8/filament-solum`, preferably a GitHub fork of `google/filament`;
- source version: exactly `google/filament` tag `v1.71.4`;
- CI platform: GitHub Actions on `ubuntu-24.04`;
- first proof scope: Android `arm64-v8a` release native build plus Android AAR build with `com.google.android.filament.abis=arm64-v8a`;
- artifacts: AARs, Android `.so` files, host tools, and logs.

This is a build proof. It is not an integration patch.

## 2. Current Solum Filament version

Current SOLUM Maven dependencies remain unchanged:

```gradle
com.google.android.filament:filament-android:1.71.4
com.google.android.filament:gltfio-android:1.71.4
com.google.android.filament:filament-utils-android:1.71.4
```

The fork CI must use the same upstream version:

```text
google/filament tag v1.71.4
```

The CI must verify the checked-out tag before building:

```bash
git describe --tags --exact-match
test "$(git describe --tags --exact-match)" = "v1.71.4"
```

## 3. Why local phone build is unsafe

Do not build full Filament locally on the phone.

Reasons:

- the shallow local source clone is already about 1.7 GB;
- free storage after clone is only about 8 GB;
- Filament Android builds generate large native build trees under `out/`;
- AAR packaging can require compiled native libraries and host tools;
- failed local builds can leave large partial build directories;
- phone thermal throttling and memory pressure can make failures noisy and hard to diagnose.

The phone should keep using SOLUM Maven dependencies until a fork artifact has CI evidence.

## 4. Recommended separate fork/repo strategy

Use a separate repository. Do not put Filament source inside `SOLUM-Platform`.

Recommended repository:

```text
AsArtilluman8/filament-solum
```

Recommended source model:

```text
GitHub fork of google/filament
```

Recommended branch/tag workflow:

```text
fork google/filament
checkout tag v1.71.4 in CI
build proof artifacts in GitHub Actions
upload artifacts and logs
do not publish to Maven yet
do not consume artifacts from SOLUM yet
```

Recommended repository layout:

```text
filament-solum/
  .github/
    workflows/
      build-filament-v1714.yml
  build/
  android/
  filament/
  libs/
  tools/
  third_party/
  ...
```

Optional later additions after the first proof:

```text
filament-solum/
  solum-ci/
    README.md
    collect-artifacts.sh
    notes/
      v1.71.4-first-proof.md
```

Keep SOLUM-specific CI notes small. Do not add SOLUM app code or SOLUM Gradle files to the Filament fork.

## 5. Full GitHub Actions YAML proposal

Workflow path:

```text
.github/workflows/build-filament-v1714.yml
```

Full proposed YAML:

```yaml
name: Build Filament v1.71.4 Android proof

on:
  workflow_dispatch:
  push:
    branches:
      - solum-ci-v1.71.4
    paths:
      - ".github/workflows/build-filament-v1714.yml"

permissions:
  contents: read

concurrency:
  group: filament-v1714-android-proof-${{ github.ref }}
  cancel-in-progress: true

jobs:
  android-arm64-proof:
    name: Android arm64-v8a proof
    runs-on: ubuntu-24.04
    timeout-minutes: 180

    env:
      FILAMENT_TAG: v1.71.4
      ANDROID_ABI: arm64-v8a
      ANDROID_API_LEVEL: "34"
      ANDROID_BUILD_TOOLS: "35.0.0"
      ANDROID_NDK_VERSION: "29.0.14206865"
      CMAKE_VERSION: "3.22.1"
      FILAMENT_DIST_DIR: ${{ github.workspace }}/out/android-release/filament
      FILAMENT_HOST_TOOLS_DIR: ${{ github.workspace }}/out/release/filament
      ARTIFACT_ROOT: ${{ github.workspace }}/_solum_filament_artifacts

    steps:
      - name: Checkout fork
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          submodules: false

      - name: Verify exact upstream tag
        shell: bash
        run: |
          set -euxo pipefail
          git fetch --force --tags origin "${FILAMENT_TAG}"
          git checkout --detach "${FILAMENT_TAG}"
          test "$(git describe --tags --exact-match)" = "${FILAMENT_TAG}"
          git rev-parse HEAD | tee filament-commit.txt
          git submodule update --init --recursive

      - name: Install Linux build dependencies
        shell: bash
        run: |
          set -euxo pipefail
          sudo apt-get update
          sudo apt-get install -y \
            clang-17 \
            libc++-17-dev \
            libc++abi-17-dev \
            libglu1-mesa-dev \
            libxi-dev \
            libxcomposite-dev \
            libxxf86vm-dev \
            ninja-build \
            zip \
            unzip \
            tree

      - name: Select Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android SDK packages
        shell: bash
        run: |
          set -euxo pipefail
          yes | sdkmanager --licenses >/dev/null
          sdkmanager \
            "platforms;android-${ANDROID_API_LEVEL}" \
            "build-tools;${ANDROID_BUILD_TOOLS}" \
            "ndk;${ANDROID_NDK_VERSION}" \
            "cmake;${CMAKE_VERSION}"
          echo "ANDROID_HOME=${ANDROID_HOME}" | tee android-env.txt
          echo "ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-${ANDROID_HOME}}" | tee -a android-env.txt
          echo "ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}" | tee -a android-env.txt

      - name: Tool versions
        shell: bash
        run: |
          set -euxo pipefail
          java -version 2>&1 | tee java-version.txt
          clang-17 --version | tee clang-version.txt
          cmake --version | tee cmake-version.txt
          ninja --version | tee ninja-version.txt
          ./build.sh -h | tee build-sh-help.txt

      - name: Build Android native libraries and required host tools
        shell: bash
        run: |
          set -euxo pipefail
          export CC=/usr/bin/clang-17
          export CXX=/usr/bin/clang++-17
          export ANDROID_HOME="${ANDROID_HOME}"
          export ANDROID_SDK_ROOT="${ANDROID_HOME}"
          ./build.sh -p android -q "${ANDROID_ABI}" release 2>&1 | tee build-android-arm64-release.log
          test -d "${FILAMENT_DIST_DIR}/lib/${ANDROID_ABI}"
          find "${FILAMENT_DIST_DIR}/lib/${ANDROID_ABI}" -maxdepth 1 -type f | sort | tee android-arm64-native-files.txt

      - name: Build Android AARs for arm64-v8a
        shell: bash
        working-directory: android
        run: |
          set -euxo pipefail
          ./gradlew \
            --no-daemon \
            -Pcom.google.android.filament.dist-dir="${FILAMENT_DIST_DIR}" \
            -Pcom.google.android.filament.tools-dir="${FILAMENT_HOST_TOOLS_DIR}" \
            -Pcom.google.android.filament.abis="${ANDROID_ABI}" \
            -Pcom.google.android.filament.skip-samples \
            :filament-android:assembleRelease \
            :gltfio-android:assembleRelease \
            :filament-utils-android:assembleRelease \
            :filamat-android:assembleRelease \
            2>&1 | tee ../gradle-aar-arm64-release.log

      - name: Collect artifacts
        if: always()
        shell: bash
        run: |
          set -euxo pipefail
          mkdir -p "${ARTIFACT_ROOT}/aars" \
                   "${ARTIFACT_ROOT}/native-libs/${ANDROID_ABI}" \
                   "${ARTIFACT_ROOT}/host-tools" \
                   "${ARTIFACT_ROOT}/logs" \
                   "${ARTIFACT_ROOT}/metadata"

          find android -path "*/build/outputs/aar/*.aar" -type f -print -exec cp -v {} "${ARTIFACT_ROOT}/aars/" \; | tee aar-files.txt || true
          find "${FILAMENT_DIST_DIR}/lib/${ANDROID_ABI}" -maxdepth 1 -type f -name "*.so" -print -exec cp -v {} "${ARTIFACT_ROOT}/native-libs/${ANDROID_ABI}/" \; | tee native-so-files.txt || true

          for tool in matc cmgen mipgen filamesh resgen uberz; do
            if [ -x "${FILAMENT_HOST_TOOLS_DIR}/bin/${tool}" ]; then
              cp -v "${FILAMENT_HOST_TOOLS_DIR}/bin/${tool}" "${ARTIFACT_ROOT}/host-tools/"
            fi
          done

          cp -v filament-commit.txt android-env.txt java-version.txt clang-version.txt cmake-version.txt ninja-version.txt build-sh-help.txt "${ARTIFACT_ROOT}/metadata/" || true
          cp -v build-android-arm64-release.log gradle-aar-arm64-release.log android-arm64-native-files.txt aar-files.txt native-so-files.txt "${ARTIFACT_ROOT}/logs/" || true
          tree "${ARTIFACT_ROOT}" | tee "${ARTIFACT_ROOT}/metadata/artifact-tree.txt"

      - name: Check required artifacts
        shell: bash
        run: |
          set -euxo pipefail
          test -f "${ARTIFACT_ROOT}/aars/filament-android-release.aar"
          test -f "${ARTIFACT_ROOT}/aars/gltfio-android-release.aar"
          test -f "${ARTIFACT_ROOT}/aars/filament-utils-android-release.aar"
          test -f "${ARTIFACT_ROOT}/aars/filamat-android-release.aar"
          test -f "${ARTIFACT_ROOT}/native-libs/${ANDROID_ABI}/libfilament-jni.so"
          test -f "${ARTIFACT_ROOT}/native-libs/${ANDROID_ABI}/libgltfio-jni.so"
          test -f "${ARTIFACT_ROOT}/native-libs/${ANDROID_ABI}/libfilament-utils-jni.so"
          test -f "${ARTIFACT_ROOT}/host-tools/matc"

      - name: Upload artifact bundle
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: filament-v1.71.4-android-arm64-proof
          path: _solum_filament_artifacts/
          if-no-files-found: warn
          retention-days: 14
```

Exact commands used inside CI:

```bash
git fetch --force --tags origin v1.71.4
git checkout --detach v1.71.4
test "$(git describe --tags --exact-match)" = "v1.71.4"
git submodule update --init --recursive

sudo apt-get update
sudo apt-get install -y clang-17 libc++-17-dev libc++abi-17-dev libglu1-mesa-dev libxi-dev libxcomposite-dev libxxf86vm-dev ninja-build zip unzip tree

yes | sdkmanager --licenses >/dev/null
sdkmanager "platforms;android-34" "build-tools;35.0.0" "ndk;29.0.14206865" "cmake;3.22.1"

export CC=/usr/bin/clang-17
export CXX=/usr/bin/clang++-17
./build.sh -p android -q arm64-v8a release

cd android
./gradlew --no-daemon \
  -Pcom.google.android.filament.dist-dir="$GITHUB_WORKSPACE/out/android-release/filament" \
  -Pcom.google.android.filament.tools-dir="$GITHUB_WORKSPACE/out/release/filament" \
  -Pcom.google.android.filament.abis=arm64-v8a \
  -Pcom.google.android.filament.skip-samples \
  :filament-android:assembleRelease \
  :gltfio-android:assembleRelease \
  :filament-utils-android:assembleRelease \
  :filamat-android:assembleRelease
```

Notes:

- Official Filament `BUILDING.md` says Android builds need host tools and native libraries before AAR packaging.
- Official `android/build.gradle` for `v1.71.4` supports `com.google.android.filament.abis`, so the first proof should try `arm64-v8a` only.
- If arm64-only AAR packaging fails because the AAR build expects all ABIs, do not switch blindly to all ABIs more than once. Capture logs and stop.

## 6. Artifact list

Expected uploaded GitHub Actions artifact:

```text
filament-v1.71.4-android-arm64-proof
```

Expected artifact bundle layout:

```text
_solum_filament_artifacts/
  aars/
    filament-android-release.aar
    gltfio-android-release.aar
    filament-utils-android-release.aar
    filamat-android-release.aar
  native-libs/
    arm64-v8a/
      libfilament-jni.so
      libgltfio-jni.so
      libfilament-utils-jni.so
      other required .so files
  host-tools/
    matc
    cmgen
    mipgen
    filamesh
    resgen
    uberz
  logs/
    build-android-arm64-release.log
    gradle-aar-arm64-release.log
    android-arm64-native-files.txt
    aar-files.txt
    native-so-files.txt
  metadata/
    artifact-tree.txt
    filament-commit.txt
    android-env.txt
    java-version.txt
    clang-version.txt
    cmake-version.txt
    ninja-version.txt
    build-sh-help.txt
```

Minimum success artifact set:

```text
filament-android-release.aar
gltfio-android-release.aar
filament-utils-android-release.aar
libfilament-jni.so
libgltfio-jni.so
libfilament-utils-jni.so
matc
build logs
artifact tree
```

If the full artifact set is too heavy, reduce scope in this order:

1. Android `arm64-v8a` AAR/native libs.
2. `matc`.
3. Other host tools.

## 7. Success gates

The CI proof is successful only if all gates pass:

- checkout is exactly tag `v1.71.4`;
- submodules initialize successfully;
- Android SDK, NDK, CMake, Java, clang, and ninja versions are printed into logs;
- `./build.sh -p android -q arm64-v8a release` exits with success;
- `out/android-release/filament/lib/arm64-v8a` exists;
- AAR build exits with success;
- required AARs are uploaded;
- required JNI `.so` files are uploaded;
- `matc` is uploaded, or the log clearly explains why host tools were not produced;
- artifact tree is uploaded;
- full build logs are uploaded;
- no SOLUM app code or build script is changed.

## 8. Failure gates

Stop and report instead of continuing if any gate is hit:

- workflow cannot checkout exact tag `v1.71.4`;
- upstream submodules fail in a way that cannot be identified from logs;
- Android build requires huge unrelated platform builds outside the requested scope;
- arm64-only artifacts cannot be clearly identified;
- AAR filenames or locations cannot be clearly identified;
- Gradle tries to build samples despite `com.google.android.filament.skip-samples`;
- host tools are missing and `matc` cannot be found;
- CI needs more than 2 blind attempts;
- all-ABI AAR packaging becomes necessary and the build time or artifact size looks unreasonable;
- any proposed fix requires modifying SOLUM app rendering or SOLUM Android code.

Expected failure points:

- missing or mismatched Android NDK version;
- Gradle plugin requiring SDK package versions not installed by `sdkmanager`;
- CMake version mismatch between Gradle and installed SDK CMake;
- arm64-only AAR packaging not matching Filament's documented universal AAR expectation;
- host tool output path differs from `out/release/filament/bin`;
- artifact names differ from the expected `*-release.aar` pattern;
- CI timeout during native build;
- `mipgen` may not exist in the Android-required host tool set for this build path.

Allowed first response to a failure:

```text
Read the uploaded logs.
Identify the first real failure.
Make one scoped workflow adjustment.
Retry once.
```

Allowed second response:

```text
If the second blind attempt fails, stop and write a failure report.
Do not keep guessing.
```

## 9. Next step after success

After successful artifact build:

1. Download the GitHub Actions artifact bundle.
2. Record the exact upstream tag, commit SHA, workflow run URL, and artifact tree in a SOLUM docs note or ADR.
3. Compare the produced AAR names and native `.so` contents against current Maven `1.71.4` dependency expectations.
4. Decide whether SOLUM should keep Maven dependencies or test a local Maven repository/artifact override in a separate branch.
5. If testing fork artifacts later, make a separate integration plan with rollback:
   - no rendering behavior changes;
   - no sky/UDS/VFX/material/gameplay changes;
   - dependency switch only;
   - build proof first;
   - runtime diagnostics after APK build;
   - revert path documented.

Do not change SOLUM dependencies until the fork CI artifact has evidence and the user explicitly approves an integration patch.

## 10. Explicit list of things not to modify

Do not modify:

- SOLUM app rendering;
- sky;
- UDS;
- VFX;
- materials;
- gameplay;
- UI;
- APK build scripts;
- Android app code;
- current SOLUM Maven Filament dependencies;
- SOLUM Gradle dependency declarations;
- SOLUM native renderer code;
- SOLUM diagnostics runtime code;
- SOLUM asset pipeline code;
- SOLUM roadmap silently;
- any file outside `SOLUM-Platform` for this docs task.

Do not:

- import full Filament source into `SOLUM-Platform`;
- build full Filament locally on the phone;
- publish fork artifacts to Maven Central;
- commit or push from SOLUM without explicit user approval;
- claim fork artifacts are integration-ready without CI logs and artifact tree.
