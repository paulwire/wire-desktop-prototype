#!/usr/bin/env bash
# Regression test for .codex/rules/default.rules: codex-cli parses this file
# as Starlark execpolicy, not YAML, so a syntax slip silently breaks
# scripts/codex-review.sh (codex exec fails to start). This asserts the file
# parses and that each documented command still gets its intended decision.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

RULES=".codex/rules/default.rules"
fail=0

check() {
  local expected="$1"
  shift
  local actual
  actual=$(codex execpolicy check --rules "$RULES" "$@" | sed -E 's/.*"decision":"([a-z]+)"\}$/\1/')
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL: '$*' expected decision '$expected', got '$actual'"
    fail=1
  else
    echo "ok: '$*' -> $actual"
  fi
}

check prompt gh api repos/foo/bar/pulls
check forbidden git push --force origin main
check forbidden gh pr create --title x
check forbidden gh pr merge 1
check forbidden git push origin main

exit "$fail"
