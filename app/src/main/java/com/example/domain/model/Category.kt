package com.example.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val type: CategoryType,
    val colorHex: String = "#10B981",
    val isCustom: Boolean = false,
    val parentCategoryId: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)
