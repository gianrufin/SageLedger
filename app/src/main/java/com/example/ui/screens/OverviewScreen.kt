package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.CurrencyFormatter
import com.example.data.model.GitHubReleaseNotice
import com.example.data.model.PaymentSource
import com.example.data.model.RecurringPreset
import com.example.data.model.SpendingTransaction
import com.example.data.model.ThemeModePreference
import com.example.notifications.SpendingAlertNotifier
import com.example.notifications.TransactionNotificationListenerService
import com.example.ui.components.AccountProfileSheet
import com.example.ui.components.AlertsInboxSheet
import com.example.ui.components.CrispCard
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.SwipeableTransactionRowCard
import com.example.ui.components.icon
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.DashboardSnapshot
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class TransactionFilter(val label: String, val source: PaymentSource? = null) {
    ALL("All"),
    E_WALLET("E-Wallet", PaymentSource.E_WALLET),
    GOOGLE_PAY("GPay", PaymentSource.GOOGLE_PAY),
    CREDIT_CARD("Card", PaymentSource.CREDIT_CARD),
    SMS_ALERT("SMS", PaymentSource.SMS_ALERT),
    CASH("Cash", PaymentSource.CASH),
    NEEDS_REVIEW("To Confirm")
}

private data class DayTransactionGroup(
    val dayKey: Long,
    val headerLabel: String,
    val dayTotalCents: Long,
    val items: List<SpendingTransaction>
)

