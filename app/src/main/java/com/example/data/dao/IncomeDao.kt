package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY date DESC, id DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getIncomesForMonth(monthPrefix: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getIncomesBetweenDates(startDate: String, endDate: String): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Query("SELECT * FROM incomes WHERE smsHash = :smsHash LIMIT 1")
    suspend fun getIncomeBySmsHash(smsHash: String): IncomeEntity?

    @Query("SELECT COUNT(*) FROM incomes WHERE (:smsHash IS NOT NULL AND :smsHash != '' AND smsHash = :smsHash) OR (:referenceId != '' AND referenceId = :referenceId) OR (:smsId != '' AND smsId = :smsId)")
    suspend fun countBySmsIdentity(smsHash: String?, referenceId: String, smsId: String): Int

    @Query("SELECT * FROM incomes WHERE isCreditCardRefund = 1 ORDER BY date DESC")
    fun getAllCreditCardRefunds(): Flow<List<IncomeEntity>>

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("SELECT * FROM incomes")
    suspend fun getAllIncomesSnapshot(): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(incomes: List<IncomeEntity>)

    @Query("DELETE FROM incomes")
    suspend fun clearAllIncomes()
}
