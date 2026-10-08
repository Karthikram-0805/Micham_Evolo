package com.example.utils

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats amounts using Indian numbering system:
     * e.g. 500 -> ₹500
     *      1000 -> ₹1,000
     *      50000 -> ₹50,000
     *      100000 -> ₹1,00,000
     *      10000000 -> ₹1,00,00,000
     */
    fun format(amount: Double, currencySymbol: String = "₹"): String {
        return formatIndian(amount, currencySymbol)
    }

    fun formatIndian(amount: Double, currencySymbol: String = "₹", showDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val positiveAmount = abs(amount)

        val longPart = positiveAmount.toLong()
        val decimalPart = if (showDecimals) {
            val dec = ((positiveAmount - longPart) * 100).toLong()
            if (dec > 0) String.format(Locale.US, ".%02d", dec) else ""
        } else ""

        val formattedLong = formatIndianLong(longPart)
        val prefix = if (isNegative) "-$currencySymbol" else currencySymbol

        return "$prefix$formattedLong$decimalPart"
    }

    private fun formatIndianLong(number: Long): String {
        val s = number.toString()
        if (s.length <= 3) return s

        val lastThree = s.substring(s.length - 3)
        val rest = s.substring(0, s.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        val formattedRest = sb.reverse().toString()
        return "$formattedRest,$lastThree"
    }
}
