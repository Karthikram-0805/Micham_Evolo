package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.DialogueEngine
import com.example.domain.model.DailySpendItem
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.DailyExpenseBarChart
import com.example.ui.components.SalaryVsExpenseComparison
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MichamGreenPrimary
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import java.time.LocalDate

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val summary by viewModel.financialSummary.collectAsState()
    val categorySpendList by viewModel.categorySpendList.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    val cycle = DateUtils.calculateCycle(settings.salaryCycleType, settings.salaryCycleDay)
    val cycleExpenses = allExpenses.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }

    // Group expenses by date for daily bar chart
    val dailySpendItems = cycleExpenses
        .groupBy { it.date }
        .map { (date, list) ->
            DailySpendItem(
                date = date.substringAfterLast("-"),
                totalAmount = list.sumOf { it.amount },
                transactionCount = list.size
            )
        }
        .sortedBy { it.date.toIntOrNull() ?: 0 }

    val topCategory = categorySpendList.firstOrNull { it.totalSpend > 0 }
    val highestDay = dailySpendItems.maxByOrNull { it.totalAmount }

    val predictionMessage = DialogueEngine.getPredictionMessage(
        projectedRemaining = summary.projectedRemaining,
        projectedSpend = summary.projectedMonthEndSpend,
        salary = summary.totalAvailable
    )

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
                    text = "Analytics & Predictions 📊",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val context = LocalContext.current
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
                            .testTag("analytics_theme_toggle_button")
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

        // Smart Month-End Prediction Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.projectedRemaining >= 0)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else SpentRed.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prediction_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Smart Month-End Prediction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (summary.projectedRemaining >= 0) "🔮 Safe" else "⚠️ Danger",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.projectedRemaining >= 0) IncomeGreen else SpentRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = predictionMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Projected Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatIndian(summary.projectedMonthEndSpend, settings.currencySymbol),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = SpentRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Expected Micham",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatIndian(summary.projectedRemaining, settings.currencySymbol),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.projectedRemaining >= 0) MichamGreenPrimary else SpentRed
                            )
                        }
                    }
                }
            }
        }

        // Key Stat Highlights Grid
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Highlights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Spending Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = topCategory?.category?.name ?: "None",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (topCategory != null) {
                                Text(
                                    text = CurrencyFormatter.formatIndian(topCategory.totalSpend, settings.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpentRed
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Highest Spending Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = highestDay?.let { "Day ${it.date}" } ?: "None",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (highestDay != null) {
                                Text(
                                    text = CurrencyFormatter.formatIndian(highestDay.totalAmount, settings.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpentRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Savings Goal Progress
        if (settings.monthlySavingsGoal > 0) {
            item {
                val savedAmount = summary.remainingBalance.coerceAtLeast(0.0)
                val goalProgress = (savedAmount / settings.monthlySavingsGoal).toFloat().coerceIn(0f, 1f)
                val goalMessage = DialogueEngine.getSavingsGoalMessage(savedAmount, settings.monthlySavingsGoal)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Savings Target 🎯",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(goalProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = goalMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { goalProgress },
                            color = IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Saved: ${CurrencyFormatter.formatIndian(savedAmount, settings.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Goal: ${CurrencyFormatter.formatIndian(settings.monthlySavingsGoal, settings.currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Category Donut Chart
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    CategoryDonutChart(
                        categoriesWithSpend = categorySpendList,
                        currencySymbol = settings.currencySymbol
                    )
                }
            }
        }

        // Daily Expense Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Expense Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DailyExpenseBarChart(
                        dailySpends = dailySpendItems,
                        currencySymbol = settings.currencySymbol
                    )
                }
            }
        }

        // Salary vs Expense Comparison
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Salary vs Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SalaryVsExpenseComparison(
                        salary = summary.salary,
                        additionalIncome = summary.additionalIncome,
                        expenses = summary.totalExpenses,
                        remaining = summary.remainingBalance,
                        currencySymbol = settings.currencySymbol
                    )
                }
            }
        }

        // Category Budgets Progress & Warnings
        val budgetedCategories = categorySpendList.filter { it.monthlyBudget > 0 }
        if (budgetedCategories.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Category Budgets & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        budgetedCategories.forEach { catItem ->
                            val progress = (catItem.totalSpend / catItem.monthlyBudget).toFloat().coerceIn(0f, 1f)
                            val warning = DialogueEngine.getCategoryBudgetWarning(catItem.category.name, catItem.percentBudgetUsed)

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = catItem.category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatIndian(catItem.totalSpend, settings.currencySymbol)} / ${CurrencyFormatter.formatIndian(catItem.monthlyBudget, settings.currencySymbol)} (${catItem.percentBudgetUsed}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (catItem.percentBudgetUsed > 90) SpentRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (catItem.percentBudgetUsed > 90) SpentRed else WarningOrange,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                if (catItem.percentBudgetUsed >= 75) {
                                    Text(
                                        text = warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (catItem.percentBudgetUsed >= 100) SpentRed else WarningOrange,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
