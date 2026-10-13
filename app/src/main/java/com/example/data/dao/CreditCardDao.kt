package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY bankName ASC, cardNumberMasked ASC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY bankName ASC, cardNumberMasked ASC")
    suspend fun getAllCreditCardsSnapshot(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE cardNumberMasked = :masked AND bankName = :bank LIMIT 1")
    suspend fun getCardByNumberAndBank(masked: String, bank: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE cardNumberMasked = :masked LIMIT 1")
    suspend fun getCardByNumber(masked: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("UPDATE credit_cards SET creditLimit = :limit WHERE id = :id")
    suspend fun updateCreditLimit(id: Long, limit: Double)
}
