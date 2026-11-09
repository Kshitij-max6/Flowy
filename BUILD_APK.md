# Building the APK

1. Create a GitHub repo and push this project (keep `.github/workflows/build-apk.yml`).
2. Open Actions > "Build Flowly debug APK" > Run workflow (it also runs on every push to main).
3. When it finishes, download `app-debug-apk` from the run's Artifacts. Unzip it to get `app-debug.apk`.
4. If the build complains about google-services.json, add your Firebase file as the
   repository secret GOOGLE_SERVICES_JSON_B64 (output of `base64 -w0 google-services.json`).

Locally: install Android Studio, open this folder, Build > Build APK(s).
Create `debug.keystore` first (see the keytool step in the workflow) since it is git-ignored.
