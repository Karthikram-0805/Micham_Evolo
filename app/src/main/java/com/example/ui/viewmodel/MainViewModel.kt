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
import java.time.YearMonth
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ExpenseRepository(
        expenseDao = database.expenseDao(),
        salaryDao = database.salaryDao(),
        categoryDao = database.categoryDao(),
        incomeDao = database.incomeDao(),
        savingsGoalDao = database.savingsGoalDao(),
        recurringExpenseDao = database.recurringExpenseDao()
    )
    val preferencesRepository = UserPreferencesRepository(application)

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
        val salaryAmount = salaryRecord?.salaryAmount ?: settings.defaultSalary

        // Incomes in this cycle
        val cycleIncomes = incomes.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }
        val totalAdditionalIncome = cycleIncomes.sumOf { it.amount }
        val totalAvailable = salaryAmount + totalAdditionalIncome

        // Expenses in this cycle
        val cycleExpenses = expenses.filter { it.date >= cycle.startDate && it.date <= cycle.endDate }
        val totalExpenses = cycleExpenses.sumOf { it.amount }
        val remaining = totalAvailable - totalExpenses

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
            repository.checkAndApplyDueRecurringExpenses()

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
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = ExpenseEntity(
                amount = amount,
                categoryId = categoryId,
                date = date,
                time = time,
                paymentMethod = paymentMethod,
                note = note.trim()
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
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = ExpenseEntity(
                id = id,
                amount = amount,
                categoryId = categoryId,
                date = date,
                time = time,
                paymentMethod = paymentMethod,
                note = note.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
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
            database.processedSmsDao().deleteByHash(hash)
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
     * User confirmed a detected bank SMS transaction with their custom description.
     * Saves as Expense (if Debit) or Income (if Credit), updates balance, marks as processed in DB.
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

            val effectiveDate = DateUtils.today()
            val effectiveTime = if (item.time.isNotBlank()) item.time else DateUtils.nowTime()

            var newAssociatedId = 0L

            if (item.type == TransactionType.DEBIT) {
                // Save as Expense -> automatically deducts from available balance and adds to damage/expenses list
                val expense = ExpenseEntity(
                    amount = item.amount,
                    categoryId = finalCategoryId,
                    date = effectiveDate,
                    time = effectiveTime,
                    paymentMethod = item.paymentMethod.ifBlank { "UPI" },
                    note = finalNote
                )
                newAssociatedId = repository.insertExpense(expense)

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
                // Save as Income -> automatically adds to available balance
                val income = IncomeEntity(
                    amount = item.amount,
                    incomeType = if (trimmedDesc.contains("Salary", ignoreCase = true)) "Salary" else "Bank Deposit",
                    date = effectiveDate,
                    note = finalNote
                )
                newAssociatedId = repository.insertIncome(income)
                val isLast = _pendingSmsTransactions.value.size <= 1
                if (isLast) {
                    _lastReactionMessage.value = "Bank credit ₹${CurrencyFormatter.format(item.amount)} vandhachu! Balance boosted 💰😎"
                }
            }

            // Save in processed_sms table so it NEVER appears again even after restart
            val processedRecord = ProcessedSmsEntity(
                smsHash = item.uniqueHash,
                sender = item.sender,
                body = item.body,
                amount = item.amount,
                transactionType = item.type.name,
                bankName = item.bankName,
                accountInfo = item.accountInfo,
                paymentMethod = item.paymentMethod,
                date = effectiveDate,
                time = effectiveTime,
                referenceId = item.referenceId,
                associatedId = newAssociatedId,
                smsId = item.smsId,
                description = trimmedDesc,
                status = "CONFIRMED",
                timestamp = System.currentTimeMillis()
            )
            database.processedSmsDao().insert(processedRecord)

            // Remove from pending list
            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = _pendingSmsTransactions.value.filter { it.uniqueHash != item.uniqueHash }
            }
        }
    }

    /**
     * User chooses to skip / ignore a detected bank SMS transaction.
     * Records as SKIPPED in DB so it is never shown again.
     */
    fun dismissDetectedTransaction(item: DetectedSmsItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val processedRecord = ProcessedSmsEntity(
                smsHash = item.uniqueHash,
                sender = item.sender,
                body = item.body,
                amount = item.amount,
                transactionType = item.type.name,
                bankName = item.bankName,
                accountInfo = item.accountInfo,
                paymentMethod = item.paymentMethod,
                date = item.date,
                time = item.time,
                referenceId = item.referenceId,
                associatedId = 0L,
                smsId = item.smsId,
                description = "Skipped by user",
                status = "SKIPPED",
                timestamp = System.currentTimeMillis()
            )
            database.processedSmsDao().insert(processedRecord)

            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = _pendingSmsTransactions.value.filter { it.uniqueHash != item.uniqueHash }
            }
        }
    }

    fun dismissAllPendingTransactions() {
        val pending = _pendingSmsTransactions.value
        viewModelScope.launch(Dispatchers.IO) {
            for (item in pending) {
                val processedRecord = ProcessedSmsEntity(
                    smsHash = item.uniqueHash,
                    sender = item.sender,
                    body = item.body,
                    amount = item.amount,
                    transactionType = item.type.name,
                    bankName = item.bankName,
                    accountInfo = item.accountInfo,
                    paymentMethod = item.paymentMethod,
                    date = item.date,
                    time = item.time,
                    referenceId = item.referenceId,
                    associatedId = 0L,
                    smsId = item.smsId,
                    description = "Skipped by user",
                    status = "SKIPPED",
                    timestamp = System.currentTimeMillis()
                )
                database.processedSmsDao().insert(processedRecord)
            }
            withContext(Dispatchers.Main) {
                _pendingSmsTransactions.value = emptyList()
            }
        }
    }

    /**
     * Simulates receiving a bank SMS today and routes it through the exact
     * Detection -> User Review & Description -> Confirm -> Spent Added to Expenses flow.
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
            val alreadyInDb = database.processedSmsDao().isSmsProcessed(tx.uniqueHash, "", tx.referenceId)
            if (alreadyInDb > 0) {
                withContext(Dispatchers.Main) {
                    onResult("Already processed! This transaction will not appear again.")
                }
                return@launch
            }

            val cats = repository.getAllCategoriesSnapshot()
            val desc = BankSmsProcessor.suggestDefaultDescription(tx)
            val catId = BankSmsProcessor.matchCategoryForTransaction(tx, cats)

            val detectedItem = DetectedSmsItem(
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
                date = tx.date,
                time = tx.time,
                timestamp = timestamp,
                uniqueHash = tx.uniqueHash,
                defaultDescription = desc,
                suggestedCategoryId = catId
            )

            withContext(Dispatchers.Main) {
                val current = _pendingSmsTransactions.value.toMutableList()
                if (current.none { it.uniqueHash == detectedItem.uniqueHash }) {
                    current.add(detectedItem)
                    _pendingSmsTransactions.value = current
                }
                onResult("Detected ₹${CurrencyFormatter.format(tx.amount)} ${tx.type.name}! Please enter description.")
            }
        }
    }
}
