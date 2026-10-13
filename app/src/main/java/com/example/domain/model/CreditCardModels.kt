package com.example.domain.model

import com.example.data.entity.CreditCardEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity

data class CardSpendingDetail(
    val card: CreditCardEntity,
    val totalSpentThisMonth: Double = 0.0,
    val currentOutstanding: Double = 0.0,
    val paymentsMadeThisMonth: Double = 0.0,
    val availableLimit: Double = 0.0,
    val utilizationPercentage: Int = 0
)

data class CreditCardSummary(
    val totalSpendingThisMonth: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalPaymentsMade: Double = 0.0,
    val totalAvailableLimit: Double = 0.0,
    val upcomingPaymentAmount: Double = 0.0,
    val nearestDueDate: String = "",
    val netAfterCardDues: Double = 0.0,
    val cards: List<CardSpendingDetail> = emptyList(),
    val monthlyDailySpends: List<DailySpendItem> = emptyList(),
    val recentTransactions: List<ExpenseEntity> = emptyList(),
    val recentRefunds: List<IncomeEntity> = emptyList()
)
