package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salaries",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class SalaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int, // 1 - 12
    val year: Int,  // e.g. 2026
    val salaryAmount: Double,
    val salaryStartDate: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
