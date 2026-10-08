package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.DialogueEngine
import com.example.domain.model.FinancialSummary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MichamGreenLight
import com.example.ui.theme.MichamGreenPrimary
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.utils.CurrencyFormatter

@Composable
fun MainBalanceCard(
    summary: FinancialSummary,
    currencySymbol: String,
    userName: String,
    isRoastMode: Boolean,
    onOpenMonthlyTransactions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progressTarget = if (summary.totalAvailable > 0) {
        (summary.totalExpenses / summary.totalAvailable).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 800),
        label = "spent_progress"
    )

    val greetingText = DialogueEngine.getGreeting(
        remainingPercent = summary.percentageRemaining,
        todaySpend = summary.todaySpend,
        isRoastMode = isRoastMode
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Dynamic Friendly Tamil Greeting Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (summary.percentageRemaining > 50) "😎" else if (summary.percentageRemaining > 20) "👀" else "😭",
                        fontSize = 22.sp
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Hero Balance Card - Clickable to open full monthly transaction history
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_balance_card")
                .clickable(enabled = onOpenMonthlyTransactions != null) {
                    onOpenMonthlyTransactions?.invoke()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Header label and Percentage remaining pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MICHAM EVLO? (Remaining)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            if (onOpenMonthlyTransactions != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "View Monthly Transactions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Percentage remaining pill
                        val badgeColor = when {
                            summary.percentageRemaining > 50 -> IncomeGreen
                            summary.percentageRemaining > 20 -> WarningOrange
                            else -> SpentRed
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            contentColor = badgeColor
                        ) {
                            Text(
                                text = "${summary.percentageRemaining}% left",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remaining Balance Amount (Highlight of the app!)
                    Text(
                        text = CurrencyFormatter.formatIndian(summary.remainingBalance, currencySymbol),
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (summary.remainingBalance < 0) SpentRed else MaterialTheme.colorScheme.onSurface
                    )

                    if (onOpenMonthlyTransactions != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to view monthly transaction ledger 📜",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar of Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${summary.percentageSpent}% spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.daysRemainingInCycle} days left in cycle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (summary.percentageSpent > 80) SpentRed else MichamGreenPrimary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Monthly Salary vs Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Salary",
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = " Total Salary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatIndian(summary.totalAvailable, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (summary.additionalIncome > 0) {
                                Text(
                                    text = "+${CurrencyFormatter.formatIndian(summary.additionalIncome, currencySymbol)} income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Spent",
                                    tint = SpentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = " Spent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatIndian(summary.totalExpenses, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpentRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Stats Trio Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Today",
                amount = CurrencyFormatter.formatIndian(summary.todaySpend, currencySymbol),
                subtitle = if (summary.todaySpend == 0.0) "Zero spend! 😌" else null,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "This Week",
                amount = CurrencyFormatter.formatIndian(summary.weekSpend, currencySymbol),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Daily Avg",
                amount = CurrencyFormatter.formatIndian(summary.dailyAverage, currencySymbol),
                modifier = Modifier.weight(1f)
            )
        }

        // Daily Budget & Overspend Warning Banner
        if (summary.dailyBudget > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (summary.todaySpend > summary.dailyBudget) WarningOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (summary.todaySpend > summary.dailyBudget) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = WarningOrange,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = if (summary.todaySpend > summary.dailyBudget) {
                                DialogueEngine.getDailyBudgetWarning()
                            } else {
                                "Suggested daily budget: ${CurrencyFormatter.formatIndian(summary.dailyBudget, currencySymbol)}/day"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (summary.todaySpend > summary.dailyBudget) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = IncomeGreen
                )
            }
        }
    }
}