@Composable
fun OverviewScreen(
    state: UiState<DashboardSnapshot>,
    updateNotice: GitHubReleaseNotice?,
    onQuickAddPreset: (RecurringPreset) -> Unit,
    onAddPresetClicked: () -> Unit,
    onTransactionClick: (SpendingTransaction) -> Unit,
    onConfirmTransaction: (SpendingTransaction) -> Unit = {},
    onManualEntryClick: () -> Unit,
    onDismissUpdate: (permanentlyForTag: Boolean) -> Unit,
    userInitial: String = "S",
    userName: String = "",
    userEmail: String = "",
    themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    onSetThemeMode: (ThemeModePreference) -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val extraColors = LocalLedgerExtraColors.current

    var listenerEnabled by remember {
        mutableStateOf(TransactionNotificationListenerService.isNotificationAccessEnabled(context))
    }
    var postNotifGranted by remember {
        mutableStateOf(SpendingAlertNotifier.hasPostPermission(context))
    }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> postNotifGranted = granted }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = TransactionNotificationListenerService.isNotificationAccessEnabled(context)
                postNotifGranted = SpendingAlertNotifier.hasPostPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (updateNotice != null) {
        GitHubUpdateDialog(
            notice = updateNotice,
            onOpenRelease = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateNotice.htmlUrl))
                runCatching { context.startActivity(intent) }
                onDismissUpdate(false)
            },
            onDismiss = { permanent -> onDismissUpdate(permanent) }
        )
    }

    val snapshotData = (state as? UiState.Success)?.data
    if (showAccountSheet && snapshotData != null) {
        AccountProfileSheet(
            userName = userName,
            userEmail = userEmail,
            userInitial = userInitial,
            currencyCode = snapshotData.settings.currencyCode,
            monthlyBudgetCents = snapshotData.settings.monthlyBudgetCents,
            monthlyIncomeCents = snapshotData.settings.monthlyIncomeCents,
            themeMode = themeMode,
            onSetThemeMode = onSetThemeMode,
            onOpenSettings = onNavigateToSettings,
            onSignOut = onSignOut,
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showAlertsSheet && snapshotData != null) {
        val pendingAlerts = remember(snapshotData.transactions) {
            snapshotData.transactions.filter { it.needsReview }
        }
        AlertsInboxSheet(
            pendingAlerts = pendingAlerts,
            currencyCode = snapshotData.settings.currencyCode,
            listenerEnabled = listenerEnabled,
            postNotifGranted = postNotifGranted,
            onRequestPostNotif = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenListenerSettings = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                runCatching { context.startActivity(intent) }
            },
            onConfirmAlert = onConfirmTransaction,
            onEditAlert = onTransactionClick,
            onDismiss = { showAlertsSheet = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                    CrispCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val data = state.data
                val currency = data.settings.currencyCode

                val filteredTransactions = remember(data.transactions, searchQuery, activeFilter) {
                    val query = searchQuery.trim().lowercase(Locale.US)
                    data.transactions
                        .filter { tx ->
                            val matchesFilter = when (activeFilter) {
                                TransactionFilter.ALL -> true
                                TransactionFilter.NEEDS_REVIEW -> tx.needsReview
                                else -> tx.paymentSourceEnum == activeFilter.source
                            }
                            val matchesQuery = query.isEmpty() ||
                                tx.merchant.lowercase(Locale.US).contains(query) ||
                                tx.categoryEnum.displayName.lowercase(Locale.US).contains(query) ||
                                tx.note.lowercase(Locale.US).contains(query)
                            matchesFilter && matchesQuery
                        }
                        .take(30)
                }
                val groupedTransactions = remember(filteredTransactions) {
                    groupTransactionsByDay(filteredTransactions)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 114.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "editorial_header_bar") {
                        EditorialTopUtilityBar(
                            userInitial = userInitial,
                            userName = userName,
                            unreviewedCount = data.unreviewedCount,
                            listenerNeedsAttention = !listenerEnabled || !postNotifGranted,
                            onAvatarClick = { showAccountSheet = true },
                            onMonthPillClick = onNavigateToReports,
                            onBellClick = { showAlertsSheet = true }
                        )
                    }

                    item(key = "editorial_hero_card") {
                        AnimatedEntrance {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EditorialSpendingHeroCard(
                                    spentCents = data.monthSpentCents,
                                    budgetCents = data.settings.monthlyBudgetCents,
                                    dailyAllowanceCents = data.savingsProjection.dailyAllowanceCents,
                                    remainingDaysInMonth = data.savingsProjection.remainingDaysInMonth,
                                    noSpendDaysThisMonth = data.savingsProjection.noSpendDaysThisMonth,
                                    currencyCode = currency
                                )
                            }
                        }
                    }

                    if (!listenerEnabled || !postNotifGranted) {
                        item(key = "capture_setup_banner") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CapturePermissionBanner(
                                    listenerEnabled = listenerEnabled,
                                    postNotifGranted = postNotifGranted,
                                    onRequestPostNotif = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onOpenListenerSettings = {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        runCatching { context.startActivity(intent) }
                                    }
                                )
                            }
                        }
                    }

                    item(key = "your_money_bento") {
                        AnimatedEntrance {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Money",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    CrispCard(
                                        onClick = onNavigateToReports,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.springPressScale()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Details",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CrispCard(
                                        onClick = onNavigateToPlan,
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .springPressScale(0.98f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = extraColors.cobaltAccent.copy(alpha = 0.12f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                                        contentDescription = null,
                                                        tint = extraColors.cobaltAccent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "Income",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                SplitCurrencyText(
                                                    amountCents = data.settings.monthlyIncomeCents,
                                                    currencyCode = currency,
                                                    style = TabularAmountStyle.copy(fontSize = 21.sp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    CrispCard(
                                        onClick = onNavigateToReports,
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .springPressScale(0.98f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = extraColors.expenseCoral.copy(alpha = 0.12f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Payments,
                                                        contentDescription = null,
                                                        tint = extraColors.expenseCoral,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "Expenses",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                SplitCurrencyText(
                                                    amountCents = data.monthSpentCents,
                                                    currencyCode = currency,
                                                    style = TabularAmountStyle.copy(fontSize = 21.sp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Editorial High-Contrast Status Pill (zero AI slop icons)
                                val proj = data.savingsProjection
                                val insightHeadline: String
                                val insightCta: String
                                val insightIcon = if (data.unreviewedCount > 0) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.AutoMirrored.Rounded.TrendingUp
                                }
                                val onInsightClick: () -> Unit

                                if (data.unreviewedCount > 0) {
                                    insightHeadline = "${data.unreviewedCount} alert${if (data.unreviewedCount > 1) "s" else ""} ready to confirm"
                                    insightCta = "Inbox"
                                    onInsightClick = { showAlertsSheet = true }
                                } else if (proj.projectedSavingsCents >= 0L) {
                                    insightHeadline = "Savings pace: ${CurrencyFormatter.formatCompactCents(proj.projectedSavingsCents, currency)} projected"
                                    insightCta = "My Plan"
                                    onInsightClick = onNavigateToPlan
                                } else {
                                    insightHeadline = "Month pace is ${CurrencyFormatter.formatCompactCents(-proj.projectedSavingsCents, currency)} over target"
                                    insightCta = "Adjust"
                                    onInsightClick = onNavigateToPlan
                                }

                                Surface(
                                    onClick = onInsightClick,
                                    shape = RoundedCornerShape(50),
                                    color = extraColors.contrastPillBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springPressScale(0.985f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = insightIcon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = insightHeadline,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = extraColors.contrastPillText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = insightCta,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = extraColors.contrastPillText.copy(alpha = 0.85f)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = extraColors.contrastPillText.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "quick_add_recurring") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick-Add",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(
                                    onClick = onAddPresetClicked,
                                    modifier = Modifier.testTag("add_preset_button")
                                ) {
                                    Text("New preset")
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(data.recurringPresets, key = { it.id }) { preset ->
                                    CrispCard(
                                        onClick = { onQuickAddPreset(preset) },
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .springPressScale()
                                            .testTag("quick_add_chip_${preset.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = preset.categoryEnum.chartColor.copy(alpha = 0.14f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = preset.categoryEnum.icon(),
                                                        contentDescription = preset.categoryEnum.displayName,
                                                        tint = preset.categoryEnum.chartColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = preset.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = CurrencyFormatter.formatCents(preset.amountCents, currency),
                                                    style = TabularAmountStyle.copy(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                item(key = "add_preset_tile") {
                                    CrispCard(
                                        onClick = onAddPresetClicked,
                                        shape = RoundedCornerShape(18.dp),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        modifier = Modifier.springPressScale()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Add recurring preset",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Shortcut",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "recent_header_and_filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transactions",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            showSearchField = !showSearchField
                                            if (!showSearchField) searchQuery = ""
                                        },
                                        shape = CircleShape,
                                        color = if (showSearchField) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(1.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("toggle_tx_search_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (showSearchField) Icons.Rounded.Close else Icons.Rounded.Search,
                                                contentDescription = "Search merchants",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    CrispCard(
                                        onClick = onManualEntryClick,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.testTag("manual_reconcile_link")
                                    ) {
                                        Text(
                                            text = "Reconcile cash",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showSearchField) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search store, category, or note...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Clear search"
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .testTag("tx_search_input")
                                )
                            }

                            // E-Wallet & Payment Source filter chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TransactionFilter.entries.forEach { filter ->
                                    val selected = activeFilter == filter
                                    FilterChip(
                                        selected = selected,
                                        onClick = { activeFilter = filter },
                                        label = {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        leadingIcon = filter.source?.let { src ->
                                            {
                                                Icon(
                                                    imageVector = src.icon(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("tx_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    if (data.transactions.isEmpty()) {
                        item(key = "empty_transactions") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CrispCard(
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No purchases recorded yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap + to record cash or E-Wallet spend, or enable Notification Access for automatic capture.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(onClick = onManualEntryClick) {
                                            Text("Record first purchase")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (groupedTransactions.isEmpty()) {
                        item(key = "empty_filtered") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CrispCard(
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "No matching purchases",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Try clearing your search or switching back to All sources.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = {
                                                searchQuery = ""
                                                activeFilter = TransactionFilter.ALL
                                            }
                                        ) {
                                            Text("Reset filters")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        groupedTransactions.forEach { group ->
                            item(key = "day_hdr_${group.dayKey}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.headerLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatCents(group.dayTotalCents, currency),
                                            style = TabularAmountStyle.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = group.items,
                                key = { _, tx -> tx.id }
                            ) { idx, tx ->
                                AnimatedEntrance(delayIdx = idx.coerceAtMost(4)) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        SwipeableTransactionRowCard(
                                            transaction = tx,
                                            currencyCode = currency,
                                            onClick = { onTransactionClick(tx) },
                                            onSwipeConfirm = onConfirmTransaction
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

@Composable
private fun EditorialTopUtilityBar(
    userInitial: String,
    userName: String,
    unreviewedCount: Int,
    listenerNeedsAttention: Boolean,
    onAvatarClick: () -> Unit,
    onMonthPillClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val extraColors = LocalLedgerExtraColors.current
    val monthLabel = remember {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
    }
    val firstName = remember(userName) {
        userName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Upper-left Avatar pill -> opens AccountProfileSheet
        CrispCard(
            onClick = onAvatarClick,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .springPressScale()
                .testTag("header_avatar_button")
        ) {
            Row(
                modifier = Modifier.padding(start = 4.dp, end = if (firstName != null) 12.dp else 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userInitial.take(1).uppercase(Locale.US),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                if (firstName != null) {
                    Text(
                        text = firstName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Center Month Selector pill -> opens Reports
        CrispCard(
            onClick = onMonthPillClick,
            shape = RoundedCornerShape(50),
            modifier = Modifier.springPressScale()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Open reports",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Upper-right Bell button -> opens AlertsInboxSheet
        Surface(
            onClick = onBellClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extraColors.hairlineBorder),
            modifier = Modifier
                .size(44.dp)
                .springPressScale()
                .testTag("header_bell_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Open Alerts Inbox",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                if (listenerNeedsAttention || unreviewedCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = extraColors.expenseCoral,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(10.dp)
                    ) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorialSpendingHeroCard(
    spentCents: Long,
    budgetCents: Long,
    dailyAllowanceCents: Long,
    remainingDaysInMonth: Int,
    noSpendDaysThisMonth: Int,
    currencyCode: String
) {
    val extraColors = LocalLedgerExtraColors.current
    val safeTarget = budgetCents.coerceAtLeast(1L)
    val spendFrac = (spentCents.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(
        targetValue = spendFrac,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "editorialBudgetRail"
    )
    val remainingCents = budgetCents - spentCents
    val isOver = remainingCents < 0L
    val pctUsed = ((spentCents.toDouble() / safeTarget.toDouble()) * 100.0).roundToInt()

    CrispCard(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENT THIS MONTH",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOver) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = "$pctUsed% of budget",
                        style = TabularAmountStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            SplitCurrencyText(
                amountCents = spentCents,
                currencyCode = currencyCode,
                style = TabularAmountStyle.copy(
                    fontSize = 46.sp,
                    letterSpacing = (-1.4).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fractionAlpha = 0.42f
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animFrac },
                    color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOver) {
                            "Over by ${CurrencyFormatter.formatCents(-remainingCents, currencyCode)}"
                        } else {
                            "${CurrencyFormatter.formatCents(remainingCents, currencyCode)} left"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Limit ${CurrencyFormatter.formatCompactCents(budgetCents, currencyCode)}",
                        style = TabularAmountStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Micro-Features 1 & 2: Safe-to-Spend Today & No-Spend Days Badge
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOver) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, extraColors.hairlineBorder),
                    modifier = Modifier.testTag("safe_to_spend_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        val text = if (isOver || dailyAllowanceCents <= 0L) {
                            "Daily limit exceeded • ${remainingDaysInMonth}d left"
                        } else {
                            "Safe today: ${CurrencyFormatter.formatCents(dailyAllowanceCents, currencyCode)}/day"
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isOver) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = extraColors.positiveGreenContainer.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("no_spend_days_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = extraColors.onPositiveGreenContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$noSpendDaysThisMonth no-spend ${if (noSpendDaysThisMonth == 1) "day" else "days"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = extraColors.onPositiveGreenContainer
                        )
                    }
                }
            }
        }
    }
}

private fun groupTransactionsByDay(transactions: List<SpendingTransaction>): List<DayTransactionGroup> {
    if (transactions.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    val nowCal = Calendar.getInstance()
    val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
    val todayYear = nowCal.get(Calendar.YEAR)

    nowCal.add(Calendar.DAY_OF_YEAR, -1)
    val yestDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
    val yestYear = nowCal.get(Calendar.YEAR)

    val fullDateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

    return transactions
        .groupBy { tx ->
            cal.timeInMillis = tx.occurredAtMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .entries
        .sortedByDescending { it.key }
        .map { (dayStart, list) ->
            cal.timeInMillis = dayStart
            val dYear = cal.get(Calendar.YEAR)
            val dDay = cal.get(Calendar.DAY_OF_YEAR)
            val label = when {
                dYear == todayYear && dDay == todayDayOfYear -> "Today"
                dYear == yestYear && dDay == yestDayOfYear -> "Yesterday"
                else -> fullDateFmt.format(Date(dayStart))
            }
            DayTransactionGroup(
                dayKey = dayStart,
                headerLabel = label,
                dayTotalCents = list.sumOf { it.amountCents },
                items = list.sortedByDescending { it.occurredAtMillis }
            )
        }
}

@Composable
private fun CapturePermissionBanner(
    listenerEnabled: Boolean,
    postNotifGranted: Boolean,
    onRequestPostNotif: () -> Unit,
    onOpenListenerSettings: () -> Unit
) {
    CrispCard(
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable automatic purchase capture",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Allow SageLedger to read Wallet, E-Wallet, card, and SMS alerts automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!postNotifGranted) {
                    Button(
                        onClick = onRequestPostNotif,
                        modifier = Modifier.testTag("grant_post_notif_button")
                    ) {
                        Text("Allow alerts")
                    }
                }
                if (!listenerEnabled) {
                    OutlinedButton(
                        onClick = onOpenListenerSettings,
                        modifier = Modifier.testTag("open_listener_settings_button")
                    ) {
                        Text("Notification access")
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubUpdateDialog(
    notice: GitHubReleaseNotice,
    onOpenRelease: () -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss(false) },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = "Update available",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Update available (${notice.tagName})")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = notice.releaseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = notice.notes.take(360),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenRelease,
                modifier = Modifier.testTag("github_update_open_button")
            ) {
                Text(
                    if (notice.htmlUrl.endsWith(".apk", ignoreCase = true)) {
                        "Download APK"
                    } else {
                        "View release"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss(true) },
                modifier = Modifier.testTag("github_update_dismiss_button")
            ) {
                Text("Skip ${notice.tagName}")
            }
        },
        modifier = Modifier.testTag("github_update_dialog")
    )
}

@Composable
private fun AnimatedEntrance(
    delayIdx: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + slideInVertically(
            initialOffsetY = { (it / 10).coerceAtLeast(14) + delayIdx * 6 },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        content()
    }
}
