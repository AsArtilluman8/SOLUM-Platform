# ADR-0004: Android keystore strategy

## Status

Accepted as required before first APK.

## Decision

Before publishing or relying on installable APK updates, SOLUM must create and protect Android signing keystore.

## Why

Android uses APK signature to decide whether a new APK can update an existing installed app.

If keystore is lost:

```text
new APK cannot update old app
↓
user must uninstall old app
↓
data may be lost
```

This becomes critical once SOLUM has multiple apps.

## Rules

- Keystore must not be stored in Download.
- Keystore must not be committed to GitHub.
- Keystore backup location must be documented privately by user.
- Build scripts must support local keystore path via environment/config file ignored by git.
- Debug builds may use debug signing, but release/update builds need stable keystore.

## Future open decision

Need final choice:

1. One keystore for all SOLUM APKs.
2. Separate keystores per app.

Initial recommendation:

```text
one SOLUM platform keystore for early ecosystem builds
```

Reason:

- simpler on phone;
- less risk of losing one among many;
- easier launcher-managed updates.

## Do not do

- Do not generate random new keystore per build.
- Do not store keystore in `/storage/emulated/0/Download`.
- Do not publish install/update flow before keystore strategy is fixed.
