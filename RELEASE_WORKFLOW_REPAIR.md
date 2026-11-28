# Release workflow history

Earlier versions of this file described one-time repairs; their sample tags and instructions are obsolete.
[RELEASING.md](RELEASING.md) and the workflows in `.github/workflows/` are the only source of truth.

Why the release system looks the way it does (see RELEASING.md for the rules themselves):

* Version/tag problems -> one planner (`plan-version.sh`) and one `versionCode` formula; the built APK is read back and must match.
* Signing problems -> the key is only ever restored from secrets, must be pinned (`EXPECTED_CERT_SHA256`), and must continue
  the previous release or the declared baseline (`BASELINE_CERT_SHA256`). A debug-style certificate is refused.
* Publishing problems -> preflight is read-only; publishing happens in one job of `release.yml` that holds no signing secrets.
* Test problems -> the self-test reports missing tools as `BLOCKED` instead of cascading failures.
* Duplicate debug workflow -> `main.yml` was removed; `build-apk.yml` is the single debug build.
