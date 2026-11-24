import { useMemo, useState } from 'react';
import { BarChart3, Calendar, PieChart, X } from 'lucide-react';
import {
  CATEGORY_META,
  PAYMENT_SOURCE_META,
  PaymentSource,
  REPORT_PERIOD_LABELS,
  ReportPeriod,
  type PeriodReportData,
} from '../types/ledger';
import { formatCompactCents } from '../lib/currency';
import { CategoryDonutChart, SpendingTrendBarChart } from './Charts';
import {
  CategoryIcon,
  CrispCard,
  PaymentSourceIcon,
  SplitCurrencyText,
} from './IconsAndVisuals';

type ChartStyle = 'DONUT' | 'BAR';
type SectionTab = 'EXPENSES' | 'INSIGHTS';

function toDateInputValue(millis: number): string {
  const d = new Date(millis);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function ReportsView({
  report,
  selectedPeriod,
  currencyCode,
  monthlyBudgetCents,
  onSelectPeriod,
  onSelectCustomRange,
}: {
  report: PeriodReportData;
  selectedPeriod: ReportPeriod;
  currencyCode: string;
  monthlyBudgetCents: number;
  onSelectPeriod: (period: ReportPeriod) => void;
  onSelectCustomRange: (startMillis: number, endMillis: number) => void;
}) {
  const [chartStyle, setChartStyle] = useState<ChartStyle>('DONUT');
  const [activeTab, setActiveTab] = useState<SectionTab>('EXPENSES');
  const [selectedCategoryIdx, setSelectedCategoryIdx] = useState<number | null>(null);
  const [showRangeModal, setShowRangeModal] = useState(false);

  const today = useMemo(() => Date.now(), []);
  const monthStart = useMemo(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).getTime();
  }, []);
  const [startDateStr, setStartDateStr] = useState(toDateInputValue(monthStart));
  const [endDateStr, setEndDateStr] = useState(toDateInputValue(today));

  const nonZeroCategories = useMemo(
    () => report.categoryBreakdown.filter((c) => c.spentCents > 0),
    [report.categoryBreakdown]
  );

  const periods: ReportPeriod[] = [
    ReportPeriod.DAY,
    ReportPeriod.WEEK,
    ReportPeriod.MONTH,
    ReportPeriod.YEAR,
    ReportPeriod.CUSTOM,
  ];

  return (
    <div className="w-full max-w-[760px] mx-auto px-4 pt-4 pb-28 md:pb-12 space-y-4">
      {/* Custom Date Range Modal */}
      {showRangeModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4"
          onClick={() => setShowRangeModal(false)}
        >
          <div
            className="w-full max-w-sm rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">Select Period</h2>
              <button
                type="button"
                onClick={() => setShowRangeModal(false)}
                className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                  Start date
                </label>
                <input
                  type="date"
                  value={startDateStr}
                  onChange={(e) => setStartDateStr(e.target.value)}
                  className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)]"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                  End date
                </label>
                <input
                  type="date"
                  value={endDateStr}
                  onChange={(e) => setEndDateStr(e.target.value)}
                  className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)]"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowRangeModal(false)}
                className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  const s = new Date(`${startDateStr}T00:00:00`).getTime();
                  const e = new Date(`${endDateStr}T23:59:59`).getTime();
                  if (!Number.isNaN(s) && !Number.isNaN(e)) {
                    onSelectCustomRange(s, e);
                  }
                  setShowRangeModal(false);
                }}
                className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold cursor-pointer"
              >
                Apply
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between gap-3 px-1">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[var(--ink-primary)]">
          Report
        </h1>
        <button
          type="button"
          onClick={() => setShowRangeModal(true)}
          className="inline-flex items-center gap-1.5 rounded-full bg-[var(--bg-surface)] border border-[var(--hairline)] px-3.5 py-2 text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/45 transition cursor-pointer max-w-[220px]"
        >
          <Calendar className="w-3.5 h-3.5 text-[var(--mint-primary)] shrink-0" />
          <span className="truncate">{report.periodLabel}</span>
        </button>
      </div>

      {/* Section Switcher */}
      <div className="grid grid-cols-2 gap-1 p-1 rounded-2xl bg-[var(--bg-surface)] border border-[var(--hairline)]">
        {[
          { key: 'EXPENSES' as const, label: 'Expenses' },
          { key: 'INSIGHTS' as const, label: 'Channels & Top' },
        ].map((tab) => {
          const isSelected = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`py-2.5 rounded-xl text-sm font-bold transition cursor-pointer ${
                isSelected
                  ? 'bg-[var(--mint-primary)] text-white dark:text-[#042419]'
                  : 'text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Period Filter Chips */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {periods.map((period) => {
          const selected = selectedPeriod === period;
          return (
            <button
              key={period}
              type="button"
              onClick={() => {
                setSelectedCategoryIdx(null);
                if (period === ReportPeriod.CUSTOM) {
                  setShowRangeModal(true);
                } else {
                  onSelectPeriod(period);
                }
              }}
              data-testid={`filter_${period.toLowerCase()}`}
              className={`shrink-0 px-3.5 py-1.5 rounded-full text-xs font-bold border transition cursor-pointer ${
                selected
                  ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                  : 'bg-[var(--bg-surface)] border-[var(--hairline)] text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
              }`}
            >
              {REPORT_PERIOD_LABELS[period]}
            </button>
          );
        })}
      </div>

      {activeTab === 'EXPENSES' ? (
        <div className="space-y-4">
          <CrispCard accentGlowColor="var(--mint-primary)" className="p-5 rounded-[24px] space-y-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h2 className="text-lg font-bold text-[var(--ink-primary)]">Expenses Report</h2>
                <p className="text-xs text-[var(--ink-muted)]">
                  {report.transactionCount} purchases · Avg{' '}
                  {formatCompactCents(report.averagePerTransactionCents, currencyCode)}
                </p>
              </div>

              <div className="inline-flex items-center gap-1 p-1 rounded-full bg-[var(--bg-surface-low)] border border-[var(--hairline)]">
                <button
                  type="button"
                  onClick={() => setChartStyle('BAR')}
                  aria-label="Bar chart view"
                  className={`w-8 h-8 rounded-full flex items-center justify-center transition cursor-pointer ${
                    chartStyle === 'BAR'
                      ? 'bg-[var(--mint-primary)] text-white dark:text-[#042419]'
                      : 'text-[var(--ink-muted)]'
                  }`}
                >
                  <BarChart3 className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setChartStyle('DONUT')}
                  aria-label="Donut chart view"
                  className={`w-8 h-8 rounded-full flex items-center justify-center transition cursor-pointer ${
                    chartStyle === 'DONUT'
                      ? 'bg-[var(--mint-primary)] text-white dark:text-[#042419]'
                      : 'text-[var(--ink-muted)]'
                  }`}
                >
                  <PieChart className="w-4 h-4" />
                </button>
              </div>
            </div>

            {report.totalSpentCents === 0 ? (
              <div className="py-8 text-center text-sm text-[var(--ink-muted)]">
                No spending recorded in this period.
              </div>
            ) : chartStyle === 'DONUT' ? (
              <CategoryDonutChart
                items={nonZeroCategories}
                totalSpentCents={report.totalSpentCents}
                currencyCode={currencyCode}
                budgetCapCents={
                  selectedPeriod === ReportPeriod.MONTH && monthlyBudgetCents > 0
                    ? monthlyBudgetCents
                    : null
                }
                selectedCategoryIndex={selectedCategoryIdx}
                onSelectCategoryIndex={setSelectedCategoryIdx}
              />
            ) : (
              <SpendingTrendBarChart buckets={report.trendBuckets} currencyCode={currencyCode} />
            )}
          </CrispCard>

          <div className="flex items-center justify-between px-1">
            <span className="font-mono-data text-[10.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)]">
              ALL CATEGORIES
            </span>
            <SplitCurrencyText
              amountCents={report.totalSpentCents}
              currencyCode={currencyCode}
              className="text-sm text-[var(--ink-primary)]"
            />
          </div>

          {report.categoryBreakdown.length === 0 ? (
            <CrispCard className="p-6 text-center text-sm text-[var(--ink-muted)]">
              No spending recorded in this period.
            </CrispCard>
          ) : (
            <CrispCard className="overflow-hidden">
              {report.categoryBreakdown.map((item, idx) => {
                const meta = CATEGORY_META[item.category];
                const nonZeroIdx = nonZeroCategories.findIndex((c) => c.category === item.category);
                const isHighlighted =
                  selectedCategoryIdx !== null && nonZeroIdx === selectedCategoryIdx;
                const showBudgetRatio = selectedPeriod === ReportPeriod.MONTH;
                const ratio =
                  showBudgetRatio && item.budgetCents > 0
                    ? Math.max(0, Math.min(1, item.spentCents / item.budgetCents))
                    : Math.max(0, Math.min(1, item.shareFraction));
                const isOver =
                  showBudgetRatio && item.budgetCents > 0 && item.spentCents > item.budgetCents;
                const sharePct = Math.round(item.shareFraction * 100);
                const budgetPct =
                  item.budgetCents > 0 ? Math.round((item.spentCents / item.budgetCents) * 100) : 0;

                return (
                  <div
                    key={item.category}
                    style={{
                      backgroundColor: isHighlighted
                        ? `color-mix(in srgb, ${meta.chartColor} 10%, transparent)`
                        : undefined,
                    }}
                  >
                    <button
                      type="button"
                      onClick={() => {
                        if (nonZeroIdx >= 0) {
                          setSelectedCategoryIdx(
                            selectedCategoryIdx === nonZeroIdx ? null : nonZeroIdx
                          );
                        }
                      }}
                      className="w-full text-left px-4 py-3.5 space-y-2 hover:bg-[var(--bg-surface-low)]/60 transition cursor-pointer"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className="w-9 h-9 rounded-[10px] bg-[var(--bg-surface-low)] border border-[var(--hairline)] flex items-center justify-center shrink-0"
                          style={{ color: meta.chartColor }}
                        >
                          <CategoryIcon category={item.category} className="w-[18px] h-[18px]" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="font-semibold text-sm text-[var(--ink-primary)] truncate">
                            {meta.displayName}
                          </div>
                          <div className="font-mono-data text-[11px] text-[var(--ink-muted)]">
                            {sharePct}% of total
                          </div>
                        </div>
                        <div className="text-right shrink-0">
                          <SplitCurrencyText
                            amountCents={item.spentCents}
                            currencyCode={currencyCode}
                            className={`text-sm ${
                              isOver ? 'text-[var(--coral-tertiary)]' : 'text-[var(--ink-primary)]'
                            }`}
                          />
                          {showBudgetRatio && item.budgetCents > 0 && (
                            <div
                              className={`font-mono-data text-[10.5px] font-semibold ${
                                isOver
                                  ? 'text-[var(--coral-tertiary)]'
                                  : 'text-[var(--mint-primary)]'
                              }`}
                            >
                              {budgetPct}% of limit
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="w-full h-1.5 rounded-full bg-[var(--bg-track)] overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-300"
                          style={{
                            width: `${ratio * 100}%`,
                            backgroundColor: isOver ? 'var(--coral-tertiary)' : meta.chartColor,
                          }}
                        />
                      </div>
                    </button>

                    {idx < report.categoryBreakdown.length - 1 && (
                      <div className="h-[1px] bg-[var(--hairline)] mx-4" />
                    )}
                  </div>
                );
              })}
            </CrispCard>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          <CrispCard className="p-5 rounded-[24px] space-y-3">
            <h2 className="text-lg font-bold text-[var(--ink-primary)]">Spending Trend</h2>
            {report.totalSpentCents === 0 ? (
              <div className="py-6 text-center text-sm text-[var(--ink-muted)]">
                No spending recorded in this period.
              </div>
            ) : (
              <SpendingTrendBarChart buckets={report.trendBuckets} currencyCode={currencyCode} />
            )}
          </CrispCard>

          {report.paymentSources.length > 0 && (
            <CrispCard className="p-5 rounded-[24px] space-y-3.5">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">By Payment Channel</h2>
              <div className="space-y-3">
                {report.paymentSources.map((srcItem) => {
                  const isEWallet = srcItem.source === PaymentSource.E_WALLET;
                  return (
                    <div key={srcItem.source} className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                            isEWallet
                              ? 'bg-[var(--sky-container)] text-[var(--sky-secondary)]'
                              : 'bg-[var(--mint-container)] text-[var(--mint-primary)]'
                          }`}
                        >
                          <PaymentSourceIcon source={srcItem.source} className="w-[18px] h-[18px]" />
                        </div>
                        <div>
                          <div className="font-semibold text-sm text-[var(--ink-primary)]">
                            {PAYMENT_SOURCE_META[srcItem.source].displayName}
                          </div>
                          <div className="text-xs text-[var(--ink-muted)]">
                            {Math.round(srcItem.shareFraction * 100)}% of spend
                          </div>
                        </div>
                      </div>
                      <SplitCurrencyText
                        amountCents={srcItem.totalCents}
                        currencyCode={currencyCode}
                        className="text-sm text-[var(--ink-primary)]"
                      />
                    </div>
                  );
                })}
              </div>
            </CrispCard>
          )}

          {report.topMerchants.length > 0 && (
            <CrispCard className="p-5 rounded-[24px] space-y-3.5">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">Top Merchants</h2>
              <div className="space-y-3">
                {report.topMerchants.map((m) => {
                  const meta = CATEGORY_META[m.primaryCategory];
                  return (
                    <div key={m.merchant} className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div
                          className="w-9 h-9 rounded-xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] flex items-center justify-center shrink-0"
                          style={{ color: meta.chartColor }}
                        >
                          <CategoryIcon category={m.primaryCategory} className="w-[18px] h-[18px]" />
                        </div>
                        <div className="min-w-0">
                          <div className="font-semibold text-sm text-[var(--ink-primary)] truncate">
                            {m.merchant}
                          </div>
                          <div className="text-xs text-[var(--ink-muted)]">
                            {meta.shortLabel} · {m.count}x
                          </div>
                        </div>
                      </div>
                      <SplitCurrencyText
                        amountCents={m.totalCents}
                        currencyCode={currencyCode}
                        className="text-sm text-[var(--coral-tertiary)] shrink-0"
                      />
                    </div>
                  );
                })}
              </div>
            </CrispCard>
          )}
        </div>
      )}
    </div>
  );
}
