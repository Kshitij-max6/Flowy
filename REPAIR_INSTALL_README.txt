FLOWLY RELEASE WORKFLOW REPAIR PACKAGE

This package includes the complete Flowly project snapshot plus a repair to the release static-checker contract.

REPAIR:
- The static checker previously expected the updater source to contain the literal 'Flowly-v$tag.apk'.
- The updater actually selects release APK assets by the '.apk' suffix, while the release workflow generates versioned Flowly APK filenames.
- The checker and self-test now validate the updater's actual APK-selection behavior. This removes the false failure and makes the drift test meaningful.
- The release script test suite was run in a Linux environment with its required tools available: 96 passed, 0 failed.

INSTALL:
1. Back up the current GitHub repository and local project folder first.
2. Extract this ZIP.
3. Copy the contents of the extracted Flowy folder into your existing repository folder, merging folders and replacing files when prompted. Do NOT copy any .git folder (none is included).
4. In Git Bash, run: bash .github/release/tests/run-tests.sh
   This test suite requires bash, keytool (JDK), jq, zip, unzip, perl, git, sha256sum and base64.
   If your local Windows/Git Bash environment lacks these, the GitHub Actions self-test runs on Ubuntu and installs Java as part of its workflow; do not treat local missing-tool errors as code test failures.
5. Review git status, then commit and push the repaired files.
6. Run Release Script Selftest on GitHub Actions and verify it is green before attempting a real release.

SAFETY:
- No signing keys, keystore files, passwords, or private credentials are included.
- Do not overwrite or generate a permanent signing key until continuity with your installed app's signing identity has been confirmed.
- The package has not been verified by a real Gradle release build, a real GitHub release, or a real installed-app update. The successful 96-test run is script-suite validation only.
