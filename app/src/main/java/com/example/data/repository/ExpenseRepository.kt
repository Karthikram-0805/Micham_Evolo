package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.RecurringExpenseDao
import com.example.data.dao.SalaryDao
import com.example.data.dao.SavingsGoalDao
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.RecurringExpenseEntity
import com.example.data.entity.SalaryEntity
import com.example.data.entity.SavingsGoalEntity
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val salaryDao: SalaryDao,
    private val categoryDao: CategoryDao,
    private val incomeDao: IncomeDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {
    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesForDate(date: String): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByDate(date)

    fun getExpensesBetweenDates(startDate: String, endDate: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesBetweenDates(startDate, endDate)

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    // Salaries
    fun getSalary(month: Int, year: Int): Flow<SalaryEntity?> = salaryDao.getSalary(month, year)

    fun getAllSalaries(): Flow<List<SalaryEntity>> = salaryDao.getAllSalaries()

    suspend fun getSalarySnapshot(month: Int, year: Int): SalaryEntity? =
        salaryDao.getSalarySnapshot(month, year)

    suspend fun setSalary(month: Int, year: Int, amount: Double, startDate: Int = 1): Long {
        val existing = salaryDao.getSalarySnapshot(month, year)
        val entity = existing?.copy(salaryAmount = amount, salaryStartDate = startDate)
            ?: SalaryEntity(month = month, year = year, salaryAmount = amount, salaryStartDate = startDate)
        return salaryDao.insertOrUpdateSalary(entity)
    }

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    suspend fun updateCategoryBudget(categoryId: Long, budget: Double) =
        categoryDao.updateCategoryBudget(categoryId, budget)

    suspend fun deleteCustomCategoryById(id: Long) = categoryDao.deleteCustomCategoryById(id)

    // Incomes
    val allIncomes: Flow<List<IncomeEntity>> = incomeDao.getAllIncomes()

    fun getIncomesBetweenDates(startDate: String, endDate: String): Flow<List<IncomeEntity>> =
        incomeDao.getIncomesBetweenDates(startDate, endDate)

    suspend fun insertIncome(income: IncomeEntity): Long = incomeDao.insertIncome(income)

    suspend fun deleteIncome(income: IncomeEntity) = incomeDao.deleteIncome(income)

    suspend fun deleteIncomeById(id: Long) = incomeDao.deleteIncomeById(id)

    // Savings Goals
    fun getSavingsGoal(month: Int, year: Int): Flow<SavingsGoalEntity?> =
        savingsGoalDao.getSavingsGoal(month, year)

    suspend fun setSavingsGoal(month: Int, year: Int, target: Double): Long {
        val existing = savingsGoalDao.getSavingsGoalSnapshot(month, year)
        val entity = existing?.copy(targetAmount = target)
            ?: SavingsGoalEntity(month = month, year = year, targetAmount = target)
        return savingsGoalDao.insertOrUpdateGoal(entity)
    }

    // Recurring Expenses
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> =
        recurringExpenseDao.getAllRecurringExpenses()

    suspend fun insertRecurringExpense(recurring: RecurringExpenseEntity): Long =
        recurringExpenseDao.insertRecurringExpense(recurring)

    suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity) =
        recurringExpenseDao.updateRecurringExpense(recurring)

    suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity) =
        recurringExpenseDao.deleteRecurringExpense(recurring)

    suspend fun deleteRecurringExpenseById(id: Long) =
        recurringExpenseDao.deleteRecurringExpenseById(id)

    suspend fun checkAndApplyDueRecurringExpenses(): Int {
        val today = LocalDate.now()
        val currentMonthYear = "${today.year}-${String.format("%02d", today.monthValue)}"
        val currentDay = today.dayOfMonth

        val activeList = recurringExpenseDao.getActiveRecurringExpensesSnapshot()
        var appliedCount = 0

        for (recurring in activeList) {
            if (recurring.lastAppliedMonthYear != currentMonthYear && currentDay >= recurring.recurringDay) {
                // Insert corresponding expense for today
                val expense = ExpenseEntity(
                    amount = recurring.amount,
                    categoryId = recurring.categoryId,
                    date = DateUtils.today(),
                    time = DateUtils.nowTime(),
                    paymentMethod = recurring.paymentMethod,
                    note = "Recurring: ${recurring.name}"
                )
                expenseDao.insertExpense(expense)
                recurringExpenseDao.updateLastAppliedMonthYear(recurring.id, currentMonthYear)
                appliedCount++
            }
        }
        return appliedCount
    }

    // Snapshot exports
    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity> = expenseDao.getAllExpensesSnapshot()
    suspend fun getAllSalariesSnapshot(): List<SalaryEntity> = salaryDao.getAllSalariesSnapshot()
    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity> = categoryDao.getAllCategoriesSnapshot()
    suspend fun getAllIncomesSnapshot(): List<IncomeEntity> = incomeDao.getAllIncomesSnapshot()
    suspend fun getAllGoalsSnapshot(): List<SavingsGoalEntity> = savingsGoalDao.getAllGoalsSnapshot()
    suspend fun getAllRecurringSnapshot(): List<RecurringExpenseEntity> = recurringExpenseDao.getAllRecurringExpensesSnapshot()

    // Clear all data (Reset Data)
    suspend fun resetAllData() {
        expenseDao.clearAllExpenses()
        salaryDao.clearAllSalaries()
        incomeDao.clearAllIncomes()
        savingsGoalDao.clearAllGoals()
        recurringExpenseDao.clearAllRecurring()
        categoryDao.clearAllCategories()
    }

    suspend fun restoreData(
        expenses: List<ExpenseEntity>,
        salaries: List<SalaryEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        goals: List<SavingsGoalEntity>,
        recurring: List<RecurringExpenseEntity>
    ) {
        if (categories.isNotEmpty()) {
            categoryDao.insertCategories(categories)
        }
        if (salaries.isNotEmpty()) {
            salaryDao.insertAll(salaries)
        }
        if (expenses.isNotEmpty()) {
            expenseDao.insertAll(expenses)
        }
        if (incomes.isNotEmpty()) {
            incomeDao.insertAll(incomes)
        }
        if (goals.isNotEmpty()) {
            savingsGoalDao.insertAll(goals)
        }
        if (recurring.isNotEmpty()) {
            recurringExpenseDao.insertAll(recurring)
        }
    }
}
