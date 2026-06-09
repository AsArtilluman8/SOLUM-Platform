# Filament Runtime Forensics - P46

Status: render diagnostics and runtime truth lock.

## Main Conclusion

- Main HUD must not call callback FPS the real FPS.
- Callback FPS is a timing source, not the final smoothness truth.
- Estimated visible FPS should be the primary user-facing value until real GPU timing/profiler evidence exists.
- Real GPU truth needs FrameMetrics validation, `dumpsys gfxinfo`, Perfetto, Android GPU Inspector, or native hooks.

The user-facing HUD should prioritize:

```text
FPS 24 | frame 41.6 ms | p95 41.6 ms | JANK
```

or:

```text
FPS 44 | frame 22.4 ms | p95 22.4 ms | OK
```

Java callback FPS belongs in Debug/Diagnostics as `Java callback FPS`, not as the main FPS claim.

## Current FPS Measurement

`FilamentGlbPreviewActivity` currently measures frame pacing from Java-side callbacks and wall-clock deltas:

- `Choreographer` drives frame callbacks.
- Each callback records elapsed wall time between Java frames.
- Rolling callback FPS is derived from callback interval samples.
- Estimated visible FPS is derived from p95 frame interval, so stutters reduce the number shown to the user.
- Render CPU submit time is approximate Java-side wall timing around render work.
- Android `Window.OnFrameMetricsAvailableListener` is sampled when available and reported in Debug.

These values are useful diagnostics signals, but they are not a full GPU profiler.

## Why Callback FPS Can Show 60 While Visual Smoothness Feels 8-12 FPS

Java/Choreographer timing can still report near 60 FPS when the app receives regular callbacks while the visible output feels much slower. Common reasons:

- GPU work can stall after Java submits the frame.
- The display compositor may repeat frames even if Java keeps ticking.
- Filament Java APIs do not expose authoritative per-pass GPU timing here.
- SSR and other screen-space effects can become GPU-bound without a matching Java frame-time spike.
- Android can batch, queue, or smooth frame delivery in ways that hide GPU pressure from a simple callback FPS counter.
- Thermal throttling and Mali tile/bandwidth pressure can reduce visible smoothness before Java logic clearly shows it.

Therefore P46 locks the wording:

```text
FPS/wall timing is estimated.
Java callback FPS is a timing source.
GPU timing is unavailable unless FrameMetrics/profiler/native hooks expose it.
SSR can visually feel much lower than HUD callback FPS if GPU stalls.
```

## What Java Can Measure Now

Current Java-side evidence can measure or report:

- Choreographer callback cadence.
- Wall-clock frame interval.
- Estimated visible FPS from p95 frame interval.
- Current/average/min/max/p95/worst frame milliseconds.
- Slow-frame and jank counters.
- Approximate Java render/submit CPU timing.
- Current quality settings and active expensive effects.
- Android `FrameMetrics` total/draw/swap durations when the platform provides them.
- `FrameMetrics.GPU_DURATION` only if Android reports a non-zero value on this device/path.

## What Needs Deeper Profiling Later

Real frame truth requires one or more of:

- Android `FrameMetrics` validation with captured samples on the target device.
- `dumpsys gfxinfo com.solum.engine framestats` from an attached ADB environment.
- Perfetto trace with SurfaceFlinger, Choreographer, HWUI, scheduling, frequency, and GPU counters where available.
- Android GPU Inspector for GPU frame capture, queue stalls, counters, and render pass cost.
- Native/C++ or Filament lower-level hooks if per-frame GPU timestamp queries become available in the chosen runtime path.

## Recommended Profiler Roadmap

1. HUD Light - always cheap:
   - estimated visible FPS;
   - p95 frame ms;
   - status: GOOD / OK / JANK / BAD;
   - active expensive effects such as SSR, AO Debug Max, Bloom High, TAA, high MSAA.

2. FrameMetrics validation - current/next:
   - total duration;
   - draw duration;
   - swap duration;
   - GPU duration if Android exposes non-zero values;
   - compare against HUD estimate and visible stutter.

3. gfxinfo capture - external command:

```bash
adb shell dumpsys gfxinfo com.solum.engine framestats
```

4. Perfetto / AGI - deeper profiling:
   - GPU queue pressure;
   - SurfaceFlinger/compositor behavior;
   - CPU scheduling;
   - render pass or frame capture cost where available.

5. Native/C++ GPU timing hooks - later if needed:
   - timestamp queries;
   - per-pass timing;
   - low-level render diagnostics;
   - only after Render Core ownership exists.

## Current Truth Labels

- Main HUD: estimated visible FPS first.
- Debug: Java callback FPS, timing sources, FrameMetrics, gfxinfo command, profiler status.
- GPU timing: `unavailable` unless measured by FrameMetrics/profiler/native path.
- SSR: marked expensive and not trusted by callback FPS alone.

## Render Control Truth Problem

FPS can only be interpreted correctly if render settings are truthful. A Low profile that still keeps AO Debug Max, Bloom High, MSAA 4x, SSR, TAA, or Dynamic Resolution Off is not really Low, even if the HUD reports an estimated FPS value.

P47 locks these rules:

- Low/Medium/High presets must force their expected render state instead of inheriting old expensive config values.
- Debug must show requested vs actual state for sensitive controls.
- The main HUD may show `preset_mismatch` if selected profile, requested state, and actual reported state diverge.
- MSAA is requested through Filament Java `View.setSampleCount`, but device-level verification is not available from this path. Debug must mark it as `live_update_requested_not_device_verified` unless a deeper profiler or runtime proof is added.
- Dynamic Resolution is requested through Filament Java `View.setDynamicResolutionOptions`, but the real GPU scaling behavior is still `not_device_verified`.

Expected preset truth:

```text
Low:
  SSR Off, TAA Off, AO Off, Bloom Off, MSAA 1x, Dynamic Resolution On, Shadows Off, Sun Glare Off.

Medium:
  SSR Off, TAA Off, AO Soft, Bloom Low, MSAA 2x, Dynamic Resolution On, Shadows Soft, Sun Glare Subtle.

High Preview:
  SSR Off, TAA Off, AO Medium max, Bloom Medium max, MSAA 2x, Dynamic Resolution On, Shadows Medium, Sun Glare Subtle.

Ultra Preview:
  SSR Off by default, TAA Off by default, AO Medium, Bloom Medium, MSAA 4x, Dynamic Resolution On, warnings for expensive state.
```

Old saved config must not silently restore expensive state into Low/Medium/High. Loading config should pass through preset enforcement and then save the hardened state.

Until profiler evidence exists, GPU timing remains `not_exposed/deferred`.
