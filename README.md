# SageLedger

Minimalist full-stack spending cockpit built with React, TypeScript, Tailwind CSS, Express, Firebase Auth + Firestore, and server-side Gemini AI merchant categorization.

## Features
- **Live Spend Sparkline & Bento Metrics**: Real-time month-to-date spending curve, projected end-of-month pace, safe daily allowance, and no-spend streak counter.
- **Smart Alert Parsing & Categorization**: Paste GCash, Maya, Google Pay, bank card, or SMS payment alerts to extract merchant, amount, and payment channel—paired with custom merchant rules and server-side Gemini AI fallback (`/api/categorize`).
- **Interactive Reports & Budgets**: Donut and trend bar charts across Day, Week, Month, Year, and custom periods, category budget rings, recurring 1-tap shortcuts, and light/dark editorial themes.
- **Cloud Sync**: Google Sign-In and real-time Firestore persistence per authenticated user.

## Development
```bash
npm install
npm run dev
```

## Environment Variables
Copy `.env.example` to `.env` and set `GEMINI_API_KEY` for server-side AI merchant categorization fallback.
