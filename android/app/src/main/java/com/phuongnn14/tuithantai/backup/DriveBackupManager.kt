package com.phuongnn14.tuithantai.backup

import android.content.Context
import android.util.Log
import com.phuongnn14.tuithantai.data.AppDatabase
import com.phuongnn14.tuithantai.data.TransactionEntity
import com.phuongnn14.tuithantai.data.CategoryEntity
import com.phuongnn14.tuithantai.data.AccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Backup & restore toàn bộ data lên Google Drive AppData folder.
 * - File backup: "tuithantai_backup.json" trong Drive AppData
 * - Không hiển thị trong Drive UI của user, chỉ app mới đọc được
 * - Cần access token từ Google Sign-In với scope drive.appdata
 */
object DriveBackupManager {

    private const val TAG = "DriveBackup"
    private const val BACKUP_FILENAME = "tuithantai_backup.json"
    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class BackupResult(val success: Boolean, val message: String, val timestamp: Long = System.currentTimeMillis())

    // ─── BACKUP ───────────────────────────────────────────────────────────────

    suspend fun backup(context: Context, accessToken: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val transactions = db.transactionDao().getAllTransactionsList()
            val categories = db.categoryDao().getAllCategoriesList()
            val accounts = db.accountDao().getAllAccountsList()

            val json = buildBackupJson(transactions, categories, accounts)

            // Tìm file backup cũ (nếu có) để update thay vì tạo mới
            val existingFileId = findBackupFileId(accessToken)

            val result = if (existingFileId != null) {
                updateDriveFile(accessToken, existingFileId, json)
            } else {
                createDriveFile(accessToken, json)
            }

            if (result) {
                Log.d(TAG, "Backup thành công: ${transactions.size} giao dịch")
                BackupResult(true, "Đã sao lưu ${transactions.size} giao dịch lên Google Drive")
            } else {
                BackupResult(false, "Sao lưu thất bại. Kiểm tra kết nối mạng.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup error: ${e.message}")
            BackupResult(false, "Lỗi: ${e.message}")
        }
    }

    // ─── RESTORE ──────────────────────────────────────────────────────────────

    suspend fun restore(context: Context, accessToken: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val fileId = findBackupFileId(accessToken)
                ?: return@withContext BackupResult(false, "Không tìm thấy bản sao lưu trên Drive.")

            val json = downloadDriveFile(accessToken, fileId)
                ?: return@withContext BackupResult(false, "Không tải được file sao lưu.")

            val db = AppDatabase.getDatabase(context)
            val (txCount, catCount, accCount) = restoreFromJson(db, json)

            Log.d(TAG, "Restore thành công: $txCount giao dịch, $catCount danh mục, $accCount tài khoản")
            BackupResult(true, "Đã khôi phục: $txCount giao dịch, $catCount danh mục, $accCount tài khoản")
        } catch (e: Exception) {
            Log.e(TAG, "Restore error: ${e.message}")
            BackupResult(false, "Lỗi khôi phục: ${e.message}")
        }
    }

    // ─── JSON BUILD/PARSE ─────────────────────────────────────────────────────

    private fun buildBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>
    ): String {
        val root = JSONObject().apply {
            put("version", 1)
            put("backup_time", System.currentTimeMillis())
            put("backup_date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))

            put("transactions", JSONArray().apply {
                transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("amount", tx.amount)
                        put("type", tx.type)
                        put("category", tx.category)
                        put("accountName", tx.accountName)
                        put("date", tx.date)
                        put("note", tx.note)
                        put("imageUri", tx.imageUri ?: "")
                    })
                }
            })

            put("categories", JSONArray().apply {
                categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("name", cat.name)
                        put("type", cat.type)
                        put("isDefault", cat.isDefault)
                    })
                }
            })

            put("accounts", JSONArray().apply {
                accounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("name", acc.name)
                        put("balance", acc.balance)
                        put("currency", acc.currency)
                    })
                }
            })
        }
        return root.toString(2)
    }

    private suspend fun restoreFromJson(db: AppDatabase, jsonStr: String): Triple<Int, Int, Int> {
        val root = JSONObject(jsonStr)

        // Restore transactions
        val txArray = root.getJSONArray("transactions")
        var txCount = 0
        for (i in 0 until txArray.length()) {
            val j = txArray.getJSONObject(i)
            val tx = TransactionEntity(
                title = j.getString("title"),
                amount = j.getDouble("amount"),
                type = j.getString("type"),
                category = j.getString("category"),
                accountName = j.getString("accountName"),
                date = j.getLong("date"),
                note = j.optString("note", ""),
                isSynced = true,
                imageUri = j.optString("imageUri", "").ifEmpty { null }
            )
            db.transactionDao().insertTransaction(tx)
            txCount++
        }

        // Restore categories (skip defaults đã có)
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        var catCount = 0
        for (i in 0 until catArray.length()) {
            val j = catArray.getJSONObject(i)
            val cat = CategoryEntity(
                name = j.getString("name"),
                type = j.getString("type"),
                isDefault = j.optBoolean("isDefault", false)
            )
            db.categoryDao().insertCategory(cat)
            catCount++
        }

        // Restore accounts
        val accArray = root.optJSONArray("accounts") ?: JSONArray()
        var accCount = 0
        for (i in 0 until accArray.length()) {
            val j = accArray.getJSONObject(i)
            val acc = AccountEntity(
                name = j.getString("name"),
                balance = j.getDouble("balance"),
                currency = j.optString("currency", "VND")
            )
            db.accountDao().insertAccount(acc)
            accCount++
        }

        return Triple(txCount, catCount, accCount)
    }

    // ─── DRIVE REST API ───────────────────────────────────────────────────────

    private fun findBackupFileId(accessToken: String): String? {
        val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=name='$BACKUP_FILENAME'&fields=files(id,name)"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "findBackupFile: code=${response.code} body=$body")
        if (!response.isSuccessful) return null

        val files = JSONObject(body).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createDriveFile(accessToken: String, jsonContent: String): Boolean {
        // Multipart: metadata + content
        val boundary = "boundary_tuithantai_backup"
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILENAME)
            put("parents", JSONArray().put("appDataFolder"))
        }

        val body = "--$boundary\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
            metadata.toString() + "\r\n" +
            "--$boundary\r\n" +
            "Content-Type: application/json\r\n\r\n" +
            jsonContent + "\r\n" +
            "--$boundary--"

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        Log.d(TAG, "createFile: code=${response.code} body=$responseBody")
        return response.isSuccessful
    }

    private fun updateDriveFile(accessToken: String, fileId: String, jsonContent: String): Boolean {
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
            .header("Authorization", "Bearer $accessToken")
            .patch(jsonContent.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        Log.d(TAG, "updateFile: code=${response.code} body=$body")
        return response.isSuccessful
    }

    private fun downloadDriveFile(accessToken: String, fileId: String): String? {
        val request = Request.Builder()
            .url("$DRIVE_FILES_URL/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        return response.body?.string()
    }
}
