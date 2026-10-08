package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.ExpenseEntity
import com.example.domain.model.ExpenseWithCategory
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.MainBalanceCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSmsSync: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onOpenMonthlyTransactions: () -> Unit = {}
) {
    val summary by viewModel.financialSummary.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val expensesWithCategory by viewModel.filteredExpenses.collectAsState()
    val pendingSmsTransactions by viewModel.pendingSmsTransactions.collectAsState()

    val recentExpenses = expensesWithCategory.take(8)

    val context = LocalContext.current
    var hasReadSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.READ_SMS] == true
        hasReadSms = granted
        if (granted) {
            viewModel.checkAppOpenSms()
            Toast.makeText(context, "SMS Permission Granted! Scanning today's bank SMS...", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // App Top Bar inside list for seamless scrolling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Micham Evlo 👛",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Vanakkam, ${settings.userName}! ✋",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenSmsSync,
                            modifier = Modifier.testTag("home_sms_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "Bank SMS Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenAddIncome) {
                            Icon(
                                imageVector = Icons.Default.AddCard,
                                contentDescription = "Add Income",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Day / Night Theme Toggle Button on Top Right
                        val context = LocalContext.current
                        val isSystemDark = isSystemInDarkTheme()
                        val isDark = when (settings.themeMode) {
                            "DARK" -> true
                            "LIGHT" -> false
                            else -> isSystemDark
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
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
                                    .testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDark) "Switch to Day Mode" else "Switch to Night Mode",
                                    tint = if (isDark) WarningOrange else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Balance and Stats Card
            item {
                MainBalanceCard(
                    summary = summary,
                    currencySymbol = settings.currencySymbol,
                    userName = settings.userName,
                    isRoastMode = settings.reactionIntensity == "ROAST",
                    onOpenMonthlyTransactions = onOpenMonthlyTransactions
                )
            }

            // SMS Permission Alert Card (if not granted yet)
            if (!hasReadSms) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_sms_permission_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Auto Bank SMS Detection 🛡️",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Grant SMS permission to automatically detect bank debit & credit transactions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.RECEIVE_SMS
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_grant_sms_button")
                            ) {
                                Text("Grant SMS Permission", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Quick Auto SMS Reader Banner
            item {
                val hasPending = pendingSmsTransactions.isNotEmpty()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPending) {
                            IncomeGreen.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSmsSync() }
                        .testTag("home_sms_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasPending) IncomeGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (hasPending) {
                                            "🔔 ${pendingSmsTransactions.size} Bank Transactions Detected!"
                                        } else {
                                            "Auto Bank SMS Reader 📱"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (hasPending) SpentRed else if (settings.autoSmsDetectionEnabled) IncomeGreen else WarningOrange
                                    ) {
                                        Text(
                                            text = if (hasPending) "ACTION NEEDED" else if (settings.autoSmsDetectionEnabled) "ACTIVE" else "OFF",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                Text(
                                    text = if (hasPending) {
                                        "Tap to review detected amounts & add description"
                                    } else {
                                        "Auto-tracks UPI, ATM & Card debit/credit from SMS • Tap to manage"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Simulation & Scan Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.simulateBankSmsForReview(
                                            "VK-HDFCBK",
                                            "Rs. 1,250 debited from A/c XX1234 via UPI"
                                        ) { status ->
                                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "⚡ ₹1,250 Debit (UPI)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = IncomeGreen.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.simulateBankSmsForReview(
                                            "AD-SBIINB",
                                            "Rs. 10,000.00 credited to A/c XX5678 by transfer"
                                        ) { status ->
                                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "⚡ ₹10,000 Credit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (hasReadSms) {
                                            viewModel.checkAppOpenSms(forceRescanToday = true) { items ->
                                                Toast.makeText(
                                                    context,
                                                    if (items.isNotEmpty()) "Found ${items.size} today's bank SMS!" else "No new bank SMS today",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                                        }
                                    }
                            ) {
                                Text(
                                    text = "🔄 Scan Inbox",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Expenses Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Damage (Expenses)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (expensesWithCategory.isNotEmpty()) {
                        TextButton(onClick = onOpenSearch) {
                            Text("View All (${expensesWithCategory.size})")
                        }
                    }
                }
            }

            // Empty State or List of Recent Expenses
            if (recentExpenses.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎉", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Expense illa... idhu real life-ah? 👀",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No expenses recorded yet. Tap below when you spend something!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onOpenAddExpense,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_first_expense_button")
                            ) {
                                Text("Add First Expense 💸")
                            }
                        }
                    }
                }
            } else {
                items(recentExpenses, key = { it.expense.id }) { item ->
                    ExpenseItemCard(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { onEditExpense(item.expense) },
                        onDelete = { viewModel.deleteExpense(item.expense.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
