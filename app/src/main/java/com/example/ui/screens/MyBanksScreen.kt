package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BankEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import com.example.utils.SmsScanSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBanksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allBanks by viewModel.allBanks.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allIncomes by viewModel.allIncomes.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: SMS Tracking Switches, 1: Bank Dashboard
    var showAddBankDialog by remember { mutableStateOf(false) }
    var selectedBankForDashboard by remember { mutableStateOf<BankEntity?>(null) } // null = All Banks
    var selectedFilterType by remember { mutableStateOf("ALL") } // ALL, CREDITS, DEBITS, UPI, EMI, SUBSCRIPTIONS, CREDIT_CARDS
    var selectedAccountFilter by remember { mutableStateOf<String?>(null) } // null = All Accounts

    var isImportingHistoryForBank by remember { mutableStateOf<BankEntity?>(null) }
    var importResultSummary by remember { mutableStateOf<SmsScanSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Banks & Tracking 🏦",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Individual SMS switches & Bank ledgers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("my_banks_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddBankDialog = true },
                        modifier = Modifier.testTag("add_bank_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bank",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: SMS Tracking Controls vs Bank Dashboard
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("SMS Tracking Switches") },
                    icon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_tracking_switches")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Bank Dashboard") },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_bank_dashboard")
                )
            }

            if (selectedTabIndex == 0) {
                // TAB 1: SMS TRACKING SWITCHES
                BankTrackingSwitchesSection(
                    banks = allBanks,
                    onToggleBank = { bank, isEnabled ->
                        viewModel.toggleBankTracking(bank, isEnabled)
                        Toast.makeText(
                            context,
                            if (isEnabled) "${bank.bankName} SMS tracking turned ON 🟢" else "${bank.bankName} SMS tracking turned OFF ⚪",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenDashboardForBank = { bank ->
                        selectedBankForDashboard = bank
                        selectedTabIndex = 1
                    },
                    onImportHistorical = { bank ->
                        isImportingHistoryForBank = bank
                        viewModel.importHistoricalSmsForBank(bank) { summary ->
                            isImportingHistoryForBank = null
                            importResultSummary = summary
                        }
                    },
                    onDeleteBank = { bank ->
                        viewModel.deleteBank(bank)
                        Toast.makeText(context, "${bank.bankName} removed", Toast.LENGTH_SHORT).show()
                    },
                    onAddNewBank = { showAddBankDialog = true }
                )
            } else {
                // TAB 2: BANK-WISE TRANSACTION DASHBOARD
                BankWiseDashboardSection(
                    allBanks = allBanks,
                    selectedBank = selectedBankForDashboard,
                    onSelectBank = { bank -> selectedBankForDashboard = bank },
                    allExpenses = allExpenses,
                    allIncomes = allIncomes,
                    currencySymbol = settings.currencySymbol,
                    selectedFilterType = selectedFilterType,
                    onFilterChange = { selectedFilterType = it },
                    selectedAccount = selectedAccountFilter,
                    onAccountChange = { selectedAccountFilter = it },
                    onToggleBankTracking = { bank, isEnabled ->
                        viewModel.toggleBankTracking(bank, isEnabled)
                    },
                    onImportPrevious = { bank ->
                        isImportingHistoryForBank = bank
                        viewModel.importHistoricalSmsForBank(bank) { summary ->
                            isImportingHistoryForBank = null
                            importResultSummary = summary
                        }
                    }
                )
            }
        }
    }

    // Add Bank Dialog
    if (showAddBankDialog) {
        AddBankDialog(
            existingBanks = allBanks,
            onDismiss = { showAddBankDialog = false },
            onAddBank = { name, patterns, color ->
                viewModel.addCustomBank(name, patterns, color)
                showAddBankDialog = false
                Toast.makeText(context, "$name added (Tracking defaults to OFF)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Import Historical SMS Loading Dialog
    if (isImportingHistoryForBank != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Importing Bank SMS 📥") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Scanning SMS inbox for ${isImportingHistoryForBank?.bankName} transactions...")
                }
            },
            confirmButton = {}
        )
    }

    // Import Result Summary Dialog
    if (importResultSummary != null) {
        val s = importResultSummary!!
        AlertDialog(
            onDismissRequest = { importResultSummary = null },
            title = { Text("Import Complete ✅") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Scanned Messages: ${s.totalScanned}")
                    Text("Debits Imported: ${s.debitsAdded} (${CurrencyFormatter.formatIndian(s.totalDebitAmount, settings.currencySymbol)})", color = SpentRed)
                    Text("Credits Imported: ${s.creditsAdded} (${CurrencyFormatter.formatIndian(s.totalCreditAmount, settings.currencySymbol)})", color = IncomeGreen)
                    Text("Duplicates Safely Skipped: ${s.duplicatesSkipped}")
                    Text("Ignored/Non-transactions: ${s.nonTransactionsIgnored}")
                }
            },
            confirmButton = {
                Button(onClick = { importResultSummary = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun BankTrackingSwitchesSection(
    banks: List<BankEntity>,
    onToggleBank: (BankEntity, Boolean) -> Unit,
    onOpenDashboardForBank: (BankEntity) -> Unit,
    onImportHistorical: (BankEntity) -> Unit,
    onDeleteBank: (BankEntity) -> Unit,
    onAddNewBank: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Header Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "My Banks — SMS Tracking Controls",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Turn ON to auto-track bank debits, credits & credit cards. Turn OFF to ignore new SMS while safely preserving all historical records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(banks, key = { it.id }) { bank ->
            BankTrackingRowCard(
                bank = bank,
                onToggle = { isEnabled -> onToggleBank(bank, isEnabled) },
                onOpenDashboard = { onOpenDashboardForBank(bank) },
                onImportHistorical = { onImportHistorical(bank) },
                onDelete = { onDeleteBank(bank) }
            )
        }

        // Add Bank button card
        item {
            OutlinedButton(
                onClick = onAddNewBank,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_bank_row_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Bank (+)", fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BankTrackingRowCard(
    bank: BankEntity,
    onToggle: (Boolean) -> Unit,
    onOpenDashboard: () -> Unit,
    onImportHistorical: () -> Unit,
    onDelete: () -> Unit
) {
    val bankColor = try {
        Color(android.graphics.Color.parseColor(bank.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_card_${bank.bankCode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bank Avatar
                    Surface(
                        shape = CircleShape,
                        color = bankColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = bank.bankCode.take(2).uppercase(Locale.ENGLISH),
                                fontWeight = FontWeight.Black,
                                color = bankColor,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bank.bankName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (bank.isTrackingEnabled) IncomeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = if (bank.isTrackingEnabled) "ON" else "OFF",
                                    color = if (bank.isTrackingEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = if (bank.isTrackingEnabled) {
                                if (bank.enabledAtTimestamp > 0L) {
                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(bank.enabledAtTimestamp))
                                    "Active since $dateStr"
                                } else "Tracking Active • Auto-detecting SMS"
                            } else {
                                "Tracking Paused • History preserved safely"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Modern sliding toggle switch with animation
                Switch(
                    checked = bank.isTrackingEnabled,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IncomeGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("bank_switch_${bank.bankCode}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenDashboard,
                    modifier = Modifier.testTag("btn_view_ledger_${bank.bankCode}")
                ) {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Bank Ledger", fontSize = 12.sp)
                }

                Row {
                    if (bank.isTrackingEnabled) {
                        TextButton(
                            onClick = onImportHistorical,
                            modifier = Modifier.testTag("btn_import_hist_${bank.bankCode}")
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Previous", fontSize = 12.sp)
                        }
                    }

                    if (bank.isCustom) {
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Bank", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankWiseDashboardSection(
    allBanks: List<BankEntity>,
    selectedBank: BankEntity?,
    onSelectBank: (BankEntity?) -> Unit,
    allExpenses: List<ExpenseEntity>,
    allIncomes: List<IncomeEntity>,
    currencySymbol: String,
    selectedFilterType: String,
    onFilterChange: (String) -> Unit,
    selectedAccount: String?,
    onAccountChange: (String?) -> Unit,
    onToggleBankTracking: (BankEntity, Boolean) -> Unit,
    onImportPrevious: (BankEntity) -> Unit
) {
    // Filter transactions for selected bank
    val filteredExpenses = remember(selectedBank, allExpenses, selectedAccount) {
        allExpenses.filter { exp ->
            val matchesBank = if (selectedBank == null) true else {
                exp.cardName.contains(selectedBank.bankName, ignoreCase = true) ||
                exp.note.contains(selectedBank.bankName, ignoreCase = true) ||
                exp.note.contains(selectedBank.bankCode, ignoreCase = true)
            }
            val matchesAccount = if (selectedAccount == null) true else {
                exp.cardMaskedNumber.contains(selectedAccount, ignoreCase = true) ||
                exp.note.contains(selectedAccount, ignoreCase = true)
            }
            matchesBank && matchesAccount
        }
    }

    val filteredIncomes = remember(selectedBank, allIncomes, selectedAccount) {
        allIncomes.filter { inc ->
            val matchesBank = if (selectedBank == null) true else {
                inc.note.contains(selectedBank.bankName, ignoreCase = true) ||
                inc.note.contains(selectedBank.bankCode, ignoreCase = true)
            }
            val matchesAccount = if (selectedAccount == null) true else {
                inc.cardMaskedNumber.contains(selectedAccount, ignoreCase = true) ||
                inc.note.contains(selectedAccount, ignoreCase = true)
            }
            matchesBank && matchesAccount
        }
    }

    // Accounts list associated with this bank
    val accounts = remember(filteredExpenses, filteredIncomes) {
        val list = mutableSetOf<String>()
        filteredExpenses.forEach {
            if (it.cardMaskedNumber.isNotBlank()) list.add(it.cardMaskedNumber)
        }
        filteredIncomes.forEach {
            if (it.cardMaskedNumber.isNotBlank()) list.add(it.cardMaskedNumber)
        }
        list.toList()
    }

    val totalCredits = filteredIncomes.sumOf { it.amount }
    val bankAccountDebits = filteredExpenses.filter { !it.isCreditCard || it.isBillPayment }.sumOf { it.amount }
    val creditCardSpends = filteredExpenses.filter { it.isCreditCard && !it.isBillPayment }.sumOf { it.amount }

    val combinedItems = remember(filteredExpenses, filteredIncomes, selectedFilterType) {
        val list = mutableListOf<BankTransactionDisplayItem>()
        if (selectedFilterType != "DEBITS" && selectedFilterType != "CREDIT_CARDS") {
            filteredIncomes.forEach { inc ->
                if (selectedFilterType == "ALL" || selectedFilterType == "CREDITS") {
                    list.add(
                        BankTransactionDisplayItem(
                            id = inc.id,
                            isExpense = false,
                            title = inc.incomeType,
                            subtitle = inc.note,
                            amount = inc.amount,
                            date = inc.date,
                            paymentMethod = "Bank Credit",
                            isCreditCard = inc.isCreditCardRefund
                        )
                    )
                }
            }
        }
        if (selectedFilterType != "CREDITS") {
            filteredExpenses.forEach { exp ->
                val matches = when (selectedFilterType) {
                    "ALL" -> true
                    "DEBITS" -> !exp.isCreditCard || exp.isBillPayment
                    "UPI" -> exp.paymentMethod.equals("UPI", ignoreCase = true)
                    "EMI" -> exp.note.contains("EMI", ignoreCase = true)
                    "SUBSCRIPTIONS" -> exp.note.contains("Subscription", ignoreCase = true)
                    "CREDIT_CARDS" -> exp.isCreditCard
                    else -> true
                }
                if (matches) {
                    list.add(
                        BankTransactionDisplayItem(
                            id = exp.id,
                            isExpense = true,
                            title = exp.note.ifBlank { exp.paymentMethod },
                            subtitle = "${exp.paymentMethod} • ${exp.time}",
                            amount = exp.amount,
                            date = exp.date,
                            paymentMethod = exp.paymentMethod,
                            isCreditCard = exp.isCreditCard
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.date }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bank Selector Chips Row
        item {
            Column {
                Text(
                    text = "Select Bank to View Ledger:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBank == null,
                            onClick = { onSelectBank(null) },
                            label = { Text("All Banks 🌐") },
                            modifier = Modifier.testTag("chip_bank_all")
                        )
                    }
                    items(allBanks) { bank ->
                        FilterChip(
                            selected = selectedBank?.id == bank.id,
                            onClick = { onSelectBank(bank) },
                            label = { Text(bank.bankName) },
                            leadingIcon = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (bank.isTrackingEnabled) IncomeGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            },
                            modifier = Modifier.testTag("chip_bank_${bank.bankCode}")
                        )
                    }
                }
            }
        }

        // Active Bank Status Banner & Toggle
        if (selectedBank != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedBank.bankName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selectedBank.isTrackingEnabled) IncomeGreen else MaterialTheme.colorScheme.outline
                                ) {
                                    Text(
                                        text = if (selectedBank.isTrackingEnabled) "TRACKING ON" else "TRACKING OFF",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = if (selectedBank.isTrackingEnabled) "Detecting new bank SMS messages automatically" else "Automatic tracking is OFF. Historical data remains visible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = selectedBank.isTrackingEnabled,
                            onCheckedChange = { onToggleBankTracking(selectedBank, it) },
                            modifier = Modifier.testTag("dashboard_bank_switch_${selectedBank.bankCode}")
                        )
                    }
                }
            }
        }

        // Account Selector (if multiple accounts detected)
        if (accounts.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedAccount == null,
                            onClick = { onAccountChange(null) },
                            label = { Text("All Accounts") }
                        )
                    }
                    items(accounts) { acc ->
                        FilterChip(
                            selected = selectedAccount == acc,
                            onClick = { onAccountChange(acc) },
                            label = { Text(acc) }
                        )
                    }
                }
            }
        }

        // 3 Key Financial Metrics for this Bank
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monthly Credits
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Credits", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                        }
                        Text(
                            text = CurrencyFormatter.formatIndian(totalCredits, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = IncomeGreen
                        )
                    }
                }

                // Monthly Debits (Bank Account)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpentRed.copy(alpha = 0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, tint = SpentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Debits", style = MaterialTheme.typography.labelSmall, color = SpentRed)
                        }
                        Text(
                            text = CurrencyFormatter.formatIndian(bankAccountDebits, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = SpentRed
                        )
                    }
                }

                // Credit Card Spends
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Card Spends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = CurrencyFormatter.formatIndian(creditCardSpends, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Filter Bar: All | Credits | Debits | UPI | EMI | Subscriptions | Credit Cards
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val filters = listOf(
                    "ALL" to "All",
                    "CREDITS" to "Credits 💰",
                    "DEBITS" to "Debits 💸",
                    "UPI" to "UPI ⚡",
                    "EMI" to "EMI 💳",
                    "SUBSCRIPTIONS" to "Subscriptions 🔁",
                    "CREDIT_CARDS" to "Credit Cards 💳"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilterType == key,
                        onClick = { onFilterChange(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_chip_$key")
                    )
                }
            }
        }



        if (combinedItems.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No transactions found for this bank filter", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedBank != null && selectedBank.isTrackingEnabled) {
                                "New bank SMS will appear automatically here."
                            } else {
                                "Tracking is currently OFF or no SMS recorded yet."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (selectedBank != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { onImportPrevious(selectedBank) }) {
                                Text("Import Previous SMS")
                            }
                        }
                    }
                }
            }
        } else {
            items(combinedItems, key = { "${if (it.isExpense) "E" else "I"}_${it.id}" }) { item ->
                BankTransactionRow(item = item, currencySymbol = currencySymbol)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private data class BankTransactionDisplayItem(
    val id: Long,
    val isExpense: Boolean,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: String,
    val paymentMethod: String,
    val isCreditCard: Boolean
)

@Composable
private fun BankTransactionRow(
    item: BankTransactionDisplayItem,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (!item.isExpense) IncomeGreen.copy(alpha = 0.15f)
                    else if (item.isCreditCard) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else SpentRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (!item.isExpense) Icons.Default.TrendingUp
                            else if (item.isCreditCard) Icons.Default.CreditCard
                            else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (!item.isExpense) IncomeGreen
                            else if (item.isCreditCard) MaterialTheme.colorScheme.primary
                            else SpentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.date} • ${item.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = (if (!item.isExpense) "+ " else "- ") + CurrencyFormatter.formatIndian(item.amount, currencySymbol),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (!item.isExpense) IncomeGreen else if (item.isCreditCard) MaterialTheme.colorScheme.primary else SpentRed
            )
        }
    }
}

@Composable
private fun AddBankDialog(
    existingBanks: List<BankEntity>,
    onDismiss: () -> Unit,
    onAddBank: (name: String, patterns: String, color: String) -> Unit
) {
    val existingCodes = existingBanks.map { it.bankCode.uppercase(Locale.ENGLISH) }.toSet()

    // Recommended Indian Banks to pick from
    val predefinedOptions = listOf(
        Triple("Federal Bank", "FEDERAL", "#00205B"),
        Triple("Bank of Baroda", "BOB", "#F26522"),
        Triple("Punjab National Bank", "PNB", "#A20A3C"),
        Triple("Union Bank", "UNION", "#E31837"),
        Triple("IndusInd Bank", "INDUSIND", "#8B181B"),
        Triple("IDFC FIRST Bank", "IDFC", "#9B1C2E"),
        Triple("Yes Bank", "YES", "#0B4EA2"),
        Triple("RBL Bank", "RBL", "#1A365D"),
        Triple("South Indian Bank", "SIB", "#B91C1C")
    ).filter { !existingCodes.contains(it.second) }

    var selectedPreset by remember { mutableStateOf<Triple<String, String, String>?>(predefinedOptions.firstOrNull()) }
    var customBankName by remember { mutableStateOf("") }
    var customSenderPattern by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bank to Tracking 🏦", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select a bank from the list or enter a custom bank name. (Tracking defaults to OFF until you enable it).",
                    style = MaterialTheme.typography.bodySmall
                )

                if (predefinedOptions.isNotEmpty()) {
                    Text("Popular Indian Banks:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(predefinedOptions) { preset ->
                            FilterChip(
                                selected = selectedPreset == preset && customBankName.isBlank(),
                                onClick = {
                                    selectedPreset = preset
                                    customBankName = ""
                                },
                                label = { Text(preset.first, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text("Or Custom Bank:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                OutlinedTextField(
                    value = customBankName,
                    onValueChange = { customBankName = it },
                    label = { Text("Bank Name") },
                    placeholder = { Text("e.g. Standard Chartered") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customSenderPattern,
                    onValueChange = { customSenderPattern = it },
                    label = { Text("SMS Sender Keyword (Optional)") },
                    placeholder = { Text("e.g. SCBL, STAN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customBankName.isNotBlank()) {
                        onAddBank(customBankName.trim(), customSenderPattern.trim(), "#3B82F6")
                    } else if (selectedPreset != null) {
                        onAddBank(selectedPreset!!.first, selectedPreset!!.second, selectedPreset!!.third)
                    }
                },
                enabled = customBankName.isNotBlank() || selectedPreset != null,
                modifier = Modifier.testTag("confirm_add_bank_button")
            ) {
                Text("Add Bank")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
