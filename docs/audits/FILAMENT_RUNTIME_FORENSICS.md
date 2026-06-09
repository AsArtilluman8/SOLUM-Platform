# Filament Runtime Forensics — P45C

Status: audit/forensics patch, not a visual feature patch.

## Current FPS Measurement

`FilamentGlbPreviewActivity` currently measures runtime frame pacing from Java-side frame callbacks and wall-clock deltas:

- `Choreographer` drives frame callbacks.
- Each callback records elapsed wall time between Java frames.
- HUD FPS is derived from rolling frame interval samples.
- Render CPU submit time is approximate Java-side wall timing around render work.
- Android `Window.OnFrameMetricsAvailableListener` is now sampled when available and reported separately.

This means the HUD is useful for trend detection, but it is not a full GPU profiler.

## Why HUD Can Show 60 While Visual Smoothness Feels 8-12 FPS

Java/Choreographer timing can still report near 60 FPS when the app keeps receiving regular callbacks, while the visible output feels much slower. Common reasons:

- GPU work can stall after Java submits the frame.
- The display compositor may repeat frames even if Java keeps ticking.
- Filament Java APIs do not expose authoritative per-pass GPU timing here.
- Screen-space effects such as SSR can become GPU-bound without showing as a matching Java frame-time spike.
- Android may batch, queue, or smooth frame delivery in ways that hide GPU pressure from a simple wall-clock HUD.
- Thermal throttling and tile-based Mali bandwidth pressure can reduce visual smoothness before Java logic clearly shows it.

Therefore P45C must not claim the HUD FPS is fully truthful. The correct runtime truth is:

```text
FPS/wall timing is estimated.
GPU timing is not exposed by the current Filament Java path.
SSR can visually feel much lower than HUD FPS if GPU stalls.
```

## What Java Can Measure Now

Current Java-side evidence can measure or report:

- Choreographer/wall-clock frame interval.
- Rolling FPS, average frame ms, min/max/p95, slow-frame and jank counters.
- Approximate Java render/submit CPU timing.
- Current quality settings and feature state.
- Android `FrameMetrics` total/draw/swap durations when the platform provides them.
- `FrameMetrics.GPU_DURATION` only if Android reports a non-zero value on this device/path.

These values are diagnostics signals, not final GPU truth.

## What Needs Deeper Profiling Later

Real frame truth requires one or more of:

- Android `FrameMetrics` integration with device validation and captured samples.
- `dumpsys gfxinfo com.solum.engine framestats` from an attached ADB environment.
- Perfetto trace with SurfaceFlinger, Choreographer, HWUI, sched, frequency, and GPU counters where available.
- Android GPU Inspector (AGI) for GPU frame capture, queue stalls, counters, and render pass cost.
- Native/C++ or Filament lower-level hooks if per-frame GPU timestamp queries become available in the chosen runtime path.

## Recommended Next Step

Use Perfetto or AGI as the next real profiler step. For quick non-invasive checks, collect:

```bash
adb shell dumpsys gfxinfo com.solum.engine framestats
```

Then compare:

- HUD wall FPS;
- FrameMetrics total/draw/swap/GPU if present;
- gfxinfo janky frames;
- Perfetto/AGI GPU queue and render pass timing.

Until that evidence exists, GPU timing remains `not_exposed/deferred`.
