import { useMemo, useState } from 'react';
import {
  CheckCircle2,
  Info,
  Pencil,
  PiggyBank,
  Plus,
  SlidersHorizontal,
  Trash2,
  X,
} from 'lucide-react';
import {
  ALL_CATEGORIES,
  CATEGORY_META,
  LIMITS,
  SpendingCategory,
  type DashboardSnapshot,
  type RecurringPreset,
} from '../types/ledger';
import { formatCents, formatCompactCents } from '../lib/currency';
import {
  CircularPercentRing,
  HatchedGoalProgressBar,
  SavingsProjectionChart,
} from './Charts';
import { CategoryIcon, CrispCard, SplitCurrencyText } from './IconsAndVisuals';

export function PlanView({
  snapshot,
  onSaveCategoryBudget,
  onQuickAddPreset,
  onDeletePreset,
  onAddPresetClicked,
  onOpenSettings,
}: {
  snapshot: DashboardSnapshot;
  onSaveCategoryBudget: (category: SpendingCategory, limitCents: number) => Promise<void>;
  onQuickAddPreset: (preset: RecurringPreset) => void;
  onDeletePreset: (id: string) => void;
  onAddPresetClicked: () => void;
  onOpenSettings: () => void;
}) {
  const [editingCat, setEditingCat] = useState<SpendingCategory | null>(null);
  const [limitText, setLimitText] = useState('');

  const currency = snapshot.settings.currencyCode;
  const proj = snapshot.savingsProjection;

  const budgetMap = useMemo(() => {
    const map = new Map<SpendingCategory, number>();
    snapshot.budgets.forEach((b) => map.set(b.category, b.monthlyLimitCents));
    return map;
  }, [snapshot.budgets]);

  const spentByCat = useMemo(() => {
    const map = new Map<SpendingCategory, number>();
    snapshot.monthTransactions.forEach((tx) => {
      map.set(tx.category, (map.get(tx.category) ?? 0) + tx.amountCents);
    });
    return map;
  }, [snapshot.monthTransactions]);

  const sortedCategories = useMemo(() => {
    return [...ALL_CATEGORIES].sort((a, b) => {
      const limA = Math.max(1, budgetMap.get(a) ?? CATEGORY_META[a].defaultBudgetCents);
      const limB = Math.max(1, budgetMap.get(b) ?? CATEGORY_META[b].defaultBudgetCents);
      const utilA = (spentByCat.get(a) ?? 0) / limA;
      const utilB = (spentByCat.get(b) ?? 0) / limB;
      return utilB - utilA;
    });
  }, [budgetMap, spentByCat]);

  const budgetTarget = Math.max(1, proj.monthlyBudgetCents);
  const spendFrac = Math.max(0, Math.min(1, proj.spentToDateCents / budgetTarget));
  const remainingCents = proj.monthlyBudgetCents - proj.spentToDateCents;
  const isOnTrack = proj.projectedMonthEndSpendCents <= proj.monthlyBudgetCents;
  const barColor = isOnTrack ? 'var(--mint-primary)' : 'var(--coral-tertiary)';

  const openEditBudget = (cat: SpendingCategory) => {
    const existingLimit = budgetMap.get(cat) ?? CATEGORY_META[cat].defaultBudgetCents;
    setLimitText(Math.round(existingLimit / 100).toString());
    setEditingCat(cat);
  };

  return (
    <div className="w-full max-w-[760px] mx-auto px-4 pt-4 pb-28 md:pb-12 space-y-5">
      {/* Edit Category Budget Modal */}
      {editingCat && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4"
          onClick={() => setEditingCat(null)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const cents = Math.max(
                0,
                Math.min(
                  LIMITS.AMOUNT_MAX_CENTS,
                  Math.round((Number.parseFloat(limitText) || 0) * 100)
                )
              );
              await onSaveCategoryBudget(editingCat, cents);
              setEditingCat(null);
            }}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">
                {CATEGORY_META[editingCat].displayName} Budget
              </h2>
              <button
                type="button"
                onClick={() => setEditingCat(null)}
                className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                Monthly limit ({currency})
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={limitText}
                onChange={(e) => {
                  const cleaned = e.target.value.replace(/[^0-9.]/g, '');
                  if ((cleaned.match(/\./g) ?? []).length <= 1) setLimitText(cleaned);
                }}
                className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-base font-bold tabular-nums text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
              />
            </div>

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingCat(null)}
                className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold cursor-pointer"
              >
                Save
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between gap-3 px-1">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[var(--ink-primary)]">
            My Plan
          </h1>
          <p className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)]">
            SAVINGS PACE &amp; CATEGORY LIMITS
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onOpenSettings}
            aria-label="Edit monthly income and budget"
            className="w-11 h-11 rounded-[14px] bg-[var(--bg-surface)] border border-[var(--hairline)] flex items-center justify-center text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/45 transition cursor-pointer"
          >
            <SlidersHorizontal className="w-[18px] h-[18px]" />
          </button>
          <button
            type="button"
            onClick={onAddPresetClicked}
            aria-label="Add recurring shortcut"
            className="w-11 h-11 rounded-[14px] bg-[var(--mint-primary)] text-white dark:text-[#042419] flex items-center justify-center hover:opacity-95 transition cursor-pointer"
          >
            <Plus className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Monthly Spending Guardrail Hero */}
      <CrispCard accentGlowColor={barColor} className="p-5 rounded-[24px] space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div
              className={`w-11 h-11 rounded-[14px] flex items-center justify-center shrink-0 ${
                isOnTrack
                  ? 'bg-[var(--mint-container)] text-[var(--mint-primary)]'
                  : 'bg-[var(--coral-container)] text-[var(--coral-tertiary)]'
              }`}
            >
              <PiggyBank className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <h2 className="font-bold text-base text-[var(--ink-primary)] truncate">
                Monthly Spending Guardrail
              </h2>
              <p className="text-xs text-[var(--ink-muted)]">
                Day {proj.currentDayOfMonth} of {proj.daysInMonth} · Income{' '}
                {formatCompactCents(proj.monthlyIncomeCents, currency)}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenSettings}
            aria-label="Edit income or budget"
            className="w-9 h-9 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer shrink-0"
          >
            <Pencil className="w-4 h-4" />
          </button>
        </div>

        <div className="flex items-baseline gap-2 flex-wrap">
          <SplitCurrencyText
            amountCents={proj.spentToDateCents}
            currencyCode={currency}
            className="text-3xl text-[var(--ink-primary)]"
          />
          <span className="text-sm text-[var(--ink-muted)]">
            of {formatCents(proj.monthlyBudgetCents, currency)}
          </span>
        </div>

        <HatchedGoalProgressBar fraction={spendFrac} fillColor={barColor} />

        <div className="flex items-center justify-between text-xs">
          <span className="text-[var(--ink-muted)]">
            {Math.round(spendFrac * 100)}% of budget used
          </span>
          <span
            className={`font-bold tabular-nums ${
              remainingCents >= 0 ? 'text-[var(--ink-primary)]' : 'text-[var(--coral-tertiary)]'
            }`}
          >
            {remainingCents >= 0
              ? `${formatCents(remainingCents, currency)} left`
              : `${formatCents(-remainingCents, currency)} over`}
          </span>
        </div>

        <div
          className={`rounded-[14px] px-3.5 py-2.5 flex items-center gap-2 text-xs font-bold text-white ${
            isOnTrack ? 'bg-[#059669]' : 'bg-[#E11D48]'
          }`}
        >
          {isOnTrack ? (
            <CheckCircle2 className="w-4 h-4 shrink-0" />
          ) : (
            <Info className="w-4 h-4 shrink-0" />
          )}
          <span>
            {proj.projectedSavingsCents >= 0
              ? isOnTrack
                ? `On track for ${formatCents(
                    proj.projectedSavingsCents,
                    currency,
                    true
                  )} savings this month.`
                : `Spend pace exceeds budget, still +${formatCents(
                    proj.projectedSavingsCents,
                    currency
                  )} vs income.`
              : `Projected ${formatCents(
                  -proj.projectedSavingsCents,
                  currency
                )} above monthly income pace.`}
          </span>
        </div>

        <SavingsProjectionChart projection={proj} currencyCode={currency} />
      </CrispCard>

      {/* Category Budgets */}
      <div className="space-y-2.5">
        <div className="flex items-center justify-between px-1">
          <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
            CATEGORY BUDGETS
          </span>
          <span className="text-xs text-[var(--ink-muted)]">Click row to edit limit</span>
        </div>

        <CrispCard className="overflow-hidden">
          {sortedCategories.map((cat, idx) => {
            const meta = CATEGORY_META[cat];
            const limit = budgetMap.get(cat) ?? meta.defaultBudgetCents;
            const spent = spentByCat.get(cat) ?? 0;
            const pct = limit > 0 ? Math.round((spent / limit) * 100) : 0;
            const ringColor = pct > 100 ? 'var(--coral-tertiary)' : meta.chartColor;

            return (
              <div key={cat}>
                <button
                  type="button"
                  onClick={() => openEditBudget(cat)}
                  data-testid={`plan_budget_${cat.toLowerCase()}`}
                  className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-[var(--bg-surface-low)]/70 transition cursor-pointer"
                >
                  <div
                    className="w-10 h-10 rounded-[10px] bg-[var(--bg-surface-low)] border border-[var(--hairline)] flex items-center justify-center shrink-0"
                    style={{ color: meta.chartColor }}
                  >
                    <CategoryIcon category={cat} className="w-[19px] h-[19px]" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-semibold text-sm text-[var(--ink-primary)] truncate">
                      {meta.displayName}
                    </div>
                    <div className="text-xs text-[var(--ink-muted)] tabular-nums">
                      {formatCents(spent, currency)} of {formatCents(limit, currency)}
                    </div>
                  </div>
                  <CircularPercentRing percentage={pct} ringColor={ringColor} />
                </button>
                {idx < sortedCategories.length - 1 && (
                  <div className="h-[1px] bg-[var(--hairline)] ml-[68px] mr-4" />
                )}
              </div>
            );
          })}
        </CrispCard>
      </div>

      {/* Quick-add shortcuts */}
      <div className="space-y-2.5">
        <div className="flex items-center justify-between px-1">
          <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
            QUICK-ADD SHORTCUTS
          </span>
          <button
            type="button"
            onClick={onAddPresetClicked}
            className="text-xs font-bold text-[var(--mint-primary)] hover:underline cursor-pointer"
          >
            Add shortcut
          </button>
        </div>

        {snapshot.recurringPresets.length === 0 ? (
          <CrispCard className="p-4 flex items-center justify-between gap-3">
            <p className="text-sm text-[var(--ink-muted)]">
              Save daily coffee or commute presets for 1-tap logging.
            </p>
            <button
              type="button"
              onClick={onAddPresetClicked}
              className="shrink-0 rounded-full border border-[var(--mint-primary)]/50 text-[var(--mint-primary)] text-xs font-bold px-4 py-2 cursor-pointer"
            >
              Create
            </button>
          </CrispCard>
        ) : (
          <CrispCard className="overflow-hidden">
            {snapshot.recurringPresets.map((preset, idx) => {
              const meta = CATEGORY_META[preset.category];
              return (
                <div key={preset.id}>
                  <div className="flex items-center gap-3 px-4 py-3 hover:bg-[var(--bg-surface-low)]/70 transition">
                    <button
                      type="button"
                      onClick={() => onQuickAddPreset(preset)}
                      className="flex flex-1 min-w-0 items-center gap-3 text-left cursor-pointer"
                    >
                      <div
                        className="w-9 h-9 rounded-[10px] bg-[var(--bg-surface-low)] border border-[var(--hairline)] flex items-center justify-center shrink-0"
                        style={{ color: meta.chartColor }}
                      >
                        <CategoryIcon category={preset.category} className="w-[18px] h-[18px]" />
                      </div>
                      <div className="min-w-0">
                        <div className="font-semibold text-sm text-[var(--ink-primary)] truncate">
                          {preset.label}
                        </div>
                        <div className="text-xs text-[var(--ink-muted)] tabular-nums truncate">
                          {preset.merchant} · {formatCents(preset.amountCents, currency)}
                        </div>
                      </div>
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeletePreset(preset.id)}
                      aria-label="Delete preset"
                      className="w-8 h-8 rounded-full hover:bg-[var(--coral-container)]/50 text-[var(--ink-muted)] hover:text-[var(--coral-tertiary)] flex items-center justify-center cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                  {idx < snapshot.recurringPresets.length - 1 && (
                    <div className="h-[1px] bg-[var(--hairline)] ml-[64px] mr-4" />
                  )}
                </div>
              );
            })}
          </CrispCard>
        )}
      </div>
    </div>
  );
}
