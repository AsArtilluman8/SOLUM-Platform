#!/usr/bin/env python3
"""Fail-closed R22 native InstanceBuffer capability contract."""

from __future__ import annotations

import argparse
import hashlib
import re
import zipfile
from io import BytesIO
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / (
    "third_party/filament-solum/patches/"
    "0015-solum-instance-buffer-android-bridge.patch"
)
WORKFLOW = ROOT / (
    ".github/workflows/compile-filament-perf-v3-instance-buffer-r22.yml"
)
EXACT_FILAMENT_COMMIT = "a0ecdbbeba5f1005bbad0a4c8b2fe6955788cdee"
EXPECTED_PATCH_SHA256 = (
    "4635736b97cc2acba3eb351245d611b4b00c4163cc8e9ea21590da089d5d837f"
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def read(path: Path) -> str:
    require(path.is_file(), f"missing {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def current_aar_status() -> tuple[str, str]:
    gradle = read(ROOT / "apps/engine/build.gradle")
    match = re.search(r"1\.71\.4-ci-perf-r\d+", gradle)
    if not match:
        return "UNKNOWN", "version_not_found"
    version = match.group(0)
    aar = ROOT / (
        "third_party/filament-solum/maven/com/solum/filament/"
        f"filament-android/{version}/filament-android-{version}.aar"
    )
    if not aar.is_file():
        return "UNKNOWN", str(aar.relative_to(ROOT))
    with zipfile.ZipFile(aar) as archive:
        classes = archive.read("classes.jar")
    with zipfile.ZipFile(BytesIO(classes)) as jar:
        names = set(jar.namelist())
        renderable = jar.read(
            "com/google/android/filament/RenderableManager$Builder.class"
        )
    present = (
        "com/google/android/filament/InstanceBuffer.class" in names
        and b"nBuilderInstanceBuffer" in renderable
    )
    return ("PRESENT_R22" if present else "PRESENT_PRE_R22"), version


def validate() -> tuple[str, str]:
    patch = read(PATCH)
    workflow = read(WORKFLOW)
    digest = hashlib.sha256(PATCH.read_bytes()).hexdigest()
    require(digest == EXPECTED_PATCH_SHA256,
            f"R22 patch SHA mismatch: {digest}")
    for token in (
        "class InstanceBuffer",
        "InstanceBuffer::Builder((size_t) instanceCount)",
        "InstanceBuffer_nSetLocalTransforms",
        "instanceBuffer->setLocalTransforms",
        "instanceBuffer->getInstanceCount()",
        "nBuilderInstanceBuffer",
        "builder->instances((size_t) instanceCount, instanceBuffer)",
        "destroyInstanceBuffer",
        "engine->destroy(instanceBuffer)",
        "src/main/cpp/InstanceBuffer.cpp",
        "instanceCount exceeds InstanceBuffer capacity",
    ):
        require(token in patch, f"R22 capability token missing: {token}")
    for forbidden in (
        "renderScale", "dynamicResolution", "instanceCount / 2",
        "oneSided", "flushAndWait", "waitForGpu", "fenceWait",
        "private_premium", "grass", "flower", "cloud",
    ):
        require(forbidden not in patch,
                f"R22 capability is asset/quality coupled: {forbidden}")
    require(EXACT_FILAMENT_COMMIT in workflow,
            "R22 exact Filament commit missing")
    require("0014-solum-perf-v3-mask-aware-r21-correctness.patch" in workflow
            and workflow.index("0014-solum-perf-v3-mask-aware-r21-correctness.patch")
                < workflow.index('"$R22_PATCH"', workflow.index("PATCHES=(")),
            "R22 must compose after the exact R21 stack")
    for token in (
        "apply --recount --check",
        "com.google.android.filament.InstanceBuffer",
        "setLocalTransforms(java.nio.Buffer, int, int)",
        "instances(int, com.google.android.filament.InstanceBuffer)",
        "InstanceBuffer_nSetLocalTransforms",
        "RenderableManager_nBuilderInstanceBuffer",
        "filament-v1714-solum-perf-v3-instance-buffer-r22-arm64",
    ):
        require(token in workflow, f"R22 CI proof missing: {token}")
    return current_aar_status()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.parse_args()
    aar_status, detail = validate()
    print("PERF_V3_INSTANCE_BUFFER_SOURCE_CONTRACT=PASS")
    print(f"patch_sha256={EXPECTED_PATCH_SHA256}")
    print(f"current_aar_status={aar_status}")
    print(f"current_aar_detail={detail}")
    print("runtime_status=" + (
        "R22_ARTIFACT_PRESENT_RUNTIME_INTEGRATION_ALLOWED"
        if aar_status == "PRESENT_R22"
        else "SOURCE_READY_RUNTIME_FAIL_CLOSED_UNTIL_R22_ARTIFACT"
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
