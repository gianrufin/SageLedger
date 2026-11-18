package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.InsertChartOutlined
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyFormatter
import com.example.data.model.CategoryBreakdownItem
import com.example.data.model.ReportPeriod
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CrispCard
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.icon
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

private enum class ReportChartStyle {
    DONUT,
    BAR
}

private enum class ReportSectionTab(val label: String) {
    EXPENSES("Expenses"),
    INSIGHTS("Channels & Top")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: UiState<com.example.data.model.PeriodReportData>,
    selectedPeriod: ReportPeriod,
    currencyCode: String,
    monthlyBudgetCents: Long = 0L,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onSelectCustomRange: (startMillis: Long, endMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var chartStyle by remember { mutableStateOf(ReportChartStyle.DONUT) }
    var activeTab by remember { mutableStateOf(ReportSectionTab.EXPENSES) }
    var selectedCategoryIdx by remember { mutableStateOf<Int?>(null) }

    if (showDateRangeDialog) {
        val rangeState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = rangeState.selectedStartDateMillis
                        val e = rangeState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            onSelectCustomRange(s, e)
                        }
                        showDateRangeDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeDialog = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = rangeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
        ) {
            // Mockup-style top bar: "Report" + active period pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Report",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val periodTitle = (state as? UiState.Success)?.data?.periodLabel ?: selectedPeriod.label
                CrispCard(
                    onClick = { showDateRangeDialog = true },
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Choose period",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = periodTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Segmented capsule switcher ("Expenses" vs "Channels & Top")
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReportSectionTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Surface(
                            onClick = { activeTab = tab },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Period filter chips row (Day / Week / Month / Year / Period)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { period ->
                    val tag = "filter_${period.name.lowercase()}"
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            selectedCategoryIdx = null
                            if (period == ReportPeriod.CUSTOM) {
                                showDateRangeDialog = true
                            } else {
                                onSelectPeriod(period)
                            }
                        },
                        label = { Text(period.label) },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            when (state) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val report = state.data
                    val nonZeroCategories = remember(report.categoryBreakdown) {
                        report.categoryBreakdown.filter { it.spentCents > 0L }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (activeTab == ReportSectionTab.EXPENSES) {
                            item(key = "expenses_chart_card") {
                                CrispCard(
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Expenses Report",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${report.transactionCount} purchases • Avg ${CurrencyFormatter.formatCompactCents(report.averagePerTransactionCents, currencyCode)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Mockup-style Bar | Donut toggle pill
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.surfaceContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Surface(
                                                        onClick = { chartStyle = ReportChartStyle.BAR },
                                                        shape = CircleShape,
                                                        color = if (chartStyle == ReportChartStyle.BAR) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.BarChart,
                                                                contentDescription = "Bar chart view",
                                                                tint = if (chartStyle == ReportChartStyle.BAR) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                                },
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        onClick = { chartStyle = ReportChartStyle.DONUT },
                                                        shape = CircleShape,
                                                        color = if (chartStyle == ReportChartStyle.DONUT) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.DonutLarge,
                                                                contentDescription = "Donut chart view",
                                                                tint = if (chartStyle == ReportChartStyle.DONUT) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                                },
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (report.totalSpentCents == 0L) {
                                            EmptyChartPlaceholder()
                                        } else {
                                            AnimatedContent(
                                                targetState = chartStyle,
                                                label = "reportChartMode"
                                            ) { mode ->
                                                when (mode) {
                                                    ReportChartStyle.DONUT -> {
                                                        CategoryDonutChart(
                                                            items = nonZeroCategories,
                                                            totalSpentCents = report.totalSpentCents,
                                                            currencyCode = currencyCode,
                                                            budgetCapCents = if (selectedPeriod == ReportPeriod.MONTH && monthlyBudgetCents > 0L) {
                                                                monthlyBudgetCents
                                                            } else {
                                                                null
                                                            },
                                                            selectedCategoryIndex = selectedCategoryIdx,
                                                            onSelectCategoryIndex = { selectedCategoryIdx = it }
                                                        )
                                                    }
                                                    ReportChartStyle.BAR -> {
                                                        SpendingTrendBarChart(
                                                            buckets = report.trendBuckets,
                                                            currencyCode = currencyCode
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "all_expenses_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "All Expenses",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Total",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        SplitCurrencyText(
                                            amountCents = report.totalSpentCents,
                                            currencyCode = currencyCode,
                                            style = TabularAmountStyle.copy(fontSize = 15.sp),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }

                            if (report.categoryBreakdown.isEmpty()) {
                                item(key = "empty_categories") {
                                    CrispCard(modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.padding(18.dp)) {
                                            EmptyChartPlaceholder()
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = report.categoryBreakdown,
                                    key = { _, item -> "cat_${item.category.name}" }
                                ) { _, item ->
                                    val nonZeroIdx = nonZeroCategories.indexOfFirst { it.category == item.category }
                                    val isHighlighted = selectedCategoryIdx != null && nonZeroIdx == selectedCategoryIdx
                                    CategoryBreakdownCard(
                                        item = item,
                                        currencyCode = currencyCode,
                                        showBudgetRatio = selectedPeriod == ReportPeriod.MONTH,
                                        highlighted = isHighlighted,
                                        onClick = {
                                            if (nonZeroIdx >= 0) {
                                                selectedCategoryIdx =
                                                    if (selectedCategoryIdx == nonZeroIdx) null else nonZeroIdx
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            // Channels, Trend Summary & Top Merchants tab
                            item(key = "period_trend_card") {
                                CrispCard(
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Spending Trend",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (report.totalSpentCents == 0L) {
                                            EmptyChartPlaceholder()
                                        } else {
                                            SpendingTrendBarChart(
                                                buckets = report.trendBuckets,
                                                currencyCode = currencyCode
                                            )
                                        }
                                    }
                                }
                            }

                            if (report.paymentSources.isNotEmpty()) {
                                item(key = "payment_sources") {
                                    CrispCard(
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Text(
                                                text = "By Payment Channel",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            report.paymentSources.forEach { srcItem ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            modifier = Modifier.size(38.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = srcItem.source.icon(),
                                                                    contentDescription = srcItem.source.displayName,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Column {
                                                            Text(
                                                                text = srcItem.source.displayName,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "${(srcItem.shareFraction * 100).roundToInt()}% of spend",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    SplitCurrencyText(
                                                        amountCents = srcItem.totalCents,
                                                        currencyCode = currencyCode,
                                                        style = TabularAmountStyle.copy(fontSize = 15.sp),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (report.topMerchants.isNotEmpty()) {
                                item(key = "top_merchants") {
                                    CrispCard(
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Text(
                                                text = "Top Merchants",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            report.topMerchants.forEach { m ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = m.primaryCategory.chartColor.copy(alpha = 0.14f),
                                                            modifier = Modifier.size(38.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = m.primaryCategory.icon(),
                                                                    contentDescription = null,
                                                                    tint = m.primaryCategory.chartColor,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Column {
                                                            Text(
                                                                text = m.merchant,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${m.primaryCategory.shortLabel} • ${m.count}x",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    SplitCurrencyText(
                                                        amountCents = m.totalCents,
                                                        currencyCode = currencyCode,
                                                        style = TabularAmountStyle.copy(fontSize = 15.sp),
                                                        color = extraColors.expenseCoral
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    item: CategoryBreakdownItem,
    currencyCode: String,
    showBudgetRatio: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val extraColors = LocalLedgerExtraColors.current
    val ratio = if (showBudgetRatio && item.budgetCents > 0L) {
        (item.spentCents.toFloat() / item.budgetCents.toFloat()).coerceIn(0f, 1f)
    } else {
        item.shareFraction.coerceIn(0f, 1f)
    }
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "categoryRatio"
    )
    val isOver = showBudgetRatio && item.budgetCents > 0L && item.spentCents > item.budgetCents
    val sharePct = (item.shareFraction * 100f).roundToInt()

    CrispCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        borderColor = if (highlighted) item.category.chartColor else extraColors.hairlineBorder,
        borderWidth = if (highlighted) 1.5.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .springPressScale(0.985f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = item.category.chartColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.category.icon(),
                            contentDescription = item.category.displayName,
                            tint = item.category.chartColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${sharePct}% of total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    SplitCurrencyText(
                        amountCents = item.spentCents,
                        currencyCode = currencyCode,
                        style = TabularAmountStyle.copy(fontSize = 15.sp),
                        color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurface
                    )
                    if (showBudgetRatio && item.budgetCents > 0L) {
                        val budgetPct = ((item.spentCents.toDouble() / item.budgetCents.toDouble()) * 100.0).roundToInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOver) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    extraColors.positiveGreenContainer
                                }
                            ) {
                                Text(
                                    text = "${budgetPct}%",
                                    style = TabularAmountStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOver) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            extraColors.onPositiveGreenContainer
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "of limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedRatio },
                color = if (isOver) extraColors.expenseCoral else item.category.chartColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.InsertChartOutlined,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "No spending recorded in this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
