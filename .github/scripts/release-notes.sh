#!/usr/bin/env bash
# Next tag and release notes of one module. Read only.
# Local run: GITHUB_TOKEN=$(gh auth token) MODULE=dispatcher BUMP=patch .github/scripts/release-notes.sh
set -euo pipefail

: "${MODULE:?module name, see release-module.yml}"
: "${BUMP:?patch, minor or major}"
PREVIOUS_VERSION=${PREVIOUS_VERSION:-}
export GITHUB_REPO=${GITHUB_REPO:-ansforge/SAMU-Hub-Sante}
RUNNER_TEMP=${RUNNER_TEMP:-$(mktemp -d)}
GITHUB_STEP_SUMMARY=${GITHUB_STEP_SUMMARY:-/dev/stdout}
GITHUB_OUTPUT=${GITHUB_OUTPUT:-/dev/null}

cd "$(git rev-parse --show-toplevel)"

# Module -> path whose changes go in its notes (the image build context).
case "$MODULE" in
  dispatcher) path="hub/dispatcher" ;;
  lrm) path="web/lrm" ;;
  landing) path="web/landing" ;;
  tmc | healthcheck | annuaire | chatbot | openssl | demo-app-tempo) path="tools/$MODULE" ;;
  specs-back) path="tools/specs/server" ;;
  specs-front) path="tools/specs/client" ;;
  *) echo "::error::Unknown module $MODULE"; exit 1 ;;
esac
prefix="$MODULE-"
filter=(--include-path "$path/**")

# One line per PR: hide PR branch commits, except breaking ones.
trap 'rm -f .cliffignore' EXIT
git log --merges --grep='^Merge pull request' --format=%H | while read -r merge; do
  git rev-list --no-merges --invert-grep -E \
    --grep='^BREAKING[ -]CHANGE' --grep='^[a-z]+(\([^)]*\))?!:' "$merge^1..$merge^2"
done > .cliffignore

# Computed here: git-cliff misses tags outside the filter (orhun/git-cliff#208).
latest=$(git tag --merged HEAD --list "${prefix}*" | grep -E "^${prefix}[0-9]+\.[0-9]+\.[0-9]+\$" | sort -V | tail -n 1 || true)
[ -n "$latest" ] || { echo "::error::No ${prefix}X.Y.Z tag on HEAD"; exit 1; }
IFS=. read -r major minor patch <<< "${latest#"$prefix"}"
case "$BUMP" in
  major) tag="$prefix$((major + 1)).0.0" ;;
  minor) tag="$prefix$major.$((minor + 1)).0" ;;
  patch) tag="$prefix$major.$minor.$((patch + 1))" ;;
  *) echo "::error::Unknown bump $BUMP"; exit 1 ;;
esac

base=$latest
if [ -n "$PREVIOUS_VERSION" ]; then
  base="$prefix$PREVIOUS_VERSION"
  git merge-base --is-ancestor "$base" HEAD 2> /dev/null || { echo "::error::No tag $base on HEAD"; exit 1; }
fi

# Explicit range, no tag visible: --tag-pattern "^$base$" with --tag split the
# chatbot notes (from chatbot-1.2.0) in two sections. The compare link comes from BASE and TAG.
BASE=$base TAG=$tag git cliff "${filter[@]}" --tag-pattern '^$' "$base..HEAD" --output "$RUNNER_TEMP/notes.md"
{ echo "Tag: \`$tag\`"; echo; cat "$RUNNER_TEMP/notes.md"; echo; } >> "$GITHUB_STEP_SUMMARY"
echo "tag=$tag" >> "$GITHUB_OUTPUT"
