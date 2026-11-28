# Releasing Flowly (and how in-app updates work)

This is the **one** authoritative release document. If another file disagrees with it, this file wins and the other
file is a bug. The workflows in `.github/workflows/` and the scripts in `.github/release/` are what actually run;
the self-test (`Release scripts self-test`) checks them against the rules written here.

Flowly checks **GitHub Releases** once a day. When a newer release exists it shows an update pop-up (once per
version, with *Later* and *Skip this version*). Settings → **App updates** checks on demand. Before installing,
Flowly saves a safety copy of your data, verifies the download's SHA-256, and checks that the new APK is signed
with the **same key** as the installed app.

---

## 1. The contract (what must always be true)

| Item | Rule |
|---|---|
| Release tag | exactly `vMAJOR.MINOR.PATCH`, e.g. `v1.3.2`. No suffixes (`-beta`), no leading zeros, major 0-999, minor and patch 0-99. |
| `versionName` | the tag without the `v` (`1.3.2`). |
| `versionCode` | `major×10000 + minor×100 + patch` (`1.3.2` → `10302`). Computed in **one** place: `version_code()` in `.github/release/lib.sh`. Must only ever go up. |
| Default (local/debug) version | `app/build.gradle.kts` falls back to `10100` / `1.1.0`. Releases must be above it. |
| Release assets | exactly one APK `Flowly-vMAJOR.MINOR.PATCH.apk` and its `Flowly-vMAJOR.MINOR.PATCH.apk.sha256`. |
| Checksum file | one line in `sha256sum` format: `<64 hex digits>␠␠<APK file name>`. |
| Release state | published (not draft, not pre-release). The updater only sees the *latest* published release. |
| Package id | `com.kshitij.flowly`. Never change it. |
| Signing alias | `upload`. |
| A published version | is **never overwritten or re-used**. A broken release is fixed by publishing the next version. |

The updater refuses (and says why) when: the release has no APK or more than one; the APK is not named exactly as above;
the checksum is missing, is not 64 hex digits, has several lines, or names a different file; the download does not match
the checksum; the package is another app; the new `versionCode` is not higher; or the signing key differs from the installed app.

---

## 2. Signing identity: READ THIS BEFORE THE FIRST RELEASE

Android refuses to update an app signed by a different key. So the key that signs releases must be the key that
signed the APKs already on people's phones, **or** you must knowingly accept that everyone reinstalls once.

**What the repository can and cannot tell you.** Every CI debug build (`Build Flowly debug APK`) is signed with a
key generated fresh on that run and thrown away (this has been so since the first commit). If the APK you gave to
friends was the `app-debug.apk` from that workflow's `app-debug-apk` artifact, its key no longer exists anywhere.
Whether that is the case is **not** provable from the repository: it must be read from the APK itself.

### Step A: find out who signed the APK your friends have

You need the **signer certificate SHA-256** of that APK (a public value, safe to share). Use any one of:

1. **Android SDK Build-Tools `apksigner`** (no Android Studio needed). Download Google's *command-line tools* zip from
   developer.android.com/studio (section "Command line tools only"), unzip it, run
   `sdkmanager "build-tools;36.0.0"`, then:
   `apksigner verify --print-certs app-debug.apk` → note the line `Signer #1 certificate SHA-256 digest`.
2. Upload the APK to an assistant that can inspect files and ask it to extract the signing certificate fingerprint.
   An APK contains no secrets; do **not** upload any `.jks` file or password.

`keytool -printcert -jarfile app-debug.apk` (any JDK) only works when the APK also has a v1/JAR signature; with
`minSdk 26` it often does not, so prefer option 1.

### Step B: decide, then record the decision as repository variables

Settings → Secrets and variables → Actions → **Variables** tab (these are public fingerprints, not secrets):

| Variable | Meaning |
|---|---|
| `EXPECTED_CERT_SHA256` | **Required.** The fingerprint of the release key you intend to use. The release is blocked until it is set and matches the key in the secrets. |
| `BASELINE_CERT_SHA256` | **Required until a first GitHub release exists.** The signer fingerprint of APKs already distributed (from Step A), or the single word `none` if nobody ever received an APK signed by another key. |
| `ACCEPT_SIGNER_CHANGE_TO` | Only for a deliberate identity change: set to the *new* key's fingerprint. Without it, a differing key is refused. |

Outcomes:

