package com.example

import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsParser
import com.example.domain.sms.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class WalletRecalculationTest {

    data class LedgerState(
        var salary: Double = 10000.0,
        val credits: MutableList<Pair<Long, Double>> = mutableListOf(),
        val debits: MutableList<Pair<Long, Double>> = mutableListOf()
    ) {
        val totalCredits: Double
            get() = BigDecimal(credits.sumOf { it.second }).setScale(2, RoundingMode.HALF_UP).toDouble()

        val totalDebits: Double
            get() = BigDecimal(debits.sumOf { it.second }).setScale(2, RoundingMode.HALF_UP).toDouble()

        val remainingBalance: Double
            get() = BigDecimal(salary + totalCredits - totalDebits).setScale(2, RoundingMode.HALF_UP).toDouble()

        fun addCredit(id: Long, amount: Double) {
            credits.add(id to amount)
        }

        fun deleteCredit(id: Long) {
            credits.removeAll { it.first == id }
        }

        fun addDebit(id: Long, amount: Double) {
            debits.add(id to amount)
        }

        fun deleteDebit(id: Long) {
            debits.removeAll { it.first == id }
        }
    }

    @Test
    fun testInitialWalletBalance() {
        val ledger = LedgerState(salary = 10000.0)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testDebitAndRecalculationOnDeletion() {
        // Initial wallet balance: Rs. 10,000
        val ledger = LedgerState(salary = 10000.0)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)

        // Transaction 1: Debit Rs. 2,000
        ledger.addDebit(id = 1L, amount = 2000.0)
        assertEquals(8000.0, ledger.remainingBalance, 0.001)

        // Delete this debit transaction -> wallet balance must automatically return to Rs. 10,000
        ledger.deleteDebit(id = 1L)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testCreditAndRecalculationOnDeletion() {
        // Initial wallet balance: Rs. 10,000
        val ledger = LedgerState(salary = 10000.0)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)

        // Transaction 2: Credit Rs. 5,000
        ledger.addCredit(id = 2L, amount = 5000.0)
        assertEquals(15000.0, ledger.remainingBalance, 0.001)

        // Delete this credit transaction -> wallet balance must automatically return to Rs. 10,000
        ledger.deleteCredit(id = 2L)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testManualExpenseDeletionRestoresBalance() {
        val ledger = LedgerState(salary = 10000.0)
        ledger.addDebit(id = 101L, amount = 750.0)
        assertEquals(9250.0, ledger.remainingBalance, 0.001)

        ledger.deleteDebit(id = 101L)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testManualIncomeDeletionDeductsBalance() {
        val ledger = LedgerState(salary = 10000.0)
        ledger.addCredit(id = 201L, amount = 1200.0)
        assertEquals(11200.0, ledger.remainingBalance, 0.001)

        ledger.deleteCredit(id = 201L)
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testPromotionalSmsDoNotAffectWalletBalance() {
        val ledger = LedgerState(salary = 10000.0)

        // Example SMS 1
        val promo1 = "Convert Rs. 25366 on HDFC Bank Credit Card xx7609 to SmartEMI at rates from 0.99%. Limited period. https://1.hdfc.bank.in/HDFCBK/s/bZGvPY3b T&C"
        val parse1 = BankSmsParser.parse("VK-HDFCBK", promo1)
        assertTrue(parse1 is BankSmsParseResult.Ignored)
        // Balance remains strictly unchanged!
        assertEquals(10000.0, ledger.remainingBalance, 0.001)

        // Example SMS 2
        val promo2 = "Convert recent HDFC Bank Credit Card x7378 spends of Rs.3066 into EMIs with SmartEMI. Don't miss it, click here: https://1.hdfc.bank.in/HDFCBK/s/AA11mLL5 T&C"
        val parse2 = BankSmsParser.parse("VK-HDFCBK", promo2)
        assertTrue(parse2 is BankSmsParseResult.Ignored)
        // Balance remains strictly unchanged!
        assertEquals(10000.0, ledger.remainingBalance, 0.001)
    }

    @Test
    fun testDecimalPrecisionArithmetic() {
        val ledger = LedgerState(salary = 10000.0)
        ledger.addDebit(1L, 199.99)
        ledger.addDebit(2L, 50.01)
        assertEquals(9750.00, ledger.remainingBalance, 0.001)

        ledger.deleteDebit(1L)
        assertEquals(9949.99, ledger.remainingBalance, 0.001)
    }
}
