import {
  ALL_CATEGORIES,
  CATEGORY_META,
  PaymentSource,
  ReportPeriod,
  SpendingCategory,
  type CategoryBreakdownItem,
  type CategoryBudget,
  type MerchantTotalItem,
  type PaymentSourceBreakdown,
  type PeriodReportData,
  type SavingsProjection,
  type SpendingTransaction,
  type TrendBucket,
} from '../types/ledger';

export interface DayTransactionGroup {
  dayKey: number;
  headerLabel: string;
  dayTotalCents: number;
  items: SpendingTransaction[];
}

export function formatDateTime(millis: number): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(millis));
}

export function formatShortDate(millis: number): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
  }).format(new Date(millis));
}

export function currentMonthBounds(nowMillis = Date.now()): [number, number] {
  const d = new Date(nowMillis);
  const start = new Date(d.getFullYear(), d.getMonth(), 1, 0, 0, 0, 0).getTime();
  const nextMonthStart = new Date(d.getFullYear(), d.getMonth() + 1, 1, 0, 0, 0, 0).getTime();
  return [start, nextMonthStart - 1];
}

function normalizeDayStart(millis: number): number {
  const d = new Date(millis);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0, 0).getTime();
}

function normalizeDayEnd(millis: number): number {
  const d = new Date(millis);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59, 999).getTime();
}

export function computePeriodBounds(
  period: ReportPeriod,
  customStartMillis: number | null,
  customEndMillis: number | null,
  nowMillis = Date.now()
): [number, number] {
  const d = new Date(nowMillis);
  switch (period) {
    case ReportPeriod.DAY: {
      const start = new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0, 0).getTime();
      const end = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, 0, 0, 0, 0).getTime() - 1;
      return [start, end];
    }
    case ReportPeriod.WEEK: {
      const dayOfWeek = d.getDay();
      const startDate = new Date(d.getFullYear(), d.getMonth(), d.getDate() - dayOfWeek, 0, 0, 0, 0);
      const endDate = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate() + 7, 0, 0, 0, 0);
      return [startDate.getTime(), endDate.getTime() - 1];
    }
    case ReportPeriod.MONTH:
      return currentMonthBounds(nowMillis);
    case ReportPeriod.YEAR: {
      const start = new Date(d.getFullYear(), 0, 1, 0, 0, 0, 0).getTime();
      const end = new Date(d.getFullYear() + 1, 0, 1, 0, 0, 0, 0).getTime() - 1;
      return [start, end];
    }
    case ReportPeriod.CUSTOM: {
      const fallback = currentMonthBounds(nowMillis);
      const s = normalizeDayStart(customStartMillis ?? fallback[0]);
      const e = normalizeDayEnd(customEndMillis ?? nowMillis);
      return s <= e ? [s, e] : [e, s];
    }
  }
}

export function computeSavingsProjection(
  transactions: SpendingTransaction[],
  monthlyIncomeCents: number,
  monthlyBudgetCents: number,
  nowMillis = Date.now()
): SavingsProjection {
  const now = new Date(nowMillis);
  const daysInMonth = Math.max(
    28,
    new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  );
  const currentDay = Math.min(daysInMonth, Math.max(1, now.getDate()));
  const [monthStart, monthEnd] = currentMonthBounds(nowMillis);

  const monthTx = transactions.filter(
    (tx) => tx.occurredAtMillis >= monthStart && tx.occurredAtMillis <= monthEnd
  );
  const dailySpend = new Array<number>(daysInMonth).fill(0);
  for (const tx of monthTx) {
    const dayIdx = Math.min(
      daysInMonth - 1,
      Math.max(0, new Date(tx.occurredAtMillis).getDate() - 1)
    );
    dailySpend[dayIdx] += tx.amountCents;
  }

  const actualCumulative: number[] = [];
  let running = 0;
  for (let d = 0; d < currentDay; d += 1) {
    running += dailySpend[d];
    actualCumulative.push(running);
  }

  const spentToDate = running;
  const avgDailyBurn = currentDay > 0 ? spentToDate / currentDay : 0;
  const projectedEnd = Math.round(spentToDate + avgDailyBurn * (daysInMonth - currentDay));

  const projectedCurve: number[] = [];
  for (let d = 1; d <= daysInMonth; d += 1) {
    if (d <= currentDay) {
      projectedCurve.push(actualCumulative[d - 1]);
    } else {
      projectedCurve.push(Math.round(spentToDate + avgDailyBurn * (d - currentDay)));
    }
  }

  const savingsBase = monthlyIncomeCents > 0 ? monthlyIncomeCents : monthlyBudgetCents;
  const projectedSavings = savingsBase - projectedEnd;
  const remainingDays = Math.max(1, daysInMonth - currentDay + 1);
  const remainingBudget = monthlyBudgetCents - spentToDate;
  const dailyAllowance = remainingBudget > 0 ? Math.floor(remainingBudget / remainingDays) : 0;
  const noSpendDays = dailySpend.slice(0, currentDay).filter((v) => v === 0).length;

  return {
    daysInMonth,
    currentDayOfMonth: currentDay,
    spentToDateCents: spentToDate,
    projectedMonthEndSpendCents: projectedEnd,
    monthlyIncomeCents,
    monthlyBudgetCents,
    projectedSavingsCents: projectedSavings,
    actualCumulativeDailyCents: actualCumulative,
    projectedDailyCents: projectedCurve,
    remainingDaysInMonth: remainingDays,
    dailyAllowanceCents: dailyAllowance,
    noSpendDaysThisMonth: noSpendDays,
  };
}

