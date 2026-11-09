package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Account
import com.example.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // BANK, CASH, UPI, etc.
    val balance: Double,
    val currency: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = try { AccountType.valueOf(type) } catch (e: Exception) { AccountType.BANK },
        balance = balance,
        currency = currency,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type.name,
            balance = account.balance,
            currency = account.currency,
            iconName = account.iconName,
            colorHex = account.colorHex,
            isDefault = account.isDefault
        )
    }
}
