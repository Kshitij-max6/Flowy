package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Loan
import com.example.domain.model.LoanPayment
import com.example.domain.model.LoanStatus
import com.example.domain.model.LoanType

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lender: String,
    val loanType: String,
    val originalAmount: Double,
    val remainingBalance: Double,
    val interestRate: Double,
    val monthlyPayment: Double,
    val nextPaymentDate: Long,
    val startDate: Long,
    val endDate: Long?,
    val currency: String,
    val status: String,
    val linkedAccountId: Long?
) {
    fun toDomain(): Loan = Loan(
        id = id,
        name = name,
        lender = lender,
        loanType = try { LoanType.valueOf(loanType) } catch (e: Exception) { LoanType.PERSONAL },
        originalAmount = originalAmount,
        remainingBalance = remainingBalance,
        interestRate = interestRate,
        monthlyPayment = monthlyPayment,
        nextPaymentDate = nextPaymentDate,
        startDate = startDate,
        endDate = endDate,
        currency = currency,
        status = try { LoanStatus.valueOf(status) } catch (e: Exception) { LoanStatus.ACTIVE },
        linkedAccountId = linkedAccountId
    )

    companion object {
        fun fromDomain(loan: Loan): LoanEntity = LoanEntity(
            id = loan.id,
            name = loan.name,
            lender = loan.lender,
            loanType = loan.loanType.name,
            originalAmount = loan.originalAmount,
            remainingBalance = loan.remainingBalance,
            interestRate = loan.interestRate,
            monthlyPayment = loan.monthlyPayment,
            nextPaymentDate = loan.nextPaymentDate,
            startDate = loan.startDate,
            endDate = loan.endDate,
            currency = loan.currency,
            status = loan.status.name,
            linkedAccountId = loan.linkedAccountId
        )
    }
}

@Entity(
    tableName = "loan_payments",
    indices = [
        Index("loanId"),
        Index("paymentDate")
    ]
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val principalAmount: Double,
    val interestAmount: Double,
    val paymentDate: Long,
    val accountId: Long?,
    val note: String
) {
    fun toDomain(): LoanPayment = LoanPayment(
        id = id,
        loanId = loanId,
        amount = amount,
        principalAmount = principalAmount,
        interestAmount = interestAmount,
        paymentDate = paymentDate,
        accountId = accountId,
        note = note
    )

    companion object {
        fun fromDomain(payment: LoanPayment): LoanPaymentEntity = LoanPaymentEntity(
            id = payment.id,
            loanId = payment.loanId,
            amount = payment.amount,
            principalAmount = payment.principalAmount,
            interestAmount = payment.interestAmount,
            paymentDate = payment.paymentDate,
            accountId = payment.accountId,
            note = payment.note
        )
    }
}
