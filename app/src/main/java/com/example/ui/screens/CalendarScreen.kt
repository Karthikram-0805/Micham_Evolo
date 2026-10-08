package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.entity.ExpenseEntity
import com.example.domain.model.ExpenseWithCategory
import com.example.ui.components.ExpenseItemCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SpentRed
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onAddExpenseForDate: (String) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit
) {
    val selectedMonthDate by viewModel.selectedCalendarMonth.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfMonth) }

    val yearMonth = YearMonth.of(selectedMonthDate.year, selectedMonthDate.monthValue)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7) // 0 for Sunday

    val catMap = categories.associateBy { it.id }

    // Map day to expenses in this month
    val monthPrefix = String.format("%04d-%02d", selectedMonthDate.year, selectedMonthDate.monthValue)
    val monthExpenses = allExpenses.filter { it.date.startsWith(monthPrefix) }
    val dayExpensesMap = monthExpenses.groupBy {
        it.date.substringAfterLast("-").toIntOrNull() ?: 1
    }

    val selectedDateStr = String.format("%04d-%02d-%02d", selectedMonthDate.year, selectedMonthDate.monthValue, selectedDay)
    val expensesForSelectedDay = (dayExpensesMap[selectedDay] ?: emptyList()).map {
        ExpenseWithCategory(expense = it, category = catMap[it.categoryId])
    }
    val selectedDayTotal = expensesForSelectedDay.sumOf { it.expense.amount }

    val daysOfWeekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar 📅",
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
                            .testTag("calendar_theme_toggle_button")
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

        // Month Navigation Header
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeCalendarMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Text(
                        text = DateUtils.formatMonthYear(selectedMonthDate.monthValue, selectedMonthDate.year),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { viewModel.changeCalendarMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Days of week row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeekLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Calendar Grid Days
        item {
            val totalCells = ((startDayOfWeek + daysInMonth + 6) / 7) * 7
            val rows = totalCells / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startDayOfWeek + 1

                            if (dayNum in 1..daysInMonth) {
                                val dayExpenses = dayExpensesMap[dayNum] ?: emptyList()
                                val daySpend = dayExpenses.sumOf { it.amount }
                                val isSelected = dayNum == selectedDay
                                val isToday = dayNum == LocalDate.now().dayOfMonth &&
                                        selectedMonthDate.monthValue == LocalDate.now().monthValue &&
                                        selectedMonthDate.year == LocalDate.now().year

                                val indicatorColor = when {
                                    daySpend == 0.0 -> Color.Transparent
                                    daySpend < 500.0 -> IncomeGreen
                                    daySpend < 2000.0 -> WarningOrange
                                    else -> SpentRed
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isToday) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { selectedDay = dayNum }
                                        .testTag("calendar_day_$dayNum")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )

                                        if (daySpend > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(5.dp))
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).height(44.dp))
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Summary Card
        item {
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
                        Column {
                            Text(
                                text = "${DateUtils.formatMonthShort(selectedMonthDate.monthValue)} $selectedDay, ${selectedMonthDate.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${expensesForSelectedDay.size} transactions recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatIndian(selectedDayTotal, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (selectedDayTotal > 0) SpentRed else IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onAddExpenseForDate(selectedDateStr) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Expense for This Day")
                    }
                }
            }
        }

        // Expenses on Selected Day
        if (expensesForSelectedDay.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses on this date! 😌\nPurse had full rest.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(expensesForSelectedDay, key = { it.expense.id }) { item ->
                ExpenseItemCard(
                    item = item,
                    currencySymbol = settings.currencySymbol,
                    onEdit = { onEditExpense(item.expense) },
                    onDelete = { viewModel.deleteExpense(item.expense.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
