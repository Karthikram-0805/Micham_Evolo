package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.CreditCardDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.ProcessedSmsDao
import com.example.data.dao.RecurringExpenseDao
import com.example.data.dao.SalaryDao
import com.example.data.dao.SavingsGoalDao
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CreditCardEntity
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
        ProcessedSmsEntity::class,
        CreditCardEntity::class
    ],
    version = 4,
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
    abstract fun creditCardDao(): CreditCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create credit_cards table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardName` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `cardNumberMasked` TEXT NOT NULL,
                        `creditLimit` REAL NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_credit_cards_cardNumberMasked_bankName` ON `credit_cards` (`cardNumberMasked`, `bankName`)")

                // 2. Add columns to expenses table
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `isCreditCard` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `cardName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `cardMaskedNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `isBillPayment` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `smsHash` TEXT DEFAULT NULL")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_smsHash` ON `expenses` (`smsHash`)")

                // 3. Add columns to incomes table
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `isCreditCardRefund` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `cardMaskedNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `smsHash` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `smsId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `referenceId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_incomes_smsHash` ON `incomes` (`smsHash`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "micham_evlo.db"
                )
                    .addMigrations(MIGRATION_3_4)
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
