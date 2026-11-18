# SageLedger APK Downloads

This folder holds the latest compiled Android APK (`SageLedger-latest.apk`) and version metadata (`version.json`) for direct installation and update checks.

## How Future Builds Update Automatically

1. Every push to `main` (or manual run of the **Build & Update APK** GitHub Actions workflow) compiles `:app:assembleDebug` using `debug.keystore.base64`, increments `versionCode` and `versionName`, updates `apk/SageLedger-latest.apk` and `apk/version.json`, and commits the updated artifacts back to this folder.
2. When you configure your `owner/repo` inside **SageLedger → Preferences → App Updates (GitHub)**, the homepage automatically checks GitHub Releases first and falls back to `apk/version.json` on `main`, prompting you with an update popup linking straight to the latest APK download.
