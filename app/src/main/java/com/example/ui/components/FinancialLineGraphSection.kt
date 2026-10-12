package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.SalaryEntity
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max

// Line Types & Standard Colors matching Excel reference & prompt specifications
enum class GraphLineType(
    val label: String,
    val color: Color,
    val defaultVisible: Boolean = true
) {
    INCOME("Total Income", IncomeGreen, true),
    EXPENSES("Total Expenses", SpentRed, true),
    UPI("UPI", Color(0xFF3B82F6), true),
    EMI("EMI", Color(0xFFF97316), true),
    SUBSCRIPTIONS("Subscriptions", Color(0xFFA855F7), true),
    OTHER("Other", Color(0xFF64748B), true)
}

enum class PrimaryViewMode {
    MONTHLY,
    YEARLY
}

enum class SpendingMode {
    DAILY,
    CUMULATIVE
}

// Data point for a day or month
data class GraphDataPoint(
    val index: Int,            // 0-based
    val label: String,         // e.g. "1", "2"... or "Jan", "Feb"...
    val fullDateOrMonth: String,
    val income: Double,
    val totalExpense: Double,
    val upiExpense: Double,
    val emiExpense: Double,
    val subscriptionExpense: Double,
    val otherExpense: Double
)

@Composable
fun InteractiveFinancialAnalyticsSection(
    allExpenses: List<ExpenseEntity>,
    allIncomes: List<IncomeEntity>,
    allSalaries: List<SalaryEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    isRoastMode: Boolean = true,
    initialMonth: Int = LocalDate.now().monthValue,
    initialYear: Int = LocalDate.now().year,
    onNavigateToTransactions: (month: Int, year: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var primaryView by remember { mutableStateOf(PrimaryViewMode.MONTHLY) }
    var spendingMode by remember { mutableStateOf(SpendingMode.DAILY) }

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    // Line visibility filters
    var visibleLines by remember {
        mutableStateOf(
            GraphLineType.values().associateWith { it.defaultVisible }
        )
    }

    // Interactive selected point index (for tap/tooltip)
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val catMap = remember(categories) { categories.associateBy { it.id } }

    // Helper: Categorize expense
    fun categorizeExpense(exp: ExpenseEntity): GraphLineType {
        val catName = catMap[exp.categoryId]?.name?.lowercase() ?: ""
        val note = exp.note.lowercase()
        val payMethod = exp.paymentMethod.lowercase()

        return when {
            payMethod.contains("upi") || note.contains("upi") || note.contains("gpay") || note.contains("phonepe") || note.contains("paytm") -> GraphLineType.UPI
            catName.contains("emi") || catName.contains("loan") || note.contains("emi") || note.contains("loan") -> GraphLineType.EMI
            catName.contains("subscription") || catName.contains("ott") || note.contains("subscription") || note.contains("netflix") || note.contains("prime") || note.contains("spotify") || note.contains("hotstar") || note.contains("recharge") -> GraphLineType.SUBSCRIPTIONS
            else -> GraphLineType.OTHER
        }
    }

    // Build Monthly Data Points
    val monthlyPoints = remember(selectedYear, selectedMonth, allExpenses, allIncomes, allSalaries, spendingMode) {
        val daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
        val points = mutableListOf<GraphDataPoint>()
        val monthPrefix = String.format("%04d-%02d", selectedYear, selectedMonth)

        val monthExpenses = allExpenses.filter { it.date.startsWith(monthPrefix) }
        val monthIncomes = allIncomes.filter { it.date.startsWith(monthPrefix) }

        // Salary is treated on day 1 (or configured day) as part of income
        val salRecord = allSalaries.find { it.month == selectedMonth && it.year == selectedYear }
        val salaryAmount = salRecord?.salaryAmount ?: 0.0

        var runningIncome = 0.0
        var runningExpense = 0.0
        var runningUpi = 0.0
        var runningEmi = 0.0
        var runningSub = 0.0
        var runningOther = 0.0

        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, day)
            val dayExps = monthExpenses.filter { it.date == dateStr }
            val dayIncs = monthIncomes.filter { it.date == dateStr }

            var dayIncome = dayIncs.sumOf { it.amount }
            if (day == 1) {
                dayIncome += salaryAmount
            }

            var dayTotalExp = 0.0
            var dayUpi = 0.0
            var dayEmi = 0.0
            var daySub = 0.0
            var dayOther = 0.0

            for (exp in dayExps) {
                dayTotalExp += exp.amount
                when (categorizeExpense(exp)) {
                    GraphLineType.UPI -> dayUpi += exp.amount
                    GraphLineType.EMI -> dayEmi += exp.amount
                    GraphLineType.SUBSCRIPTIONS -> daySub += exp.amount
                    else -> dayOther += exp.amount
                }
            }

            if (spendingMode == SpendingMode.CUMULATIVE) {
                runningIncome += dayIncome
                runningExpense += dayTotalExp
                runningUpi += dayUpi
                runningEmi += dayEmi
                runningSub += daySub
                runningOther += dayOther

                points.add(
                    GraphDataPoint(
                        index = day - 1,
                        label = "$day",
                        fullDateOrMonth = DateUtils.formatDisplayDate(dateStr),
                        income = runningIncome,
                        totalExpense = runningExpense,
                        upiExpense = runningUpi,
                        emiExpense = runningEmi,
                        subscriptionExpense = runningSub,
                        otherExpense = runningOther
                    )
                )
            } else {
                points.add(
                    GraphDataPoint(
                        index = day - 1,
                        label = "$day",
                        fullDateOrMonth = DateUtils.formatDisplayDate(dateStr),
                        income = dayIncome,
                        totalExpense = dayTotalExp,
                        upiExpense = dayUpi,
                        emiExpense = dayEmi,
                        subscriptionExpense = daySub,
                        otherExpense = dayOther
                    )
                )
            }
        }
        points
    }

    // Build Yearly Data Points (12 months)
    val yearlyPoints = remember(selectedYear, allExpenses, allIncomes, allSalaries) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val points = mutableListOf<GraphDataPoint>()

        for (m in 1..12) {
            val prefix = String.format("%04d-%02d", selectedYear, m)
            val monthExps = allExpenses.filter { it.date.startsWith(prefix) }
            val monthIncs = allIncomes.filter { it.date.startsWith(prefix) }

            val salRecord = allSalaries.find { it.month == m && it.year == selectedYear }
            val salAmount = salRecord?.salaryAmount ?: 0.0
            val monthIncome = salAmount + monthIncs.sumOf { it.amount }

            var monthTotalExp = 0.0
            var monthUpi = 0.0
            var monthEmi = 0.0
            var monthSub = 0.0
            var monthOther = 0.0

            for (exp in monthExps) {
                monthTotalExp += exp.amount
                when (categorizeExpense(exp)) {
                    GraphLineType.UPI -> monthUpi += exp.amount
                    GraphLineType.EMI -> monthEmi += exp.amount
                    GraphLineType.SUBSCRIPTIONS -> monthSub += exp.amount
                    else -> monthOther += exp.amount
                }
            }

            points.add(
                GraphDataPoint(
                    index = m - 1,
                    label = monthNames[m - 1],
                    fullDateOrMonth = DateUtils.formatMonthYear(m, selectedYear),
                    income = monthIncome,
                    totalExpense = monthTotalExp,
                    upiExpense = monthUpi,
                    emiExpense = monthEmi,
                    subscriptionExpense = monthSub,
                    otherExpense = monthOther
                )
            )
        }
        points
    }

    val activePoints = if (primaryView == PrimaryViewMode.MONTHLY) monthlyPoints else yearlyPoints

    // Reset selected point when view changes
    LaunchedEffect(primaryView, selectedMonth, selectedYear, spendingMode) {
        selectedPointIndex = null
    }

    // Calculations for Analytics Cards & Roast Mode
    val totalPeriodIncome = remember(activePoints) {
        if (primaryView == PrimaryViewMode.MONTHLY && spendingMode == SpendingMode.CUMULATIVE) {
            activePoints.lastOrNull()?.income ?: 0.0
        } else {
            activePoints.sumOf { it.income }
        }
    }

    val totalPeriodExpense = remember(activePoints) {
        if (primaryView == PrimaryViewMode.MONTHLY && spendingMode == SpendingMode.CUMULATIVE) {
            activePoints.lastOrNull()?.totalExpense ?: 0.0
        } else {
            activePoints.sumOf { it.totalExpense }
        }
    }

    val highestDayOrMonth = remember(activePoints) {
        activePoints.maxByOrNull { it.totalExpense }
    }

    val averageDailyOrMonthly = remember(activePoints, totalPeriodExpense) {
        if (activePoints.isNotEmpty()) totalPeriodExpense / activePoints.size else 0.0
    }

    // Roast Mode Commentary based on trends
    val roastCommentary = remember(totalPeriodIncome, totalPeriodExpense, isRoastMode, primaryView, highestDayOrMonth) {
        if (!isRoastMode) {
            "Spending insights generated based on transaction logs."
        } else {
            when {
                totalPeriodExpense > totalPeriodIncome && totalPeriodIncome > 0 ->
                    "Inime Swiggy paatha phone-ah switch off pannidu 📱💀 Red line green line-ah thaandi vinnai thodudhu!"
                totalPeriodExpense > 0 && highestDayOrMonth != null && highestDayOrMonth.totalExpense > 3000 ->
                    "Bro... ${highestDayOrMonth.fullDateOrMonth} la ₹${highestDayOrMonth.totalExpense.toInt()} spend pannadhu paathu wallet bayandhuduchu 😱 ICU admit pannalama?"
                totalPeriodExpense == 0.0 ->
                    "Expense illa... idhu real life-ah? Wallet romba nimmadhiya thoongudhu 😌✨"
                totalPeriodExpense < (totalPeriodIncome * 0.5) && totalPeriodIncome > 0 ->
                    "Control pannita boss! Mass hero da nee! Micham nalla iruku 🏆💰"
                else ->
                    "Konjam discipline theva padum bro... graph-la red line steady-ah errudhu 👀"
            }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_financial_analytics_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Financial Analytics Graph",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "📈", fontSize = 16.sp)
                    }
                    Text(
                        text = "Interactive spending & income multi-line tracker",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (primaryView == PrimaryViewMode.MONTHLY) "MONTHLY" else "YEARLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Primary Toggle: Monthly | Yearly TabRow
            TabRow(
                selectedTabIndex = if (primaryView == PrimaryViewMode.MONTHLY) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("graph_primary_view_toggle")
            ) {
                Tab(
                    selected = primaryView == PrimaryViewMode.MONTHLY,
                    onClick = { primaryView = PrimaryViewMode.MONTHLY },
                    text = {
                        Text(
                            text = "Monthly View 📅",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("tab_monthly_view")
                )
                Tab(
                    selected = primaryView == PrimaryViewMode.YEARLY,
                    onClick = { primaryView = PrimaryViewMode.YEARLY },
                    text = {
                        Text(
                            text = "Yearly View 📊",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("tab_yearly_view")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month / Year Selector Bar with arrows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (primaryView == PrimaryViewMode.MONTHLY) {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        } else {
                            selectedYear -= 1
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("graph_prev_time_button")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val now = LocalDate.now()
                        selectedYear = now.year
                        selectedMonth = now.monthValue
                    }
                ) {
                    Text(
                        text = if (primaryView == PrimaryViewMode.MONTHLY)
                            DateUtils.formatMonthYear(selectedMonth, selectedYear)
                        else
                            "$selectedYear Full Year",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to jump to current ⏱️",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        if (primaryView == PrimaryViewMode.MONTHLY) {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        } else {
                            selectedYear += 1
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("graph_next_time_button")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            // 2. Secondary Toggle (Daily Spending vs Cumulative Spending) inside Monthly View
            if (primaryView == PrimaryViewMode.MONTHLY) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (spendingMode == SpendingMode.DAILY)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                modifier = Modifier
                                    .clickable { spendingMode = SpendingMode.DAILY }
                                    .testTag("toggle_daily_spending")
                            ) {
                                Text(
                                    text = "Daily Spending",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (spendingMode == SpendingMode.DAILY)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (spendingMode == SpendingMode.CUMULATIVE)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                modifier = Modifier
                                    .clickable { spendingMode = SpendingMode.CUMULATIVE }
                                    .testTag("toggle_cumulative_spending")
                            ) {
                                Text(
                                    text = "Cumulative Spending",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (spendingMode == SpendingMode.CUMULATIVE)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Interactive Graph Line Toggle Filter Chips (Total Income, Total Expenses, UPI, EMI, Subscriptions, Other)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GraphLineType.values().forEach { lineType ->
                    val isChecked = visibleLines[lineType] ?: true
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            visibleLines = visibleLines.toMutableMap().apply {
                                put(lineType, !isChecked)
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(lineType.color)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lineType.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = lineType.color.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("filter_chip_${lineType.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Line Chart (Excel Style Multi-line with dynamic grid & axes)
            MultiLineFinancialCanvas(
                points = activePoints,
                visibleLines = visibleLines,
                currencySymbol = currencySymbol,
                selectedIndex = selectedPointIndex,
                onSelectIndex = { selectedPointIndex = it },
                onMonthTap = { monthIdx ->
                    if (primaryView == PrimaryViewMode.YEARLY) {
                        onNavigateToTransactions(monthIdx + 1, selectedYear)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("financial_multiline_canvas")
            )

            // Interactive Tooltip / Point Details Card when a point is tapped
            AnimatedVisibility(visible = selectedPointIndex != null) {
                val pt = selectedPointIndex?.let { idx -> activePoints.getOrNull(idx) }
                if (pt != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("graph_point_tooltip")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📌 ${pt.fullDateOrMonth}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (primaryView == PrimaryViewMode.YEARLY) {
                                    TextButton(
                                        onClick = {
                                            onNavigateToTransactions(pt.index + 1, selectedYear)
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Open Ledger 📜", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                    Text(
                                        text = CurrencyFormatter.formatIndian(pt.income, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = SpentRed)
                                    Text(
                                        text = CurrencyFormatter.formatIndian(pt.totalExpense, currencySymbol),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("UPI / EMI / Sub", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "₹${pt.upiExpense.toInt()} / ₹${pt.emiExpense.toInt()} / ₹${pt.subscriptionExpense.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 7. Tamil Roast Mode Integration Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (totalPeriodExpense > totalPeriodIncome && totalPeriodIncome > 0)
                    SpentRed.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tamil_roast_trend_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalPeriodExpense > totalPeriodIncome && totalPeriodIncome > 0) "😭" else "🔥",
                        fontSize = 26.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRoastMode) "Tamil Roast Trend Alert 🌶️" else "Financial Summary Notice 📊",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPeriodExpense > totalPeriodIncome && totalPeriodIncome > 0)
                                SpentRed
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = roastCommentary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Analytics Summary Metrics Grid (Total Income, Total Expenses, Highest Day/Month, Daily/Monthly Average)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Total Income
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatIndian(totalPeriodIncome, currencySymbol),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Metric 2: Total Spent
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpentRed.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = SpentRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = SpentRed)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatIndian(totalPeriodExpense, currencySymbol),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SpentRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 3: Highest Day / Month
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (primaryView == PrimaryViewMode.MONTHLY) "Highest Day 🔥" else "Highest Month 🔥",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = highestDayOrMonth?.let {
                                "${it.label} (${CurrencyFormatter.formatIndian(it.totalExpense, currencySymbol)})"
                            } ?: "None",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SpentRed
                        )
                    }
                }

                // Metric 4: Daily / Monthly Avg
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (primaryView == PrimaryViewMode.MONTHLY) "Daily Average ⏱️" else "Monthly Average ⏱️",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatIndian(averageDailyOrMonthly, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick Button to open full transaction ledger for this period
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToTransactions(selectedMonth, selectedYear)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Detailed Ledger for ${DateUtils.formatMonthYear(selectedMonth, selectedYear)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom Multi-Line Graph Canvas
 * Supports X-axis date/month labels, Y-axis Rupee amounts, multiple colored lines,
 * smooth curves, touch hit detection, animated entrance, and selection point highlights.
 */
@Composable
private fun MultiLineFinancialCanvas(
    points: List<GraphDataPoint>,
    visibleLines: Map<GraphLineType, Boolean>,
    currencySymbol: String,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    onMonthTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available for this range.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points, visibleLines) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    // Determine max value for Y-axis scale
    var maxVal = 100.0
    points.forEach { pt ->
        if (visibleLines[GraphLineType.INCOME] == true) maxVal = max(maxVal, pt.income)
        if (visibleLines[GraphLineType.EXPENSES] == true) maxVal = max(maxVal, pt.totalExpense)
        if (visibleLines[GraphLineType.UPI] == true) maxVal = max(maxVal, pt.upiExpense)
        if (visibleLines[GraphLineType.EMI] == true) maxVal = max(maxVal, pt.emiExpense)
        if (visibleLines[GraphLineType.SUBSCRIPTIONS] == true) maxVal = max(maxVal, pt.subscriptionExpense)
        if (visibleLines[GraphLineType.OTHER] == true) maxVal = max(maxVal, pt.otherExpense)
    }

    // Add 15% top padding for visual breathing room
    val yAxisMax = maxVal * 1.15

    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val cursorLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { offset ->
                val padLeft = 45.dp.toPx()
                val padRight = 16.dp.toPx()
                val padBottom = 28.dp.toPx()
                val padTop = 16.dp.toPx()

                val chartWidth = size.width - padLeft - padRight
                val n = points.size
                if (n <= 1) return@detectTapGestures

                val stepX = chartWidth / (n - 1)
                val relX = offset.x - padLeft

                if (relX in -20f..(chartWidth + 20f)) {
                    val touchedIdx = (relX / stepX).toInt().coerceIn(0, n - 1)
                    onSelectIndex(touchedIdx)
                    onMonthTap(touchedIdx)
                } else {
                    onSelectIndex(null)
                }
            }
        }
    ) {
        val padLeft = 45.dp.toPx()
        val padRight = 16.dp.toPx()
        val padBottom = 28.dp.toPx()
        val padTop = 16.dp.toPx()

        val chartWidth = size.width - padLeft - padRight
        val chartHeight = size.height - padTop - padBottom

        val n = points.size
        if (n <= 1 || chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val stepX = chartWidth / (n - 1)

        // Draw 4 horizontal grid lines and Y-axis labels
        val gridSteps = 4
        val paint = android.graphics.Paint().apply {
            color = axisTextColor
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }

        for (i in 0..gridSteps) {
            val ratio = i / gridSteps.toFloat()
            val y = padTop + chartHeight * (1f - ratio)
            val valAtStep = yAxisMax * ratio

            // Grid line
            drawLine(
                color = gridLineColor,
                start = Offset(padLeft, y),
                end = Offset(size.width - padRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // Y-axis label text in nativeCanvas
            val labelText = if (valAtStep >= 1000) {
                "${(valAtStep / 1000).toInt()}k"
            } else {
                "${valAtStep.toInt()}"
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$currencySymbol$labelText",
                padLeft - 6.dp.toPx(),
                y + 4.dp.toPx(),
                paint
            )
        }

        // Draw X-axis labels
        val xPaint = android.graphics.Paint().apply {
            color = axisTextColor
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // Determine step for label drawing to prevent overlapping (e.g. for 31 days show every 5th or 7th)
        val labelStep = if (n > 15) max(1, n / 6) else 1
        for (i in 0 until n step labelStep) {
            val x = padLeft + i * stepX
            val label = points[i].label
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height - 8.dp.toPx(),
                xPaint
            )
        }

        // Always show the last point's label if not already drawn
        if ((n - 1) % labelStep != 0) {
            val x = padLeft + (n - 1) * stepX
            drawContext.canvas.nativeCanvas.drawText(
                points.last().label,
                x,
                size.height - 8.dp.toPx(),
                xPaint
            )
        }

        // Helper to project value to Y
        fun getY(value: Double): Float {
            val ratio = (value / yAxisMax).toFloat().coerceIn(0f, 1f)
            val animRatio = ratio * animationProgress.value
            return padTop + chartHeight * (1f - animRatio)
        }

        // Helper to draw a specific line
        fun drawLineGraph(
            lineType: GraphLineType,
            getValue: (GraphDataPoint) -> Double
        ) {
            if (visibleLines[lineType] != true) return

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { i, pt ->
                val x = padLeft + i * stepX
                val y = getY(getValue(pt))

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, padTop + chartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = padLeft + (i - 1) * stepX
                    val prevY = getY(getValue(points[i - 1]))

                    // Smooth cubic bezier curve
                    val cx1 = prevX + (x - prevX) / 2
                    val cy1 = prevY
                    val cx2 = prevX + (x - prevX) / 2
                    val cy2 = y
                    path.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                }
            }

            // Fill area under primary Expense or Income line
            if (lineType == GraphLineType.EXPENSES || lineType == GraphLineType.INCOME) {
                fillPath.lineTo(padLeft + (n - 1) * stepX, padTop + chartHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineType.color.copy(alpha = 0.14f * animationProgress.value),
                            Color.Transparent
                        ),
                        startY = padTop,
                        endY = padTop + chartHeight
                    )
                )
            }

            // Draw line stroke
            drawPath(
                path = path,
                color = lineType.color,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw circular points
            points.forEachIndexed { i, pt ->
                val x = padLeft + i * stepX
                val y = getY(getValue(pt))
                val isSelected = selectedIndex == i

                // Highlight point if selected or key peak
                if (isSelected) {
                    drawCircle(
                        color = lineType.color.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = lineType.color,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                } else if (n <= 12) {
                    // In yearly or small count, draw crisp dots
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = lineType.color,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Draw in back-to-front order: Other, Subs, EMI, UPI, Income, Expenses
        drawLineGraph(GraphLineType.OTHER) { it.otherExpense }
        drawLineGraph(GraphLineType.SUBSCRIPTIONS) { it.subscriptionExpense }
        drawLineGraph(GraphLineType.EMI) { it.emiExpense }
        drawLineGraph(GraphLineType.UPI) { it.upiExpense }
        drawLineGraph(GraphLineType.INCOME) { it.income }
        drawLineGraph(GraphLineType.EXPENSES) { it.totalExpense }

        // Draw vertical selection indicator cursor
        if (selectedIndex != null && selectedIndex in 0 until n) {
            val cursorX = padLeft + selectedIndex * stepX
            drawLine(
                color = cursorLineColor,
                start = Offset(cursorX, padTop),
                end = Offset(cursorX, padTop + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }
    }
}
