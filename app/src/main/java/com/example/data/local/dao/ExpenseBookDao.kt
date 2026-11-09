package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseBookDao {

    @Query("SELECT * FROM expense_books ORDER BY isDefault DESC, isArchived ASC, name ASC")
    fun getAllBooks(): Flow<List<ExpenseBookEntity>>

    @Query("SELECT * FROM expense_books WHERE isArchived = 0 ORDER BY isDefault DESC, name ASC")
    fun getActiveBooks(): Flow<List<ExpenseBookEntity>>

    @Query("SELECT * FROM expense_books WHERE id = :id")
    suspend fun getBookById(id: Long): ExpenseBookEntity?

    @Query("SELECT * FROM expense_books WHERE id = :id")
    fun getBookByIdFlow(id: Long): Flow<ExpenseBookEntity?>

    @Query("SELECT * FROM expense_books WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBook(): ExpenseBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: ExpenseBookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<ExpenseBookEntity>)

    @Update
    suspend fun updateBook(book: ExpenseBookEntity)

    @Delete
    suspend fun deleteBook(book: ExpenseBookEntity)

    @Query("DELETE FROM expense_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE expense_books SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE expense_books SET isDefault = 0")
    suspend fun resetAllDefaults()

    @Query("UPDATE expense_books SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultBook(id: Long)

    @Query("SELECT COUNT(*) FROM expense_books")
    suspend fun getBookCount(): Int
}
