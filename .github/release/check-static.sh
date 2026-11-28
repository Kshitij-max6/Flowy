#!/usr/bin/env bash
# Checks the repository itself (no secrets, no network): application id, release build configuration,
# version wiring, updater repository, asset naming contract, and that no signing key is committed.
# Env: REPO (owner/repo), APP_ID (default com.kshitij.flowly), KEY_ALIAS (default upload)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
APP_ID="${APP_ID:-com.kshitij.flowly}"; KEY_ALIAS="${KEY_ALIAS:-upload}"; REPO="${REPO:?REPO is required}"
G="${GRADLE_FILE:-app/build.gradle.kts}"; PROPS="${PROPS_FILE:-gradle.properties}"
UPDATER="${UPDATER_FILE:-app/src/main/java/com/example/core/update/AppUpdateManager.kt}"
MANIFEST="${MANIFEST_FILE:-app/src/main/AndroidManifest.xml}"

for t in keytool jq sha256sum unzip perl base64 git; do need "$t"; done
for f in "$G" "$PROPS" "$UPDATER" "$MANIFEST"; do [ -f "$f" ] || err "Project file missing" "$f was not found. Is the workflow running on the right repository?"; done

grep -Eq "applicationId[[:space:]]*=[[:space:]]*\"$APP_ID\"" "$G" \
  || err "Application ID changed" "applicationId in $G is not \"$APP_ID\". Changing it makes Android treat the app as a different app, so nobody could update. Restore it."

perl -0777 -ne 'exit(/release\s*\{[^}]*signingConfig\s*=\s*signingConfigs\.getByName\("release"\)/ ? 0 : 1)' "$G" \
  || err "Release build not signed with the release key" "The release build type in $G must use signingConfigs.getByName(\"release\")."
perl -0777 -ne 'exit(/release\s*\{[^}]*isDebuggable\s*=\s*true/ ? 0 : 1)' "$G" \
  && err "Release build is debuggable" "The release build type in $G sets isDebuggable = true. Remove it."
grep -Eq "keyAlias[[:space:]]*=[[:space:]]*\"$KEY_ALIAS\"" "$G" \
  || err "Signing alias mismatch" "$G does not use key alias \"$KEY_ALIAS\", which is what the permanent key and the workflow expect."
for v in KEYSTORE_PATH STORE_PASSWORD KEY_PASSWORD VERSION_CODE VERSION_NAME; do
  grep -q "System.getenv(\"$v\")" "$G" || err "Build wiring changed" "$G no longer reads $v from the environment, so the release workflow could not set the version or signing key."
done
grep -q 'buildConfigField("String", "UPDATE_REPO"' "$G" || err "Updater repository not wired" "$G no longer writes BuildConfig.UPDATE_REPO."

PROP_REPO="$(sed -n 's/^flowly\.updateRepo=//p' "$PROPS" | tr -d '\r ' | head -n1)"
[ -n "$PROP_REPO" ] || err "Updater repository missing" "flowly.updateRepo is not set in $PROPS."
if [ "${PROP_REPO,,}" != "${REPO,,}" ]; then
  err "Updater repository differs from this repository" "flowly.updateRepo in $PROPS is '$PROP_REPO' but this workflow runs in '$REPO'. If you renamed or moved the repository, update flowly.updateRepo. Otherwise installed apps would look for updates in the wrong place."
fi

grep -qF 'name.endsWith(".apk", true)' "$UPDATER" \
  || err "Asset naming contract changed" "$UPDATER no longer selects APK assets by the .apk suffix. The release workflow publishes a versioned APK asset; keep the updater's APK selection in step."
grep -qF 'Flowly-v$tag.apk' "$UPDATER" \
  || err "Asset naming contract changed" "$UPDATER no longer requires the exact asset name Flowly-v<tag>.apk (what finalize-assets.sh publishes). Keep the updater and the release scripts in step."
grep -q 'fileprovider' "$MANIFEST" || err "FileProvider missing" "$MANIFEST has no fileprovider entry, so the updater could not hand the APK to the installer."

tracked="$(git ls-files 2>/dev/null | grep -Ei '\.(jks|keystore|p12|pfx)$|keystore\.properties$' | grep -v '^debug\.keystore$' || true)"
[ -z "$tracked" ] || err "A signing key file is committed" "Remove these from git (and treat the key as exposed, see RELEASING.md > Recovery): $(echo "$tracked" | tr '\n' ' ')"

note "Static checks passed: applicationId, release signing config, version wiring, updater repository ($PROP_REPO), asset naming, no committed keys."
