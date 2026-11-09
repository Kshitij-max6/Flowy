package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY id ASC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY id ASC")
    fun getActiveLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    fun getLoanByIdFlow(id: Long): Flow<LoanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("UPDATE loans SET remainingBalance = :balance WHERE id = :id")
    suspend fun updateRemainingBalance(id: Long, balance: Double)

    @Query("UPDATE loans SET status = :status WHERE id = :id")
    suspend fun updateLoanStatus(id: Long, status: String)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    // Payments
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<LoanPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LoanPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<LoanPaymentEntity>)

    @Delete
    suspend fun deletePayment(payment: LoanPaymentEntity)
}
