export enum SpendingCategory {
  FOOD = 'FOOD',
  GROCERIES = 'GROCERIES',
  TRANSPORT_FUEL = 'TRANSPORT_FUEL',
  CONVENIENCE = 'CONVENIENCE',
  GADGETS = 'GADGETS',
  UTILITIES = 'UTILITIES',
  SHOPPING = 'SHOPPING',
  OTHER = 'OTHER',
}

export interface CategoryMeta {
  displayName: string;
  shortLabel: string;
  chartColor: string;
  defaultBudgetCents: number;
}

export const CATEGORY_META: Record<SpendingCategory, CategoryMeta> = {
  [SpendingCategory.FOOD]: {
    displayName: 'Food & Dining',
    shortLabel: 'Food',
    chartColor: '#FB923C',
    defaultBudgetCents: 45_000,
  },
  [SpendingCategory.GROCERIES]: {
    displayName: 'Groceries',
    shortLabel: 'Groceries',
    chartColor: '#38BDF8',
    defaultBudgetCents: 50_000,
  },
  [SpendingCategory.TRANSPORT_FUEL]: {
    displayName: 'Fuel, Transpo & Tolls',
    shortLabel: 'Transpo',
    chartColor: '#34D399',
    defaultBudgetCents: 30_000,
  },
  [SpendingCategory.CONVENIENCE]: {
    displayName: 'Convenience Stores',
    shortLabel: 'Convenience',
    chartColor: '#2DD4BF',
    defaultBudgetCents: 15_000,
  },
  [SpendingCategory.GADGETS]: {
    displayName: 'Gadgets & Tech',
    shortLabel: 'Gadgets',
    chartColor: '#818CF8',
    defaultBudgetCents: 25_000,
  },
  [SpendingCategory.UTILITIES]: {
    displayName: 'Bills & Utilities',
    shortLabel: 'Bills',
    chartColor: '#FBBF24',
    defaultBudgetCents: 35_000,
  },
  [SpendingCategory.SHOPPING]: {
    displayName: 'Shopping',
    shortLabel: 'Shopping',
    chartColor: '#F472B6',
    defaultBudgetCents: 25_000,
  },
  [SpendingCategory.OTHER]: {
    displayName: 'Other',
    shortLabel: 'Other',
    chartColor: '#94A3B8',
    defaultBudgetCents: 15_000,
  },
};

export const ALL_CATEGORIES: SpendingCategory[] = [
  SpendingCategory.FOOD,
  SpendingCategory.GROCERIES,
  SpendingCategory.TRANSPORT_FUEL,
  SpendingCategory.CONVENIENCE,
  SpendingCategory.GADGETS,
  SpendingCategory.UTILITIES,
  SpendingCategory.SHOPPING,
  SpendingCategory.OTHER,
];

export function parseSpendingCategory(raw?: string | null): SpendingCategory {
  if (!raw) return SpendingCategory.OTHER;
  const upper = raw.trim().toUpperCase();
  return ALL_CATEGORIES.find((c) => c === upper) ?? SpendingCategory.OTHER;
}

export enum PaymentSource {
  GOOGLE_PAY = 'GOOGLE_PAY',
  E_WALLET = 'E_WALLET',
  CREDIT_CARD = 'CREDIT_CARD',
  SMS_ALERT = 'SMS_ALERT',
  CASH = 'CASH',
  MANUAL = 'MANUAL',
}

export interface PaymentSourceMeta {
  displayName: string;
  shortLabel: string;
}

export const PAYMENT_SOURCE_META: Record<PaymentSource, PaymentSourceMeta> = {
  [PaymentSource.GOOGLE_PAY]: { displayName: 'Google Pay', shortLabel: 'GPay' },
  [PaymentSource.E_WALLET]: { displayName: 'E-Wallet', shortLabel: 'E-Wallet' },
  [PaymentSource.CREDIT_CARD]: { displayName: 'Credit Card', shortLabel: 'Card' },
  [PaymentSource.SMS_ALERT]: { displayName: 'SMS Alert', shortLabel: 'SMS' },
  [PaymentSource.CASH]: { displayName: 'Cash', shortLabel: 'Cash' },
  [PaymentSource.MANUAL]: { displayName: 'Manual Entry', shortLabel: 'Manual' },
};

export const ALL_PAYMENT_SOURCES: PaymentSource[] = [
  PaymentSource.GOOGLE_PAY,
  PaymentSource.E_WALLET,
  PaymentSource.CREDIT_CARD,
  PaymentSource.SMS_ALERT,
  PaymentSource.CASH,
  PaymentSource.MANUAL,
];

