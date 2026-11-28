#!/usr/bin/env bash
# Restores the permanent release key from GitHub secrets and proves it works, WITHOUT ever printing a secret.
# Never creates, replaces or rotates a key. Env: KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD (secrets);
# KEY_ALIAS (default upload); EXPECTED_CERT_SHA256 (REQUIRED pin, variable not secret); KEEP_KEYSTORE=1 keeps the file for the build step.
# Outputs: cert (SHA-256 of the signing certificate: public information).
set -euo pipefail
. "$(dirname "$0")/lib.sh"
KEY_ALIAS="${KEY_ALIAS:-upload}"; TMP="${RUNNER_TEMP:-/tmp}"

missing=()
[ -n "${KEYSTORE_BASE64:-}" ] || missing+=(KEYSTORE_BASE64)
[ -n "${STORE_PASSWORD:-}" ]  || missing+=(STORE_PASSWORD)
[ -n "${KEY_PASSWORD:-}" ]    || missing+=(KEY_PASSWORD)
if [ "${#missing[@]}" -gt 0 ]; then
  err "Release signing secrets are missing" "Add these repository secrets (Settings > Secrets and variables > Actions > Secrets tab): ${missing[*]}. Nothing was built. Steps: RELEASING.md > One-time setup."
fi
for t in keytool base64 sha256sum; do need "$t"; done

umask 077
KS="$TMP/flowly-release.jks"
fail() { rm -f "$KS" "$TMP/check.csr"; err "Release keystore problem" "$1"; }

printf '%s' "$KEYSTORE_BASE64" | tr -d ' \r\n' | base64 -d > "$KS" 2>/dev/null \
  || fail "KEYSTORE_BASE64 is not valid base64. Re-create it from the .jks file (RELEASING.md) and paste the whole value."
[ -s "$KS" ] || fail "KEYSTORE_BASE64 decoded to an empty file."

keytool -J-Duser.language=en -list -keystore "$KS" -storepass:env STORE_PASSWORD -alias "$KEY_ALIAS" >/dev/null 2>&1 \
  || fail "The keystore could not be opened with STORE_PASSWORD, or it has no key with alias '$KEY_ALIAS'."

TYPE="$(keytool -J-Duser.language=en -list -keystore "$KS" -storepass:env STORE_PASSWORD 2>/dev/null | sed -n 's/^Keystore type: //p' | head -n1)"
if [ "$TYPE" = "PKCS12" ] && [ "$KEY_PASSWORD" != "$STORE_PASSWORD" ]; then
  fail "This is a PKCS12 keystore, where the key password must equal the store password. Set KEY_PASSWORD to the same value as STORE_PASSWORD."
fi
keytool -J-Duser.language=en -certreq -alias "$KEY_ALIAS" -keystore "$KS" \
  -storepass:env STORE_PASSWORD -keypass:env KEY_PASSWORD -file "$TMP/check.csr" >/dev/null 2>&1 \
  || fail "KEY_PASSWORD does not unlock the '$KEY_ALIAS' key."
rm -f "$TMP/check.csr"

CERT="$(keytool -exportcert -alias "$KEY_ALIAS" -keystore "$KS" -storepass:env STORE_PASSWORD | sha256sum | cut -d' ' -f1)"
is_sha256 "$CERT" || fail "Could not compute the certificate fingerprint of the '$KEY_ALIAS' key."
note "Signing certificate SHA-256 (public value): $CERT"

# A release must never be signed with an Android debug-style certificate (that is what CI generates for debug builds).
OWNER="$(keytool -J-Duser.language=en -list -v -alias "$KEY_ALIAS" -keystore "$KS" -storepass:env STORE_PASSWORD 2>/dev/null | sed -n 's/^Owner: //p' | head -n1 || true)"
case "$OWNER" in
  *"CN=Android Debug"*) fail "The '$KEY_ALIAS' key has an Android debug certificate ($OWNER). Debug keys are throw-away and must never sign a release." ;;
esac

# The signing identity must be pinned. Without a pin, any key that opens would be accepted, including a freshly
# generated one that no installed app trusts. Pinning is a deliberate human decision (RELEASING.md > Signing identity).
if [ -z "${EXPECTED_CERT_SHA256:-}" ]; then
  fail "The signing identity is not pinned, so this key cannot be approved. Only after you have confirmed that this is the key installed apps trust (or that you accept a one-time reinstall, RELEASING.md > Signing identity), set the repository VARIABLE (not secret) EXPECTED_CERT_SHA256 to: $CERT"
fi
WANT_PIN="$(norm_hex "$EXPECTED_CERT_SHA256")"
is_sha256 "$WANT_PIN" || fail "The repository variable EXPECTED_CERT_SHA256 is not a 64-digit SHA-256 fingerprint (colons and upper case are fine)."
[ "$CERT" = "$WANT_PIN" ] \
  || fail "The restored key's certificate ($CERT) does not match the repository variable EXPECTED_CERT_SHA256. This is not your usual signing key, so installed apps would refuse the update. Nothing was built. Do not change the variable to make this pass unless you intend to change identity (RELEASING.md > Recovery)."
note "Certificate matches EXPECTED_CERT_SHA256."

out cert "$CERT"
if [ "${KEEP_KEYSTORE:-0}" = 1 ]; then
  [ -z "${GITHUB_ENV:-}" ] || echo "KEYSTORE_PATH=$KS" >> "$GITHUB_ENV"
else
  rm -f "$KS"
fi
