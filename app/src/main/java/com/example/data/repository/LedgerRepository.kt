package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CategoryBudget
import com.example.data.model.MerchantRule
import com.example.data.model.PaymentSource
import com.example.data.model.RecurringPreset
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.data.model.UserFinanceSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class LedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireActiveUid(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user access." }
        }
        return uid
    }

    fun observeTransactions(userId: String): Flow<List<SpendingTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .orderBy("occurredAtMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SpendingTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeBudgets(userId: String): Flow<List<CategoryBudget>> {
        val path = "users/$userId/budgets"
        return db.collection("users").document(userId).collection("budgets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CategoryBudget::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeRecurringPresets(userId: String): Flow<List<RecurringPreset>> {
        val path = "users/$userId/recurringPresets"
        return db.collection("users").document(userId).collection("recurringPresets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RecurringPreset::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.label.lowercase(Locale.US) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeMerchantRules(userId: String): Flow<List<MerchantRule>> {
        val path = "users/$userId/merchantRules"
        return db.collection("users").document(userId).collection("merchantRules")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MerchantRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.merchantKeyword }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeSettings(userId: String): Flow<UserFinanceSettings> {
        val path = "users/$userId/settings/preferences"
        return db.collection("users").document(userId)
            .collection("settings").document("preferences")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserFinanceSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserFinanceSettings(userId = userId)
                } else {
                    UserFinanceSettings(userId = userId)
                }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, path, userId)
                throw error
            }
    }

    suspend fun getMerchantRulesOnce(userId: String): List<MerchantRule> {
        val path = "users/$userId/merchantRules"
        return try {
            requireActiveUid(userId)
            db.collection("users").document(userId).collection("merchantRules")
                .get()
                .await()
                .toObjects(
                    MerchantRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun getCurrencyOnce(userId: String): String {
        val path = "users/$userId/settings/preferences"
        return try {
            requireActiveUid(userId)
            val snap = db.collection("users").document(userId)
                .collection("settings").document("preferences")
                .get()
                .await()
            snap.getString("currencyCode") ?: "PHP"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            "PHP"
        }
    }

    suspend fun ensureDefaultSettings(userId: String): Result<Unit> {
        val path = "users/$userId/settings/preferences"
        return try {
            val uid = requireActiveUid(userId)
            val ref = db.collection("users").document(uid)
                .collection("settings").document("preferences")
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "monthlyIncomeCents" to 400_000L,
                    "monthlyBudgetCents" to 240_000L,
                    "currencyCode" to "PHP",
                    "githubRepo" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(
        userId: String,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource,
        note: String = "",
        rawSnippet: String? = null,
        occurredAtMillis: Long = System.currentTimeMillis(),
        needsReview: Boolean = false
    ): Result<String> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireActiveUid(userId)
            val cleanMerchant = merchant.trim().take(120)
            require(cleanMerchant.isNotEmpty()) { "Merchant name is required." }
            require(amountCents in 1L..1_000_000_000L) { "Amount must be greater than zero." }
            require(occurredAtMillis > 0L) { "Invalid transaction timestamp." }

            val docRef = db.collection("users").document(uid)
                .collection("transactions").document()
            docPath = docRef.path

            val model = SpendingTransaction(
                userId = uid,
                merchant = cleanMerchant,
                amountCents = amountCents,
                category = category.name,
                paymentSource = paymentSource.name,
                note = note.trim().take(300),
                rawSnippet = rawSnippet?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                occurredAtMillis = occurredAtMillis,
                needsReview = needsReview
            )
            docRef.set(model.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        userId: String,
        transactionId: String,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource,
        note: String,
        occurredAtMillis: Long,
        needsReview: Boolean = false
    ): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireActiveUid(userId)
            require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
            val cleanMerchant = merchant.trim().take(120)
            require(cleanMerchant.isNotEmpty()) { "Merchant name is required." }
            require(amountCents in 1L..1_000_000_000L) { "Amount must be greater than zero." }
            require(occurredAtMillis > 0L) { "Invalid transaction timestamp." }

            val updates = mapOf(
                "merchant" to cleanMerchant,
                "amountCents" to amountCents,
                "category" to category.name,
                "paymentSource" to paymentSource.name,
                "note" to note.trim().take(300),
                "occurredAtMillis" to occurredAtMillis,
                "needsReview" to needsReview,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireActiveUid(userId)
            require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
            db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertCategoryBudget(
        userId: String,
        category: SpendingCategory,
        monthlyLimitCents: Long
    ): Result<Unit> {
        val docPath = "users/$userId/budgets/${category.name}"
        return try {
            val uid = requireActiveUid(userId)
            require(monthlyLimitCents in 0L..1_000_000_000L) { "Budget limit out of bounds." }
            val ref = db.collection("users").document(uid)
                .collection("budgets").document(category.name)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "monthlyLimitCents" to monthlyLimitCents,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "category" to category.name,
                        "monthlyLimitCents" to monthlyLimitCents,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addRecurringPreset(
        userId: String,
        label: String,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource
    ): Result<Unit> {
        var docPath = "users/$userId/recurringPresets"
        return try {
            val uid = requireActiveUid(userId)
            val cleanLabel = label.trim().take(40)
            val cleanMerchant = merchant.trim().take(120)
            require(cleanLabel.isNotEmpty()) { "Preset label is required." }
            require(cleanMerchant.isNotEmpty()) { "Merchant name is required." }
            require(amountCents in 1L..1_000_000_000L) { "Amount must be greater than zero." }

            val docRef = db.collection("users").document(uid)
                .collection("recurringPresets").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "label" to cleanLabel,
                    "merchant" to cleanMerchant,
                    "amountCents" to amountCents,
                    "category" to category.name,
                    "paymentSource" to paymentSource.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringPreset(userId: String, presetId: String): Result<Unit> {
        val docPath = "users/$userId/recurringPresets/$presetId"
        return try {
            val uid = requireActiveUid(userId)
            require(presetId.isNotBlank())
            db.collection("users").document(uid)
                .collection("recurringPresets").document(presetId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertMerchantRule(
        userId: String,
        merchantKeyword: String,
        category: SpendingCategory
    ): Result<Unit> {
        val normalized = merchantKeyword.trim().lowercase(Locale.US).take(100)
        val docId = normalized.replace(Regex("[^a-z0-9_\\-]"), "_").take(120).ifBlank { "rule" }
        val docPath = "users/$userId/merchantRules/$docId"
        return try {
            val uid = requireActiveUid(userId)
            require(normalized.isNotEmpty()) { "Keyword cannot be empty." }
            val ref = db.collection("users").document(uid)
                .collection("merchantRules").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "merchantKeyword" to normalized,
                        "category" to category.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "merchantKeyword" to normalized,
                        "category" to category.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMerchantRule(userId: String, ruleId: String): Result<Unit> {
        val docPath = "users/$userId/merchantRules/$ruleId"
        return try {
            val uid = requireActiveUid(userId)
            require(ruleId.isNotBlank())
            db.collection("users").document(uid)
                .collection("merchantRules").document(ruleId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveSettings(
        userId: String,
        monthlyIncomeCents: Long,
        monthlyBudgetCents: Long,
        currencyCode: String,
        githubRepo: String
    ): Result<Unit> {
        val docPath = "users/$userId/settings/preferences"
        return try {
            val uid = requireActiveUid(userId)
            val safeIncome = monthlyIncomeCents.coerceIn(0L, 10_000_000_000L)
            val safeBudget = monthlyBudgetCents.coerceIn(0L, 10_000_000_000L)
            val safeCurrency = if (currencyCode in listOf("PHP", "USD", "EUR", "GBP", "JPY", "SGD", "AUD", "CAD")) {
                currencyCode
            } else {
                "PHP"
            }
            val safeRepo = githubRepo.trim().take(120)

            val ref = db.collection("users").document(uid)
                .collection("settings").document("preferences")
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "monthlyIncomeCents" to safeIncome,
                        "monthlyBudgetCents" to safeBudget,
                        "currencyCode" to safeCurrency,
                        "githubRepo" to safeRepo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "monthlyIncomeCents" to safeIncome,
                        "monthlyBudgetCents" to safeBudget,
                        "currencyCode" to safeCurrency,
                        "githubRepo" to safeRepo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        userId: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
