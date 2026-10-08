package com.example.domain.sms

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object BankSmsParser {

    // Regex to remove balance clauses to avoid confusing available balance with transaction amount
    private val BALANCE_CLAUSE_REGEX = Pattern.compile(
        """(?i)(?:avl(?:[\.\s]+bal(?:ance)?)?|available\s+bal(?:ance)?|clear\s+bal(?:ance)?|total\s+bal(?:ance)?|account\s+bal(?:ance)?|bal(?:ance)?\s*[:=])\s*(?:is)?\s*(?:inr|rs\.?|₹)?\s*[\d,]+(?:\.\d{1,2})?"""
    )

    // Regex for OTP / Verification
    private val OTP_PATTERNS = listOf(
        """(?i)\b(?:otp|one\s*time\s*password|verification\s*code|security\s*code|secret\s*code|login\s*code)\b""",
        """(?i)\b(?:do\s*not\s*share|never\s*share|valid\s*for\s*\d+\s*(?:mins?|minutes?))\b"""
    )

    // Regex for LOANS, OFFERS, PROMOTIONS, PRE-APPROVED, ELIGIBILITY, EMI OFFERS
    private val PROMOTIONAL_LOAN_PATTERNS = listOf(
        """(?i)\b(?:pre[\s-]?approved|eligible\s*for|eligibility|check\s*eligibility)\b""",
        """(?i)\b(?:loan\s*offer|personal\s*loan|home\s*loan|car\s*loan|instant\s*loan|business\s*loan|gold\s*loan)\b""",
        """(?i)\b(?:avail\s*loan|apply\s*now|claim\s*now|click\s*here\s*to\s*apply|disbursal\s*offer|loan\s*disbursement\s*ad)\b""",
        """(?i)\b(?:credit\s*card\s*offer|credit\s*offer|credit\s*limit\s*(?:offer|increase|enhancement))\b""",
        """(?i)\b(?:increase\s*your\s*limit|limit\s*enhancement|upgrade\s*your\s*card)\b""",
        """(?i)\b(?:congratulations!?\s*you\s*are\s*eligible|you\s*are\s*eligible\s*for)\b""",
        """(?i)\b(?:special\s*offer|exclusive\s*offer|discount\s*offer|promo\s*code|reward\s*points)\b""",
        """(?i)\b(?:win\s*up\s*to|stand\s*a\s*chance|cashback\s*offer|earn\s*up\s*to)\b""",
        """(?i)\b(?:emi\s*offer|convert\s*to\s*emi|avail\s*emi|easy\s*emi\s*option)\b"""
    )

    // Regex for Reminders, Bills, Mandates without transaction
    private val REMINDER_PATTERNS = listOf(
        """(?i)\b(?:payment\s*reminder|bill\s*due|due\s*date\s*is|minimum\s*amount\s*due|reminder\s*to\s*pay)\b""",
        """(?i)\b(?:please\s*pay\s*before|payment\s*is\s*pending|bill\s*generated)\b""",
        """(?i)\b(?:mandate\s*created|autopay\s*registered|e-mandate\s*active)\b"""
    )

    // Regex for Failed, Declined, Incomplete, Cancelled transactions
    private val FAILED_TRANSACTION_PATTERNS = listOf(
        """(?i)\b(?:failed|declined|unsuccessful|cancelled|rejected|declined\s*due\s*to)\b""",
        """(?i)\b(?:insufficient\s*funds?|insufficient\s*balance|could\s*not\s*be\s*processed)\b""",
        """(?i)\b(?:request\s*to\s*pay|payment\s*request\s*received|has\s*requested\s*money)\b"""
    )

    // Regex for Pure Balance Inquiry (no debit / credit happened)
    private val PURE_BALANCE_INQUIRY = Pattern.compile(
        """(?i)^(?:.*)(?:avl|available|clear)?\s*bal(?:ance)?\s*(?:in|for)?\s*(?:a/c|account).*(?:is|:)\s*(?:rs\.?|inr|₹)?\s*[\d,]+(?:\.\d{1,2})?(?:[^\w]*)$"""
    )

    // Transaction verbs
    private val DEBIT_VERB_REGEX = Pattern.compile(
        """(?i)\b(?:debited|debit|debited\s*by|debited\s*with|debited\s*for|dr\s*to|spent|paid|withdrawn|withdrew|transferred\s*to|sent\s*to|purchase\s*at|purchase\s*of|charged\s*to)\b"""
    )

    private val CREDIT_VERB_REGEX = Pattern.compile(
        """(?i)\b(?:credited|credit|credited\s*with|credited\s*by|credited\s*to|cr\s*to|deposited|received|refunded|refund\s*of|transferred\s*into|added\s*to)\b"""
    )

    // Currency Amount Patterns
    private val AMOUNT_PATTERNS = listOf(
        // Pattern 1: Rs. 500 / INR 500 / ₹ 500 / Rs500 / ₹1,250.50
        Pattern.compile("""(?i)(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)"""),
        // Pattern 2: 500 Rs / 500.00 INR
        Pattern.compile("""(?i)([\d,]+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|₹)"""),
        // Pattern 3: debited by/with/for 500.00
        Pattern.compile("""(?i)(?:debited|credited|withdrawn|deposited|paid|spent)\s+(?:by|with|for|of)?\s*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    // Known Indian banks and institutions
    private val KNOWN_BANKS = listOf(
        "HDFC" to listOf("HDFC", "HDFCBK", "HDFC BANK"),
        "SBI" to listOf("SBI", "SBIINB", "STATE BANK", "SBIPSG"),
        "ICICI" to listOf("ICICI", "ICICIB", "ICICI BANK"),
        "Axis" to listOf("AXIS", "AXISBK", "AXIS BANK", "UTIBNK"),
        "Kotak" to listOf("KOTAK", "KOTAKB", "KOTAK MAHINDRA"),
        "Punjab National Bank" to listOf("PNB", "PNBSMS", "PUNJAB NATIONAL"),
        "Bank of Baroda" to listOf("BOB", "BOBTXN", "BARODA"),
        "Canara Bank" to listOf("CANBNK", "CANARA"),
        "IndusInd" to listOf("INDUS", "INDUSB", "INDUSIND"),
        "Federal Bank" to listOf("FEDBNK", "FEDERAL"),
        "Union Bank" to listOf("UNION", "UNIONB", "UBIN"),
        "IDFC FIRST" to listOf("IDFC", "IDFCFB", "IDFC FIRST"),
        "Yes Bank" to listOf("YESBNK", "YES BANK"),
        "Paytm Payments Bank" to listOf("PAYTM", "PYTM"),
        "Google Pay" to listOf("GPAY", "GOOGLEPAY"),
        "PhonePe" to listOf("PHONEPE", "PHNPE"),
        "Cred" to listOf("CRED"),
        "Jupiter" to listOf("JUPITR", "JUPITER"),
        "Fi Money" to listOf("FI_BNK", "FIMONEY")
    )

    /**
     * Parses an incoming or inbox SMS message.
     * Returns BankSmsParseResult.Success if high confidence genuine bank transaction.
     * Returns BankSmsParseResult.Ignored with human-readable reason otherwise.
     */
    fun parse(
        sender: String,
        body: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): BankSmsParseResult {
        val cleanBody = body.trim()
        val cleanSender = sender.trim().uppercase(Locale.ENGLISH)

        if (cleanBody.isBlank()) {
            return BankSmsParseResult.Ignored("Empty message content")
        }

        // STEP 1: Check if OTP or verification code
        for (pattern in OTP_PATTERNS) {
            if (Regex(pattern).containsMatchIn(cleanBody)) {
                return BankSmsParseResult.Ignored("Ignored: OTP or security verification message")
            }
        }

        // STEP 2: Strict check for Loan, Credit limit, and Promotional offers
        for (pattern in PROMOTIONAL_LOAN_PATTERNS) {
            if (Regex(pattern).containsMatchIn(cleanBody)) {
                return BankSmsParseResult.Ignored("Ignored: Loan offer, credit eligibility, or promotional advertisement")
            }
        }

        // STEP 3: Check for payment reminders, bill dues, or pending requests
        for (pattern in REMINDER_PATTERNS) {
            if (Regex(pattern).containsMatchIn(cleanBody)) {
                return BankSmsParseResult.Ignored("Ignored: Payment reminder or bill due notification")
            }
        }

        // STEP 4: Check for failed, declined, or cancelled transactions
        for (pattern in FAILED_TRANSACTION_PATTERNS) {
            if (Regex(pattern).containsMatchIn(cleanBody)) {
                return BankSmsParseResult.Ignored("Ignored: Failed, declined, or pending transaction")
            }
        }

        // STEP 5: Check if pure balance inquiry without transaction
        if (isPureBalanceInquiry(cleanBody)) {
            return BankSmsParseResult.Ignored("Ignored: Account balance inquiry without a transaction")
        }

        // STEP 6: Determine Transaction Type (DEBIT vs CREDIT)
        val transactionType = determineTransactionType(cleanBody)
            ?: return BankSmsParseResult.Ignored("Ignored: No clear completed debit or credit action found")

        // STEP 7: Extract Amount
        val amount = extractAmount(cleanBody)
            ?: return BankSmsParseResult.Ignored("Ignored: Could not identify valid transaction amount")

        if (amount <= 0.0) {
            return BankSmsParseResult.Ignored("Ignored: Transaction amount is zero or invalid")
        }

        // STEP 8: Extract Bank / Financial Institution
        val bankName = detectBank(cleanSender, cleanBody)

        // STEP 9: Extract Account / Card Information
        val accountInfo = extractAccountInfo(cleanBody)

        // STEP 10: Extract Payment Method
        val paymentMethod = detectPaymentMethod(cleanBody)

        // STEP 11: Extract Payee or Merchant
        val payee = extractPayee(cleanBody, transactionType)

        // STEP 12: Extract Reference / UTR / UPI Ref ID
        val referenceId = extractReferenceId(cleanBody)

        // STEP 13: Extract Date & Time
        val (transDate, transTime) = extractDateTime(cleanBody, timestampMillis)

        // Generate deterministic unique hash to prevent duplicate processing
        val uniqueHash = generateTransactionHash(
            cleanSender,
            amount,
            transactionType,
            accountInfo,
            transDate,
            transTime,
            timestampMillis,
            referenceId,
            cleanBody
        )

        val parsedTransaction = ParsedBankTransaction(
            amount = amount,
            type = transactionType,
            bankName = bankName,
            accountInfo = accountInfo,
            paymentMethod = paymentMethod,
            payeeOrMerchant = payee,
            referenceId = referenceId,
            date = transDate,
            time = transTime,
            rawSender = sender,
            rawBody = cleanBody,
            uniqueHash = uniqueHash
        )

        return BankSmsParseResult.Success(parsedTransaction)
    }

    private fun isPureBalanceInquiry(body: String): Boolean {
        val hasDebit = DEBIT_VERB_REGEX.matcher(body).find()
        val hasCredit = CREDIT_VERB_REGEX.matcher(body).find()
        if (!hasDebit && !hasCredit) {
            return PURE_BALANCE_INQUIRY.matcher(body).find() ||
                    body.contains("balance", ignoreCase = true) ||
                    body.contains("bal", ignoreCase = true)
        }
        return false
    }

    private fun determineTransactionType(body: String): TransactionType? {
        val hasDebit = DEBIT_VERB_REGEX.matcher(body).find()
        val hasCredit = CREDIT_VERB_REGEX.matcher(body).find()

        return when {
            hasDebit && !hasCredit -> TransactionType.DEBIT
            hasCredit && !hasDebit -> TransactionType.CREDIT
            hasDebit && hasCredit -> {
                // If both present, check which verb comes first or is adjacent to the amount
                val debitMatcher = DEBIT_VERB_REGEX.matcher(body)
                val creditMatcher = CREDIT_VERB_REGEX.matcher(body)
                val debitIndex = if (debitMatcher.find()) debitMatcher.start() else Int.MAX_VALUE
                val creditIndex = if (creditMatcher.find()) creditMatcher.start() else Int.MAX_VALUE
                if (debitIndex < creditIndex) TransactionType.DEBIT else TransactionType.CREDIT
            }
            else -> null
        }
    }

    /**
     * Extracts the transaction amount while safely stripping out available balance amounts.
     */
    private fun extractAmount(body: String): Double? {
        // Strip out balance clause first so its amount is not grabbed
        val bodyWithoutBalance = BALANCE_CLAUSE_REGEX.matcher(body).replaceAll("")

        // Try patterns on cleaned body first
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(bodyWithoutBalance)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = amountStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        // Fallback: If not found in bodyWithoutBalance, try on full body before balance keywords
        val balanceKeywordIndex = findFirstBalanceKeywordIndex(body)
        val textBeforeBalance = if (balanceKeywordIndex > 0) body.substring(0, balanceKeywordIndex) else body

        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(textBeforeBalance)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = amountStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        return null
    }

    private fun findFirstBalanceKeywordIndex(body: String): Int {
        val keywords = listOf("avl bal", "available bal", "bal is", "clear bal", "total bal", "bal:")
        val lower = body.lowercase(Locale.ENGLISH)
        var minIndex = -1
        for (kw in keywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1 && (minIndex == -1 || idx < minIndex)) {
                minIndex = idx
            }
        }
        return minIndex
    }

    private fun detectBank(sender: String, body: String): String {
        for ((bankName, aliases) in KNOWN_BANKS) {
            for (alias in aliases) {
                if (sender.contains(alias, ignoreCase = true) || body.contains(alias, ignoreCase = true)) {
                    return bankName
                }
            }
        }
        return if (sender.isNotBlank()) sender.takeLast(6) else "Bank"
    }

    private fun extractAccountInfo(body: String): String {
        val patterns = listOf(
            Pattern.compile("""(?i)(?:a/c|acct|account|card|c/d|ending\s+with|ending\s+in|xx)\s*(?:no\.?|#)?\s*[:\s]*([xX\*\d]{2,16}\d{3,4}|\d{4})"""),
            Pattern.compile("""(?i)\b([xX\*\d]{2,16}\d{4})\b""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val acc = matcher.group(1)?.trim()
                if (!acc.isNullOrBlank() && acc.any { it.isDigit() }) {
                    return if (acc.startsWith("XX", ignoreCase = true) || acc.contains("*")) {
                        acc
                    } else {
                        "XX" + acc.takeLast(4)
                    }
                }
            }
        }
        return "Bank Account"
    }

    private fun detectPaymentMethod(body: String): String {
        val lower = body.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") ||
                    lower.contains("phonepe") || lower.contains("paytm") || lower.contains("bhim") -> "UPI"
            lower.contains("credit card") || lower.contains("cc ending") -> "Credit Card"
            lower.contains("debit card") || lower.contains("pos") || lower.contains("swipe") -> "Debit Card"
            lower.contains("atm") || lower.contains("cash withdrawal") || lower.contains("cash wdl") -> "ATM"
            lower.contains("imps") -> "IMPS"
            lower.contains("neft") -> "NEFT"
            lower.contains("rtgs") -> "RTGS"
            lower.contains("net banking") || lower.contains("netbanking") || lower.contains("bank transfer") -> "Net Banking"
            else -> "Bank Transfer"
        }
    }

    private fun extractPayee(body: String, type: TransactionType): String {
        // Look for "to <merchant/vpa>", "at <merchant>", "vpa <id>", "info <text>"
        val patterns = listOf(
            Pattern.compile("""(?i)(?:to|at|info|towards|vpa)\s+([A-Za-z0-9@\.\-_]{3,25})"""),
            Pattern.compile("""(?i)(?:from)\s+([A-Za-z0-9@\.\-_]{3,25})""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && !found.equals("account", ignoreCase = true) && !found.equals("your", ignoreCase = true)) {
                    return found
                }
            }
        }
        return if (type == TransactionType.DEBIT) "Merchant / Payee" else "Sender / Source"
    }

    private fun extractReferenceId(body: String): String {
        val patterns = listOf(
            Pattern.compile("""(?i)(?:upi\s*ref(?:erence)?\s*(?:no\.?|#)?|ref(?:erence)?\s*(?:no\.?|#)?|rrn|utr|txn\s*(?:id|no\.?|#)?)\s*[:\s]*([A-Za-z0-9]{6,20})"""),
            Pattern.compile("""(?i)\b(?:ref|utr|rrn)\s*([0-9]{8,14})\b""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim()
                if (!ref.isNullOrBlank()) {
                    return ref
                }
            }
        }
        return ""
    }

    private fun extractDateTime(body: String, timestampMillis: Long): Pair<String, String> {
        // Default to timestamp
        val instant = Instant.ofEpochMilli(timestampMillis)
        val zone = ZoneId.systemDefault()
        val defaultDate = instant.atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val defaultTime = instant.atZone(zone).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

        // Check if date appears in body like 06-10-24 or 06/10/2024 or 06-Oct-24
        val datePattern = Pattern.compile("""(?i)\b(\d{1,2})[-/](\d{1,2}|[A-Za-z]{3})[-/](\d{2,4})\b""")
        val matcher = datePattern.matcher(body)
        if (matcher.find()) {
            val d = matcher.group(1)?.padStart(2, '0')
            val m = matcher.group(2)
            val y = matcher.group(3)
            val yearFormatted = if (y?.length == 2) "20$y" else y

            // Format month
            val monthFormatted = when (m?.lowercase(Locale.ENGLISH)) {
                "jan" -> "01"; "feb" -> "02"; "mar" -> "03"; "apr" -> "04"
                "may" -> "05"; "jun" -> "06"; "jul" -> "07"; "aug" -> "08"
                "sep" -> "09"; "oct" -> "10"; "nov" -> "11"; "dec" -> "12"
                else -> m?.padStart(2, '0')
            }

            if (d != null && monthFormatted != null && yearFormatted != null) {
                val currentYear = java.time.LocalDate.now().year
                val parsedYearInt = yearFormatted.toIntOrNull() ?: currentYear
                // If parsed year is from the past (e.g. sample SMS with 2024), adapt to current year
                val finalYear = if (parsedYearInt < currentYear) currentYear.toString() else yearFormatted
                val parsedDate = "$finalYear-$monthFormatted-$d"
                return Pair(parsedDate, defaultTime)
            }
        }

        return Pair(defaultDate, defaultTime)
    }

    private fun generateTransactionHash(
        sender: String,
        amount: Double,
        type: TransactionType,
        accountInfo: String,
        date: String,
        time: String,
        timestampMillis: Long,
        referenceId: String,
        body: String
    ): String {
        // If reference ID exists, it's globally unique!
        val rawKey = if (referenceId.isNotBlank()) {
            "REF_${type.name}_${referenceId.lowercase(Locale.ENGLISH)}"
        } else {
            // Fingerprint from sender, amount, type, account, date, time, and core text
            // Including time ensures two separate transactions of the same amount are treated as distinct
            val normalizedBody = body.lowercase(Locale.ENGLISH).replace(Regex("""\s+"""), " ")
            "FP_${sender}_${type.name}_${amount}_${accountInfo}_${date}_${time}_${timestampMillis}_${normalizedBody.hashCode()}"
        }

        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(rawKey.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
