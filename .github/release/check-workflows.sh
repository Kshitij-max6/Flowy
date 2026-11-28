#!/usr/bin/env bash
# Enforces the workflow safety policy on the repository's own workflow files (no secrets, no network):
#   - every workflow declares explicit top-level permissions
#   - "contents: write" exists ONLY on the publish job of release.yml
#   - preflight.yml is read-only (contents: read, no mutating commands)
#   - anything that creates/edits/deletes releases or tags, or pushes, lives only in release.yml
#   - release.yml starts only from a strict version tag (GitHub glob, NOT a regex) or a manual run
#   - signing secrets are referenced only by release.yml and preflight.yml
#   - no pull_request_target, no "secrets: inherit"
# Env: WF_DIR (default .github/workflows), REL_DIR (default .github/release)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
WF="${WF_DIR:-.github/workflows}"; RD="${REL_DIR:-.github/release}"
[ -d "$WF" ] || err "Workflows missing" "$WF was not found."
for f in release.yml preflight.yml; do [ -f "$WF/$f" ] || err "Workflow missing" "$WF/$f was not found."; done

# The one tag filter allowed. GitHub tag filters are GLOB patterns (not regular expressions): '+' repeats the previous
# character or class, '.' is a literal dot, '*' matches any run of characters. 'v*' would also fire for v1.2.3-beta.
STRICT_TAG='v[0-9]+.[0-9]+.[0-9]+'

shopt -s nullglob
for f in "$WF"/*.yml "$WF"/*.yaml; do
  b="$(basename "$f")"
  grep -Eq '^permissions:' "$f" || err "Permissions not explicit" "$b has no top-level 'permissions:' block, so it would inherit the repository default token rights."
  grep -Eq 'pull_request_target' "$f" && err "Dangerous trigger" "$b uses pull_request_target."
  grep -Eq 'secrets:[[:space:]]*inherit' "$f" && err "Secrets inherited" "$b uses 'secrets: inherit'. Pass the needed secrets by name."
  if [ "$b" != release.yml ]; then
    if grep -Eq 'contents:[[:space:]]*write' "$f"; then err "Write permission outside release" "$b requests 'contents: write'. Only the publish job of release.yml may."; fi
    if grep -Eq 'softprops/action-gh-release|gh release (create|delete|edit|upload)|git push|git tag +[^ -]|gh api +(-X|--method)[ =]*(POST|DELETE|PATCH|PUT)|releases/latest.*-X' "$f"; then
      err "Publishing outside release.yml" "$b contains a command that creates, edits or deletes releases/tags or pushes. Publishing is isolated to release.yml."
    fi
  fi
  if [ "$b" != release.yml ] && [ "$b" != preflight.yml ]; then
    if grep -Eq 'secrets\.(KEYSTORE_BASE64|STORE_PASSWORD|KEY_PASSWORD)' "$f"; then err "Signing secret in the wrong workflow" "$b references a release signing secret."; fi
  fi
done

# preflight.yml: read-only.
grep -Eq '^[[:space:]]*contents:[[:space:]]*read' "$WF/preflight.yml" || err "Preflight is not read-only" "preflight.yml must declare 'contents: read'."
grep -Eq 'contents:[[:space:]]*write' "$WF/preflight.yml" && err "Preflight is not read-only" "preflight.yml requests 'contents: write'."

# Release scripts used by preflight must not mutate GitHub either (only release.yml publishes).
for s in check-github.sh check-continuity.sh check-signing.sh check-static.sh plan-version.sh; do
  [ -f "$RD/$s" ] || continue
  if grep -Ev '^[[:space:]]*#' "$RD/$s" | grep -Eq 'gh +api +(-X|--method)|gh +release +(create|delete|edit|upload)|git +push|git +tag +[^ -]'; then
    err "Preflight script mutates GitHub" "$s contains a command that creates, edits or deletes something on GitHub. Preflight scripts are read-only."
  fi
done

# release.yml: tag filter and the single write permission.
R="$WF/release.yml"
TAGS="$(awk '/^on:/{on=1;next} on&&/^[^ #]/{on=0} on&&/^    tags:/{t=1;next} t&&/^[[:space:]]*#/{next} t&&/^      - /{sub(/^      - /,"");gsub(/[\047" ]/,"");print;next} t&&!/^      - /{t=0}' "$R")"
[ -n "$TAGS" ] || err "Release has no tag filter" "release.yml must start from the tag filter '$STRICT_TAG'."
[ "$TAGS" = "$STRICT_TAG" ] || err "Release tag filter is too loose" "release.yml tag filter is '$(echo "$TAGS" | tr '\n' ' ')' but must be exactly '$STRICT_TAG' (a GitHub glob, not a regular expression; 'v*' would also start on v1.2.3-beta)."
grep -Eq '^[[:space:]]*(pull_request|schedule):' "$R" && err "Release trigger" "release.yml must start only from a tag or a manual run."
top="$(awk '/^permissions:/{p=1;next} p&&/^[^ ]/{p=0} p' "$R")"
grep -Eq 'write' <<<"$top" && err "Release default token is writable" "The top-level permissions of release.yml must be read-only; only the publish job may write."
WRITES="$(grep -Ec 'contents:[[:space:]]*write' "$R" || true)"
[ "$WRITES" = 1 ] || err "Write permission count" "release.yml must contain exactly one 'contents: write' (the publish job); found $WRITES."
awk '/^  publish:/{j=1} /^  [a-z_-]+:/&&!/^  publish:/{j=0} j&&/contents:[[:space:]]*write/{f=1} END{exit f?0:1}' "$R" \
  || err "Write permission misplaced" "'contents: write' in release.yml is not inside the 'publish' job."
for j in preflight build; do
  awk -v j="$j" '$0 ~ "^  "j":"{x=1;next} /^  [a-z_-]+:/{x=0} x&&/contents:[[:space:]]*write/{f=1} END{exit f?1:0}' "$R" \
    || err "Write permission misplaced" "The '$j' job of release.yml must not have 'contents: write'."
done
# Signing secrets must not reach the publish job (it only uploads already-verified files).
if awk '/^  publish:/{j=1;next} /^  [a-z_-]+:/{j=0} j' "$R" | grep -Eq 'secrets\.(KEYSTORE_BASE64|STORE_PASSWORD|KEY_PASSWORD)'; then
  err "Signing secret in publish job" "The publish job of release.yml references a signing secret; only the build job may."
fi

note "Workflow policy passed: explicit permissions, read-only preflight, strict tag filter ($STRICT_TAG), write access only on the publish job, no signing secrets outside release/preflight."
