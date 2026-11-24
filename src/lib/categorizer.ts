import { parseSpendingCategory, SpendingCategory, type MerchantRule } from '../types/ledger';

const BUILT_IN_CATEGORY_RULES: Array<[SpendingCategory, string[]]> = [
  [
    SpendingCategory.CONVENIENCE,
    [
      '7-eleven', '7 eleven', '7eleven', 'ministop', 'uncle john', 'familymart',
      'lawson', 'circle k', 'wawa', 'sheetz', 'alfamart', 'convenience', 'mini mart',
      'minimart', 'quick stop', 'am pm', 'ampm',
    ],
  ],
  [
    SpendingCategory.GADGETS,
    [
      'apple store', 'apple.com', 'best buy', 'samsung', 'sony', 'lenovo',
      'power mac', 'beyond the box', 'digital walker', 'octagon', 'micro center',
      'b&h', 'newegg', 'xiaomi', 'abenson', 'datablitz', 'itech', 'gadget',
      'electronics', 'computer', 'pc express', 'cyberzone', 'logitech', 'anker',
    ],
  ],
  [
    SpendingCategory.TRANSPORT_FUEL,
    [
      'shell', 'petron', 'caltex', 'chevron', 'exxon', 'mobil', 'bp ', 'cleanfuel',
      'seaoil', 'phoenix', 'unioil', 'gas station', 'fuel', 'petrol', 'diesel',
      'toll', 'easytrip', 'autosweep', 'ez-pass', 'ezpass', 'fastrak', 'sunpass',
      'uber', 'lyft', 'grab ', 'angkas', 'joyride', 'metro', 'mrt', 'lrt',
      'transit', 'parking', 'valet', 'airline', 'skyway', 'nlex', 'slex',
    ],
  ],
  [
    SpendingCategory.GROCERIES,
    [
      'trader joe', 'whole foods', 'walmart', 'target', 'costco', 'aldi', 'kroger',
      'safeway', 'publix', 'wegmans', 'sm supermarket', 'sm hypermarket',
      'robinsons supermarket', 'puregold', 'landers', 's&r', 'marketplace',
      "rustan's", 'supermarket', 'grocery', 'groceries', 'fresh market',
    ],
  ],
  [
    SpendingCategory.FOOD,
    [
      'jollibee', 'mcdonald', 'kfc', 'burger king', 'wendy', 'popeyes', 'chowking',
      'mang inasal', 'greenwich', 'shake shack', 'chipotle', 'subway', 'taco bell',
      'chick-fil-a', 'panda express', 'starbucks', 'coffee', 'cafe', 'bistro',
      'ramen', 'sushi', 'pizza', 'bakery', 'bakeshop', 'grill', 'kitchen',
      'diner', 'restaurant', 'eatery', 'grabfood', 'foodpanda', 'doordash',
      'ubereats', 'deliveroo', 'tim hortons', 'dunkin', "bo's coffee", 'pickup coffee',
    ],
  ],
  [
    SpendingCategory.UTILITIES,
    [
      'electric', 'meralco', 'water', 'maynilad', 'manila water', 'internet',
      'fiber', 'converge', 'pldt', 'globe', 'smart', 'verizon', 'at&t', 't-mobile',
      'comcast', 'xfinity', 'netflix', 'spotify', 'icloud', 'google one',
      'youtube premium', 'insurance', 'utility', 'bill', 'power',
    ],
  ],
  [
    SpendingCategory.SHOPPING,
    [
      'amazon', 'shopee', 'lazada', 'uniqlo', 'zara', 'h&m', 'nike', 'adidas',
      'ikea', 'decathlon', 'sephora', 'watsons', 'mercury drug', 'cvs', 'walgreens',
      'department store', 'boutique', 'apparel', 'mall',
    ],
  ],
];

export function categorizeLocal(
  merchant: string,
  rawSnippet: string | undefined,
  userRules: MerchantRule[] = []
): SpendingCategory | null {
  const normalizedMerchant = merchant.toLowerCase().trim();
  const combined = `${normalizedMerchant} ${(rawSnippet ?? '').toLowerCase()}`;

  const sortedRules = [...userRules].sort(
    (a, b) => b.merchantKeyword.length - a.merchantKeyword.length
  );
  for (const rule of sortedRules) {
    const kw = rule.merchantKeyword.toLowerCase().trim();
    if (kw.length > 0 && normalizedMerchant.includes(kw)) {
      return rule.category;
    }
  }

  for (const [category, keywords] of BUILT_IN_CATEGORY_RULES) {
    if (keywords.some((kw) => combined.includes(kw))) {
      return category;
    }
  }

  return null;
}

export async function categorizeWithFallback(
  merchant: string,
  rawSnippet?: string,
  userRules: MerchantRule[] = []
): Promise<{ category: SpendingCategory; usedAiFallback: boolean }> {
  const local = categorizeLocal(merchant, rawSnippet, userRules);
  if (local) {
    return { category: local, usedAiFallback: false };
  }

  try {
    const resp = await fetch('/api/categorize', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        merchant: merchant.trim().slice(0, 120),
        rawSnippet: rawSnippet?.slice(0, 200),
      }),
    });
    if (!resp.ok) {
      return { category: SpendingCategory.OTHER, usedAiFallback: false };
    }
    const data = (await resp.json()) as { category?: string; usedAiFallback?: boolean };
    return {
      category: parseSpendingCategory(data.category),
      usedAiFallback: Boolean(data.usedAiFallback),
    };
  } catch {
    return { category: SpendingCategory.OTHER, usedAiFallback: false };
  }
}
