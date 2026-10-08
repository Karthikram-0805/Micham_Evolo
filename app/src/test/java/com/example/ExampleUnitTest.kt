package com.example

import com.example.sms.BankSmsParser
import com.example.sms.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test genuine bank debit via UPI`() {
        val sms = "Rs. 500 debited from A/c XX1234 via UPI to Swiggy on 06-Oct-26. Avl Bal: Rs 24,500."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("XX1234", result.accountInfo)
        assertEquals("UPI", result.paymentMethod)
        assertEquals("Food", result.inferredCategoryName)
    }

    @Test
    fun `test genuine bank credit into account`() {
        val sms = "Rs. 10,000 credited to A/c XX1234 on 06-Oct-26 via NEFT from Employer. Avl Bal: Rs 45,000."
        val result = BankSmsParser.parse("AD-SBIINB", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(10000.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("XX1234", result.accountInfo)
    }

    @Test
    fun `test credit card expense with currency symbol`() {
        val sms = "INR 1,250.50 spent on your Credit Card XX9988 at HPCL Fuel Station on 06-Oct-26."
        val result = BankSmsParser.parse("AX-ICICIB", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(1250.50, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Credit Card", result.paymentMethod)
        assertEquals("Petrol", result.inferredCategoryName)
    }

    @Test
    fun `test pre-approved loan message is strictly ignored`() {
        val sms = "Congratulations! You are eligible for a pre-approved personal loan of Rs. 2,00,000. Apply now."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertNull("Loan offer message MUST be ignored and not treated as income!", result)
    }

    @Test
    fun `test loan eligibility message is strictly ignored`() {
        val sms = "Dear Customer, you are eligible for instant loan of Rs. 5,00,000 at low interest rate. Click to avail loan."
        val result = BankSmsParser.parse("AX-BAJAJ", sms, System.currentTimeMillis())

        assertNull("Loan eligibility message MUST be ignored!", result)
    }

    @Test
    fun `test OTP messages are ignored`() {
        val sms = "Your OTP is 492810 for HDFC netbanking login. Valid for 10 mins. Do not share with anyone."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertNull("OTP message must be ignored!", result)
    }

    @Test
    fun `test EMI and bill reminders are ignored`() {
        val sms = "Reminder: Your EMI payment of Rs. 4,500 is due on 10th Oct. Please pay on time."
        val result = BankSmsParser.parse("VM-KOTAKB", sms, System.currentTimeMillis())

        assertNull("EMI reminder must be ignored!", result)
    }

    @Test
    fun `test failed or declined transactions are ignored`() {
        val sms = "Transaction of Rs. 1,500 on A/c XX1234 declined due to insufficient funds."
        val result = BankSmsParser.parse("VK-HDFCBK", sms, System.currentTimeMillis())

        assertNull("Declined transaction must be ignored!", result)
    }
}
