#!/usr/bin/env bash
set -u

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOLUM_ROOT="${SOLUM_ROOT:-/storage/emulated/0/SOLUMCreative}"
if [ ! -d "$(dirname "$SOLUM_ROOT")" ]; then
  SOLUM_ROOT="/storage/emulated/0/Download/SOLUMCreative"
fi

TS="$(date +%Y%m%d_%H%M%S)"
PATCH_ID="${SOLUM_PATCH_ID:-P02_diagnostics_vulkan_caps}"
LATEST_DIAG_DIR="$SOLUM_ROOT/diagnostics/latest"
LATEST_REPORT_DIR="$SOLUM_ROOT/reports/latest"
ARCHIVE_DIR="$SOLUM_ROOT/diagnostics/archive/${TS}_${PATCH_ID}"
WORK_DIR="$ARCHIVE_DIR/work"

mkdir -p "$LATEST_DIAG_DIR" "$LATEST_REPORT_DIR" "$ARCHIVE_DIR" "$WORK_DIR"

log() { printf '%s\n' "$*"; }
write_json_string() { python3 - <<'PY' "$1"
import json, sys
print(json.dumps(sys.argv[1]))
PY
}

capture_cmd() {
  local out="$1"; shift
  {
    echo "$ $*"
    "$@"
  } > "$out" 2>&1 || true
}

log "SOLUM diagnostics → $WORK_DIR"

# Basic summary
cat > "$WORK_DIR/summary.txt" <<EOF
SOLUM Diagnostics v1
Timestamp: $TS
Patch: $PATCH_ID
Project root: $PROJECT_ROOT
SOLUM root: $SOLUM_ROOT
EOF

# Device info from Android properties when available
python3 - <<'PY' > "$WORK_DIR/device_info.json"
import json, os, subprocess

def getprop(name):
    try:
        return subprocess.check_output(['getprop', name], text=True, stderr=subprocess.DEVNULL).strip()
    except Exception:
        return ''

def read(path):
    try:
        return open(path, 'r', errors='ignore').read().strip()
    except Exception:
        return ''

info = {
    'manufacturer': getprop('ro.product.manufacturer'),
    'brand': getprop('ro.product.brand'),
    'model': getprop('ro.product.model'),
    'device': getprop('ro.product.device'),
    'androidVersion': getprop('ro.build.version.release'),
    'sdk': getprop('ro.build.version.sdk'),
    'abi': getprop('ro.product.cpu.abi'),
    'hardware': getprop('ro.hardware'),
    'kernel': read('/proc/version'),
    'meminfoFirstLines': read('/proc/meminfo').splitlines()[:8],
    'thermalZones': []
}
for root, dirs, files in os.walk('/sys/class/thermal'):
    if root.count(os.sep) > 4:
        dirs[:] = []
    if 'type' in files and 'temp' in files:
        t = read(os.path.join(root, 'type'))
        v = read(os.path.join(root, 'temp'))
        if t or v:
            info['thermalZones'].append({'type': t, 'tempRaw': v})
print(json.dumps(info, indent=2, ensure_ascii=False))
PY

# Environment info
python3 - <<'PY' > "$WORK_DIR/env_info.json"
import json, os, shutil, subprocess

def run(cmd):
    try:
        return subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT, timeout=8).strip()
    except Exception as e:
        return str(e)

def which(x):
    return shutil.which(x) or ''

info = {
    'shell': os.environ.get('SHELL', ''),
    'prefix': os.environ.get('PREFIX', ''),
    'home': os.environ.get('HOME', ''),
    'androidHome': os.environ.get('ANDROID_HOME', ''),
    'androidSdkRoot': os.environ.get('ANDROID_SDK_ROOT', ''),
    'path': os.environ.get('PATH', ''),
    'tools': {
        'python3': which('python3'),
        'git': which('git'),
        'java': which('java'),
        'gradle': which('gradle'),
        'clang': which('clang'),
        'clang++': which('clang++'),
        'aapt2': which('aapt2'),
        'zip': which('zip'),
        'unzip': which('unzip')
    },
    'versions': {
        'python3': run(['python3', '--version']),
        'git': run(['git', '--version']),
        'java': run(['java', '-version']),
        'clang': run(['clang', '--version']),
        'aapt2': run(['aapt2', 'version']) if which('aapt2') else 'missing'
    }
}
print(json.dumps(info, indent=2, ensure_ascii=False))
PY

# Git state
(
  cd "$PROJECT_ROOT" || exit 0
  python3 - <<'PY'
import json, subprocess

def run(cmd):
    try:
        return subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT).strip()
    except Exception as e:
        return str(e)
print(json.dumps({
    'branch': run(['git','rev-parse','--abbrev-ref','HEAD']),
    'commit': run(['git','rev-parse','HEAD']),
    'statusShort': run(['git','status','--short']),
    'remote': run(['git','remote','-v']),
    'lastCommit': run(['git','log','-1','--oneline'])
}, indent=2, ensure_ascii=False))
PY
) > "$WORK_DIR/git_state.json" 2>&1 || true

