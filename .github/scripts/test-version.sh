#!/usr/bin/env bash
# Tests for version.sh, run against throwaway git repositories.
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/version.sh"
FAILURES=0

assert_eq() {
  if [ "$2" != "$3" ]; then
    echo "FAIL: $1"
    echo "  expected: $2"
    echo "  actual:   $3"
    FAILURES=$((FAILURES + 1))
  else
    echo "ok: $1"
  fi
}

new_repo() {
  REPO=$(mktemp -d)
  cd "$REPO"
  git init -q -b main
  git config user.email test@example.com
  git config user.name test
  echo "pluginVersion=${1:-1.10.0}" > gradle.properties
  git add gradle.properties
  git commit -q -m "Initial commit"
}

commit() {
  echo "$RANDOM" >> file.txt
  git add file.txt
  git commit -q -m "$1"
}

# No stable tag yet: the first release uses pluginVersion from gradle.properties.
new_repo 1.10.0
commit "Add feature"
commit "Fix bug"
assert_eq "first stable version comes from gradle.properties" "1.10.0" "$("$SCRIPT" stable auto)"
assert_eq "first eap version counts all commits" "1.10.0-eap.3" "$("$SCRIPT" eap)"

# After a stable tag the next version bumps the patch by default.
git tag v1.10.0
commit "Fix another bug"
commit "Improve docs"
assert_eq "auto bumps patch" "1.10.1" "$("$SCRIPT" stable auto)"
assert_eq "eap counts commits since stable tag" "1.10.1-eap.2" "$("$SCRIPT" eap)"
assert_eq "explicit patch" "1.10.1" "$("$SCRIPT" stable patch)"
assert_eq "explicit minor" "1.11.0" "$("$SCRIPT" stable minor)"
assert_eq "explicit major" "2.0.0" "$("$SCRIPT" stable major)"

# Markers in commit messages choose the bump for auto.
commit "Add mapper option [minor]"
assert_eq "[minor] marker" "1.11.0" "$("$SCRIPT" stable auto)"
assert_eq "eap follows marker" "1.11.0-eap.3" "$("$SCRIPT" eap)"
commit "Drop old settings [major]"
assert_eq "[major] marker wins" "2.0.0" "$("$SCRIPT" stable auto)"

# Pre-release tags are not stable releases.
git tag v2.0.0-eap.4
commit "More work"
assert_eq "eap tags are ignored" "2.0.0-eap.5" "$("$SCRIPT" eap)"

# A higher pluginVersion in gradle.properties raises the next version.
new_repo 1.10.0
git tag v1.10.0
echo "pluginVersion=3.0.0" > gradle.properties
git commit -q -am "Prepare 3.0"
assert_eq "gradle.properties raises version" "3.0.0" "$("$SCRIPT" stable auto)"
assert_eq "gradle.properties raises eap" "3.0.0-eap.1" "$("$SCRIPT" eap)"

# Change notes list commit subjects since the last stable tag, HTML escaped.
new_repo 1.10.0
git tag v1.10.0
commit "Fix <b> & \"quotes\""
commit "Add feature [minor]"
git checkout -q -b topic
commit "Topic work"
git checkout -q main
git merge -q --no-ff topic -m "Merge pull request #2"
assert_eq "change notes" '<ul><li>Topic work</li><li>Add feature [minor]</li><li>Fix &lt;b&gt; &amp; &quot;quotes&quot;</li></ul>' "$("$SCRIPT" notes)"

new_repo 1.10.0
git tag v1.10.0
assert_eq "change notes without commits" '<ul><li>Maintenance release</li></ul>' "$("$SCRIPT" notes)"

if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES test(s) failed"
  exit 1
fi
echo "all tests passed"
