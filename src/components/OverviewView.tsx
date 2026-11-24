import { useMemo, useState } from 'react';
import {
  Bell,
  ChevronDown,
  ChevronRight,
  Flame,
  PiggyBank,
  Plus,
  ReceiptText,
  Search,
  Shield,
  Sparkles,
  Wallet,
  X,
  Zap,
} from 'lucide-react';
import {
  CATEGORY_META,
  PaymentSource,
  ThemeModePreference,
  type DashboardSnapshot,
  type GitHubReleaseNotice,
  type RecurringPreset,
  type SpendingTransaction,
} from '../types/ledger';
import { formatCents, formatCompactCents } from '../lib/currency';
import { groupTransactionsByDay } from '../lib/analytics';
import { LiveSpendSparklineChart } from './Charts';
import { CrispCard, LedgerTransactionRow, SplitCurrencyText } from './IconsAndVisuals';

type TransactionFilter =
  | 'ALL'
  | 'E_WALLET'
  | 'GOOGLE_PAY'
  | 'CREDIT_CARD'
  | 'SMS_ALERT'
  | 'CASH'
  | 'NEEDS_REVIEW';

const FILTER_OPTIONS: Array<{
  key: TransactionFilter;
  label: string;
  source?: PaymentSource;
}> = [
  { key: 'ALL', label: 'All' },
  { key: 'E_WALLET', label: 'E-Wallet', source: PaymentSource.E_WALLET },
  { key: 'GOOGLE_PAY', label: 'GPay', source: PaymentSource.GOOGLE_PAY },
  { key: 'CREDIT_CARD', label: 'Card', source: PaymentSource.CREDIT_CARD },
  { key: 'SMS_ALERT', label: 'SMS', source: PaymentSource.SMS_ALERT },
  { key: 'CASH', label: 'Cash', source: PaymentSource.CASH },
  { key: 'NEEDS_REVIEW', label: 'To Confirm' },
];

