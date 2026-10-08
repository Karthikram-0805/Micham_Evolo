package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.sms.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class UnifiedMonthlyTransaction(
    val id: Long,
    val type: TransactionType, // DEBIT or CREDIT
    val amount: Double,
    val date: String,          // YYYY-MM-DD
    val time: String,          // HH:mm
    val description: String,
    val source: String,        // "Bank SMS" or "Manual Entry"
    val categoryName: String? = null,
    val categoryColorHex: String? = null,
    val paymentMethod: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyTransactionDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentToday = remember { LocalDate.now() }
    var selectedYear by remember { mutableIntStateOf(currentToday.year) }
    var selectedMonth by remember { mutableIntStateOf(currentToday.monthValue) }

    val allExpenses by viewModel.allExpenses.collectAsState()
    val allIncomes by viewModel.allIncomes.collectAsState()
    val allSalaries by viewModel.allSalaries.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    // Transaction pending deletion confirmation dialog state
    var transactionToDelete by remember { mutableStateOf<UnifiedMonthlyTransaction?>(null) }

    val monthPrefix = String.format("%04d-%02d", selectedYear, selectedMonth)
    val catMap = remember(categories) { categories.associateBy { it.id } }

    // 1. Monthly Opening Balance:
    // Salary record for this month/year, fallback to defaultSalary
    val salaryRecord = allSalaries.find { it.month == selectedMonth && it.year == selectedYear }
    val monthlyOpeningBalance = salaryRecord?.salaryAmount ?: settings.defaultSalary

    // 2. Filter all debits and credits for this month
    val monthExpenses = allExpenses.filter { it.date.startsWith(monthPrefix) }
    val monthIncomes = allIncomes.filter { it.date.startsWith(monthPrefix) }

    // Total Credits for this month
    val totalCredits = BigDecimal(monthIncomes.sumOf { it.amount })
        .setScale(2, RoundingMode.HALF_UP).toDouble()

    // Total Debits for this month (all expenses: bank debits + manual expenses)
    val totalDebits = BigDecimal(monthExpenses.sumOf { it.amount })
        .setScale(2, RoundingMode.HALF_UP).toDouble()

    // Total Manual Expenses in this month
    val totalManualExpenses = BigDecimal(
        monthExpenses.filter { !it.note.startsWith("Bank SMS", ignoreCase = true) }.sumOf { it.amount }
    ).setScale(2, RoundingMode.HALF_UP).toDouble()

    // Monthly Closing Balance = Monthly Opening Balance + Total Credits − Total Debits
    val monthlyClosingBalance = BigDecimal(monthlyOpeningBalance + totalCredits - totalDebits)
        .setScale(2, RoundingMode.HALF_UP).toDouble()

    // Build unified transactions list
    val unifiedTransactions = remember(monthExpenses, monthIncomes, catMap) {
        val list = mutableListOf<UnifiedMonthlyTransaction>()

        // Debits
        for (exp in monthExpenses) {
            val isBank = exp.note.startsWith("Bank SMS", ignoreCase = true)
            val cat = catMap[exp.categoryId]
            val desc = if (isBank) {
                // Strip "Bank SMS: " prefix if present for cleaner display
                exp.note.removePrefix("Bank SMS:").trim()
            } else {
                exp.note.ifBlank { cat?.name ?: "Manual Expense" }
            }

            list.add(
                UnifiedMonthlyTransaction(
                    id = exp.id,
                    type = TransactionType.DEBIT,
                    amount = exp.amount,
                    date = exp.date,
                    time = exp.time,
                    description = desc,
                    source = if (isBank) "Bank SMS" else "Manual Entry",
                    categoryName = cat?.name,
                    categoryColorHex = cat?.colorHex,
                    paymentMethod = exp.paymentMethod
                )
            )
        }

        // Credits
        for (inc in monthIncomes) {
            val isBank = inc.note.startsWith("Bank SMS", ignoreCase = true) || inc.incomeType.contains("Bank", ignoreCase = true)
            val desc = if (isBank) {
                inc.note.removePrefix("Bank SMS Credit:").trim()
            } else {
                inc.note.ifBlank { inc.incomeType }
            }

            list.add(
                UnifiedMonthlyTransaction(
                    id = inc.id,
                    type = TransactionType.CREDIT,
                    amount = inc.amount,
                    date = inc.date,
                    time = "10:00", // Default if not specified
                    description = desc,
                    source = if (isBank) "Bank SMS" else "Manual Entry",
                    categoryName = inc.incomeType,
                    categoryColorHex = "#10B981"
                )
            )
        }

        // Sort in chronological order, newest first!
        list.sortWith(
            compareByDescending<UnifiedMonthlyTransaction> { it.date }
                .thenByDescending { it.time }
                .thenByDescending { it.id }
        )
        list
    }

    val totalTransactionCount = unifiedTransactions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Monthly Transactions 📜",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Complete ledger for ${DateUtils.formatMonthYear(selectedMonth, selectedYear)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("monthly_transactions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Month & Year Selector Card
                MonthYearSelectorCard(
                    month = selectedMonth,
                    year = selectedYear,
                    onPrevMonth = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    },
                    onNextMonth = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    },
                    onCurrentMonth = {
                        selectedYear = currentToday.year
                        selectedMonth = currentToday.monthValue
                    }
                )
            }

            // Monthly Summary Card
            item {
                MonthlySummaryCard(
                    openingBalance = monthlyOpeningBalance,
                    totalCredits = totalCredits,
                    totalDebits = totalDebits,
                    totalManualExpenses = totalManualExpenses,
                    closingBalance = monthlyClosingBalance,
                    totalCount = totalTransactionCount,
                    currencySymbol = settings.currencySymbol
                )
            }

            // Ledger Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${totalTransactionCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Newest First ⏱️",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty state
            if (unifiedTransactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🍃", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions in ${DateUtils.formatMonthYear(selectedMonth, selectedYear)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Opening balance: ${CurrencyFormatter.formatIndian(monthlyOpeningBalance, settings.currencySymbol)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(unifiedTransactions, key = { "${it.type}_${it.id}" }) { item ->
                    TransactionLedgerItemCard(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onDeleteClick = { transactionToDelete = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation Dialog before permanently deleting a transaction
    if (transactionToDelete != null) {
        val item = transactionToDelete!!
        val isDebit = item.type == TransactionType.DEBIT
        val amountStr = CurrencyFormatter.formatIndian(item.amount, settings.currencySymbol)

        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Delete ${if (isDebit) "Debit" else "Credit"} Transaction?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete this transaction?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (isDebit) "Debit: -" else "Credit: +"}$amountStr on ${DateUtils.formatDisplayDate(item.date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDebit) SpentRed else IncomeGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Source: ${item.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isDebit) {
                            "✨ Deleting this debit will restore $amountStr back to your wallet balance."
                        } else {
                            "✨ Deleting this credit will subtract $amountStr from your wallet balance."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = transactionToDelete
                        transactionToDelete = null
                        if (deleted != null) {
                            if (deleted.type == TransactionType.DEBIT) {
                                viewModel.deleteExpense(deleted.id)
                            } else {
                                viewModel.deleteIncome(deleted.id)
                            }
                            Toast.makeText(
                                context,
                                "Transaction deleted! Wallet balance updated automatically.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_transaction_button")
                ) {
                    Text(
                        text = "Delete",
                        color = SpentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MonthYearSelectorCard(
    month: Int,
    year: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month"
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCurrentMonth() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = DateUtils.formatMonthYear(month, year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to view current month",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month"
                )
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    openingBalance: Double,
    totalCredits: Double,
    totalDebits: Double,
    totalManualExpenses: Double,
    closingBalance: Double,
    totalCount: Int,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTHLY SUMMARY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$totalCount Txns",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opening Balance vs Closing Balance Highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Monthly Opening Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatIndian(openingBalance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Monthly Closing Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatIndian(closingBalance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (closingBalance < 0) SpentRed else IncomeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid: Credits, Debits, Manual Expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total Credits
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Credits (+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${CurrencyFormatter.formatIndian(totalCredits, currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                // Total Debits
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Debits (-)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpentRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "-${CurrencyFormatter.formatIndian(totalDebits, currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpentRed
                    )
                }

                // Total Manual Expenses (subset note)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Manual Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.formatIndian(totalManualExpenses, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ℹ️ Manual expenses are included in Total Debits and not subtracted twice.",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun TransactionLedgerItemCard(
    item: UnifiedMonthlyTransaction,
    currencySymbol: String,
    onDeleteClick: () -> Unit
) {
    val isDebit = item.type == TransactionType.DEBIT
    val amountFormatted = CurrencyFormatter.formatIndian(item.amount, currencySymbol)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${item.type}_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDebit) SpentRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.source == "Bank SMS") Icons.Default.Sms else Icons.Default.EditNote,
                    contentDescription = item.source,
                    tint = if (isDebit) SpentRed else IncomeGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source Badge: Bank SMS vs Manual Entry
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.source,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Date & Time
                    Text(
                        text = "${DateUtils.formatDisplayDate(item.date)} • ${item.time}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Delete Button
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isDebit) "-$amountFormatted" else "+$amountFormatted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isDebit) SpentRed else IncomeGreen
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_transaction_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
