#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

SEVERE=0
echo "SOLUM_REPO_HEALTH"

for path in \
  BUILD.md \
  SECURITY.md \
  docs/TESTING.md \
  docs/AI_AGENT_CONTINUATION.md \
  docs/design/SOLUM_CONTROL_TRUTH.md \
  docs/roadmap/SOLUM_ROADMAP.md
do
  if [ -f "$path" ]; then
    echo "doc_ok=$path"
  else
    echo "WARNING_missing_doc=$path"
  fi
done

scan_files="$(find . -maxdepth 4 -type f \( -name '*.md' -o -name '*.txt' -o -name '*.json' \) \
  -not -path './.git/*' -not -path './_build/*' -not -path './build/*' -not -path './apps/*/build/*' 2>/dev/null)"

for pattern in "GPT Pro" "API credits" "private application" "personal device" "private chat" "private account"; do
  matches="$(printf '%s\n' "$scan_files" | xargs -r grep -nI "$pattern" 2>/dev/null | head -5 || true)"
  if [ -n "$matches" ]; then
    echo "WARNING_public_string=$pattern"
    printf '%s\n' "$matches"
  fi
done

for pattern in "token=" "api_key"; do
  matches="$(printf '%s\n' "$scan_files" | xargs -r grep -nIE "$pattern" 2>/dev/null | head -5 || true)"
  if [ -n "$matches" ]; then
    severe_matches="$(printf '%s\n' "$matches" | grep -vE 'token=(present_redacted|not_read|redacted|missing)|api_key(_status)?=(present_redacted|not_read|redacted|missing)' || true)"
    if [ -n "$severe_matches" ]; then
      echo "SEVERE_secret_like_pattern=$pattern"
      printf '%s\n' "$severe_matches"
      SEVERE=1
    else
      echo "WARNING_redacted_secret_reference=$pattern"
      printf '%s\n' "$matches"
    fi
  fi
done

if [ "$SEVERE" -eq 1 ]; then
  echo "REPO_HEALTH=severe_secret_like_warning"
  exit 2
fi

echo "REPO_HEALTH=ok_with_possible_warnings"
