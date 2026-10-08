package com.example

import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsParser
import com.example.domain.sms.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun testDebitTransactionViaUpi() {
        val message = "Rs. 500 debited from A/c XX1234 via UPI to SWIGGY. Avl Bal Rs. 14,500.00."
        val result = BankSmsParser.parse("VK-HDFCBK", message)

        assertTrue(result is BankSmsParseResult.Success)
        val transaction = (result as BankSmsParseResult.Success).transaction
        assertEquals(500.0, transaction.amount, 0.001)
        assertEquals(TransactionType.DEBIT, transaction.type)
        assertEquals("HDFC", transaction.bankName)
        assertEquals("UPI", transaction.paymentMethod)
        assertTrue(transaction.accountInfo.contains("1234"))
    }

    @Test
    fun testCreditTransactionDeposit() {
        val message = "Rs. 10,000 credited to A/c XX1234 on 06-10-24 by transfer. Avl Bal is Rs. 55,000.00."
        val result = BankSmsParser.parse("AD-SBIINB", message)

        assertTrue(result is BankSmsParseResult.Success)
        val transaction = (result as BankSmsParseResult.Success).transaction
        assertEquals(10000.0, transaction.amount, 0.001)
        assertEquals(TransactionType.CREDIT, transaction.type)
        assertEquals("SBI", transaction.bankName)
        assertTrue(transaction.accountInfo.contains("1234"))
    }

    @Test
    fun testLoanOfferIgnored() {
        val loanMessages = listOf(
            "Congratulations! You are eligible for a pre-approved personal loan of ₹5,00,000 at low interest.",
            "Dear Customer, your loan eligibility of Rs. 2,00,000 is approved. Click here to avail instant cash.",
            "HDFC Bank: Pre-approved car loan offer of Rs. 8,00,000 waiting for you. Apply now.",
            "Avail easy EMI option on your credit card. Convert purchase of Rs. 15,000 to EMI."
        )

        for (msg in loanMessages) {
            val result = BankSmsParser.parse("VK-HDFCBK", msg)
            assertTrue("Expected loan message to be ignored: $msg", result is BankSmsParseResult.Ignored)
        }
    }

    @Test
    fun testOtpMessagesIgnored() {
        val otpMessages = listOf(
            "123456 is your secret OTP for HDFC Bank login. Do NOT share with anyone.",
            "Your one time password (OTP) is 987654 for transaction at Amazon. Valid for 10 mins.",
            "849201 is verification code for UPI registration. Never share your OTP."
        )

        for (msg in otpMessages) {
            val result = BankSmsParser.parse("VK-HDFCBK", msg)
            assertTrue("Expected OTP to be ignored: $msg", result is BankSmsParseResult.Ignored)
        }
    }

    @Test
    fun testBalanceInquiryWithoutTransactionIgnored() {
        val msg = "Available balance in A/c XX1234 is Rs. 24,500.00 as on 06-10-24."
        val result = BankSmsParser.parse("AX-ICICIB", msg)
        assertTrue("Expected balance inquiry to be ignored", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testFailedTransactionIgnored() {
        val msg = "Your payment of Rs. 450.00 to ZOMATO failed due to bank network error."
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue("Expected failed transaction to be ignored", result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testAmountsWithCommaAndDecimals() {
        val msg = "₹1,250.50 spent on Credit Card ending 4321 at RELIANCE on 06-10-24."
        val result = BankSmsParser.parse("AXISBK", msg)

        assertTrue(result is BankSmsParseResult.Success)
        val transaction = (result as BankSmsParseResult.Success).transaction
        assertEquals(1250.50, transaction.amount, 0.001)
        assertEquals(TransactionType.DEBIT, transaction.type)
        assertEquals("Credit Card", transaction.paymentMethod)
    }

    @Test
    fun testUserPromptSpecificExample() {
        val debitMsg = "Rs. 1,250 debited from A/c XX1234 via UPI"
        val debitResult = BankSmsParser.parse("HDFCBK", debitMsg)
        assertTrue(debitResult is BankSmsParseResult.Success)
        val debitTx = (debitResult as BankSmsParseResult.Success).transaction
        assertEquals(1250.0, debitTx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, debitTx.type)
        assertTrue(debitTx.accountInfo.contains("1234"))
        assertEquals("UPI", debitTx.paymentMethod)

        val creditMsg = "Rs. 10,000 credited to A/c XX1234"
        val creditResult = BankSmsParser.parse("HDFCBK", creditMsg)
        assertTrue(creditResult is BankSmsParseResult.Success)
        val creditTx = (creditResult as BankSmsParseResult.Success).transaction
        assertEquals(10000.0, creditTx.amount, 0.001)
        assertEquals(TransactionType.CREDIT, creditTx.type)
    }

    @Test
    fun testDifferentTransactionsWithSameAmountHaveUniqueHashes() {
        val msg1 = "Rs. 500 debited from A/c XX1234 via UPI to SWIGGY. Ref 11223344"
        val msg2 = "Rs. 500 debited from A/c XX1234 via UPI to ZOMATO. Ref 99887766"

        val res1 = BankSmsParser.parse("HDFCBK", msg1) as BankSmsParseResult.Success
        val res2 = BankSmsParser.parse("HDFCBK", msg2) as BankSmsParseResult.Success

        assertEquals(500.0, res1.transaction.amount, 0.001)
        assertEquals(500.0, res2.transaction.amount, 0.001)
        assertTrue("Transactions with different references must have different hashes",
            res1.transaction.uniqueHash != res2.transaction.uniqueHash)
    }

    @Test
    fun testHdfcSmartEmiPromotionalMessage1Ignored() {
        val msg = "Convert Rs. 25366 on HDFC Bank Credit Card xx7609 to SmartEMI at rates from 0.99%. Limited period. https://1.hdfc.bank.in/HDFCBK/s/bZGvPY3b T&C"
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue("Promotional SmartEMI message 1 must be ignored and not change wallet balance",
            result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testHdfcSmartEmiPromotionalMessage2Ignored() {
        val msg = "Convert recent HDFC Bank Credit Card x7378 spends of Rs.3066 into EMIs with SmartEMI. Don't miss it, click here: https://1.hdfc.bank.in/HDFCBK/s/AA11mLL5 T&C"
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue("Promotional SmartEMI message 2 with 'spends' must be ignored and not change wallet balance",
            result is BankSmsParseResult.Ignored)
    }

    @Test
    fun testGenuineBankCredit5000() {
        val msg = "Rs. 5,000 credited to A/c XX1234 on 08-10-26 by UPI. Avl Bal Rs. 15,000."
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(5000.0, tx.amount, 0.001)
        assertEquals(TransactionType.CREDIT, tx.type)
    }

    @Test
    fun testGenuineBankDebit2000() {
        val msg = "Rs. 2,000 debited from A/c XX1234 on 08-10-26 via UPI to SWIGGY. Avl Bal Rs. 13,000."
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(2000.0, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
    }

    @Test
    fun testActualEmiPaymentProcessedAsDebit() {
        val msg = "Your EMI of Rs. 3,500 has been debited from A/c XX1234 for Loan. Avl Bal Rs. 10,000."
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(3500.0, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
    }

    @Test
    fun testGenuineCreditCardPurchaseAlert() {
        val msg = "Alert: Rs. 450 spent on HDFC Bank Credit Card ending 7609 at SWIGGY. Avl Limit Rs. 85,000."
        val result = BankSmsParser.parse("VK-HDFCBK", msg)
        assertTrue(result is BankSmsParseResult.Success)
        val tx = (result as BankSmsParseResult.Success).transaction
        assertEquals(450.0, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("Credit Card", tx.paymentMethod)
    }
}
