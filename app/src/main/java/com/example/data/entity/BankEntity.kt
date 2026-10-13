package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "banks",
    indices = [
        Index(value = ["bankCode"], unique = true)
    ]
)
data class BankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String, // e.g. "HDFC Bank", "ICICI Bank", "SBI"
    val bankCode: String, // e.g. "HDFC", "ICICI", "SBI", "AXIS", "INDIAN", "CANARA", "KOTAK"
    val isTrackingEnabled: Boolean = false, // Sliding toggle switch. Defaults to FALSE!
    val enabledAtTimestamp: Long = 0L, // Timestamp when tracking was turned ON
    val senderPatterns: String = "", // Comma-separated sender substrings e.g. "HDFCBK,HDFC"
    val colorHex: String = "#3B82F6",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
