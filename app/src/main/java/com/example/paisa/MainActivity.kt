package com.example.paisa

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisa.data.backup.BackupManager
import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.data.preferences.PaisaPreferences
import com.example.paisa.data.preferences.UserPreferences
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.notification.TransactionNotificationHelper
import com.example.paisa.sms.SmsPreferences
import com.example.paisa.ui.components.AccountFormSheet
import com.example.paisa.ui.components.AddTransactionSheet
import com.example.paisa.ui.components.DeleteAccountConfirmDialog
import com.example.paisa.ui.components.PaisaBottomBar
import com.example.paisa.ui.components.PaisaTopBar
import com.example.paisa.ui.components.ReconcileBalanceDialog
import com.example.paisa.ui.components.SettingsSheet
import com.example.paisa.ui.components.TransactionDetailSheet
import com.example.paisa.ui.navigation.NavDestination
import com.example.paisa.ui.screens.AccountsScreen
import com.example.paisa.ui.screens.HistoryScreen
import com.example.paisa.ui.screens.HomeScreen
import com.example.paisa.ui.screens.OnboardingScreen
import com.example.paisa.ui.theme.PaisaTheme
import com.example.paisa.ui.viewmodel.AccountsViewModel
import com.example.paisa.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.paisa.ui.viewmodel.HomeViewModel
import com.example.paisa.ui.viewmodel.PaisaViewModelFactory
import com.example.paisa.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaisaTheme {
                PaisaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaApp() {
    val context = LocalContext.current
    val database = remember(context) { PaisaDatabase.getInstance(context) }
    val repository = remember(database) {
        ExpenseTrackerRepository(
            bankAccountDao = database.bankAccountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            database = database
        )
    }
    val viewModelFactory = remember(context) { PaisaViewModelFactory.create(context) }

    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val accountsViewModel: AccountsViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val transactionViewModel: TransactionViewModel = viewModel(factory = viewModelFactory)

    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val accountsUiState by accountsViewModel.uiState.collectAsStateWithLifecycle()
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val transactionAccounts by transactionViewModel.accounts.collectAsStateWithLifecycle()
    val transactionCategories by transactionViewModel.categories.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(NavDestination.HOME) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val paisaPreferences = remember { PaisaPreferences.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val userPreferences by paisaPreferences.userPreferencesFlow.collectAsStateWithLifecycle(
        initialValue = UserPreferences(
            onboardingCompleted = SmsPreferences.isOnboardingCompleted(context),
            smsDetectionEnabled = SmsPreferences.isSmsDetectionEnabled(context),
            notificationsEnabled = SmsPreferences.isNotificationsEnabled(context),
            manualOnlyMode = SmsPreferences.isManualOnlyMode(context),
            userConsented = SmsPreferences.hasUserConsented(context)
        )
    )

    // Manual-Only Fallback & SMS Detection State
    var isManualOnlyMode by rememberSaveable {
        mutableStateOf(SmsPreferences.isManualOnlyMode(context))
    }

    var isSmsDetectionEnabled by rememberSaveable {
        mutableStateOf(SmsPreferences.isSmsDetectionEnabled(context))
    }

    var isNotificationsEnabled by rememberSaveable {
        mutableStateOf(SmsPreferences.isNotificationsEnabled(context))
    }

    var isSmsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Ensure Notification Channel is created on launch and permissions are reconciled immediately
    LaunchedEffect(Unit) {
        TransactionNotificationHelper.createNotificationChannel(context)

        val smsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!smsGranted && isSmsDetectionEnabled) {
            isSmsDetectionEnabled = false
            paisaPreferences.setSmsDetectionEnabled(false)
        }

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!notifGranted && isNotificationsEnabled) {
            isNotificationsEnabled = false
            paisaPreferences.setNotificationsEnabled(false)
        }
    }

    // Discrete permission launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
        isNotificationsEnabled = isGranted
        coroutineScope.launch {
            paisaPreferences.setNotificationsEnabled(isGranted)
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isSmsPermissionGranted = isGranted
        isSmsDetectionEnabled = isGranted
        coroutineScope.launch {
            paisaPreferences.setSmsDetectionEnabled(isGranted)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val summary = BackupManager.exportBackup(
                            outputStream = outputStream,
                            accounts = repository.getAllAccounts(),
                            categories = repository.getAllCategories(),
                            transactions = repository.getAllTransactionsRaw(),
                            userPreferences = userPreferences
                        )
                        Toast.makeText(
                            context,
                            "Backup saved: ${summary.accountsCount} accounts, ${summary.transactionsCount} transactions",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    // Recheck permission state when returning to the app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val smsGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
                isSmsPermissionGranted = smsGranted

                if (!smsGranted && isSmsDetectionEnabled) {
                    isSmsDetectionEnabled = false
                    coroutineScope.launch {
                        paisaPreferences.setSmsDetectionEnabled(false)
                    }
                }

                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
                isNotificationPermissionGranted = notifGranted

                if (!notifGranted && isNotificationsEnabled) {
                    isNotificationsEnabled = false
                    coroutineScope.launch {
                        paisaPreferences.setNotificationsEnabled(false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onToggleManualOnlyMode: (Boolean) -> Unit = { manualOnly ->
        isManualOnlyMode = manualOnly
        if (manualOnly) {
            isSmsDetectionEnabled = false
            isNotificationsEnabled = false
        }
        coroutineScope.launch {
            paisaPreferences.setManualOnlyMode(manualOnly)
        }
    }

    val onToggleSmsDetection: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            )
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                isSmsPermissionGranted = true
                isSmsDetectionEnabled = true
                coroutineScope.launch {
                    paisaPreferences.setSmsDetectionEnabled(true)
                }
            } else {
                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
        } else {
            isSmsDetectionEnabled = false
            coroutineScope.launch {
                paisaPreferences.setSmsDetectionEnabled(false)
            }
        }
    }

    val onToggleNotifications: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    isNotificationPermissionGranted = true
                    isNotificationsEnabled = true
                    coroutineScope.launch {
                        paisaPreferences.setNotificationsEnabled(true)
                    }
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                isNotificationPermissionGranted = true
                isNotificationsEnabled = true
                coroutineScope.launch {
                    paisaPreferences.setNotificationsEnabled(true)
                }
            }
        } else {
            isNotificationsEnabled = false
            coroutineScope.launch {
                paisaPreferences.setNotificationsEnabled(false)
            }
        }
    }

    // Form and Dialog States
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionWithDetails?>(null) }
    val addTransactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transactionForDetail by remember { mutableStateOf<TransactionWithDetails?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAccountFormSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountWithBalance?>(null) }
    val accountFormSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var accountToReconcile by remember { mutableStateOf<AccountWithBalance?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountWithBalance?>(null) }
    var transactionCountForDelete by remember { mutableIntStateOf(0) }

    if (!userPreferences.onboardingCompleted) {
        OnboardingScreen(
            isSmsPermissionGranted = isSmsPermissionGranted,
            isNotificationPermissionGranted = isNotificationPermissionGranted,
            onRequestSmsPermission = {
                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onCompleteOnboarding = { smsEnabled, notifEnabled ->
                isSmsDetectionEnabled = smsEnabled
                isNotificationsEnabled = notifEnabled
                coroutineScope.launch {
                    paisaPreferences.setSmsDetectionEnabled(smsEnabled)
                    paisaPreferences.setNotificationsEnabled(notifEnabled)
                    paisaPreferences.setOnboardingCompleted(true)
                }
            }
        )
    } else {
        Scaffold(
        topBar = {
            val topBarSubtitle = when (currentDestination) {
                NavDestination.HOME -> com.example.paisa.util.DateFormatter.formatCurrentMonthYear()
                else -> currentDestination.topBarSubtitle
            }
            PaisaTopBar(
                title = currentDestination.topBarTitle,
                subtitle = topBarSubtitle,
                onSettingsClick = {
                    showSettingsSheet = true
                }
            )
        },
        bottomBar = {
            PaisaBottomBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    currentDestination = destination
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    transactionToEdit = null
                    showAddTransactionSheet = true
                },
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "New Transaction",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                NavDestination.HOME -> {
                    HomeScreen(
                        uiState = homeUiState,
                        onNavigateToHistory = {
                            currentDestination = NavDestination.HISTORY
                        },
                        onTransactionClick = { tx ->
                            transactionForDetail = tx
                        }
                    )
                }
                NavDestination.ACCOUNTS -> {
                    AccountsScreen(
                        uiState = accountsUiState,
                        onAddAccountClick = {
                            accountToEdit = null
                            showAccountFormSheet = true
                        },
                        onEditAccountClick = { account ->
                            accountToEdit = account
                            showAccountFormSheet = true
                        },
                        onReconcileAccountClick = { account ->
                            accountToReconcile = account
                        }
                    )
                }
                NavDestination.HISTORY -> {
                    HistoryScreen(
                        uiState = historyUiState,
                        onFilterSelected = { filter ->
                            historyViewModel.setFilter(filter)
                        },
                        onTransactionClick = { tx ->
                            transactionForDetail = tx
                        }
                    )
                }
            }
        }

        // Add / Edit Transaction Bottom Sheet
        if (showAddTransactionSheet) {
            AddTransactionSheet(
                sheetState = addTransactionSheetState,
                accounts = transactionAccounts,
                categories = transactionCategories,
                initialTransaction = transactionToEdit,
                onDismissRequest = {
                    coroutineScope.launch {
                        addTransactionSheetState.hide()
                    }.invokeOnCompletion {
                        showAddTransactionSheet = false
                        transactionToEdit = null
                    }
                },
                onSaveTransaction = { accountId, amount, type, categoryId, timestamp, note, onComplete ->
                    if (transactionToEdit != null) {
                        transactionViewModel.validateAndUpdateTransaction(
                            transactionId = transactionToEdit!!.id,
                            accountId = accountId,
                            amountRupeesInput = amount,
                            type = type,
                            categoryId = categoryId,
                            timestamp = timestamp,
                            note = note,
                            onComplete = { result ->
                                onComplete(result)
                                if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Success) {
                                    coroutineScope.launch {
                                        addTransactionSheetState.hide()
                                    }.invokeOnCompletion {
                                        showAddTransactionSheet = false
                                        transactionToEdit = null
                                    }
                                }
                            }
                        )
                    } else {
                        transactionViewModel.validateAndAddTransaction(
                            accountId = accountId,
                            amountRupeesInput = amount,
                            type = type,
                            categoryId = categoryId,
                            timestamp = timestamp,
                            note = note,
                            onComplete = { result ->
                                onComplete(result)
                                if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Success) {
                                    coroutineScope.launch {
                                        addTransactionSheetState.hide()
                                    }.invokeOnCompletion {
                                        showAddTransactionSheet = false
                                        transactionToEdit = null
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }

        // Transaction Details Bottom Sheet
        transactionForDetail?.let { tx ->
            TransactionDetailSheet(
                sheetState = detailSheetState,
                transaction = tx,
                categories = transactionCategories,
                onDismissRequest = {
                    coroutineScope.launch {
                        detailSheetState.hide()
                    }.invokeOnCompletion {
                        transactionForDetail = null
                    }
                },
                onCategoryChanged = { newCategoryId ->
                    transactionViewModel.updateTransactionCategory(tx.id, newCategoryId)
                    val catName = transactionCategories.firstOrNull { it.id == newCategoryId }?.name
                    if (catName != null) {
                        val merchant = tx.note ?: tx.reference
                        com.example.paisa.sms.CategorySuggestionEngine.getInstance(context).learn(
                            merchantOrPayee = merchant,
                            categoryName = catName,
                            amountPaise = tx.amountPaise
                        )
                    }
                    coroutineScope.launch {
                        detailSheetState.hide()
                    }.invokeOnCompletion {
                        transactionForDetail = null
                    }
                },
                onEditRequest = {
                    val currentTx = tx
                    coroutineScope.launch {
                        detailSheetState.hide()
                    }.invokeOnCompletion {
                        transactionForDetail = null
                        transactionToEdit = currentTx
                        showAddTransactionSheet = true
                    }
                },
                onDeleteConfirmed = {
                    transactionViewModel.deleteTransaction(tx.id)
                    coroutineScope.launch {
                        detailSheetState.hide()
                    }.invokeOnCompletion {
                        transactionForDetail = null
                    }
                }
            )
        }

        // Account Form Sheet (Add / Edit)
        if (showAccountFormSheet) {
            AccountFormSheet(
                sheetState = accountFormSheetState,
                accountToEdit = accountToEdit,
                onDismissRequest = {
                    coroutineScope.launch {
                        accountFormSheetState.hide()
                    }.invokeOnCompletion {
                        showAccountFormSheet = false
                        accountToEdit = null
                    }
                },
                onSave = { id, bankName, nickname, lastFour, balance, type, isPrimary, bankIdentifier, themeId, onComplete ->
                    accountsViewModel.validateAndSaveAccount(
                        id = id,
                        bankName = bankName,
                        nickname = nickname,
                        lastFourDigits = lastFour,
                        openingBalanceRupeesInput = balance,
                        accountType = type,
                        isPrimary = isPrimary,
                        bankIdentifier = bankIdentifier,
                        themeId = themeId,
                        onComplete = { result ->
                            onComplete(result)
                            if (result is com.example.paisa.ui.viewmodel.AccountValidationResult.Success) {
                                coroutineScope.launch {
                                    accountFormSheetState.hide()
                                }.invokeOnCompletion {
                                    showAccountFormSheet = false
                                    accountToEdit = null
                                }
                            }
                        }
                    )
                },
                onDeleteRequest = { accountId ->
                    accountsViewModel.getTransactionCountForAccount(accountId) { count ->
                        transactionCountForDelete = count
                        accountToDelete = accountToEdit
                        coroutineScope.launch {
                            accountFormSheetState.hide()
                        }.invokeOnCompletion {
                            showAccountFormSheet = false
                            accountToEdit = null
                        }
                    }
                }
            )
        }

        // Safe Delete Account Dialog with Explicit Confirmation
        accountToDelete?.let { account ->
            DeleteAccountConfirmDialog(
                accountWithBalance = account,
                transactionCount = transactionCountForDelete,
                onConfirmDelete = {
                    accountsViewModel.deleteAccount(account.account.id) {
                        accountToDelete = null
                        transactionCountForDelete = 0
                    }
                },
                onDismissRequest = {
                    accountToDelete = null
                    transactionCountForDelete = 0
                }
            )
        }

        // Reconcile Balance Dialog
        accountToReconcile?.let { account ->
            ReconcileBalanceDialog(
                accountWithBalance = account,
                onSaveReconciliation = { accountId, rupees, onComplete ->
                    accountsViewModel.reconcileBalance(accountId, rupees, onComplete)
                },
                onDismissRequest = {
                    accountToReconcile = null
                }
            )
        }

        // Settings Sheet
        if (showSettingsSheet) {
            SettingsSheet(
                sheetState = settingsSheetState,
                isManualOnlyMode = isManualOnlyMode,
                onToggleManualOnlyMode = onToggleManualOnlyMode,
                isSmsDetectionEnabled = isSmsDetectionEnabled,
                onToggleSmsDetection = onToggleSmsDetection,
                isNotificationsEnabled = isNotificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                onExportBackup = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportBackupLauncher.launch("paisa_backup_$timestamp.json")
                },
                onRestoreBackup = {
                    restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                },
                onClearData = {
                    showClearDataConfirmDialog = true
                },
                onDismissRequest = {
                    coroutineScope.launch {
                        settingsSheetState.hide()
                    }.invokeOnCompletion {
                        showSettingsSheet = false
                    }
                }
            )
        }

        // Restore Confirmation Dialog
        pendingRestoreUri?.let { uri ->
            AlertDialog(
                onDismissRequest = { pendingRestoreUri = null },
                title = {
                    Text(
                        text = "Restore Data Backup?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "Restoring will replace all current bank accounts and transactions with the records from the backup file. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetUri = uri
                            pendingRestoreUri = null
                            coroutineScope.launch {
                                try {
                                    context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                                        val parsed = BackupManager.parseBackup(inputStream)
                                        repository.restoreDatabase(
                                            accounts = parsed.accounts,
                                            categories = parsed.categories,
                                            transactions = parsed.transactions
                                        )
                                        parsed.preferences?.let { prefs ->
                                            paisaPreferences.setSmsDetectionEnabled(prefs.smsDetectionEnabled)
                                            paisaPreferences.setNotificationsEnabled(prefs.notificationsEnabled)
                                            paisaPreferences.setManualOnlyMode(prefs.manualOnlyMode)
                                        }
                                        Toast.makeText(
                                            context,
                                            "Restored: ${parsed.accounts.size} accounts, ${parsed.transactions.size} transactions",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Restore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestoreUri = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Clear All Data Confirmation Dialog
        if (showClearDataConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataConfirmDialog = false },
                title = {
                    Text(
                        text = "Clear All Records?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "This will permanently delete all bank accounts and transactions from your device. Your balance will reset to zero.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDataConfirmDialog = false
                            coroutineScope.launch {
                                repository.clearAllData()
                                Toast.makeText(context, "All financial records cleared", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
}


