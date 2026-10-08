package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Micham Evlo", appName)
    }

    @Test
    fun `test indian currency formatter`() {
        assertEquals("₹500", CurrencyFormatter.formatIndian(500.0, "₹"))
        assertEquals("₹1,000", CurrencyFormatter.formatIndian(1000.0, "₹"))
        assertEquals("₹50,000", CurrencyFormatter.formatIndian(50000.0, "₹"))
        assertEquals("₹1,00,000", CurrencyFormatter.formatIndian(100000.0, "₹"))
        assertEquals("₹10,00,000", CurrencyFormatter.formatIndian(1000000.0, "₹"))
    }

    @Test
    fun `test financial balance scenarios from spec`() {
        val salary = 50000.0
        val foodExpense = 500.0
        val petrolExpense = 1000.0
        val bonusIncome = 5000.0

        // TEST 1
        var spent = foodExpense
        var remaining = salary - spent
        assertEquals(500.0, spent, 0.001)
        assertEquals(49500.0, remaining, 0.001)

        // TEST 2
        spent += petrolExpense
        remaining = salary - spent
        assertEquals(1500.0, spent, 0.001)
        assertEquals(48500.0, remaining, 0.001)

        // TEST 3 (Delete 500 food expense)
        spent -= foodExpense
        remaining = salary - spent
        assertEquals(1000.0, spent, 0.001)
        assertEquals(49000.0, remaining, 0.001)

        // TEST 4 (Add income bonus 5000)
        val available = salary + bonusIncome
        remaining = available - spent
        assertEquals(55000.0, available, 0.001)
        assertEquals(54000.0, remaining, 0.001)
    }
}
