import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  type Unsubscribe,
} from 'firebase/firestore';
import {
  LIMITS,
  parsePaymentSource,
  parseSpendingCategory,
  PaymentSource,
  SpendingCategory,
  type CategoryBudget,
  type MerchantRule,
  type RecurringPreset,
  type SpendingTransaction,
  type SupportedCurrency,
  type UserFinanceSettings,
  SUPPORTED_CURRENCIES,
} from '../types/ledger';
import { auth, db, handleFirestoreError, OperationType } from './firebase';

function requireActiveUid(expectedUid?: string): string {
  const uid = auth.currentUser?.uid;
  if (!uid) {
    throw new Error('User must be signed in with Google before accessing Firestore.');
  }
  if (expectedUid && uid !== expectedUid) {
    throw new Error('Signed-in user changed; refusing cross-user access.');
  }
  return uid;
}

function assertValidDocId(id: string, label: string): string {
  const trimmed = id.trim();
  if (!LIMITS.ID_PATTERN.test(trimmed)) {
    throw new Error(`Invalid ${label} ID.`);
  }
  return trimmed;
}

function clampInt(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) return min;
  return Math.max(min, Math.min(max, Math.round(value)));
}

function toMillis(ts: unknown): number | undefined {
  if (ts instanceof Timestamp) return ts.toMillis();
  return undefined;
}

function sanitizeCurrency(code: string): SupportedCurrency {
  const upper = code.trim().toUpperCase();
  return (SUPPORTED_CURRENCIES as readonly string[]).includes(upper)
    ? (upper as SupportedCurrency)
    : 'PHP';
}

export function observeTransactions(
  userId: string,
  onData: (items: SpendingTransaction[]) => void
): Unsubscribe {
  const path = `users/${userId}/transactions`;
  const q = query(collection(db, 'users', userId, 'transactions'), where('userId', '==', userId));
  return onSnapshot(
    q,
    (snapshot) => {
      const list: SpendingTransaction[] = snapshot.docs.map((d) => {
        const raw = d.data({ serverTimestamps: 'estimate' });
        return {
          id: d.id,
          userId: typeof raw.userId === 'string' ? raw.userId : userId,
          merchant: typeof raw.merchant === 'string' ? raw.merchant : '',
          amountCents: typeof raw.amountCents === 'number' ? raw.amountCents : 0,
          category: parseSpendingCategory(typeof raw.category === 'string' ? raw.category : null),
          paymentSource: parsePaymentSource(
            typeof raw.paymentSource === 'string' ? raw.paymentSource : null
          ),
          note: typeof raw.note === 'string' ? raw.note : '',
          rawSnippet: typeof raw.rawSnippet === 'string' ? raw.rawSnippet : undefined,
          occurredAtMillis: typeof raw.occurredAtMillis === 'number' ? raw.occurredAtMillis : 0,
          needsReview: Boolean(raw.needsReview),
          createdAtMillis: toMillis(raw.createdAt),
          updatedAtMillis: toMillis(raw.updatedAt),
        };
      });
      list.sort((a, b) => b.occurredAtMillis - a.occurredAtMillis);
      onData(list);
    },
    (error) => {
      if (auth.currentUser?.uid === userId) {
        handleFirestoreError(error, OperationType.LIST, path);
      }
    }
  );
}

export function observeBudgets(
  userId: string,
  onData: (items: CategoryBudget[]) => void
): Unsubscribe {
  const path = `users/${userId}/budgets`;
  const q = query(collection(db, 'users', userId, 'budgets'), where('userId', '==', userId));
  return onSnapshot(
    q,
    (snapshot) => {
      const list: CategoryBudget[] = snapshot.docs.map((d) => {
        const raw = d.data({ serverTimestamps: 'estimate' });
        return {
          id: d.id,
          userId: typeof raw.userId === 'string' ? raw.userId : userId,
          category: parseSpendingCategory(typeof raw.category === 'string' ? raw.category : d.id),
          monthlyLimitCents: typeof raw.monthlyLimitCents === 'number' ? raw.monthlyLimitCents : 0,
        };
      });
      onData(list);
    },
    (error) => {
      if (auth.currentUser?.uid === userId) {
        handleFirestoreError(error, OperationType.LIST, path);
      }
    }
  );
}

