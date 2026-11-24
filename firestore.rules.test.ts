import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { test, before, after, beforeEach } from 'node:test';
import fs from 'node:fs';
import { serverTimestamp } from 'firebase/firestore';

let testEnv: Awaited<ReturnType<typeof initializeTestEnvironment>>;
const PROJECT_ID = process.env.GCP_PROJECT || 'demo-sageledger';
const ALICE_UID = 'alice_uid_123';
const BOB_UID = 'bob_uid_456';

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085').split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules, host: emulatorHost, port: emulatorPort },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test('Dirty Dozen #1-4: unauthenticated, unverified, and cross-user isolation', async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const unverifiedAliceDb = testEnv.authenticatedContext(ALICE_UID, { email_verified: false }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email_verified: true }).firestore();

  // #1 Unauthenticated read/write blocked
  await assertFails(unauthDb.collection('users').doc(ALICE_UID).collection('transactions').get());

  // #2 Unverified email write blocked
  await assertFails(
    unverifiedAliceDb.collection('users').doc(ALICE_UID).collection('transactions').doc('tx_unv').set({
      userId: ALICE_UID,
      merchant: '7-Eleven',
      amountCents: 450,
      category: 'CONVENIENCE',
      paymentSource: 'E_WALLET',
      note: '',
      occurredAtMillis: Date.now(),
      needsReview: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Valid verified owner create succeeds
  await assertSucceeds(
    aliceDb.collection('users').doc(ALICE_UID).collection('transactions').doc('tx_1').set({
      userId: ALICE_UID,
      merchant: 'Jollibee',
      amountCents: 12500,
      category: 'FOOD',
      paymentSource: 'E_WALLET',
      note: 'Lunch',
      occurredAtMillis: Date.now(),
      needsReview: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // #3-4 Cross-user list and update blocked
  await assertFails(bobDb.collection('users').doc(ALICE_UID).collection('transactions').where('userId', '==', ALICE_UID).get());
  await assertFails(bobDb.collection('users').doc(ALICE_UID).collection('transactions').doc('tx_1').update({ amountCents: 100, updatedAt: serverTimestamp() }));
});

test('Dirty Dozen #5-12: spoofing, shadow fields, immutability, timestamps, poisoning, and enums', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email_verified: true }).firestore();
  const txCol = aliceDb.collection('users').doc(ALICE_UID).collection('transactions');

  // #5 Identity spoofing
  await assertFails(txCol.doc('tx_spoof').set({
    userId: BOB_UID, merchant: 'Cafe', amountCents: 500, category: 'FOOD',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));

  // #6 Shadow field on create
  await assertFails(txCol.doc('tx_shadow').set({
    userId: ALICE_UID, merchant: 'Cafe', amountCents: 500, category: 'FOOD',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    isAdmin: true, createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));

  // #9 Temporal tampering
  await assertFails(txCol.doc('tx_future').set({
    userId: ALICE_UID, merchant: 'Cafe', amountCents: 500, category: 'FOOD',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    createdAt: new Date(Date.now() + 60000), updatedAt: new Date(Date.now() + 60000),
  }));

  // #10 Value poisoning (negative amount & oversized merchant)
  await assertFails(txCol.doc('tx_neg').set({
    userId: ALICE_UID, merchant: 'Cafe', amountCents: -100, category: 'FOOD',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));
  await assertFails(txCol.doc('tx_long').set({
    userId: ALICE_UID, merchant: 'A'.repeat(121), amountCents: 100, category: 'FOOD',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));

  // #11 Settings doc ID restriction
  await assertFails(aliceDb.collection('users').doc(ALICE_UID).collection('settings').doc('evil').set({
    userId: ALICE_UID, monthlyIncomeCents: 1000, monthlyBudgetCents: 500,
    currencyCode: 'PHP', githubRepo: '', createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));

  // #12 Invalid enum
  await assertFails(txCol.doc('tx_enum').set({
    userId: ALICE_UID, merchant: 'Cafe', amountCents: 100, category: 'CRYPTO',
    paymentSource: 'CASH', note: '', occurredAtMillis: Date.now(), needsReview: false,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  }));
});
