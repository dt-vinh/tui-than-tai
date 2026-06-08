package com.phuongnn14.tuithantai.data

import android.content.Context
import androidx.room.*
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
    suspend fun adjustBalance(name: String, amount: Double)
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tui_than_tai_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
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
        db.transactionDao().insertTransaction(transaction)
        val multiplier = if (transaction.type == "INCOME") 1.0 else -1.0
        db.accountDao().adjustBalance(transaction.accountName, transaction.amount * multiplier)
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

    suspend fun saveSetting(key: String, value: String) = db.settingDao().insertSetting(UserSettingEntity(key, value))
}
