package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY isTrackingEnabled DESC, bankName ASC")
    fun getAllBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks ORDER BY bankName ASC")
    suspend fun getAllBanksSnapshot(): List<BankEntity>

    @Query("SELECT * FROM banks WHERE isTrackingEnabled = 1")
    suspend fun getEnabledBanksSnapshot(): List<BankEntity>

    @Query("SELECT * FROM banks WHERE bankCode = :code LIMIT 1")
    suspend fun getBankByCode(code: String): BankEntity?

    @Query("SELECT * FROM banks WHERE id = :id LIMIT 1")
    suspend fun getBankById(id: Long): BankEntity?

    @Query("SELECT COUNT(*) FROM banks")
    suspend fun getBankCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBank(bank: BankEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBanks(banks: List<BankEntity>)

    @Update
    suspend fun updateBank(bank: BankEntity)

    @Query("UPDATE banks SET isTrackingEnabled = :isEnabled, enabledAtTimestamp = :enabledAtTimestamp WHERE id = :bankId")
    suspend fun updateTrackingStatus(bankId: Long, isEnabled: Boolean, enabledAtTimestamp: Long)

    @Delete
    suspend fun deleteBank(bank: BankEntity)

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteBankById(id: Long)
}
