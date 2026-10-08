package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // e.g. "Food", "Travel", etc.
    val colorHex: String, // e.g. "#FF5722"
    val isCustom: Boolean = false,
    val monthlyBudget: Double = 0.0 // 0 means no limit
)