function buildTrendBuckets(
  filtered: SpendingTransaction[],
  period: ReportPeriod,
  startMillis: number,
  endMillis: number
): TrendBucket[] {
  switch (period) {
    case ReportPeriod.DAY: {
      const labels = ['12a', '4a', '8a', '12p', '4p', '8p'];
      const sums = new Array<number>(6).fill(0);
      for (const tx of filtered) {
        const idx = Math.min(5, Math.max(0, Math.floor(new Date(tx.occurredAtMillis).getHours() / 4)));
        sums[idx] += tx.amountCents;
      }
      return labels.map((label, i) => ({ label, amountCents: sums[i] }));
    }
    case ReportPeriod.WEEK: {
      const buckets: TrendBucket[] = [];
      const cursor = new Date(startMillis);
      for (let i = 0; i < 7; i += 1) {
        const dayStart = cursor.getTime();
        cursor.setDate(cursor.getDate() + 1);
        const dayEnd = cursor.getTime() - 1;
        const amt = filtered
          .filter((tx) => tx.occurredAtMillis >= dayStart && tx.occurredAtMillis <= dayEnd)
          .reduce((acc, tx) => acc + tx.amountCents, 0);
        const dayLabel = new Intl.DateTimeFormat('en-US', { weekday: 'short' })
          .format(new Date(dayStart))
          .slice(0, 2);
        buckets.push({ label: dayLabel, amountCents: amt });
      }
      return buckets;
    }
    case ReportPeriod.MONTH: {
      const labels = ['W1', 'W2', 'W3', 'W4', 'W5'];
      const sums = new Array<number>(5).fill(0);
      for (const tx of filtered) {
        const idx = Math.min(4, Math.max(0, Math.floor((new Date(tx.occurredAtMillis).getDate() - 1) / 7)));
        sums[idx] += tx.amountCents;
      }
      return labels.map((label, i) => ({ label, amountCents: sums[i] }));
    }
    case ReportPeriod.YEAR: {
      const labels = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'];
      const sums = new Array<number>(12).fill(0);
      for (const tx of filtered) {
        const idx = Math.min(11, Math.max(0, new Date(tx.occurredAtMillis).getMonth()));
        sums[idx] += tx.amountCents;
      }
      return labels.map((label, i) => ({ label, amountCents: sums[i] }));
    }
    case ReportPeriod.CUSTOM: {
      const spanMillis = Math.max(1, endMillis - startMillis);
      const bucketCount = 6;
      const step = Math.floor(spanMillis / bucketCount);
      const buckets: TrendBucket[] = [];
      for (let i = 0; i < bucketCount; i += 1) {
        const bStart = startMillis + step * i;
        const bEnd = i === bucketCount - 1 ? endMillis : startMillis + step * (i + 1) - 1;
        const amt = filtered
          .filter((tx) => tx.occurredAtMillis >= bStart && tx.occurredAtMillis <= bEnd)
          .reduce((acc, tx) => acc + tx.amountCents, 0);
        const d = new Date(bStart);
        buckets.push({
          label: `${d.getMonth() + 1}/${d.getDate()}`,
          amountCents: amt,
        });
      }
      return buckets;
    }
  }
}

