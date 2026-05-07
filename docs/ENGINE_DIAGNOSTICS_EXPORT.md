# ENGINE_DIAGNOSTICS_EXPORT

Engine diagnostics are exported from `apps/engine`.

## User flow

1. Open SOLUM Engine.
2. Tap `Choose Diagnostics Folder`.
3. In Android picker, choose:

```text
/storage/emulated/0/SOLUMCreative
```

4. Tap `Export Engine Diagnostics`.

## Output

Preferred output through SAF:

```text
/storage/emulated/0/SOLUMCreative/diagnostics/latest/engine_runtime_state.json
/storage/emulated/0/SOLUMCreative/diagnostics/latest/engine_diagnostics_manifest.json
```

If SAF is not configured, Engine tries direct public storage. If that fails, it writes app-specific fallback.

## Visible feedback

The Engine screen shows:

```text
Diagnostics folder: configured/not configured
Last export: not run/running/ok/failed
Last export route: saf/direct/fallback/failed
Last export reason/path
Last export timestamp
```

During export, the button changes:

```text
Exporting...
Export OK
Export Failed
```

## Manifest truth

`engine_diagnostics_manifest.json` must include:

```text
exportStatus
exportRoute
actualRoot
reason
screenshot.status = not_available
screenshot.reason = renderer_readback_not_implemented
```

Screenshot/readback is intentionally not implemented in this patch.
