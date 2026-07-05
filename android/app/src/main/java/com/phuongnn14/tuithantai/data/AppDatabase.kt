package com.phuongnn14.tuithantai.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

// --- Entities ---

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val accountName: String,
    val date: Long,
    val note: String = "",
    val isSynced: Boolean = false,
    val imageUri: String? = null
) : Serializable

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val name: String,
    val balance: Double,
    val currency: String = "VND"
) : Serializable

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryName: String,
    val amount: Double,
    val period: String,
    val startDate: Long,
    val endDate: Long = 0
) : Serializable

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val isDefault: Boolean = false
) : Serializable

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val amount: Double,
    val category: String,
    val accountName: String,
    val cycle: String,
    val startDate: Long,
    val isEnabled: Boolean = true
) : Serializable

@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey val key: String,
    val value: String
) : Serializable

// --- DAOs ---

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("UPDATE transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("UPDATE transactions SET accountName = :newName WHERE accountName = :oldName")
    suspend fun updateAccountName(oldName: String, newName: String)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE name = :name")
    suspend fun deleteAccountByName(name: String)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE name = :name")
    suspend fun adjustBalance(name: String, amount: Double): Int
}

@Dao
abstract class WalletTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTransactionInternal(transaction: TransactionEntity): Long

    @Query("UPDATE accounts SET balance = balance + :amount WHERE name = :name")
    protected abstract suspend fun adjustBalanceInternal(name: String, amount: Double): Int

    @Transaction
    open suspend fun insertTransactionAndAdjustBalance(transaction: TransactionEntity) {
        insertTransactionInternal(transaction)
        val multiplier = if (transaction.type == "INCOME") 1.0 else -1.0
        val updatedRows = adjustBalanceInternal(transaction.accountName, transaction.amount * multiplier)
        check(updatedRows == 1) { "Account not found: ${transaction.accountName}" }
    }
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("UPDATE budgets SET categoryName = :newName WHERE categoryName = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: Long)

    @Query("UPDATE recurring_transactions SET accountName = :newName WHERE accountName = :oldName")
    suspend fun updateAccountName(oldName: String, newName: String)

    @Query("UPDATE recurring_transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)
}

@Dao
interface UserSettingDao {
    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValueByKey(key: String): String?

    /** Load tất cả settings 1 query duy nhất — dùng khi khởi động app */
    @Query("SELECT * FROM user_settings")
    suspend fun getAllSettings(): List<UserSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSettingEntity)
}

