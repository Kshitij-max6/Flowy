#!/usr/bin/env bash
# Self-test for the release scripts. Simulates good and bad release situations; touches no real GitHub data and uses
# throwaway keys created here. Run:  bash .github/release/tests/run-tests.sh
# Needs: bash, keytool (any JDK), jq, git, zip, perl.
set -uo pipefail

# Prerequisite gate: a missing tool must produce ONE clear "BLOCKED" message, never dozens of misleading test failures.
MISSING=()
for t in bash keytool jq git zip unzip perl sha256sum base64 tr sed awk; do command -v "$t" >/dev/null 2>&1 || MISSING+=("$t"); done
if [ "${#MISSING[@]}" -gt 0 ]; then
  printf 'BLOCKED: the release self-test did not run, because these tools are missing: %s\n' "${MISSING[*]}"
  printf 'This is NOT a test failure. Nothing was checked. Run it where they exist: GitHub Actions (workflow "Release scripts self-test"),\n'
  printf 'WSL/Ubuntu (sudo apt install default-jdk-headless jq zip unzip perl), or Git Bash after adding the missing tools.\n'
  printf '(keytool comes with any JDK; jq, zip and unzip are not part of Git for Windows by default.)\n'
  exit 2
fi
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"; R="$ROOT/.github/release"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
export RUNNER_TEMP="$T/rt"; mkdir -p "$RUNNER_TEMP" "$T/bin"
PASS=0; FAIL=0; FAILED=()

# case_ NAME ok|fail 'regex that must appear in the output' command...
case_() {
  local name="$1" want="$2" pat="$3"; shift 3
  local o rc; o="$("$@" 2>&1)"; rc=$?
  local ok=1
  if [ "$want" = ok ]; then [ $rc -eq 0 ] || ok=0; else [ $rc -ne 0 ] || ok=0; fi
  [ -z "$pat" ] || grep -Eq -- "$pat" <<<"$o" || ok=0
  if [ $ok -eq 1 ]; then PASS=$((PASS+1)); printf '  ok    %s\n' "$name"
  else FAIL=$((FAIL+1)); FAILED+=("$name"); printf '  FAIL  %s (rc=%s, wanted %s /%s/)\n        %s\n' "$name" "$rc" "$want" "$pat" "$(head -c 400 <<<"$o" | tr '\n' ' ')"; fi
}
section() { printf '\n== %s\n' "$1"; }

# Every helper below runs the script under test with a default environment, then the caller's NAME=value overrides
# (the later assignment wins).

# ---------------------------------------------------------------- plan-version
section "plan-version: tags, versions, reuse, order"
G="$ROOT/app/build.gradle.kts"
pv() { # EVENT TAG BUMP VERSION EXISTING_TAGS
  env EVENT="$1" TAG="${2:-}" BUMP="${3:-}" VERSION="${4:-}" EXISTING_TAGS="${5:-}" GRADLE_FILE="$G" GITHUB_OUTPUT="$T/pv_out" bash "$R/plan-version.sh"; }
: > "$T/pv_out"
case_ "v1.3.1 maps to 1.3.1 / 10301"        ok   'versionName 1.3.1, versionCode 10301' pv push v1.3.1 "" "" $'v1.3.0\nv1.2.0'
case_ "outputs are written"                 ok   '' bash -c "grep -q '^code=10301' $T/pv_out && grep -q '^name=1.3.1' $T/pv_out && grep -q '^tag=v1.3.1' $T/pv_out"
for bad in v1.3 1.3.1 v1.3.1-beta v01.2.3 v1.3.1.4 vx.y.z; do
  case_ "rejects malformed tag $bad"        fail 'Invalid release tag' pv push "$bad" "" "" ""
