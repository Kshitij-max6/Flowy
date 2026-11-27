#!/usr/bin/env bash
# Signing-identity and version continuity against the most recent published release:
#  - its APK must be signed by the SAME certificate as the key restored for this release
#  - the new versionCode must be higher than that APK's real versionCode
#  - its published checksum must match its APK
# Env: REPO, TAG, CERT, NEW_CODE, BT (build-tools dir with apksigner + aapt2), GH_TOKEN, ACCEPT_SIGNER_CHANGE_TO (optional)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
need gh; need jq
REPO="${REPO:?}"; TAG="${TAG:?}"; CERT="$(norm_hex "${CERT:?}")"; NEW_CODE="${NEW_CODE:?}"; BT="${BT:?}"
[ -x "$BT/apksigner" ] && [ -x "$BT/aapt2" ] || err "Build tools missing" "apksigner/aapt2 were not found in $BT."

tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
LIST="$(gh release list --repo "$REPO" --limit 50 --json tagName,isDraft,isPrerelease 2>/dev/null)" \
  || err "Couldn't list releases" "gh release list failed for $REPO. Re-run the workflow."

best=""; best_code=0
while IFS= read -r t; do
  [ -n "$t" ] && [ "$t" != "$TAG" ] || continue
  c="$(version_code "$t" 2>/dev/null)" || continue
  if (( c > best_code )); then best="$t"; best_code="$c"; fi
done < <(jq -r '.[] | select((.isDraft|not) and (.isPrerelease|not)) | .tagName' <<<"$LIST")

if [ -z "$best" ]; then
  note "No earlier published release, so key continuity cannot be compared yet. After this release, pin EXPECTED_CERT_SHA256 (RELEASING.md)."
  out previous_apk_code ""; exit 0
fi

gh release download "$best" --repo "$REPO" --dir "$tmp" --pattern '*.apk' --pattern '*.sha256' >/dev/null 2>&1 \
  || err "Couldn't download the previous release" "Release $best could not be downloaded for comparison."
shopt -s nullglob; apks=("$tmp"/*.apk)
[ "${#apks[@]}" -eq 1 ] || err "Previous release is unusual" "Release $best has ${#apks[@]} APK files; expected exactly one."
PREV="${apks[0]}"

if sums=("$tmp"/*.sha256) && [ "${#sums[@]}" -ge 1 ]; then
  want="$(cut -d' ' -f1 "${sums[0]}" | head -n1 | tr 'A-F' 'a-f')"; have="$(sha256sum "$PREV" | cut -d' ' -f1)"
  [ "$want" = "$have" ] || warn "Previous release checksum is wrong" "The .sha256 published with $best does not match its APK. Phones updating from an older version to $best are being refused; consider re-publishing it."
else
  warn "Previous release has no checksum" "$best has no .sha256 file."
fi

PREV_CERT="$("$BT/apksigner" verify --print-certs "$PREV" 2>/dev/null | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n1 | tr 'A-F' 'a-f')" || true
[ -n "$PREV_CERT" ] || err "Previous release not verifiable" "apksigner could not read the signature of $best."
if [ "$PREV_CERT" != "$CERT" ]; then
  if [ -n "${ACCEPT_SIGNER_CHANGE_TO:-}" ] && [ "$(norm_hex "$ACCEPT_SIGNER_CHANGE_TO")" = "$CERT" ]; then
    warn "Signing identity change accepted" "Release $best was signed by $PREV_CERT; this release uses $CERT because ACCEPT_SIGNER_CHANGE_TO says so. Existing installs must be reinstalled (RELEASING.md > Recovery)."
  else
    err "Signing key changed" "The previous release $best is signed by certificate $PREV_CERT but the key restored for this release is $CERT. Phones with $best installed would REFUSE this update. Restore your original key into the KEYSTORE_BASE64 secret. If you really intend to change identity, set the repository variable ACCEPT_SIGNER_CHANGE_TO to $CERT (RELEASING.md > Recovery)."
  fi
else
  note "Signing identity continues: same certificate as $best."
fi

PREV_PKG="$("$BT/aapt2" dump badging "$PREV" 2>/dev/null | grep -m1 '^package:' || true)"
PREV_VC="$(printf '%s' "$PREV_PKG" | sed -n "s/.* versionCode='\([^']*\)'.*/\1/p")"
[ -n "$PREV_VC" ] || err "Previous release not readable" "aapt2 could not read the version of $best."
(( NEW_CODE > PREV_VC )) || err "versionCode not increasing" "This release's versionCode $NEW_CODE is not higher than $best's real versionCode $PREV_VC. Android refuses such an update. Use a higher version."
note "Version continues: $NEW_CODE > $PREV_VC ($best)."
out previous_apk_code "$PREV_VC"
