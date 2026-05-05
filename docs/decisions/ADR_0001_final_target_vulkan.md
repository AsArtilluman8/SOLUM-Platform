# ADR-0001: Final rendering target is Vulkan

## Status

Accepted.

## Decision

SOLUM Engine final rendering target is Vulkan on Android.

OpenGL must not be used as a production shortcut or temporary production renderer.

## Why

SOLUM target is AAA-like mobile graphics:

- real lighting;
- real shadows;
- material pipeline;
- post-process;
- diagnostics;
- performance profiling;
- consistent preview between Engine and Studio tools.

OpenGL preview now / Vulkan later is a known trap: temporary preview code tends to remain forever.

## Consequences

- Patch 02 must include Vulkan capability check.
- Patch 04 must create Vulkan Foundation v1.
- Render architecture must use Vulkan concepts from the start.
- No OpenGL fallback in production path.
- Devices without required Vulkan path should show a clear error, not silently switch to fake renderer.

## Rejected

- OpenGL preview first, Vulkan later.
- Canvas/bitmap renderer as production preview.
- Blob shadow/fake renderer as final direction.

## Notes

Debug/prototype code can exist outside production path only if explicitly marked and not shipped.
