#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

STATE="solum_agent_state.json"

json_value() {
  key="$1"
  if command -v python3 >/dev/null 2>&1 && [ -f "$STATE" ]; then
    python3 - "$key" <<'PY'
import json, sys
key = sys.argv[1]
with open("solum_agent_state.json", "r", encoding="utf-8") as fh:
    data = json.load(fh)
value = data.get(key, "")
if isinstance(value, list):
    print(", ".join(str(x) for x in value))
else:
    print(value)
PY
  elif [ -f "$STATE" ]; then
    sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "$STATE" | head -1
  fi
}

echo "SOLUM_AGENT_CONTEXT"
echo "branch=$(git branch --show-current 2>/dev/null || echo unknown)"
echo "last_commits:"
git log --oneline -5 2>/dev/null || true
echo "active_patch=$(json_value active_patch)"
echo "priority=$(json_value priority)"
echo "main_file=$(json_value main_file)"
echo "key_docs=$(json_value key_docs)"
echo "build_command_shape=bash tools/build_native_engine.sh && gradle --no-daemon -p \"\$PWD\" clean assembleDebug"
echo "expected_apk=$(json_value expected_apk)"
echo "do_not=$(json_value do_not)"
