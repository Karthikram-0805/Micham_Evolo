package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [
        Index(value = ["date"]),
        Index(value = ["smsHash"], unique = true)
    ]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val incomeType: String, // Bank Deposit, Bonus, Freelance, Cashback, Refund, Other
    val date: String, // YYYY-MM-DD
    val note: String = "",
    val isCreditCardRefund: Boolean = false,
    val cardMaskedNumber: String = "",
    val smsHash: String? = null,
    val smsId: String = "",
    val referenceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
