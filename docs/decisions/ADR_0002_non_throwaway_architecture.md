# ADR-0002: Non-throwaway architecture

## Status

Accepted.

## Decision

SOLUM must not introduce throwaway/fake systems into production path.

MVP can be incomplete, but must not be incorrect.

## Meaning

Allowed:

- minimal version of final system;
- limited parameters;
- one real path before many paths;
- one asset type before all asset types;
- one shadow map before full CSM;
- simple MaterialDocument before full node graph.

Forbidden:

- fake substitute that will be deleted later;
- blob shadows instead of real shadow system;
- OpenGL instead of Vulkan production path;
- low-poly target drift;
- UI buttons instead of a real editor tool;
- hardcoded assets instead of asset manifest.

## Example

Good:

```text
CloudSystem v1 has parameters a,b.
Later it grows to a,b,c,d,e,f.
Same system remains.
```

Bad:

```text
Draw PNG cloud overlay just for testing.
Later delete it.
```

## Consequences

Every patch must answer:

```text
Will this code live in final architecture?
```

If answer is “no, later delete” — patch is rejected unless it is a debug-only experiment outside production path.

## Rejected

- “First make fake simple version, later replace.”
- “Temporary UI buttons instead of planned tool.”
- “Simple demo renderer just to see something.”
