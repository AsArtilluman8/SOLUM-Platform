# SOLUM Mali Render Optimization Rules

Status: P49 foundation.

## Mobile GPU Rules

Target Android/Mali-class devices are bandwidth sensitive.

Default assumptions:

- transparency and overdraw are expensive;
- SSR is gameplay unsafe by default until proven by profiling;
- MSAA 4x can be expensive;
- AO Strong / Debug Max can be expensive;
- Bloom High can be expensive;
- TAA is not free and remains medium/unknown cost until measured;
- render scale and dynamic resolution matter on high-resolution phones.

## Diagnostics Language

Do not claim exact per-feature milliseconds without a profiler or future cost probe.

Allowed wording:

```text
estimated_cost
not_runtime_measured
needs_cost_probe_later
```

## Practical Defaults

For normal phone preview:

- keep Dynamic Resolution on;
- prefer MSAA 2x or FXAA before MSAA 4x;
- keep SSR off for gameplay;
- keep Bloom and AO moderate;
- treat transparent models/materials as unknown cost until material scan;
- treat heavy models as unknown cost until mesh scan.

## Future Work

Future patches can add:

- Run Cost Probe;
- material scan for alpha/transparent overdraw risk;
- mesh scan for triangle/material/draw-call risk;
- profiler integration when available.
