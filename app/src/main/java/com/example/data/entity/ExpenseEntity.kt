package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["categoryId"]),
        Index(value = ["smsHash"], unique = true)
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val paymentMethod: String, // Cash, UPI, Debit Card, Credit Card, Net Banking, Other
    val note: String = "",
    val isCreditCard: Boolean = false,
    val cardName: String = "",
    val cardMaskedNumber: String = "",
    val isBillPayment: Boolean = false,
    val smsHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
