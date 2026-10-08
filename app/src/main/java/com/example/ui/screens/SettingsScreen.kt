package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.RecurringExpenseEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenSmsSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringExpenses by viewModel.recurringExpenses.collectAsState()

    // Dialog states
    var showNameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSalaryDialog by remember { mutableStateOf(false) }
    var showSavingsGoalDialog by remember { mutableStateOf(false) }
    var showAppLockDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showManageRecurringDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    // Inputs for dialogs
    var nameInput by remember { mutableStateOf(settings.userName) }
    var salaryInput by remember { mutableStateOf(settings.defaultSalary.toLong().toString()) }
    var savingsGoalInput by remember { mutableStateOf(settings.monthlySavingsGoal.let { if (it > 0) it.toLong().toString() else "" }) }
    var pinInput by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }

    // Add category inputs
    var newCatName by remember { mutableStateOf("") }
    var newCatBudget by remember { mutableStateOf("") }

    // Add recurring inputs
    var recName by remember { mutableStateOf("") }
    var recAmount by remember { mutableStateOf("") }
    var recDay by remember { mutableIntStateOf(5) }
    var recCategory by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings ⚙️",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val isSystemDark = isSystemInDarkTheme()
                val isDark = when (settings.themeMode) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> isSystemDark
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    IconButton(
                        onClick = {
                            viewModel.toggleDayNightTheme(isSystemDark)
                            Toast.makeText(
                                context,
                                if (isDark) "Switched to Day Mode ☀️" else "Switched to Night Mode 🌙",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("settings_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Day Mode" else "Night Mode",
                            tint = if (isDark) WarningOrange else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Profile & Currency
        item {
            SettingsGroup(title = "Profile & Currency") {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Display Name",
                    subtitle = settings.userName,
                    onClick = {
                        nameInput = settings.userName
                        showNameDialog = true
                    }
                )
                SettingsItem(
                    icon = Icons.Default.CurrencyRupee,
                    title = "Currency",
                    subtitle = "${settings.currencySymbol} (Tap to change)",
                    onClick = { showCurrencyDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Savings,
                    title = "Default Monthly Salary",
                    subtitle = CurrencyFormatter.formatIndian(settings.defaultSalary, settings.currencySymbol),
                    onClick = {
                        salaryInput = settings.defaultSalary.toLong().toString()
                        showSalaryDialog = true
                    }
                )
            }
        }

        // Comedy & Reaction Controls
        item {
            SettingsGroup(title = "Funny Personality & Roast Mode") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Funny Reactions", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Enable sarcastic Tamil/Tanglish reactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.funnyReactionsEnabled,
                        onCheckedChange = { scope.launch { viewModel.preferencesRepository.setFunnyReactionsEnabled(it) } }
                    )
                }

                if (settings.funnyReactionsEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Reaction Intensity:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SOFT" to "Soft 😇", "NORMAL" to "Normal 😎", "ROAST" to "Roast Me 😂").forEach { (key, label) ->
                                FilterChip(
                                    selected = settings.reactionIntensity == key,
                                    onClick = { scope.launch { viewModel.preferencesRepository.setReactionIntensity(key) } },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Budgeting & Goals
        item {
            SettingsGroup(title = "Budgeting & Recurring Expenses") {
                SettingsItem(
                    icon = Icons.Default.Savings,
                    title = "Monthly Savings Goal",
                    subtitle = if (settings.monthlySavingsGoal > 0)
                        CurrencyFormatter.formatIndian(settings.monthlySavingsGoal, settings.currencySymbol)
                    else "Not set (Tap to set goal)",
                    onClick = { showSavingsGoalDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Repeat,
                    title = "Manage Recurring Expenses",
                    subtitle = "${recurringExpenses.size} items scheduled (Rent, EMI, Netflix)",
                    onClick = { showManageRecurringDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "Manage Categories & Budgets",
                    subtitle = "${categories.size} categories configured",
                    onClick = { showAddCategoryDialog = true }
                )
            }
        }

        // Theme & App Lock
        item {
            SettingsGroup(title = "Appearance & Security") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { scope.launch { viewModel.preferencesRepository.setThemeMode(mode) } },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (PIN)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.appLockEnabled) "Enabled (PIN set)" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showAppLockDialog = true
                            } else {
                                scope.launch { viewModel.preferencesRepository.setAppLock(false, "") }
                            }
                        }
                    )
                }
            }
        }

        // Bank SMS Sync & Automation
        item {
            SettingsGroup(title = "Bank SMS Automation") {
                SettingsItem(
                    icon = Icons.Default.Sms,
                    title = "Bank SMS Reader & Sync",
                    subtitle = "View detected transactions, test parser & scan inbox",
                    onClick = onOpenSmsSync
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-detect Bank SMS",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (settings.autoSmsDetectionEnabled) {
                                "Automatically subtracts debit and adds credit to balance"
                            } else {
                                "Disabled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoSmsDetectionEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleAutoSmsDetection(enabled)
                        }
                    )
                }
            }
        }

        // Backup, Restore & Reset
        item {
            SettingsGroup(title = "Offline Backup & Data") {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Export Backup (JSON)",
                    subtitle = "Copy complete offline database backup",
                    onClick = {
                        scope.launch {
                            val json = viewModel.exportDataJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MichamEvloBackup", json))
                            Toast.makeText(context, "Backup copied to clipboard! Save safely.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Export as CSV",
                    subtitle = "Export expenses spreadsheet to clipboard",
                    onClick = {
                        scope.launch {
                            val csv = viewModel.exportDataCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MichamEvloCSV", csv))
                            Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_LONG).show()
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Upload,
                    title = "Restore from JSON",
                    subtitle = "Import previously exported backup",
                    onClick = { showRestoreDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Reset All Data",
                    subtitle = "Clear all expenses and restore defaults",
                    titleColor = SpentRed,
                    onClick = { showResetConfirmDialog = true }
                )
            }
        }

        // Privacy & About
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👛 Micham Evlo v1.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“Your money data stays on your device.”",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = IncomeGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% Offline • No accounts • No tracking • No ads",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Name Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Display Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name / Nickname") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.preferencesRepository.setUserName(nameInput.ifBlank { "Bro" }) }
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("₹" to "INR (₹)", "$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)", "AED" to "AED", "SGD" to "SGD")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    currencies.forEach { (sym, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { viewModel.preferencesRepository.setCurrencySymbol(sym) }
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = settings.currencySymbol == sym, onClick = null)
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Salary Dialog
    if (showSalaryDialog) {
        AlertDialog(
            onDismissRequest = { showSalaryDialog = false },
            title = { Text("Default Monthly Salary") },
            text = {
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monthly Salary") },
                    prefix = { Text("${settings.currencySymbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = salaryInput.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        scope.launch {
                            viewModel.preferencesRepository.updateSalaryConfig(
                                amount,
                                settings.salaryCycleType,
                                settings.salaryCycleDay
                            )
                        }
                    }
                    showSalaryDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSalaryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Savings Goal Dialog
    if (showSavingsGoalDialog) {
        AlertDialog(
            onDismissRequest = { showSavingsGoalDialog = false },
            title = { Text("Monthly Savings Goal") },
            text = {
                Column {
                    Text(
                        text = "Set a target savings amount you want left at month end (e.g. ₹10,000).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = savingsGoalInput,
                        onValueChange = { savingsGoalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Goal Amount") },
                        prefix = { Text("${settings.currencySymbol} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val goal = savingsGoalInput.toDoubleOrNull() ?: 0.0
                    viewModel.setSavingsGoal(goal)
                    showSavingsGoalDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavingsGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // App Lock PIN Dialog
    if (showAppLockDialog) {
        AlertDialog(
            onDismissRequest = { showAppLockDialog = false },
            title = { Text("Set 4-Digit App Lock PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure your financial data locally.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput.length == 4) {
                        scope.launch { viewModel.preferencesRepository.setAppLock(true, pinInput) }
                        showAppLockDialog = false
                        Toast.makeText(context, "App Lock enabled!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showAppLockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore from JSON Backup") },
            text = {
                Column {
                    Text("Paste your exported JSON backup text below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("JSON text") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreFromJson(
                        jsonStr = restoreJsonInput,
                        onSuccess = {
                            showRestoreDialog = false
                            Toast.makeText(context, "Restore complete!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Restore failed: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Data?") },
            text = {
                Text("This will delete all expenses, custom categories, and reset all settings to defaults. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetConfirmDialog = false
                    Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                }) { Text("Reset", color = SpentRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Manage Categories Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Manage Categories") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Add Custom Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newCatBudget,
                        onValueChange = { newCatBudget = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Monthly Budget (Optional)") },
                        prefix = { Text("${settings.currencySymbol} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newCatName.isNotBlank()) {
                                val budget = newCatBudget.toDoubleOrNull() ?: 0.0
                                viewModel.addCategory(newCatName, "more_horiz", "#8B5CF6", budget)
                                newCatName = ""
                                newCatBudget = ""
                                Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Category")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Done") }
            }
        )
    }

    // Manage Recurring Expenses Dialog
    if (showManageRecurringDialog) {
        AlertDialog(
            onDismissRequest = { showManageRecurringDialog = false },
            title = { Text("Recurring Expenses") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Add Schedule (Rent, EMI, Netflix):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = recName,
                        onValueChange = { recName = it },
                        label = { Text("Name (e.g. House Rent)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = recAmount,
                        onValueChange = { recAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount") },
                        prefix = { Text("${settings.currencySymbol} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recurring Day: ${recDay}th", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { if (recDay > 1) recDay-- }) { Text("-", fontSize = 18.sp) }
                        IconButton(onClick = { if (recDay < 31) recDay++ }) { Text("+", fontSize = 18.sp) }
                    }
                    Button(
                        onClick = {
                            val amount = recAmount.toDoubleOrNull()
                            if (recName.isNotBlank() && amount != null && amount > 0) {
                                viewModel.addRecurringExpense(recName, amount, recCategory, "UPI", recDay)
                                recName = ""
                                recAmount = ""
                                Toast.makeText(context, "Recurring expense added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Recurring Expense")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageRecurringDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor != MaterialTheme.colorScheme.onSurface) titleColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
