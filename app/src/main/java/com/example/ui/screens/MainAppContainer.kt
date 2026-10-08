package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.data.entity.ExpenseEntity
import com.example.ui.components.AddEditExpenseSheet
import com.example.ui.components.AddEditIncomeSheet
import com.example.ui.components.DetectedSmsReviewDialog
import com.example.ui.components.ReactionDialog
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: MainViewModel) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isLocked by viewModel.isAppLocked.collectAsState()
    val lastReaction by viewModel.lastReactionMessage.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pendingSmsTransactions by viewModel.pendingSmsTransactions.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true
        if (granted) {
            viewModel.checkAppOpenSms()
        }
    }

    // Check SMS automatically when app opens if user has completed onboarding
    LaunchedEffect(userSettings.onboardingCompleted, isLocked) {
        if (userSettings.onboardingCompleted && !isLocked && userSettings.autoSmsDetectionEnabled) {
            val hasReadSms = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasReadSms) {
                viewModel.checkAppOpenSms()
            } else {
                // Politely ask for SMS permission so auto-detection can operate
                smsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            }
        }
    }

    // Bottom sheet states
    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isAddIncomeOpen by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var preselectedExpenseDate by remember { mutableStateOf<String?>(null) }

    val expenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val incomeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Check App Lock
    if (isLocked) {
        PinLockScreen(
            onUnlockAttempt = { pin ->
                viewModel.unlockApp(pin)
            }
        )
        return
    }

    // Check First Launch / Onboarding
    if (!userSettings.onboardingCompleted) {
        OnboardingScreen(
            onFinish = { salary, cycleType, cycleDay, userName ->
                viewModel.completeOnboarding(salary, cycleType, cycleDay, userName)
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.Search && currentScreen != Screen.MonthlyTransactions) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_tab_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.SmsSync,
                        onClick = { currentScreen = Screen.SmsSync },
                        icon = { Icon(Icons.Default.Sms, contentDescription = "Bank SMS") },
                        label = { Text("Bank SMS") },
                        modifier = Modifier.testTag("nav_tab_sms")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Analytics,
                        onClick = { currentScreen = Screen.Analytics },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.History,
                        onClick = { currentScreen = Screen.History },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenAddExpense = {
                            editingExpense = null
                            preselectedExpenseDate = null
                            isAddExpenseOpen = true
                        },
                        onOpenAddIncome = { isAddIncomeOpen = true },
                        onOpenSearch = { currentScreen = Screen.Search },
                        onOpenSmsSync = { currentScreen = Screen.SmsSync },
                        onEditExpense = { exp ->
                            editingExpense = exp
                            isAddExpenseOpen = true
                        },
                        onOpenMonthlyTransactions = {
                            currentScreen = Screen.MonthlyTransactions
                        }
                    )
                }
                Screen.Calendar -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onAddExpenseForDate = { dateStr ->
                            editingExpense = null
                            preselectedExpenseDate = dateStr
                            isAddExpenseOpen = true
                        },
                        onEditExpense = { exp ->
                            editingExpense = exp
                            isAddExpenseOpen = true
                        }
                    )
                }
                Screen.Analytics -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                Screen.History -> {
                    HistoryScreen(viewModel = viewModel)
                }
                Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenSmsSync = { currentScreen = Screen.SmsSync }
                    )
                }
                Screen.Search -> {
                    SearchFilterScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Home },
                        onEditExpense = { exp ->
                            editingExpense = exp
                            isAddExpenseOpen = true
                        }
                    )
                }
                Screen.SmsSync -> {
                    SmsBankSyncScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                Screen.MonthlyTransactions -> {
                    MonthlyTransactionDetailsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                else -> {}
            }

            // Funny Reaction Popup (shows after completing SMS review or adding expense)
            if (lastReaction != null && pendingSmsTransactions.isEmpty()) {
                ReactionDialog(
                    message = lastReaction,
                    onDismiss = { viewModel.dismissReaction() }
                )
            }

            // Interactive Review & Description Dialog for Today's Detected Bank SMS
            if (pendingSmsTransactions.isNotEmpty()) {
                DetectedSmsReviewDialog(
                    pendingItems = pendingSmsTransactions,
                    categories = categories,
                    currencySymbol = userSettings.currencySymbol,
                    onConfirm = { item, description, categoryId ->
                        viewModel.confirmDetectedTransaction(item, description, categoryId)
                    },
                    onSkip = { item ->
                        viewModel.dismissDetectedTransaction(item)
                    },
                    onDismissAll = {
                        viewModel.dismissAllPendingTransactions()
                    }
                )
            }

            // Add/Edit Expense Bottom Sheet
            if (isAddExpenseOpen) {
                AddEditExpenseSheet(
                    sheetState = expenseSheetState,
                    categories = categories,
                    currencySymbol = userSettings.currencySymbol,
                    editingExpense = editingExpense?.copy(
                        date = preselectedExpenseDate ?: editingExpense?.date ?: ""
                    ) ?: if (preselectedExpenseDate != null) ExpenseEntity(
                        amount = 0.0,
                        categoryId = categories.firstOrNull()?.id ?: 1L,
                        date = preselectedExpenseDate!!,
                        time = "",
                        paymentMethod = "UPI"
                    ) else null,
                    onDismiss = { isAddExpenseOpen = false },
                    onSave = { amount, categoryId, date, time, paymentMethod, note ->
                        if (editingExpense != null) {
                            viewModel.updateExpense(
                                id = editingExpense!!.id,
                                amount = amount,
                                categoryId = categoryId,
                                date = date,
                                time = time,
                                paymentMethod = paymentMethod,
                                note = note
                            )
                        } else {
                            viewModel.addExpense(
                                amount = amount,
                                categoryId = categoryId,
                                date = date,
                                time = time,
                                paymentMethod = paymentMethod,
                                note = note
                            )
                        }
                        isAddExpenseOpen = false
                    }
                )
            }

            // Add Income Bottom Sheet
            if (isAddIncomeOpen) {
                AddEditIncomeSheet(
                    sheetState = incomeSheetState,
                    currencySymbol = userSettings.currencySymbol,
                    onDismiss = { isAddIncomeOpen = false },
                    onSave = { amount, type, date, note ->
                        viewModel.addIncome(amount, type, date, note)
                        isAddIncomeOpen = false
                    }
                )
            }
        }
    }
}
