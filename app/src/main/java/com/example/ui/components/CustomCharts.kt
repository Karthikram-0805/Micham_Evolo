package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategoryWithSpend
import com.example.domain.model.DailySpendItem
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MichamGreenPrimary
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.utils.CurrencyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categoriesWithSpend: List<CategoryWithSpend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val nonZero = categoriesWithSpend.filter { it.totalSpend > 0 }
    val totalSpend = nonZero.sumOf { it.totalSpend }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categoriesWithSpend) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    if (totalSpend <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Expense illa... idhu real life-ah? 👀\n(Add expenses to see category graph)",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(170.dp)) {
                val strokeWidth = 26.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                nonZero.forEach { item ->
                    val sweepAngle = (item.percentageOfTotal * 360f * animationProgress.value)
                    val color = CategoryIconHelper.parseColor(item.category.colorHex)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Text inside Donut
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatIndian(totalSpend, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            nonZero.take(8).forEach { item ->
                val color = CategoryIconHelper.parseColor(item.category.colorHex)
                val pct = (item.percentageOfTotal * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.category.name} $pct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyExpenseBarChart(
    dailySpends: List<DailySpendItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (dailySpends.isEmpty() || dailySpends.all { it.totalAmount == 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Konjam expense add pannunga... apram graph vera level la varum 😎",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = dailySpends.maxOfOrNull { it.totalAmount }?.coerceAtLeast(100.0) ?: 100.0
    val highestDay = dailySpends.maxByOrNull { it.totalAmount }

    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = SpentRed

    Column(modifier = modifier.fillMaxWidth()) {
        if (highestDay != null && highestDay.totalAmount > 0) {
            Text(
                text = "Highest Day: ${highestDay.date} (${CurrencyFormatter.formatIndian(highestDay.totalAmount, currencySymbol)}) 🔥",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SpentRed,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val barCount = dailySpends.size
                if (barCount == 0) return@Canvas

                val availableWidth = size.width
                val barWidth = (availableWidth / barCount) * 0.65f
                val spacing = (availableWidth / barCount) * 0.35f

                dailySpends.forEachIndexed { index, item ->
                    val x = index * (barWidth + spacing) + spacing / 2
                    val heightRatio = (item.totalAmount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                    val barHeight = size.height * heightRatio
                    val y = size.height - barHeight

                    val isHighest = item.totalAmount == highestDay?.totalAmount && item.totalAmount > 0
                    val colorToUse = if (isHighest) peakColor else barColor

                    drawRoundRect(
                        color = colorToUse,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun SalaryVsExpenseComparison(
    salary: Double,
    additionalIncome: Double,
    expenses: Double,
    remaining: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalAvailable = salary + additionalIncome
    val maxVal = maxOf(totalAvailable, expenses).coerceAtLeast(1.0)

    val expenseRatio = (expenses / maxVal).toFloat().coerceIn(0f, 1f)
    val remainingRatio = (remaining.coerceAtLeast(0.0) / maxVal).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Bar 1: Available (Salary + Income)
        Text(
            text = "Total Available: ${CurrencyFormatter.formatIndian(totalAvailable, currencySymbol)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = IncomeGreen,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {}

        Spacer(modifier = Modifier.height(10.dp))

        // Bar 2: Spent
        Text(
            text = "Total Spent: ${CurrencyFormatter.formatIndian(expenses, currencySymbol)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SpentRed
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(expenseRatio)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpentRed)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bar 3: Micham (Remaining)
        Text(
            text = "Micham (Remaining): ${CurrencyFormatter.formatIndian(remaining, currencySymbol)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (remaining >= 0) MichamGreenPrimary else SpentRed
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(remainingRatio)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (remaining >= 0) MichamGreenPrimary else SpentRed)
            )
        }
    }
}
