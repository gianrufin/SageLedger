package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.core.CurrencyFormatter
import com.example.data.model.SpendingCategory
import com.example.data.model.ThemeModePreference
import androidx.compose.ui.unit.sp
import com.example.notifications.SpendingAlertNotifier
import com.example.notifications.TransactionNotificationListenerService
import com.example.ui.components.CrispCard
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.DashboardSnapshot
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    state: UiState<DashboardSnapshot>,
    themeMode: ThemeModePreference,
    onSetThemeMode: (ThemeModePreference) -> Unit,
    onSaveFinanceSettings: (incomeCents: Long, budgetCents: Long, currency: String, githubRepo: String) -> Unit,
    onSaveCategoryBudget: (SpendingCategory, Long) -> Unit,
    onDeletePreset: (String) -> Unit,
    onAddPresetClicked: () -> Unit,
    onSaveMerchantRule: (keyword: String, category: SpendingCategory) -> Unit,
    onDeleteMerchantRule: (String) -> Unit,
    onCheckGitHubUpdate: (repo: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUser = Firebase.auth.currentUser

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

    val snapshot = (state as? UiState.Success)?.data
    val settings = snapshot?.settings

    var incomeInput by remember(settings) {
        mutableStateOf(
            settings?.let { String.format(java.util.Locale.US, "%.0f", it.monthlyIncomeCents / 100.0) } ?: "4000"
        )
    }
    var budgetInput by remember(settings) {
        mutableStateOf(
            settings?.let { String.format(java.util.Locale.US, "%.0f", it.monthlyBudgetCents / 100.0) } ?: "2400"
        )
    }
    var selectedCurrency by remember(settings) {
        mutableStateOf(settings?.currencyCode ?: "PHP")
    }
    var githubRepoInput by remember(settings) {
        mutableStateOf(settings?.githubRepo.orEmpty())
    }

    var editingBudgetCategory by remember { mutableStateOf<SpendingCategory?>(null) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    if (editingBudgetCategory != null && snapshot != null) {
        val cat = editingBudgetCategory!!
        val existingLimit = snapshot.budgets.firstOrNull { it.categoryEnum == cat }?.monthlyLimitCents
            ?: cat.defaultBudgetCents
        var limitText by remember(cat) {
            mutableStateOf(String.format(java.util.Locale.US, "%.0f", existingLimit / 100.0))
        }
        AlertDialog(
            onDismissRequest = { editingBudgetCategory = null },
            title = { Text("${cat.displayName} Budget") },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly limit ($selectedCurrency)") },
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

    if (showAddRuleDialog) {
        var keyword by remember { mutableStateOf("") }
        var ruleCategory by remember { mutableStateOf(SpendingCategory.FOOD) }
        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Add Store Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it.take(100) },
                        label = { Text("Store keyword") },
                        placeholder = { Text("e.g. Uncle John, DataBlitz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpendingCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = ruleCategory == cat,
                                onClick = { ruleCategory = cat },
                                label = { Text(cat.shortLabel) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyword.isNotBlank()) {
                            onSaveMerchantRule(keyword, ruleCategory)
                            showAddRuleDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "settings_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentUser?.email ?: currentUser?.displayName ?: "Preferences & sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initial = (currentUser?.displayName ?: currentUser?.email ?: "S")
                                .firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item(key = "appearance_card") {
                SectionCard(title = "Appearance") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Dark mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Charcoal low-glare appearance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = themeMode == ThemeModePreference.DARK,
                            onCheckedChange = { isDark ->
                                onSetThemeMode(if (isDark) ThemeModePreference.DARK else ThemeModePreference.LIGHT)
                            },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeModePreference.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            item(key = "budget_income_card") {
                SectionCard(title = "Monthly Income & Budget") {
                    OutlinedTextField(
                        value = incomeInput,
                        onValueChange = { incomeInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Monthly income ($selectedCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Monthly spending budget ($selectedCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Currency",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyFormatter.supportedCurrencies.forEach { code ->
                            FilterChip(
                                selected = selectedCurrency == code,
                                onClick = { selectedCurrency = code },
                                label = { Text("$code (${CurrencyFormatter.symbolFor(code)})") }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val inc = ((incomeInput.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                            val bud = ((budgetInput.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                            onSaveFinanceSettings(inc, bud, selectedCurrency, githubRepoInput)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_finance_settings_button")
                    ) {
                        Text("Save budget preferences")
                    }
                }
            }

            if (snapshot != null) {
                item(key = "category_limits_card") {
                    SectionCard(title = "Category Monthly Limits") {
                        val budgetMap = snapshot.budgets.associateBy { it.categoryEnum }
                        SpendingCategory.entries.forEachIndexed { idx, cat ->
                            val limit = budgetMap[cat]?.monthlyLimitCents ?: cat.defaultBudgetCents
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { editingBudgetCategory = cat }) {
                                    Text(CurrencyFormatter.formatCents(limit, selectedCurrency))
                                }
                            }
                            if (idx < SpendingCategory.entries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item(key = "recurring_presets_card") {
                    SectionCard(title = "Quick-Add Recurring Buttons") {
                        if (snapshot.recurringPresets.isEmpty()) {
                            Text(
                                text = "No quick-add buttons saved yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            snapshot.recurringPresets.forEach { preset ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${preset.merchant} • ${CurrencyFormatter.formatCents(preset.amountCents, selectedCurrency)}",
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
                        OutlinedButton(onClick = onAddPresetClicked) {
                            Text("Add recurring shortcut")
                        }
                    }
                }

                item(key = "merchant_rules_card") {
                    SectionCard(title = "Custom Store Rules") {
                        if (snapshot.merchantRules.isEmpty()) {
                            Text(
                                text = "No custom store overrides yet. Common chains and stores are matched automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            snapshot.merchantRules.forEach { rule ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rule.merchantKeyword,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = rule.categoryEnum.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteMerchantRule(rule.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete store rule",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedButton(onClick = { showAddRuleDialog = true }) {
                            Text("Add store rule")
                        }
                    }
                }
            }

            item(key = "permissions_card") {
                SectionCard(title = "Automatic Purchase Capture") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (listenerEnabled) "Active for E-Wallet, GPay, bank, and SMS alerts" else "Off — tap to enable in system settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Text(if (listenerEnabled) "Manage" else "Enable")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Confirmation Popups",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (postNotifGranted) "Enabled" else "Required for purchase confirmation alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!postNotifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
            }

            item(key = "github_updates_card") {
                SectionCard(title = "App Updates (GitHub Releases)") {
                    Text(
                        text = "Installed version: v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it.take(120) },
                        label = { Text("GitHub repository (owner/repo)") },
                        placeholder = { Text("e.g. gianrufin/SageLedger") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_repo_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val inc = ((incomeInput.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                                val bud = ((budgetInput.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                                onSaveFinanceSettings(inc, bud, selectedCurrency, githubRepoInput)
                                onCheckGitHubUpdate(githubRepoInput)
                            },
                            modifier = Modifier.testTag("check_github_updates_button")
                        ) {
                            Text("Save & check updates")
                        }
                    }
                }
            }

            item(key = "account_card") {
                SectionCard(title = "Google Account") {
                    Text(
                        text = currentUser?.email ?: currentUser?.displayName ?: "Signed in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Text("Sign out")
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(modifier = Modifier.height(132.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(java.util.Locale.getDefault()),
            style = MonoCodeStyle.copy(fontSize = 10.5.sp, letterSpacing = 0.85.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        CrispCard(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}
