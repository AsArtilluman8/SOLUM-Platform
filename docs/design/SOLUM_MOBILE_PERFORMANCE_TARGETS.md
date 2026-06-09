# SOLUM Mobile Performance Targets

Status: public target document.

SOLUM is a mobile-first project. Performance goals must be understandable and testable across device tiers, not tied to one private test phone.

This document defines target FPS ranges, device tiers, and quality-profile expectations. These targets should guide render settings, presets, optimization work, and future Labs.

## Core Rule

Low must be playable and visually acceptable. Medium must be a practical gameplay target. High and Ultra are preview/flagship targets, not guaranteed on every device.

SOLUM should not accept a profile as valid if it silently leaves expensive debug effects enabled while claiming to be Low or Medium.

## Device Tiers

| Tier | Description | Expected Role |
|---|---|---|
| Baseline Android | Entry-level or older Android devices | Low Safe target, reduced visual complexity |
| Mid-range Android | Common mainstream Android devices | Low Safe and Medium Mobile target |
| Upper-mid Android | Strong modern Android devices | Medium Mobile and High Preview target |
| Flagship Android | High-end Android devices | High Preview and some Ultra Preview target |
| Flagship iOS | High-end iOS devices if supported later | High/Ultra target after platform support |
| Desktop / External / Future | Desktop-class or external GPU testing | Quality validation, screenshots, heavy debug/profiling |

Device tiers are intentionally generic. Public documentation should not depend on one maintainer's personal device.

## FPS Targets

| Quality Profile | Baseline Android | Mid-range Android | Upper-mid Android | Flagship Mobile | Purpose |
|---|---:|---:|---:|---:|---|
| Low Safe | 45-60 FPS | 50-60 FPS | 60 FPS target | 60 FPS target | Comfortable gameplay, not visually broken |
| Medium Mobile | 30-45 FPS | 40-60 FPS | 50-60 FPS | 60 FPS target | Main gameplay profile |
| High Preview | Not guaranteed | 28-40 FPS | 40-60 FPS | 50-60 FPS | Better visuals, preview/gameplay on stronger devices |
| Ultra Preview | Not guaranteed | Screenshot/preview only | 25-45 FPS | 30-60 FPS | Expensive features, flagship/preview |
| Screenshot / Experimental | FPS secondary | FPS secondary | FPS secondary | FPS secondary | Image quality, experiments, diagnostics |

## Interpreting Results

| Result | Meaning |
|---|---|
| 50-60 FPS in Low Safe | Good target result |
| Below 45 FPS in Low Safe on mid-range devices | Optimization or preset truth problem |
| 40-50 FPS in Medium Mobile | Acceptable mainstream target |
| Below 35 FPS in Medium Mobile on mid-range devices | Needs optimization or feature reduction |
| 28-40 FPS in High Preview | Acceptable preview result on non-flagship devices |
| 15-30 FPS in Ultra/Screenshot | Acceptable only for preview, screenshots, or experimental modes |

## Main HUD Principle

The main HUD should be readable by non-expert users:

```text
FPS 48 | OK | Medium | cause: none
FPS 29 | BAD | Medium | cause: Bloom, AO, Shadows
FPS 12 | BAD | SSR | cause: SSR GPU heavy
```

The main HUD must not use Java callback FPS as the primary game-facing FPS value.

Debug may expose detailed metrics:

- Java callback FPS;
- estimated visible FPS;
- FrameMetrics total/draw/swap/GPU;
- p95/worst/jank;
- timing disagreement;
- requested versus actual render state;
- profiler commands.

## Optimization Priority

SOLUM should optimize in this order:

1. make quality profiles truthful;
2. remove expensive debug modes from gameplay profiles;
3. use dynamic resolution for mobile profiles;
4. reduce MSAA/SSR/AO/Bloom/Shadows when needed;
5. detect and explain bottlenecks in the HUD/Debug;
6. add deeper profiler workflows only after basic render controls are truthful.

## What Counts As Failure

A profile fails if:

- it claims Low but keeps high-cost effects enabled;
- it claims applied state that is only requested and not actual;
- it shows callback FPS as main FPS while visible smoothness is poor;
- it exposes controls that appear interactive but do not change state or output;
- it hides required recreate/restart behavior.