* **You still have the original key** (the `.jks` and passwords): put it in the secrets (section 3), set
  `EXPECTED_CERT_SHA256` and `BASELINE_CERT_SHA256` to its fingerprint. Updates install in place.
* **The original key is gone** (for example a throw-away CI debug key): there is nothing to recover. Create one permanent
  key (section 3), set `EXPECTED_CERT_SHA256` to it, set `BASELINE_CERT_SHA256` to the old APK's fingerprint, and set
  `ACCEPT_SIGNER_CHANGE_TO` to the new fingerprint. Everyone with the old app does the **one last reinstall** (section 9).
  Nothing in the workflows does this for you; it is your explicit decision.
* **Nobody outside you ever installed anything**: `BASELINE_CERT_SHA256` = `none`.

Until these are decided, `Release preflight` fails with `Signing continuity is unresolved` or `The signing identity is
not pinned`. That is intentional: it is the block, not a bug. Do not "fix" it by loosening the scripts.

---

## 3. One-time setup

1. **One permanent signing key** (once; keep it forever; back it up in two separate places, e.g. password manager +
   offline drive; **never** only in GitHub):

   ```bash
   keytool -genkeypair -v -keystore flowly-release.jks -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Flowly,O=Flowly,C=IN"
   ```

   Alias must be `upload`. Do not use a name containing `Android Debug` (the scripts refuse such certificates).
   **Do this only after Section 2**, and never to get past a missing-secret error.
2. Repository **Secrets**: `KEYSTORE_BASE64` (`base64 -w0 flowly-release.jks`), `STORE_PASSWORD`, `KEY_PASSWORD`
   (for a PKCS12 keystore the two passwords must be identical), optional `GOOGLE_SERVICES_JSON_B64`.
3. Repository **Variables**: see section 2.
4. Repository settings: the repository must be **public** (the app reads releases without logging in); Actions enabled.
   The default "Workflow permissions" may stay *Read repository contents*: the publish job requests its own write access.
5. Never commit a `.jks`/`.keystore` (git-ignored; the static check also fails if one is tracked).
6. Back up the keystore **and** write down: alias, both passwords, and the certificate SHA-256, in your password manager.

The repository the app checks is `flowly.updateRepo` in `gradle.properties` (`Kshitij-max6/Flowy`). CI overrides it
with `github.repository`; each workflow then verifies the value really is inside the APK. If you rename or move the
repository, change it.

---

## 4. Release preflight (a free dress rehearsal)

Actions → **Release preflight** → Run workflow. It is **strictly read-only** (`contents: read`; it never creates,
edits or deletes releases, tags or files; the self-test fails if that ever changes). It checks:
version/tag validity and reuse (including malformed twins such as `V.1.2.0`), repository configuration and workflow
policy, that the signing secrets exist and open the key, the key is not a debug key and equals `EXPECTED_CERT_SHA256`,
the repository is public, the commit is on the default branch, the tag is unused, and (against the previous release
and/or `BASELINE_CERT_SHA256`) the signing key and `versionCode` continue correctly.

**Not verified by preflight** (it cannot do so read-only): that the token can create releases (the publish job
requests that itself), Actions availability, and that a real build will succeed.

---

## 5. Publishing a version

Either:

* Actions → **Release Flowly APK** → Run workflow (branch `main`): pick patch/minor/major, or type an exact version; or
* `git tag v1.3.2 && git push origin v1.3.2` (the tag must be on `main`).

Three jobs run in order: **preflight** (read-only) → **build** (read-only token; the only job that sees the signing
secrets; runs the updater tests, builds, names the APK, verifies the real APK with `apksigner`/`aapt2`: package id,
`versionName`, `versionCode`, not debuggable, valid v2/v3 signature, exactly one signer equal to the restored key and
to `EXPECTED_CERT_SHA256`, update repository baked in) → **publish** (the only job with `contents: write`; sees no
signing secret; re-checks name and checksum, publishes, then confirms that GitHub's *latest* release is this tag, that
it has exactly one APK, and that the downloadable files match the verified ones byte for byte).

Notes in the release text: put `[required]` anywhere to make an emergency update that cannot be dismissed.
Pre-releases and drafts are ignored by the updater.

## 6. Verifying a published release yourself

```bash
sha256sum -c Flowly-v1.3.2.apk.sha256                      # in the folder with both files
apksigner verify --print-certs Flowly-v1.3.2.apk            # signer must equal EXPECTED_CERT_SHA256
aapt2 dump badging Flowly-v1.3.2.apk | head -1              # package, versionCode 10302, versionName 1.3.2
```

