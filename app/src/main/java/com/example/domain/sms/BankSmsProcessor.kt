package com.example.domain.sms

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.database.AppDatabase
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProcessedSmsEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.DialogueEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BankSmsProcessResult {
    data class AddedAsExpense(
        val amount: Double,
        val bankName: String,
        val accountInfo: String,
        val paymentMethod: String,
        val date: String,
        val reaction: String
    ) : BankSmsProcessResult()

    data class AddedAsIncome(
        val amount: Double,
        val bankName: String,
        val accountInfo: String,
        val date: String,
        val reaction: String
    ) : BankSmsProcessResult()

    data class Duplicate(
        val message: String = "Transaction already processed previously"
    ) : BankSmsProcessResult()

    data class Ignored(
        val reason: String
    ) : BankSmsProcessResult()
}

object BankSmsProcessor {

    suspend fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        forceProcess: Boolean = false
    ): BankSmsProcessResult = withContext(Dispatchers.IO) {

        val userPrefs = UserPreferencesRepository(context)
        val settings = userPrefs.userSettingsFlow.first()

        if (!forceProcess && !settings.autoSmsDetectionEnabled) {
            return@withContext BankSmsProcessResult.Ignored("Auto SMS detection is turned off in settings")
        }

        // Parse SMS
        val parseResult = BankSmsParser.parse(sender, body, timestamp)
        val transaction = when (parseResult) {
            is BankSmsParseResult.Success -> parseResult.transaction
            is BankSmsParseResult.Ignored -> return@withContext BankSmsProcessResult.Ignored(parseResult.reason)
        }

        // Check if duplicate in Database
        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val processedDao = db.processedSmsDao()

        val existingCount = processedDao.countByHash(transaction.uniqueHash)
        if (existingCount > 0) {
            return@withContext BankSmsProcessResult.Duplicate(
                "Duplicate skipped: ${transaction.bankName} ₹${transaction.amount} on ${transaction.date}"
            )
        }

        // Process Debit (Expense) or Credit (Income)
        val result = if (transaction.type == TransactionType.DEBIT) {
            val categories = db.categoryDao().getAllCategoriesSnapshot()
            val categoryId = matchCategoryForTransaction(transaction, categories)

            val note = buildString {
                append("Bank SMS: ")
                append(transaction.bankName)
                append(" (")
                append(transaction.accountInfo)
                append(")")
                if (transaction.payeeOrMerchant.isNotBlank() &&
                    transaction.payeeOrMerchant != "Merchant / Payee"
                ) {
                    append(" - ")
                    append(transaction.payeeOrMerchant)
                }
                if (transaction.referenceId.isNotBlank()) {
                    append(" [Ref: ")
                    append(transaction.referenceId)
                    append("]")
                }
            }

            val expenseDate = if (transaction.date.isNotBlank() && transaction.date >= "2026-01-01") {
                transaction.date
            } else {
                com.example.utils.DateUtils.today()
            }
            val expenseTime = if (transaction.time.isNotBlank()) transaction.time else com.example.utils.DateUtils.nowTime()

            val expense = ExpenseEntity(
                amount = transaction.amount,
                categoryId = categoryId,
                date = expenseDate,
                time = expenseTime,
                paymentMethod = transaction.paymentMethod,
                note = note
            )

            val expenseId = db.expenseDao().insertExpense(expense)

            // Save in processed_sms to prevent future duplicates
            val processedRecord = ProcessedSmsEntity(
                smsHash = transaction.uniqueHash,
                sender = transaction.rawSender,
                body = transaction.rawBody,
                amount = transaction.amount,
                transactionType = "DEBIT",
                bankName = transaction.bankName,
                accountInfo = transaction.accountInfo,
                paymentMethod = transaction.paymentMethod,
                date = expenseDate,
                time = expenseTime,
                referenceId = transaction.referenceId,
                associatedId = expenseId,
                timestamp = System.currentTimeMillis()
            )
            processedDao.insert(processedRecord)

            // Generate Tamil reaction
            val reaction = DialogueEngine.getExpenseReaction(
                amount = transaction.amount,
                monthlySalary = settings.defaultSalary,
                remainingBalance = settings.defaultSalary - transaction.amount,
                isRoastMode = settings.reactionIntensity == "ROAST"
            )

            // Show Toast if enabled
            if (settings.autoSmsNotificationEnabled) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context.applicationContext,
                        "💸 Bank SMS: ₹${transaction.amount} debited (${transaction.bankName}). $reaction",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            BankSmsProcessResult.AddedAsExpense(
                amount = transaction.amount,
                bankName = transaction.bankName,
                accountInfo = transaction.accountInfo,
                paymentMethod = transaction.paymentMethod,
                date = transaction.date,
                reaction = reaction
            )
        } else {
            // CREDIT -> Add as Income
            val note = buildString {
                append("Bank SMS Credit: ")
                append(transaction.bankName)
                append(" (")
                append(transaction.accountInfo)
                append(")")
                if (transaction.referenceId.isNotBlank()) {
                    append(" [Ref: ")
                    append(transaction.referenceId)
                    append("]")
                }
            }

            val incomeDate = if (transaction.date.isNotBlank() && transaction.date >= "2026-01-01") {
                transaction.date
            } else {
                com.example.utils.DateUtils.today()
            }

            val income = IncomeEntity(
                amount = transaction.amount,
                incomeType = "Bank Deposit",
                date = incomeDate,
                note = note
            )

            val incomeId = db.incomeDao().insertIncome(income)

            // Save in processed_sms
            val processedRecord = ProcessedSmsEntity(
                smsHash = transaction.uniqueHash,
                sender = transaction.rawSender,
                body = transaction.rawBody,
                amount = transaction.amount,
                transactionType = "CREDIT",
                bankName = transaction.bankName,
                accountInfo = transaction.accountInfo,
                paymentMethod = transaction.paymentMethod,
                date = incomeDate,
                time = transaction.time,
                referenceId = transaction.referenceId,
                associatedId = incomeId,
                timestamp = System.currentTimeMillis()
            )
            processedDao.insert(processedRecord)

            val reaction = "Bank-la ₹${transaction.amount} credit aayiduchu! Micham ippo super balance! 💰🎉"

            if (settings.autoSmsNotificationEnabled) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context.applicationContext,
                        "💰 Bank SMS: ₹${transaction.amount} credited to ${transaction.accountInfo}! Party eppo? 🎉",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            BankSmsProcessResult.AddedAsIncome(
                amount = transaction.amount,
                bankName = transaction.bankName,
                accountInfo = transaction.accountInfo,
                date = transaction.date,
                reaction = reaction
            )
        }

        result
    }

    fun suggestDefaultDescription(transaction: ParsedBankTransaction): String {
        val lowerText = (transaction.rawBody + " " + transaction.payeeOrMerchant).lowercase()

        if (transaction.payeeOrMerchant.isNotBlank() &&
            transaction.payeeOrMerchant != "Merchant / Payee" &&
            transaction.payeeOrMerchant != "Sender / Source"
        ) {
            return transaction.payeeOrMerchant
        }

        if (transaction.type == TransactionType.CREDIT) {
            return when {
                lowerText.contains("salary") -> "Salary"
                lowerText.contains("refund") -> "Refund"
                lowerText.contains("cashback") -> "Cashback"
                lowerText.contains("freelance") -> "Freelance"
                lowerText.contains("bonus") -> "Bonus"
                else -> "Bank Deposit"
            }
        }

        return when {
            listOf("swiggy", "zomato", "restaurant", "cafe", "tea", "coffee", "bistro", "hotel", "food", "eat", "biryani", "bakery").any { lowerText.contains(it) } -> "Food"
            listOf("uber", "ola", "rapido", "metro", "bus", "irctc", "rail", "travel", "flight", "makemytrip").any { lowerText.contains(it) } -> "Travel"
            listOf("petrol", "fuel", "diesel", "hpcl", "bpcl", "iocl", "shell").any { lowerText.contains(it) } -> "Petrol"
            listOf("blinkit", "zepto", "instamart", "bigbasket", "dmart", "grocery", "supermarket").any { lowerText.contains(it) } -> "Groceries"
            listOf("amazon", "flipkart", "myntra", "meesho", "shopping", "ajio").any { lowerText.contains(it) } -> "Shopping"
            listOf("electricity", "eb bill", "water", "gas", "wifi", "broadband", "airtel", "jio", "vi", "recharge", "bill").any { lowerText.contains(it) } -> "Bills"
            listOf("netflix", "spotify", "prime", "hotstar", "youtube", "subscript").any { lowerText.contains(it) } -> "Subscriptions"
            listOf("cinema", "pvr", "inox", "movie", "bookmyshow", "entertainment").any { lowerText.contains(it) } -> "Entertainment"
            listOf("pharmacy", "apollo", "medplus", "hospital", "clinic", "health", "doctor").any { lowerText.contains(it) } -> "Health"
            listOf("rent", "owner", "flat", "pg").any { lowerText.contains(it) } -> "Rent"
            listOf("emi", "loan emi").any { lowerText.contains(it) } -> "EMI"
            else -> transaction.paymentMethod
        }
    }

    fun matchCategoryForTransaction(
        transaction: ParsedBankTransaction,
        categories: List<com.example.data.entity.CategoryEntity>
    ): Long {
        val lowerText = (transaction.rawBody + " " + transaction.payeeOrMerchant).lowercase()

        val keywordCategoryMap = listOf(
            listOf("swiggy", "zomato", "restaurant", "cafe", "tea", "coffee", "bistro", "hotel", "food", "eat") to "Food",
            listOf("uber", "ola", "rapido", "metro", "bus", "irctc", "rail", "travel", "flight", "makemytrip") to "Travel",
            listOf("petrol", "fuel", "diesel", "hpcl", "bpcl", "iocl", "shell") to "Petrol",
            listOf("blinkit", "zepto", "instamart", "bigbasket", "dmart", "grocery", "supermarket") to "Groceries",
            listOf("amazon", "flipkart", "myntra", "meesho", "shopping", "ajio") to "Online Shopping",
            listOf("electricity", "eb bill", "water", "gas", "wifi", "broadband", "airtel", "jio", "vi", "recharge", "bill") to "Bills",
            listOf("netflix", "spotify", "prime", "hotstar", "youtube", "subscript") to "Subscriptions",
            listOf("cinema", "pvr", "inox", "movie", "bookmyshow", "entertainment") to "Entertainment",
            listOf("pharmacy", "apollo", "medplus", "hospital", "clinic", "health", "doctor") to "Health",
            listOf("rent", "owner", "flat", "pg") to "Rent",
            listOf("emi", "loan emi") to "EMI"
        )

        for ((keywords, catName) in keywordCategoryMap) {
            if (keywords.any { lowerText.contains(it) }) {
                val found = categories.find { it.name.equals(catName, ignoreCase = true) }
                if (found != null) return found.id
            }
        }

        // Fallback to "Other" or first category
        return categories.find { it.name.equals("Other", ignoreCase = true) }?.id
            ?: categories.firstOrNull()?.id
            ?: 1L
    }
}
