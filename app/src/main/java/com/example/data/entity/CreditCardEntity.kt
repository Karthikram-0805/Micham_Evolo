package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_cards",
    indices = [
        Index(value = ["cardNumberMasked", "bankName"], unique = true)
    ]
)
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String, // e.g. "HDFC Bank Credit Card XX7609"
    val bankName: String, // e.g. "HDFC"
    val cardNumberMasked: String, // e.g. "XX7609"
    val creditLimit: Double = 0.0, // optional user-entered limit
    val colorHex: String = "#3B82F6",
    val billingDay: Int = 0, // optional 1-31
    val dueDay: Int = 0, // optional 1-31
    val openingOutstanding: Double = 0.0, // optional opening outstanding balance
    val createdAt: Long = System.currentTimeMillis()
)
