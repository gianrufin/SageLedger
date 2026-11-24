import React from 'react';
import {
  Banknote,
  Bolt,
  Check,
  CreditCard,
  Fuel,
  Laptop,
  MessageSquare,
  Pencil,
  Receipt,
  ShoppingBag,
  ShoppingCart,
  Store,
  Utensils,
  Wallet,
} from 'lucide-react';
import {
  CATEGORY_META,
  PAYMENT_SOURCE_META,
  PaymentSource,
  SpendingCategory,
  type SpendingTransaction,
} from '../types/ledger';
import { splitCurrencyParts } from '../lib/currency';
import { formatDateTime } from '../lib/analytics';

export function CategoryIcon({
  category,
  className = 'w-4 h-4',
  style,
}: {
  category: SpendingCategory;
  className?: string;
  style?: React.CSSProperties;
}) {
  switch (category) {
    case SpendingCategory.FOOD:
      return <Utensils className={className} style={style} />;
    case SpendingCategory.GROCERIES:
      return <ShoppingCart className={className} style={style} />;
    case SpendingCategory.TRANSPORT_FUEL:
      return <Fuel className={className} style={style} />;
    case SpendingCategory.CONVENIENCE:
      return <Store className={className} style={style} />;
    case SpendingCategory.GADGETS:
      return <Laptop className={className} style={style} />;
    case SpendingCategory.UTILITIES:
      return <Bolt className={className} style={style} />;
    case SpendingCategory.SHOPPING:
      return <ShoppingBag className={className} style={style} />;
    case SpendingCategory.OTHER:
    default:
      return <Receipt className={className} style={style} />;
  }
}

export function PaymentSourceIcon({
  source,
  className = 'w-4 h-4',
}: {
  source: PaymentSource;
  className?: string;
}) {
  switch (source) {
    case PaymentSource.GOOGLE_PAY:
    case PaymentSource.E_WALLET:
      return <Wallet className={className} />;
    case PaymentSource.CREDIT_CARD:
      return <CreditCard className={className} />;
    case PaymentSource.SMS_ALERT:
      return <MessageSquare className={className} />;
    case PaymentSource.CASH:
      return <Banknote className={className} />;
    case PaymentSource.MANUAL:
    default:
      return <Pencil className={className} />;
  }
}

export function SplitCurrencyText({
  amountCents,
  currencyCode,
  showPlusSign = false,
  className = '',
  fractionOpacity = 0.45,
}: {
  amountCents: number;
  currencyCode: string;
  showPlusSign?: boolean;
  className?: string;
  fractionOpacity?: number;
}) {
  const { major, fraction } = splitCurrencyParts(amountCents, currencyCode, showPlusSign);
  return (
    <span className={`tabular-nums inline-flex items-baseline ${className}`}>
      <span className="font-extrabold">{major}</span>
      {fraction && (
        <span className="font-semibold" style={{ opacity: fractionOpacity }}>
          {fraction}
        </span>
      )}
    </span>
  );
}

export function CrispCard({
  children,
  className = '',
  accentGlowColor,
  onClick,
}: {
  children: React.ReactNode;
  className?: string;
  accentGlowColor?: string;
  onClick?: () => void;
}) {
  const baseClass = `relative overflow-hidden rounded-[22px] bg-[var(--bg-surface)] border border-[var(--hairline)] ${
    onClick ? 'cursor-pointer transition active:scale-[0.99] hover:border-[var(--mint-primary)]/40 text-left w-full' : ''
  } ${className}`;

  const glowStyle: React.CSSProperties | undefined = accentGlowColor
    ? {
        backgroundImage: `radial-gradient(240px circle at 100% 0%, color-mix(in srgb, ${accentGlowColor} 12%, transparent), transparent 72%)`,
      }
    : undefined;

  if (onClick) {
    return (
      <button type="button" onClick={onClick} style={glowStyle} className={baseClass}>
        {children}
      </button>
    );
  }

  return (
    <div style={glowStyle} className={baseClass}>
      {children}
    </div>
  );
}

export function LedgerTransactionRow({
  transaction,
  currencyCode,
  onClick,
  onConfirm,
  showDivider = true,
}: {
  transaction: SpendingTransaction;
  currencyCode: string;
  onClick: () => void;
  onConfirm?: (tx: SpendingTransaction) => void;
  showDivider?: boolean;
}) {
  const catMeta = CATEGORY_META[transaction.category];
  const srcMeta = PAYMENT_SOURCE_META[transaction.paymentSource];
  const isEWallet = transaction.paymentSource === PaymentSource.E_WALLET;

  return (
    <div className="group">
      <div className="flex items-center gap-3 px-4 py-3 hover:bg-[var(--bg-surface-low)]/70 transition">
        <button
          type="button"
          onClick={onClick}
          className="flex flex-1 min-w-0 items-center gap-3 text-left cursor-pointer"
        >
          <div
            className="w-10 h-10 rounded-[12px] bg-[var(--bg-surface-low)] border border-[var(--hairline)] flex items-center justify-center shrink-0"
            style={{ color: catMeta.chartColor }}
          >
            <CategoryIcon category={transaction.category} className="w-[18px] h-[18px]" />
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-[15px] text-[var(--ink-primary)] truncate">
                {transaction.merchant}
              </span>
              {transaction.needsReview && (
                <span
                  className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-[var(--coral-container)] text-[var(--on-coral-container)] shrink-0"
                >
                  Review
                </span>
              )}
            </div>
            <div className="flex items-center gap-1.5 text-xs text-[var(--ink-muted)] truncate mt-0.5">
              <span>{catMeta.shortLabel}</span>
              <span>·</span>
              <span className={isEWallet ? 'text-[var(--sky-secondary)] font-semibold' : ''}>
                {srcMeta.shortLabel}
              </span>
              <span>·</span>
              <span className="truncate">{formatDateTime(transaction.occurredAtMillis)}</span>
            </div>
          </div>

          <div className="text-right shrink-0">
            <SplitCurrencyText
              amountCents={transaction.amountCents}
              currencyCode={currencyCode}
              className="text-[15px] text-[var(--ink-primary)]"
            />
          </div>
        </button>

        {transaction.needsReview && onConfirm && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onConfirm(transaction);
            }}
            title="Confirm alert"
            aria-label="Confirm purchase alert"
            className="shrink-0 w-8 h-8 rounded-full bg-[var(--mint-container)] text-[var(--on-mint-container)] hover:opacity-90 flex items-center justify-center transition cursor-pointer"
          >
            <Check className="w-4 h-4" />
          </button>
        )}
      </div>

      {showDivider && (
        <div className="h-[1px] bg-[var(--hairline)] ml-[68px] mr-4" />
      )}
    </div>
  );
}
