package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val categoryId: Long,
    val paymentMethod: String,
    val recurringDay: Int, // 1 - 31
    val enabled: Boolean = true,
    val lastAppliedMonthYear: String = "" // e.g. "2026-10"
)
