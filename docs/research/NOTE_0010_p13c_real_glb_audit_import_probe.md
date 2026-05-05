# NOTE-0010: P13C Real GLB Audit + Import Probe

## Decision

Do not implement fake materials. Add a real GLB/glTF audit stage first.

## Why

The user supplied real assets with different complexity levels:

- character armor: skinning, alpha mask, normal maps, tangents;
- PBR toy car: multiple KHR material extensions.

A baseColor-only shader would be misleading and would not match author intent.

## P13C scope

- Add `tools/gltf_glb_audit.py`.
- Search imported model folders.
- Parse GLB JSON chunk and glTF JSON.
- Extract mesh/material/texture/extension state.
- Write runtime model/material/texture diagnostics.
- Keep current renderer unchanged.

## Next

P13D should add native `cgltf` import skeleton or mesh upload from imported GLB. PBR rendering comes later after texture decode/upload and material binding are defined.
