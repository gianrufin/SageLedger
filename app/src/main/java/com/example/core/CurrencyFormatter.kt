package com.example.core

import com.example.data.model.CategoryBudget
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.MerchantTotalItem
import com.example.data.model.PaymentSource
import com.example.data.model.PaymentSourceBreakdown
import com.example.data.model.PeriodReportData
import com.example.data.model.ReportPeriod
import com.example.data.model.SavingsProjection
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.data.model.TrendBucket
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyFormatter {
    val supportedCurrencies = listOf("USD", "PHP", "EUR", "GBP", "JPY", "SGD", "AUD", "CAD")

    fun symbolFor(currencyCode: String): String {
        return when (currencyCode.uppercase(Locale.US)) {
            "USD" -> "$"
            "PHP" -> "₱"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "SGD" -> "S$"
            "AUD" -> "A$"
            "CAD" -> "C$"
            else -> "$"
        }
    }

    fun formatCents(amountCents: Long, currencyCode: String = "USD", showPlusSign: Boolean = false): String {
        val code = currencyCode.uppercase(Locale.US)
        val isZeroDecimal = code == "JPY"
        val major = if (isZeroDecimal) amountCents.toDouble() / 100.0 else amountCents.toDouble() / 100.0
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            runCatching { currency = Currency.getInstance(code) }
            maximumFractionDigits = if (isZeroDecimal) 0 else 2
            minimumFractionDigits = if (isZeroDecimal) 0 else 2
        }
        val raw = formatter.format(kotlin.math.abs(major))
        val symbolReplaced = if (code == "PHP") raw.replace("PHP", "₱").replace("Php", "₱") else raw
        return when {
            amountCents < 0 -> "-$symbolReplaced"
            showPlusSign && amountCents > 0 -> "+$symbolReplaced"
            else -> symbolReplaced
        }
    }

    fun formatCompactCents(amountCents: Long, currencyCode: String = "USD"): String {
        val sym = symbolFor(currencyCode)
        val absMajor = kotlin.math.abs(amountCents) / 100.0
        val prefix = if (amountCents < 0) "-" else ""
        return when {
            absMajor >= 1000.0 -> String.format(Locale.US, "%s%s%.1fk", prefix, sym, absMajor / 1000.0)
            absMajor >= 100.0 -> String.format(Locale.US, "%s%s%.0f", prefix, sym, absMajor)
            else -> String.format(Locale.US, "%s%s%.0f", prefix, sym, absMajor)
        }
    }
}

object LedgerAnalytics {

