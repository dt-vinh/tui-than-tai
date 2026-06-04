package com.phuongnn14.tuithantai.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class ExpenseRepository(
    private val database: AppDatabase
) {
    val categories: Flow<List<CategoryEntity>> = database.categoryDao().observeCategories()
    val expenses: Flow<List<ExpenseEntity>> = database.expenseDao().observeExpenses()

    suspend fun ensureSeedData() {
        if (database.categoryDao().getCategories().isEmpty()) {
            database.categoryDao().upsertAll(defaultCategories)
        }
    }

    fun monthlyTotal(): Flow<Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return database.expenseDao().observeTotal(start, end)
    }

    fun categoryTotals(): Flow<List<CategoryTotal>> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return database.expenseDao().observeCategoryTotals(start, end)
    }

    suspend fun saveExpense(
        title: String,
        amount: Long,
        categoryId: String,
        wallet: String,
        note: String,
        receiptPath: String?,
        ocrText: String?,
        spentAt: Long = System.currentTimeMillis()
    ) {
        val now = System.currentTimeMillis()
        database.expenseDao().upsert(
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Expense" },
                amount = amount.coerceAtLeast(0),
                categoryId = categoryId,
                wallet = wallet.ifBlank { "Vi ca nhan" },
                note = note,
                receiptPath = receiptPath,
                ocrText = ocrText,
                spentAt = spentAt,
                updatedAt = now,
                syncStatus = SyncStatus.Pending
            )
        )
    }
}