done
case_ "rejects minor 100"                   fail 'out of range' pv push v1.100.0 "" "" ""
case_ "rejects patch 100"                   fail 'out of range' pv push v1.2.100 "" "" ""
case_ "rejects major 1000"                  fail 'out of range' pv push v1000.0.0 "" "" ""
case_ "rejects an older version"            fail 'Older version' pv push v1.2.0 "" "" $'v1.3.0'
case_ "rejects a reused version (twin tag without v)" fail 'already released' pv push v1.3.0 "" "" $'1.3.0'
case_ "rejects a reused version (twin tag V.1.3.0)" fail 'already released' pv push v1.3.0 "" "" $'V.1.3.0'
case_ "rejects a reused version (twin tag V1.3.0)"  fail 'already released' pv push v1.3.0 "" "" $'V1.3.0'
case_ "the repo's real tag set still allows the next patch" ok 'versionCode 10302' pv push v1.3.2 "" "" $'V.1.2.0\nv1.2.0\nv1.3.0\nv1.3.1'
case_ "a tag does not collide with itself"  ok   'versionCode 10300' pv push v1.3.0 "" "" $'v1.3.0\nv1.2.0'
case_ "ignores non-release tags"            ok   'versionCode 10301' pv push v1.3.1 "" "" $'nightly\nlatest\nv1.3.0\nfoo-1'
case_ "rejects the app's built-in default (1.1.0)" fail 'built-in version' pv push v1.1.0 "" "" ""
case_ "accepts the first version above it"  ok   'versionCode 10101' pv push v1.1.1 "" "" ""
case_ "dispatch: patch bump"                ok   'Release v1.3.2' pv workflow_dispatch "" patch "" $'v1.3.1\nv1.2.0'
case_ "dispatch: minor bump"                ok   'Release v1.4.0' pv workflow_dispatch "" minor "" $'v1.3.1'
case_ "dispatch: major bump"                ok   'Release v2.0.0' pv workflow_dispatch "" major "" $'v1.3.1'
case_ "dispatch: explicit version"          ok   'Release v1.5.0' pv workflow_dispatch "" patch "1.5.0" $'v1.3.1'
case_ "dispatch: explicit lower version"    fail 'Older version' pv workflow_dispatch "" patch "1.2.0" $'v1.3.1'
case_ "dispatch: explicit existing version" fail 'already released' pv workflow_dispatch "" patch "v1.3.1" $'v1.3.1'
case_ "dispatch: nothing to bump"           fail 'No earlier release' pv workflow_dispatch "" patch "" ""
case_ "dispatch: unknown bump"              fail 'Unknown bump' pv workflow_dispatch "" huge "" $'v1.3.1'
case_ "unsupported trigger"                 fail 'Unsupported trigger' pv schedule "" "" "" ""

# ---------------------------------------------------------------- check-static
section "check-static: repository configuration"
P="$T/proj"; mkdir -p "$P"
(cd "$ROOT" && tar --exclude='./.git' --exclude='./app/build' --exclude='./.gradle' -cf - .) | (cd "$P" && tar xf -)
cs() { (cd "$P" && env REPO=Kshitij-max6/Flowy "$@" bash "$R/check-static.sh"); }
mut()   { cp "$P/$1" "$P/$1.bak"; sed -i "$2" "$P/$1"; }
unmut() { mv "$P/$1.bak" "$P/$1"; }
UPD=app/src/main/java/com/example/core/update/AppUpdateManager.kt
case_ "the real project passes"             ok   'Static checks passed' cs
case_ "repository name compares case-insensitively" ok 'Static checks passed' cs REPO=kshitij-MAX6/flowy
case_ "updater repository drift is caught"  fail 'Updater repository differs' cs REPO=someone/else
mut app/build.gradle.kts 's/applicationId = "com.kshitij.flowly"/applicationId = "com.other.app"/'
case_ "changed applicationId is caught"     fail 'Application ID changed' cs;                 unmut app/build.gradle.kts
mut app/build.gradle.kts 's/signingConfig = signingConfigs.getByName("release")/signingConfig = signingConfigs.getByName("debug")/'
case_ "release signed with the debug key is caught" fail 'not signed with the release key' cs; unmut app/build.gradle.kts
mut app/build.gradle.kts 's/isMinifyEnabled = false/isMinifyEnabled = false\n            isDebuggable = true/'
case_ "debuggable release is caught"        fail 'debuggable' cs;                             unmut app/build.gradle.kts
mut app/build.gradle.kts 's/keyAlias = "upload"/keyAlias = "other"/'
case_ "key alias change is caught"          fail 'alias' cs;                                  unmut app/build.gradle.kts
mut app/build.gradle.kts 's/System.getenv("VERSION_CODE")/"1"/'
case_ "hard-coded version is caught"        fail 'no longer reads VERSION_CODE' cs;           unmut app/build.gradle.kts
mut "$UPD" 's/name.endsWith(\".apk\", true)/name.endsWith(\".bin\", true)/'
case_ "asset-naming drift is caught"        fail 'Asset naming contract' cs;                  unmut "$UPD"
mut "$UPD" 's/Flowly-v\$tag\.apk/Flowly-x.apk/'
case_ "exact-asset-name drift is caught"    fail 'Asset naming contract' cs;                  unmut "$UPD"
( cd "$P" && git init -q . && git config user.email t@t && git config user.name t && echo x > my-upload-key.jks && git add -A >/dev/null 2>&1 && git add -f my-upload-key.jks && git commit -qm t ) >/dev/null 2>&1
case_ "a committed key file is caught"      fail 'signing key file is committed' cs
rm -rf "$P/.git"

