package com.example.domain.sms

import com.example.data.entity.BankEntity
import java.util.Locale

object BankTrackingHelper {

    /**
     * Matches an SMS to a registered bank entity using sender pattern, bankCode, and bankName.
     */
    fun findMatchingBank(
        sender: String,
        body: String,
        bankCode: String,
        bankName: String,
        registeredBanks: List<BankEntity>
    ): BankEntity? {
        val upperSender = sender.uppercase(Locale.ENGLISH)
        val upperBody = body.uppercase(Locale.ENGLISH)
        val upperCode = bankCode.uppercase(Locale.ENGLISH)
        val upperName = bankName.uppercase(Locale.ENGLISH)

        // 1. Direct match on bankCode
        if (upperCode.isNotBlank() && upperCode != "OTHER") {
            val byCode = registeredBanks.find { it.bankCode.equals(upperCode, ignoreCase = true) }
            if (byCode != null) return byCode
        }

        // 2. Direct match on bankName
        val byName = registeredBanks.find {
            it.bankName.equals(upperName, ignoreCase = true) ||
            upperName.contains(it.bankName, ignoreCase = true) ||
            it.bankName.contains(upperName, ignoreCase = true)
        }
        if (byName != null) return byName

        // 3. Match sender patterns
        for (bank in registeredBanks) {
            val patterns = bank.senderPatterns.split(",")
                .map { it.trim().uppercase(Locale.ENGLISH) }
                .filter { it.isNotBlank() }
            for (p in patterns) {
                if (upperSender.contains(p) || upperBody.contains(p)) {
                    return bank
                }
            }
        }

        return null
    }

    /**
     * Checks if bank tracking is currently ON and if message meets timestamp checkpoint.
     * Returns Pair(isAllowed, reason)
     */
    fun isTrackingAllowed(
        bank: BankEntity?,
        messageTimestamp: Long,
        isHistoricalImport: Boolean = false
    ): Pair<Boolean, String> {
        if (bank == null) {
            return false to "Bank not identified or not registered in My Banks"
        }
        if (!bank.isTrackingEnabled) {
            return false to "Tracking is OFF for ${bank.bankName}"
        }
        if (!isHistoricalImport && bank.enabledAtTimestamp > 0L && messageTimestamp < bank.enabledAtTimestamp) {
            return false to "SMS received before tracking was enabled for ${bank.bankName}"
        }
        return true to "Tracking active for ${bank.bankName}"
    }
}
