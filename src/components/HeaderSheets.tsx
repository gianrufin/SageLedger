import { useState } from 'react';
import {
  Bolt,
  Check,
  CheckCheck,
  ChevronRight,
  LogOut,
  Settings,
  Sparkles,
  X,
} from 'lucide-react';
import {
  ThemeModePreference,
  type MerchantRule,
  type PaymentSource,
  type SpendingCategory,
  type SpendingTransaction,
} from '../types/ledger';
import { formatCents } from '../lib/currency';
import { parseRawAlertText } from '../lib/notificationParser';
import { categorizeWithFallback } from '../lib/categorizer';
import { CrispCard, LedgerTransactionRow } from './IconsAndVisuals';

export function AccountProfileModal({
  userName,
  userEmail,
  userInitial,
  currencyCode,
  monthlyBudgetCents,
  monthlyIncomeCents,
  themeMode,
  onSetThemeMode,
  onOpenSettings,
  onSignOut,
  onClose,
}: {
  userName: string;
  userEmail: string;
  userInitial: string;
  currencyCode: string;
  monthlyBudgetCents: number;
  monthlyIncomeCents: number;
  themeMode: ThemeModePreference;
  onSetThemeMode: (mode: ThemeModePreference) => void;
  onOpenSettings: () => void;
  onSignOut: () => void;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/55 backdrop-blur-xs p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        className="w-full sm:max-w-md rounded-t-[28px] sm:rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-5 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
        data-testid="account_profile_sheet"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-14 h-14 rounded-full bg-[var(--mint-container)] border border-[var(--mint-primary)]/45 text-[var(--mint-primary)] flex items-center justify-center text-xl font-extrabold shrink-0">
              {userInitial}
            </div>
            <div className="min-w-0">
              <h2 className="text-lg font-bold text-[var(--ink-primary)] truncate">
                {userName || 'SageLedger Account'}
              </h2>
              {userEmail && (
                <p className="text-sm text-[var(--ink-muted)] truncate">{userEmail}</p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close account modal"
            className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Budget snapshot */}
        <CrispCard
          accentGlowColor="var(--mint-primary)"
          className="p-4 bg-[var(--bg-surface-low)]"
        >
          <div className="flex items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="font-mono-data text-[10px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
                MONTHLY INCOME
              </div>
              <div className="text-lg font-extrabold tabular-nums text-[var(--ink-primary)]">
                {formatCents(monthlyIncomeCents, currencyCode)}
              </div>
            </div>
            <div className="space-y-1 text-right">
              <div className="font-mono-data text-[10px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
                MONTHLY LIMIT
              </div>
              <div className="text-lg font-extrabold tabular-nums text-[var(--mint-primary)]">
                {formatCents(monthlyBudgetCents, currencyCode)}
              </div>
            </div>
          </div>
        </CrispCard>

        {/* Theme Mode */}
        <div className="space-y-2">
          <div className="font-mono-data text-[10px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
            APPEARANCE
          </div>
          <div className="grid grid-cols-3 gap-1 p-1 rounded-[14px] bg-[var(--bg-surface-low)] border border-[var(--hairline)]">
            {[
              { mode: ThemeModePreference.SYSTEM, label: 'System' },
              { mode: ThemeModePreference.LIGHT, label: 'Light' },
              { mode: ThemeModePreference.DARK, label: 'Dark' },
            ].map(({ mode, label }) => {
              const selected = themeMode === mode;
              return (
                <button
                  key={mode}
                  type="button"
                  onClick={() => onSetThemeMode(mode)}
                  className={`py-2.5 rounded-[10px] text-xs font-bold transition cursor-pointer ${
                    selected
                      ? 'bg-[var(--mint-primary)] text-white dark:text-[#042419]'
                      : 'text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="space-y-2.5">
          <CrispCard
            onClick={() => {
              onClose();
              onOpenSettings();
            }}
            className="px-4 py-3.5 bg-[var(--bg-surface-low)]"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Settings className="w-[18px] h-[18px] text-[var(--mint-primary)]" />
                <span className="text-sm font-semibold text-[var(--ink-primary)]">
                  Budget, Store Rules &amp; Updates
                </span>
              </div>
              <ChevronRight className="w-4 h-4 text-[var(--ink-muted)]" />
            </div>
          </CrispCard>

          <button
            type="button"
            onClick={() => {
              onClose();
              onSignOut();
            }}
            data-testid="account_sheet_sign_out"
            className="w-full h-12 rounded-2xl border border-[var(--coral-tertiary)]/45 text-[var(--coral-tertiary)] font-semibold text-sm inline-flex items-center justify-center gap-2 hover:bg-[var(--coral-container)]/35 transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign out of Google</span>
          </button>
        </div>
      </div>
    </div>
  );
}

export function AlertsInboxModal({
  pendingAlerts,
  currencyCode,
  merchantRules,
  onConfirmAlert,
  onEditAlert,
  onCaptureAlertToInbox,
  onClose,
}: {
  pendingAlerts: SpendingTransaction[];
  currencyCode: string;
  merchantRules: MerchantRule[];
  onConfirmAlert: (tx: SpendingTransaction) => void;
  onEditAlert: (tx: SpendingTransaction) => void;
  onCaptureAlertToInbox: (payload: {
    merchant: string;
    amountCents: number;
    category: SpendingCategory;
    paymentSource: PaymentSource;
    rawSnippet: string;
  }) => Promise<void>;
  onClose: () => void;
}) {
  const [alertText, setAlertText] = useState('');
  const [isParsing, setIsParsing] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);

  const handleCapture = async () => {
    const parsed = parseRawAlertText(alertText);
    if (!parsed) {
      setParseError('Could not detect a purchase amount (ignoring OTP, promo, or refund text).');
      return;
    }
    setParseError(null);
    setIsParsing(true);
    try {
      const { category } = await categorizeWithFallback(
        parsed.merchant,
        parsed.rawSnippet,
        merchantRules
      );
      await onCaptureAlertToInbox({
        merchant: parsed.merchant,
        amountCents: parsed.amountCents,
        category,
        paymentSource: parsed.paymentSource,
        rawSnippet: parsed.rawSnippet,
      });
      setAlertText('');
    } finally {
      setIsParsing(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/55 backdrop-blur-xs p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        className="w-full sm:max-w-lg rounded-t-[28px] sm:rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4 max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
        data-testid="alerts_inbox_sheet"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-[var(--ink-primary)]">Alerts Inbox</h2>
            <p className="text-xs text-[var(--ink-muted)] mt-0.5">
              {pendingAlerts.length === 0
                ? 'Smart alert parser & pending confirmations'
                : `${pendingAlerts.length} awaiting your confirmation`}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                pendingAlerts.length === 0
                  ? 'bg-[var(--mint-container)] text-[var(--on-mint-container)]'
                  : 'bg-[var(--coral-container)] text-[var(--on-coral-container)]'
              }`}
            >
              {pendingAlerts.length === 0 ? 'Caught up' : `${pendingAlerts.length} pending`}
            </span>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close alerts inbox"
              className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Interactive Alert Parser for Web */}
        <CrispCard className="p-3.5 bg-[var(--bg-surface-low)] space-y-2.5">
          <div className="flex items-center gap-2 text-xs font-semibold text-[var(--ink-primary)]">
            <Bolt className="w-4 h-4 text-[var(--sky-secondary)]" />
            <span>Capture E-Wallet, Google Pay, Card, or SMS Alert</span>
          </div>
          <textarea
            value={alertText}
            onChange={(e) => {
              setAlertText(e.target.value);
              if (parseError) setParseError(null);
            }}
            rows={2}
            placeholder="Paste alert text, e.g. Paid ₱245.00 via GCash at 7-Eleven"
            className="w-full rounded-xl bg-[var(--bg-surface)] border border-[var(--hairline)] px-3 py-2 text-sm text-[var(--ink-primary)] placeholder:text-[var(--ink-muted)] focus:outline-none focus:border-[var(--mint-primary)]"
          />
          {parseError && <p className="text-xs text-[var(--coral-tertiary)]">{parseError}</p>}
          <div className="flex items-center justify-between gap-2">
            <button
              type="button"
              onClick={() =>
                setAlertText('You paid ₱385.00 to Starbucks Reserve via GCash QRPh')
              }
              className="text-xs font-semibold text-[var(--ink-muted)] hover:text-[var(--ink-primary)] cursor-pointer"
            >
              Try sample alert
            </button>
            <button
              type="button"
              disabled={!alertText.trim() || isParsing}
              onClick={handleCapture}
              className="inline-flex items-center gap-1.5 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-xs px-3.5 py-2 disabled:opacity-50 cursor-pointer"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{isParsing ? 'Parsing...' : 'Parse to Inbox'}</span>
            </button>
          </div>
        </CrispCard>

        {/* Pending Alerts List */}
        <div className="flex-1 overflow-y-auto space-y-2.5 pr-0.5">
          {pendingAlerts.length === 0 ? (
            <CrispCard className="p-6 bg-[var(--bg-surface-low)] text-center space-y-2">
              <div className="w-10 h-10 rounded-full bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center mx-auto">
                <CheckCheck className="w-5 h-5" />
              </div>
              <div className="font-bold text-sm text-[var(--ink-primary)]">All alerts caught up</div>
              <p className="text-xs text-[var(--ink-muted)]">
                Parsed Google Pay, GCash/Maya E-Wallet, bank card, and SMS alerts appear here for
                1-tap approval.
              </p>
            </CrispCard>
          ) : (
            pendingAlerts.map((tx) => (
              <div key={tx.id} className="rounded-2xl border border-[var(--hairline)] overflow-hidden">
                <LedgerTransactionRow
                  transaction={tx}
                  currencyCode={currencyCode}
                  onClick={() => {
                    onClose();
                    onEditAlert(tx);
                  }}
                  onConfirm={onConfirmAlert}
                  showDivider={false}
                />
                <div className="flex items-center justify-end gap-2 px-3 pb-2.5 bg-[var(--bg-surface)]">
                  <button
                    type="button"
                    onClick={() => {
                      onClose();
                      onEditAlert(tx);
                    }}
                    className="text-xs font-semibold text-[var(--ink-muted)] hover:text-[var(--ink-primary)] px-2.5 py-1 cursor-pointer"
                  >
                    Edit category
                  </button>
                  <button
                    type="button"
                    onClick={() => onConfirmAlert(tx)}
                    className="inline-flex items-center gap-1 rounded-full bg-[var(--mint-container)] text-[var(--on-mint-container)] text-xs font-bold px-3 py-1 cursor-pointer"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>Confirm</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
