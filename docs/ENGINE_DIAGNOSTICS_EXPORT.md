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

## FPS truth

`engine_runtime_state.json` must use the stable HUD FPS source:

```text
fpsCurrent
frameTimeMs
fpsSource
fpsLastStable
frameTimeLastStableMs
```

If FPS is not measured yet, `fpsCurrent = 0` is valid only with `fpsSource = not_ready`.
Import/export pauses must not replace the last stable HUD FPS with a near-zero FPS or huge frame time.

## Debug ZIP

`Export Debug ZIP` writes:

```text
/storage/emulated/0/Download/SOLUM_EXPORTS/SOLUM_DEBUG_YYYYMMDD_HHMMSS.zip
```

Required ZIP entries:

```text
engine_runtime_state.json
engine_diagnostics_manifest.json
model_import_state.json
asset_report.json
debug_zip_runtime_note.txt
```

Optional entry:

```text
glb_model_summary.json
```

After ZIP creation, final diagnostics must report:

```text
debugZipStatus = ok
debugZipPath
debugZipIncludedFiles
debugZipReason
```

## Manifest truth

`engine_diagnostics_manifest.json` must include:

```text
exportStatus
exportRoute
actualRoot
reason
debugZipStatus
debugZipPath
debugZipIncludedFiles
debugZipReason
debugZipRequiredFileStatus
debugZipOptionalFileStatus
screenshot.status = not_available
screenshot.reason = renderer_readback_not_implemented
```

Screenshot/readback is intentionally not implemented in this patch.