export function computePeriodReport(
  allTransactions: SpendingTransaction[],
  budgets: CategoryBudget[],
  period: ReportPeriod,
  customStartMillis: number | null,
  customEndMillis: number | null,
  nowMillis = Date.now()
): PeriodReportData {
  const [startMillis, endMillis] = computePeriodBounds(
    period,
    customStartMillis,
    customEndMillis,
    nowMillis
  );
  const filtered = allTransactions
    .filter((tx) => tx.occurredAtMillis >= startMillis && tx.occurredAtMillis <= endMillis)
    .sort((a, b) => b.occurredAtMillis - a.occurredAtMillis);

  const totalSpent = filtered.reduce((acc, tx) => acc + tx.amountCents, 0);
  const count = filtered.length;
  const avgPerTx = count > 0 ? Math.floor(totalSpent / count) : 0;

  const trendBuckets = buildTrendBuckets(filtered, period, startMillis, endMillis);

  const budgetMap = new Map<SpendingCategory, CategoryBudget>();
  for (const b of budgets) {
    budgetMap.set(b.category, b);
  }

  const byCategory = new Map<SpendingCategory, SpendingTransaction[]>();
  for (const tx of filtered) {
    const arr = byCategory.get(tx.category) ?? [];
    arr.push(tx);
    byCategory.set(tx.category, arr);
  }

  const categoryBreakdown: CategoryBreakdownItem[] = ALL_CATEGORIES.map((cat) => {
    const list = byCategory.get(cat) ?? [];
    const spent = list.reduce((acc, tx) => acc + tx.amountCents, 0);
    const limit = budgetMap.get(cat)?.monthlyLimitCents ?? CATEGORY_META[cat].defaultBudgetCents;
    const share = totalSpent > 0 ? spent / totalSpent : 0;
    return {
      category: cat,
      spentCents: spent,
      budgetCents: limit,
      shareFraction: share,
      transactionCount: list.length,
    };
  })
    .filter((item) => item.spentCents > 0 || period === ReportPeriod.MONTH)
    .sort((a, b) => b.spentCents - a.spentCents);

  const byMerchant = new Map<string, SpendingTransaction[]>();
  for (const tx of filtered) {
    const key = tx.merchant.trim() || 'Unknown Store';
    const arr = byMerchant.get(key) ?? [];
    arr.push(tx);
    byMerchant.set(key, arr);
  }

  const topMerchants: MerchantTotalItem[] = Array.from(byMerchant.entries())
    .map(([merchant, list]) => {
      const catCounts = new Map<SpendingCategory, number>();
      for (const item of list) {
        catCounts.set(item.category, (catCounts.get(item.category) ?? 0) + 1);
      }
      let dominantCat = SpendingCategory.OTHER;
      let maxCount = -1;
      for (const [cat, c] of catCounts.entries()) {
        if (c > maxCount) {
          maxCount = c;
          dominantCat = cat;
        }
      }
      return {
        merchant,
        totalCents: list.reduce((acc, tx) => acc + tx.amountCents, 0),
        count: list.length,
        primaryCategory: dominantCat,
      };
    })
    .sort((a, b) => b.totalCents - a.totalCents)
    .slice(0, 5);

  const bySource = new Map<PaymentSource, number>();
  for (const tx of filtered) {
    bySource.set(tx.paymentSource, (bySource.get(tx.paymentSource) ?? 0) + tx.amountCents);
  }
  const paymentSources: PaymentSourceBreakdown[] = Array.from(bySource.entries())
    .map(([source, amt]) => ({
      source,
      totalCents: amt,
      shareFraction: totalSpent > 0 ? amt / totalSpent : 0,
    }))
    .sort((a, b) => b.totalCents - a.totalCents);

  let periodLabel = '';
  switch (period) {
    case ReportPeriod.DAY:
      periodLabel = new Intl.DateTimeFormat('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
      }).format(new Date(startMillis));
      break;
    case ReportPeriod.WEEK:
      periodLabel = `This Week (${formatShortDate(startMillis)} – ${formatShortDate(endMillis)})`;
      break;
    case ReportPeriod.MONTH:
      periodLabel = new Intl.DateTimeFormat('en-US', {
        month: 'long',
        year: 'numeric',
      }).format(new Date(startMillis));
      break;
    case ReportPeriod.YEAR:
      periodLabel = String(new Date(startMillis).getFullYear());
      break;
    case ReportPeriod.CUSTOM:
      periodLabel = `${formatShortDate(startMillis)} – ${formatShortDate(endMillis)}`;
      break;
  }

  return {
    periodLabel,
    totalSpentCents: totalSpent,
    transactionCount: count,
    averagePerTransactionCents: avgPerTx,
    trendBuckets,
    categoryBreakdown,
    topMerchants,
    paymentSources,
  };
}

export function groupTransactionsByDay(transactions: SpendingTransaction[]): DayTransactionGroup[] {
  if (transactions.length === 0) return [];
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const yestDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
  const yestStart = yestDate.getTime();

  const fullDateFmt = new Intl.DateTimeFormat('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });

  const groups = new Map<number, SpendingTransaction[]>();
  for (const tx of transactions) {
    const d = new Date(tx.occurredAtMillis);
    const dayStart = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const list = groups.get(dayStart) ?? [];
    list.push(tx);
    groups.set(dayStart, list);
  }

  return Array.from(groups.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([dayStart, list]) => {
      let headerLabel: string;
      if (dayStart === todayStart) {
        headerLabel = 'Today';
      } else if (dayStart === yestStart) {
        headerLabel = 'Yesterday';
      } else {
        headerLabel = fullDateFmt.format(new Date(dayStart));
      }
      return {
        dayKey: dayStart,
        headerLabel,
        dayTotalCents: list.reduce((acc, item) => acc + item.amountCents, 0),
        items: [...list].sort((a, b) => b.occurredAtMillis - a.occurredAtMillis),
      };
    });
}