export function parsePaymentSource(raw?: string | null): PaymentSource {
  if (!raw) return PaymentSource.MANUAL;
  const upper = raw.trim().toUpperCase();
  return ALL_PAYMENT_SOURCES.find((s) => s === upper) ?? PaymentSource.MANUAL;
}

export enum ThemeModePreference {
  SYSTEM = 'SYSTEM',
  LIGHT = 'LIGHT',
  DARK = 'DARK',
}

export enum ReportPeriod {
  DAY = 'DAY',
  WEEK = 'WEEK',
  MONTH = 'MONTH',
  YEAR = 'YEAR',
  CUSTOM = 'CUSTOM',
}

export const REPORT_PERIOD_LABELS: Record<ReportPeriod, string> = {
  [ReportPeriod.DAY]: 'Day',
  [ReportPeriod.WEEK]: 'Week',
  [ReportPeriod.MONTH]: 'Month',
  [ReportPeriod.YEAR]: 'Year',
  [ReportPeriod.CUSTOM]: 'Period',
};

export const SUPPORTED_CURRENCIES = ['PHP', 'USD', 'EUR', 'GBP', 'JPY', 'SGD', 'AUD', 'CAD'] as const;
export type SupportedCurrency = (typeof SUPPORTED_CURRENCIES)[number];

export interface SpendingTransaction {
  id: string;
  userId: string;
  merchant: string;
  amountCents: number;
  category: SpendingCategory;
  paymentSource: PaymentSource;
  note: string;
  rawSnippet?: string;
  occurredAtMillis: number;
  needsReview: boolean;
  createdAtMillis?: number;
  updatedAtMillis?: number;
}

export interface CategoryBudget {
  id: string;
  userId: string;
  category: SpendingCategory;
  monthlyLimitCents: number;
}

export interface RecurringPreset {
  id: string;
  userId: string;
  label: string;
  merchant: string;
  amountCents: number;
  category: SpendingCategory;
  paymentSource: PaymentSource;
}

export interface MerchantRule {
  id: string;
  userId: string;
  merchantKeyword: string;
  category: SpendingCategory;
}

export interface UserFinanceSettings {
  id: string;
  userId: string;
  monthlyIncomeCents: number;
  monthlyBudgetCents: number;
  currencyCode: SupportedCurrency;
  githubRepo: string;
}

export interface SavingsProjection {
  daysInMonth: number;
  currentDayOfMonth: number;
  spentToDateCents: number;
  projectedMonthEndSpendCents: number;
  monthlyIncomeCents: number;
  monthlyBudgetCents: number;
  projectedSavingsCents: number;
  actualCumulativeDailyCents: number[];
  projectedDailyCents: number[];
  remainingDaysInMonth: number;
  dailyAllowanceCents: number;
  noSpendDaysThisMonth: number;
}

export interface TrendBucket {
  label: string;
  amountCents: number;
}

export interface CategoryBreakdownItem {
  category: SpendingCategory;
  spentCents: number;
  budgetCents: number;
  shareFraction: number;
  transactionCount: number;
}

export interface MerchantTotalItem {
  merchant: string;
  totalCents: number;
  count: number;
  primaryCategory: SpendingCategory;
}

export interface PaymentSourceBreakdown {
  source: PaymentSource;
  totalCents: number;
  shareFraction: number;
}

export interface PeriodReportData {
  periodLabel: string;
  totalSpentCents: number;
  transactionCount: number;
  averagePerTransactionCents: number;
  trendBuckets: TrendBucket[];
  categoryBreakdown: CategoryBreakdownItem[];
  topMerchants: MerchantTotalItem[];
  paymentSources: PaymentSourceBreakdown[];
}

export interface GitHubReleaseNotice {
  tagName: string;
  releaseName: string;
  notes: string;
  htmlUrl: string;
}

export interface DashboardSnapshot {
  settings: UserFinanceSettings;
  transactions: SpendingTransaction[];
  monthTransactions: SpendingTransaction[];
  monthSpentCents: number;
  budgets: CategoryBudget[];
  recurringPresets: RecurringPreset[];
  merchantRules: MerchantRule[];
  savingsProjection: SavingsProjection;
  unreviewedCount: number;
}

// Validation limits synchronized verbatim with firebase-blueprint.json & firestore.rules
export const LIMITS = {
  MERCHANT_MAX_LEN: 120,
  NOTE_MAX_LEN: 300,
  RAW_SNIPPET_MAX_LEN: 500,
  PRESET_LABEL_MAX_LEN: 40,
  RULE_KEYWORD_MAX_LEN: 100,
  GITHUB_REPO_MAX_LEN: 120,
  AMOUNT_MAX_CENTS: 1_000_000_000,
  SETTINGS_MAX_CENTS: 10_000_000_000,
  ID_PATTERN: /^[a-zA-Z0-9_-]{1,128}$/,
} as const;
