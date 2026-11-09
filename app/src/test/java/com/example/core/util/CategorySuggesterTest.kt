package com.example.core.util

import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggesterTest {
    private val cats = listOf(
        Category(1, "Food & Dining", "x", CategoryType.EXPENSE),
        Category(102, "Cafes & Coffee", "x", CategoryType.EXPENSE, parentCategoryId = 1),
        Category(203, "Taxi & Cab", "x", CategoryType.EXPENSE, parentCategoryId = 2),
        Category(500, "My Custom", "x", CategoryType.EXPENSE),
        Category(1301, "Monthly Salary", "x", CategoryType.INCOME)
    )

    @Test fun keywordMatchesSubcategory() {
        assertEquals(102L, CategorySuggester.suggest("cutting chai", TransactionType.EXPENSE, cats, emptyList())?.categoryId)
        assertEquals(203L, CategorySuggester.suggest("Ola to office", TransactionType.EXPENSE, cats, emptyList())?.categoryId)
    }

    @Test fun historyBeatsKeywords() {
        val history = listOf(Transaction(1, 40.0, TransactionType.EXPENSE, 500, 1, note = "chai"))
        val s = CategorySuggester.suggest("Chai", TransactionType.EXPENSE, cats, history)
        assertEquals(500L, s?.categoryId)
        assertEquals(CategorySuggester.Source.HISTORY, s?.source)
    }

    @Test fun incomeUsesIncomeCategories() {
        assertEquals(1301L, CategorySuggester.suggest("salary", TransactionType.INCOME, cats, emptyList())?.categoryId)
    }

    @Test fun unknownTextGivesNothing() {
        assertNull(CategorySuggester.suggest("xyzzy", TransactionType.EXPENSE, cats, emptyList()))
    }
}