---

## 7. Debug builds (testing only)

`Build Flowly debug APK` (push to `main`, or run manually) produces `app-debug-apk` → `app-debug.apk`, signed with a
**new throw-away key on every run**. It can never be updated by another build and **must not be given to other
people**. Real APKs come only from `Release Flowly APK`. (This single workflow replaced the duplicate `main.yml`.)

## 8. Your data and updates

An in-place update never touches app data: Android keeps private storage when the package name and signing key stay the
same, and Flowly's database changes only through numbered Room migrations (`MIGRATION_x_y` in `AppDatabase`), with no
destructive fallback. Rules when changing the schema:

1. Bump `AppDatabase.DB_VERSION` and add a `MIGRATION_<old>_<new>`; register it in `getDatabase()`.
2. Build once and commit the generated `app/schemas/<version>.json` (**currently no schema file is committed**, so
   migrations cannot be tested yet; building once and committing them is an open to-do).
3. Never lower the version and never reuse a `versionCode`.

Safety nets: a database copy is saved right before each install and on first launch of each new version
(Settings → **Restore pre-update backup**); a recovery screen appears if the database cannot be opened.

## 9. Recovery

* **The one last reinstall** (only after an accepted signing-identity change): Settings → Backup & Restore → export a
  backup file and keep it outside the app → uninstall the old Flowly → install the new release APK → import the backup.
* **A release failed before publishing** (preflight or build red): nothing is public. Fix the cause, then re-run the
  failed run (for a pushed tag) or start *Release Flowly APK* again. If a pushed tag points at the wrong commit, do not
  move or delete it yourself: ask first, and prefer releasing the next version.
* **The publish job failed after the release went live** (`Confirm what the app will find`): open the Releases page.
  If it is a draft or pre-release, publish it as latest; if its files are wrong, do **not** overwrite them: mark it
  pre-release (the updater then ignores it) and release the next version.
* **Wrong key used / key lost / key leaked**: stop releasing. A leaked key must be treated as compromised: create a new
  key only as a deliberate identity change (section 2, outcome 2). Never replace the secrets "to get green".
* **Preflight says a secret is missing**: add that secret. Do not generate a new keystore (Section 2 first).
* **Checksum of the previous release is wrong** (warning in preflight): phones updating to it are refused; release the next version.

## 10. Maintenance checklist

* Upgrading Gradle/AGP/Kotlin/JDK/SDK: change `gradle-version`/`java-version`/`BUILD_TOOLS`/`platforms;android-N`
  consistently in `release.yml`, `preflight.yml`, `build-apk.yml` (a `grep` for the old number finds them all), merge, let
  *Build Flowly debug APK* and *Release preflight* pass, then release.
* GitHub Actions: Dependabot (`.github/dependabot.yml`) opens weekly PRs for newer actions. Review, merge, re-run preflight.
* Rotating signing secrets: only restore the *same* key; a different key is an identity change (section 2).
* After editing anything in `.github/` or this file's contracts, `Release scripts self-test` must be green.
* `minSdk`/`targetSdk`: raising `minSdk` cuts off older phones from updates; check this deliberately.

## 11. Tools

* **GitHub Actions** has everything. Normal releases need nothing installed on your PC.
* Local self-test (`bash .github/release/tests/run-tests.sh`) needs: bash, JDK (`keytool`), `jq`, `zip`, `unzip`,
  `perl`, `git`, `sha256sum`, `base64`. If any are missing it prints `BLOCKED` and exits with code 2 (not a test
  failure). On Windows use WSL/Ubuntu or just rely on the GitHub workflow.
* Android SDK (`platform-tools`, `platforms;android-36`, `build-tools;36.0.0`) is only needed to build locally.
  The project ships `gradle-wrapper.properties` but **not** `gradlew` or the wrapper JAR; CI uses Gradle 9.3.1 directly.

## 12. Do not change without explicit approval

* `applicationId` (`com.kshitij.flowly`), key alias `upload`, the keystore, its passwords, the secrets;
* `EXPECTED_CERT_SHA256`, `BASELINE_CERT_SHA256`, `ACCEPT_SIGNER_CHANGE_TO`;
* the tag format, the versionCode formula, the asset names, the checksum format (the app in people's hands depends on them);
* the write permission of the publish job, anything that lets preflight write, or the tag filter in `release.yml`;
* Room migrations and `DB_VERSION`;
* the self-test expectations, to make a failing check pass.
