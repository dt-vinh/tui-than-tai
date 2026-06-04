package com.phuongnn14.tuithantai.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameVi: String,
    val nameEn: String,
    val keywords: String,
    val sortOrder: Int
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Long,
    val currency: String = "VND",
    val categoryId: String,
    val wallet: String = "Vi ca nhan",
    val note: String = "",
    val receiptPath: String? = null,
    val ocrText: String? = null,
    val spentAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.Pending,
    val serverVersion: Long = 0
)

enum class SyncStatus {
    Pending,
    Synced,
    Failed
}

data class CategoryTotal(
    val categoryId: String,
    val total: Long
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoryEntity>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY spentAt DESC, updatedAt DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE syncStatus != 'Synced' AND deletedAt IS NULL")
    suspend fun getPendingSync(): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE deletedAt IS NULL AND spentAt BETWEEN :from AND :to")
    fun observeTotal(from: Long, to: Long): Flow<Long>

    @Query("SELECT categoryId, COALESCE(SUM(amount), 0) AS total FROM expenses WHERE deletedAt IS NULL AND spentAt BETWEEN :from AND :to GROUP BY categoryId ORDER BY total DESC")
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("UPDATE expenses SET syncStatus = :status, serverVersion = :serverVersion WHERE id = :id")
    suspend fun setSyncStatus(id: String, status: SyncStatus, serverVersion: Long)
}

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tui_than_tai.db"
                ).build().also { instance = it }
            }
        }
    }
}

val defaultCategories = listOf(
    CategoryEntity("food", "Ăn uống", "Food & drink", "food,restaurant,banh mi,meal,com,pho", 0),
    CategoryEntity("coffee", "Cafe", "Coffee", "coffee,cafe,tra sua,drink", 1),
    CategoryEntity("transport", "Đi lại", "Transport", "taxi,grab,bus,fuel,xang", 2),
    CategoryEntity("shopping", "Mua sắm", "Shopping", "shop,market,store,clothes", 3),
    CategoryEntity("bills", "Hóa đơn cố định", "Bills", "electricity,water,internet,phone,bill", 4),
    CategoryEntity("home", "Nhà cửa", "Home", "household,gia dung,furniture", 5),
    CategoryEntity("health", "Sức khỏe", "Health", "medicine,pharmacy,clinic", 6),
    CategoryEntity("entertainment", "Giải trí", "Entertainment", "movie,game,karaoke", 7),
    CategoryEntity("travel", "Du lịch", "Travel", "hotel,flight,trip", 8),
    CategoryEntity("family", "Gia đình", "Family", "parents,bo me,family", 9),
    CategoryEntity("gifts", "Quà tặng", "Gifts", "gift,donate", 10),
    CategoryEntity("repair", "Sửa chữa", "Repair", "repair,service", 11),
    CategoryEntity("other", "Khác", "Other", "other", 12)
)
