#!/usr/bin/env bash
# GitHub-side checks before any build: public repo (the updater has no token), commit is on the default branch,
# and the tag is not already published. READ-ONLY: it never creates, edits or deletes anything on GitHub.
# Env: REPO, TAG, EVENT (push|workflow_dispatch), DEFAULT_BRANCH, REF_NAME, GITHUB_SHA, GH_TOKEN
set -euo pipefail
. "$(dirname "$0")/lib.sh"
need gh; need jq
REPO="${REPO:?}"; TAG="${TAG:?}"; EVENT="${EVENT:?}"; DEFAULT_BRANCH="${DEFAULT_BRANCH:?}"

# 1. The app asks GitHub for releases without logging in, so the repository must be public.
PRIVATE="$(gh api "repos/$REPO" --jq .private 2>/dev/null)" \
  || err "Couldn't read the repository" "The GitHub API call for $REPO failed (network or token). Re-run the workflow."
[ "$PRIVATE" = "false" ] || err "Repository is private" "$REPO is private. Flowly's updater reads releases without a login, so phones could never see updates. Make the repository public (Settings > General > Danger Zone) or change the update mechanism."

# 2. Only code that is on the default branch may be released.
if [ "$EVENT" = push ]; then
  git fetch --no-tags --quiet origin "$DEFAULT_BRANCH" 2>/dev/null || err "Couldn't fetch $DEFAULT_BRANCH" "git fetch origin $DEFAULT_BRANCH failed."
  git merge-base --is-ancestor "${GITHUB_SHA:?}" "origin/$DEFAULT_BRANCH" \
    || err "Tag is not on the $DEFAULT_BRANCH branch" "$TAG points at a commit that is not part of $DEFAULT_BRANCH. Merge your work into $DEFAULT_BRANCH first, then tag that commit."
else
  [ "${REF_NAME:-}" = "$DEFAULT_BRANCH" ] || err "Run from the wrong branch" "Releases are made from $DEFAULT_BRANCH only (this run is on '${REF_NAME:-?}'). Choose Branch: $DEFAULT_BRANCH when you click Run workflow."
fi

# 3. A published version is immutable: never publish a second APK under the same tag.
set +e; VIEW="$(gh release view "$TAG" --repo "$REPO" --json isDraft,assets 2>&1)"; rc=$?; set -e
if [ "$rc" -eq 0 ]; then
  if jq -e '[.assets[].name | select(endswith(".apk"))] | length > 0' <<<"$VIEW" >/dev/null; then
    err "Tag already released" "Release $TAG already has an APK. Published releases are never overwritten, because installed apps, checksums and the updater rely on them. Use the next version number. (If this release is broken, follow RELEASING.md > Recovery.)"
  fi
  note "A release object for $TAG exists without an APK (for example created in the GitHub web page); the APK will be attached to it."
elif ! grep -qi 'not found' <<<"$VIEW"; then
  err "Couldn't read releases" "gh release view failed: $(echo "$VIEW" | head -n1)"
fi

# Deliberately NOT checked here: whether the token may create releases. Proving that would require creating
# something (even a hidden draft), and preflight is strictly read-only. The publish job in release.yml declares
# "contents: write" for itself, which works regardless of the repository's default token setting.

note "GitHub checks passed (read-only): public repository, commit on $DEFAULT_BRANCH, $TAG unused. Release-creation permission is not tested by preflight."