    fun formatDateTime(millis: Long): String {
        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun currentMonthBounds(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1L
        return start to end
    }

    fun computePeriodBounds(
        period: ReportPeriod,
        customStartMillis: Long?,
        customEndMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (period) {
            ReportPeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                start to (cal.timeInMillis - 1L)
            }
            ReportPeriod.WEEK -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                start to (cal.timeInMillis - 1L)
            }
            ReportPeriod.MONTH -> currentMonthBounds(nowMillis)
            ReportPeriod.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                start to (cal.timeInMillis - 1L)
            }
            ReportPeriod.CUSTOM -> {
                val fallback = currentMonthBounds(nowMillis)
                val s = normalizeDayStart(customStartMillis ?: fallback.first)
                val e = normalizeDayEnd(customEndMillis ?: nowMillis)
                if (s <= e) s to e else e to s
            }
        }
    }

    private fun normalizeDayStart(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun normalizeDayEnd(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    fun computeSavingsProjection(
        transactions: List<SpendingTransaction>,
        monthlyIncomeCents: Long,
        monthlyBudgetCents: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): SavingsProjection {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(28)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
        val (monthStart, monthEnd) = currentMonthBounds(nowMillis)

        val monthTx = transactions.filter { it.occurredAtMillis in monthStart..monthEnd }
        val dailySpend = LongArray(daysInMonth) { 0L }
        val txCal = Calendar.getInstance()
        for (tx in monthTx) {
            txCal.timeInMillis = tx.occurredAtMillis
            val dayIdx = (txCal.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, daysInMonth - 1)
            dailySpend[dayIdx] += tx.amountCents
        }

        val actualCumulative = mutableListOf<Long>()
        var running = 0L
        for (d in 0 until currentDay) {
            running += dailySpend[d]
            actualCumulative.add(running)
        }

        val spentToDate = running
        val avgDailyBurn = if (currentDay > 0) spentToDate.toDouble() / currentDay.toDouble() else 0.0
        val projectedEnd = (spentToDate + avgDailyBurn * (daysInMonth - currentDay)).roundToLong()

        val projectedCurve = mutableListOf<Long>()
        for (d in 1..daysInMonth) {
            val value = if (d <= currentDay) {
                actualCumulative[d - 1]
            } else {
                (spentToDate + avgDailyBurn * (d - currentDay)).roundToLong()
            }
            projectedCurve.add(value)
        }

        val savingsBase = if (monthlyIncomeCents > 0L) monthlyIncomeCents else monthlyBudgetCents
        val projectedSavings = savingsBase - projectedEnd

        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
        val remainingBudget = monthlyBudgetCents - spentToDate
        val dailyAllowance = if (remainingBudget > 0L) remainingBudget / remainingDays else 0L
        val noSpendDays = (0 until currentDay).count { dailySpend[it] == 0L }

        return SavingsProjection(
            daysInMonth = daysInMonth,
            currentDayOfMonth = currentDay,
            spentToDateCents = spentToDate,
            projectedMonthEndSpendCents = projectedEnd,
            monthlyIncomeCents = monthlyIncomeCents,
            monthlyBudgetCents = monthlyBudgetCents,
            projectedSavingsCents = projectedSavings,
            actualCumulativeDailyCents = actualCumulative,
            projectedDailyCents = projectedCurve,
            remainingDaysInMonth = remainingDays,
            dailyAllowanceCents = dailyAllowance,
            noSpendDaysThisMonth = noSpendDays
        )
    }

    fun computePeriodReport(
        allTransactions: List<SpendingTransaction>,
        budgets: List<CategoryBudget>,
        period: ReportPeriod,
        customStartMillis: Long?,
        customEndMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): PeriodReportData {
        val (startMillis, endMillis) = computePeriodBounds(period, customStartMillis, customEndMillis, nowMillis)
        val filtered = allTransactions
            .filter { it.occurredAtMillis in startMillis..endMillis }
            .sortedByDescending { it.occurredAtMillis }

        val totalSpent = filtered.sumOf { it.amountCents }
        val count = filtered.size
        val avgPerTx = if (count > 0) totalSpent / count else 0L

        val trendBuckets = buildTrendBuckets(filtered, period, startMillis, endMillis)

        val budgetMap = budgets.associateBy { it.categoryEnum }
        val byCategory = filtered.groupBy { it.categoryEnum }
        val categoryBreakdown = SpendingCategory.entries
            .map { cat ->
                val list = byCategory[cat].orEmpty()
                val spent = list.sumOf { it.amountCents }
                val limit = budgetMap[cat]?.monthlyLimitCents ?: cat.defaultBudgetCents
                val share = if (totalSpent > 0L) spent.toFloat() / totalSpent.toFloat() else 0f
                CategoryBreakdownItem(
                    category = cat,
                    spentCents = spent,
                    budgetCents = limit,
                    shareFraction = share,
                    transactionCount = list.size
                )
            }
            .filter { it.spentCents > 0L || period == ReportPeriod.MONTH }
            .sortedByDescending { it.spentCents }

        val topMerchants = filtered
            .groupBy { it.merchant.trim() }
            .map { (merchant, list) ->
                val dominantCat = list.groupBy { it.categoryEnum }
                    .maxByOrNull { it.value.size }?.key ?: SpendingCategory.OTHER
                MerchantTotalItem(
                    merchant = merchant.ifBlank { "Unknown Store" },
                    totalCents = list.sumOf { it.amountCents },
                    count = list.size,
                    primaryCategory = dominantCat
                )
            }
            .sortedByDescending { it.totalCents }
            .take(5)

        val paymentSources = filtered
            .groupBy { it.paymentSourceEnum }
            .map { (src, list) ->
                val amt = list.sumOf { it.amountCents }
                PaymentSourceBreakdown(
                    source = src,
                    totalCents = amt,
                    shareFraction = if (totalSpent > 0L) amt.toFloat() / totalSpent.toFloat() else 0f
                )
            }
            .sortedByDescending { it.totalCents }

        val periodLabel = when (period) {
            ReportPeriod.DAY -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(startMillis))
            ReportPeriod.WEEK -> "This Week (${formatShortDate(startMillis)} – ${formatShortDate(endMillis)})"
            ReportPeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startMillis))
            ReportPeriod.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(startMillis))
            ReportPeriod.CUSTOM -> "${formatShortDate(startMillis)} – ${formatShortDate(endMillis)}"
        }

        return PeriodReportData(
            periodLabel = periodLabel,
            totalSpentCents = totalSpent,
            transactionCount = count,
            averagePerTransactionCents = avgPerTx,
            trendBuckets = trendBuckets,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            paymentSources = paymentSources
        )
    }

    private fun buildTrendBuckets(
        filtered: List<SpendingTransaction>,
        period: ReportPeriod,
        startMillis: Long,
        endMillis: Long
    ): List<TrendBucket> {
        val cal = Calendar.getInstance()
        return when (period) {
            ReportPeriod.DAY -> {
                val labels = listOf("12a", "4a", "8a", "12p", "4p", "8p")
                val sums = LongArray(6)
                for (tx in filtered) {
                    cal.timeInMillis = tx.occurredAtMillis
                    val idx = (cal.get(Calendar.HOUR_OF_DAY) / 4).coerceIn(0, 5)
                    sums[idx] += tx.amountCents
                }
                labels.mapIndexed { i, label -> TrendBucket(label, sums[i]) }
            }
            ReportPeriod.WEEK -> {
                val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                val buckets = mutableListOf<TrendBucket>()
                val cursor = Calendar.getInstance().apply { timeInMillis = startMillis }
                repeat(7) {
                    val dayStart = cursor.timeInMillis
                    cursor.add(Calendar.DAY_OF_MONTH, 1)
                    val dayEnd = cursor.timeInMillis - 1L
                    val amt = filtered.filter { it.occurredAtMillis in dayStart..dayEnd }.sumOf { it.amountCents }
                    buckets.add(TrendBucket(dayFmt.format(Date(dayStart)).take(2), amt))
                }
                buckets
            }
            ReportPeriod.MONTH -> {
                val labels = listOf("W1", "W2", "W3", "W4", "W5")
                val sums = LongArray(5)
                for (tx in filtered) {
                    cal.timeInMillis = tx.occurredAtMillis
                    val idx = ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceIn(0, 4)
                    sums[idx] += tx.amountCents
                }
                labels.mapIndexed { i, label -> TrendBucket(label, sums[i]) }
            }
            ReportPeriod.YEAR -> {
                val labels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val sums = LongArray(12)
                for (tx in filtered) {
                    cal.timeInMillis = tx.occurredAtMillis
                    val idx = cal.get(Calendar.MONTH).coerceIn(0, 11)
                    sums[idx] += tx.amountCents
                }
                labels.mapIndexed { i, label -> TrendBucket(label, sums[i]) }
            }
            ReportPeriod.CUSTOM -> {
                val spanMillis = (endMillis - startMillis).coerceAtLeast(1L)
                val bucketCount = 6
                val step = spanMillis / bucketCount
                val fmt = SimpleDateFormat("M/d", Locale.getDefault())
                (0 until bucketCount).map { i ->
                    val bStart = startMillis + step * i
                    val bEnd = if (i == bucketCount - 1) endMillis else (startMillis + step * (i + 1) - 1L)
                    val amt = filtered.filter { it.occurredAtMillis in bStart..bEnd }.sumOf { it.amountCents }
                    TrendBucket(fmt.format(Date(bStart)), amt)
                }
            }
        }
    }
}