# Storage state
python3 - <<'PY' "$SOLUM_ROOT" > "$WORK_DIR/storage_state.json"
import json, os, shutil, sys
root = sys.argv[1]
usage = shutil.disk_usage('/storage/emulated/0') if os.path.exists('/storage/emulated/0') else shutil.disk_usage('/')
print(json.dumps({
    'solumRoot': root,
    'solumRootExists': os.path.exists(root),
    'externalStorageExists': os.path.exists('/storage/emulated/0'),
    'totalBytes': usage.total,
    'usedBytes': usage.used,
    'freeBytes': usage.free
}, indent=2))
PY

# Build log placeholders for Patch 02
cat > "$WORK_DIR/build_log.txt" <<EOF
Patch 02 diagnostics-only collection.
No Android build executed by collect_diagnostics.sh.
EOF
cat > "$WORK_DIR/runtime_log.txt" <<EOF
Patch 02 diagnostics-only collection.
No app runtime executed by collect_diagnostics.sh.
EOF
cat > "$WORK_DIR/crash_log.txt" <<EOF
No crash log collected in diagnostics v1 shell mode.
Future Android app diagnostics should include logcat/crash traces.
EOF

# Vulkan caps
VULKAN_DIR="$PROJECT_ROOT/tools/vulkan_caps"
if [ -x "$VULKAN_DIR/build_and_run_vulkan_caps.sh" ]; then
  SOLUM_VULKAN_CAPS_OUT="$WORK_DIR/vulkan_caps.json" bash "$VULKAN_DIR/build_and_run_vulkan_caps.sh" > "$WORK_DIR/vulkan_caps_build_log.txt" 2>&1 || true
fi
if [ ! -s "$WORK_DIR/vulkan_caps.json" ]; then
  cat > "$WORK_DIR/vulkan_caps.json" <<EOF
{
  "schema": "solum.vulkan_caps",
  "schemaVersion": 1,
  "status": "failed",
  "reason": "vulkan_caps tool did not produce output",
  "next": "Check vulkan_caps_build_log.txt. If Termux native Vulkan caps is unavailable, move caps runner into Android native module in Patch 04.",
  "devices": []
}
EOF
fi

# Performance baseline placeholder
python3 - <<'PY' "$TS" "$PATCH_ID" > "$WORK_DIR/performance_history.json"
import json, sys
print(json.dumps({
  'schema': 'solum.performance_history',
  'schemaVersion': 1,
  'samples': [{
    'timestamp': sys.argv[1],
    'patch': sys.argv[2],
    'scene': 'diagnostics_shell',
    'fpsAvg': None,
    'frameMsAvg': None,
    'cpuMsApprox': None,
    'gpuMsApprox': None,
    'memoryMb': None,
    'notes': 'No renderer in Patch 02. This file establishes the performance history schema.'
  }]
}, indent=2))
PY

cat > "$WORK_DIR/known_issues.txt" <<EOF
- Vulkan caps may fail from Termux shell on some devices.
- If vulkan_caps.json status=failed, Patch 04 must implement Android native-module caps runner.
- No renderer, swapchain or triangle in Patch 02 by design.
EOF

# Changed files snapshot
(
  cd "$PROJECT_ROOT" || exit 0
  git status --short
) > "$WORK_DIR/changed_files.txt" 2>&1 || true

# Build HTML report
python3 "$PROJECT_ROOT/tools/report_builder.py" "$WORK_DIR" "$WORK_DIR/report.html" || {
  cat > "$WORK_DIR/report.html" <<EOF
<html><body><h1>SOLUM Diagnostics v1</h1><p>report_builder.py failed. See raw JSON files.</p></body></html>
EOF
}

# Package and latest copies
( cd "$WORK_DIR" && zip -qr "$ARCHIVE_DIR/SOLUM_LATEST_DIAGNOSTICS.zip" . ) || true
cp "$WORK_DIR/report.html" "$LATEST_REPORT_DIR/SOLUM_LATEST_REPORT.html" 2>/dev/null || true
cp "$ARCHIVE_DIR/SOLUM_LATEST_DIAGNOSTICS.zip" "$LATEST_DIAG_DIR/SOLUM_LATEST_DIAGNOSTICS.zip" 2>/dev/null || true
cp "$WORK_DIR/report.html" "$ARCHIVE_DIR/SOLUM_LATEST_REPORT.html" 2>/dev/null || true

log "Done."
log "ZIP: $LATEST_DIAG_DIR/SOLUM_LATEST_DIAGNOSTICS.zip"
log "HTML: $LATEST_REPORT_DIR/SOLUM_LATEST_REPORT.html"
log "Archive: $ARCHIVE_DIR"
