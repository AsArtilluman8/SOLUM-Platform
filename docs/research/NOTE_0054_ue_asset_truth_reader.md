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

P55 validation used the official UE 5.5 serialization implementations for `FPackageTrailer`, `FMeshDescription`, `FMeshElementContainer`, and mesh attribute arrays as the format authority. The Nebula Sphere editor payload was matched by its 20-byte trailer IoHash, bounded and Oodle-decoded, then consumed exactly with no trailing bytes. The exported GLB contains 3,840 vertex instances and 1,280 triangles; all indices resolve, all normals are unit length, all triangle windings agree with serialized normals, and every GLB chunk/view stays in bounds. Serialized tangents in this fixture are zero vectors and are therefore honestly omitted rather than synthesized.

## Known limits
Verified GLB currently covers UE5 editor-domain `FMeshDescription` stored locally in a v0-v2 package trailer. Cooked `FStaticMeshLODResources`, legacy `FByteBulkData`, unbounded mesh attributes, IoStore/Zen and virtualized/remote payloads remain separate versioned paths. MetaSound/Niagara executable semantics are represented as data/graph contracts, not invented source code.

## Test plan
- `python3 -m compileall -q tools/ue_asset_tool/src`
- CLI help and malformed/truncated input rejection.
- Private fixture verification outside Git.
