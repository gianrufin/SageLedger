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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.RecurringPreset
import com.example.data.model.SpendingTransaction
import com.example.notifications.SpendingAlertNotifier
import com.example.notifications.TransactionNotificationListenerService
import com.example.ui.components.CrispCard
import com.example.ui.components.SplitCurrencyText
import com.example.ui.components.TransactionRowCard
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
    onManualEntryClick: () -> Unit,
    onDismissUpdate: (permanentlyForTag: Boolean) -> Unit,
    userInitial: String = "S",
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
                val recentGrouped = remember(data.transactions) {
                    groupTransactionsByDay(data.transactions.take(25))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "hero_gradient_section") {
                        AtmosphericHeroHeader(
                            spentCents = data.monthSpentCents,
                            budgetCents = data.settings.monthlyBudgetCents,
                            unreviewedCount = data.unreviewedCount,
                            listenerNeedsAttention = !listenerEnabled || !postNotifGranted,
                            currencyCode = currency,
                            userInitial = userInitial,
                            onMonthPillClick = onNavigateToReports,
                            onBellClick = {
                                val firstUnreviewed = data.transactions.firstOrNull { it.needsReview }
                                if (firstUnreviewed != null) {
                                    onTransactionClick(firstUnreviewed)
                                } else {
                                    onNavigateToSettings()
                                }
                            },
                            onAvatarClick = onNavigateToSettings
                        )
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
                                    // Income tile
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
                                                color = Color(0xFF1E8CF5).copy(alpha = 0.14f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                                        contentDescription = null,
                                                        tint = Color(0xFF1E8CF5),
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
                                                    style = TabularAmountStyle.copy(fontSize = 19.sp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Month Expenses tile
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
                                                color = extraColors.expenseCoral.copy(alpha = 0.14f),
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
                                                    style = TabularAmountStyle.copy(fontSize = 19.sp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quirky High-Contrast Obsidian Insight Pill
                                val proj = data.savingsProjection
                                val insightHeadline: String
                                val insightCta: String
                                val onInsightClick: () -> Unit

                                val firstUnreviewed = data.transactions.firstOrNull { it.needsReview }
                                if (firstUnreviewed != null && data.unreviewedCount > 0) {
                                    insightHeadline = "${data.unreviewedCount} captured alert${if (data.unreviewedCount > 1) "s" else ""} ready to confirm"
                                    insightCta = "Review"
                                    onInsightClick = { onTransactionClick(firstUnreviewed) }
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
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB3A6FF),
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
                                    style = MaterialTheme.typography.titleMedium,
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
                                                        fontSize = 12.sp,
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
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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

                    item(key = "recent_header") {
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
                                CrispCard(
                                    onClick = onManualEntryClick,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("manual_reconcile_link")
                                ) {
                                    Text(
                                        text = "Reconcile cash",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "For the Period",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
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
                                            text = "Tap + to record cash or card spend, or enable Notification Access for automatic Wallet and bank alerts.",
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
                    } else {
                        recentGrouped.forEach { group ->
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
                                                fontSize = 13.sp,
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
                                        TransactionRowCard(
                                            transaction = tx,
                                            currencyCode = currency,
                                            onClick = { onTransactionClick(tx) }
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
private fun AtmosphericHeroHeader(
    spentCents: Long,
    budgetCents: Long,
    unreviewedCount: Int,
    listenerNeedsAttention: Boolean,
    currencyCode: String,
    userInitial: String,
    onMonthPillClick: () -> Unit,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val extraColors = LocalLedgerExtraColors.current
    val monthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val remainingCents = budgetCents - spentCents
    val isOver = remainingCents < 0L

    Box(modifier = Modifier.fillMaxWidth()) {
        // Gradient + subtle atmospheric orb glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            extraColors.heroGradientTop,
                            extraColors.heroGradientMid,
                            extraColors.heroGradientBottom
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.12f, size.height * 0.1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.92f, size.height * 0.65f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header row: Avatar | Period Pill | Alert Bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onAvatarClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.55f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userInitial.take(1).uppercase(Locale.US),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = onMonthPillClick,
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Open reports",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onBellClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Spending alerts status",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            if (listenerNeedsAttention || unreviewedCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF4D4D),
                                    border = BorderStroke(1.5.dp, Color.White),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(9.dp)
                                ) {}
                            }
                        }
                    }
                }

                // Hero spending metric
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Spent This Month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                    SplitCurrencyText(
                        amountCents = spentCents,
                        currencyCode = currencyCode,
                        style = TabularAmountStyle.copy(fontSize = 40.sp),
                        color = Color.White,
                        fractionAlpha = 0.72f
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isOver) {
                            Color(0xFFFF5959).copy(alpha = 0.34f)
                        } else {
                            Color.White.copy(alpha = 0.20f)
                        }
                    ) {
                        val deltaText = if (isOver) {
                            "Over budget by ${CurrencyFormatter.formatCents(-remainingCents, currencyCode)}"
                        } else {
                            "${CurrencyFormatter.formatCents(remainingCents, currencyCode)} left of ${CurrencyFormatter.formatCompactCents(budgetCents, currencyCode)}"
                        }
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Overlapping curved sheet lip connecting cleanly into LazyColumn background
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .align(Alignment.BottomCenter)
        ) {}
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
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
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
                        text = "Allow SageLedger to read Wallet, card, and SMS alerts and send quick confirmation popups.",
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
