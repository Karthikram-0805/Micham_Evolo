package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [Index(value = ["date"])]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val incomeType: String, // Bonus, Freelance, Cashback, Refund, Other
    val date: String, // YYYY-MM-DD
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
