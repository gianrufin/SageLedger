package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyFormatter
import com.example.data.model.RecurringPreset
import com.example.data.model.SpendingCategory
import com.example.ui.components.CircularPercentRing
import com.example.ui.components.CrispCard
import com.example.ui.components.HatchedGoalProgressBar
import com.example.ui.components.SavingsProjectionChart
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.icon
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.DashboardSnapshot
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun PlanScreen(
    state: UiState<DashboardSnapshot>,
    onSaveCategoryBudget: (SpendingCategory, Long) -> Unit,
    onQuickAddPreset: (RecurringPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onAddPresetClicked: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val snapshot = (state as? UiState.Success)?.data
    var editingBudgetCategory by remember { mutableStateOf<SpendingCategory?>(null) }

    if (editingBudgetCategory != null && snapshot != null) {
        val cat = editingBudgetCategory!!
        val existingLimit = snapshot.budgets.firstOrNull { it.categoryEnum == cat }?.monthlyLimitCents
            ?: cat.defaultBudgetCents
        var limitText by remember(cat) {
            mutableStateOf(String.format(java.util.Locale.US, "%.0f", existingLimit / 100.0))
        }
        val currency = snapshot.settings.currencyCode
        AlertDialog(
            onDismissRequest = { editingBudgetCategory = null },
            title = { Text("${cat.displayName} Budget") },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly limit ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cents = ((limitText.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                        onSaveCategoryBudget(cat, cents)
                        editingBudgetCategory = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingBudgetCategory = null }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
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
                val data = state.data
                val currency = data.settings.currencyCode
                val proj = data.savingsProjection
                val budgetMap = remember(data.budgets) { data.budgets.associateBy { it.categoryEnum } }
                val spentByCat = remember(data.monthTransactions) {
                    data.monthTransactions.groupBy { it.categoryEnum }.mapValues { (_, list) ->
                        list.sumOf { it.amountCents }
                    }
                }
                val sortedCategories = remember(budgetMap, spentByCat) {
                    SpendingCategory.entries.sortedByDescending { cat ->
                        val limit = (budgetMap[cat]?.monthlyLimitCents ?: cat.defaultBudgetCents).coerceAtLeast(1L)
                        (spentByCat[cat] ?: 0L).toFloat() / limit.toFloat()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "plan_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "My Plan",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Savings pace & category limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = onOpenSettings,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, extraColors.hairlineBorder),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = "Edit monthly income & budget",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Surface(
                                    onClick = onAddPresetClicked,
                                    shape = CircleShape,
                                    color = extraColors.contrastPillBg,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .springPressScale()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add recurring shortcut",
                                            tint = extraColors.contrastPillText,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "goals_hero") {
                        val budgetTarget = proj.monthlyBudgetCents.coerceAtLeast(1L)
                        val spendFrac = (proj.spentToDateCents.toFloat() / budgetTarget.toFloat()).coerceIn(0f, 1f)
                        val remainingCents = proj.monthlyBudgetCents - proj.spentToDateCents
                        val isOnTrack = proj.projectedMonthEndSpendCents <= proj.monthlyBudgetCents
                        val barColor = if (isOnTrack) extraColors.positiveGreen else extraColors.expenseCoral

                        CrispCard(
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = barColor.copy(alpha = 0.14f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Savings,
                                                contentDescription = null,
                                                tint = barColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Monthly Spending Guardrail",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Day ${proj.currentDayOfMonth} of ${proj.daysInMonth} • Income ${CurrencyFormatter.formatCompactCents(proj.monthlyIncomeCents, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = onOpenSettings) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit income or budget",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SplitCurrencyText(
                                        amountCents = proj.spentToDateCents,
                                        currencyCode = currency,
                                        style = TabularAmountStyle.copy(fontSize = 30.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "of ${CurrencyFormatter.formatCents(proj.monthlyBudgetCents, currency)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }

                                HatchedGoalProgressBar(
                                    fraction = spendFrac,
                                    fillColor = barColor
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(spendFrac * 100f).roundToInt()}% of budget used",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (remainingCents >= 0L) {
                                            "${CurrencyFormatter.formatCents(remainingCents, currency)} left"
                                        } else {
                                            "${CurrencyFormatter.formatCents(-remainingCents, currency)} over"
                                        },
                                        style = TabularAmountStyle.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingCents >= 0L) MaterialTheme.colorScheme.onSurface else extraColors.expenseCoral
                                        )
                                    )
                                }

                                val bannerColor = if (isOnTrack) extraColors.positiveGreen else extraColors.expenseCoral
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = bannerColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isOnTrack) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Info,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        val msg = if (proj.projectedSavingsCents >= 0L) {
                                            if (isOnTrack) {
                                                "On track for ${CurrencyFormatter.formatCents(proj.projectedSavingsCents, currency, showPlusSign = true)} savings this month."
                                            } else {
                                                "Spend pace exceeds budget, still +${CurrencyFormatter.formatCents(proj.projectedSavingsCents, currency)} vs income."
                                            }
                                        } else {
                                            "Projected ${CurrencyFormatter.formatCents(-proj.projectedSavingsCents, currency)} above monthly income pace."
                                        }
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }

                                SavingsProjectionChart(
                                    projection = proj,
                                    currencyCode = currency
                                )
                            }
                        }
                    }

                    item(key = "budgets_section_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Budgets",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap card to edit limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(sortedCategories, key = { "budget_${it.name}" }) { cat ->
                        val limit = budgetMap[cat]?.monthlyLimitCents ?: cat.defaultBudgetCents
                        val spent = spentByCat[cat] ?: 0L
                        val pct = if (limit > 0L) ((spent.toDouble() / limit.toDouble()) * 100.0).roundToInt() else 0
                        val ringColor = if (pct > 100) extraColors.expenseCoral else cat.chartColor

                        CrispCard(
                            onClick = { editingBudgetCategory = cat },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPressScale(0.985f)
                                .testTag("plan_budget_${cat.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = cat.chartColor.copy(alpha = 0.14f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = cat.icon(),
                                            contentDescription = cat.displayName,
                                            tint = cat.chartColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatCents(spent, currency)} of ${CurrencyFormatter.formatCents(limit, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                CircularPercentRing(
                                    percentage = pct,
                                    ringColor = ringColor
                                )
                            }
                        }
                    }

                    item(key = "presets_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick-Add Shortcuts",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onAddPresetClicked) {
                                Text("Add shortcut")
                            }
                        }
                    }

                    if (data.recurringPresets.isEmpty()) {
                        item(key = "empty_presets") {
                            CrispCard(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Save daily coffee or commute presets for 1-tap logging.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = onAddPresetClicked) {
                                        Text("Create")
                                    }
                                }
                            }
                        }
                    } else {
                        items(data.recurringPresets, key = { "preset_${it.id}" }) { preset ->
                            CrispCard(
                                onClick = { onQuickAddPreset(preset) },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springPressScale(0.985f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = preset.categoryEnum.chartColor.copy(alpha = 0.14f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = preset.categoryEnum.icon(),
                                                contentDescription = preset.categoryEnum.displayName,
                                                tint = preset.categoryEnum.chartColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${preset.merchant} • Tap to log ${CurrencyFormatter.formatCents(preset.amountCents, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeletePreset(preset.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete preset",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "plan_bottom_spacer") {
                        Spacer(modifier = Modifier.height(104.dp))
                    }
                }
            }
        }
    }
}
