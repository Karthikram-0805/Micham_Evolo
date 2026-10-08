package com.example.utils

import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.RecurringExpenseEntity
import com.example.data.entity.SalaryEntity
import com.example.data.entity.SavingsGoalEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val expenses: List<ExpenseEntity>,
    val salaries: List<SalaryEntity>,
    val incomes: List<IncomeEntity>,
    val categories: List<CategoryEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val recurringExpenses: List<RecurringExpenseEntity>
)

object ExportImportHelper {

    fun exportToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val expArray = JSONArray()
        for (e in payload.expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("amount", e.amount)
            obj.put("categoryId", e.categoryId)
            obj.put("date", e.date)
            obj.put("time", e.time)
            obj.put("paymentMethod", e.paymentMethod)
            obj.put("note", e.note)
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        val salArray = JSONArray()
        for (s in payload.salaries) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("month", s.month)
            obj.put("year", s.year)
            obj.put("salaryAmount", s.salaryAmount)
            obj.put("salaryStartDate", s.salaryStartDate)
            salArray.put(obj)
        }
        root.put("salaries", salArray)

        val incArray = JSONArray()
        for (i in payload.incomes) {
            val obj = JSONObject()
            obj.put("id", i.id)
            obj.put("amount", i.amount)
            obj.put("incomeType", i.incomeType)
            obj.put("date", i.date)
            obj.put("note", i.note)
            incArray.put(obj)
        }
        root.put("incomes", incArray)

        val catArray = JSONArray()
        for (c in payload.categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isCustom", c.isCustom)
            obj.put("monthlyBudget", c.monthlyBudget)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val recArray = JSONArray()
        for (r in payload.recurringExpenses) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("amount", r.amount)
            obj.put("categoryId", r.categoryId)
            obj.put("paymentMethod", r.paymentMethod)
            obj.put("recurringDay", r.recurringDay)
            obj.put("enabled", r.enabled)
            recArray.put(obj)
        }
        root.put("recurringExpenses", recArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonStr: String): BackupPayload {
        val root = JSONObject(jsonStr)

        val expenses = mutableListOf<ExpenseEntity>()
        if (root.has("expenses")) {
            val arr = root.getJSONArray("expenses")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                expenses.add(
                    ExpenseEntity(
                        id = o.optLong("id", 0),
                        amount = o.getDouble("amount"),
                        categoryId = o.getLong("categoryId"),
                        date = o.getString("date"),
                        time = o.getString("time"),
                        paymentMethod = o.getString("paymentMethod"),
                        note = o.optString("note", "")
                    )
                )
            }
        }

        val salaries = mutableListOf<SalaryEntity>()
        if (root.has("salaries")) {
            val arr = root.getJSONArray("salaries")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                salaries.add(
                    SalaryEntity(
                        id = o.optLong("id", 0),
                        month = o.getInt("month"),
                        year = o.getInt("year"),
                        salaryAmount = o.getDouble("salaryAmount"),
                        salaryStartDate = o.optInt("salaryStartDate", 1)
                    )
                )
            }
        }

        val incomes = mutableListOf<IncomeEntity>()
        if (root.has("incomes")) {
            val arr = root.getJSONArray("incomes")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                incomes.add(
                    IncomeEntity(
                        id = o.optLong("id", 0),
                        amount = o.getDouble("amount"),
                        incomeType = o.getString("incomeType"),
                        date = o.getString("date"),
                        note = o.optString("note", "")
                    )
                )
            }
        }

        val categories = mutableListOf<CategoryEntity>()
        if (root.has("categories")) {
            val arr = root.getJSONArray("categories")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.optLong("id", 0),
                        name = o.getString("name"),
                        iconName = o.getString("iconName"),
                        colorHex = o.getString("colorHex"),
                        isCustom = o.optBoolean("isCustom", false),
                        monthlyBudget = o.optDouble("monthlyBudget", 0.0)
                    )
                )
            }
        }

        val recurring = mutableListOf<RecurringExpenseEntity>()
        if (root.has("recurringExpenses")) {
            val arr = root.getJSONArray("recurringExpenses")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                recurring.add(
                    RecurringExpenseEntity(
                        id = o.optLong("id", 0),
                        name = o.getString("name"),
                        amount = o.getDouble("amount"),
                        categoryId = o.getLong("categoryId"),
                        paymentMethod = o.getString("paymentMethod"),
                        recurringDay = o.getInt("recurringDay"),
                        enabled = o.optBoolean("enabled", true)
                    )
                )
            }
        }

        return BackupPayload(
            expenses = expenses,
            salaries = salaries,
            incomes = incomes,
            categories = categories,
            savingsGoals = emptyList(),
            recurringExpenses = recurring
        )
    }

    fun exportToCsv(expenses: List<ExpenseEntity>, categoriesMap: Map<Long, String>): String {
        val sb = StringBuilder()
        sb.append("Date,Time,Amount,Category,Payment Method,Note\n")
        for (e in expenses) {
            val catName = categoriesMap[e.categoryId] ?: "Unknown"
            val sanitizedNote = e.note.replace("\"", "\"\"")
            sb.append("${e.date},${e.time},${e.amount},\"$catName\",\"${e.paymentMethod}\",\"$sanitizedNote\"\n")
        }
        return sb.toString()
    }
}
