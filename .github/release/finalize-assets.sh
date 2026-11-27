#!/usr/bin/env bash
# Picks the single signed release APK, gives it the name the updater expects (Flowly-vX.Y.Z.apk) and writes the
# SHA-256 file from that final file. Outputs: file
set -euo pipefail
. "$(dirname "$0")/lib.sh"
TAG="${TAG:?}"; DIR="${APK_DIR:-app/build/outputs/apk/release}"
shopt -s nullglob; apks=("$DIR"/*.apk)
[ "${#apks[@]}" -eq 1 ] || err "Unexpected build output" "Expected exactly one release APK in $DIR, found ${#apks[@]}."
case "${apks[0]}" in *unsigned*|*debug*) err "Wrong APK" "The build produced '${apks[0]}', which is not a signed release APK." ;; esac
OUT="Flowly-${TAG}.apk"
[[ "$OUT" =~ ^Flowly-v[0-9]+\.[0-9]+\.[0-9]+\.apk$ ]] || err "Asset name" "'$OUT' is not Flowly-vX.Y.Z.apk, the name the in-app updater looks for."
cp "${apks[0]}" "$OUT"
sha256sum "$OUT" > "$OUT.sha256"
sha256sum -c "$OUT.sha256" >/dev/null || err "Checksum" "The checksum written for $OUT does not match it."
[ "$(cut -d' ' -f1 "$OUT.sha256" | tr -d '\n' | wc -c)" -eq 64 ] || err "Checksum" "The checksum file is not a 64-digit SHA-256."
note "Prepared $OUT and $OUT.sha256"
out file "$OUT"
