# SOLUM Vulkan Capability Check

This tool is part of Patch 02 diagnostics.

## Goal

Collect real Vulkan device capabilities without building the full renderer yet.

It attempts to:

```text
create VkInstance
↓
enumerate physical devices
↓
dump properties/features/limits/extensions
↓
write vulkan_caps.json
```

## Run through diagnostics

Preferred:

```bash
bash tools/collect_diagnostics.sh
```

Direct run:

```bash
cd tools/vulkan_caps
bash build_and_run_vulkan_caps.sh
```

## Output

```text
vulkan_caps.json
```

Main fields:

- status;
- deviceName;
- apiVersion;
- driverVersion;
- vendorID;
- features;
- limits;
- extensions;
- missingCritical.

## Important

If this fails from Termux shell, that is still useful diagnostics.

Correct fallback:

```text
record failure in vulkan_caps.json
↓
include build log in diagnostics ZIP
↓
move actual caps runner into Android native module in Patch 04 if needed
```

Wrong fallback:

```text
fake success
OpenGL fallback
skip Vulkan capability check silently
```
