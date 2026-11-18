package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.data.categorization.MerchantCategorizer
import com.example.data.model.SpendingTransaction
import com.example.data.model.ThemeModePreference
import com.example.data.preferences.AppPreferencesStore
import com.example.data.remote.GitHubUpdateRepository
import com.example.data.repository.LedgerRepository
import com.example.notifications.SpendingAlertNotifier
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.components.springPressScale
import com.example.ui.screens.AddRecurringPresetDialog
import com.example.ui.screens.ManualEntrySheet
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalLedgerExtraColors
import com.example.ui.theme.SageLedgerTheme
import com.example.ui.viewmodel.LedgerViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_PLAN = "plan"
private const val ROUTE_SETTINGS = "settings"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    private var deepLinkTransactionId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)
        enableEdgeToEdge()
        deepLinkTransactionId = intent?.getStringExtra(SpendingAlertNotifier.EXTRA_TRANSACTION_ID)

        val preferencesStore = AppPreferencesStore(applicationContext)

        setContent {
            val themeMode by preferencesStore.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeModePreference.SYSTEM
            )
            val isDark = when (themeMode) {
                ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
                ThemeModePreference.LIGHT -> false
                ThemeModePreference.DARK -> true
            }

            SageLedgerTheme(darkTheme = isDark) {
                SageLedgerRoot(
                    deepLinkTransactionId = deepLinkTransactionId,
                    onConsumeDeepLink = { deepLinkTransactionId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTransactionId = intent.getStringExtra(SpendingAlertNotifier.EXTRA_TRANSACTION_ID)
    }
}

@Composable
private fun SageLedgerRoot(
    deepLinkTransactionId: String?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val firebaseReady = remember(context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            runCatching { FirebaseApp.initializeApp(context) }.getOrNull() != null
        } else {
            true
        }
    }

    if (!firebaseReady) {
        FirebaseNotConfiguredScreen()
        return
    }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val initial = remember(user.displayName, user.email) {
                (user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "S")
                    .firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            }
            AuthenticatedLedgerShell(
                currentUserId = user.uid,
                userInitial = initial,
                deepLinkTransactionId = deepLinkTransactionId,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@Composable
private fun FirebaseNotConfiguredScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Firebase config missing in this build",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This APK was compiled without google-services.json. Add the GOOGLE_SERVICES_JSON and DEBUG_KEYSTORE_BASE64 secrets in GitHub repository settings and re-run the Build & Update APK workflow, or install the APK exported from AI Studio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun AuthenticatedLedgerShell(
    currentUserId: String,
    userInitial: String,
    deepLinkTransactionId: String?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LedgerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LedgerViewModel(
                    repository = LedgerRepository(db),
                    preferencesStore = AppPreferencesStore(app),
                    categorizer = MerchantCategorizer(),
                    gitHubUpdateRepository = GitHubUpdateRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val reportState by viewModel.reportState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val githubUpdateNotice by viewModel.githubUpdateNotice.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    var showManualEntrySheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<SpendingTransaction?>(null) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    val dashboardData = (dashboardState as? com.example.ui.viewmodel.UiState.Success)?.data
    val currencyCode = dashboardData?.settings?.currencyCode ?: "USD"
    val monthlyBudgetCents = dashboardData?.settings?.monthlyBudgetCents ?: 0L
    val merchantRules = dashboardData?.merchantRules.orEmpty()

    LaunchedEffect(deepLinkTransactionId, dashboardData) {
        if (!deepLinkTransactionId.isNullOrBlank() && dashboardData != null) {
            val match = dashboardData.transactions.firstOrNull { it.id == deepLinkTransactionId }
            if (match != null) {
                editingTransaction = match
                showManualEntrySheet = true
                onConsumeDeepLink()
            }
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_OVERVIEW

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_OVERVIEW,
                labelRes = R.string.nav_overview,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_overview"
            ),
            NavDestinationItem(
                route = ROUTE_REPORTS,
                labelRes = R.string.nav_reports,
                selectedIcon = Icons.Rounded.Analytics,
                unselectedIcon = Icons.Outlined.Analytics,
                testTag = "nav_reports"
            ),
            NavDestinationItem(
                route = ROUTE_PLAN,
                labelRes = R.string.nav_plan,
                selectedIcon = Icons.Rounded.Savings,
                unselectedIcon = Icons.Outlined.Savings,
                testTag = "nav_plan"
            ),
            NavDestinationItem(
                route = ROUTE_SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openNewExpense: () -> Unit = {
        editingTransaction = null
        showManualEntrySheet = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    header = {
                        FloatingActionButton(
                            onClick = openNewExpense,
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .springPressScale()
                                .testTag("fab_add_expense")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Record purchase or cash spend"
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        val label = stringResource(item.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = if (useNavRail) 12.dp else 84.dp)
                    )
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_OVERVIEW,
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() }
                    ) {
                        composable(ROUTE_OVERVIEW) {
                            OverviewScreen(
                                state = dashboardState,
                                updateNotice = githubUpdateNotice,
                                onQuickAddPreset = { preset -> viewModel.quickAddPreset(preset) },
                                onAddPresetClicked = { showAddPresetDialog = true },
                                onTransactionClick = { tx ->
                                    editingTransaction = tx
                                    showManualEntrySheet = true
                                },
                                onManualEntryClick = openNewExpense,
                                onDismissUpdate = { perm -> viewModel.dismissUpdateNotice(perm) },
                                userInitial = userInitial,
                                onNavigateToReports = { navigateToTab(ROUTE_REPORTS) },
                                onNavigateToPlan = { navigateToTab(ROUTE_PLAN) },
                                onNavigateToSettings = { navigateToTab(ROUTE_SETTINGS) }
                            )
                        }

                        composable(ROUTE_REPORTS) {
                            ReportsScreen(
                                state = reportState,
                                selectedPeriod = selectedPeriod,
                                currencyCode = currencyCode,
                                monthlyBudgetCents = monthlyBudgetCents,
                                onSelectPeriod = { viewModel.selectPeriod(it) },
                                onSelectCustomRange = { s, e -> viewModel.setCustomDateRange(s, e) }
                            )
                        }

                        composable(ROUTE_PLAN) {
                            PlanScreen(
                                state = dashboardState,
                                onSaveCategoryBudget = { cat, limit ->
                                    viewModel.saveCategoryBudget(cat, limit)
                                },
                                onQuickAddPreset = { preset -> viewModel.quickAddPreset(preset) },
                                onDeletePreset = { viewModel.deleteRecurringPreset(it) },
                                onAddPresetClicked = { showAddPresetDialog = true },
                                onOpenSettings = { navigateToTab(ROUTE_SETTINGS) }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                state = dashboardState,
                                themeMode = themeMode,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSaveFinanceSettings = { inc, bud, curr, repo ->
                                    viewModel.saveFinanceSettings(inc, bud, curr, repo)
                                },
                                onSaveCategoryBudget = { cat, limit ->
                                    viewModel.saveCategoryBudget(cat, limit)
                                },
                                onDeletePreset = { viewModel.deleteRecurringPreset(it) },
                                onAddPresetClicked = { showAddPresetDialog = true },
                                onSaveMerchantRule = { kw, cat -> viewModel.saveMerchantRule(kw, cat) },
                                onDeleteMerchantRule = { viewModel.deleteMerchantRule(it) },
                                onCheckGitHubUpdate = { repo ->
                                    viewModel.checkGitHubUpdates(repo, showNoUpdateToast = true)
                                },
                                onSignOut = {
                                    performSignOut(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }

                    if (!useNavRail) {
                        PlayfulDockedBottomBar(
                            items = navItems,
                            currentRoute = currentRoute,
                            onSelectRoute = navigateToTab,
                            onAddClick = openNewExpense,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    if (showManualEntrySheet) {
        ManualEntrySheet(
            editingTransaction = editingTransaction,
            currencyCode = currencyCode,
            merchantRules = merchantRules,
            onSuggestCategory = { merchant, snippet, callback ->
                viewModel.suggestCategoryForMerchant(merchant, snippet, merchantRules, callback)
            },
            onParseSnippet = { snippet, onParsed, onUnrecognized ->
                viewModel.parseSnippetAndCategorize(snippet, merchantRules, onParsed, onUnrecognized)
            },
            onSave = { existingId, merchant, amountCents, category, source, note, snippet, millis, rememberRule ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    merchant = merchant,
                    amountCents = amountCents,
                    category = category,
                    paymentSource = source,
                    note = note,
                    rawSnippet = snippet,
                    occurredAtMillis = millis,
                    rememberMerchantRule = rememberRule
                )
            },
            onDelete = { txId -> viewModel.deleteTransaction(txId) },
            onDismiss = {
                showManualEntrySheet = false
                editingTransaction = null
            }
        )
    }

    if (showAddPresetDialog) {
        AddRecurringPresetDialog(
            currencyCode = currencyCode,
            onSave = { label, merchant, cents, category, source ->
                viewModel.addRecurringPreset(label, merchant, cents, category, source)
            },
            onDismiss = { showAddPresetDialog = false }
        )
    }
}

@Composable
private fun PlayfulDockedBottomBar(
    items: List<NavDestinationItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalLedgerExtraColors.current
    val leftItems = items.take(2)
    val rightItems = items.drop(2)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extraColors.hairlineBorder),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                leftItems.forEach { item ->
                    DockedNavTab(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onSelectRoute(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Space reserved under the centered raised + button
                Spacer(modifier = Modifier.width(68.dp))

                rightItems.forEach { item ->
                    DockedNavTab(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onSelectRoute(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Raised centered circular + button sitting proud above the dock
        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(58.dp)
                .springPressScale()
                .testTag("fab_add_expense")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Record purchase or cash spend",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun DockedNavTab(
    item: NavDestinationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(item.labelRes)
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