# ---------------------------------------------------------------- workflow policy
section "workflow policy: permissions, publish isolation, tag filter"
cw() { WF_DIR="$T/wf" REL_DIR="${REL_DIR_OVERRIDE:-$R}" bash "$R/check-workflows.sh"; }
wfreset() { rm -rf "$T/wf"; cp -r "$ROOT/.github/workflows" "$T/wf"; }
wfreset
case_ "the real workflows satisfy the policy"           ok   'Workflow policy passed' cw
wfreset; sed -i "s/'v\[0-9\]+.\[0-9\]+.\[0-9\]+'/'v*'/" "$T/wf/release.yml"
case_ "a loose 'v*' tag filter is rejected"            fail 'too loose' cw
wfreset; sed -i '/^    tags:/,/^  workflow_dispatch:/d' "$T/wf/release.yml"
case_ "a release with no tag filter is rejected"       fail 'no tag filter|tag filter' cw
wfreset; sed -i '0,/contents: read/ s/contents: read/contents: write/' "$T/wf/preflight.yml"
case_ "write permission in preflight is rejected"      fail 'Write permission outside release|not read-only' cw
wfreset; sed -i '/^  build:/,/^  publish:/ s/contents: read/contents: write/' "$T/wf/release.yml"
case_ "write permission on the build job is rejected"  fail 'contents: write' cw
wfreset; sed -i '0,/^permissions:/ s/^permissions:/xpermissions:/' "$T/wf/release.yml"
case_ "missing top-level permissions are rejected"     fail 'not explicit|writable|permissions' cw
wfreset; sed -i 's/^    secrets:$/    secrets: inherit\n    xsecrets:/' "$T/wf/release.yml"
case_ "'secrets: inherit' is rejected"                 fail 'inherit' cw
wfreset; printf '      - run: git push origin HEAD\n' >> "$T/wf/build-apk.yml"
case_ "publishing commands outside release.yml are rejected" fail 'Publishing outside release.yml' cw
wfreset; printf '      - run: gh release delete v1.0.0 -y\n' >> "$T/wf/preflight.yml"
case_ "deleting a release from preflight is rejected"  fail 'Publishing outside release.yml' cw
wfreset; printf '      - run: echo ${{ secrets.STORE_PASSWORD }}\n' >> "$T/wf/build-apk.yml"
case_ "signing secrets in the debug workflow are rejected" fail 'wrong workflow' cw
wfreset; sed -i '/^  publish:/,$ s/^      VERIFIED_SHA256: .*/&\n      LEAK: ${{ secrets.KEY_PASSWORD }}/' "$T/wf/release.yml"
case_ "signing secrets in the publish job are rejected" fail 'publish job' cw
wfreset; sed -i 's/^on:$/on:\n  pull_request_target:/' "$T/wf/build-apk.yml"
case_ "pull_request_target is rejected"                fail 'pull_request_target' cw
mkdir -p "$T/rel" && cp "$R"/*.sh "$T/rel/" && wfreset
printf '\ngh api -X POST "repos/$REPO/releases" -f tag_name=x\n' >> "$T/rel/check-github.sh"
case_ "a mutating call added to a preflight script is rejected" fail 'mutates GitHub' env REL_DIR_OVERRIDE="$T/rel" bash -c "WF_DIR=$T/wf REL_DIR=$T/rel bash $R/check-workflows.sh"
rm -rf "$T/rel"; wfreset
case_ "exactly one workflow builds debug APKs (no duplicate)" ok '' bash -c "[ \$(grep -l 'assembleDebug' $ROOT/.github/workflows/*.yml | wc -l) -eq 1 ]"
case_ "only release.yml assembles a release APK"       ok   '' bash -c "[ \$(grep -l 'assembleRelease' $ROOT/.github/workflows/*.yml | wc -l) -eq 1 ] && grep -q assembleRelease $ROOT/.github/workflows/release.yml"

# GitHub tag filters are GLOBS, not regexes. This translates the documented glob rules ('*' = any run except '/',
# '+' = one or more of the previous character/class, '?' = zero or one, '[..]' classes, everything else literal)
# and checks the filter that release.yml really contains against realistic tags.
tagmatch() { perl -e '
  my ($glob,$tag)=@ARGV; my $re="";
  my @c=split //,$glob;
  for (my $i=0;$i<@c;$i++){ my $ch=$c[$i];
    if($ch eq "["){ my $j=$i; $j++ while $j<@c && $c[$j] ne "]"; $re.=join("",@c[$i..$j]); $i=$j }
    elsif($ch eq "*"){ $re.="[^/]*" } elsif($ch eq "+"){ $re.="+" } elsif($ch eq "?"){ $re.="?" }
    else { $re.=quotemeta($ch) } }
  exit(($tag =~ /^$re$/) ? 0 : 1);' "$1" "$2"; }
FILTER="$(perl -ne 'if(/^\s+- \x27(v[^\x27]*)\x27/){print $1; exit}' "$ROOT/.github/workflows/release.yml")"
for ok_tag in v1.3.2 v0.0.1 v10.20.30; do case_ "tag filter starts a release for $ok_tag" ok '' tagmatch "$FILTER" "$ok_tag"; done
for no_tag in v1.3 v1.3.2-beta v1.3.2.4 V.1.2.0 V1.2.0 vfoo v1.3.x 1.3.2 latest nightly v1..2 v1.2.3rc1; do
  case_ "tag filter ignores $no_tag"                  fail '' tagmatch "$FILTER" "$no_tag"
done
case_ "(documentation) the old filter 'v*' WOULD have fired on v1.3.2-beta" ok '' tagmatch 'v*' v1.3.2-beta

# ---------------------------------------------------------------- check-signing
section "check-signing: permanent key restore and checks (throwaway keys)"
KT="$T/keys"; mkdir -p "$KT"
mk() { keytool -genkeypair -keystore "$KT/$1" "${@:2}" -keyalg RSA -keysize 2048 -validity 100 >/dev/null 2>&1; }
mk good.p12   -storetype PKCS12 -storepass TestPass123 -alias upload  -dname "CN=Test Only"
mk other.p12  -storetype PKCS12 -storepass TestPass123 -alias upload  -dname "CN=Other"
mk alias.p12  -storetype PKCS12 -storepass TestPass123 -alias release -dname "CN=A"
mk legacy.jks -storetype JKS -storepass StorePw1 -keypass KeyPw2 -alias upload -dname "CN=Legacy"
b64() { base64 < "$1" | tr -d '\n'; }
GOOD64="$(b64 "$KT/good.p12")"
CERT="$(keytool -exportcert -alias upload -keystore "$KT/good.p12" -storepass TestPass123 2>/dev/null | sha256sum | cut -d' ' -f1)"
mk dbg.p12    -storetype PKCS12 -storepass TestPass123 -alias upload  -dname "CN=Android Debug,O=Android,C=US"
LEGACYCERT="$(keytool -exportcert -alias upload -keystore "$KT/legacy.jks" -storepass StorePw1 2>/dev/null | sha256sum | cut -d' ' -f1)"
DBGCERT="$(keytool -exportcert -alias upload -keystore "$KT/dbg.p12" -storepass TestPass123 2>/dev/null | sha256sum | cut -d' ' -f1)"
# Default: correctly pinned (the pin is mandatory). Tests override with later NAME=value arguments.
sg() { env KEY_ALIAS=upload KEYSTORE_BASE64="$GOOD64" STORE_PASSWORD=TestPass123 KEY_PASSWORD=TestPass123 KEEP_KEYSTORE=0 EXPECTED_CERT_SHA256="$CERT" \
         GITHUB_OUTPUT="$T/sg_out" GITHUB_ENV="$T/sg_env" "$@" bash "$R/check-signing.sh"; }
: > "$T/sg_out"; : > "$T/sg_env"
PINCOLON="$(echo "$CERT" | tr a-f A-F | sed 's/../&:/g; s/:$//')"
case_ "valid key, correctly pinned"         ok   'matches EXPECTED_CERT_SHA256' sg
case_ "UNPINNED identity is refused (mandatory pin)" fail 'not pinned' sg EXPECTED_CERT_SHA256=
case_ "the refusal prints the fingerprint to pin" fail "$CERT" sg EXPECTED_CERT_SHA256=
case_ "a malformed pin is refused"          fail 'not a 64-digit' sg EXPECTED_CERT_SHA256=abc123
case_ "a pin with spaces only is refused"   fail 'not pinned|not a 64-digit' sg EXPECTED_CERT_SHA256="   "
case_ "an Android debug certificate is refused even when pinned" fail 'debug certificate' sg KEYSTORE_BASE64="$(b64 "$KT/dbg.p12")" EXPECTED_CERT_SHA256="$DBGCERT"
case_ "the certificate fingerprint is reported" ok "$CERT" sg
case_ "pinned correctly (upper case, colons)" ok 'matches EXPECTED_CERT_SHA256' sg EXPECTED_CERT_SHA256="$PINCOLON"
case_ "pinned to another identity is refused" fail 'does not match the repository variable EXPECTED_CERT_SHA256' sg EXPECTED_CERT_SHA256="$(printf 'ab%.0s' {1..32})"
case_ "a different key with the right pin is refused" fail 'does not match the repository variable EXPECTED_CERT_SHA256' sg KEYSTORE_BASE64="$(b64 "$KT/other.p12")" EXPECTED_CERT_SHA256="$CERT"
case_ "CRLF and spaces in the base64 are tolerated" ok 'Signing certificate' sg KEYSTORE_BASE64="$(printf '%s' "$GOOD64" | sed 's/^\(.\{40\}\)/\1\r\n  /')"
case_ "legacy JKS with a different key password" ok 'Signing certificate' sg KEYSTORE_BASE64="$(b64 "$KT/legacy.jks")" STORE_PASSWORD=StorePw1 KEY_PASSWORD=KeyPw2 EXPECTED_CERT_SHA256="$LEGACYCERT"
case_ "wrong store password"                fail 'could not be opened' sg STORE_PASSWORD=nope
case_ "PKCS12 with a different key password" fail 'must equal the store password' sg KEY_PASSWORD=different
case_ "wrong key password on a JKS key"     fail 'does not unlock' sg KEYSTORE_BASE64="$(b64 "$KT/legacy.jks")" STORE_PASSWORD=StorePw1 KEY_PASSWORD=wrong EXPECTED_CERT_SHA256="$LEGACYCERT"
case_ "garbage base64"                      fail 'not valid base64' sg KEYSTORE_BASE64='@@@not base64@@@'
case_ "keystore without the alias"          fail "alias 'upload'" sg KEYSTORE_BASE64="$(b64 "$KT/alias.p12")"
case_ "no secrets at all names all three"   fail 'KEYSTORE_BASE64 STORE_PASSWORD KEY_PASSWORD' env -i PATH="$PATH" HOME="$T" RUNNER_TEMP="$RUNNER_TEMP" bash "$R/check-signing.sh"
case_ "one missing secret is named"         fail 'secrets are missing.*STORE_PASSWORD' env -i PATH="$PATH" HOME="$T" RUNNER_TEMP="$RUNNER_TEMP" KEYSTORE_BASE64=abc KEY_PASSWORD=x bash "$R/check-signing.sh"
leak() { local o; o="$("$@" 2>&1)"; ! grep -qF -e TestPass123 -e "wrong-secret-value" -e "${GOOD64:0:30}" <<<"$o"; }
case_ "no secret value is printed on success" ok '' leak sg
case_ "no secret value is printed on failure" ok '' leak sg STORE_PASSWORD=wrong-secret-value
rm -f "$RUNNER_TEMP/flowly-release.jks"; sg >/dev/null 2>&1
case_ "preflight mode leaves no key file"   ok   '' test ! -e "$RUNNER_TEMP/flowly-release.jks"
sg KEEP_KEYSTORE=1 >/dev/null 2>&1
case_ "build mode keeps the key outside the source tree" ok '' bash -c "test -s $RUNNER_TEMP/flowly-release.jks && grep -q 'KEYSTORE_PATH=$RUNNER_TEMP/flowly-release.jks' $T/sg_env"
case_ "the key file is private (mode 600)"  ok   '' bash -c "[ \$(stat -c %a $RUNNER_TEMP/flowly-release.jks) = 600 ]"
sg KEEP_KEYSTORE=1 STORE_PASSWORD=bad >/dev/null 2>&1
case_ "a failed check deletes the key file" ok   '' test ! -e "$RUNNER_TEMP/flowly-release.jks"

# ---------------------------------------------------------------- gh stub for the next sections
cat > "$T/bin/gh" <<'GHEOF'
#!/usr/bin/env bash
# Read-only GitHub stub. ANY mutating call is recorded in $STUB_MUT and fails, so tests can prove preflight never writes.
args="$*"
case "$args" in
  "api -X "*|"api --method "*|"release create"*|"release delete"*|"release edit"*|"release upload"*)
    echo "MUTATION: $args" >> "${STUB_MUT:-/dev/null}"; echo "stub refused a mutating call" >&2; exit 98 ;;
  "api repos/"*)     echo "${STUB_PRIVATE:-false}" ;;
  "release view "*)  case "${STUB_RELEASE:-none}" in
                       none)  echo "release not found" >&2; exit 1 ;;
                       empty) echo '{"isDraft":false,"assets":[]}' ;;
                       apk)   echo '{"isDraft":false,"assets":[{"name":"Flowly-v1.3.1.apk"}]}' ;;
                       error) echo "HTTP 500 server error" >&2; exit 1 ;;
                     esac ;;
  "release list "*)  echo "${STUB_RELEASES:-[]}" ;;
  "release download "*) d=""; p=""; for a in "$@"; do [ "$p" = "--dir" ] && d="$a"; p="$a"; done; cp "${STUB_DL:?}"/* "$d"/ ;;
  *) echo "unexpected gh call: $args" >&2; exit 99 ;;
esac
GHEOF
chmod +x "$T/bin/gh"; export PATH="$T/bin:$PATH"

# ---------------------------------------------------------------- check-github
section "check-github: public repo, branch, reused tag (READ-ONLY)"
O="$T/origin.git"; W="$T/work"; git init -q --bare "$O"; git clone -q "$O" "$W" >/dev/null 2>&1
( cd "$W" && git config user.email t@t && git config user.name t && git checkout -qb main && echo a>a && git add a && git commit -qm a \
  && git push -q origin main && git checkout -qb feature && echo b>b && git add b && git commit -qm b ) >/dev/null 2>&1
MAIN_SHA="$(cd "$W" && git rev-parse main)"; FEAT_SHA="$(cd "$W" && git rev-parse feature)"
cg() { (cd "$W" && env REPO=o/r TAG=v1.3.1 EVENT=push DEFAULT_BRANCH=main REF_NAME=main GITHUB_SHA="$MAIN_SHA" RUN_ID=7 GH_TOKEN=x "$@" bash "$R/check-github.sh"); }
case_ "all good (tag on main)"              ok   'GitHub checks passed' cg
case_ "a private repository is refused"     fail 'Repository is private' cg STUB_PRIVATE=true
case_ "a tag on an unmerged branch is refused" fail 'not on the main branch' cg GITHUB_SHA="$FEAT_SHA"
case_ "a manual run from the wrong branch is refused" fail 'wrong branch' cg EVENT=workflow_dispatch REF_NAME=feature
case_ "a manual run from main is accepted"  ok   'GitHub checks passed' cg EVENT=workflow_dispatch REF_NAME=main
case_ "an already-published tag is refused" fail 'Tag already released' cg STUB_RELEASE=apk
case_ "a release object without an APK is accepted" ok 'without an APK' cg STUB_RELEASE=empty
case_ "a GitHub error reading releases stops the run" fail "Couldn't read releases" cg STUB_RELEASE=error
: > "$T/mut"
case_ "preflight makes NO mutating GitHub call (success path)" ok '' bash -c "cd $W && env REPO=o/r TAG=v1.3.1 EVENT=push DEFAULT_BRANCH=main REF_NAME=main GITHUB_SHA=$MAIN_SHA GH_TOKEN=x STUB_MUT=$T/mut bash $R/check-github.sh >/dev/null 2>&1 && [ ! -s $T/mut ]"
case_ "preflight does not claim to have tested write permission" ok 'not tested' cg
case_ "a read-only token is fine for preflight (no probe)" ok 'GitHub checks passed' cg STUB_MUT="$T/mut"
case_ "no mutating call was ever attempted"  ok '' test ! -s "$T/mut"

# ---------------------------------------------------------------- check-continuity
section "check-continuity: same key and higher version than the previous release"
BT="$T/bt"; mkdir -p "$BT" "$T/dl"
cat > "$BT/apksigner" <<'SEOF'
#!/usr/bin/env bash
echo "Signer #1 certificate SHA-256 digest: $(cat "$(dirname "$0")/prev_cert")"
SEOF
cat > "$BT/aapt2" <<'SEOF'
#!/usr/bin/env bash
echo "package: name='com.kshitij.flowly' versionCode='$(cat "$(dirname "$0")/prev_code")' versionName='x' platformBuildVersionName='16'"
SEOF
chmod +x "$BT/apksigner" "$BT/aapt2"
NEWC="$(printf 'cd%.0s' {1..32})"; OLDC="$(printf 'ef%.0s' {1..32})"
echo "$NEWC" > "$BT/prev_cert"; echo 10300 > "$BT/prev_code"
printf 'apk-bytes' > "$T/dl/Flowly-v1.3.0.apk"; ( cd "$T/dl" && sha256sum Flowly-v1.3.0.apk > Flowly-v1.3.0.apk.sha256 )
REL='[{"tagName":"v1.3.0","isDraft":false,"isPrerelease":false},{"tagName":"v1.9.9","isDraft":true,"isPrerelease":false},{"tagName":"v2.0.0","isDraft":false,"isPrerelease":true}]'
cc() { env REPO=o/r TAG=v1.3.1 CERT="$NEWC" NEW_CODE=10301 BT="$BT" GH_TOKEN=x STUB_RELEASES="$REL" STUB_DL="$T/dl" "$@" bash "$R/check-continuity.sh"; }
case_ "first release, baseline NOT declared: BLOCKED" fail 'continuity is unresolved' cc STUB_RELEASES='[]'
case_ "first release, baseline declared as none" ok 'No earlier published release' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256=none
case_ "first release, baseline 'NONE' (any case, spaces)" ok 'No earlier published release' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256=' NONE '
case_ "first release, baseline equals the new key" ok 'same certificate as the APKs already distributed' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256="$NEWC"
case_ "first release, baseline in colon/upper-case form" ok 'same certificate as the APKs already distributed' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256="$(echo "$NEWC" | tr a-f A-F | sed 's/../&:/g; s/:$//')"
case_ "first release, baseline is a DIFFERENT key: refused" fail 'differs from the distributed APK' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256="$OLDC"
case_ "…unless that exact change is accepted" ok 'change accepted' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256="$OLDC" ACCEPT_SIGNER_CHANGE_TO="$NEWC"
case_ "…a wrong acceptance value does not unblock" fail 'differs from the distributed APK' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256="$OLDC" ACCEPT_SIGNER_CHANGE_TO="$OLDC"
case_ "a malformed baseline is refused"      fail 'malformed' cc STUB_RELEASES='[]' BASELINE_CERT_SHA256=12345
case_ "baseline is enforced even with an earlier release" fail 'differs from the distributed APK' cc BASELINE_CERT_SHA256="$OLDC"
case_ "same key and higher version"         ok   'Signing identity continues' cc
case_ "…and the version check says so"      ok   'Version continues: 10301 > 10300' cc
case_ "drafts and pre-releases are ignored (compares v1.3.0)" ok 'v1.3.0' cc
case_ "a different key is refused"          fail 'Signing key changed' cc CERT="$OLDC"
case_ "a deliberate key change needs the exact fingerprint" ok 'change accepted' cc CERT="$OLDC" ACCEPT_SIGNER_CHANGE_TO="$OLDC"
case_ "…a wrong acceptance value does not work" fail 'Signing key changed' cc CERT="$OLDC" ACCEPT_SIGNER_CHANGE_TO="$NEWC"
case_ "a versionCode equal to the previous one is refused" fail 'versionCode not increasing' cc NEW_CODE=10300
case_ "a lower versionCode is refused"      fail 'versionCode not increasing' cc NEW_CODE=10200
printf 'tampered' > "$T/dl/Flowly-v1.3.0.apk"
case_ "a wrong checksum on the previous release only warns" ok 'checksum is wrong' cc
printf 'apk-bytes' > "$T/dl/Flowly-v1.3.0.apk"
case_ "missing build tools are reported"    fail 'Build tools missing' cc BT="$T/nowhere"

# ---------------------------------------------------------------- verify-apk
section "verify-apk: what was really built"
va_setup() { # pkg code name debuggable(yes|no) signer v2(true|false) signers dex-string
  rm -rf "$T/sdk"; local bt="$T/sdk/bt"; mkdir -p "$bt"
  { echo "#!/usr/bin/env bash"; echo "cat <<'X'"
    echo "package: name='$1' versionCode='$2' versionName='$3' platformBuildVersionName='16' platformBuildVersionCode='36'"
    echo "sdkVersion:'26'"; [ "$4" = yes ] && echo "application-debuggable"; echo "X"; } > "$bt/aapt2"
  { echo "#!/usr/bin/env bash"; echo "cat <<'X'"; echo "Verifies"
    echo "Verified using v1 scheme (JAR signing): false"
    echo "Verified using v2 scheme (APK Signature Scheme v2): $6"; echo "Verified using v3 scheme (APK Signature Scheme v3): $6"
    echo "Number of signers: $7"
    for i in $(seq 1 "$7"); do echo "Signer #$i certificate DN: CN=x"; echo "Signer #$i certificate SHA-256 digest: $5"; done; echo "X"; } > "$bt/apksigner"
  chmod +x "$bt/aapt2" "$bt/apksigner"
  ( cd "$T" && rm -f fake.apk classes.dex && printf 'xx %s yy' "$8" > classes.dex && zip -q fake.apk classes.dex )
}
vb() { env APK="$T/fake.apk" BT="$T/sdk/bt" TAG=v1.3.1 WANT_NAME=1.3.1 WANT_CODE=10301 KEYSTORE_CERT="$CERT" UPDATE_REPO=Kshitij-max6/Flowy "$@" bash "$R/verify-apk.sh"; }
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" true 1 Kshitij-max6/Flowy
case_ "a correct release APK passes"        ok   'Verified: package, version' vb
case_ "…also when the identity is pinned"   ok   'Verified' vb EXPECTED_CERT_SHA256="$CERT"
case_ "pinned to another identity"          fail 'EXPECTED_CERT_SHA256' vb EXPECTED_CERT_SHA256="$(printf 'ab%.0s' {1..32})"
va_setup com.kshitij.flowly 10301 1.1.0 no "$CERT" true 1 Kshitij-max6/Flowy;  case_ "wrong versionName inside the APK" fail "versionName is '1.1.0'" vb
va_setup com.kshitij.flowly 10100 1.3.1 no "$CERT" true 1 Kshitij-max6/Flowy;  case_ "wrong versionCode (Gradle fallback)" fail "versionCode is '10100'" vb
va_setup com.example.other  10301 1.3.1 no "$CERT" true 1 Kshitij-max6/Flowy;  case_ "wrong applicationId" fail 'applicationId' vb
va_setup com.kshitij.flowly 10301 1.3.1 yes "$CERT" true 1 Kshitij-max6/Flowy; case_ "a debuggable APK" fail 'debuggable' vb
va_setup com.kshitij.flowly 10301 1.3.1 no "$OLDC" true 1 Kshitij-max6/Flowy;  case_ "signed by another key" fail 'not signed with the restored release key' vb
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" false 1 Kshitij-max6/Flowy; case_ "no v2/v3 signature" fail 'no valid v2/v3' vb
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" true 2 Kshitij-max6/Flowy;  case_ "two signers" fail 'exactly one signer' vb
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" true 1 someone/else;        case_ "updater repository not baked in" fail 'update repository' vb
case_ "a missing APK file"                  fail 'does not exist' vb APK="$T/nope.apk"

# Regression: real APKs have multi-megabyte dex files and large aapt2 output. Under 'set -o pipefail' a "grep -q" on such a
# stream used to (a) fail every genuine release and (b) fail OPEN on the debuggable check.
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" true 1 Kshitij-max6/Flowy
( cd "$T" && rm -f fake.apk classes.dex && { printf 'xx Kshitij-max6/Flowy yy'; head -c 20000000 /dev/urandom; } > classes.dex && zip -q fake.apk classes.dex )
case_ "a genuine APK with a 20 MB dex passes (no SIGPIPE false failure)" ok 'Verified: package' vb
va_setup com.kshitij.flowly 10301 1.3.1 no "$CERT" true 1 Kshitij-max6/Flowy
{ echo '#!/usr/bin/env bash'; echo "cat <<'X'"; echo "package: name='com.kshitij.flowly' versionCode='10301' versionName='1.3.1'"; echo "application-debuggable"
  seq 1 60000 | sed 's/^/uses-permission: name=android.permission.P/'; echo X; } > "$T/sdk/bt/aapt2"; chmod +x "$T/sdk/bt/aapt2"
case_ "debuggable is still detected in huge aapt2 output (no fail-open)" fail 'debuggable' vb

# ---------------------------------------------------------------- finalize-assets
section "finalize-assets: naming and checksum"
fa() { (cd "$T" && env TAG=v1.3.1 APK_DIR="$T/out/apk" GITHUB_OUTPUT="$T/fa_out" "$@" bash "$R/finalize-assets.sh"); }
mkdir -p "$T/out/apk"; printf 'real-apk' > "$T/out/apk/app-release.apk"; : > "$T/fa_out"
case_ "names the APK as the updater expects" ok  'Flowly-v1.3.1.apk' fa
case_ "the checksum file matches the final file" ok '' bash -c "cd $T && sha256sum -c Flowly-v1.3.1.apk.sha256 >/dev/null"
case_ "the updater can read it (first token is 64 hex)" ok '' bash -c "cd $T && cut -d' ' -f1 Flowly-v1.3.1.apk.sha256 | grep -Eq '^[0-9a-f]{64}\$'"
case_ "a bad tag cannot produce a bad asset name" fail 'is not Flowly-vX.Y.Z.apk' fa TAG=1.3.1-beta
mv "$T/out/apk/app-release.apk" "$T/out/apk/app-release-unsigned.apk"
case_ "an unsigned APK is refused"          fail 'Wrong APK' fa
rm -f "$T/out/apk/"*; printf a > "$T/out/apk/one.apk"; printf b > "$T/out/apk/two.apk"
case_ "two APKs are refused"                fail 'found 2' fa
rm -f "$T/out/apk/"*
case_ "no APK is refused"                   fail 'found 0' fa

# ---------------------------------------------------------------- summary
printf '\n================================\nRESULT: %d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ] || { printf 'Failed:\n'; printf '  - %s\n' "${FAILED[@]}"; exit 1; }
