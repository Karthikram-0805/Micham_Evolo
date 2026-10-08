package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE month = :month AND year = :year LIMIT 1")
    fun getSavingsGoal(month: Int, year: Int): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getSavingsGoalSnapshot(month: Int, year: Int): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: SavingsGoalEntity): Long

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoalsSnapshot(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllGoals()
}