// --- App Database ---

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class,
        UserSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringTransactionDao
    abstract fun settingDao(): UserSettingDao
    abstract fun walletTransactionDao(): WalletTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Legacy v1 used different tables (expenses/categories). Keep them under
                // legacy names and create the v2 schema; do not destructively drop user data.
                renameLegacyTableIfExists(db, "categories", "legacy_categories_v1")
                renameLegacyTableIfExists(db, "expenses", "legacy_expenses_v1")
                createV2Tables(db)
                migrateLegacyExpensesIfPresent(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tui_than_tai_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun createV2Tables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `type` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `accountName` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `isSynced` INTEGER NOT NULL,
                    `imageUri` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `accounts` (
                    `name` TEXT NOT NULL,
                    `balance` REAL NOT NULL,
                    `currency` TEXT NOT NULL,
                    PRIMARY KEY(`name`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budgets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `period` TEXT NOT NULL,
                    `startDate` INTEGER NOT NULL,
                    `endDate` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `categories` (
                    `name` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `isDefault` INTEGER NOT NULL,
                    PRIMARY KEY(`name`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `category` TEXT NOT NULL,
                    `accountName` TEXT NOT NULL,
                    `cycle` TEXT NOT NULL,
                    `startDate` INTEGER NOT NULL,
                    `isEnabled` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_settings` (
                    `key` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`key`)
                )
                """.trimIndent()
            )
        }

        private fun migrateLegacyExpensesIfPresent(db: SupportSQLiteDatabase) {
            if (!tableExists(db, "legacy_expenses_v1")) return
            db.execSQL(
                """
                INSERT OR IGNORE INTO `accounts` (`name`, `balance`, `currency`)
                SELECT DISTINCT
                    COALESCE(NULLIF(`wallet`, ''), 'Ví cá nhân'),
                    0.0,
                    COALESCE(NULLIF(`currency`, ''), 'VND')
                FROM `legacy_expenses_v1`
                WHERE `deletedAt` IS NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `transactions`
                    (`title`, `amount`, `type`, `category`, `accountName`, `date`, `note`, `isSynced`, `imageUri`)
                SELECT
                    COALESCE(NULLIF(`title`, ''), 'Giao dịch cũ'),
                    CAST(`amount` AS REAL),
                    'EXPENSE',
                    COALESCE(NULLIF(`categoryId`, ''), 'Khác'),
                    COALESCE(NULLIF(`wallet`, ''), 'Ví cá nhân'),
                    `spentAt`,
                    COALESCE(`note`, ''),
                    CASE WHEN `syncStatus` = 'Synced' THEN 1 ELSE 0 END,
                    `receiptPath`
                FROM `legacy_expenses_v1`
                WHERE `deletedAt` IS NULL
                """.trimIndent()
            )
        }

        private fun renameLegacyTableIfExists(
            db: SupportSQLiteDatabase,
            oldName: String,
            legacyName: String
        ) {
            if (tableExists(db, oldName) && !tableExists(db, legacyName)) {
                db.execSQL("ALTER TABLE `$oldName` RENAME TO `$legacyName`")
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean =
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(tableName)
            ).use { cursor -> cursor.moveToFirst() }
    }
}

// --- Repository ---

class LuckyWalletRepository(private val db: AppDatabase) {

    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactionsFlow()
    val accounts: Flow<List<AccountEntity>> = db.accountDao().getAllAccountsFlow()
    val budgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgetsFlow()
    val categories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategoriesFlow()
    val recurringTransactions: Flow<List<RecurringTransactionEntity>> = db.recurringDao().getAllRecurringFlow()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        db.walletTransactionDao().insertTransactionAndAdjustBalance(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.transactionDao().deleteTransactionById(transaction.id)
        val multiplier = if (transaction.type == "INCOME") -1.0 else 1.0
        db.accountDao().adjustBalance(transaction.accountName, transaction.amount * multiplier)
    }

    suspend fun getUnsyncedTransactions(): List<TransactionEntity> = db.transactionDao().getUnsyncedTransactions()

    suspend fun markTransactionsSynced(ids: List<Long>) = db.transactionDao().markSynced(ids)

    suspend fun insertAccount(account: AccountEntity) = db.accountDao().insertAccount(account)

    suspend fun deleteAccount(account: AccountEntity) = db.accountDao().deleteAccountByName(account.name)

    suspend fun updateAccountReferences(oldName: String, newName: String) {
        db.transactionDao().updateAccountName(oldName, newName)
        db.recurringDao().updateAccountName(oldName, newName)
    }

    suspend fun insertBudget(budget: BudgetEntity) = db.budgetDao().insertBudget(budget)

    suspend fun deleteBudget(id: Long) = db.budgetDao().deleteBudgetById(id)

    suspend fun insertCategory(category: CategoryEntity) = db.categoryDao().insertCategory(category)

    suspend fun deleteCategory(name: String) = db.categoryDao().deleteCategoryByName(name)

    suspend fun updateCategoryReferences(oldName: String, newName: String) {
        db.transactionDao().updateCategoryName(oldName, newName)
        db.recurringDao().updateCategoryName(oldName, newName)
        db.budgetDao().updateCategoryName(oldName, newName)
    }

    suspend fun insertRecurring(recurring: RecurringTransactionEntity) = db.recurringDao().insertRecurring(recurring)

    suspend fun deleteRecurring(id: Long) = db.recurringDao().deleteRecurringById(id)

    suspend fun getSetting(key: String): String? = db.settingDao().getValueByKey(key)

    /** Load tất cả settings 1 lần — nhanh hơn nhiều lần getSetting() riêng lẻ */
    suspend fun getAllSettings(): Map<String, String> =
        db.settingDao().getAllSettings().associate { it.key to it.value }

    suspend fun saveSetting(key: String, value: String) = db.settingDao().insertSetting(UserSettingEntity(key, value))
}
