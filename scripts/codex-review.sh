#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

PROMPT="Review the changes on this branch compared to main. Follow the Code Review Rules in AGENTS.md strictly. List findings with severity P0 to P3, file and line, and a short explanation. Do not modify any files."

codex exec \
  --sandbox read-only \
  --output-last-message review.md \
  "$PROMPT"

{
  printf '## Codex review\n\n'
  cat review.md
} | gh pr comment --body-file -
