import React, { useState } from 'react';
import { Calendar, Sparkles, Trash2, X } from 'lucide-react';
import {
  ALL_CATEGORIES,
  ALL_PAYMENT_SOURCES,
  CATEGORY_META,
  LIMITS,
  PAYMENT_SOURCE_META,
  PaymentSource,
  SpendingCategory,
  type MerchantRule,
  type SpendingTransaction,
} from '../types/ledger';
import { symbolForCurrency } from '../lib/currency';
import { parseRawAlertText } from '../lib/notificationParser';
import { categorizeWithFallback } from '../lib/categorizer';
import { CategoryIcon, PaymentSourceIcon } from './IconsAndVisuals';

function toLocalDateTimeInputValue(millis: number): string {
  const d = new Date(millis);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}

export function ManualEntryModal({
  editingTransaction,
  currencyCode,
  merchantRules,
  onSave,
  onDelete,
  onClose,
}: {
  editingTransaction: SpendingTransaction | null;
  currencyCode: string;
  merchantRules: MerchantRule[];
  onSave: (payload: {
    existingId: string | null;
    merchant: string;
    amountCents: number;
    category: SpendingCategory;
    paymentSource: PaymentSource;
    note: string;
    rawSnippet?: string;
    occurredAtMillis: number;
    rememberMerchantRule: boolean;
  }) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
  onClose: () => void;
}) {
  const [merchant, setMerchant] = useState(editingTransaction?.merchant ?? '');
  const [amountText, setAmountText] = useState(
    editingTransaction ? (editingTransaction.amountCents / 100).toFixed(2) : ''
  );
  const [selectedCategory, setSelectedCategory] = useState<SpendingCategory>(
    editingTransaction?.category ?? SpendingCategory.FOOD
  );
  const [selectedSource, setSelectedSource] = useState<PaymentSource>(
    editingTransaction?.paymentSource ?? PaymentSource.CASH
  );
  const [note, setNote] = useState(editingTransaction?.note ?? '');
  const [rawSnippet, setRawSnippet] = useState(editingTransaction?.rawSnippet ?? '');
  const [occurredAtMillis, setOccurredAtMillis] = useState(
    editingTransaction?.occurredAtMillis ?? Date.now()
  );
  const [rememberRule, setRememberRule] = useState(false);
  const [showSmsPasteBox, setShowSmsPasteBox] = useState(false);
  const [smsPasteInput, setSmsPasteInput] = useState('');
  const [isSuggesting, setIsSuggesting] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  const sym = symbolForCurrency(currencyCode);

  const handleSuggestCategory = async () => {
    if (!merchant.trim()) return;
    setIsSuggesting(true);
    try {
      const { category } = await categorizeWithFallback(
        merchant,
        rawSnippet.trim() || undefined,
        merchantRules
      );
      setSelectedCategory(category);
    } finally {
      setIsSuggesting(false);
    }
  };

  const handleParseSnippet = async () => {
    const parsed = parseRawAlertText(smsPasteInput);
    if (!parsed) {
      setValidationError('Could not detect an amount in that text.');
      return;
    }
    setValidationError(null);
    setIsSuggesting(true);
    try {
      const { category } = await categorizeWithFallback(
        parsed.merchant,
        parsed.rawSnippet,
        merchantRules
      );
      setMerchant(parsed.merchant);
      setAmountText((parsed.amountCents / 100).toFixed(2));
      setSelectedSource(parsed.paymentSource);
      setSelectedCategory(category);
      setRawSnippet(parsed.rawSnippet);
      setShowSmsPasteBox(false);
    } finally {
      setIsSuggesting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanMerchant = merchant.trim().slice(0, LIMITS.MERCHANT_MAX_LEN);
    const parsedDouble = Number.parseFloat(amountText);
    const cents = Number.isFinite(parsedDouble) ? Math.round(parsedDouble * 100) : 0;

    if (!cleanMerchant) {
      setValidationError('Please enter a store or merchant name.');
      return;
    }
    if (cents <= 0) {
      setValidationError('Please enter an amount greater than zero.');
      return;
    }
    if (cents > LIMITS.AMOUNT_MAX_CENTS) {
      setValidationError('Amount exceeds supported limit.');
      return;
    }

    setValidationError(null);
    setIsSubmitting(true);
    try {
      await onSave({
        existingId: editingTransaction?.id ?? null,
        merchant: cleanMerchant,
        amountCents: cents,
        category: selectedCategory,
        paymentSource: selectedSource,
        note: note.trim().slice(0, LIMITS.NOTE_MAX_LEN),
        rawSnippet: rawSnippet.trim() ? rawSnippet.trim().slice(0, LIMITS.RAW_SNIPPET_MAX_LEN) : undefined,
        occurredAtMillis,
        rememberMerchantRule: rememberRule,
      });
      onClose();
    } catch (err) {
      setValidationError(err instanceof Error ? err.message : 'Failed to save transaction.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/55 backdrop-blur-xs p-0 sm:p-4"
      onClick={onClose}
    >
      <form
        onSubmit={handleSubmit}
        onClick={(e) => e.stopPropagation()}
        className="w-full sm:max-w-lg rounded-t-[28px] sm:rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4 max-h-[92vh] overflow-y-auto"
      >
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-xl font-bold text-[var(--ink-primary)]">
            {editingTransaction ? 'Review Purchase' : 'Record Purchase or Cash'}
          </h2>
          <div className="flex items-center gap-1.5">
            {!editingTransaction ? (
              <button
                type="button"
                onClick={() => setShowSmsPasteBox((v) => !v)}
                data-testid="toggle_paste_alert_button"
                className="text-xs font-bold text-[var(--mint-primary)] hover:underline px-2 py-1 cursor-pointer"
              >
                {showSmsPasteBox ? 'Hide alert parser' : 'Paste SMS/Alert'}
              </button>
            ) : (
              <button
                type="button"
                onClick={async () => {
                  await onDelete(editingTransaction.id);
                  onClose();
                }}
                data-testid="delete_transaction_button"
                aria-label="Delete transaction"
                className="w-8 h-8 rounded-full hover:bg-[var(--coral-container)] text-[var(--coral-tertiary)] flex items-center justify-center cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close modal"
              className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {showSmsPasteBox && (
          <div className="p-3.5 rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] space-y-2.5">
            <label className="block text-xs font-semibold text-[var(--ink-muted)]">
              Bank, E-Wallet, or SMS text
            </label>
            <textarea
              value={smsPasteInput}
              onChange={(e) => setSmsPasteInput(e.target.value)}
              rows={2}
              placeholder="e.g. Paid ₱245.00 via GCash at 7-Eleven"
              data-testid="paste_sms_input"
              className="w-full rounded-xl bg-[var(--bg-surface)] border border-[var(--hairline)] px-3 py-2 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
            />
            <button
              type="button"
              disabled={!smsPasteInput.trim() || isSuggesting}
              onClick={handleParseSnippet}
              data-testid="parse_sms_button"
              className="rounded-full border border-[var(--mint-primary)]/50 text-[var(--mint-primary)] font-bold text-xs px-3.5 py-1.5 hover:bg-[var(--mint-container)]/35 disabled:opacity-50 cursor-pointer"
            >
              {isSuggesting ? 'Extracting...' : 'Extract details'}
            </button>
          </div>
        )}

        {/* Amount */}
        <div className="space-y-1">
          <label className="block text-xs font-semibold text-[var(--ink-muted)]">
            Amount ({sym})
          </label>
          <div className="relative">
            <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-base font-bold text-[var(--ink-muted)]">
              {sym}
            </span>
            <input
              type="text"
              inputMode="decimal"
              value={amountText}
              onChange={(e) => {
                const cleaned = e.target.value.replace(/[^0-9.]/g, '');
                if ((cleaned.match(/\./g) ?? []).length <= 1) {
                  setAmountText(cleaned);
                }
              }}
              placeholder="0.00"
              data-testid="amount_input"
              className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] pl-9 pr-4 py-3 text-lg font-extrabold tabular-nums text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
            />
          </div>
        </div>

        {/* Merchant */}
        <div className="space-y-1">
          <label className="block text-xs font-semibold text-[var(--ink-muted)]">
            Store or merchant
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={merchant}
              maxLength={LIMITS.MERCHANT_MAX_LEN}
              onChange={(e) => setMerchant(e.target.value)}
              placeholder="e.g. Jollibee, Starbucks, Shell, 7-Eleven"
              data-testid="merchant_input"
              className="flex-1 rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
            />
            {merchant.trim() && (
              <button
                type="button"
                disabled={isSuggesting}
                onClick={handleSuggestCategory}
                title="Auto-categorize merchant"
                className="shrink-0 inline-flex items-center gap-1.5 rounded-2xl bg-[var(--mint-container)] text-[var(--on-mint-container)] px-3 py-2 text-xs font-bold hover:opacity-90 disabled:opacity-50 cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>{isSuggesting ? '...' : 'Match'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Payment source */}
        <div className="space-y-1.5">
          <span className="block text-xs font-semibold text-[var(--ink-muted)]">Payment source</span>
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {ALL_PAYMENT_SOURCES.map((source) => {
              const selected = selectedSource === source;
              return (
                <button
                  key={source}
                  type="button"
                  onClick={() => setSelectedSource(source)}
                  className={`shrink-0 inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold border transition cursor-pointer ${
                    selected
                      ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                      : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
                  }`}
                >
                  <PaymentSourceIcon source={source} className="w-3.5 h-3.5" />
                  <span>{PAYMENT_SOURCE_META[source].displayName}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Category */}
        <div className="space-y-1.5">
          <span className="block text-xs font-semibold text-[var(--ink-muted)]">Category</span>
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {ALL_CATEGORIES.map((cat) => {
              const selected = selectedCategory === cat;
              return (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setSelectedCategory(cat)}
                  className={`shrink-0 inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold border transition cursor-pointer ${
                    selected
                      ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                      : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)] hover:text-[var(--ink-primary)]'
                  }`}
                >
                  <CategoryIcon category={cat} className="w-3.5 h-3.5" />
                  <span>{CATEGORY_META[cat].shortLabel}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Date & time */}
        <div className="space-y-1">
          <label className="flex items-center gap-1.5 text-xs font-semibold text-[var(--ink-muted)]">
            <Calendar className="w-3.5 h-3.5" />
            <span>Date &amp; time</span>
          </label>
          <input
            type="datetime-local"
            value={toLocalDateTimeInputValue(occurredAtMillis)}
            onChange={(e) => {
              const ts = new Date(e.target.value).getTime();
              if (!Number.isNaN(ts) && ts > 0) {
                setOccurredAtMillis(ts);
              }
            }}
            className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
          />
        </div>

        {/* Note */}
        <div className="space-y-1">
          <label className="block text-xs font-semibold text-[var(--ink-muted)]">
            Note (optional)
          </label>
          <input
            type="text"
            value={note}
            maxLength={LIMITS.NOTE_MAX_LEN}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Cash receipt, lunch split, etc."
            data-testid="note_input"
            className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
          />
        </div>

        {/* Remember merchant rule */}
        <label className="flex items-center justify-between gap-3 py-1 cursor-pointer">
          <div>
            <div className="text-sm font-semibold text-[var(--ink-primary)]">
              Remember store category
            </div>
            <div className="text-xs text-[var(--ink-muted)]">
              Auto-assign future alerts from this store
            </div>
          </div>
          <input
            type="checkbox"
            checked={rememberRule}
            onChange={(e) => setRememberRule(e.target.checked)}
            className="w-5 h-5 accent-[var(--mint-primary)] rounded cursor-pointer"
          />
        </label>

        {validationError && (
          <p className="text-xs font-medium text-[var(--coral-tertiary)]">{validationError}</p>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          data-testid="save_transaction_button"
          className="w-full h-13 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-base hover:opacity-95 disabled:opacity-60 transition cursor-pointer"
        >
          {isSubmitting
            ? 'Saving...'
            : editingTransaction
              ? 'Confirm & Update'
              : 'Save Spending'}
        </button>
      </form>
    </div>
  );
}

export function AddRecurringPresetModal({
  currencyCode,
  onSave,
  onClose,
}: {
  currencyCode: string;
  onSave: (payload: {
    label: string;
    merchant: string;
    amountCents: number;
    category: SpendingCategory;
    paymentSource: PaymentSource;
  }) => Promise<void>;
  onClose: () => void;
}) {
  const [label, setLabel] = useState('');
  const [merchant, setMerchant] = useState('');
  const [amountText, setAmountText] = useState('');
  const [category, setCategory] = useState<SpendingCategory>(SpendingCategory.FOOD);
  const [source, setSource] = useState<PaymentSource>(PaymentSource.GOOGLE_PAY);
  const [errorText, setErrorText] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const sym = symbolForCurrency(currencyCode);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cents = Math.round((Number.parseFloat(amountText) || 0) * 100);
    if (!label.trim()) {
      setErrorText('Enter a short button label.');
      return;
    }
    if (!merchant.trim()) {
      setErrorText('Enter a store name.');
      return;
    }
    if (cents <= 0 || cents > LIMITS.AMOUNT_MAX_CENTS) {
      setErrorText('Enter a valid amount.');
      return;
    }
    setIsSaving(true);
    try {
      await onSave({
        label: label.trim().slice(0, LIMITS.PRESET_LABEL_MAX_LEN),
        merchant: merchant.trim().slice(0, LIMITS.MERCHANT_MAX_LEN),
        amountCents: cents,
        category,
        paymentSource: source,
      });
      onClose();
    } catch (err) {
      setErrorText(err instanceof Error ? err.message : 'Failed to save shortcut.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4"
      onClick={onClose}
    >
      <form
        onSubmit={handleSubmit}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4 max-h-[90vh] overflow-y-auto"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-[var(--ink-primary)]">New Quick-Add Expense</h2>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <input
          type="text"
          value={label}
          maxLength={LIMITS.PRESET_LABEL_MAX_LEN}
          onChange={(e) => setLabel(e.target.value)}
          placeholder="Button label (e.g. Morning Coffee)"
          className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
        />

        <input
          type="text"
          value={merchant}
          maxLength={LIMITS.MERCHANT_MAX_LEN}
          onChange={(e) => setMerchant(e.target.value)}
          placeholder="Store (e.g. Starbucks)"
          className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
        />

        <input
          type="text"
          inputMode="decimal"
          value={amountText}
          onChange={(e) => {
            const cleaned = e.target.value.replace(/[^0-9.]/g, '');
            if ((cleaned.match(/\./g) ?? []).length <= 1) setAmountText(cleaned);
          }}
          placeholder={`Amount (${sym})`}
          className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm tabular-nums text-[var(--ink-primary)] focus:outline-none focus:border-[var(--mint-primary)]"
        />

        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {ALL_CATEGORIES.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setCategory(cat)}
              className={`shrink-0 px-3 py-1 rounded-full text-xs font-semibold border cursor-pointer ${
                category === cat
                  ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                  : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)]'
              }`}
            >
              {CATEGORY_META[cat].shortLabel}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {ALL_PAYMENT_SOURCES.map((src) => (
            <button
              key={src}
              type="button"
              onClick={() => setSource(src)}
              className={`shrink-0 px-3 py-1 rounded-full text-xs font-semibold border cursor-pointer ${
                source === src
                  ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                  : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)]'
              }`}
            >
              {PAYMENT_SOURCE_META[src].shortLabel}
            </button>
          ))}
        </div>

        {errorText && <p className="text-xs text-[var(--coral-tertiary)]">{errorText}</p>}

        <div className="flex items-center justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] hover:text-[var(--ink-primary)] cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSaving}
            className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold disabled:opacity-50 cursor-pointer"
          >
            {isSaving ? 'Adding...' : 'Add'}
          </button>
        </div>
      </form>
    </div>
  );
}
