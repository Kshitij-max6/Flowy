package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.CreditCard
import com.example.domain.model.CreditCardStatus

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bankName: String,
    val cardNumberMasked: String,
    val creditLimit: Double,
    val outstandingBalance: Double,
    val billingCycleDay: Int,
    val paymentDueDay: Int,
    val linkedAccountId: Long?,
    val currency: String,
    val cardColorHex: String,
    val status: String // ACTIVE, ARCHIVED
) {
    fun toDomain(): CreditCard = CreditCard(
        id = id,
        name = name,
        bankName = bankName,
        cardNumberMasked = cardNumberMasked,
        creditLimit = creditLimit,
        outstandingBalance = outstandingBalance,
        billingCycleDay = billingCycleDay,
        paymentDueDay = paymentDueDay,
        linkedAccountId = linkedAccountId,
        currency = currency,
        cardColorHex = cardColorHex,
        status = try { CreditCardStatus.valueOf(status) } catch (e: Exception) { CreditCardStatus.ACTIVE }
    )

    companion object {
        fun fromDomain(card: CreditCard): CreditCardEntity = CreditCardEntity(
            id = card.id,
            name = card.name,
            bankName = card.bankName,
            cardNumberMasked = card.cardNumberMasked,
            creditLimit = card.creditLimit,
            outstandingBalance = card.outstandingBalance,
            billingCycleDay = card.billingCycleDay,
            paymentDueDay = card.paymentDueDay,
            linkedAccountId = card.linkedAccountId,
            currency = card.currency,
            cardColorHex = card.cardColorHex,
            status = card.status.name
        )
    }
}
