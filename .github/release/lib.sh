#!/usr/bin/env bash
# Shared helpers for the Flowly release scripts. Nothing here ever prints a secret value.

# err "Title" "What went wrong and how to fix it"  -> GitHub error annotation, then stop.
err()  { echo "::error title=$1::$2"; exit 1; }
warn() { echo "::warning title=$1::$2"; }
note() { echo "$*"; }
out()  { if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "$1=$2" >> "$GITHUB_OUTPUT"; fi; }
need() { command -v "$1" >/dev/null 2>&1 || err "Missing tool" "'$1' is not available on this machine."; }

# The ONE place that maps a version to a versionCode: major*10000 + minor*100 + patch.
# Prints the code. Return 1 = not a clean X.Y.Z (a leading v is optional), 2 = a part is out of range.
version_code() {
  local re='^v?(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
  [[ "$1" =~ $re ]] || return 1
  local a="${BASH_REMATCH[1]}" b="${BASH_REMATCH[2]}" c="${BASH_REMATCH[3]}"
  (( a <= 999 && b <= 99 && c <= 99 )) || return 2
  echo $(( a * 10000 + b * 100 + c ))
}

# Lower-case hex without colons/spaces, for comparing fingerprints.
norm_hex() { printf '%s' "$1" | tr -d ': \r\n' | tr 'A-F' 'a-f'; }

# Looser reading of an EXISTING tag, used only to detect reuse: also counts malformed twins such as V1.2.0,
# V.1.2.0 or v.1.2.0 as "this version is taken". Same return codes as version_code.
lenient_version_code() {
  local re='^[vV]?\.?(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
  [[ "$1" =~ $re ]] || return 1
  local a="${BASH_REMATCH[1]}" b="${BASH_REMATCH[2]}" c="${BASH_REMATCH[3]}"
  (( a <= 999 && b <= 99 && c <= 99 )) || return 2
  echo $(( a * 10000 + b * 100 + c ))
}

# True when $1 is exactly 64 hex digits (a SHA-256), in either case.
is_sha256() { [[ "$1" =~ ^[0-9a-fA-F]{64}$ ]]; }
