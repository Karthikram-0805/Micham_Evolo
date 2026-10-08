package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SalaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDao {
    @Query("SELECT * FROM salaries WHERE month = :month AND year = :year LIMIT 1")
    fun getSalary(month: Int, year: Int): Flow<SalaryEntity?>

    @Query("SELECT * FROM salaries WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getSalarySnapshot(month: Int, year: Int): SalaryEntity?

    @Query("SELECT * FROM salaries ORDER BY year DESC, month DESC")
    fun getAllSalaries(): Flow<List<SalaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSalary(salary: SalaryEntity): Long

    @Query("DELETE FROM salaries")
    suspend fun clearAllSalaries()

    @Query("SELECT * FROM salaries")
    suspend fun getAllSalariesSnapshot(): List<SalaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(salaries: List<SalaryEntity>)
}
