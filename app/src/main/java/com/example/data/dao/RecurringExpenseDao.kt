package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY recurringDay ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE enabled = 1")
    suspend fun getActiveRecurringExpensesSnapshot(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllRecurringExpensesSnapshot(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurring: RecurringExpenseEntity): Long

    @Update
    suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: Long)

    @Query("UPDATE recurring_expenses SET lastAppliedMonthYear = :monthYear WHERE id = :id")
    suspend fun updateLastAppliedMonthYear(id: Long, monthYear: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RecurringExpenseEntity>)

    @Query("DELETE FROM recurring_expenses")
    suspend fun clearAllRecurring()
}
