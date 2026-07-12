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

P56 replaced the last mesh-payload heuristic with the UE 5.5 `FEditorBulkData::Serialize` layout: serialized flags, unique bulk GUID, 20-byte content IoHash, signed 64-bit payload size and the conditional legacy offset. `FMeshDescriptionBulkData` additionally consumes its GUID and serialized boolean, for exactly 68 bytes in the fixture. The hash-derived mesh GUID, trailer identifier, trailer raw size and `FCompressedBuffer` raw hash all agree before geometry is emitted.

The complete P56 UDS Blueprint inputs also removed the earlier truncation blocker. Sky (13,298 exports), Weather (8,045) and Configuration Manager (1,278) reach physical EOF with no missing export ranges. Their K2 contracts decode all 20,222 node pin streams across 954 graphs. The validator found zero duplicate pin IDs, owner mismatches, dangling targets, asymmetric links or self-links. Remaining class-native tails keep per-export `RAW_VERIFIED` provenance and are not represented as decompiled C++.

P57 follows the supplied UE 5.5 `UStruct::Serialize`, `FStructScriptLoader`, `FPropertyProxyArchive`, field serializers, `ScriptSerialization.h` and `UFunction::Serialize` implementations. Export-map `ScriptSerializationStartOffset/EndOffset` are correctly treated as tagged-property bounds, not bytecode offsets. The decoder instead consumes `SuperStruct`, child references, every serialized FProperty (including nested container fields), the declared bytecode/storage sizes, recursive EX_* expressions and the complete UFunction footer. Configuration Manager verifies 34/34 functions and 5,286 expressions; Sky 538/538 and 61,297; Weather 419/419 and 37,793. All 991 functions consume exactly 892,870 serialized script bytes into exactly 622,166 VM bytes and finish with `EX_EndOfScript`.

P57 also binds media output to owning UE serialization rather than relying only on container discovery. Cloud Wisps consumes both strip-flag records and the exact 56-byte FEditorBulkData record, then matches payload offset, raw size and content ID to the Oodle FCompressedBuffer before validating the 2048x2048 grayscale PNG. Dust_2 consumes the exact 20-byte FByteBulkData header plus SoundWave GUID/footer, resolves its payload through `BulkDataStartOffset`, and cross-checks WAV channels, sample rate, sample frames and duration. UDS_Close_Thunder contains no embedded waveform; its MetaSound contract now records the six external SoundWave imports required for audio extraction.

Niagara P57 adds exact current-version native serializers for `FNiagaraVariable`, `FNiagaraVariableWithOffset`, map/set delta records and arrays of tagged script structs. Dripping Curve now exposes all three emitter handles (`Drips`, `Collision_Cache`, `Splashes`) and all 19 user parameters. Scalar defaults are decoded from the 52-byte ParameterData buffer by declared type/offset, while object and data-interface parameters resolve through their serialized arrays. The separate graph contract still verifies 273 nodes, 1,051 pins and 371 reciprocal links.

P58 follows the UE 5.5 native `FExpressionInput` and material-input layouts instead of interpreting them as generic tagged structs. `FExpressionInput` consumes its exact package index, output index, input name and five component-mask fields; color, scalar, vector and shading-model inputs consume their exact constant/use-constant suffixes. A legacy complete inner `FPropertyTag` path also decodes pre-UE5 struct arrays without guessing their element boundaries. Across 18 Material Functions and two Materials this reconstructs 971 expression objects, 970 serialized links, 115 parameters and 34 function calls. All package-index targets, outputs, masks and expression-collection memberships validate. Material shader-resource native tails remain raw; the output is explicitly a graph contract, not HLSL.

The same legacy array-tag work makes five P58 Curve assets exact rather than raw. Their 14 `FRichCurve` channels contain 115 fixed 27-byte keys. Each contract preserves interpolation/tangent/weight modes, time/value/tangents, source offsets and hashes, and checks that all numeric fields are finite and times are sorted.

P58 also exercises TextureSource transform data that was not present earlier. Two v2-trailer Texture2D assets use `TSCF_UEDELTA` over one BGRA8 mip/slice. The bounded inverse applies the UE vertical byte predictor with its 32-row reset, then requires the reconstructed size and every channel's min/max to agree with the serialized `LayerColorInfo` before emitting PNG. The verified outputs are 16384×128 and 4096×64. This is intentionally not generalized to other formats, mip layouts or slice counts without fixtures.

Regression coverage in the same archive verifies three more editor `FMeshDescription` GLBs (96/408/576 vertex instances), six metadata-matched stereo PCM16 SoundWave files, nine additional Blueprint-class packages and the existing Dripping Curve Niagara contract. The two VolumeTexture fixtures are retained as unsupported because flattening a volume to one PNG would not be a faithful representation.

## Known limits
Verified GLB currently covers UE5 editor-domain `FMeshDescription` stored locally in a v0-v2 package trailer. The implemented legacy FByteBulkData reader covers its exact metadata widths and locally addressable payloads, but cooked `FStaticMeshLODResources`, BCn platform texture mips, chunked/streaming audio, IoStore/Zen and virtualized/remote payloads remain separate versioned paths. The UEDELTA inverse is restricted to the verified one-mip/one-slice BGRA8/RGBA8 layout. Material, Blueprint, MetaSound and Niagara executable semantics are represented as data/graph/bytecode contracts, not invented HLSL or C++. The available archives contain no cooked `.uexp/.ubulk/.uptnl` sample, so those cooked paths still require legal companion fixtures for proof.

## Test plan
- `python3 -m compileall -q tools/ue_asset_tool/src tools/ue_asset_tool/tests`
- `PYTHONPATH=tools/ue_asset_tool/src python3 -m unittest discover -s tools/ue_asset_tool/tests -v`
- CLI help and malformed/truncated input rejection.
- Private fixture verification outside Git.