export function observeRecurringPresets(
  userId: string,
  onData: (items: RecurringPreset[]) => void
): Unsubscribe {
  const path = `users/${userId}/recurringPresets`;
  const q = query(collection(db, 'users', userId, 'recurringPresets'), where('userId', '==', userId));
  return onSnapshot(
    q,
    (snapshot) => {
      const list: RecurringPreset[] = snapshot.docs.map((d) => {
        const raw = d.data({ serverTimestamps: 'estimate' });
        return {
          id: d.id,
          userId: typeof raw.userId === 'string' ? raw.userId : userId,
          label: typeof raw.label === 'string' ? raw.label : '',
          merchant: typeof raw.merchant === 'string' ? raw.merchant : '',
          amountCents: typeof raw.amountCents === 'number' ? raw.amountCents : 0,
          category: parseSpendingCategory(typeof raw.category === 'string' ? raw.category : null),
          paymentSource: parsePaymentSource(
            typeof raw.paymentSource === 'string' ? raw.paymentSource : null
          ),
        };
      });
      list.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()));
      onData(list);
    },
    (error) => {
      if (auth.currentUser?.uid === userId) {
        handleFirestoreError(error, OperationType.LIST, path);
      }
    }
  );
}

export function observeMerchantRules(
  userId: string,
  onData: (items: MerchantRule[]) => void
): Unsubscribe {
  const path = `users/${userId}/merchantRules`;
  const q = query(collection(db, 'users', userId, 'merchantRules'), where('userId', '==', userId));
  return onSnapshot(
    q,
    (snapshot) => {
      const list: MerchantRule[] = snapshot.docs.map((d) => {
        const raw = d.data({ serverTimestamps: 'estimate' });
        return {
          id: d.id,
          userId: typeof raw.userId === 'string' ? raw.userId : userId,
          merchantKeyword: typeof raw.merchantKeyword === 'string' ? raw.merchantKeyword : '',
          category: parseSpendingCategory(typeof raw.category === 'string' ? raw.category : null),
        };
      });
      list.sort((a, b) => a.merchantKeyword.localeCompare(b.merchantKeyword));
      onData(list);
    },
    (error) => {
      if (auth.currentUser?.uid === userId) {
        handleFirestoreError(error, OperationType.LIST, path);
      }
    }
  );
}

export function defaultSettingsFor(userId: string): UserFinanceSettings {
  return {
    id: 'preferences',
    userId,
    monthlyIncomeCents: 400_000,
    monthlyBudgetCents: 240_000,
    currencyCode: 'PHP',
    githubRepo: '',
  };
}

export function observeSettings(
  userId: string,
  onData: (settings: UserFinanceSettings) => void
): Unsubscribe {
  const path = `users/${userId}/settings/preferences`;
  const ref = doc(db, 'users', userId, 'settings', 'preferences');
  return onSnapshot(
    ref,
    (snapshot) => {
      if (!snapshot.exists()) {
        onData(defaultSettingsFor(userId));
        return;
      }
      const raw = snapshot.data({ serverTimestamps: 'estimate' });
      onData({
        id: snapshot.id,
        userId: typeof raw.userId === 'string' ? raw.userId : userId,
        monthlyIncomeCents:
          typeof raw.monthlyIncomeCents === 'number' ? raw.monthlyIncomeCents : 400_000,
        monthlyBudgetCents:
          typeof raw.monthlyBudgetCents === 'number' ? raw.monthlyBudgetCents : 240_000,
        currencyCode: sanitizeCurrency(
          typeof raw.currencyCode === 'string' ? raw.currencyCode : 'PHP'
        ),
        githubRepo: typeof raw.githubRepo === 'string' ? raw.githubRepo : '',
      });
    },
    (error) => {
      if (auth.currentUser?.uid === userId) {
        handleFirestoreError(error, OperationType.GET, path);
      }
    }
  );
}

