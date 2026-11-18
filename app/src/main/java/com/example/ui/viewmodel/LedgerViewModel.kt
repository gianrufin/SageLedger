package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.LedgerAnalytics
import com.example.data.categorization.MerchantCategorizer
import com.example.data.categorization.PurchaseNotificationParser
import com.example.data.model.CategoryBudget
import com.example.data.model.GitHubReleaseNotice
import com.example.data.model.MerchantRule
import com.example.data.model.PaymentSource
import com.example.data.model.PeriodReportData
import com.example.data.model.RecurringPreset
import com.example.data.model.ReportPeriod
import com.example.data.model.SavingsProjection
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.data.model.ThemeModePreference
import com.example.data.model.UserFinanceSettings
import com.example.data.preferences.AppPreferencesStore
import com.example.data.remote.GitHubUpdateRepository
import com.example.data.repository.LedgerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DashboardSnapshot(
    val settings: UserFinanceSettings,
    val transactions: List<SpendingTransaction>,
    val monthTransactions: List<SpendingTransaction>,
    val monthSpentCents: Long,
    val budgets: List<CategoryBudget>,
    val recurringPresets: List<RecurringPreset>,
    val merchantRules: List<MerchantRule>,
    val savingsProjection: SavingsProjection,
    val unreviewedCount: Int
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val preferencesStore: AppPreferencesStore,
    private val categorizer: MerchantCategorizer,
    private val gitHubUpdateRepository: GitHubUpdateRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val _customStartMillis = MutableStateFlow<Long?>(null)
    val customStartMillis: StateFlow<Long?> = _customStartMillis.asStateFlow()

    private val _customEndMillis = MutableStateFlow<Long?>(null)
    val customEndMillis: StateFlow<Long?> = _customEndMillis.asStateFlow()

    private val _githubUpdateNotice = MutableStateFlow<GitHubReleaseNotice?>(null)
    val githubUpdateNotice: StateFlow<GitHubReleaseNotice?> = _githubUpdateNotice.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastAddedTransactionId = MutableStateFlow<String?>(null)
    val lastAddedTransactionId: StateFlow<String?> = _lastAddedTransactionId.asStateFlow()

    private var hasCheckedUpdateForRepo: String? = null

    val themeMode: StateFlow<ThemeModePreference> = preferencesStore.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ThemeModePreference.SYSTEM
        )

    private val transactionsFlow = repository.observeTransactions(currentUserId)
    private val budgetsFlow = repository.observeBudgets(currentUserId)
    private val presetsFlow = repository.observeRecurringPresets(currentUserId)
    private val rulesFlow = repository.observeMerchantRules(currentUserId)
    private val settingsFlow = repository.observeSettings(currentUserId)

    val dashboardState: StateFlow<UiState<DashboardSnapshot>> = combine(
        transactionsFlow,
        budgetsFlow,
        presetsFlow,
        rulesFlow,
        settingsFlow
    ) { transactions, budgets, presets, rules, settings ->
        val (monthStart, monthEnd) = LedgerAnalytics.currentMonthBounds()
        val monthTx = transactions.filter { it.occurredAtMillis in monthStart..monthEnd }
        val monthSpent = monthTx.sumOf { it.amountCents }
        val projection = LedgerAnalytics.computeSavingsProjection(
            transactions = transactions,
            monthlyIncomeCents = settings.monthlyIncomeCents,
            monthlyBudgetCents = settings.monthlyBudgetCents
        )
        val unreviewed = transactions.count { it.needsReview }
        DashboardSnapshot(
            settings = settings,
            transactions = transactions,
            monthTransactions = monthTx,
            monthSpentCents = monthSpent,
            budgets = budgets,
            recurringPresets = presets,
            merchantRules = rules,
            savingsProjection = projection,
            unreviewedCount = unreviewed
        )
    }
        .flowOn(Dispatchers.Default)
        .map<DashboardSnapshot, UiState<DashboardSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing ledger dashboard", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync your spending data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val reportState: StateFlow<UiState<PeriodReportData>> = combine(
        transactionsFlow,
        budgetsFlow,
        _selectedPeriod,
        _customStartMillis,
        _customEndMillis
    ) { transactions, budgets, period, customStart, customEnd ->
        LedgerAnalytics.computePeriodReport(
            allTransactions = transactions,
            budgets = budgets,
            period = period,
            customStartMillis = customStart,
            customEndMillis = customEnd
        )
    }
        .flowOn(Dispatchers.Default)
        .map<PeriodReportData, UiState<PeriodReportData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing period report", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to build report."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings(currentUserId)
        }
        viewModelScope.launch {
            settingsFlow.collect { settings ->
                val repo = settings.githubRepo.trim()
                if (repo.isNotEmpty() && repo != hasCheckedUpdateForRepo) {
                    hasCheckedUpdateForRepo = repo
                    checkGitHubUpdates(repo, showNoUpdateToast = false)
                }
            }
        }
    }

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customStartMillis.value = startMillis
        _customEndMillis.value = endMillis
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    fun setThemeMode(mode: ThemeModePreference) {
        viewModelScope.launch {
            preferencesStore.setThemeMode(mode)
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastAddedTransactionId() {
        _lastAddedTransactionId.value = null
    }

    fun dismissUpdateNotice(permanentlyForTag: Boolean) {
        val current = _githubUpdateNotice.value
        _githubUpdateNotice.value = null
        if (permanentlyForTag && current != null) {
            viewModelScope.launch {
                preferencesStore.dismissReleaseTag(current.tagName)
            }
        }
    }

    fun checkGitHubUpdates(repoSlug: String, showNoUpdateToast: Boolean = true) {
        if (repoSlug.isBlank()) {
            if (showNoUpdateToast) {
                _statusMessage.value = "Set a GitHub owner/repo in Settings first."
            }
            return
        }
        viewModelScope.launch {
            val result = gitHubUpdateRepository.checkForUpdate(repoSlug)
            result.onSuccess { notice ->
                if (notice != null) {
                    val dismissed = preferencesStore.dismissedReleaseTagFlow.first()
                    if (showNoUpdateToast || dismissed != notice.tagName) {
                        _githubUpdateNotice.value = notice
                    }
                } else if (showNoUpdateToast) {
                    _statusMessage.value = "You are on the latest release."
                }
            }.onFailure {
                if (showNoUpdateToast) {
                    _statusMessage.value = "Could not check GitHub releases right now."
                }
            }
        }
    }

    fun suggestCategoryForMerchant(
        merchant: String,
        rawSnippet: String?,
        rules: List<MerchantRule>,
        onResult: (SpendingCategory) -> Unit
    ) {
        val local = categorizer.categorizeLocal(merchant, rawSnippet, rules)
        if (local != null) {
            onResult(local)
            return
        }
        viewModelScope.launch {
            val res = categorizer.categorizeWithFallback(merchant, rawSnippet, rules)
            onResult(res.category)
        }
    }

    fun parseSnippetAndCategorize(
        snippet: String,
        rules: List<MerchantRule>,
        onParsed: (merchant: String, amountCents: Long, source: PaymentSource, category: SpendingCategory) -> Unit,
        onUnrecognized: () -> Unit
    ) {
        val parsed = PurchaseNotificationParser.parseRawText(snippet)
        if (parsed == null) {
            onUnrecognized()
            return
        }
        val local = categorizer.categorizeLocal(parsed.merchant, parsed.rawSnippet, rules)
        if (local != null) {
            onParsed(parsed.merchant, parsed.amountCents, parsed.paymentSource, local)
        } else {
            viewModelScope.launch {
                val res = categorizer.categorizeWithFallback(parsed.merchant, parsed.rawSnippet, rules)
                onParsed(parsed.merchant, parsed.amountCents, parsed.paymentSource, res.category)
            }
        }
    }

    fun saveTransaction(
        existingId: String?,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource,
        note: String,
        rawSnippet: String?,
        occurredAtMillis: Long,
        rememberMerchantRule: Boolean
    ) {
        viewModelScope.launch {
            val res: Result<Unit> = if (existingId.isNullOrBlank()) {
                repository.addTransaction(
                    userId = currentUserId,
                    merchant = merchant,
                    amountCents = amountCents,
                    category = category,
                    paymentSource = paymentSource,
                    note = note,
                    rawSnippet = rawSnippet,
                    occurredAtMillis = occurredAtMillis,
                    needsReview = false
                ).map { txId ->
                    _lastAddedTransactionId.value = txId
                    Unit
                }
            } else {
                repository.updateTransaction(
                    userId = currentUserId,
                    transactionId = existingId,
                    merchant = merchant,
                    amountCents = amountCents,
                    category = category,
                    paymentSource = paymentSource,
                    note = note,
                    occurredAtMillis = occurredAtMillis,
                    needsReview = false
                )
            }

            if (res.isSuccess && rememberMerchantRule && merchant.isNotBlank()) {
                repository.upsertMerchantRule(currentUserId, merchant.trim(), category)
            }

            res.onSuccess {
                _statusMessage.value = if (existingId.isNullOrBlank()) {
                    "Recorded $merchant"
                } else {
                    "Updated $merchant"
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save transaction."
            }
        }
    }

    fun quickAddPreset(preset: RecurringPreset) {
        viewModelScope.launch {
            val res = repository.addTransaction(
                userId = currentUserId,
                merchant = preset.merchant,
                amountCents = preset.amountCents,
                category = preset.categoryEnum,
                paymentSource = preset.paymentSourceEnum,
                note = "Quick-add: ${preset.label}",
                occurredAtMillis = System.currentTimeMillis(),
                needsReview = false
            )
            res.onSuccess { txId ->
                _lastAddedTransactionId.value = txId
                _statusMessage.value = "Added ${preset.label}"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to add preset expense."
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(currentUserId, transactionId)
                .onSuccess { _statusMessage.value = "Transaction removed" }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to delete transaction."
                }
        }
    }

    fun saveCategoryBudget(category: SpendingCategory, monthlyLimitCents: Long) {
        viewModelScope.launch {
            repository.upsertCategoryBudget(currentUserId, category, monthlyLimitCents)
                .onSuccess { _statusMessage.value = "Saved ${category.shortLabel} budget" }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to update budget."
                }
        }
    }

    fun addRecurringPreset(
        label: String,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        paymentSource: PaymentSource
    ) {
        viewModelScope.launch {
            repository.addRecurringPreset(
                userId = currentUserId,
                label = label,
                merchant = merchant,
                amountCents = amountCents,
                category = category,
                paymentSource = paymentSource
            ).onSuccess {
                _statusMessage.value = "Saved quick-add preset"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save preset."
            }
        }
    }

    fun deleteRecurringPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteRecurringPreset(currentUserId, presetId)
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to remove preset."
                }
        }
    }

    fun saveMerchantRule(keyword: String, category: SpendingCategory) {
        viewModelScope.launch {
            repository.upsertMerchantRule(currentUserId, keyword, category)
                .onSuccess { _statusMessage.value = "Saved merchant rule" }
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to save rule."
                }
        }
    }

    fun deleteMerchantRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteMerchantRule(currentUserId, ruleId)
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to remove rule."
                }
        }
    }

    fun saveFinanceSettings(
        monthlyIncomeCents: Long,
        monthlyBudgetCents: Long,
        currencyCode: String,
        githubRepo: String
    ) {
        viewModelScope.launch {
            repository.saveSettings(
                userId = currentUserId,
                monthlyIncomeCents = monthlyIncomeCents,
                monthlyBudgetCents = monthlyBudgetCents,
                currencyCode = currencyCode,
                githubRepo = githubRepo
            ).onSuccess {
                _statusMessage.value = "Preferences saved"
                if (githubRepo.isNotBlank() && githubRepo.trim() != hasCheckedUpdateForRepo) {
                    hasCheckedUpdateForRepo = githubRepo.trim()
                    checkGitHubUpdates(githubRepo.trim(), showNoUpdateToast = false)
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save preferences."
            }
        }
    }

    private companion object {
        const val TAG = "LedgerVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
