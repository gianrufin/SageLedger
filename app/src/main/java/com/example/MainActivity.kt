package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SageLedgerTheme
import com.example.ui.viewmodel.LedgerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_REPORTS = "reports"
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

        // Configure App Check with launch intent before rendering UI or creating ViewModels
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
    onConsumeDeepLink: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedLedgerShell(
                currentUserId = user.uid,
                deepLinkTransactionId = deepLinkTransactionId,
                onConsumeDeepLink = onConsumeDeepLink
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedLedgerShell(
    currentUserId: String,
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
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
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            val titleText = when (currentRoute) {
                                ROUTE_REPORTS -> "Spending Reports"
                                ROUTE_SETTINGS -> "Preferences"
                                else -> stringResource(R.string.app_name)
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                floatingActionButton = {
                    if (currentRoute != ROUTE_SETTINGS) {
                        FloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                showManualEntrySheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .springPressScale()
                                .testTag("fab_add_expense")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Record purchase or cash spend"
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
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
                }
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
                                onManualEntryClick = {
                                    editingTransaction = null
                                    showManualEntrySheet = true
                                },
                                onDismissUpdate = { perm -> viewModel.dismissUpdateNotice(perm) }
                            )
                        }

                        composable(ROUTE_REPORTS) {
                            ReportsScreen(
                                state = reportState,
                                selectedPeriod = selectedPeriod,
                                currencyCode = currencyCode,
                                onSelectPeriod = { viewModel.selectPeriod(it) },
                                onSelectCustomRange = { s, e -> viewModel.setCustomDateRange(s, e) }
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
