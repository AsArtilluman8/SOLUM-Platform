# Research Summary — P54B: UE Asset Truth Reader

## Problem
SOLUM needs an honest offline path from classic Unreal packages to inspectable data without requiring UE on Android/Termux.

## References checked
- Epic Unreal package/version structures — serialization model reference.
- CUE4Parse — class/version coverage reference only; not copied or added as a dependency.
- UE FCompressedBuffer layout — bounded payload validation model.
- Existing SOLUM asset schema, UDS audit, private-asset and dependency policies.

## Decision
Use a dependency-free Python SMALL_SLICE plus ADAPTER boundary for codecs. Package tables, tagged properties, Blueprint pins and Niagara curve data are decoded only when structurally verified. Unknown native serialization retains raw provenance. Oodle is an external helper boundary.

The GPL `powzix/ooz` implementation used during private validation is not vendored into this Apache-2.0 repository. Adding a bundled decoder later requires explicit license ADR.

## Evidence
Private local validation recovered a real 2048x2048 PNG and PCM WAV, reconstructed a large Blueprint graph, decoded Niagara rich-curve keys, and rejected a physically truncated package. No paid assets or derived full reports are committed.

## Known limits
Arbitrary StaticMesh-to-GLB conversion is not claimed. It requires exact per-version native serialization (and mappings for unversioned packages). MetaSound/Niagara executable semantics are represented as data/graph contracts, not invented source code.

## Test plan
- `python3 -m compileall -q tools/ue_asset_tool/src`
- CLI help and malformed/truncated input rejection.
- Private fixture verification outside Git.
