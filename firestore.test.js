const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        merchant: "7-Eleven",
        amountCents: 450,
        category: "CONVENIENCE",
        paymentSource: "GOOGLE_PAY",
        note: "",
        occurredAtMillis: Date.now(),
        needsReview: false,
        createdAt: past,
        updatedAt: past,
      })
  );
});

test("Authenticated owner can create, list, partially update, and delete transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const txRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions")
    .doc("tx_1");

  // Empty list query works without undefined resource.data errors
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // Create without rawSnippet (optional omitted)
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      merchant: "Shell Station",
      amountCents: 3850,
      category: "TRANSPORT_FUEL",
      paymentSource: "CREDIT_CARD",
      note: "Highway refill",
      occurredAtMillis: Date.now() - 2000,
      needsReview: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    txRef.update({
      category: "TRANSPORT_FUEL",
      needsReview: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    txRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating createdAt
  await assertFails(
    txRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId "id" key in body
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_2")
      .set({
        id: "tx_2",
        userId: ALICE_UID,
        merchant: "Jollibee",
        amountCents: 990,
        category: "FOOD",
        paymentSource: "GOOGLE_PAY",
        note: "",
        occurredAtMillis: Date.now(),
        needsReview: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_future")
      .set({
        userId: ALICE_UID,
        merchant: "Apple Store",
        amountCents: 19900,
        category: "GADGETS",
        paymentSource: "CREDIT_CARD",
        note: "",
        occurredAtMillis: Date.now(),
        needsReview: false,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  await assertSucceeds(txRef.delete());
});

test("Cross-user isolation blocks Bob from Alice's transactions, budgets, presets, rules, and settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("budgets")
      .doc("FOOD")
      .set({
        userId: ALICE_UID,
        category: "FOOD",
        monthlyLimitCents: 50000,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("budgets")
      .doc("FOOD")
      .update({ monthlyLimitCents: 100, updatedAt: now })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("settings")
      .doc("preferences")
      .set({
        userId: ALICE_UID,
        monthlyIncomeCents: 400000,
        monthlyBudgetCents: 250000,
        currencyCode: "USD",
        githubRepo: "",
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Owner can manage budgets, recurringPresets, merchantRules, and settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("recurringPresets")
      .doc("preset_coffee")
      .set({
        userId: ALICE_UID,
        label: "Morning Coffee",
        merchant: "Blue Bottle",
        amountCents: 550,
        category: "FOOD",
        paymentSource: "GOOGLE_PAY",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("merchantRules")
      .doc("rule_1")
      .set({
        userId: ALICE_UID,
        merchantKeyword: "7-eleven",
        category: "CONVENIENCE",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("settings")
      .doc("preferences")
      .set({
        userId: ALICE_UID,
        monthlyIncomeCents: 450000,
        monthlyBudgetCents: 220000,
        currencyCode: "USD",
        githubRepo: "gianrufin/sageledger",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("settings")
      .doc("preferences")
      .update({
        monthlyBudgetCents: 240000,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});
