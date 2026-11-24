# SageLedger Security Specification

## 1. Data Invariants
1. Every document under `/users/{userId}/...` belongs strictly to `{userId}` (`data.userId == userId == request.auth.uid`).
2. All write operations require an authenticated, email-verified user (`request.auth != null && request.auth.token.email_verified == true`).
3. Every document ID (`{transactionId}`, `{categoryId}`, `{presetId}`, `{ruleId}`) must satisfy `^[a-zA-Z0-9_\-]+$` with length 1–128; `settings` documents are restricted strictly to `settingId == 'preferences'`.
4. `createdAt` and `userId` (and `category` on `CategoryBudget`) are immutable after creation; `createdAt` must equal `request.time` on create, and `updatedAt` must equal `request.time` on create and update.
5. Numeric amounts (`amountCents > 0 && amountCents <= 1000000000`, `monthlyLimitCents >= 0 && monthlyLimitCents <= 1000000000`, `monthlyIncomeCents >= 0 && monthlyIncomeCents <= 10000000000`) and string lengths (`merchant <= 120`, `note <= 300`, `rawSnippet <= 500`, `label <= 40`, `merchantKeyword <= 100`, `githubRepo <= 120`) are strictly bounded.
6. No shadow/ghost fields are accepted on create (`hasAll` + `hasOnly`) or update (`affectedKeys().hasOnly(...)`).
7. List queries must enforce `resource.data.userId == request.auth.uid` on the server rules side.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated read/write**: Unauthenticated client reading or writing `/users/{uid}/transactions`.
2. **Unverified email write**: Authenticated user with `email_verified: false` creating a transaction.
3. **Cross-user read/list**: Bob (`bob_uid`) listing `/users/alice_uid/transactions`.
4. **Cross-user write/update**: Bob modifying Alice's `/users/alice_uid/budgets/FOOD`.
5. **Identity spoofing**: Alice writing a transaction under `/users/alice_uid/transactions/tx1` with `userId: "bob_uid"`.
6. **Shadow/ghost field on create**: Alice creating a transaction with extra `{ isAdmin: true }`.
7. **Shadow/ghost field on update**: Alice updating a transaction with `{ role: "admin" }`.
8. **Immutable field mutation**: Alice updating `createdAt` or `userId` on an existing transaction.
9. **Temporal tampering**: Alice submitting a backdated/future `createdAt` or `updatedAt` instead of `request.time`.
10. **Value/type poisoning**: Alice creating a transaction with `amountCents: -500`, `amountCents: "100"`, or a 200-char `merchant`.
11. **Path ID poisoning**: Alice creating a transaction with ID containing invalid chars or >128 chars, or creating a settings doc with ID `evil` instead of `preferences`.
12. **Category/Source enum bypass**: Alice creating a transaction with `category: "CRYPTO"` or `paymentSource: "BITCOIN"`.