export async function ensureDefaultSettings(userId: string): Promise<void> {
  const path = `users/${userId}/settings/preferences`;
  try {
    const uid = requireActiveUid(userId);
    const ref = doc(db, 'users', uid, 'settings', 'preferences');
    const existing = await getDoc(ref);
    if (!existing.exists()) {
      await setDoc(ref, {
        userId: uid,
        monthlyIncomeCents: 400_000,
        monthlyBudgetCents: 240_000,
        currencyCode: 'PHP',
        githubRepo: '',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export interface AddTransactionInput {
  merchant: string;
  amountCents: number;
  category: SpendingCategory;
  paymentSource: PaymentSource;
  note?: string;
  rawSnippet?: string;
  occurredAtMillis?: number;
  needsReview?: boolean;
}

export async function addTransaction(
  userId: string,
  input: AddTransactionInput
): Promise<string> {
  let docPath = `users/${userId}/transactions`;
  try {
    const uid = requireActiveUid(userId);
    const cleanMerchant = input.merchant.trim().slice(0, LIMITS.MERCHANT_MAX_LEN);
    if (!cleanMerchant) {
      throw new Error('Merchant name is required.');
    }
    const cleanAmount = clampInt(input.amountCents, 1, LIMITS.AMOUNT_MAX_CENTS);
    const occurredAtMillis = clampInt(
      input.occurredAtMillis ?? Date.now(),
      1,
      Number.MAX_SAFE_INTEGER
    );
    const cleanNote = (input.note ?? '').trim().slice(0, LIMITS.NOTE_MAX_LEN);
    const cleanSnippet = (input.rawSnippet ?? '').trim().slice(0, LIMITS.RAW_SNIPPET_MAX_LEN);

    const ref = doc(collection(db, 'users', uid, 'transactions'));
    docPath = ref.path;

    const payload: Record<string, unknown> = {
      userId: uid,
      merchant: cleanMerchant,
      amountCents: cleanAmount,
      category: parseSpendingCategory(input.category),
      paymentSource: parsePaymentSource(input.paymentSource),
      note: cleanNote,
      occurredAtMillis,
      needsReview: Boolean(input.needsReview),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (cleanSnippet.length > 0) {
      payload.rawSnippet = cleanSnippet;
    }

    await setDoc(ref, payload);
    return ref.id;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, docPath);
  }
}

export interface UpdateTransactionInput {
  transactionId: string;
  merchant: string;
  amountCents: number;
  category: SpendingCategory;
  paymentSource: PaymentSource;
  note: string;
  occurredAtMillis: number;
  needsReview: boolean;
}

export async function updateTransaction(
  userId: string,
  input: UpdateTransactionInput
): Promise<void> {
  const txId = assertValidDocId(input.transactionId, 'transaction');
  const docPath = `users/${userId}/transactions/${txId}`;
  try {
    const uid = requireActiveUid(userId);
    const cleanMerchant = input.merchant.trim().slice(0, LIMITS.MERCHANT_MAX_LEN);
    if (!cleanMerchant) {
      throw new Error('Merchant name is required.');
    }
    const cleanAmount = clampInt(input.amountCents, 1, LIMITS.AMOUNT_MAX_CENTS);
    const occurredAtMillis = clampInt(input.occurredAtMillis, 1, Number.MAX_SAFE_INTEGER);
    const cleanNote = input.note.trim().slice(0, LIMITS.NOTE_MAX_LEN);

    const ref = doc(db, 'users', uid, 'transactions', txId);
    await updateDoc(ref, {
      merchant: cleanMerchant,
      amountCents: cleanAmount,
      category: parseSpendingCategory(input.category),
      paymentSource: parsePaymentSource(input.paymentSource),
      note: cleanNote,
      occurredAtMillis,
      needsReview: Boolean(input.needsReview),
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, docPath);
  }
}

export async function deleteTransaction(userId: string, transactionId: string): Promise<void> {
  const txId = assertValidDocId(transactionId, 'transaction');
  const docPath = `users/${userId}/transactions/${txId}`;
  try {
    const uid = requireActiveUid(userId);
    await deleteDoc(doc(db, 'users', uid, 'transactions', txId));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, docPath);
  }
}

export async function upsertCategoryBudget(
  userId: string,
  category: SpendingCategory,
  monthlyLimitCents: number
): Promise<void> {
  const safeCat = parseSpendingCategory(category);
  const docPath = `users/${userId}/budgets/${safeCat}`;
  try {
    const uid = requireActiveUid(userId);
    const safeLimit = clampInt(monthlyLimitCents, 0, LIMITS.AMOUNT_MAX_CENTS);
    const ref = doc(db, 'users', uid, 'budgets', safeCat);
    const snap = await getDoc(ref);
    if (snap.exists()) {
      await updateDoc(ref, {
        monthlyLimitCents: safeLimit,
        updatedAt: serverTimestamp(),
      });
    } else {
      await setDoc(ref, {
        userId: uid,
        category: safeCat,
        monthlyLimitCents: safeLimit,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, docPath);
  }
}

export async function addRecurringPreset(
  userId: string,
  input: {
    label: string;
    merchant: string;
    amountCents: number;
    category: SpendingCategory;
    paymentSource: PaymentSource;
  }
): Promise<void> {
  let docPath = `users/${userId}/recurringPresets`;
  try {
    const uid = requireActiveUid(userId);
    const cleanLabel = input.label.trim().slice(0, LIMITS.PRESET_LABEL_MAX_LEN);
    const cleanMerchant = input.merchant.trim().slice(0, LIMITS.MERCHANT_MAX_LEN);
    if (!cleanLabel) throw new Error('Preset label is required.');
    if (!cleanMerchant) throw new Error('Merchant name is required.');
    const cleanAmount = clampInt(input.amountCents, 1, LIMITS.AMOUNT_MAX_CENTS);

    const ref = doc(collection(db, 'users', uid, 'recurringPresets'));
    docPath = ref.path;
    await setDoc(ref, {
      userId: uid,
      label: cleanLabel,
      merchant: cleanMerchant,
      amountCents: cleanAmount,
      category: parseSpendingCategory(input.category),
      paymentSource: parsePaymentSource(input.paymentSource),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, docPath);
  }
}

export async function deleteRecurringPreset(userId: string, presetId: string): Promise<void> {
  const safeId = assertValidDocId(presetId, 'preset');
  const docPath = `users/${userId}/recurringPresets/${safeId}`;
  try {
    const uid = requireActiveUid(userId);
    await deleteDoc(doc(db, 'users', uid, 'recurringPresets', safeId));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, docPath);
  }
}

export async function upsertMerchantRule(
  userId: string,
  merchantKeyword: string,
  category: SpendingCategory
): Promise<void> {
  const normalized = merchantKeyword.trim().toLowerCase().slice(0, LIMITS.RULE_KEYWORD_MAX_LEN);
  const rawDocId = normalized.replace(/[^a-z0-9_-]/g, '_').slice(0, 120) || 'rule';
  const docId = LIMITS.ID_PATTERN.test(rawDocId) ? rawDocId : 'rule';
  const docPath = `users/${userId}/merchantRules/${docId}`;
  try {
    const uid = requireActiveUid(userId);
    if (!normalized) {
      throw new Error('Keyword cannot be empty.');
    }
    const safeCat = parseSpendingCategory(category);
    const ref = doc(db, 'users', uid, 'merchantRules', docId);
    const snap = await getDoc(ref);
    if (snap.exists()) {
      await updateDoc(ref, {
        merchantKeyword: normalized,
        category: safeCat,
        updatedAt: serverTimestamp(),
      });
    } else {
      await setDoc(ref, {
        userId: uid,
        merchantKeyword: normalized,
        category: safeCat,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, docPath);
  }
}

export async function deleteMerchantRule(userId: string, ruleId: string): Promise<void> {
  const safeId = assertValidDocId(ruleId, 'rule');
  const docPath = `users/${userId}/merchantRules/${safeId}`;
  try {
    const uid = requireActiveUid(userId);
    await deleteDoc(doc(db, 'users', uid, 'merchantRules', safeId));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, docPath);
  }
}

export async function saveSettings(
  userId: string,
  input: {
    monthlyIncomeCents: number;
    monthlyBudgetCents: number;
    currencyCode: string;
    githubRepo: string;
  }
): Promise<void> {
  const docPath = `users/${userId}/settings/preferences`;
  try {
    const uid = requireActiveUid(userId);
    const safeIncome = clampInt(input.monthlyIncomeCents, 0, LIMITS.SETTINGS_MAX_CENTS);
    const safeBudget = clampInt(input.monthlyBudgetCents, 0, LIMITS.SETTINGS_MAX_CENTS);
    const safeCurrency = sanitizeCurrency(input.currencyCode);
    const safeRepo = input.githubRepo.trim().slice(0, LIMITS.GITHUB_REPO_MAX_LEN);

    const ref = doc(db, 'users', uid, 'settings', 'preferences');
    const snap = await getDoc(ref);
    if (snap.exists()) {
      await updateDoc(ref, {
        monthlyIncomeCents: safeIncome,
        monthlyBudgetCents: safeBudget,
        currencyCode: safeCurrency,
        githubRepo: safeRepo,
        updatedAt: serverTimestamp(),
      });
    } else {
      await setDoc(ref, {
        userId: uid,
        monthlyIncomeCents: safeIncome,
        monthlyBudgetCents: safeBudget,
        currencyCode: safeCurrency,
        githubRepo: safeRepo,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, docPath);
  }
}
