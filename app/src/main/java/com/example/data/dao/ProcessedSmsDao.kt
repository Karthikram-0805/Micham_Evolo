package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ProcessedSmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedSmsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: ProcessedSmsEntity): Long

    @Query("SELECT COUNT(*) FROM processed_sms WHERE smsHash = :smsHash")
    suspend fun countByHash(smsHash: String): Int

    @Query("SELECT COUNT(*) FROM processed_sms WHERE smsHash = :smsHash OR (smsId != '' AND smsId = :smsId) OR (referenceId != '' AND referenceId = :referenceId)")
    suspend fun isSmsProcessed(smsHash: String, smsId: String, referenceId: String): Int

    @Query("SELECT COUNT(*) FROM processed_sms WHERE smsHash = :smsHash OR (smsId != '' AND smsId = :smsId)")
    suspend fun isSmsProcessed(smsHash: String, smsId: String): Int

    @Query("SELECT smsHash FROM processed_sms")
    suspend fun getAllProcessedHashes(): List<String>

    @Query("SELECT * FROM processed_sms ORDER BY timestamp DESC")
    fun getAllProcessedSms(): Flow<List<ProcessedSmsEntity>>

    @Query("SELECT * FROM processed_sms ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentProcessedSms(): List<ProcessedSmsEntity>

    @Query("DELETE FROM processed_sms WHERE smsHash = :smsHash")
    suspend fun deleteByHash(smsHash: String)

    @Query("DELETE FROM processed_sms")
    suspend fun clearAll()
}
