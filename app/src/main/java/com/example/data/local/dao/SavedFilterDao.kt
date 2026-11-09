package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavedFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {

    @Query("SELECT * FROM saved_transaction_filters ORDER BY id DESC")
    fun getAllSavedFilters(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_transaction_filters WHERE id = :id")
    suspend fun getSavedFilterById(id: Long): SavedFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFilter(filter: SavedFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFilters(filters: List<SavedFilterEntity>)

    @Update
    suspend fun updateSavedFilter(filter: SavedFilterEntity)

    @Delete
    suspend fun deleteSavedFilter(filter: SavedFilterEntity)
}
