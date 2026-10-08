package com.example.domain.sms

sealed class BankSmsParseResult {
    data class Success(
        val transaction: ParsedBankTransaction
    ) : BankSmsParseResult()

    data class Ignored(
        val reason: String,
        val details: String? = null
    ) : BankSmsParseResult()
}

enum class TransactionType {
    DEBIT,
    CREDIT
}

data class ParsedBankTransaction(
    val amount: Double,
    val type: TransactionType,
    val bankName: String,
    val accountInfo: String,
    val paymentMethod: String,
    val payeeOrMerchant: String,
    val referenceId: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val rawSender: String,
    val rawBody: String,
    val uniqueHash: String
)
