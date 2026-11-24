import React, { useState } from 'react';
import {
  ArrowRight,
  Bolt,
  Cloud,
  PiggyBank,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { signInWithPopup } from 'firebase/auth';
import { auth, googleProvider } from '../lib/firebase';
import { CrispCard } from './IconsAndVisuals';

interface OnboardingPage {
  stepNum: string;
  titleLine1: string;
  titleLine2: string;
  description: string;
  highlightEyebrow: string;
  cardTitle: string;
  cardSubtitle: string;
  icon: React.ReactNode;
}

const ONBOARDING_PAGES: OnboardingPage[] = [
  {
    stepNum: '01 / 03',
    titleLine1: 'Your Money,',
    titleLine2: 'Your Future.',
    description:
      'Understand your finances, build better habits, and grow with confidence using our sleek editorial dashboard.',
    highlightEyebrow: 'SMART SPENDING COCKPIT',
    cardTitle: 'Live Net Worth & Sparkline',
    cardSubtitle: 'Track daily pace and month-end projections effortlessly.',
    icon: <PiggyBank className="w-9 h-9" />,
  },
  {
    stepNum: '02 / 03',
    titleLine1: 'Instant Alert',
    titleLine2: '& E-Wallet Scan.',
    description:
      'Never miss a single transaction. SageLedger parses Google Wallet, GCash, Maya, bank card, and SMS purchase alerts.',
    highlightEyebrow: 'SMART ALERT PARSING',
    cardTitle: 'GCash, Maya & Google Pay',
    cardSubtitle: 'Paste any payment alert to extract merchant, source, and category in one tap.',
    icon: <Bolt className="w-9 h-9" />,
  },
  {
    stepNum: '03 / 03',
    titleLine1: 'Manual Cash',
    titleLine2: '& Safe Allowances.',
    description:
      'Add physical cash purchases in seconds, monitor your safe-to-spend daily allowance, and maintain your no-spend streak.',
    highlightEyebrow: 'FLEXIBLE CONTROL',
    cardTitle: 'Cash & Store Categories',
    cardSubtitle: 'Customizable category budgets and instant merchant rules.',
    icon: <Wallet className="w-9 h-9" />,
  },
];

export function OnboardingScreen({ onFinish }: { onFinish: () => void }) {
  const [pageIdx, setPageIdx] = useState(0);
  const page = ONBOARDING_PAGES[pageIdx];

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)] text-[var(--ink-primary)] flex items-center justify-center p-5 sm:p-8">
      <div className="w-full max-w-[520px] min-h-[600px] flex flex-col justify-between gap-8">
        <div className="flex items-center justify-between">
          <span className="font-mono-data text-xs font-bold tracking-[0.12em] uppercase text-[var(--mint-primary)]">
            {page.stepNum}
          </span>
          <button
            type="button"
            onClick={onFinish}
            data-testid="onboarding_skip_button"
            className="text-sm font-bold text-[var(--ink-muted)] hover:text-[var(--ink-primary)] px-2 py-1 transition cursor-pointer"
          >
            Skip
          </button>
        </div>

        <div className="space-y-6 flex-1 flex flex-col justify-center">
          <div className="space-y-2.5">
            <h1 className="text-4xl sm:text-[42px] font-extrabold leading-[1.1] tracking-tight">
              {page.titleLine1}
              <br />
              {page.titleLine2}
            </h1>
            <p className="text-base text-[var(--ink-muted)] leading-relaxed">{page.description}</p>
          </div>

          <CrispCard
            accentGlowColor="var(--mint-primary)"
            className="p-7 sm:p-8 rounded-[28px] flex flex-col items-center text-center gap-4"
          >
            <div className="w-[72px] h-[72px] rounded-full bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center">
              {page.icon}
            </div>
            <div className="space-y-1">
              <div className="font-mono-data text-[11px] font-bold tracking-[0.1em] uppercase text-[var(--mint-primary)]">
                {page.highlightEyebrow}
              </div>
              <h2 className="text-xl font-bold text-[var(--ink-primary)]">{page.cardTitle}</h2>
              <p className="text-sm text-[var(--ink-muted)] max-w-sm mx-auto">{page.cardSubtitle}</p>
            </div>
          </CrispCard>
        </div>

        <div className="space-y-5">
          <div className="flex items-center justify-center gap-2">
            {ONBOARDING_PAGES.map((p, idx) => (
              <button
                key={p.stepNum}
                type="button"
                aria-label={`Step ${idx + 1}`}
                onClick={() => setPageIdx(idx)}
                className={`h-2 rounded-full transition-all cursor-pointer ${
                  idx === pageIdx
                    ? 'w-6 bg-[var(--mint-primary)]'
                    : 'w-2 bg-[var(--bg-highest)]'
                }`}
              />
            ))}
          </div>

          <button
            type="button"
            onClick={() => {
              if (pageIdx < ONBOARDING_PAGES.length - 1) {
                setPageIdx((i) => i + 1);
              } else {
                onFinish();
              }
            }}
            data-testid="onboarding_next_button"
            className="w-full h-14 rounded-2xl bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-base inline-flex items-center justify-center gap-2 hover:opacity-95 transition cursor-pointer"
          >
            <span>{pageIdx < ONBOARDING_PAGES.length - 1 ? 'Continue' : 'Get Started'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

export function AuthScreen() {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleGoogleSignIn = async () => {
    setErrorMessage(null);
    setIsLoading(true);
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Google Sign-In failed.';
      if (!msg.includes('popup-closed-by-user')) {
        setErrorMessage(msg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen bg-[var(--bg-canvas)] text-[var(--ink-primary)] flex items-center justify-center p-5 sm:p-8"
      style={{
        backgroundImage:
          'radial-gradient(600px circle at 25% 12%, color-mix(in srgb, var(--mint-primary) 14%, transparent), transparent 70%), radial-gradient(480px circle at 85% 42%, color-mix(in srgb, var(--sky-secondary) 9%, transparent), transparent 70%)',
      }}
    >
      <div className="w-full max-w-[520px] flex flex-col justify-between gap-8 min-h-[600px]">
        <div className="space-y-6">
          {/* Brand Lockup */}
          <div className="flex items-center gap-3">
            <div className="w-[42px] h-[42px] rounded-[14px] bg-[var(--mint-container)] border border-[var(--mint-primary)]/40 text-[var(--mint-primary)] flex items-center justify-center">
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-extrabold tracking-tight leading-none">SageLedger</div>
              <div className="font-mono-data text-[10px] font-bold tracking-[0.12em] uppercase text-[var(--ink-muted)] mt-1">
                SMART SPENDING COCKPIT
              </div>
            </div>
          </div>

          {/* Display Headline */}
          <div className="space-y-2.5">
            <h1 className="text-4xl sm:text-[42px] font-extrabold leading-[1.08] tracking-tight">
              Every peso, mapped live.
            </h1>
            <p className="text-base text-[var(--ink-muted)] leading-relaxed">
              Capture E-Wallet, Google Pay, card, and SMS alerts effortlessly—paired with a live
              daily spending sparkline.
            </p>
          </div>

          {/* Bento Capability Grid */}
          <div className="space-y-2.5">
            <CrispCard accentGlowColor="var(--sky-secondary)" className="p-4">
              <div className="flex items-start gap-3.5">
                <div className="w-[42px] h-[42px] rounded-[14px] bg-[var(--sky-container)] border border-[var(--sky-secondary)]/30 text-[var(--sky-secondary)] flex items-center justify-center shrink-0">
                  <Bolt className="w-5 h-5" />
                </div>
                <div className="space-y-1">
                  <div className="font-mono-data text-[10px] font-bold tracking-[0.1em] uppercase text-[var(--sky-secondary)]">
                    SMART PARSE &amp; AI
                  </div>
                  <div className="font-bold text-[15px]">E-Wallet, GPay, Card &amp; SMS</div>
                  <p className="text-xs text-[var(--ink-muted)] leading-relaxed">
                    Detects GCash, Maya, GrabPay, PayPal, Google Pay &amp; bank alerts with 1-tap
                    confirmation.
                  </p>
                </div>
              </div>
            </CrispCard>

            <div className="grid grid-cols-2 gap-2.5">
              <CrispCard accentGlowColor="var(--mint-primary)" className="p-4 space-y-2.5">
                <div className="w-9 h-9 rounded-xl bg-[var(--mint-container)] border border-[var(--mint-primary)]/30 text-[var(--mint-primary)] flex items-center justify-center">
                  <TrendingUp className="w-[18px] h-[18px]" />
                </div>
                <div>
                  <div className="font-bold text-sm">Live Pace</div>
                  <p className="text-xs text-[var(--ink-muted)] mt-0.5">
                    Daily safe-to-spend &amp; no-spend streaks
                  </p>
                </div>
              </CrispCard>

              <CrispCard accentGlowColor="var(--amber-accent)" className="p-4 space-y-2.5">
                <div className="w-9 h-9 rounded-xl bg-[var(--bg-surface-low)] border border-[var(--amber-accent)]/30 text-[var(--amber-accent)] flex items-center justify-center">
                  <Cloud className="w-[18px] h-[18px]" />
                </div>
                <div>
                  <div className="font-bold text-sm">Private Sync</div>
                  <p className="text-xs text-[var(--ink-muted)] mt-0.5">
                    Secured to your Google account
                  </p>
                </div>
              </CrispCard>
            </div>
          </div>
        </div>

        <div className="space-y-3">
          {errorMessage && (
            <div className="rounded-2xl bg-[var(--coral-container)] border border-[var(--coral-tertiary)]/35 text-[var(--on-coral-container)] text-sm p-3.5">
              {errorMessage}
            </div>
          )}

          <button
            type="button"
            disabled={isLoading}
            onClick={handleGoogleSignIn}
            data-testid="sign_in_google_button"
            className="w-full h-14 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-base inline-flex items-center justify-center gap-2.5 hover:opacity-95 disabled:opacity-60 transition cursor-pointer"
          >
            {isLoading ? (
              <span className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin" />
            ) : (
              <span>Continue with Google</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
