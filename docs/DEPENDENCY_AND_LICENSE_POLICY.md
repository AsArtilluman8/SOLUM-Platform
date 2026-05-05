# DEPENDENCY_AND_LICENSE_POLICY — external code and license rules

SOLUM will study proven open-source repositories, but must not blindly copy code or add risky dependencies.

## Reference vs dependency

There are different levels of usage:

```text
reference only
small code slice
adapter layer
direct dependency
fork/submodule
```

Default for most repos:

```text
reference only
```

A direct dependency requires explicit decision.

## License check rule

Before copying code or adding a dependency, agent must check license.

Do not add unknown-license code.

Do not add GPL/copyleft dependency without explicit user approval and ADR.

Prefer permissive licenses where possible:

- MIT;
- BSD;
- Apache-2.0;
- zlib;
- public domain where valid.

## Required documentation

If external repo influences code, write a research note:

```text
docs/research/NOTE_XXXX_topic.md
```

If external code becomes dependency, write ADR:

```text
docs/decisions/ADR_XXXX_dependency_name.md
```

ADR must include:

- repo name;
- license;
- why needed;
- build impact on Termux/Android;
- size impact;
- maintenance risk;
- alternative considered;
- integration mode.

## Forbidden

- Copy large external code without license note.
- Add GPL/copyleft dependency casually.
- Import huge framework without Android/Termux build proof.
- Replace SOLUM architecture with external engine.
- Paste code from unknown source.

## The Forge / Filament / bgfx rule

These are primarily architecture references unless explicitly approved as dependency/slice.

Use them to study:

- patterns;
- architecture;
- resource management;
- material model;
- render graph concepts.

Do not blindly import whole frameworks.

## Dependency acceptance checklist

Before accepting dependency:

```text
[ ] License checked
[ ] Android/Termux build path understood
[ ] Size impact acceptable
[ ] No conflict with SOLUM architecture
[ ] Diagnostics/test plan exists
[ ] ADR written
[ ] User approved if risk is high
```
