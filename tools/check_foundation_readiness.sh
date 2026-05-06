#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TS="$(date +%Y%m%d_%H%M%S)"
REPORT_DIR="_work/agent_reports/latest"
REPORT="$REPORT_DIR/SOLUM_FOUNDATION_READINESS.txt"
mkdir -p "$REPORT_DIR"
: > "$REPORT"

failures=0
warnings=0

line(){
  echo "$*" | tee -a "$REPORT"
}

pass(){
  line "PASS $*"
}

warn(){
  warnings=$((warnings + 1))
  line "WARN $*"
}

fail(){
  failures=$((failures + 1))
  line "FAIL $*"
}

check_file(){
  local path="$1"
  if [ -f "$path" ]; then
    pass "$path"
  else
    fail "$path missing"
  fi
}

check_dir(){
  local path="$1"
  if [ -d "$path" ]; then
    pass "$path/"
  else
    fail "$path/ missing"
  fi
}

check_exec(){
  local path="$1"
  if [ -x "$path" ]; then
    pass "$path executable"
  elif [ -f "$path" ]; then
    warn "$path exists but is not executable"
  else
    fail "$path missing"
  fi
}

line "SOLUM Foundation Readiness Check"
line "timestamp=$TS"
line "root=$ROOT"
line ""

line "== Required memory docs =="
check_file "docs/PROJECT_MEMORY_INDEX.md"
check_file "docs/CURRENT_STAGE.md"
check_file "docs/AGENT_RULES.md"
check_file "docs/ARCHITECTURE_RULES.md"
check_file "docs/UX_AND_WORKFLOW_RULES.md"
check_file "docs/PATCH_ROADMAP.md"
check_file "docs/RENDERING_TARGET_SPEC.md"
check_file "docs/ASSET_FORMAT_SPEC.md"
check_file "docs/errors/ERROR_KNOWLEDGE_BASE.md"

line ""
line "== Repo skeleton =="
check_file "README.md"
check_file "AGENTS.md"
check_dir "core"
check_dir "engine-core"
check_dir "apps"
check_dir "tools"
check_dir "docs/patch_history"
check_dir "docs/decisions"
check_dir "docs/ux_negative_cases"
check_dir "docs/ideas"

line ""
line "== Build/tools foundation =="
check_exec "tools/agent_build_runner.sh"
check_exec "tools/collect_diagnostics.sh"
check_exec "tools/asset_validator.py"
check_exec "tools/create_sample_asset.py"
check_exec "tools/transaction_save.py"
check_file "tools/vulkan_caps/README.md"
check_file "tools/vulkan_caps/vulkan_caps.c"

line ""
line "== GitHub workflow foundation =="
check_file "docs/GITHUB_WORKFLOW.md"
check_file "docs/AGENT_AUTOPILOT_WORKFLOW.md"
check_file ".github/pull_request_template.md"

line ""
line "== Git state =="
branch="$(git branch --show-current 2>/dev/null || true)"
head="$(git rev-parse --short HEAD 2>/dev/null || true)"
STATUS_TMP="$REPORT_DIR/SOLUM_FOUNDATION_GIT_STATUS.tmp"
line "branch=${branch:-unknown}"
line "head=${head:-unknown}"
if git status --short >"$STATUS_TMP" 2>/dev/null; then
  if [ -s "$STATUS_TMP" ]; then
    warn "working tree has local changes"
    cat "$STATUS_TMP" | tee -a "$REPORT"
  else
    pass "working tree clean"
  fi
else
  warn "git status unavailable"
fi
rm -f "$STATUS_TMP"

line ""
line "== Result =="
if [ "$failures" -eq 0 ]; then
  line "RESULT=FOUNDATION_READY"
else
  line "RESULT=FOUNDATION_NOT_READY"
fi
line "failures=$failures"
line "warnings=$warnings"
line "report=$REPORT"

if [ "$failures" -eq 0 ]; then
  exit 0
fi
exit 1
