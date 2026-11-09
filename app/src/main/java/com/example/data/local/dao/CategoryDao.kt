package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY isFavorite DESC, sortOrder ASC, isCustom ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY isFavorite DESC, sortOrder ASC, isCustom ASC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (parentCategoryId IS NULL OR parentCategoryId = 0) AND type = :type AND isArchived = 0 ORDER BY isFavorite DESC, sortOrder ASC, isCustom ASC, name ASC")
    fun getParentCategories(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (name LIKE '%' || :query || '%') AND isArchived = 0 ORDER BY name ASC")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchived(id: Long, isArchived: Boolean)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
