#!/usr/bin/env bash
# Verifies the APK that was actually built, before it is published. Prints only public values.
# Env: APK, WANT_NAME, WANT_CODE, KEYSTORE_CERT, UPDATE_REPO, BT (build-tools dir), APP_ID, TAG,
#      EXPECTED_CERT_SHA256 (optional)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
APK="${APK:?}"; BT="${BT:?}"; APP_ID="${APP_ID:-com.kshitij.flowly}"
bad() { err "APK verification failed" "$1"; }
[ -f "$APK" ] || bad "$APK does not exist."
[ -x "$BT/aapt2" ] && [ -x "$BT/apksigner" ] || bad "aapt2/apksigner were not found in $BT."
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

BADGING="$("$BT/aapt2" dump badging "$APK")"
PKG="$(printf '%s\n' "$BADGING" | grep -m1 '^package:' || true)"
GOT_ID="$(printf '%s' "$PKG"   | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
GOT_CODE="$(printf '%s' "$PKG" | sed -n "s/.* versionCode='\([^']*\)'.*/\1/p")"
GOT_NAME="$(printf '%s' "$PKG" | sed -n "s/.* versionName='\([^']*\)'.*/\1/p")"
note "APK says: package=$GOT_ID versionName=$GOT_NAME versionCode=$GOT_CODE"
[ "$GOT_ID"   = "$APP_ID" ]          || bad "applicationId is '$GOT_ID', expected '$APP_ID'."
[ "$GOT_NAME" = "${WANT_NAME:?}" ]   || bad "versionName is '$GOT_NAME', expected '$WANT_NAME' (from ${TAG:-the tag})."
[ "$GOT_CODE" = "${WANT_CODE:?}" ]   || bad "versionCode is '$GOT_CODE', expected '$WANT_CODE' (from ${TAG:-the tag})."
if printf '%s\n' "$BADGING" | grep -q 'application-debuggable'; then bad "The APK is debuggable; only release builds may be published."; fi

"$BT/apksigner" verify --verbose --print-certs "$APK" > "$tmp/sig.txt" || bad "apksigner could not verify the APK signature."
grep -Eq 'Verified using v(2|3) scheme .*: true' "$tmp/sig.txt" || bad "The APK has no valid v2/v3 signature."
[ "$(grep -c 'certificate SHA-256 digest' "$tmp/sig.txt")" -eq 1 ] || bad "The APK must have exactly one signer."
APK_CERT="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$tmp/sig.txt" | tr 'A-F' 'a-f')"
note "APK signer certificate SHA-256 (public value): $APK_CERT"
[ "$APK_CERT" = "$(norm_hex "${KEYSTORE_CERT:?}")" ] || bad "The APK is not signed with the restored release key."
if [ -n "${EXPECTED_CERT_SHA256:-}" ]; then
  [ "$APK_CERT" = "$(norm_hex "$EXPECTED_CERT_SHA256")" ] || bad "The APK signer does not match EXPECTED_CERT_SHA256."
fi

unzip -p "$APK" 'classes*.dex' | grep -a -F -q -- "${UPDATE_REPO:?}" \
  || bad "The APK does not contain the update repository $UPDATE_REPO, so its in-app updater could not work."
note "Verified: package, version, release build, signature, signing identity and update repository."
