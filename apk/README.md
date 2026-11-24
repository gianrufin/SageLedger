# SageLedger APK Downloads

This folder holds the latest compiled Android APK (`SageLedger-latest.apk`) and version metadata (`version.json`) for direct installation and update checks.

## How Future Builds Update Automatically

1. Pushes to `main`/`master` and manual workflow runs execute `:app:testDebugUnitTest`.
2. The workflow publishes an APK only when Firebase configuration and all release-signing secrets are present. It builds `:app:assembleRelease`, writes `apk/SageLedger-latest.apk` and `apk/version.json`, then commits those artifacts. Without the secrets, tests can still run but no APK is published.
3. When you configure your `owner/repo` inside **SageLedger → Preferences → App Updates (GitHub Releases)**, the homepage checks GitHub Releases first and falls back to `apk/version.json` on `main`/`master`, prompting with a **Download APK** popup when an update is available.

The existing debug-signed APK and manifest were removed. Direct APK downloads resume after the release-signing secrets are configured and a signed release build is produced.

## Required GitHub Secrets for Automated APK Updates

Add these under **Settings → Secrets and variables → Actions**:

- `GOOGLE_SERVICES_JSON`: Contents of `app/google-services.json`.
- `RELEASE_KEYSTORE_BASE64`: Base64-encoded release keystore file (.jks or .keystore). Use the same keystore for every update.
- `RELEASE_STORE_PASSWORD`: Password for that keystore.
- `RELEASE_KEY_PASSWORD`: Password for the `upload` key alias.

The signing certificate must match the certificate used for any existing SageLedger installation that you want to update.
