package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards WHERE status = 'ACTIVE' ORDER BY id ASC")
    fun getAllActiveCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun getCardByIdFlow(id: Long): Flow<CreditCardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CreditCardEntity>)

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Query("UPDATE credit_cards SET outstandingBalance = :balance WHERE id = :id")
    suspend fun updateOutstandingBalance(id: Long, balance: Double)

    @Query("UPDATE credit_cards SET outstandingBalance = outstandingBalance + :delta WHERE id = :id")
    suspend fun adjustOutstandingBalance(id: Long, delta: Double)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)
}
