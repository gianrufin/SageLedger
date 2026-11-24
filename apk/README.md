# SageLedger APK Downloads

This folder holds the latest compiled Android APK (`SageLedger-latest.apk`) and version metadata (`version.json`) for direct installation and update checks.

## How Future Builds Update Automatically

1. Every push to `main`/`master` (or manual run of the **Build & Update APK** GitHub Actions workflow) sets up JDK 21 and Android SDK 36.1, builds `:app:assembleDebug`, and runs `:app:testDebugUnitTest`.
2. When `GOOGLE_SERVICES_JSON` is present (via GitHub Secrets), the workflow increments `versionCode` and `versionName`, updates `apk/SageLedger-latest.apk` and `apk/version.json`, and commits the updated artifacts back to this folder. If `GOOGLE_SERVICES_JSON` is not set in GitHub Secrets, the workflow preserves the existing Firebase-configured `apk/SageLedger-latest.apk` instead of overwriting it with an unconfigured APK.
3. When you configure your `owner/repo` inside **SageLedger → Preferences → App Updates (GitHub Releases)**, the homepage checks GitHub Releases first and falls back to `apk/version.json` on `main`/`master`, prompting you with a **Download APK** popup.

## Required GitHub Secrets for Automated APK Updates

Because AI Studio omits `debug.keystore.base64` and `app/google-services.json` when pushing to GitHub, add these two secrets under **Settings → Secrets and variables → Actions** in your repository so CI-built APKs include Firebase, Firestore, and Google Sign-In:

- `GOOGLE_SERVICES_JSON`: Contents of `app/google-services.json`.
- `DEBUG_KEYSTORE_BASE64`: Contents of `debug.keystore.base64` (keeps the Firebase-registered SHA-1 signing fingerprint).
