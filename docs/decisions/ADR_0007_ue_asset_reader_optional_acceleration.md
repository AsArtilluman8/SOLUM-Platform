# ADR 0007 — Optional acceleration for UE Asset Truth Reader

## Status

Accepted for P59.

## Context

Large UE TextureSource payloads require a vertical unsigned-byte prefix sum and UE hash-derived source IDs require BLAKE3. The truth reader must remain usable in Python 3.10 on Termux without making a compiled wheel a correctness dependency.

## Decision

Keep the standard-library implementation as the required path and expose two optional performance extras:

- `blake3>=1.0` — native BLAKE3 accelerator; CC0-1.0 or Apache-2.0.
- `numpy>=1.24` — bounded unsigned-byte accumulation for large UEDELTA images; BSD-3-Clause.

The package imports either accelerator only inside the relevant path. The BLAKE3 fallback is checked against official vectors, and UEDELTA output is accepted only after the same serialized extrema and source-ID checks regardless of implementation.

## Build and size impact

- Default install has no third-party runtime dependency.
- `pip install -e '.[performance]'` opts into platform wheels or a local build.
- Termux users can skip the extra when a compatible wheel/toolchain is unavailable; decoding is slower but not weakened.
- No dependency is bundled into the Android APK or SOLUM engine runtime.

## Alternatives rejected

- Making NumPy mandatory: unnecessary installation and binary-size cost for small assets.
- Trusting only the native BLAKE3 module: would make identity validation unavailable on unsupported platforms.
- Vendoring either project: increases maintenance and license surface without improving correctness.

## Verification

- Pure-Python BLAKE3 tests use official empty-input and multi-chunk vectors.
- The same P59 TextureSource corpus was verified with the optional accelerators enabled.
- Unit tests run with and without optional imports where practical; output provenance and hashes do not change.
