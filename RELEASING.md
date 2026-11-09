# Releasing Flowly (and how in-app updates work)

Flowly checks **GitHub Releases** once a day. When a newer release exists it shows an update pop-up
(once per version, with *Later* and *Skip this version*). Settings → **App updates** checks on demand.
Before installing, Flowly saves a safety copy of your data, verifies the download's SHA-256, and checks
that the new APK is signed with the **same key** as the installed app.

## One-time setup

1. **Make one permanent signing key** (do this once, keep it forever, back it up somewhere safe):

   ```bash
   keytool -genkeypair -v -keystore flowly-release.jks -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

   The alias must be `upload` (that is what `app/build.gradle.kts` expects).
   **If you lose this file or its passwords, nobody can update the app in place again.**

2. In the GitHub repo: *Settings → Secrets and variables → Actions → New repository secret*:

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | output of `base64 -w0 flowly-release.jks` |
   | `STORE_PASSWORD` | the keystore password |
   | `KEY_PASSWORD` | the key password |
   | `GOOGLE_SERVICES_JSON_B64` | optional, only if you use Firebase |

3. Never commit the `.jks` file. (`*.jks` is in `.gitignore`.)

The workflow sets `UPDATE_REPO` to `owner/repo` automatically, so the app knows where to look.
For local builds you can export `UPDATE_REPO=yourname/Final-Flowly`.

## Publishing a version

```bash
git tag v1.2.0
git push origin v1.2.0
```

`.github/workflows/release.yml` builds a signed APK, computes `versionName` = `1.2.0` and
`versionCode` = `10200` (major×10000 + minor×100 + patch), attaches `Flowly-v1.2.0.apk` and its
`.sha256` to a GitHub Release.

* Use plain numeric tags (`v1.2.0`). Pre-releases and drafts are ignored by the updater.
* Put `[required]` anywhere in the release notes to make it an **emergency update**: the pop-up cannot be
  dismissed or skipped.
* Edit the release notes on GitHub; the first ~600 characters appear in the pop-up.

## The one last reinstall

Older builds of Flowly were signed by throw-away keys generated on each CI run (and the debug build uses a
different key again). Android refuses to update an app signed with a different key, so **everyone needs one
last manual reinstall** when moving to the first release built by this workflow:

1. Settings → Backup & Restore → export a backup file and keep it outside the app.
2. Uninstall the old Flowly, install the new release APK.
3. Backup & Restore → import the file.

After that, every update installs in place and keeps the data.

## Safety nets for updates

* A copy of the database is saved right before each install and again on the first launch of every new
  version, before the database is opened. Settings → **Restore pre-update backup** brings it back.
* If the database cannot be opened after an update, a recovery screen appears (look for a fix, restore the
  saved copy, or export the raw data file) instead of crashing.
* Database schemas are exported to `app/schemas/` on every build. Commit them: they document each version
  and let migrations be tested.
* The Room version lives in one place: `AppDatabase.DB_VERSION`. Bump it together with a new migration.