export function OverviewView({
  snapshot,
  loading,
  error,
  updateNotice,
  userInitial,
  userName,
  onQuickAddPreset,
  onAddPresetClicked,
  onTransactionClick,
  onConfirmTransaction,
  onManualEntryClick,
  onOpenAccount,
  onOpenAlerts,
  onDismissUpdate,
  onNavigateToReports,
  onNavigateToPlan,
}: {
  snapshot: DashboardSnapshot | null;
  loading: boolean;
  error: string | null;
  updateNotice: GitHubReleaseNotice | null;
  userInitial: string;
  userName: string;
  themeMode: ThemeModePreference;
  onQuickAddPreset: (preset: RecurringPreset) => void;
  onAddPresetClicked: () => void;
  onTransactionClick: (tx: SpendingTransaction) => void;
  onConfirmTransaction: (tx: SpendingTransaction) => void;
  onManualEntryClick: () => void;
  onOpenAccount: () => void;
  onOpenAlerts: () => void;
  onDismissUpdate: (permanentlyForTag: boolean) => void;
  onNavigateToReports: () => void;
  onNavigateToPlan: () => void;
}) {
  const [showSearchField, setShowSearchField] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<TransactionFilter>('ALL');

  const monthLabel = useMemo(
    () =>
      new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric' }).format(new Date()),
    []
  );
  const monthHeader = useMemo(
    () =>
      `${new Intl.DateTimeFormat('en-US', { month: 'long' }).format(new Date()).toUpperCase()} SPEND`,
    []
  );

  const firstName = useMemo(() => {
    const first = userName.trim().split(/\s+/)[0];
    return first || 'SageLedger';
  }, [userName]);

  const filteredTransactions = useMemo(() => {
    if (!snapshot) return [];
    const q = searchQuery.trim().toLowerCase();
    const filterOption = FILTER_OPTIONS.find((f) => f.key === activeFilter);
    return snapshot.transactions
      .filter((tx) => {
        const matchesFilter =
          activeFilter === 'ALL'
            ? true
            : activeFilter === 'NEEDS_REVIEW'
              ? tx.needsReview
              : tx.paymentSource === filterOption?.source;
        const matchesQuery =
          !q ||
          tx.merchant.toLowerCase().includes(q) ||
          CATEGORY_META[tx.category].displayName.toLowerCase().includes(q) ||
          tx.note.toLowerCase().includes(q);
        return matchesFilter && matchesQuery;
      })
      .slice(0, 30);
  }, [snapshot, searchQuery, activeFilter]);

  const groupedTransactions = useMemo(
    () => groupTransactionsByDay(filteredTransactions),
    [filteredTransactions]
  );

  if (loading && !snapshot) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-8 h-8 border-2 border-[var(--mint-primary)] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error && !snapshot) {
    return (
      <div className="max-w-xl mx-auto p-6">
        <div className="rounded-2xl bg-[var(--coral-container)] border border-[var(--coral-tertiary)]/35 text-[var(--on-coral-container)] p-5 text-sm">
          {error}
        </div>
      </div>
    );
  }

  if (!snapshot) return null;

  const { settings, savingsProjection: proj } = snapshot;
  const currency = settings.currencyCode;
  const safeTarget = Math.max(1, settings.monthlyBudgetCents);
  const remainingCents = settings.monthlyBudgetCents - snapshot.monthSpentCents;
  const isOver = remainingCents < 0;
  const pctUsed = Math.round((snapshot.monthSpentCents / safeTarget) * 100);
  const heroAccent = isOver ? 'var(--coral-tertiary)' : 'var(--mint-primary)';
  const projectedPositive = proj.projectedSavingsCents >= 0;
  const absProjected = Math.abs(proj.projectedSavingsCents);
  const projectedPrefix =
    projectedPositive && absProjected > 0 ? '+' : !projectedPositive ? '-' : '';

  return (
    <div className="w-full max-w-[760px] mx-auto px-4 pt-3 pb-28 md:pb-12 space-y-4">
      {/* Update notice dialog */}
      {updateNotice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4">
          <div
            className="w-full max-w-md rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4"
            data-testid="github_update_dialog"
          >
            <div className="flex items-center gap-2.5 text-[var(--mint-primary)]">
              <Sparkles className="w-5 h-5" />
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">
                Update available ({updateNotice.tagName})
              </h2>
            </div>
            <div className="space-y-1.5">
              <div className="text-sm font-semibold text-[var(--ink-primary)]">
                {updateNotice.releaseName}
              </div>
              <p className="text-sm text-[var(--ink-muted)] whitespace-pre-line">
                {updateNotice.notes.slice(0, 360)}
              </p>
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => onDismissUpdate(true)}
                data-testid="github_update_dismiss_button"
                className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] hover:text-[var(--ink-primary)] cursor-pointer"
              >
                Skip {updateNotice.tagName}
              </button>
              <a
                href={updateNotice.htmlUrl}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => onDismissUpdate(false)}
                data-testid="github_update_open_button"
                className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold"
              >
                {updateNotice.htmlUrl.toLowerCase().endsWith('.apk')
                  ? 'Download APK'
                  : 'View release'}
              </a>
            </div>
          </div>
        </div>
      )}

      {/* Top Utility Bar */}
      <div className="flex items-center justify-between py-1">
        <button
          type="button"
          onClick={onOpenAccount}
          data-testid="header_avatar_button"
          className="flex items-center gap-2.5 pr-3 py-1 rounded-full hover:bg-[var(--bg-surface)]/70 transition text-left cursor-pointer"
        >
          <div className="w-9 h-9 rounded-full bg-[var(--mint-container)] border border-[var(--mint-primary)]/45 text-[var(--mint-primary)] flex items-center justify-center text-sm font-extrabold">
            {userInitial}
          </div>
          <div>
            <div className="text-sm font-bold text-[var(--ink-primary)] leading-tight truncate max-w-[160px]">
              {firstName}
            </div>
            <div className="text-[11px] text-[var(--ink-muted)] leading-tight">
              Account &amp; theme
            </div>
          </div>
        </button>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onNavigateToReports}
            className="inline-flex items-center gap-1 rounded-full bg-[var(--bg-surface)] border border-[var(--hairline)] px-3 py-2 text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/40 transition cursor-pointer"
          >
            <span>{monthLabel}</span>
            <ChevronDown className="w-3.5 h-3.5 text-[var(--ink-muted)]" />
          </button>

          <button
            type="button"
            onClick={onOpenAlerts}
            data-testid="header_bell_button"
            aria-label="Open Alerts Inbox"
            className="relative w-10 h-10 rounded-full bg-[var(--bg-surface)] border border-[var(--hairline)] flex items-center justify-center text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/40 transition cursor-pointer"
          >
            <Bell className="w-[18px] h-[18px]" />
            {snapshot.unreviewedCount > 0 && (
              <span className="absolute top-2 right-2 w-2.5 h-2.5 rounded-full bg-[var(--coral-tertiary)] border-2 border-[var(--bg-surface)]" />
            )}
          </button>
        </div>
      </div>

      {/* Live Sparkline Hero Bento */}
      <CrispCard accentGlowColor={heroAccent} className="p-5 sm:p-6 rounded-[26px] space-y-3.5">
        <div className="flex items-center justify-between gap-2">
          <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
            {monthHeader}
          </span>
          <span
            className={`font-mono-data text-[10.5px] font-bold tracking-[0.06em] uppercase px-2.5 py-1 rounded-full ${
              isOver
                ? 'bg-[var(--coral-container)] text-[var(--on-coral-container)]'
                : 'bg-[var(--mint-container)] text-[var(--on-mint-container)]'
            }`}
          >
            {pctUsed}% OF {formatCompactCents(settings.monthlyBudgetCents, currency)}
          </span>
        </div>

        <div className="space-y-1">
          <SplitCurrencyText
            amountCents={snapshot.monthSpentCents}
            currencyCode={currency}
            className="text-4xl sm:text-[44px] tracking-tight text-[var(--ink-primary)]"
          />
          <p
            className={`text-xs font-medium ${
              isOver ? 'text-[var(--coral-tertiary)]' : 'text-[var(--ink-muted)]'
            }`}
          >
            {isOver
              ? `Over limit by ${formatCents(-remainingCents, currency)}`
              : `${formatCents(remainingCents, currency)} remaining this month`}
          </p>
        </div>

        <LiveSpendSparklineChart projection={proj} currencyCode={currency} />
      </CrispCard>

      {/* 2x2 Bento Metrics Grid */}
      <div className="grid grid-cols-2 gap-2.5">
        <CrispCard accentGlowColor={isOver ? 'var(--coral-tertiary)' : 'var(--sky-secondary)'} className="p-4 space-y-2.5">
          <div className="flex items-center gap-2">
            <div
              className={`w-7 h-7 rounded-[10px] flex items-center justify-center ${
                isOver
                  ? 'bg-[var(--coral-container)] text-[var(--coral-tertiary)]'
                  : 'bg-[var(--sky-container)] text-[var(--sky-secondary)]'
              }`}
            >
              <Shield className="w-3.5 h-3.5" />
            </div>
            <span className="font-mono-data text-[9.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)] truncate">
              SAFE TODAY
            </span>
          </div>
          <div data-testid="safe_to_spend_pill">
            <div
              className={`text-lg sm:text-xl font-extrabold tabular-nums truncate ${
                isOver ? 'text-[var(--coral-tertiary)]' : 'text-[var(--ink-primary)]'
              }`}
            >
              {isOver || proj.dailyAllowanceCents <= 0
                ? 'Limit hit'
                : formatCents(proj.dailyAllowanceCents, currency)}
            </div>
            <div className="text-xs text-[var(--ink-muted)] truncate">
              {isOver || proj.dailyAllowanceCents <= 0
                ? `${proj.remainingDaysInMonth} days left`
                : `per day · ${proj.remainingDaysInMonth}d left`}
            </div>
          </div>
        </CrispCard>

        <CrispCard accentGlowColor="var(--mint-primary)" className="p-4 space-y-2.5">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-[10px] bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center">
              <Flame className="w-3.5 h-3.5" />
            </div>
            <span className="font-mono-data text-[9.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)] truncate">
              NO-SPEND STREAK
            </span>
          </div>
          <div data-testid="no_spend_days_badge">
            <div className="text-lg sm:text-xl font-extrabold tabular-nums text-[var(--mint-primary)] truncate">
              {proj.noSpendDaysThisMonth} {proj.noSpendDaysThisMonth === 1 ? 'day' : 'days'}
            </div>
            <div className="text-xs text-[var(--ink-muted)] truncate">zero spend this month</div>
          </div>
        </CrispCard>

        <CrispCard
          accentGlowColor="var(--amber-accent)"
          onClick={onNavigateToPlan}
          className="p-4 space-y-2.5"
        >
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-[10px] bg-[var(--bg-surface-low)] text-[var(--amber-accent)] flex items-center justify-center">
              <Wallet className="w-3.5 h-3.5" />
            </div>
            <span className="font-mono-data text-[9.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)] truncate">
              MONTHLY INCOME
            </span>
          </div>
          <div>
            <div className="text-lg sm:text-xl font-extrabold tabular-nums text-[var(--ink-primary)] truncate">
              {formatCompactCents(settings.monthlyIncomeCents, currency)}
            </div>
            <div className="text-xs text-[var(--ink-muted)] truncate">
              Limit {formatCompactCents(settings.monthlyBudgetCents, currency)}
            </div>
          </div>
        </CrispCard>

        <CrispCard
          accentGlowColor={projectedPositive ? 'var(--mint-primary)' : 'var(--coral-tertiary)'}
          onClick={onNavigateToReports}
          className="p-4 space-y-2.5"
        >
          <div className="flex items-center gap-2">
            <div
              className={`w-7 h-7 rounded-[10px] flex items-center justify-center ${
                projectedPositive
                  ? 'bg-[var(--mint-container)] text-[var(--mint-primary)]'
                  : 'bg-[var(--coral-container)] text-[var(--coral-tertiary)]'
              }`}
            >
              <PiggyBank className="w-3.5 h-3.5" />
            </div>
            <span className="font-mono-data text-[9.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)] truncate">
              {projectedPositive ? 'PROJECTED SAVED' : 'PROJECTED OVER'}
            </span>
          </div>
          <div>
            <div
              className={`text-lg sm:text-xl font-extrabold tabular-nums truncate ${
                projectedPositive ? 'text-[var(--mint-primary)]' : 'text-[var(--coral-tertiary)]'
              }`}
            >
              {projectedPrefix}
              {formatCompactCents(absProjected, currency)}
            </div>
            <div className="text-xs text-[var(--ink-muted)] truncate">month-end pace</div>
          </div>
        </CrispCard>
      </div>

      {/* Unreviewed inbox strip */}
      {snapshot.unreviewedCount > 0 && (
        <CrispCard
          onClick={onOpenAlerts}
          className="px-4 py-3 bg-[var(--coral-container)] border-[var(--coral-tertiary)]/35"
        >
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0">
              <Zap className="w-4 h-4 text-[var(--coral-tertiary)] shrink-0" />
              <span className="text-sm font-semibold text-[var(--on-coral-container)] truncate">
                {snapshot.unreviewedCount} captured alert
                {snapshot.unreviewedCount > 1 ? 's' : ''} ready · Review inbox
              </span>
            </div>
            <ChevronRight className="w-4 h-4 text-[var(--coral-tertiary)] shrink-0" />
          </div>
        </CrispCard>
      )}

      {/* Quick Log Strip */}
      <div className="space-y-2">
        <div className="flex items-center justify-between px-1">
          <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
            QUICK LOG
          </span>
          <button
            type="button"
            onClick={onAddPresetClicked}
            data-testid="add_preset_button"
            className="text-xs font-bold text-[var(--mint-primary)] hover:underline cursor-pointer"
          >
            + New shortcut
          </button>
        </div>

        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {snapshot.recurringPresets.map((preset) => (
            <button
              key={preset.id}
              type="button"
              onClick={() => onQuickAddPreset(preset)}
              data-testid={`quick_add_chip_${preset.id}`}
              className="shrink-0 inline-flex items-center gap-2 rounded-2xl bg-[var(--bg-surface)] border border-[var(--hairline)] px-3.5 py-2.5 hover:border-[var(--mint-primary)]/45 transition cursor-pointer"
            >
              <span className="text-xs font-semibold text-[var(--ink-primary)]">
                {preset.label}
              </span>
              <span className="text-xs font-bold tabular-nums text-[var(--mint-primary)]">
                {formatCents(preset.amountCents, currency)}
              </span>
            </button>
          ))}
          {snapshot.recurringPresets.length === 0 && (
            <button
              type="button"
              onClick={onAddPresetClicked}
              className="shrink-0 inline-flex items-center gap-1.5 rounded-2xl border border-dashed border-[var(--hairline)] px-3.5 py-2 text-xs text-[var(--ink-muted)] hover:text-[var(--ink-primary)] cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Save 1-tap shortcut for coffee or commute</span>
            </button>
          )}
        </div>
      </div>

      {/* Transactions Controls */}
      <div className="space-y-2.5 pt-1">
        <div className="flex items-center justify-between px-1">
          <h2 className="text-xl font-bold tracking-tight text-[var(--ink-primary)]">
            Transactions
          </h2>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                setShowSearchField((prev) => {
                  if (prev) setSearchQuery('');
                  return !prev;
                });
              }}
              data-testid="toggle_tx_search_button"
              aria-label="Search merchants"
              className={`w-10 h-10 rounded-xl border flex items-center justify-center transition cursor-pointer ${
                showSearchField
                  ? 'bg-[var(--mint-container)] border-[var(--mint-primary)]/40 text-[var(--mint-primary)]'
                  : 'bg-[var(--bg-surface)] border-[var(--hairline)] text-[var(--ink-primary)]'
              }`}
            >
              {showSearchField ? <X className="w-4 h-4" /> : <Search className="w-4 h-4" />}
            </button>

            <button
              type="button"
              onClick={onManualEntryClick}
              data-testid="manual_reconcile_link"
              className="inline-flex items-center gap-1.5 h-10 px-3 rounded-xl bg-[var(--bg-surface)] border border-[var(--hairline)] text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/40 transition cursor-pointer"
            >
              <Plus className="w-4 h-4 text-[var(--mint-primary)]" />
              <span>Reconcile</span>
            </button>
          </div>
        </div>

        {showSearchField && (
          <div className="relative">
            <Search className="w-4 h-4 text-[var(--ink-muted)] absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search store, category, or note..."
              data-testid="tx_search_input"
              className="w-full rounded-2xl bg-[var(--bg-surface)] border border-[var(--hairline)] pl-10 pr-9 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--ink-muted)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        )}

        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {FILTER_OPTIONS.map((f) => {
            const selected = activeFilter === f.key;
            let selectedClasses =
              'bg-[var(--mint-primary)] text-white dark:text-[#042419] border-transparent';
            if (f.key === 'E_WALLET') {
              selectedClasses = 'bg-[var(--sky-secondary)] text-[#061624] border-transparent';
            } else if (f.key === 'NEEDS_REVIEW') {
              selectedClasses = 'bg-[var(--coral-tertiary)] text-white border-transparent';
            }

            return (
              <button
                key={f.key}
                type="button"
                onClick={() => setActiveFilter(f.key)}
                data-testid={`tx_filter_${f.key.toLowerCase()}`}
                className={`shrink-0 px-3.5 py-1.5 rounded-full text-xs font-bold border transition cursor-pointer ${
                  selected
                    ? selectedClasses
                    : 'bg-[var(--bg-surface)] border-[var(--hairline)] text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Transactions List */}
      {snapshot.transactions.length === 0 ? (
        <CrispCard className="p-8 text-center space-y-3">
          <div className="w-11 h-11 rounded-2xl bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center mx-auto">
            <ReceiptText className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-[var(--ink-primary)]">
            No purchases recorded yet
          </h3>
          <p className="text-sm text-[var(--ink-muted)] max-w-sm mx-auto">
            Click Reconcile or + to record cash or E-Wallet spend, or paste a payment alert in the
            inbox.
          </p>
          <button
            type="button"
            onClick={onManualEntryClick}
            className="inline-flex items-center justify-center rounded-full border border-[var(--mint-primary)]/50 text-[var(--mint-primary)] font-bold text-sm px-5 py-2 hover:bg-[var(--mint-container)]/35 transition cursor-pointer"
          >
            Record first purchase
          </button>
        </CrispCard>
      ) : groupedTransactions.length === 0 ? (
        <CrispCard className="p-6 text-center space-y-2">
          <div className="font-bold text-base text-[var(--ink-primary)]">No matching purchases</div>
          <p className="text-xs text-[var(--ink-muted)]">
            Try clearing your search or switching back to All sources.
          </p>
          <button
            type="button"
            onClick={() => {
              setSearchQuery('');
              setActiveFilter('ALL');
            }}
            className="text-xs font-bold text-[var(--mint-primary)] hover:underline pt-1 cursor-pointer"
          >
            Reset filters
          </button>
        </CrispCard>
      ) : (
        <div className="space-y-4">
          {groupedTransactions.map((group) => (
            <div key={group.dayKey} className="space-y-2">
              <div className="flex items-center justify-between px-1">
                <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
                  {group.headerLabel.toUpperCase()}
                </span>
                <span className="text-xs font-bold tabular-nums text-[var(--ink-primary)]">
                  {formatCents(group.dayTotalCents, currency)}
                </span>
              </div>
              <CrispCard className="overflow-hidden">
                {group.items.map((tx, idx) => (
                  <LedgerTransactionRow
                    key={tx.id}
                    transaction={tx}
                    currencyCode={currency}
                    onClick={() => onTransactionClick(tx)}
                    onConfirm={onConfirmTransaction}
                    showDivider={idx < group.items.length - 1}
                  />
                ))}
              </CrispCard>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
