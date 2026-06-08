package com.phuongnn14.tuithantai.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker: tự động sao lưu lên Google Drive lúc 8:00 PM hàng ngày.
 * Chỉ chạy khi user đã đăng nhập Google và có kết nối mạng.
 */
class BackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "tuithantai_daily_backup"
        private const val TAG = "BackupWorker"

        /**
         * Lên lịch backup 8:00 PM hàng ngày.
         * Gọi sau khi user bật "Tự động sao lưu".
         */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                // Nếu đã qua 8 PM hôm nay → lên lịch cho 8 PM ngày mai
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "Đã lên lịch backup 8 PM hàng ngày (delay ${initialDelay / 60000} phút)")
        }

        /** Hủy lịch backup tự động. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Đã hủy backup tự động")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Bắt đầu backup tự động...")

        // Lấy access token từ Google Sign-In đang active
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.w(TAG, "Chưa đăng nhập Google, bỏ qua backup")
            return Result.success() // không phải failure — chỉ skip
        }

        // Lấy token (silent)
        return try {
            val tokenResult = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(context)
            // Token được lưu trong ViewModel khi sign-in thành công
            // Worker đọc từ SharedPreferences
            val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("drive_access_token", null)

            if (token.isNullOrBlank()) {
                Log.w(TAG, "Không có access token, bỏ qua backup")
                return Result.success()
            }

            val result = DriveBackupManager.backup(context, token)
            if (result.success) {
                prefs.edit().putLong("last_backup_time", System.currentTimeMillis()).apply()
                Log.d(TAG, "Auto backup thành công: ${result.message}")
                Result.success()
            } else {
                Log.w(TAG, "Auto backup thất bại: ${result.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup lỗi: ${e.message}")
            Result.retry()
        }
    }
}
