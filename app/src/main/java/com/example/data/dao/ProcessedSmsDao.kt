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

    @Query("UPDATE processed_sms SET status = 'DELETED' WHERE associatedId = :associatedId AND transactionType = :type")
    suspend fun markAsDeleted(associatedId: Long, type: String): Int

    @Query("UPDATE processed_sms SET status = 'DELETED' WHERE smsHash = :smsHash")
    suspend fun markHashAsDeleted(smsHash: String): Int

    @Query("DELETE FROM processed_sms WHERE smsHash = :smsHash")
    suspend fun deleteByHash(smsHash: String)

    @Query("SELECT * FROM processed_sms WHERE smsHash = :smsHash OR (smsId != '' AND smsId = :smsId) OR (referenceId != '' AND referenceId = :referenceId) LIMIT 1")
    suspend fun getProcessedByHashOrRef(smsHash: String, smsId: String, referenceId: String): ProcessedSmsEntity?

    @Query("SELECT * FROM processed_sms WHERE smsHash = :smsHash LIMIT 1")
    suspend fun getProcessedByHash(smsHash: String): ProcessedSmsEntity?

    @Query("SELECT * FROM processed_sms WHERE associatedId = :associatedId LIMIT 1")
    suspend fun getByAssociatedId(associatedId: Long): ProcessedSmsEntity?

    @Query("SELECT * FROM processed_sms WHERE status = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    suspend fun getPendingReviewSms(): List<ProcessedSmsEntity>

    @Query("UPDATE processed_sms SET status = :status, description = :description WHERE smsHash = :smsHash")
    suspend fun updateStatusAndDescription(smsHash: String, status: String, description: String): Int

    @Query("UPDATE processed_sms SET status = :status WHERE smsHash = :smsHash")
    suspend fun updateStatus(smsHash: String, status: String): Int

    @Query("UPDATE processed_sms SET associatedId = :associatedId WHERE smsHash = :smsHash")
    suspend fun updateAssociatedId(smsHash: String, associatedId: Long): Int

    @Query("DELETE FROM processed_sms")
    suspend fun clearAll()
}
