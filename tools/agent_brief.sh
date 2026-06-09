#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

OUT="_build/agent"
BRIEF="$OUT/agent_brief.md"
mkdir -p "$OUT"

BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"
LAST_COMMITS="$(git log -5 --pretty=format:'- %h %s' 2>/dev/null || echo '- unavailable')"

ACTIVE_PATCH="unknown"
PRIORITY="unknown"
MAIN_FILE="unknown"
KEY_DOCS=""
BUILD_COMMAND="bash tools/agent_build_report.sh"
EXPECTED_APK="apps/engine/build/outputs/apk/debug/engine-debug.apk"
DO_NOT=""
ROADMAP=""
REQUIRED_REPORT=""

if [ -f solum_agent_state.json ] && command -v python3 >/dev/null 2>&1; then
  eval "$(python3 - <<'PY'
import json, shlex
with open("solum_agent_state.json", "r", encoding="utf-8") as fh:
    data = json.load(fh)
def emit(name, value):
    print(f"{name}={shlex.quote(value)}")
emit("ACTIVE_PATCH", str(data.get("active_patch", "unknown")))
emit("PRIORITY", str(data.get("priority", "unknown")))
emit("MAIN_FILE", str(data.get("main_file", "unknown")))
emit("KEY_DOCS", "\n".join(f"- {x}" for x in data.get("key_docs", [])))
cmd = [str(x) for x in data.get("build_command", ["bash", "tools/agent_build_report.sh"])]
if "gradle" in cmd and cmd[:2] == ["bash", "tools/build_native_engine.sh"]:
    idx = cmd.index("gradle")
    build_command = " ".join(cmd[:idx]) + "\n" + " ".join(cmd[idx:])
else:
    build_command = " ".join(cmd)
emit("BUILD_COMMAND", build_command)
emit("EXPECTED_APK", str(data.get("expected_apk", "apps/engine/build/outputs/apk/debug/engine-debug.apk")))
emit("DO_NOT", "\n".join(f"- {x}" for x in data.get("do_not", [])))
emit("ROADMAP", "\n".join(f"- {x}" for x in data.get("current_near_term_order", [])))
emit("REQUIRED_REPORT", "\n".join(f"- {x}" for x in data.get("required_report", [])))
PY
)"
fi

cat > "$BRIEF" <<EOF
# SOLUM Agent Brief

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

## Branch
$BRANCH

## Last 5 Commits
$LAST_COMMITS

## Active Patch
$ACTIVE_PATCH

## Current Priority
$PRIORITY

## Key Files
- $MAIN_FILE
$KEY_DOCS

## Build Command
\`\`\`bash
$BUILD_COMMAND
\`\`\`

## Expected APK
$EXPECTED_APK

## Mandatory Do Not Rules
$DO_NOT

## Near-Term Roadmap
$ROADMAP

## Required Final Report
$REQUIRED_REPORT

## Token Guidance
- Use this brief as a starting index.
- Open exact files and line ranges when needed.
- Do not reduce code quality or verification to save tokens.
- Do not claim planned systems are implemented.
EOF

echo "agent_brief=$BRIEF"
