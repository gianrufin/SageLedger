package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CatConvenienceColor
import com.example.ui.theme.CatFoodColor
import com.example.ui.theme.CatGadgetsColor
import com.example.ui.theme.CatGroceriesColor
import com.example.ui.theme.CatOtherColor
import com.example.ui.theme.CatShoppingColor
import com.example.ui.theme.CatTransportColor
import com.example.ui.theme.CatUtilitiesColor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SpendingCategory(
    val displayName: String,
    val shortLabel: String,
    val chartColor: Color,
    val defaultBudgetCents: Long
) {
    FOOD("Food & Dining", "Food", CatFoodColor, 45_000L),
    GROCERIES("Groceries", "Groceries", CatGroceriesColor, 50_000L),
    TRANSPORT_FUEL("Fuel, Transpo & Tolls", "Transpo", CatTransportColor, 30_000L),
    CONVENIENCE("Convenience Stores", "Convenience", CatConvenienceColor, 15_000L),
    GADGETS("Gadgets & Tech", "Gadgets", CatGadgetsColor, 25_000L),
    UTILITIES("Bills & Utilities", "Bills", CatUtilitiesColor, 35_000L),
    SHOPPING("Shopping", "Shopping", CatShoppingColor, 25_000L),
    OTHER("Other", "Other", CatOtherColor, 15_000L);

    companion object {
        fun fromRaw(raw: String?): SpendingCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
    }
}

enum class PaymentSource(val displayName: String, val shortLabel: String) {
    GOOGLE_PAY("Google Pay", "GPay"),
    E_WALLET("E-Wallet", "E-Wallet"),
    CREDIT_CARD("Credit Card", "Card"),
    SMS_ALERT("SMS Alert", "SMS"),
    CASH("Cash", "Cash"),
    MANUAL("Manual Entry", "Manual");

    companion object {
        fun fromRaw(raw: String?): PaymentSource =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MANUAL
    }
}

enum class ThemeModePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromRaw(raw: String?): ThemeModePreference =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class ReportPeriod(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    CUSTOM("Period")
}

@Keep
@IgnoreExtraProperties
data class SpendingTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val merchant: String = "",
    val amountCents: Long = 0L,
    val category: String = SpendingCategory.OTHER.name,
    val paymentSource: String = PaymentSource.MANUAL.name,
    val note: String = "",
    val rawSnippet: String? = null,
    val occurredAtMillis: Long = 0L,
    val needsReview: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: SpendingCategory
        get() = SpendingCategory.fromRaw(category)

    val paymentSourceEnum: PaymentSource
        get() = PaymentSource.fromRaw(paymentSource)

    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val base = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "merchant" to merchant.trim().take(120),
            "amountCents" to amountCents,
            "category" to SpendingCategory.fromRaw(category).name,
            "paymentSource" to PaymentSource.fromRaw(paymentSource).name,
            "note" to note.trim().take(300),
            "rawSnippet" to rawSnippet?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "occurredAtMillis" to occurredAtMillis,
            "needsReview" to needsReview,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return base.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = SpendingCategory.OTHER.name,
    val monthlyLimitCents: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: SpendingCategory
        get() = SpendingCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class RecurringPreset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val merchant: String = "",
    val amountCents: Long = 0L,
    val category: String = SpendingCategory.OTHER.name,
    val paymentSource: String = PaymentSource.GOOGLE_PAY.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: SpendingCategory
        get() = SpendingCategory.fromRaw(category)

    val paymentSourceEnum: PaymentSource
        get() = PaymentSource.fromRaw(paymentSource)
}

@Keep
@IgnoreExtraProperties
data class MerchantRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val merchantKeyword: String = "",
    val category: String = SpendingCategory.OTHER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: SpendingCategory
        get() = SpendingCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class UserFinanceSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthlyIncomeCents: Long = 400_000L,
    val monthlyBudgetCents: Long = 240_000L,
    val currencyCode: String = "PHP",
    val githubRepo: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SavingsProjection(
    val daysInMonth: Int,
    val currentDayOfMonth: Int,
    val spentToDateCents: Long,
    val projectedMonthEndSpendCents: Long,
    val monthlyIncomeCents: Long,
    val monthlyBudgetCents: Long,
    val projectedSavingsCents: Long,
    val actualCumulativeDailyCents: List<Long>,
    val projectedDailyCents: List<Long>,
    val remainingDaysInMonth: Int = (daysInMonth - currentDayOfMonth + 1).coerceAtLeast(1),
    val dailyAllowanceCents: Long = 0L,
    val noSpendDaysThisMonth: Int = 0
)

data class TrendBucket(
    val label: String,
    val amountCents: Long
)

data class CategoryBreakdownItem(
    val category: SpendingCategory,
    val spentCents: Long,
    val budgetCents: Long,
    val shareFraction: Float,
    val transactionCount: Int
)

data class MerchantTotalItem(
    val merchant: String,
    val totalCents: Long,
    val count: Int,
    val primaryCategory: SpendingCategory
)

data class PaymentSourceBreakdown(
    val source: PaymentSource,
    val totalCents: Long,
    val shareFraction: Float
)

data class PeriodReportData(
    val periodLabel: String,
    val totalSpentCents: Long,
    val transactionCount: Int,
    val averagePerTransactionCents: Long,
    val trendBuckets: List<TrendBucket>,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val topMerchants: List<MerchantTotalItem>,
    val paymentSources: List<PaymentSourceBreakdown>
)

data class GitHubReleaseNotice(
    val tagName: String,
    val releaseName: String,
    val notes: String,
    val htmlUrl: String
)
