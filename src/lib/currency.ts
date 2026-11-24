import { SUPPORTED_CURRENCIES, type SupportedCurrency } from '../types/ledger';

export function symbolForCurrency(currencyCode: string): string {
  switch (currencyCode.toUpperCase()) {
    case 'USD':
      return '$';
    case 'PHP':
      return '₱';
    case 'EUR':
      return '€';
    case 'GBP':
      return '£';
    case 'JPY':
      return '¥';
    case 'SGD':
      return 'S$';
    case 'AUD':
      return 'A$';
    case 'CAD':
      return 'C$';
    default:
      return '$';
  }
}

export function isSupportedCurrency(code: string): code is SupportedCurrency {
  return (SUPPORTED_CURRENCIES as readonly string[]).includes(code.toUpperCase());
}

export function formatCents(
  amountCents: number,
  currencyCode = 'PHP',
  showPlusSign = false
): string {
  const code = currencyCode.toUpperCase();
  const isZeroDecimal = code === 'JPY';
  const major = Math.abs(amountCents) / 100;

  let formatted: string;
  try {
    formatted = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: code,
      minimumFractionDigits: isZeroDecimal ? 0 : 2,
      maximumFractionDigits: isZeroDecimal ? 0 : 2,
    }).format(major);
  } catch {
    formatted = `${symbolForCurrency(code)}${major.toFixed(isZeroDecimal ? 0 : 2)}`;
  }

  const normalized = code === 'PHP' ? formatted.replace(/PHP|Php/g, '₱').trim() : formatted;
  if (amountCents < 0) return `-${normalized}`;
  if (showPlusSign && amountCents > 0) return `+${normalized}`;
  return normalized;
}

export function formatCompactCents(amountCents: number, currencyCode = 'PHP'): string {
  const sym = symbolForCurrency(currencyCode);
  const absMajor = Math.abs(amountCents) / 100;
  const prefix = amountCents < 0 ? '-' : '';
  if (absMajor >= 1000) {
    return `${prefix}${sym}${(absMajor / 1000).toFixed(1)}k`;
  }
  return `${prefix}${sym}${absMajor.toFixed(0)}`;
}

export function splitCurrencyParts(
  amountCents: number,
  currencyCode = 'PHP',
  showPlusSign = false
): { major: string; fraction: string } {
  const formatted = formatCents(amountCents, currencyCode, showPlusSign);
  const dotIdx = formatted.lastIndexOf('.');
  if (dotIdx > 0 && dotIdx < formatted.length - 1) {
    return {
      major: formatted.slice(0, dotIdx),
      fraction: formatted.slice(dotIdx),
    };
  }
  return { major: formatted, fraction: '' };
}
