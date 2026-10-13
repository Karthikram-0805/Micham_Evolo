package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProcessedSmsEntity
import com.example.data.entity.RecurringExpenseEntity
import com.example.data.entity.SalaryEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.ExpenseRepository
import com.example.domain.engine.DialogueEngine
import com.example.domain.model.CategoryWithSpend
import com.example.domain.model.DailySpendItem
import com.example.domain.model.ExpenseFilter
import com.example.domain.model.ExpenseWithCategory
import com.example.domain.model.FinancialSummary
import com.example.domain.model.MonthHistoryItem
import com.example.domain.sms.BankSmsParseResult
import com.example.domain.sms.BankSmsParser
import com.example.domain.sms.BankSmsProcessResult
import com.example.domain.sms.BankSmsProcessor
import com.example.domain.sms.TransactionType
import com.example.utils.BackupPayload
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import com.example.utils.DetectedSmsItem
import com.example.utils.ExportImportHelper
import com.example.utils.SmsInboxHelper
import com.example.utils.SmsScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneId

import com.example.data.entity.CreditCardEntity
import com.example.domain.model.CardSpendingDetail
import com.example.domain.model.CreditCardSummary

import com.example.data.entity.BankEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ExpenseRepository(
        expenseDao = database.expenseDao(),
        salaryDao = database.salaryDao(),
        categoryDao = database.categoryDao(),
        incomeDao = database.incomeDao(),
        savingsGoalDao = database.savingsGoalDao(),
        recurringExpenseDao = database.recurringExpenseDao(),
        creditCardDao = database.creditCardDao(),
        bankDao = database.bankDao()
    )
    val preferencesRepository = UserPreferencesRepository(application)

    // Banks for SMS Tracking
    val allBanks: StateFlow<List<BankEntity>> = repository.allBanks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Credit Cards
    val allCreditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // User settings flow
    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserSettings()
    )

    // Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // All expenses
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // All incomes
    val allIncomes: StateFlow<List<IncomeEntity>> = repository.allIncomes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // All salaries
    val allSalaries: StateFlow<List<SalaryEntity>> = repository.getAllSalaries().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Recurring expenses
    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurringExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Processed Bank SMS list
    val processedSmsList: StateFlow<List<ProcessedSmsEntity>> = database.processedSmsDao().getAllProcessedSms().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Pending newly detected bank transactions awaiting user description
    private val _pendingSmsTransactions = MutableStateFlow<List<DetectedSmsItem>>(emptyList())
    val pendingSmsTransactions: StateFlow<List<DetectedSmsItem>> = _pendingSmsTransactions.asStateFlow()

    // Current Date Cycle Range
    private val _selectedCalendarMonth = MutableStateFlow(LocalDate.now())
    val selectedCalendarMonth = _selectedCalendarMonth.asStateFlow()

    // Reaction popup state
    private val _lastReactionMessage = MutableStateFlow<String?>(null)
    val lastReactionMessage = _lastReactionMessage.asStateFlow()

    // App Lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked = _isAppLocked.asStateFlow()

    // Filter state
    val expenseFilter = MutableStateFlow(ExpenseFilter())

    // Financial Summary
    val financialSummary: StateFlow<FinancialSummary> = combine(
        userSettings,
        allExpenses,
        allIncomes,
        allSalaries
    ) { settings, expenses, incomes, salaries ->
        val cycle = DateUtils.calculateCycle(settings.salaryCycleType, settings.salaryCycleDay)

        // Find salary for this cycle month/year, fallback to defaultSalary
        val salaryRecord = salaries.find { it.month == cycle.cycleMonth && it.year == cycle.cycleYear }
        val salaryAmount = BigDecimal(salaryRecord?.salaryAmount ?: settings.defaultSalary).setScale(2, RoundingMode.HALF_UP).toDouble()

        // Incomes in this cycle (Bank credits + manual incomes) - exclude credit card refunds from main wallet
        val cycleIncomes = incomes.filter { it.date >= cycle.startDate && it.date <= cycle.endDate && !it.isCreditCardRefund }
        val totalAdditionalIncome = BigDecimal(cycleIncomes.sumOf { it.amount }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalAvailable = BigDecimal(salaryAmount + totalAdditionalIncome).setScale(2, RoundingMode.HALF_UP).toDouble()

        // Expenses in this cycle (Bank debits + UPI + cash + actual Credit Card bill payments)
        // EXCLUDE Credit Card purchases so they NEVER reduce the Main In-Hand Wallet Balance!
        val cycleExpenses = expenses.filter {
            it.date >= cycle.startDate && it.date <= cycle.endDate && (!it.isCreditCard || it.isBillPayment)
        }
        val totalExpenses = BigDecimal(cycleExpenses.sumOf { it.amount }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val remaining = BigDecimal(totalAvailable - totalExpenses).setScale(2, RoundingMode.HALF_UP).toDouble()

        val percentRemaining = if (totalAvailable > 0) {
            ((remaining / totalAvailable) * 100).toInt().coerceIn(0, 100)
        } else 0

        val percentSpent = if (totalAvailable > 0) {
            ((totalExpenses / totalAvailable) * 100).toInt().coerceIn(0, 100)
        } else 0

        val todayDate = DateUtils.today()
        val todaySpend = cycleExpenses.filter { it.date == todayDate }.sumOf { it.amount }

        val sevenDaysAgo = LocalDate.now().minusDays(6).toString()
        val weekSpend = cycleExpenses.filter { it.date >= sevenDaysAgo && it.date <= todayDate }.sumOf { it.amount }

        val elapsed = cycle.elapsedDays.coerceAtLeast(1)
        val dailyAvg = totalExpenses / elapsed

        val remainingDays = cycle.remainingDays.coerceAtLeast(1)
        val dailyBudget = if (remaining > 0) remaining / remainingDays else 0.0

        val projectedMonthEndSpend = dailyAvg * cycle.totalDays
        val projectedRemaining = totalAvailable - projectedMonthEndSpend
        val projectedSavingsPercent = if (totalAvailable > 0) {
            ((projectedRemaining / totalAvailable) * 100).toInt()
        } else 0

        FinancialSummary(
            salary = salaryAmount,
            additionalIncome = totalAdditionalIncome,
            totalExpenses = totalExpenses,
            remainingBalance = remaining,
            percentageRemaining = percentRemaining,
            percentageSpent = percentSpent,
            todaySpend = todaySpend,
            weekSpend = weekSpend,
            dailyAverage = dailyAvg,
            dailyBudget = dailyBudget,
            daysRemainingInCycle = remainingDays,
            projectedMonthEndSpend = projectedMonthEndSpend,
            projectedRemaining = projectedRemaining,
            projectedSavingsPercentage = projectedSavingsPercent,
            totalAvailable = totalAvailable
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialSummary()
    )

    // Dedicated Credit Card Summary & Analytics
    val creditCardSummary: StateFlow<CreditCardSummary> = combine(
        allCreditCards,
        allExpenses,
        allIncomes,
        financialSummary,
        userSettings
    ) { cards, expenses, incomes, finSummary, settings ->
        val cycle = DateUtils.calculateCycle(settings.salaryCycleType, settings.salaryCycleDay)

        // All credit card purchases in current cycle
        val cycleCcPurchases = expenses.filter {
            it.isCreditCard && !it.isBillPayment && it.date >= cycle.startDate && it.date <= cycle.endDate
        }
        val totalSpendingThisMonth = BigDecimal(cycleCcPurchases.sumOf { it.amount }).setScale(2, RoundingMode.HALF_UP).toDouble()

        // All credit card purchases all-time
        val allCcPurchases = expenses.filter { it.isCreditCard && !it.isBillPayment }
        // All payments made towards credit cards (bill payments)
        val allCcPayments = expenses.filter { it.isBillPayment }
        // All CC refunds
        val allCcRefunds = incomes.filter { it.isCreditCardRefund }

        val totalPurchasesAmount = allCcPurchases.sumOf { it.amount }
        val totalPaymentsAmount = allCcPayments.sumOf { it.amount }
        val totalRefundsAmount = allCcRefunds.sumOf { it.amount }
        val totalOpeningOutstanding = cards.sumOf { it.openingOutstanding }

        val totalOutstanding = BigDecimal(
            (totalOpeningOutstanding + totalPurchasesAmount - totalPaymentsAmount - totalRefundsAmount).coerceAtLeast(0.0)
        ).setScale(2, RoundingMode.HALF_UP).toDouble()

        val paymentsThisMonth = allCcPayments.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }.sumOf { it.amount }

        // Card details list
        val registeredMasked = cards.map { it.cardNumberMasked }.toSet()
        val extraCards = allCcPurchases.map { it.cardMaskedNumber to it.cardName }
            .distinctBy { it.first }
            .filter { it.first.isNotBlank() && !registeredMasked.contains(it.first) }
            .map { (masked, name) ->
                CreditCardEntity(
                    id = 0,
                    cardName = name.ifBlank { "Credit Card $masked" },
                    bankName = name.substringBefore(" ").ifBlank { "Card" },
                    cardNumberMasked = masked,
                    creditLimit = 0.0
                )
            }

        val allCombinedCards = cards + extraCards

        val cardDetails = allCombinedCards.map { card ->
            val cardCyclePurchases = cycleCcPurchases.filter {
                it.cardMaskedNumber == card.cardNumberMasked || (card.cardNumberMasked.isBlank() && it.cardName == card.cardName)
            }.sumOf { it.amount }

            val cardAllPurchases = allCcPurchases.filter {
                it.cardMaskedNumber == card.cardNumberMasked || (card.cardNumberMasked.isBlank() && it.cardName == card.cardName)
            }.sumOf { it.amount }

            val cardPayments = allCcPayments.filter {
                it.cardMaskedNumber == card.cardNumberMasked
            }.sumOf { it.amount }

            val cardRefunds = allCcRefunds.filter {
                it.cardMaskedNumber == card.cardNumberMasked
            }.sumOf { it.amount }

            val cardOutstanding = BigDecimal(
                (card.openingOutstanding + cardAllPurchases - cardPayments - cardRefunds).coerceAtLeast(0.0)
            ).setScale(2, RoundingMode.HALF_UP).toDouble()

            val availLimit = if (card.creditLimit > 0) {
                BigDecimal((card.creditLimit - cardOutstanding).coerceAtLeast(0.0)).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else 0.0

            val utilPercent = if (card.creditLimit > 0) {
                ((cardOutstanding / card.creditLimit) * 100).toInt().coerceIn(0, 100)
            } else 0

            CardSpendingDetail(
                card = card,
                totalSpentThisMonth = cardCyclePurchases,
                currentOutstanding = cardOutstanding,
                paymentsMadeThisMonth = allCcPayments.filter {
                    it.cardMaskedNumber == card.cardNumberMasked && it.date >= cycle.startDate && it.date <= cycle.endDate
                }.sumOf { it.amount },
                availableLimit = availLimit,
                utilizationPercentage = utilPercent
            )
        }

        val totalAvailableLimit = cardDetails.sumOf { it.availableLimit }

        val cardsWithDues = cardDetails.filter { it.currentOutstanding > 0 }
        val upcomingPaymentAmount = if (cardsWithDues.isNotEmpty()) {
            val dueCards = cardsWithDues.filter { it.card.dueDay > 0 }
            if (dueCards.isNotEmpty()) dueCards.sumOf { it.currentOutstanding } else totalOutstanding
        } else 0.0

        val nearestDueCard = cardsWithDues.filter { it.card.dueDay > 0 }.minByOrNull { it.card.dueDay }
        val nearestDueDate = if (nearestDueCard != null) {
            "Due on ${nearestDueCard.card.dueDay}th"
        } else if (totalOutstanding > 0) {
            "Due this billing cycle"
        } else {
            "No dues pending"
        }

        val netAfterCardDues = BigDecimal(finSummary.remainingBalance - totalOutstanding).setScale(2, RoundingMode.HALF_UP).toDouble()

        // Monthly Daily Spends for graph
        val dailyMap = mutableMapOf<String, Double>()
        try {
            val startLd = LocalDate.parse(cycle.startDate)
            val endLd = LocalDate.parse(cycle.endDate)
            var cur = startLd
            while (!cur.isAfter(endLd)) {
                dailyMap[cur.toString()] = 0.0
                cur = cur.plusDays(1)
            }
        } catch (e: Exception) {
            // fallback
        }
        cycleCcPurchases.forEach {
            dailyMap[it.date] = (dailyMap[it.date] ?: 0.0) + it.amount
        }
        val dailySpends = dailyMap.map { (dt, amt) ->
            DailySpendItem(
                date = dt,
                totalAmount = amt,
                transactionCount = cycleCcPurchases.count { it.date == dt }
            )
        }.sortedBy { it.date }

        CreditCardSummary(
            totalSpendingThisMonth = totalSpendingThisMonth,
            totalOutstanding = totalOutstanding,
            totalPaymentsMade = paymentsThisMonth,
            totalAvailableLimit = totalAvailableLimit,
            upcomingPaymentAmount = upcomingPaymentAmount,
            nearestDueDate = nearestDueDate,
            netAfterCardDues = netAfterCardDues,
            cards = cardDetails,
            monthlyDailySpends = dailySpends,
            recentTransactions = (allCcPurchases + allCcPayments).sortedByDescending { it.date },
            recentRefunds = allCcRefunds.sortedByDescending { it.date }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CreditCardSummary()
    )

    // Categories with spend for current cycle
    val categorySpendList: StateFlow<List<CategoryWithSpend>> = combine(
        categories,
        allExpenses,
        userSettings
    ) { catList, expList, settings ->
        val cycle = DateUtils.calculateCycle(settings.salaryCycleType, settings.salaryCycleDay)
        val cycleExpenses = expList.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }
        val totalCycleSpend = cycleExpenses.sumOf { it.amount }

        catList.map { cat ->
            val catSpend = cycleExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            val percentOfTotal = if (totalCycleSpend > 0) (catSpend / totalCycleSpend).toFloat() else 0f
            val percentBudgetUsed = if (cat.monthlyBudget > 0) {
                ((catSpend / cat.monthlyBudget) * 100).toInt()
            } else 0
            CategoryWithSpend(
                category = cat,
                totalSpend = catSpend,
                percentageOfTotal = percentOfTotal,
                monthlyBudget = cat.monthlyBudget,
                percentBudgetUsed = percentBudgetUsed
            )
        }.sortedByDescending { it.totalSpend }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered expenses list
    val filteredExpenses: StateFlow<List<ExpenseWithCategory>> = combine(
        allExpenses,
        categories,
        expenseFilter
    ) { expenses, catList, filter ->
        val catMap = catList.associateBy { it.id }
        expenses.filter { exp ->
            val matchesCategory = filter.categoryId == null || exp.categoryId == filter.categoryId
            val matchesPayment = filter.paymentMethod == null || exp.paymentMethod.equals(filter.paymentMethod, ignoreCase = true)
            val matchesStart = filter.startDate == null || exp.date >= filter.startDate
            val matchesEnd = filter.endDate == null || exp.date <= filter.endDate
            val matchesMin = filter.minAmount == null || exp.amount >= filter.minAmount
            val matchesMax = filter.maxAmount == null || exp.amount <= filter.maxAmount
            val matchesQuery = filter.searchQuery.isBlank() ||
                    exp.note.contains(filter.searchQuery, ignoreCase = true) ||
                    (catMap[exp.categoryId]?.name?.contains(filter.searchQuery, ignoreCase = true) == true) ||
                    exp.amount.toString().contains(filter.searchQuery)

            matchesCategory && matchesPayment && matchesStart && matchesEnd && matchesMin && matchesMax && matchesQuery
        }.map { exp ->
            ExpenseWithCategory(expense = exp, category = catMap[exp.categoryId])
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Monthly History list
    val monthlyHistory: StateFlow<List<MonthHistoryItem>> = combine(
        allExpenses,
        allSalaries,
        allIncomes,
        userSettings
    ) { expenses, salaries, incomes, settings ->
        val currentYearMonth = YearMonth.now()
        val list = mutableListOf<MonthHistoryItem>()

        // Look at past 12 months
        for (i in 0..11) {
            val ym = currentYearMonth.minusMonths(i.toLong())
            val month = ym.monthValue
            val year = ym.year
            val prefix = String.format("%04d-%02d", year, month)

            val salaryRecord = salaries.find { it.month == month && it.year == year }
            val salAmount = salaryRecord?.salaryAmount ?: settings.defaultSalary

            val monthIncomes = incomes.filter { it.date.startsWith(prefix) }.sumOf { it.amount }
            val totalAvail = salAmount + monthIncomes

            val monthExpenses = expenses.filter { it.date.startsWith(prefix) }.sumOf { it.amount }
            val remaining = totalAvail - monthExpenses
            val savingsPercent = if (totalAvail > 0) {
                ((remaining / totalAvail) * 100).toInt()
            } else 0

            list.add(
                MonthHistoryItem(
                    month = month,
                    year = year,
                    salary = salAmount,
                    additionalIncome = monthIncomes,
                    totalSpent = monthExpenses,
                    remaining = remaining,
                    savingsPercentage = savingsPercent
                )
            )
        }
        list
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        // Initial setup check
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.populateDefaultCategories(database.categoryDao())
            AppDatabase.populateDefaultBanks(database.bankDao())
            repository.checkAndApplyDueRecurringExpenses()
            repository.reconcileDuplicateTransactions()

            val settings = preferencesRepository.userSettingsFlow.first()
            if (settings.appLockEnabled && settings.appLockPin.isNotBlank()) {
                _isAppLocked.value = true
            }
        }
    }

    // App Lock Operations
    fun unlockApp(pin: String): Boolean {
        val currentPin = userSettings.value.appLockPin
        if (pin == currentPin) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        if (userSettings.value.appLockEnabled) {
            _isAppLocked.value = true
        }
    }

    // Add / Edit / Delete Expense
    fun addExpense(
        amount: Double,
        categoryId: Long,
        date: String,
        time: String,
        paymentMethod: String,
        note: String,
        isCreditCard: Boolean = paymentMethod.equals("Credit Card", ignoreCase = true),
        cardName: String = "",
        cardMaskedNumber: String = "",
        isBillPayment: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = ExpenseEntity(
                amount = amount,
                categoryId = categoryId,
                date = date,
                time = time,
                paymentMethod = paymentMethod,
                note = note.trim(),
                isCreditCard = isCreditCard,
                cardName = cardName,
                cardMaskedNumber = cardMaskedNumber,
                isBillPayment = isBillPayment
            )
            repository.insertExpense(expense)

            val settings = userSettings.value
            if (settings.funnyReactionsEnabled) {
                val summary = financialSummary.value
                val isRoast = settings.reactionIntensity == "ROAST"
                val reaction = DialogueEngine.getExpenseReaction(
                    amount = amount,
                    monthlySalary = summary.salary,
                    remainingBalance = summary.remainingBalance - amount,
                    isRoastMode = isRoast
                )
                _lastReactionMessage.value = reaction
            }
        }
    }

    fun updateExpense(
        id: Long,
        amount: Double,
        categoryId: Long,
        date: String,
        time: String,
        paymentMethod: String,
        note: String,
        isCreditCard: Boolean? = null,
        cardName: String? = null,
        cardMaskedNumber: String? = null,
        isBillPayment: Boolean? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getExpenseById(id)
            val isCc = isCreditCard ?: (paymentMethod.equals("Credit Card", ignoreCase = true) || (existing?.isCreditCard == true))
            val cName = cardName ?: (existing?.cardName ?: "")
            val cMasked = cardMaskedNumber ?: (existing?.cardMaskedNumber ?: "")
            val isBill = isBillPayment ?: (existing?.isBillPayment ?: false)

            val expense = ExpenseEntity(
                id = id,
                amount = amount,
                categoryId = categoryId,
                date = date,
                time = time,
                paymentMethod = paymentMethod,
                note = note.trim(),
                isCreditCard = isCc,
                cardName = cName,
                cardMaskedNumber = cMasked,
                isBillPayment = isBill,
                smsHash = existing?.smsHash,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateExpense(expense)
        }
    }

    // Bank SMS Tracking Operations
    fun toggleBankTracking(bank: BankEntity, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBankTrackingStatus(bank.id, isEnabled)
        }
    }

    fun addCustomBank(bankName: String, senderPatterns: String = "", colorHex: String = "#3B82F6") {
        viewModelScope.launch(Dispatchers.IO) {
            val code = bankName.filter { it.isLetter() }.take(6).uppercase(java.util.Locale.ENGLISH)
            val newBank = BankEntity(
                bankName = bankName.trim(),
                bankCode = code.ifBlank { "CUSTOM" },
                isTrackingEnabled = false,
                senderPatterns = senderPatterns.trim().ifBlank { code },
                colorHex = colorHex,
                isCustom = true
            )
            repository.insertBank(newBank)
        }
    }

    fun deleteBank(bank: BankEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBank(bank)
        }
    }

    fun importHistoricalSmsForBank(bank: BankEntity, onResult: (SmsScanSummary) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = SmsInboxHelper.importHistoricalSmsForBank(getApplication(), bank)
            withContext(Dispatchers.Main) {
                onResult(summary)
            }
        }
    }

    // Credit Card Operations
    fun addCreditCard(
        cardName: String,
        bankName: String,
        cardNumberMasked: String,
        creditLimit: Double,
        colorHex: String = "#3B82F6",
        billingDay: Int = 0,
        dueDay: Int = 0,
        openingOutstanding: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val masked = if (cardNumberMasked.startsWith("XX", ignoreCase = true)) {
                cardNumberMasked
            } else if (cardNumberMasked.isNotBlank()) {
                "XX" + cardNumberMasked.takeLast(4)
            } else "XX0000"

            val card = CreditCardEntity(
                cardName = cardName.trim().ifBlank { "${bankName.trim()} Credit Card $masked" },
                bankName = bankName.trim().ifBlank { "Bank" },
                cardNumberMasked = masked,
                creditLimit = creditLimit,
                colorHex = colorHex,
                billingDay = billingDay,
                dueDay = dueDay,
                openingOutstanding = openingOutstanding
            )
            repository.insertCreditCard(card)
        }
    }

    fun updateCreditCardDetails(
        card: CreditCardEntity,
        cardName: String,
        creditLimit: Double,
        billingDay: Int,
        dueDay: Int,
        openingOutstanding: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = card.copy(
                cardName = cardName.trim().ifBlank { card.cardName },
                creditLimit = creditLimit,
                billingDay = billingDay,
                dueDay = dueDay,
                openingOutstanding = openingOutstanding
            )
            repository.updateCreditCard(updated)
        }
    }

    fun updateCreditLimit(cardId: Long, limit: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCreditLimit(cardId, limit)
        }
    }

    fun deleteCreditCard(card: CreditCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCreditCard(card)
        }
    }

    fun addManualCreditCardExpense(
        amount: Double,
        cardName: String,
        cardMaskedNumber: String,
        categoryId: Long,
        date: String,
        time: String = DateUtils.nowTime(),
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = ExpenseEntity(
                amount = amount,
                categoryId = categoryId,
                date = date.ifBlank { DateUtils.today() },
                time = time.ifBlank { DateUtils.nowTime() },
                paymentMethod = "Credit Card",
                note = note.ifBlank { "Credit Card Purchase" },
                isCreditCard = true,
                cardName = cardName,
                cardMaskedNumber = cardMaskedNumber,
                isBillPayment = false
            )
            repository.insertExpense(expense)
        }
    }

    fun recordCreditCardBillPayment(
        amount: Double,
        cardMaskedNumber: String,
        cardName: String,
        date: String = DateUtils.today(),
        note: String = "Credit Card Bill Payment"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val billsCatId = categories.value.find { it.name.equals("Bills", ignoreCase = true) }?.id ?: 1L
            val expense = ExpenseEntity(
                amount = amount,
                categoryId = billsCatId,
                date = date.ifBlank { DateUtils.today() },
                time = DateUtils.nowTime(),
                paymentMethod = "Bank Account",
                note = if (note.isNotBlank()) note else "Credit Card Bill Payment: $cardName ($cardMaskedNumber)",
                isCreditCard = false,
                cardName = cardName,
                cardMaskedNumber = cardMaskedNumber,
                isBillPayment = true
            )
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Keep persistent record of the processed SMS hash if originated from SMS so it is NEVER re-imported
            database.processedSmsDao().markAsDeleted(id, "DEBIT")
            repository.deleteExpenseById(id)
        }
    }

    fun dismissReaction() {
        _lastReactionMessage.value = null
    }

    // Salary operations
    fun setSalaryForMonth(month: Int, year: Int, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSalary(month, year, amount)
            _lastReactionMessage.value = DialogueEngine.getPaydayReaction()
        }
    }

    // Income operations
    fun addIncome(amount: Double, type: String, date: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val income = IncomeEntity(
                amount = amount,
                incomeType = type,
                date = date,
                note = note.trim()
            )
            repository.insertIncome(income)
            _lastReactionMessage.value = "Extra income vandhachu! Balance safe zone ku pochu 💰😎"
        }
    }

    fun deleteIncome(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Keep persistent record of the processed SMS hash if originated from SMS so it is NEVER re-imported
            database.processedSmsDao().markAsDeleted(id, "CREDIT")
            repository.deleteIncomeById(id)
        }
    }

    // Category operations
    fun addCategory(name: String, iconName: String, colorHex: String, budget: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            val category = CategoryEntity(
                name = name.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true,
                monthlyBudget = budget
            )
            repository.insertCategory(category)
        }
    }

    fun updateCategoryBudget(categoryId: Long, budget: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategoryBudget(categoryId, budget)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomCategoryById(id)
        }
    }

    // Recurring operations
    fun addRecurringExpense(name: String, amount: Double, categoryId: Long, paymentMethod: String, day: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val recurring = RecurringExpenseEntity(
                name = name.trim(),
                amount = amount,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                recurringDay = day.coerceIn(1, 31)
            )
            repository.insertRecurringExpense(recurring)
        }
    }

    fun deleteRecurringExpense(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecurringExpenseById(id)
        }
    }

    fun toggleRecurringExpense(recurring: RecurringExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecurringExpense(recurring.copy(enabled = !recurring.enabled))
        }
    }

    // Theme Toggle
    fun toggleDayNightTheme(isSystemInDark: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userSettings.value.themeMode
            val isCurrentlyDark = when (current) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDark
            }
            val nextMode = if (isCurrentlyDark) "LIGHT" else "DARK"
            preferencesRepository.setThemeMode(nextMode)
        }
    }

    // Savings goal
    fun setSavingsGoal(goal: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setMonthlySavingsGoal(goal)
        }
    }

    // Onboarding completion
    fun completeOnboarding(salary: Double, cycleType: String, cycleDay: Int, userName: String = "Bro") {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateSalaryConfig(salary, cycleType, cycleDay)
            preferencesRepository.setUserName(userName)
            preferencesRepository.setOnboardingCompleted(true)

            // Save salary for current cycle
            val cycle = DateUtils.calculateCycle(cycleType, cycleDay)
            repository.setSalary(cycle.cycleMonth, cycle.cycleYear, salary, cycleDay)
        }
    }

    // Calendar Navigation
    fun changeCalendarMonth(offset: Long) {
        _selectedCalendarMonth.value = _selectedCalendarMonth.value.plusMonths(offset)
    }

    // Reset Data
    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllData()
            preferencesRepository.clearAllPreferences()
            AppDatabase.populateDefaultCategories(database.categoryDao())
        }
    }

    // Export & Restore
    suspend fun exportDataJson(): String {
        val payload = BackupPayload(
            expenses = repository.getAllExpensesSnapshot(),
            salaries = repository.getAllSalariesSnapshot(),
            incomes = repository.getAllIncomesSnapshot(),
            categories = repository.getAllCategoriesSnapshot(),
            savingsGoals = repository.getAllGoalsSnapshot(),
            recurringExpenses = repository.getAllRecurringSnapshot()
        )
        return ExportImportHelper.exportToJson(payload)
    }

    suspend fun exportDataCsv(): String {
        val expenses = repository.getAllExpensesSnapshot()
        val categories = repository.getAllCategoriesSnapshot().associate { it.id to it.name }
        return ExportImportHelper.exportToCsv(expenses, categories)
    }

    fun restoreFromJson(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = ExportImportHelper.parseFromJson(jsonStr)
                repository.restoreData(
                    expenses = payload.expenses,
                    salaries = payload.salaries,
                    incomes = payload.incomes,
                    categories = payload.categories,
                    goals = payload.savingsGoals,
                    recurring = payload.recurringExpenses
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Invalid backup file format")
            }
        }
    }

    // Auto Bank SMS Methods
    fun toggleAutoSmsDetection(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setAutoSmsDetection(enabled)
        }
    }

    fun toggleAutoSmsNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setAutoSmsNotification(enabled)
        }
    }

    fun scanSmsInbox(onComplete: (SmsScanSummary) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = SmsInboxHelper.scanAndImportBankSms(getApplication(), maxMessagesToScan = 200)
            withContext(Dispatchers.Main) {
                onComplete(summary)
            }
        }
    }

    fun testParseSms(sender: String, body: String): BankSmsParseResult {
        return BankSmsParser.parse(sender, body)
    }

    fun processTestSms(
        sender: String,
        body: String,
        onComplete: (BankSmsProcessResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BankSmsProcessor.processIncomingSms(
                context = getApplication(),
                sender = sender,
                body = body,
                timestamp = System.currentTimeMillis(),
                forceProcess = true
            )
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    fun deleteProcessedSms(hash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.processedSmsDao().markHashAsDeleted(hash)
        }
    }

    /**
     * Scans for bank transactions received today.
     * - First open today: scans all of today's bank SMS received up to current open time.
     * - Subsequent opens today: scans only bank SMS received between [last scan time] and [current open time].
     * Saves current time as new last scan time so previous transactions are never scanned again.
     */
    fun checkAppOpenSms(forceRescanToday: Boolean = false, onDetected: (List<DetectedSmsItem>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val todayString = today.toString()
            val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val settings = userSettings.value
            if (!settings.autoSmsDetectionEnabled) {
                return@launch
            }

            val lastScanTime = settings.lastSmsScanTimestamp
            val lastScanDate = settings.lastSmsScanDate

            // If not scanned yet today or previous scan was before today's start, scan from 00:00:00 today!
            // Otherwise, scan only from last scan time up to now.
            val fromTimestamp = if (forceRescanToday || lastScanDate != todayString || lastScanTime < startOfToday) {
                startOfToday
            } else {
                lastScanTime
            }

            val toTimestamp = now

            val categoriesList = repository.getAllCategoriesSnapshot()
            val detected = SmsInboxHelper.scanUnprocessedBankSmsBetween(
                context = getApplication(),
                fromTimestamp = fromTimestamp,
                toTimestamp = toTimestamp,
                categories = categoriesList
            )

            // Save now as the new last scan time and today as the scan date
            preferencesRepository.updateLastSmsScanTime(now, todayString)

            withContext(Dispatchers.Main) {
                if (detected.isNotEmpty()) {
                    val currentList = _pendingSmsTransactions.value.toMutableList()
                    for (item in detected) {
                        if (currentList.none { it.uniqueHash == item.uniqueHash }) {
                            currentList.add(item)
                        }
                    }
                    _pendingSmsTransactions.value = currentList
                    onDetected(detected)
                }
            }
        }
    }

    /**
     * User confirmed a detected bank SMS transaction with their custom description and category.
     * REQUIRED FIX 1 & 3:
     * - Uses an UPDATE operation for category selection on the existing transaction ID, NOT INSERT.
     * - Prevents duplicate transactions from ever being inserted.
     * - Amount was deducted once when the transaction was created; updating category only updates category attribution.
     * - Marks the SMS as CONFIRMED persistently so it is never re-processed.
     */
    fun confirmDetectedTransaction(
        item: DetectedSmsItem,
        description: String,
        categoryId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedDesc = description.trim().ifEmpty { item.defaultDescription }
            val finalNote = trimmedDesc

            val allCats = categories.value
            val matchedCat = allCats.find { it.name.equals(trimmedDesc, ignoreCase = true) }
                ?: allCats.find { it.id == categoryId }
                ?: allCats.firstOrNull()
            val finalCategoryId = matchedCat?.id ?: categoryId

            if (item.type == TransactionType.DEBIT) {
                // Update the single existing transaction record in the database using transactionId
                val existingExpense = if (item.transactionId > 0L) {
                    repository.getExpenseById(item.transactionId)
                } else null

                if (existingExpense != null) {
                    val updatedExpense = existingExpense.copy(
                        categoryId = finalCategoryId,
                        note = finalNote,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateExpense(updatedExpense)
                } else {
                    // Fallback only if the record was somehow not in DB yet
                    val effectiveDate = if (item.date.isNotBlank() && item.date >= "2026-01-01") item.date else DateUtils.today()
                    val effectiveTime = if (item.time.isNotBlank()) item.time else DateUtils.nowTime()
                    val newExpense = ExpenseEntity(
                        amount = item.amount,
                        categoryId = finalCategoryId,
                        date = effectiveDate,
                        time = effectiveTime,
                        paymentMethod = item.paymentMethod.ifBlank { "UPI" },
                        note = finalNote
                    )
                    val insertedId = repository.insertExpense(newExpense)
                    database.processedSmsDao().updateAssociatedId(item.uniqueHash, insertedId)
                }

                // Update persistent processed_sms state to CONFIRMED
                database.processedSmsDao().updateStatusAndDescription(item.uniqueHash, "CONFIRMED", finalNote)

                // Trigger funny Tamil reaction only when finishing review
                val isLast = _pendingSmsTransactions.value.size <= 1
                if (isLast) {
                    val summary = financialSummary.value
                    val isRoast = userSettings.value.reactionIntensity == "ROAST"
                    val reaction = DialogueEngine.getExpenseReaction(
                        amount = item.amount,
                        monthlySalary = summary.salary,
                        remainingBalance = summary.remainingBalance - item.amount,
                        isRoastMode = isRoast
                    )
                    _lastReactionMessage.value = reaction
                }
            } else {
                // Credit transaction already recorded in ledger; mark confirmed
                database.processedSmsDao().updateStatusAndDescription(item.uniqueHash, "CONFIRMED", finalNote)
                val isLast = _pendingSmsTransactions.value.size <= 1
                if (isLast) {
                    _lastReactionMessage.value = "Bank credit ₹${CurrencyFormatter.format(item.amount)} vandhachu! Balance boosted 💰😎"
                }
            }

            // Remove from pending list
            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = _pendingSmsTransactions.value.filter { it.uniqueHash != item.uniqueHash }
            }
        }
    }

    /**
     * User chooses to skip / ignore a detected bank SMS transaction popup.
     * Retains the original transaction record safely in the database with its initial category.
     * Updates status to CONFIRMED so it never pops up again and is never duplicated.
     */
    fun dismissDetectedTransaction(item: DetectedSmsItem) {
        viewModelScope.launch(Dispatchers.IO) {
            database.processedSmsDao().updateStatus(item.uniqueHash, "CONFIRMED")
            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = _pendingSmsTransactions.value.filter { it.uniqueHash != item.uniqueHash }
            }
        }
    }

    fun dismissAllPendingTransactions() {
        val pending = _pendingSmsTransactions.value
        viewModelScope.launch(Dispatchers.IO) {
            for (item in pending) {
                database.processedSmsDao().updateStatus(item.uniqueHash, "CONFIRMED")
            }
            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = emptyList()
            }
        }
    }

    /**
     * Simulates receiving a bank SMS today and routes it through the exact
     * Detection -> Single Transaction Creation -> User Review & Description -> Update Category flow.
     */
    fun simulateBankSmsForReview(
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        onResult: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val parseResult = BankSmsParser.parse(sender, body, timestamp)
            if (parseResult !is BankSmsParseResult.Success) {
                withContext(Dispatchers.Main) {
                    onResult("Ignored: Not a valid bank debit or credit transaction message.")
                }
                return@launch
            }

            val tx = parseResult.transaction
            val existing = database.processedSmsDao().getProcessedByHashOrRef(tx.uniqueHash, "", tx.referenceId)
            if (existing != null && existing.status != "PENDING_REVIEW") {
                withContext(Dispatchers.Main) {
                    onResult("Already processed! This transaction will not appear again.")
                }
                return@launch
            }

            val cats = repository.getAllCategoriesSnapshot()
            val desc = BankSmsProcessor.suggestDefaultDescription(tx)
            val catId = BankSmsProcessor.matchCategoryForTransaction(tx, cats)
            val note = BankSmsProcessor.buildNote(tx)

            val expenseDate = if (tx.date.isNotBlank() && tx.date >= "2026-01-01") tx.date else DateUtils.today()
            val expenseTime = if (tx.time.isNotBlank()) tx.time else DateUtils.nowTime()

            val transactionId = if (existing != null && existing.associatedId > 0L) {
                existing.associatedId
            } else {
                // Auto-register card if masked card number is detected
                if (tx.cardMaskedNumber.isNotBlank()) {
                    val existingCard = database.creditCardDao().getCardByNumber(tx.cardMaskedNumber)
                    if (existingCard == null) {
                        val newCard = CreditCardEntity(
                            cardName = tx.cardName.ifBlank { "${tx.bankName} Credit Card ${tx.cardMaskedNumber}" },
                            bankName = tx.bankName,
                            cardNumberMasked = tx.cardMaskedNumber,
                            creditLimit = 0.0
                        )
                        database.creditCardDao().insertCard(newCard)
                    }
                }

                // Create single transaction record in DB
                if (tx.type == TransactionType.DEBIT) {
                    val existingExp = database.expenseDao().getExpenseBySmsHash(tx.uniqueHash)
                    if (existingExp != null) {
                        withContext(Dispatchers.Main) {
                            onResult("Already processed! This debit transaction was already recorded.")
                        }
                        return@launch
                    }

                    val exp = ExpenseEntity(
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
                    val id = repository.insertExpense(exp)
                    val record = ProcessedSmsEntity(
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
                        associatedId = id,
                        smsId = "SIM_${System.currentTimeMillis()}",
                        description = desc,
                        status = "PENDING_REVIEW",
                        timestamp = timestamp
                    )
                    database.processedSmsDao().insert(record)
                    id
                } else {
                    // Check duplicate at income level
                    val existingInc = database.incomeDao().getIncomeBySmsHash(tx.uniqueHash)
                    val countInc = database.incomeDao().countBySmsIdentity(tx.uniqueHash, tx.referenceId, "")
                    if (existingInc != null || countInc > 0) {
                        withContext(Dispatchers.Main) {
                            onResult("Already processed! This credit transaction was already recorded.")
                        }
                        return@launch
                    }

                    val inc = IncomeEntity(
                        amount = tx.amount,
                        incomeType = if (tx.isCreditCardRefund) "Credit Card Refund" else "Bank Deposit",
                        date = expenseDate,
                        note = note,
                        isCreditCardRefund = tx.isCreditCardRefund,
                        cardMaskedNumber = tx.cardMaskedNumber,
                        smsHash = tx.uniqueHash,
                        referenceId = tx.referenceId
                    )
                    val id = repository.insertIncome(inc)
                    val record = ProcessedSmsEntity(
                        smsHash = tx.uniqueHash,
                        sender = sender,
                        body = body,
                        amount = tx.amount,
                        transactionType = "CREDIT",
                        bankName = tx.bankName,
                        accountInfo = tx.accountInfo,
                        paymentMethod = tx.paymentMethod,
                        date = expenseDate,
                        time = expenseTime,
                        referenceId = tx.referenceId,
                        associatedId = id,
                        smsId = "SIM_${System.currentTimeMillis()}",
                        description = note,
                        status = "CONFIRMED",
                        timestamp = timestamp
                    )
                    database.processedSmsDao().insert(record)
                    id
                }
            }

            if (tx.type == TransactionType.DEBIT) {
                val detectedItem = DetectedSmsItem(
                    transactionId = transactionId,
                    smsId = "SIM_${System.currentTimeMillis()}",
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
                    timestamp = timestamp,
                    uniqueHash = tx.uniqueHash,
                    defaultDescription = desc,
                    suggestedCategoryId = catId,
                    isCreditCard = tx.isCreditCard,
                    cardMaskedNumber = tx.cardMaskedNumber,
                    cardName = tx.cardName
                )

                withContext(Dispatchers.Main) {
                    val current = _pendingSmsTransactions.value.toMutableList()
                    if (current.none { it.uniqueHash == detectedItem.uniqueHash }) {
                        current.add(detectedItem)
                        _pendingSmsTransactions.value = current
                    }
                    val msg = if (tx.isCreditCard) {
                        "💳 Credit Card ₹${CurrencyFormatter.format(tx.amount)} detected! (Will NOT deduct from Main Wallet). Please confirm category."
                    } else {
                        "Detected ₹${CurrencyFormatter.format(tx.amount)}! Please review and confirm category."
                    }
                    onResult(msg)
                }
            } else {
                withContext(Dispatchers.Main) {
                    val msg = if (tx.isCreditCardRefund) {
                        "Credit Card Refund of ₹${CurrencyFormatter.format(tx.amount)} recorded! CC Outstanding reduced."
                    } else {
                        "Credit of ₹${CurrencyFormatter.format(tx.amount)} added to wallet balance successfully!"
                    }
                    onResult(msg)
                }
            }
        }
    }
}
