package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BankDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CreditCardDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.IncomeDao
import com.example.data.dao.ProcessedSmsDao
import com.example.data.dao.RecurringExpenseDao
import com.example.data.dao.SalaryDao
import com.example.data.dao.SavingsGoalDao
import com.example.data.entity.BankEntity
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
        CreditCardEntity::class,
        BankEntity::class
    ],
    version = 5,
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
    abstract fun bankDao(): BankDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create banks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `banks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `bankCode` TEXT NOT NULL,
                        `isTrackingEnabled` INTEGER NOT NULL DEFAULT 0,
                        `enabledAtTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `senderPatterns` TEXT NOT NULL DEFAULT '',
                        `colorHex` TEXT NOT NULL DEFAULT '#3B82F6',
                        `isCustom` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_banks_bankCode` ON `banks` (`bankCode`)")

                // 2. Add billingDay, dueDay, openingOutstanding to credit_cards
                db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `billingDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `dueDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `openingOutstanding` REAL NOT NULL DEFAULT 0.0")

                // Pre-populate default banks with tracking OFF
                val now = System.currentTimeMillis()
                val defaultBanks = listOf(
                    Triple("HDFC Bank", "HDFC", "#004C8F"),
                    Triple("ICICI Bank", "ICICI", "#F37021"),
                    Triple("SBI", "SBI", "#280071"),
                    Triple("Axis Bank", "AXIS", "#97144D"),
                    Triple("Indian Bank", "INDIAN", "#0A3B82"),
                    Triple("Canara Bank", "CANARA", "#0083CA"),
                    Triple("Kotak Mahindra Bank", "KOTAK", "#ED1B24"),
                    Triple("Bank of Baroda", "BOB", "#F26522"),
                    Triple("Punjab National Bank", "PNB", "#A20A3C")
                )
                for ((name, code, color) in defaultBanks) {
                    val patterns = when (code) {
                        "HDFC" -> "HDFCBK,HDFC"
                        "ICICI" -> "ICICIB,ICICI"
                        "SBI" -> "SBI,SBIINB,SBIPSG,STATE BANK"
                        "AXIS" -> "AXIS,AXISBK,UTIB,UTIBNK"
                        "INDIAN" -> "INDBNK,INDIAN,INDIAN BANK"
                        "CANARA" -> "CANBNK,CANARA"
                        "KOTAK" -> "KOTAK,KOTAKB"
                        "BOB" -> "BOB,BOBTXN,BARODA"
                        "PNB" -> "PNB,PNBSMS"
                        else -> code
                    }
                    db.execSQL("""
                        INSERT OR IGNORE INTO `banks` (`bankName`, `bankCode`, `isTrackingEnabled`, `enabledAtTimestamp`, `senderPatterns`, `colorHex`, `isCustom`, `createdAt`)
                        VALUES ('$name', '$code', 0, 0, '$patterns', '$color', 0, $now)
                    """.trimIndent())
                }
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "micham_evlo.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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

        val DEFAULT_BANKS = listOf(
            BankEntity(bankName = "HDFC Bank", bankCode = "HDFC", isTrackingEnabled = false, senderPatterns = "HDFCBK,HDFC", colorHex = "#004C8F"),
            BankEntity(bankName = "ICICI Bank", bankCode = "ICICI", isTrackingEnabled = false, senderPatterns = "ICICIB,ICICI", colorHex = "#F37021"),
            BankEntity(bankName = "SBI", bankCode = "SBI", isTrackingEnabled = false, senderPatterns = "SBI,SBIINB,SBIPSG,STATE BANK", colorHex = "#280071"),
            BankEntity(bankName = "Axis Bank", bankCode = "AXIS", isTrackingEnabled = false, senderPatterns = "AXIS,AXISBK,UTIB,UTIBNK", colorHex = "#97144D"),
            BankEntity(bankName = "Indian Bank", bankCode = "INDIAN", isTrackingEnabled = false, senderPatterns = "INDBNK,INDIAN,INDIAN BANK", colorHex = "#0A3B82"),
            BankEntity(bankName = "Canara Bank", bankCode = "CANARA", isTrackingEnabled = false, senderPatterns = "CANBNK,CANARA", colorHex = "#0083CA"),
            BankEntity(bankName = "Kotak Mahindra Bank", bankCode = "KOTAK", isTrackingEnabled = false, senderPatterns = "KOTAK,KOTAKB", colorHex = "#ED1B24"),
            BankEntity(bankName = "Bank of Baroda", bankCode = "BOB", isTrackingEnabled = false, senderPatterns = "BOB,BOBTXN,BARODA", colorHex = "#F26522"),
            BankEntity(bankName = "Punjab National Bank", bankCode = "PNB", isTrackingEnabled = false, senderPatterns = "PNB,PNBSMS", colorHex = "#A20A3C")
        )

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultCategories(database.categoryDao())
                        populateDefaultBanks(database.bankDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DEFAULT_CATEGORIES)
            }
        }

        suspend fun populateDefaultBanks(bankDao: BankDao) {
            if (bankDao.getBankCount() == 0) {
                bankDao.insertBanks(DEFAULT_BANKS)
            }
        }
    }
}
