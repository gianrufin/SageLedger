import { LIMITS, PaymentSource } from '../types/ledger';

export interface ParsedPurchase {
  merchant: string;
  amountCents: number;
  paymentSource: PaymentSource;
  rawSnippet: string;
}

const E_WALLET_KEYWORDS = [
  'gcash', 'paymaya', 'maya', 'grabpay', 'shopeepay', 'paypal',
  'venmo', 'cash app', 'coins.ph', 'e-wallet', 'ewallet', 'qrph', 'qr ph',
];

const IGNORE_KEYWORDS = [
  'otp', 'one-time password', 'verification code', 'security code', 'login code',
  '2fa', 'declined', 'failed', 'unsuccessful', 'refunded', 'credited to your',
  'salary credited', 'deposit received', 'promo', 'cashback earned', 'statement ready',
  'due date', 'minimum payment',
];

const AMOUNT_REGEX =
  /(?:USD|PHP|EUR|GBP|SGD|AUD|CAD|JPY|Php|[$₱€£¥])\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)/i;

const POST_CURRENCY_AMOUNT_REGEX =
  /([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:USD|PHP|EUR|GBP|SGD|AUD|CAD)/i;

const MERCHANT_PATTERNS = [
  /\b(?:at|to|from|merchant:?|store:?)\s+([A-Za-z0-9&'.,\-/# ]{2,45}?)(?:\s+(?:on|for|using|via|with|card|ending|ref|was|is|balance|avail)\b|[.!?\n•|]|$)/i,
  /\bpaid\s+([A-Za-z0-9&'.,\-/# ]{2,40}?)\s+(?:[$₱€£¥]|USD|PHP|EUR|GBP)/i,
  /^([A-Za-z0-9&'.,\-/# ]{2,40}?)\s*[-•|]\s*(?:[$₱€£¥]|USD|PHP|EUR|GBP)/i,
];

const GENERIC_TITLES = new Set([
  'google wallet', 'google pay', 'wallet', 'payment alert', 'transaction alert',
  'card alert', 'bank alert', 'purchase alert', 'new message', 'sms', 'messages',
]);

function extractAmountCents(text: string): number | null {
  const match = AMOUNT_REGEX.exec(text) ?? POST_CURRENCY_AMOUNT_REGEX.exec(text);
  const numericStr = match?.[1]?.replace(/,/g, '');
  if (!numericStr) return null;
  const val = Number.parseFloat(numericStr);
  if (Number.isNaN(val)) return null;
  return Math.round(val * 100);
}

function extractMerchant(text: string): string | null {
  for (const pattern of MERCHANT_PATTERNS) {
    const match = pattern.exec(text);
    const candidate = match?.[1]
      ?.trim()
      .replace(/[.,\-:]+$/, '')
      .replace(/\s+/g, ' ');
    if (candidate && !GENERIC_TITLES.has(candidate.toLowerCase())) {
      return candidate;
    }
  }
  return null;
}

export function parseRawAlertText(snippet: string): ParsedPurchase | null {
  const trimmed = snippet.trim();
  if (!trimmed) return null;
  const lower = trimmed.toLowerCase();
  if (IGNORE_KEYWORDS.some((kw) => lower.includes(kw))) return null;

  const amountCents = extractAmountCents(trimmed);
  if (amountCents === null || amountCents <= 0) return null;

  const merchant = (extractMerchant(trimmed) ?? 'Store Purchase').slice(0, LIMITS.MERCHANT_MAX_LEN);
  let paymentSource = PaymentSource.SMS_ALERT;
  if (E_WALLET_KEYWORDS.some((kw) => lower.includes(kw))) {
    paymentSource = PaymentSource.E_WALLET;
  } else if (
    lower.includes('google pay') ||
    lower.includes('google wallet') ||
    lower.includes('gpay')
  ) {
    paymentSource = PaymentSource.GOOGLE_PAY;
  } else if (
    lower.includes('card') ||
    lower.includes('visa') ||
    lower.includes('mastercard') ||
    lower.includes('amex')
  ) {
    paymentSource = PaymentSource.CREDIT_CARD;
  }

  return {
    merchant,
    amountCents,
    paymentSource,
    rawSnippet: trimmed.slice(0, LIMITS.RAW_SNIPPET_MAX_LEN),
  };
}
