package com.phuongnn14.tuithantai.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phuongnn14.tuithantai.data.AppDatabase
import com.phuongnn14.tuithantai.data.SyncStatus
import com.phuongnn14.tuithantai.settings.SettingsStore
import kotlinx.coroutines.flow.first

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).settings.first()
        if (settings.accessToken.isBlank()) return Result.success()

        val database = AppDatabase.get(applicationContext)
        val pending = database.expenseDao().getPendingSync()
        if (pending.isEmpty()) return Result.success()

        return runCatching {
            val versions = BackendClient(settings.backendUrl).pushExpenses(settings.accessToken, pending)
            pending.forEach { expense ->
                database.expenseDao().setSyncStatus(
                    expense.id,
                    SyncStatus.Synced,
                    versions[expense.id] ?: expense.serverVersion
                )
            }
            Result.success()
        }.getOrElse {
            pending.forEach { expense ->
                database.expenseDao().setSyncStatus(expense.id, SyncStatus.Failed, expense.serverVersion)
            }
            Result.retry()
        }
    }
}
