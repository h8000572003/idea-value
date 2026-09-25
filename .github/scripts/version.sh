#!/usr/bin/env bash
# Computes plugin versions and change notes from git history.
#
#   version.sh stable [auto|patch|minor|major]  next stable version, e.g. 1.10.1
#   version.sh eap                              EAP version for HEAD, e.g. 1.10.1-eap.4
#   version.sh notes                            HTML change notes since the last stable release
#
# The last stable release is the newest tag vX.Y.Z (tags with a suffix such as -eap are ignored).
# auto bumps the patch unless a commit since that tag contains [minor] or [major].
# pluginVersion in gradle.properties is a floor: raise it to force a version, e.g. for the first release.
set -euo pipefail

last_tag() {
  git describe --tags --abbrev=0 --match 'v[0-9]*' --exclude '*-*' 2>/dev/null || true
}

range() {
  local tag
  tag=$(last_tag)
  if [ -n "$tag" ]; then echo "$tag..HEAD"; else echo "HEAD"; fi
}

floor_version() {
  sed -n 's/^pluginVersion=\([0-9]*\.[0-9]*\.[0-9]*\).*/\1/p' gradle.properties
}

bump_kind() {
  if [ "$1" != "auto" ]; then
    echo "$1"
  elif git log --format=%B "$(range)" | grep -q '\[major\]'; then
    echo major
  elif git log --format=%B "$(range)" | grep -q '\[minor\]'; then
    echo minor
  else
    echo patch
  fi
}

max_version() {
  printf '%s\n%s\n' "$1" "$2" | sort -V | tail -n 1
}

next_stable() {
  local tag floor major minor patch next
  tag=$(last_tag)
  floor=$(floor_version)
  if [ -z "$tag" ]; then
    echo "$floor"
    return
  fi
  IFS=. read -r major minor patch <<< "${tag#v}"
  case "$(bump_kind "$1")" in
    major) next="$((major + 1)).0.0" ;;
    minor) next="$major.$((minor + 1)).0" ;;
    patch) next="$major.$minor.$((patch + 1))" ;;
    *) echo "unknown bump: $1" >&2; exit 2 ;;
  esac
  max_version "$next" "$floor"
}

change_notes() {
  local subjects
  subjects=$(git log --no-merges --topo-order --format=%s "$(range)")
  if [ -z "$subjects" ]; then
    subjects="Maintenance release"
  fi
  printf '<ul>'
  printf '%s\n' "$subjects" \
    | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g' \
    | while IFS= read -r subject; do printf '<li>%s</li>' "$subject"; done
  printf '</ul>\n'
}

case "${1:-}" in
  stable) next_stable "${2:-auto}" ;;
  eap) echo "$(next_stable auto)-eap.$(git rev-list --count "$(range)")" ;;
  notes) change_notes ;;
  *) echo "usage: $0 stable [auto|patch|minor|major] | eap | notes" >&2; exit 2 ;;
esac
