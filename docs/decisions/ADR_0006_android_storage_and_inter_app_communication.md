# ADR-0006: Android storage and inter-app communication

## Status

Accepted as design direction. Implementation details must be tested on device.

## Decision

SOLUM needs a shared creative root for projects/assets/reports and a safe way for future separate APKs to exchange files.

Preferred root:

```text
/storage/emulated/0/SOLUMCreative/
```

Fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

## Why

SOLUM will eventually have multiple apps:

- Engine;
- Launcher;
- Asset Hub;
- Material Studio;
- AniStudio;
- etc.

They need to work with shared projects and assets without scattering files across Download/app cache/random folders.

## Android storage risk

Android 11+ scoped storage can restrict file access.

Potential options:

1. Shared external folder with permission.
2. App-specific storage for cache/temp.
3. Storage Access Framework for selected folders.
4. FileProvider + explicit Intents between apps.
5. MANAGE_EXTERNAL_STORAGE only if justified and user approves.

## Rules

- Do not hardcode one storage path without checking access.
- App-specific cache goes under app cache/data, not shared project folder.
- User-facing assets/projects/reports go under SOLUMCreative.
- Permission denied must show clear explanation and fallback.
- Inter-app file sharing must not require manual file hunting.

## Future inter-app flow

Example: Material Studio sends material to Engine.

```text
Material Studio saves .solummat folder/file
↓
Asset manifest validated
↓
Asset Hub indexes it
↓
Engine receives asset id or file URI
↓
Engine validates compatibility
↓
Engine imports/uses material
```

For separate APKs:

- use explicit Intent where possible;
- use FileProvider for file URI sharing;
- define stable authorities before public release;
- avoid silent hidden file copies.

## Launcher role

Launcher later manages:

- app list;
- versions;
- compatibility;
- manual APK install/update;
- opening tools;
- opening project context.

No silent auto-update.

## Open questions for implementation

- Can `/storage/emulated/0/SOLUMCreative/` be used reliably on target device?
- Which permission path is acceptable for dev build?
- What fallback is best if permission denied?
- How to handle Google Play policy later if needed?

## Do not do

- Do not scatter outputs in Download root.
- Do not make each app use incompatible private asset folders.
- Do not depend on silent APK installs.
- Do not use unstable FileProvider authorities after release.
