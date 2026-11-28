<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_art_1794992247754.jpg" width="96" alt="SageLedger App Icon" />

# SageLedger

**Smart spending cockpit for Android — automatic e-wallet, Google Wallet, card & SMS purchase capture paired with live monthly budget pacing.**

[![Download APK](https://img.shields.io/badge/Download_APK-v1.0.37-16A34A?style=for-the-badge&logo=android&logoColor=white)](apk/SageLedger-latest.apk?raw=true)
[![Android](https://img.shields.io/badge/Android-7.0%2B_(API_24)-1F2937?style=for-the-badge)](#install-on-android)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#tech-stack)

[Download Latest APK](apk/SageLedger-latest.apk?raw=true) · [Features](#features) · [How It Works](#how-it-works) · [Install Guide](#install-on-android)

</div>

---

## Download & Install

> **Direct Download:** [**Download `SageLedger-latest.apk` (v1.0.37)**](apk/SageLedger-latest.apk?raw=true)

Every new build produced by the CI workflow is published directly to [`apk/SageLedger-latest.apk`](apk/SageLedger-latest.apk?raw=true) along with [`apk/version.json`](apk/version.json) for in-app update notifications.

---

## What is SageLedger?

**SageLedger** is a personal finance and expense-tracking Android app built to eliminate manual spreadsheet entry without forcing you to link bank passwords.

Instead of typing every coffee, ride, or online checkout by hand, SageLedger listens locally on your device for payment notifications—such as **GCash**, **Maya**, **GrabPay**, **Google Wallet**, credit card taps, and bank SMS alerts—extracts the merchant and amount, categorizes the purchase automatically, and updates your monthly budget pace in real time. Cash purchases and custom receipts can still be logged in seconds with manual entry, SMS snippet paste, or one-tap recurring shortcuts.

Designed primarily with **Philippine Peso (`PHP / ₱`)** workflows in mind, SageLedger also supports **USD, EUR, GBP, JPY, SGD, AUD, and CAD**.

---

## Features

### 1. Automatic Notification Capture
- **E-Wallets & Tap-to-Pay:** Captures spending alerts from **GCash, Maya, GrabPay, ShopeePay, PayPal, Coins.ph**, and **Google Wallet / Google Pay**.
- **Bank Cards & SMS Alerts:** Parses debit/credit card charges and bank transaction SMS messages while automatically ignoring OTPs, 2FA codes, failed payments, and promotional spam.
- **Quick Review Queue:** Newly captured alerts appear in your **To Confirm** filter and trigger an optional confirmation notification so you can verify or tweak them in one tap.

### 2. Fast Manual & Cash Tracking
- **Cash & Manual Entry:** Record cash purchases, split bills, or offline receipts cleanly with amount, merchant, payment source, category, date/time, and notes.
- **Paste Alert Snippet:** Copy-paste any bank SMS or receipt notification directly into the entry sheet to auto-fill merchant, amount, channel, and suggested category.
- **1-Tap Recurring Shortcuts:** Create quick-add buttons for everyday habits (morning coffee, daily commute, subscriptions) and log them in a single tap.

### 3. Intelligent Store Categorization
- **Custom Store Rules:** Teach SageLedger once (e.g., `"DataBlitz" → Gadgets & Tech`) and future purchases match your rule automatically.
- **Built-in Store Dictionary:** Recognizes popular Philippine and international stores across 8 categories:
  - *Food & Dining*, *Groceries*, *Fuel, Transpo & Tolls*, *Convenience Stores*, *Gadgets & Tech*, *Bills & Utilities*, *Shopping*, and *Other*.
- **AI Fallback Categorization:** Unfamiliar merchants are classified seamlessly using Gemini via Firebase AI Logic.

### 4. Monthly Budget Pacing & Savings Projection
- **Live Hero Budget Ring:** See total month-to-date spending against your monthly budget at a glance.
- **Safe Daily Allowance:** Automatically calculates how much you can spend per remaining day in the month without breaking your budget.
- **Savings Projection & Streaks:** Projects month-end spending vs. income and tracks your no-spend days for the month.
- **Category Limits:** Set tailored monthly limits for each of the 8 spending categories and monitor progress bars in the **Plan** tab.

### 5. Period Reports & Channel Insights
- **Flexible Timeframes:** Inspect spending by **Day, Week, Month, Year**, or any **Custom Date Range**.
- **Interactive Visuals:** Toggle between **Category Donut** and **Trend Bar** charts, view top merchants, and compare spend across payment channels (*E-Wallet, GPay, Card, SMS, Cash, Manual*).

### 6. Cloud Sync, Themes & Self-Updating Builds
- **Google Sign-In & Firestore Sync:** Secure per-user cloud storage backed by Firebase Authentication (Android Credential Manager) and strict Firestore security rules.
- **Light & Charcoal Dark Mode:** Editorial light and low-glare charcoal dark themes, controllable in Settings (*System / Light / Dark*).
- **In-App GitHub Update Checker:** Enter your `owner/repo` in **Settings → App Updates** to check GitHub Releases and `apk/version.json` for new builds.

---

## How It Works

1. **Onboard & Sign In:** Step through the 3-card introduction and sign in with your Google account.
2. **Optional Auto-Capture Setup:** Grant **Notification Listener** access so SageLedger can detect payment alerts as they arrive on your phone.
3. **Track Effortlessly:** Let digital payments log themselves, tap shortcuts for recurring spend, or use `+` to record cash transactions.
4. **Stay on Pace:** Check your daily allowance on **Overview**, explore breakdowns in **Reports**, and fine-tune category targets in **Plan**.

---

## Install on Android

1. Tap [**Download `SageLedger-latest.apk`**](apk/SageLedger-latest.apk?raw=true) on your Android phone (Android 7.0 / API 24 or higher).
2. Open the downloaded APK from your notifications or Files app.
3. If prompted, allow your browser or file manager to **Install unknown apps**, then confirm **Install**.
4. Open **SageLedger**, complete onboarding, and sign in with Google.
5. *(Recommended)* Open **Settings → Automatic Purchase Capture** inside the app to enable **Notification Listener** access and **Confirmation Popups**.

---

## Tech Stack

- **UI & Architecture:** Kotlin, Jetpack Compose, Material Design 3, Navigation Compose, ViewModel + StateFlow (MVVM)
- **Backend & Auth:** Firebase Authentication (Google Sign-In via Jetpack Credential Manager), Cloud Firestore (with strict per-user security rules), Firebase App Check
- **AI Categorization:** Firebase AI Logic (`gemini-3.8-flash`) with deterministic local dictionary & user-defined merchant rule precedence
- **Networking & Preferences:** Retrofit, OkHttp, Moshi, Jetpack DataStore
- **CI/CD:** Automated `.apk` builds & unit/Robolectric tests via [`.github/workflows/update-apk.yml`](.github/workflows/update-apk.yml) (see [`apk/README.md`](apk/README.md) for required repository secrets)
