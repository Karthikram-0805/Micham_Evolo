package com.example

import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsParser
import com.example.domain.sms.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGenuineBankDebitViaUpi() {
        val sms = "Rs. 500 debited from A/c XX1234 via UPI to Swiggy on 06-Oct-26. Avl Bal: Rs 24,500."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(500.0, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertTrue(tx.accountInfo.contains("1234"))
        assertEquals("UPI", tx.paymentMethod)
    }

    @Test
    fun testGenuineBankCreditIntoAccount() {
        val sms = "Rs. 10,000 credited to A/c XX1234 on 06-Oct-26 via NEFT from Employer. Avl Bal: Rs 45,000."
        val result = BankSmsParser.parse("AD-SBIINB", sms, System.currentTimeMillis())

        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(10000.0, tx.amount, 0.001)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertTrue(tx.accountInfo.contains("1234"))
    }

    @Test
    fun testCreditCardExpenseWithCurrencySymbol() {
        val sms = "INR 1,250.50 spent on your Credit Card XX9988 at HPCL Fuel Station on 06-Oct-26."
        val result = BankSmsParser.parse("AX-ICICIB", sms, System.currentTimeMillis())

        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(1250.50, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("Credit Card", tx.paymentMethod)
    }

    @Test
    fun testPreApprovedLoanMessageStrictlyIgnored() {
        val sms = "Congratulations! You are eligible for a pre-approved personal loan of Rs. 2,00,000. Apply now."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertTrue("Loan offer message MUST be ignored!", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testLoanEligibilityMessageStrictlyIgnored() {
        val sms = "Dear Customer, you are eligible for instant loan of Rs. 5,00,000 at low interest rate. Click to avail loan."
        val result = BankSmsParser.parse("AX-BAJAJ", sms, System.currentTimeMillis())

        assertTrue("Loan eligibility message MUST be ignored!", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testOtpMessagesAreIgnored() {
        val sms = "Your OTP is 492810 for HDFC netbanking login. Valid for 10 mins. Do not share with anyone."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertTrue("OTP message must be ignored!", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testEmiAndBillRemindersAreIgnored() {
        val sms = "Reminder: Your EMI payment of Rs. 4,500 is due on 10th Oct. Please pay on time."
        val result = BankSmsParser.parse("VM-KOTAKB", sms, System.currentTimeMillis())

        assertTrue("EMI reminder must be ignored!", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testFailedOrDeclinedTransactionsAreIgnored() {
        val sms = "Transaction of Rs. 1,500 on A/c XX1234 declined due to insufficient funds."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertTrue("Declined transaction must be ignored!", result is BankSmsParseResult.Ignored)
    }
}
