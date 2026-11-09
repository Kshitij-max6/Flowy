package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Category
import com.example.domain.model.CategoryType

@Entity(
    tableName = "categories",
    indices = [Index(value = ["parentCategoryId"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val type: String, // EXPENSE, INCOME
    val colorHex: String,
    val isCustom: Boolean,
    val parentCategoryId: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        iconName = iconName,
        type = CategoryType.valueOf(type),
        colorHex = colorHex,
        isCustom = isCustom,
        parentCategoryId = parentCategoryId,
        isFavorite = isFavorite,
        isArchived = isArchived,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            iconName = category.iconName,
            type = category.type.name,
            colorHex = category.colorHex,
            isCustom = category.isCustom,
            parentCategoryId = category.parentCategoryId,
            isFavorite = category.isFavorite,
            isArchived = category.isArchived,
            sortOrder = category.sortOrder
        )
    }
}
