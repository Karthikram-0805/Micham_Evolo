package com.example.domain.model

import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity

data class FinancialSummary(
    val salary: Double = 0.0,
    val additionalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val percentageRemaining: Int = 100,
    val percentageSpent: Int = 0,
    val todaySpend: Double = 0.0,
    val weekSpend: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val daysRemainingInCycle: Int = 30,
    val projectedMonthEndSpend: Double = 0.0,
    val projectedRemaining: Double = 0.0,
    val projectedSavingsPercentage: Int = 0,
    val totalAvailable: Double = 0.0
)

data class CategoryWithSpend(
    val category: CategoryEntity,
    val totalSpend: Double = 0.0,
    val percentageOfTotal: Float = 0f,
    val monthlyBudget: Double = 0.0,
    val percentBudgetUsed: Int = 0
)

data class ExpenseWithCategory(
    val expense: ExpenseEntity,
    val category: CategoryEntity?
)

data class DailySpendItem(
    val date: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class MonthHistoryItem(
    val month: Int,
    val year: Int,
    val salary: Double,
    val additionalIncome: Double,
    val totalSpent: Double,
    val remaining: Double,
    val savingsPercentage: Int
)

data class ExpenseFilter(
    val searchQuery: String = "",
    val categoryId: Long? = null,
    val paymentMethod: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)
