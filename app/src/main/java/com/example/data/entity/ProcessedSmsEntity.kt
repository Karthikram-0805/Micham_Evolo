package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processed_sms",
    indices = [
        Index(value = ["smsHash"], unique = true),
        Index(value = ["date"])
    ]
)
data class ProcessedSmsEntity(
    @PrimaryKey val smsHash: String,
    val sender: String,
    val body: String,
    val amount: Double,
    val transactionType: String, // "DEBIT" or "CREDIT"
    val bankName: String,
    val accountInfo: String,
    val paymentMethod: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val referenceId: String = "",
    val associatedId: Long = 0, // Id of ExpenseEntity or IncomeEntity
    val smsId: String = "",
    val description: String = "",
    val status: String = "CONFIRMED", // "CONFIRMED" or "SKIPPED"
    val timestamp: Long = System.currentTimeMillis()
)
