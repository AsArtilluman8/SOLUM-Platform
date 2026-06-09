#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

echo "SOLUM_AGENT_GATE"
if [ -f solum_agent_state.json ]; then
  if command -v python3 >/dev/null 2>&1; then
    ACTIVE_PATCH="$(python3 - <<'PY'
import json
with open("solum_agent_state.json", "r", encoding="utf-8") as fh:
    print(json.load(fh).get("active_patch", "unknown"))
PY
)"
  else
    ACTIVE_PATCH="$(sed -n 's/.*"active_patch"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' solum_agent_state.json | head -1)"
  fi
else
  ACTIVE_PATCH="unknown"
fi
echo "ACTIVE_PATCH=${ACTIVE_PATCH:-unknown}"
echo "MUST_USE_TOOLS=true"
if [ -x tools/agent_brief.sh ]; then
  echo "AGENT_BRIEF_AVAILABLE=run bash tools/agent_brief.sh"
else
  echo "AGENT_BRIEF_AVAILABLE=false_optional_tool_missing"
fi

STATUS=0

if [ -x tools/agent_context.sh ]; then
  bash tools/agent_context.sh || STATUS=1
else
  echo "WARNING_missing_or_not_executable=tools/agent_context.sh"
  STATUS=1
fi

if [ -x tools/agent_repo_health.sh ]; then
  bash tools/agent_repo_health.sh
  CODE=$?
  if [ "$CODE" -ge 2 ]; then STATUS=2; fi
else
  echo "WARNING_missing_or_not_executable=tools/agent_repo_health.sh"
  STATUS=1
fi

if command -v python3 >/dev/null 2>&1; then
  python3 tools/agent_control_truth_static_check.py || true
else
  echo "WARNING_python3_missing_static_check_skipped"
fi

if [ "$STATUS" -ge 2 ]; then
  echo "AGENT_READY=false"
  exit "$STATUS"
fi

echo "AGENT_READY=true"
exit 0
