#!/usr/bin/env bash
# Koan privacy check, run as a git pre-commit hook (see CLAUDE.md "Public repo").
# Install once per clone: ln -s ../../tools/privacy-pre-commit.sh .git/hooks/pre-commit
# Refuses a commit when a staged added line or file name matches a pattern in .privacy-patterns
# (gitignored; one case-insensitive extended regex per line, # comments and blank lines ignored).
# Example: .privacy-patterns.example. Skip once, knowingly: git commit --no-verify
set -euo pipefail
root=$(git rev-parse --show-toplevel)
patterns_file="$root/.privacy-patterns"

if [ ! -s "$patterns_file" ]; then
  echo "privacy check: $patterns_file is missing or empty." >&2
  echo "  cp .privacy-patterns.example .privacy-patterns, then put your real patterns in it." >&2
  exit 1
fi

pats=$(mktemp)
trap 'rm -f "$pats"' EXIT
grep -vE '^[[:space:]]*(#|$)' "$patterns_file" > "$pats" || true
[ -s "$pats" ] || { echo "privacy check: no patterns in $patterns_file" >&2; exit 1; }

# Added lines as "file:line: text", plus the staged file names themselves.
added=$(
  git diff --cached -U0 --no-color --no-ext-diff --diff-filter=ACMR |
    awk '
      /^\+\+\+ b\// { file = substr($0, 7); next }
      /^\+\+\+ /    { next }
      /^@@ /        { split($3, a, /[+,]/); line = a[2]; next }
      /^\+/         { print file ":" line ": " substr($0, 2); line++ }
    '
  git diff --cached --name-only --diff-filter=ACMR | sed 's/^/(file name) /'
)

if hits=$(grep -inE -f "$pats" <<<"$added" | cut -d: -f2-); then
  echo "privacy check: the staged changes match .privacy-patterns, commit refused:" >&2
  echo "$hits" | cut -c1-200 | sed 's/^/  /' >&2
  echo "Remove it (public repo), or commit with --no-verify if it's a false positive." >&2
  exit 1
fi
