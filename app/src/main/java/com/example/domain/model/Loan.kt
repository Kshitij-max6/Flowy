package com.example.domain.model

data class Loan(
    val id: Long = 0,
    val name: String,
    val lender: String,
    val loanType: LoanType = LoanType.PERSONAL,
    val originalAmount: Double,
    val remainingBalance: Double,
    val interestRate: Double = 10.0, // Annual %
    val monthlyPayment: Double = 0.0, // EMI
    val nextPaymentDate: Long = System.currentTimeMillis(),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val currency: String = "₹",
    val status: LoanStatus = LoanStatus.ACTIVE,
    val linkedAccountId: Long? = null
) {
    val paidAmount: Double
        get() = (originalAmount - remainingBalance).coerceAtLeast(0.0)

    val progressPercentage: Float
        get() = if (originalAmount > 0) ((paidAmount / originalAmount).coerceIn(0.0, 1.0)).toFloat() else 0f
}

data class LoanPayment(
    val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val principalAmount: Double = amount,
    val interestAmount: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val accountId: Long? = null,
    val note: String = ""
)

data class LoanDetail(
    val loan: Loan,
    val payments: List<LoanPayment> = emptyList(),
    val linkedAccount: Account? = null
)

val LoanDetail.totalPrincipalPaid: Double
    get() = payments.sumOf { it.principalAmount }

val LoanDetail.totalInterestPaid: Double
    get() = payments.sumOf { it.interestAmount }
