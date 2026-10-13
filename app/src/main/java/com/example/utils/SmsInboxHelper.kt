package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.database.AppDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProcessedSmsEntity
import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsProcessResult
import com.example.domain.sms.BankSmsProcessor
import com.example.domain.sms.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedSmsItem(
    val transactionId: Long = 0L,
    val smsId: String,
    val sender: String,
    val body: String,
    val amount: Double,
    val type: TransactionType,
    val bankName: String,
    val accountInfo: String,
    val paymentMethod: String,
    val payeeOrMerchant: String,
    val referenceId: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val timestamp: Long,
    val uniqueHash: String,
    val defaultDescription: String,
    val suggestedCategoryId: Long,
    val isCreditCard: Boolean = false,
    val cardMaskedNumber: String = "",
    val cardName: String = ""
)

data class SmsScanSummary(
    val totalScanned: Int = 0,
    val debitsAdded: Int = 0,
    val creditsAdded: Int = 0,
    val duplicatesSkipped: Int = 0,
    val nonTransactionsIgnored: Int = 0,
    val totalDebitAmount: Double = 0.0,
    val totalCreditAmount: Double = 0.0
)

object SmsInboxHelper {

    /**
     * Scans SMS messages between fromTimestamp and toTimestamp.
     * Implements single transaction creation:
     * - Bank SMS Detected -> Checks if already recorded.
     * - If brand new: Creates EXACTLY ONE ExpenseEntity (for Debit) with status PENDING_REVIEW in database.
     * - If already created (e.g. by real-time listener): reuses existing transactionId.
     * - Category popup only updates the category on that single record.
     * - Credits are recorded directly as Income without category popup.
     */
    suspend fun scanUnprocessedBankSmsBetween(
        context: Context,
        fromTimestamp: Long,
        toTimestamp: Long,
        categories: List<CategoryEntity> = emptyList()
    ): List<DetectedSmsItem> = withContext(Dispatchers.IO) {
        val detectedList = mutableListOf<DetectedSmsItem>()
        val seenHashes = mutableSetOf<String>()

        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val processedDao = db.processedSmsDao()

        // 1. First, check if there are any existing PENDING_REVIEW debit transactions in DB
        val pendingFromDb = processedDao.getPendingReviewSms()
        for (pending in pendingFromDb) {
            if (pending.transactionType == "DEBIT" && pending.associatedId > 0L) {
                seenHashes.add(pending.smsHash)
                val catId = categories.find { it.name.equals(pending.description, ignoreCase = true) }?.id
                    ?: categories.firstOrNull()?.id ?: 1L
                detectedList.add(
                    DetectedSmsItem(
                        transactionId = pending.associatedId,
                        smsId = pending.smsId,
                        sender = pending.sender,
                        body = pending.body,
                        amount = pending.amount,
                        type = TransactionType.DEBIT,
                        bankName = pending.bankName,
                        accountInfo = pending.accountInfo,
                        paymentMethod = pending.paymentMethod,
                        payeeOrMerchant = pending.description,
                        referenceId = pending.referenceId,
                        date = pending.date,
                        time = pending.time,
                        timestamp = pending.timestamp,
                        uniqueHash = pending.smsHash,
                        defaultDescription = pending.description,
                        suggestedCategoryId = catId
                    )
                )
            }
        }

        // 2. Scan telephony SMS inbox for new messages
        val contentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                "${Telephony.Sms.Inbox.DATE} >= ? AND ${Telephony.Sms.Inbox.DATE} <= ?",
                arrayOf(fromTimestamp.toString(), toTimestamp.toString()),
                "${Telephony.Sms.Inbox.DATE} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms.Inbox._ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    val smsId = if (idIdx != -1) it.getString(idIdx) ?: "" else ""
                    val sender = if (addressIdx != -1) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val dateMillis = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    val parseResult = com.example.domain.sms.BankSmsParser.parse(sender, body, dateMillis)
                    if (parseResult is BankSmsParseResult.Success) {
                        val tx = parseResult.transaction
                        if (!seenHashes.contains(tx.uniqueHash)) {
                            seenHashes.add(tx.uniqueHash)

                            // Check if already in processed_sms
                            val existing = processedDao.getProcessedByHashOrRef(tx.uniqueHash, smsId, tx.referenceId)
                            if (existing != null) {
                                // Already recorded
                                if (existing.status == "PENDING_REVIEW" && tx.type == TransactionType.DEBIT && existing.associatedId > 0L) {
                                    val catId = BankSmsProcessor.matchCategoryForTransaction(tx, categories)
                                    detectedList.add(
                                        DetectedSmsItem(
                                            transactionId = existing.associatedId,
                                            smsId = smsId,
                                            sender = sender,
                                            body = body,
                                            amount = tx.amount,
                                            type = tx.type,
                                            bankName = tx.bankName,
                                            accountInfo = tx.accountInfo,
                                            paymentMethod = tx.paymentMethod,
                                            payeeOrMerchant = tx.payeeOrMerchant,
                                            referenceId = tx.referenceId,
                                            date = tx.date,
                                            time = tx.time,
                                            timestamp = dateMillis,
                                            uniqueHash = tx.uniqueHash,
                                            defaultDescription = existing.description.ifEmpty { BankSmsProcessor.suggestDefaultDescription(tx) },
                                            suggestedCategoryId = catId,
                                            isCreditCard = tx.isCreditCard,
                                            cardMaskedNumber = tx.cardMaskedNumber,
                                            cardName = tx.cardName
                                        )
                                    )
                                }
                                // If CONFIRMED, SKIPPED, or DELETED, do nothing (skip duplicate)
                            } else {
                                // Auto-register card if masked card number is detected
                                if (tx.cardMaskedNumber.isNotBlank()) {
                                    val existingCard = db.creditCardDao().getCardByNumber(tx.cardMaskedNumber)
                                    if (existingCard == null) {
                                        val newCard = com.example.data.entity.CreditCardEntity(
                                            cardName = tx.cardName.ifBlank { "${tx.bankName} Credit Card ${tx.cardMaskedNumber}" },
                                            bankName = tx.bankName,
                                            cardNumberMasked = tx.cardMaskedNumber,
                                            creditLimit = 0.0
                                        )
                                        db.creditCardDao().insertCard(newCard)
                                    }
                                }

                                // Brand new SMS detected! Create ONE transaction record immediately
                                if (tx.type == TransactionType.DEBIT) {
                                    // Check uniqueness at expense level too
                                    val existingExp = db.expenseDao().getExpenseBySmsHash(tx.uniqueHash)
                                    if (existingExp != null) {
                                        continue
                                    }

                                    val catId = BankSmsProcessor.matchCategoryForTransaction(tx, categories)
                                    val desc = BankSmsProcessor.suggestDefaultDescription(tx)
                                    val note = BankSmsProcessor.buildNote(tx)
                                    val expenseDate = if (tx.date.isNotBlank() && tx.date >= "2026-01-01") tx.date else DateUtils.today()
                                    val expenseTime = if (tx.time.isNotBlank()) tx.time else DateUtils.nowTime()

                                    // Create single expense record in database
                                    val expense = ExpenseEntity(
                                        amount = tx.amount,
                                        categoryId = catId,
                                        date = expenseDate,
                                        time = expenseTime,
                                        paymentMethod = tx.paymentMethod,
                                        note = note,
                                        isCreditCard = tx.isCreditCard,
                                        cardName = tx.cardName,
                                        cardMaskedNumber = tx.cardMaskedNumber,
                                        isBillPayment = tx.isCreditCardPayment,
                                        smsHash = tx.uniqueHash
                                    )
                                    val expenseId = db.expenseDao().insertExpense(expense)

                                    // Save in processed_sms with status PENDING_REVIEW
                                    val newProcessed = ProcessedSmsEntity(
                                        smsHash = tx.uniqueHash,
                                        sender = sender,
                                        body = body,
                                        amount = tx.amount,
                                        transactionType = "DEBIT",
                                        bankName = tx.bankName,
                                        accountInfo = tx.accountInfo,
                                        paymentMethod = tx.paymentMethod,
                                        date = expenseDate,
                                        time = expenseTime,
                                        referenceId = tx.referenceId,
                                        associatedId = expenseId,
                                        smsId = smsId,
                                        description = desc,
                                        status = "PENDING_REVIEW",
                                        timestamp = dateMillis
                                    )
                                    processedDao.insert(newProcessed)

                                    // Add to detectedList so user can review category for this transaction
                                    detectedList.add(
                                        DetectedSmsItem(
                                            transactionId = expenseId,
                                            smsId = smsId,
                                            sender = sender,
                                            body = body,
                                            amount = tx.amount,
                                            type = tx.type,
                                            bankName = tx.bankName,
                                            accountInfo = tx.accountInfo,
                                            paymentMethod = tx.paymentMethod,
                                            payeeOrMerchant = tx.payeeOrMerchant,
                                            referenceId = tx.referenceId,
                                            date = expenseDate,
                                            time = expenseTime,
                                            timestamp = dateMillis,
                                            uniqueHash = tx.uniqueHash,
                                            defaultDescription = desc,
                                            suggestedCategoryId = catId,
                                            isCreditCard = tx.isCreditCard,
                                            cardMaskedNumber = tx.cardMaskedNumber,
                                            cardName = tx.cardName
                                        )
                                    )
                                } else {
                                    // Bank Credit: check duplicate at income level
                                    val existingInc = db.incomeDao().getIncomeBySmsHash(tx.uniqueHash)
                                    val countInc = db.incomeDao().countBySmsIdentity(tx.uniqueHash, tx.referenceId, smsId)
                                    if (existingInc != null || countInc > 0) {
                                        continue
                                    }

                                    // Bank Credit: add directly as Income without popup (as per requirements)
                                    val incomeDate = if (tx.date.isNotBlank() && tx.date >= "2026-01-01") tx.date else DateUtils.today()
                                    val incomeNote = (if (tx.isCreditCardRefund) "Credit Card Refund: " else "Bank SMS Credit: ") +
                                        "${tx.bankName} (${tx.accountInfo})" +
                                        if (tx.referenceId.isNotBlank()) " [Ref: ${tx.referenceId}]" else ""
                                    val income = IncomeEntity(
                                        amount = tx.amount,
                                        incomeType = if (tx.isCreditCardRefund) "Credit Card Refund" else "Bank Deposit",
                                        date = incomeDate,
                                        note = incomeNote,
                                        isCreditCardRefund = tx.isCreditCardRefund,
                                        cardMaskedNumber = tx.cardMaskedNumber,
                                        smsHash = tx.uniqueHash,
                                        smsId = smsId,
                                        referenceId = tx.referenceId
                                    )
                                    val incomeId = db.incomeDao().insertIncome(income)

                                    val newProcessed = ProcessedSmsEntity(
                                        smsHash = tx.uniqueHash,
                                        sender = sender,
                                        body = body,
                                        amount = tx.amount,
                                        transactionType = "CREDIT",
                                        bankName = tx.bankName,
                                        accountInfo = tx.accountInfo,
                                        paymentMethod = tx.paymentMethod,
                                        date = incomeDate,
                                        time = tx.time,
                                        referenceId = tx.referenceId,
                                        associatedId = incomeId,
                                        smsId = smsId,
                                        description = incomeNote,
                                        status = "CONFIRMED",
                                        timestamp = dateMillis
                                    )
                                    processedDao.insert(newProcessed)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        detectedList
    }

    suspend fun scanAndImportBankSms(
        context: Context,
        maxMessagesToScan: Int = 100
    ): SmsScanSummary = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var debitsAdded = 0
        var creditsAdded = 0
        var duplicatesSkipped = 0
        var ignored = 0
        var totalDebitAmount = 0.0
        var totalCreditAmount = 0.0

        val contentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )

            cursor?.use {
                val addressIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext() && totalScanned < maxMessagesToScan) {
                    val sender = if (addressIdx != -1) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val dateMillis = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    totalScanned++

                    val result = BankSmsProcessor.processIncomingSms(
                        context = context,
                        sender = sender,
                        body = body,
                        timestamp = dateMillis,
                        forceProcess = true
                    )

                    when (result) {
                        is BankSmsProcessResult.AddedAsExpense -> {
                            debitsAdded++
                            totalDebitAmount += result.amount
                        }
                        is BankSmsProcessResult.AddedAsIncome -> {
                            creditsAdded++
                            totalCreditAmount += result.amount
                        }
                        is BankSmsProcessResult.Duplicate -> {
                            duplicatesSkipped++
                        }
                        is BankSmsProcessResult.Ignored -> {
                            ignored++
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SmsScanSummary(
            totalScanned = totalScanned,
            debitsAdded = debitsAdded,
            creditsAdded = creditsAdded,
            duplicatesSkipped = duplicatesSkipped,
            nonTransactionsIgnored = ignored,
            totalDebitAmount = totalDebitAmount,
            totalCreditAmount = totalCreditAmount
        )
    }
}
