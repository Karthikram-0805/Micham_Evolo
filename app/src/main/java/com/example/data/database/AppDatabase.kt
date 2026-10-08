package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.ProcessedSmsDao
import com.example.data.dao.RecurringExpenseDao
import com.example.data.dao.SalaryDao
import com.example.data.dao.SavingsGoalDao
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.IncomeEntity
import com.example.data.entity.ProcessedSmsEntity
import com.example.data.entity.RecurringExpenseEntity
import com.example.data.entity.SalaryEntity
import com.example.data.entity.SavingsGoalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SalaryEntity::class,
        ExpenseEntity::class,
        CategoryEntity::class,
        IncomeEntity::class,
        SavingsGoalEntity::class,
        RecurringExpenseEntity::class,
        ProcessedSmsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salaryDao(): SalaryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun processedSmsDao(): ProcessedSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "micham_evlo.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Food", iconName = "restaurant", colorHex = "#EF4444"),
            CategoryEntity(name = "Tea/Coffee", iconName = "local_cafe", colorHex = "#F59E0B"),
            CategoryEntity(name = "Travel", iconName = "directions_bus", colorHex = "#06B6D4"),
            CategoryEntity(name = "Petrol", iconName = "local_gas_station", colorHex = "#EC4899"),
            CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#8B5CF6"),
            CategoryEntity(name = "Bills", iconName = "receipt_long", colorHex = "#EAB308"),
            CategoryEntity(name = "Rent", iconName = "home", colorHex = "#3B82F6"),
            CategoryEntity(name = "EMI", iconName = "credit_card", colorHex = "#6366F1"),
            CategoryEntity(name = "Subscriptions", iconName = "subscriptions", colorHex = "#A855F7"),
            CategoryEntity(name = "Entertainment", iconName = "movie", colorHex = "#F43F5E"),
            CategoryEntity(name = "Health", iconName = "medical_services", colorHex = "#10B981"),
            CategoryEntity(name = "Groceries", iconName = "shopping_cart", colorHex = "#84CC16"),
            CategoryEntity(name = "Family", iconName = "family_restroom", colorHex = "#14B8A6"),
            CategoryEntity(name = "Friends", iconName = "groups", colorHex = "#F97316"),
            CategoryEntity(name = "Education", iconName = "school", colorHex = "#0284C7"),
            CategoryEntity(name = "Online Shopping", iconName = "inventory_2", colorHex = "#6D28D9"),
            CategoryEntity(name = "Other", iconName = "more_horiz", colorHex = "#64748B")
        )

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DEFAULT_CATEGORIES)
            }
        }
    }
}
