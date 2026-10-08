package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.database.AppDatabase
import com.example.data.entity.CategoryEntity
import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsProcessResult
import com.example.domain.sms.BankSmsProcessor
import com.example.domain.sms.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedSmsItem(
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
    val suggestedCategoryId: Long
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
     * Returns newly detected, unprocessed bank transactions so user can review and provide description.
     */
    suspend fun scanUnprocessedBankSmsBetween(
        context: Context,
        fromTimestamp: Long,
        toTimestamp: Long,
        categories: List<CategoryEntity> = emptyList()
    ): List<DetectedSmsItem> = withContext(Dispatchers.IO) {
        val detectedList = mutableListOf<DetectedSmsItem>()
        val seenHashes = mutableSetOf<String>()

        val contentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val processedDao = db.processedSmsDao()

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
                            // Check if already saved in database (by hash, SMS ID, or reference ID)
                            val alreadyInDb = processedDao.isSmsProcessed(tx.uniqueHash, smsId, tx.referenceId)
                            if (alreadyInDb == 0) {
                                seenHashes.add(tx.uniqueHash)
                                val desc = BankSmsProcessor.suggestDefaultDescription(tx)
                                val catId = BankSmsProcessor.matchCategoryForTransaction(tx, categories)
                                detectedList.add(
                                    DetectedSmsItem(
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
                                        defaultDescription = desc,
                                        suggestedCategoryId = catId
                                    )
                                )
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
