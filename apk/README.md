# SageLedger APK Downloads

This folder holds the latest compiled Android APK (`SageLedger-latest.apk`) and version metadata (`version.json`) for direct installation and update checks.

## How Future Builds Update Automatically

1. Every push to `main`/`master` (or manual run of the **Build & Update APK** GitHub Actions workflow) sets up JDK 21 and Android SDK 36.1, prepares `debug.keystore`, increments `versionCode` and `versionName`, builds `:app:assembleDebug`, updates `apk/SageLedger-latest.apk` and `apk/version.json`, and commits the updated artifacts back to this folder.
2. When you configure your `owner/repo` inside **SageLedger → Preferences → App Updates (GitHub Releases)**, the homepage checks GitHub Releases first and falls back to `apk/version.json` on `main`/`master`, prompting you with a **Download APK** popup.

## Optional GitHub Repository Secrets

Because AI Studio omits `debug.keystore.base64` and `app/google-services.json` when exporting to GitHub, the workflow automatically generates a fallback debug keystore so CI builds succeed out of the box. To make GitHub-built APKs retain your Firebase & Google Sign-In configuration and matching SHA-1 fingerprint, add these optional secrets under **Settings → Secrets and variables → Actions**:

- `DEBUG_KEYSTORE_BASE64`: Base64 contents of `debug.keystore.base64`.
- `GOOGLE_SERVICES_JSON`: Contents of `app/google-services.json`.
