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
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.SavingsProjection
import com.example.data.model.SpendingTransaction
import com.example.data.model.ThemeModePreference
import com.example.notifications.SpendingAlertNotifier
import com.example.notifications.TransactionNotificationListenerService
import com.example.ui.components.AccountProfileSheet
import com.example.ui.components.AlertsInboxSheet
import com.example.ui.components.CrispCard
import com.example.ui.components.LedgerTransactionRow
import com.example.ui.components.LiveSpendSparklineChart
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.springPressScale
import com.example.ui.theme.BentoEyebrowStyle
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
    themeMode: ThemeModePreference = ThemeModePreference.DARK,
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    contentPadding = PaddingValues(top = 6.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "top_utility_bar") {
                        BentoTopBar(
                            userInitial = userInitial,
                            userName = userName,
                            unreviewedCount = data.unreviewedCount,
                            listenerNeedsAttention = !listenerEnabled || !postNotifGranted,
                            onAvatarClick = { showAccountSheet = true },
                            onMonthClick = onNavigateToReports,
                            onBellClick = { showAlertsSheet = true }
                        )
                    }

                    // Live Sparkline Chart + Clean Balance Hero Bento
                    item(key = "sparkline_hero_bento") {
                        AnimatedEntrance {
                            LiveSpendHeroBento(
                                spentCents = data.monthSpentCents,
                                budgetCents = data.settings.monthlyBudgetCents,
                                projection = data.savingsProjection,
                                currencyCode = currency,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // 2x2 Dark Bento Metrics Grid
                    item(key = "bento_metrics_grid") {
                        AnimatedEntrance(delayIdx = 1) {
                            BentoMetricsGrid(
                                projection = data.savingsProjection,
                                budgetCents = data.settings.monthlyBudgetCents,
                                spentCents = data.monthSpentCents,
                                incomeCents = data.settings.monthlyIncomeCents,
                                currencyCode = currency,
                                onIncomeClick = onNavigateToPlan,
                                onProjectedClick = onNavigateToReports,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Contextual Alert Inbox Strip when unreviewed items exist
                    if (data.unreviewedCount > 0) {
                        item(key = "unreviewed_inbox_strip") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CrispCard(
                                    onClick = { showAlertsSheet = true },
                                    shape = RoundedCornerShape(18.dp),
                                    containerColor = extraColors.expenseCoralContainer,
                                    borderColor = extraColors.expenseCoral.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springPressScale()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bolt,
                                                contentDescription = null,
                                                tint = extraColors.expenseCoral,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "${data.unreviewedCount} captured alert${if (data.unreviewedCount > 1) "s" else ""} ready · Review inbox",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = extraColors.onExpenseCoralContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = extraColors.expenseCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
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

                    // Quick-Log Strip
                    item(key = "quick_add_recurring") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QUICK LOG",
                                    style = BentoEyebrowStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+ New shortcut",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable(onClick = onAddPresetClicked)
                                        .padding(vertical = 4.dp)
                                        .testTag("add_preset_button")
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(data.recurringPresets, key = { it.id }) { preset ->
                                    CrispCard(
                                        onClick = { onQuickAddPreset(preset) },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .springPressScale()
                                            .testTag("quick_add_chip_${preset.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatCents(preset.amountCents, currency),
                                                style = TabularAmountStyle.copy(
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Transactions Header, Search & Source Filter Pills
                    item(key = "transactions_controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
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
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (showSearchField) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(1.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("toggle_tx_search_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (showSearchField) Icons.Rounded.Close else Icons.Rounded.Search,
                                                contentDescription = "Search merchants",
                                                tint = if (showSearchField) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = onManualEntryClick,
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 40.dp)
                                            .testTag("manual_reconcile_link")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Reconcile",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
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
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .testTag("tx_search_input")
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TransactionFilter.entries.forEach { filter ->
                                    val selected = activeFilter == filter
                                    val isEWalletFilter = filter == TransactionFilter.E_WALLET
                                    val selectedBg = when {
                                        isEWalletFilter -> extraColors.cobaltAccent
                                        filter == TransactionFilter.NEEDS_REVIEW -> extraColors.expenseCoral
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val selectedText = if (isEWalletFilter || filter == TransactionFilter.NEEDS_REVIEW) {
                                        Color(0xFF061624)
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    }

                                    Surface(
                                        onClick = { activeFilter = filter },
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) selectedBg else MaterialTheme.colorScheme.surface,
                                        border = if (selected) null else BorderStroke(0.9.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 36.dp)
                                            .testTag("tx_filter_${filter.name.lowercase()}")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) selectedText else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
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
                        items(
                            items = groupedTransactions,
                            key = { "day_sheet_${it.dayKey}" }
                        ) { group ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.headerLabel.uppercase(Locale.getDefault()),
                                        style = BentoEyebrowStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCents(group.dayTotalCents, currency),
                                        style = TabularAmountStyle.copy(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                }

                                CrispCard(
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                ) {
                                    group.items.forEachIndexed { idx, tx ->
                                        LedgerTransactionRow(
                                            transaction = tx,
                                            currencyCode = currency,
                                            onClick = { onTransactionClick(tx) },
                                            onSwipeConfirm = onConfirmTransaction,
                                            showBottomDivider = idx < group.items.lastIndex
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
private fun BentoTopBar(
    userInitial: String,
    userName: String,
    unreviewedCount: Int,
    listenerNeedsAttention: Boolean,
    onAvatarClick: () -> Unit,
    onMonthClick: () -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Upper-left Account Trigger -> AccountProfileSheet
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onAvatarClick)
                .padding(end = 10.dp)
                .testTag("header_avatar_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userInitial.take(1).uppercase(Locale.US),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    text = firstName ?: "SageLedger",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Account & theme",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onMonthClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extraColors.hairlineBorder),
                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

            // Upper-right Bell Trigger -> AlertsInboxSheet
            Surface(
                onClick = onBellClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extraColors.hairlineBorder),
                modifier = Modifier
                    .size(44.dp)
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
                                .padding(9.dp)
                                .size(9.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSpendHeroBento(
    spentCents: Long,
    budgetCents: Long,
    projection: SavingsProjection,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val safeTarget = budgetCents.coerceAtLeast(1L)
    val remainingCents = budgetCents - spentCents
    val isOver = remainingCents < 0L
    val pctUsed = ((spentCents.toDouble() / safeTarget.toDouble()) * 100.0).roundToInt()
    val heroAccent = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.primary
    val monthHeader = remember {
        SimpleDateFormat("MMMM SPEND", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
    }

    CrispCard(
        shape = RoundedCornerShape(26.dp),
        accentGlowColor = heroAccent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthHeader,
                    style = BentoEyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOver) extraColors.expenseCoralContainer else extraColors.positiveGreenContainer
                ) {
                    Text(
                        text = "$pctUsed% OF ${CurrencyFormatter.formatCompactCents(budgetCents, currencyCode)}",
                        style = BentoEyebrowStyle.copy(
                            fontSize = 10.sp,
                            color = if (isOver) extraColors.onExpenseCoralContainer else extraColors.onPositiveGreenContainer
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SplitCurrencyText(
                    amountCents = spentCents,
                    currencyCode = currencyCode,
                    style = TabularAmountStyle.copy(
                        fontSize = 44.sp,
                        letterSpacing = (-1.6).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fractionAlpha = 0.42f
                )
                Text(
                    text = if (isOver) {
                        "Over limit by ${CurrencyFormatter.formatCents(-remainingCents, currencyCode)}"
                    } else {
                        "${CurrencyFormatter.formatCents(remainingCents, currencyCode)} remaining this month"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LiveSpendSparklineChart(
                projection = projection,
                currencyCode = currencyCode
            )
        }
    }
}

@Composable
private fun BentoMetricsGrid(
    projection: SavingsProjection,
    budgetCents: Long,
    spentCents: Long,
    incomeCents: Long,
    currencyCode: String,
    onIncomeClick: () -> Unit,
    onProjectedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val isOver = budgetCents - spentCents < 0L
    val projectedPositive = projection.projectedSavingsCents >= 0L

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoMetricTile(
                icon = Icons.Rounded.Shield,
                accentColor = if (isOver) extraColors.expenseCoral else extraColors.cobaltAccent,
                eyebrow = "SAFE TODAY",
                valueText = if (isOver || projection.dailyAllowanceCents <= 0L) {
                    "Limit hit"
                } else {
                    CurrencyFormatter.formatCents(projection.dailyAllowanceCents, currencyCode)
                },
                subtitle = if (isOver || projection.dailyAllowanceCents <= 0L) {
                    "${projection.remainingDaysInMonth} days left"
                } else {
                    "per day · ${projection.remainingDaysInMonth}d left"
                },
                valueColor = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .testTag("safe_to_spend_pill")
            )

            BentoMetricTile(
                icon = Icons.Rounded.LocalFireDepartment,
                accentColor = extraColors.positiveGreen,
                eyebrow = "NO-SPEND STREAK",
                valueText = "${projection.noSpendDaysThisMonth} ${if (projection.noSpendDaysThisMonth == 1) "day" else "days"}",
                subtitle = "zero spend this month",
                valueColor = extraColors.positiveGreen,
                modifier = Modifier
                    .weight(1f)
                    .testTag("no_spend_days_badge")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoMetricTile(
                icon = Icons.Rounded.AccountBalanceWallet,
                accentColor = extraColors.amberAccent,
                eyebrow = "MONTHLY INCOME",
                valueText = CurrencyFormatter.formatCompactCents(incomeCents, currencyCode),
                subtitle = "Limit ${CurrencyFormatter.formatCompactCents(budgetCents, currencyCode)}",
                valueColor = MaterialTheme.colorScheme.onSurface,
                onClick = onIncomeClick,
                modifier = Modifier.weight(1f)
            )

            val absProjected = if (projectedPositive) projection.projectedSavingsCents else -projection.projectedSavingsCents
            val projectedPrefix = if (projectedPositive && absProjected > 0L) "+" else if (!projectedPositive) "-" else ""
            BentoMetricTile(
                icon = Icons.Rounded.Savings,
                accentColor = if (projectedPositive) extraColors.positiveGreen else extraColors.expenseCoral,
                eyebrow = if (projectedPositive) "PROJECTED SAVED" else "PROJECTED OVER",
                valueText = projectedPrefix + CurrencyFormatter.formatCompactCents(absProjected, currencyCode),
                subtitle = "month-end pace",
                valueColor = if (projectedPositive) extraColors.positiveGreen else extraColors.expenseCoral,
                onClick = onProjectedClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoMetricTile(
    icon: ImageVector,
    accentColor: Color,
    eyebrow: String,
    valueText: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    CrispCard(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        accentGlowColor = accentColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = eyebrow,
                    style = BentoEyebrowStyle.copy(fontSize = 9.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = valueText,
                    style = TabularAmountStyle.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = valueColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

    val fullDateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

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
    val extraColors = LocalLedgerExtraColors.current
    CrispCard(
        shape = RoundedCornerShape(20.dp),
        containerColor = extraColors.cobaltContainer.copy(alpha = 0.72f),
        borderColor = extraColors.cobaltAccent.copy(alpha = 0.35f),
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
                    tint = extraColors.cobaltAccent
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable automatic purchase capture",
                        style = MaterialTheme.typography.titleSmall,
                        color = extraColors.onCobaltContainer
                    )
                    Text(
                        text = "Allow SageLedger to read Wallet, E-Wallet, card, and SMS alerts automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.onCobaltContainer.copy(alpha = 0.85f)
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
            initialOffsetY = { (it / 12).coerceAtLeast(12) + delayIdx * 5 },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        content()
    }
}
