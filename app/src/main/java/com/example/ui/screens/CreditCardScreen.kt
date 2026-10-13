package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CreditCardEntity
import com.example.data.entity.ExpenseEntity
import com.example.domain.model.CardSpendingDetail
import com.example.domain.model.DailySpendItem
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreditCardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val ccSummary by viewModel.creditCardSummary.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showManualExpenseDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var editingLimitCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var selectedTab by remember { mutableStateOf("ALL") } // ALL, PURCHASES, PAYMENTS, REFUNDS

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Credit Cards 💳",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("credit_card_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddCardDialog = true },
                        modifier = Modifier.testTag("add_credit_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Info banner explaining the separation
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                        Text(
                            text = "Credit card purchases are tracked separately and do not reduce your Main In-Hand Wallet Balance. Only actual bill payments made from your bank reduce the Main Balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Summary KPI Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total CC Spending This Month
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Monthly Spending",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatIndian(ccSummary.totalSpendingThisMonth, settings.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = SpentRed
                                )
                                Text(
                                    text = "Does not reduce wallet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Current Outstanding Amount
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ccSummary.totalOutstanding > 0) SpentRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Current Outstanding",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatIndian(ccSummary.totalOutstanding, settings.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (ccSummary.totalOutstanding > 0) SpentRed else IncomeGreen
                                )
                                Text(
                                    text = if (ccSummary.totalOutstanding > 0) "Due for bill payment" else "All settled! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ccSummary.totalOutstanding > 0) SpentRed else IncomeGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Payments Made
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Payments Made",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatIndian(ccSummary.totalPaymentsMade, settings.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = IncomeGreen
                                )
                                Text(
                                    text = "Deducted from wallet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Total Available Limit
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Available Limit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (ccSummary.totalAvailableLimit > 0) {
                                        CurrencyFormatter.formatIndian(ccSummary.totalAvailableLimit, settings.currencySymbol)
                                    } else {
                                        "Set Limit"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Across all active cards",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPaymentDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_cc_payment_button")
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Bill", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showManualExpenseDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_cc_expense_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Spend", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showAddCardDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Card", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Card-Wise Spending Summary Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card-wise Spending Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ccSummary.cards.size} Cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card-Wise Cards List
            if (ccSummary.cards.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "💳", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No credit cards tracked yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When bank credit card SMS is received, it will automatically appear here! Or tap below to add one.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddCardDialog = true }) {
                                Text("Add Credit Card")
                            }
                        }
                    }
                }
            } else {
                items(ccSummary.cards, key = { it.card.cardNumberMasked + it.card.cardName }) { cardDetail ->
                    CreditCardItemCard(
                        cardDetail = cardDetail,
                        currencySymbol = settings.currencySymbol,
                        onSetLimit = { editingLimitCard = cardDetail.card },
                        onRecordPayment = {
                            showPaymentDialog = true
                        }
                    )
                }
            }

            // Monthly Credit Card Spending Graph
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Credit Card Spending Graph",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daily credit card purchase trend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatIndian(ccSummary.totalSpendingThisMonth, settings.currencySymbol),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CreditCardMonthlyGraph(
                            dailySpends = ccSummary.monthlyDailySpends,
                            currencySymbol = settings.currencySymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }

            // Test SMS Simulation Section for Credit Cards
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚡ Instant Test SMS Simulator",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Test credit card spending, bill payments, and duplicate prevention:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpentRed.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    viewModel.simulateBankSmsForReview(
                                        "VK-HDFCBK",
                                        "Rs. 5,000 spent on your HDFC Bank Credit Card ending 7609 at AMAZON"
                                    ) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                }
                            ) {
                                Text(
                                    text = "💳 HDFC ₹5,000 Spend",
                                    color = SpentRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarningOrange.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    viewModel.simulateBankSmsForReview(
                                        "VK-ICICIB",
                                        "Transaction of INR 3,000 on your ICICI Credit Card XX7378 at RELIANCE"
                                    ) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                }
                            ) {
                                Text(
                                    text = "💳 ICICI ₹3,000 Spend",
                                    color = WarningOrange,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = IncomeGreen.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    viewModel.simulateBankSmsForReview(
                                        "VK-HDFCBK",
                                        "Rs. 2,000 debited from A/c XX1234 towards HDFC Credit Card bill XX7609"
                                    ) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                }
                            ) {
                                Text(
                                    text = "🏦 ₹2,000 Bill Payment",
                                    color = IncomeGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    viewModel.simulateBankSmsForReview(
                                        "VK-HDFCBK",
                                        "Refund of Rs 500 credited to your credit card XX7609 for transaction"
                                    ) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                }
                            ) {
                                Text(
                                    text = "💰 ₹500 CC Refund",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Transactions Header & Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Individual Credit Card Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ccSummary.recentTransactions.size} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTab == "ALL",
                                onClick = { selectedTab = "ALL" },
                                label = { Text("All (${ccSummary.recentTransactions.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTab == "PURCHASES",
                                onClick = { selectedTab = "PURCHASES" },
                                label = { Text("Purchases (${ccSummary.recentTransactions.count { it.isCreditCard && !it.isBillPayment }})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTab == "PAYMENTS",
                                onClick = { selectedTab = "PAYMENTS" },
                                label = { Text("Bill Payments (${ccSummary.recentTransactions.count { it.isBillPayment }})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTab == "REFUNDS",
                                onClick = { selectedTab = "REFUNDS" },
                                label = { Text("Refunds (${ccSummary.recentRefunds.size})") }
                            )
                        }
                    }
                }
            }

            // Transaction items
            val displayedTransactions = when (selectedTab) {
                "PURCHASES" -> ccSummary.recentTransactions.filter { it.isCreditCard && !it.isBillPayment }
                "PAYMENTS" -> ccSummary.recentTransactions.filter { it.isBillPayment }
                "REFUNDS" -> emptyList()
                else -> ccSummary.recentTransactions
            }

            if (selectedTab == "REFUNDS") {
                if (ccSummary.recentRefunds.isEmpty()) {
                    item {
                        Text(
                            text = "No credit card refunds recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(ccSummary.recentRefunds, key = { "ref_${it.id}" }) { ref ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = IncomeGreen.copy(alpha = 0.2f), modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = ref.note.ifBlank { "Credit Card Refund" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "${ref.date} • ${ref.cardMaskedNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(text = "+ " + CurrencyFormatter.formatIndian(ref.amount, settings.currencySymbol), fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                        }
                    }
                }
            } else {
                if (displayedTransactions.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions found in this section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(displayedTransactions, key = { it.id }) { exp ->
                        CreditCardTransactionItem(
                            expense = exp,
                            currencySymbol = settings.currencySymbol,
                            onDelete = { viewModel.deleteExpense(exp.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Dialog 1: Add New Credit Card
    if (showAddCardDialog) {
        var cardNameInput by remember { mutableStateOf("") }
        var bankNameInput by remember { mutableStateOf("HDFC") }
        var cardLast4Input by remember { mutableStateOf("") }
        var limitInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Add Credit Card 💳", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bankNameInput,
                        onValueChange = { bankNameInput = it },
                        label = { Text("Bank Name (e.g. HDFC, ICICI, SBI, Axis)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cardLast4Input,
                        onValueChange = { cardLast4Input = it.take(4).filter { c -> c.isDigit() } },
                        label = { Text("Last 4 Digits (e.g. 7609)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cardNameInput,
                        onValueChange = { cardNameInput = it },
                        label = { Text("Card Display Name (Optional)") },
                        placeholder = { Text("$bankNameInput Credit Card XX$cardLast4Input") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Credit Limit (Optional, e.g. 50000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val masked = if (cardLast4Input.isNotBlank()) "XX$cardLast4Input" else "XX0000"
                        val cName = cardNameInput.trim().ifBlank { "${bankNameInput.trim()} Credit Card $masked" }
                        val limit = limitInput.toDoubleOrNull() ?: 0.0
                        viewModel.addCreditCard(
                            cardName = cName,
                            bankName = bankNameInput.trim(),
                            cardNumberMasked = masked,
                            creditLimit = limit
                        )
                        showAddCardDialog = false
                        Toast.makeText(context, "Added $cName!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Add Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 2: Edit Credit Limit
    editingLimitCard?.let { card ->
        var limitInput by remember { mutableStateOf(if (card.creditLimit > 0) card.creditLimit.toLong().toString() else "") }
        AlertDialog(
            onDismissRequest = { editingLimitCard = null },
            title = { Text("Set Credit Limit for ${card.cardName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter total credit limit assigned by the bank:")
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Credit Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateCreditLimit(card.id, limit)
                        editingLimitCard = null
                        Toast.makeText(context, "Limit updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLimitCard = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 3: Record Bill Payment
    if (showPaymentDialog) {
        var paymentAmountInput by remember { mutableStateOf("") }
        var selectedPaidCard by remember {
            mutableStateOf(ccSummary.cards.firstOrNull()?.card?.cardNumberMasked ?: "")
        }
        var paymentDateInput by remember { mutableStateOf(DateUtils.today()) }
        var paymentNoteInput by remember { mutableStateOf("Credit Card Bill Payment") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Credit Card Bill Payment 🏦", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This payment will be deducted from your Main Bank Wallet and will reduce your Credit Card Outstanding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount Paid (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ccSummary.cards.isNotEmpty()) {
                        Text("For Card:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ccSummary.cards) { c ->
                                FilterChip(
                                    selected = selectedPaidCard == c.card.cardNumberMasked,
                                    onClick = { selectedPaidCard = c.card.cardNumberMasked },
                                    label = { Text(c.card.cardNumberMasked) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = paymentDateInput,
                        onValueChange = { paymentDateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentNoteInput,
                        onValueChange = { paymentNoteInput = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val targetCard = ccSummary.cards.find { it.card.cardNumberMasked == selectedPaidCard }?.card
                            val cardName = targetCard?.cardName ?: "Credit Card $selectedPaidCard"
                            viewModel.recordCreditCardBillPayment(
                                amount = amount,
                                cardMaskedNumber = selectedPaidCard,
                                cardName = cardName,
                                date = paymentDateInput,
                                note = paymentNoteInput
                            )
                            showPaymentDialog = false
                            Toast.makeText(context, "Recorded bill payment of ₹$amount!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 4: Manual Credit Card Expense
    if (showManualExpenseDialog) {
        var expenseAmountInput by remember { mutableStateOf("") }
        var selectedSpendCard by remember {
            mutableStateOf(ccSummary.cards.firstOrNull()?.card?.cardNumberMasked ?: "")
        }
        var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
        var expenseDateInput by remember { mutableStateOf(DateUtils.today()) }
        var expenseNoteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualExpenseDialog = false },
            title = { Text("Add Credit Card Purchase 🛍️", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This purchase will NOT deduct from your Main Bank Wallet balance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = expenseAmountInput,
                        onValueChange = { expenseAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Purchase Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ccSummary.cards.isNotEmpty()) {
                        Text("Paid With Card:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ccSummary.cards) { c ->
                                FilterChip(
                                    selected = selectedSpendCard == c.card.cardNumberMasked,
                                    onClick = { selectedSpendCard = c.card.cardNumberMasked },
                                    label = { Text(c.card.cardNumberMasked) }
                                )
                            }
                        }
                    }
                    Text("Category:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = expenseDateInput,
                        onValueChange = { expenseDateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseNoteInput,
                        onValueChange = { expenseNoteInput = it },
                        label = { Text("Note / Merchant (e.g. Amazon, Flight)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = expenseAmountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val targetCard = ccSummary.cards.find { it.card.cardNumberMasked == selectedSpendCard }?.card
                            val cardName = targetCard?.cardName ?: "Credit Card $selectedSpendCard"
                            viewModel.addManualCreditCardExpense(
                                amount = amount,
                                cardName = cardName,
                                cardMaskedNumber = selectedSpendCard,
                                categoryId = selectedCategoryId,
                                date = expenseDateInput,
                                note = expenseNoteInput
                            )
                            showManualExpenseDialog = false
                            Toast.makeText(context, "Added CC purchase of ₹$amount!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add CC Purchase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreditCardItemCard(
    cardDetail: CardSpendingDetail,
    currencySymbol: String,
    onSetLimit: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val card = cardDetail.card
    val gradientColors = when (card.bankName.uppercase()) {
        "HDFC" -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
        "ICICI" -> listOf(Color(0xFF991B1B), Color(0xFFEA580C))
        "SBI" -> listOf(Color(0xFF0F766E), Color(0xFF06B6D4))
        "AXIS" -> listOf(Color(0xFF831843), Color(0xFFBE185D))
        else -> listOf(Color(0xFF374151), Color(0xFF1F2937))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_summary_${card.cardNumberMasked}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Visual Credit Card Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(gradientColors))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.bankName.ifBlank { "BANK" },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CREDIT",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.85f),
                            modifier = Modifier.size(width = 30.dp, height = 22.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "••••  ••••  ••••  " + card.cardNumberMasked.takeLast(4),
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.cardName,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "EXP: ••/••",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Statistics Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatIndian(cardDetail.totalSpentThisMonth, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SpentRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Current Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatIndian(cardDetail.currentOutstanding, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (cardDetail.currentOutstanding > 0) SpentRed else IncomeGreen
                    )
                }
            }

            // Credit Limit Progress Bar (if limit set)
            if (card.creditLimit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Limit: ${CurrencyFormatter.formatIndian(card.creditLimit, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Available: ${CurrencyFormatter.formatIndian(cardDetail.availableLimit, currencySymbol)} (${100 - cardDetail.utilizationPercentage}% left)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (cardDetail.utilizationPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (cardDetail.utilizationPercentage > 75) SpentRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Pay Bill / Edit Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSetLimit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (card.creditLimit > 0) "Edit Limit" else "Set Limit", fontSize = 12.sp)
                }

                Button(
                    onClick = onRecordPayment,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Bill", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CreditCardMonthlyGraph(
    dailySpends: List<DailySpendItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dailySpends) {
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val maxAmount = (dailySpends.maxOfOrNull { it.totalAmount } ?: 0.0).coerceAtLeast(100.0)

    if (dailySpends.isEmpty() || dailySpends.all { it.totalAmount <= 0 }) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No credit card spending recorded this month yet 🥳",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingBottom = 20.dp.toPx()
        val graphHeight = height - paddingBottom

        val points = dailySpends.mapIndexed { index, item ->
            val x = if (dailySpends.size > 1) {
                index * (width / (dailySpends.size - 1))
            } else {
                width / 2
            }
            val y = (graphHeight - (item.totalAmount / maxAmount * graphHeight * animProgress.value)).toFloat()
            Offset(x, y.coerceIn(0f, graphHeight))
        }

        // Draw horizontal grid lines
        for (i in 1..3) {
            val y = graphHeight * (i / 4f)
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Gradient Area under Curve
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, graphHeight)
            path.lineTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx = (prev.x + curr.x) / 2
                path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
            path.lineTo(points.last().x, graphHeight)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SpentRed.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = graphHeight
                )
            )
        }

        // Draw smooth line
        val strokePath = Path()
        if (points.isNotEmpty()) {
            strokePath.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx = (prev.x + curr.x) / 2
                strokePath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
            drawPath(
                path = strokePath,
                color = SpentRed,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw point dots
        points.forEachIndexed { idx, pt ->
            if (dailySpends[idx].totalAmount > 0) {
                drawCircle(color = SpentRed, radius = 4.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }
    }
}

@Composable
fun CreditCardTransactionItem(
    expense: ExpenseEntity,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val isBillPayment = expense.isBillPayment
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isBillPayment) IncomeGreen.copy(alpha = 0.15f) else SpentRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBillPayment) Icons.Default.Payment else Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = if (isBillPayment) IncomeGreen else SpentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isBillPayment) "Bill Payment" else expense.note.ifBlank { "Credit Card Purchase" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (expense.cardMaskedNumber.isNotBlank()) expense.cardMaskedNumber else "Credit Card",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${expense.date} • ${expense.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isBillPayment) "- " else "- ") + CurrencyFormatter.formatIndian(expense.amount, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    color = if (isBillPayment) IncomeGreen else SpentRed
                )
                Text(
                    text = if (isBillPayment) "Reduces Wallet" else "CC Spends Only",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBillPayment) IncomeGreen else MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
            }
        }
    }
}
