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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import com.example.ui.components.LedgerTransactionRow
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.MonoCodeStyle
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
                    contentPadding = PaddingValues(top = 6.dp, bottom = 148.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Clean Minimalist Top Bar (no bulky bordered capsules)
                    item(key = "top_utility_bar") {
                        LedgerTopBar(
                            userInitial = userInitial,
                            userName = userName,
                            unreviewedCount = data.unreviewedCount,
                            listenerNeedsAttention = !listenerEnabled || !postNotifGranted,
                            onAvatarClick = { showAccountSheet = true },
                            onMonthClick = onNavigateToReports,
                            onBellClick = { showAlertsSheet = true }
                        )
                    }

                    // Open Paper-Canvas Editorial Spend Hero (no enclosing white card)
                    item(key = "open_spend_hero") {
                        AnimatedEntrance {
                            OpenLedgerSpendingHero(
                                spentCents = data.monthSpentCents,
                                budgetCents = data.settings.monthlyBudgetCents,
                                dailyAllowanceCents = data.savingsProjection.dailyAllowanceCents,
                                remainingDaysInMonth = data.savingsProjection.remainingDaysInMonth,
                                noSpendDaysThisMonth = data.savingsProjection.noSpendDaysThisMonth,
                                currencyCode = currency,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
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

                    // Unified 2-Column Architectural Ledger Summary
                    item(key = "unified_money_summary") {
                        AnimatedEntrance {
                            UnifiedMoneySummarySheet(
                                incomeCents = data.settings.monthlyIncomeCents,
                                projectedSavingsCents = data.savingsProjection.projectedSavingsCents,
                                unreviewedCount = data.unreviewedCount,
                                currencyCode = currency,
                                onIncomeClick = onNavigateToPlan,
                                onPaceClick = onNavigateToReports,
                                onInboxClick = { showAlertsSheet = true },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Quick-Add Strip (crisp compact tokens)
                    item(key = "quick_add_recurring") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QUICK LOG",
                                    style = MonoCodeStyle.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
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
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .springPressScale()
                                            .testTag("quick_add_chip_${preset.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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

                    // Transactions Header, Search & Source Filter Tabs
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
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (showSearchField) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(0.9.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .size(40.dp)
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
                                    Surface(
                                        onClick = onManualEntryClick,
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.9.dp, extraColors.hairlineBorder),
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
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .testTag("tx_search_input")
                                )
                            }

                            // Crisp Minimal Segmented Source Filter Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TransactionFilter.entries.forEach { filter ->
                                    val selected = activeFilter == filter
                                    Surface(
                                        onClick = { activeFilter = filter },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        border = if (selected) null else BorderStroke(0.8.dp, extraColors.hairlineBorder),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 36.dp)
                                            .testTag("tx_filter_${filter.name.lowercase()}")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
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
                                    shape = RoundedCornerShape(18.dp),
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
                                    shape = RoundedCornerShape(18.dp),
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
                        // Unified Daily Ledger Sheets (each day = 1 paper sheet with inset hairline dividers)
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
                                        style = MonoCodeStyle.copy(fontSize = 11.sp, letterSpacing = 0.6.sp),
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
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
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
private fun LedgerTopBar(
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
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Upper-left minimal account trigger -> AccountProfileSheet
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAvatarClick)
                .padding(end = 8.dp)
                .testTag("header_avatar_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            Column {
                Text(
                    text = firstName ?: "SageLedger",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Account",
                    style = MonoCodeStyle.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onMonthClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Open reports",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Upper-right Bell trigger -> AlertsInboxSheet
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBellClick)
                    .testTag("header_bell_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Open Alerts Inbox",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp)
                )
                if (listenerNeedsAttention || unreviewedCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = extraColors.expenseCoral,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(9.dp)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun OpenLedgerSpendingHero(
    spentCents: Long,
    budgetCents: Long,
    dailyAllowanceCents: Long,
    remainingDaysInMonth: Int,
    noSpendDaysThisMonth: Int,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val safeTarget = budgetCents.coerceAtLeast(1L)
    val spendFrac = (spentCents.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(
        targetValue = spendFrac,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "openHeroRail"
    )
    val remainingCents = budgetCents - spentCents
    val isOver = remainingCents < 0L
    val pctUsed = ((spentCents.toDouble() / safeTarget.toDouble()) * 100.0).roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPENT THIS MONTH",
                style = MonoCodeStyle.copy(fontSize = 11.sp, letterSpacing = 0.9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$pctUsed% OF LIMIT",
                style = MonoCodeStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.primary
                )
            )
        }

        SplitCurrencyText(
            amountCents = spentCents,
            currencyCode = currencyCode,
            style = TabularAmountStyle.copy(
                fontSize = 52.sp,
                letterSpacing = (-1.8).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fractionAlpha = 0.36f
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { animFrac },
                color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isOver) {
                        "Over budget by ${CurrencyFormatter.formatCents(-remainingCents, currencyCode)}"
                    } else {
                        "${CurrencyFormatter.formatCents(remainingCents, currencyCode)} remaining"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${CurrencyFormatter.formatCompactCents(budgetCents, currencyCode)} cap",
                    style = MonoCodeStyle.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Open architectural 2-column stat strip (Safe-to-Spend Today & No-Spend Days)
        HorizontalDivider(thickness = 0.8.dp, color = extraColors.hairlineBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("safe_to_spend_pill"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "SAFE TODAY",
                    style = MonoCodeStyle.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOver || dailyAllowanceCents <= 0L) {
                        "Limit reached · ${remainingDaysInMonth}d"
                    } else {
                        "${CurrencyFormatter.formatCents(dailyAllowanceCents, currencyCode)}/day"
                    },
                    style = TabularAmountStyle.copy(
                        fontSize = 16.sp,
                        color = if (isOver) extraColors.expenseCoral else MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            VerticalDivider(
                thickness = 0.8.dp,
                color = extraColors.hairlineBorder,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("no_spend_days_badge"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "NO-SPEND STREAK",
                    style = MonoCodeStyle.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$noSpendDaysThisMonth ${if (noSpendDaysThisMonth == 1) "day" else "days"} this month",
                    style = TabularAmountStyle.copy(
                        fontSize = 16.sp,
                        color = extraColors.positiveGreen
                    )
                )
            }
        }
        HorizontalDivider(thickness = 0.8.dp, color = extraColors.hairlineBorder)
    }
}

@Composable
private fun UnifiedMoneySummarySheet(
    incomeCents: Long,
    projectedSavingsCents: Long,
    unreviewedCount: Int,
    currencyCode: String,
    onIncomeClick: () -> Unit,
    onPaceClick: () -> Unit,
    onInboxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current

    CrispCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onIncomeClick)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "INCOME",
                    style = MonoCodeStyle.copy(fontSize = 10.sp, letterSpacing = 0.7.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SplitCurrencyText(
                    amountCents = incomeCents,
                    currencyCode = currencyCode,
                    style = TabularAmountStyle.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            VerticalDivider(
                thickness = 0.8.dp,
                color = extraColors.hairlineBorder,
                modifier = Modifier.fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPaceClick)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (projectedSavingsCents >= 0L) "PROJECTED SAVED" else "PACE OVER",
                    style = MonoCodeStyle.copy(fontSize = 10.sp, letterSpacing = 0.7.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SplitCurrencyText(
                    amountCents = if (projectedSavingsCents >= 0L) projectedSavingsCents else -projectedSavingsCents,
                    currencyCode = currencyCode,
                    style = TabularAmountStyle.copy(fontSize = 20.sp),
                    color = if (projectedSavingsCents >= 0L) extraColors.positiveGreen else extraColors.expenseCoral,
                    showPlusSign = projectedSavingsCents >= 0L
                )
            }
        }

        HorizontalDivider(thickness = 0.8.dp, color = extraColors.hairlineBorder)

        val footerClick = if (unreviewedCount > 0) onInboxClick else onPaceClick
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = footerClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (unreviewedCount > 0) {
                    "$unreviewedCount captured alert${if (unreviewedCount > 1) "s" else ""} waiting for confirmation"
                } else {
                    "View category & channel breakdown"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (unreviewedCount > 0) extraColors.expenseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
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
    CrispCard(
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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
            initialOffsetY = { (it / 12).coerceAtLeast(12) + delayIdx * 5 },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        content()
    }
}
