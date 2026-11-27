#!/usr/bin/env bash
# Decides WHICH version is being released and refuses anything that could go wrong later:
# malformed or out-of-range tags, reused versions, duplicate or lower versionCodes, versions the app already ships with.
#
# Inputs (env): EVENT = push | workflow_dispatch
#   push:               TAG      the pushed tag, e.g. v1.3.1
#   workflow_dispatch:  VERSION  optional explicit version, e.g. 1.4.0 ; otherwise BUMP = patch|minor|major
#   EXISTING_TAGS       newline-separated list of every tag/release already known (git tags + GitHub releases)
#   GRADLE_FILE         path to app/build.gradle.kts (default app/build.gradle.kts), used for the built-in default versionCode
# Outputs: tag, name, code, previous_tag, previous_code
set -euo pipefail
. "$(dirname "$0")/lib.sh"

EVENT="${EVENT:?EVENT is required}"
SELF=""; [ "$EVENT" = push ] && SELF="${TAG:?TAG is required for a push event}"

max_code=0; max_tag=""; declare -A seen=()
while IFS= read -r t; do
  [ -n "$t" ] || continue
  [ "$t" = "$SELF" ] && continue
  c="$(version_code "$t" 2>/dev/null)" || continue          # ignore tags that are not release versions
  seen["$c"]="$t"
  if (( c > max_code )); then max_code="$c"; max_tag="$t"; fi
done <<< "${EXISTING_TAGS:-}"

case "$EVENT" in
  push) NEW="$TAG" ;;
  workflow_dispatch)
    if [ -n "${VERSION:-}" ]; then
      NEW="v${VERSION#v}"
    else
      [ -n "$max_tag" ] || err "No earlier release to bump" "There is no earlier version tag, so there is nothing to add 1 to. Run the workflow with an explicit version (for example 1.2.0)."
      re='^v?([0-9]+)\.([0-9]+)\.([0-9]+)$'; [[ "$max_tag" =~ $re ]]
      a="${BASH_REMATCH[1]}"; b="${BASH_REMATCH[2]}"; c="${BASH_REMATCH[3]}"
      case "${BUMP:-patch}" in
        major) a=$((a + 1)); b=0; c=0 ;;
        minor) b=$((b + 1)); c=0 ;;
        patch) c=$((c + 1)) ;;
        *) err "Unknown bump" "BUMP must be patch, minor or major (got '${BUMP:-}')." ;;
      esac
      NEW="v$a.$b.$c"
    fi ;;
  *) err "Unsupported trigger" "Releases are created only from a pushed v-tag or a manual run (got '$EVENT')." ;;
esac

[[ "$NEW" == v* ]] || err "Invalid release tag" "'$NEW' must start with v, for example v1.3.1."
set +e; CODE="$(version_code "$NEW")"; rc=$?; set -e
[ "$rc" -ne 1 ] || err "Invalid release tag" "'$NEW' is not a valid release tag. Use v<major>.<minor>.<patch>, for example v1.3.1 (no suffixes such as -beta, no leading zeros)."
[ "$rc" -ne 2 ] || err "Version out of range" "'$NEW': major must be 0-999 and minor and patch 0-99, otherwise versionCode (major*10000 + minor*100 + patch) would stop increasing."
NAME="${NEW#v}"

if [ -n "${seen[$CODE]:-}" ]; then
  err "Version already released" "Version $NAME already exists as tag '${seen[$CODE]}' (versionCode $CODE). A version is released once and never re-used: pick the next number."
fi
if (( CODE <= max_code )); then
  err "Older version" "$NEW (versionCode $CODE) is not higher than the already released $max_tag (versionCode $max_code). Versions may only go up, or installed apps will refuse the update."
fi

GRADLE_FILE="${GRADLE_FILE:-app/build.gradle.kts}"
FLOOR="$(sed -n 's/.*VERSION_CODE")[^"]*"\([0-9][0-9]*\)".*/\1/p' "$GRADLE_FILE" 2>/dev/null | head -n1 || true)"
if [ -n "$FLOOR" ]; then
  (( CODE > FLOOR )) || err "Below the app's built-in version" "$NEW (versionCode $CODE) is not above the default versionCode $FLOOR that local and test builds carry, so phones with such a build could not update to it. Use a higher version."
else
  warn "Version floor not checked" "Could not read the default versionCode from $GRADLE_FILE."
fi

note "Release $NEW -> versionName $NAME, versionCode $CODE (previous: ${max_tag:-none})"
out tag "$NEW"; out name "$NAME"; out code "$CODE"; out previous_tag "$max_tag"; out previous_code "$max_code"
