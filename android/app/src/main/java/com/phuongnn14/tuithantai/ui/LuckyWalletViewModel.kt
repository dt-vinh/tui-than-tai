package com.phuongnn14.tuithantai.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phuongnn14.tuithantai.backup.BackupWorker
import com.phuongnn14.tuithantai.backup.DriveBackupManager
import com.phuongnn14.tuithantai.data.*
import com.phuongnn14.tuithantai.ml.ExpenseAnalyzer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LuckyWalletViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = LuckyWalletRepository(db)

    val transactionsFlow = repository.transactions
    val accountsFlow = repository.accounts
    val budgetsFlow = repository.budgets
    val categoriesFlow = repository.categories
    val recurringTransactionsFlow = repository.recurringTransactions

    private val _currentLanguage = MutableStateFlow(AppLanguage.VIETNAMESE)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://localhost:8080")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncMessage = _syncMessage.asSharedFlow()

    // ── Google Sign-In + Drive backup state ──────────────────────────────────
    /** Access token sau khi Google Sign-In thành công */
    private val _driveAccessToken = MutableStateFlow<String?>(null)
    val driveAccessToken: StateFlow<String?> = _driveAccessToken.asStateFlow()

    /** Trạng thái backup đang chạy */
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    /** Thông báo kết quả backup gần nhất */
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    /** Thời gian backup gần nhất (Long timestamp, 0 = chưa backup) */
    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    /** Auto backup đang bật không */
    private val _autoBackupEnabled = MutableStateFlow(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val expenseAnalyzer = ExpenseAnalyzer()

    /** Splash screen giữ nguyên đến khi cờ này = true */
    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady.asStateFlow()

    init {
        viewModelScope.launch {
            // Chạy song song: load settings + prepopulate data
            try {
                loadSettings()
            } finally {
                _isAppReady.value = true
            }
            prepopulateDataIfNeeded()
        }
    }

    private suspend fun loadSettings() {
        // 1 query duy nhất thay vì 6 query riêng lẻ
        val settings = repository.getAllSettings()
        settings["lang"]?.let {
            runCatching { _currentLanguage.value = AppLanguage.valueOf(it) }
        }
        _isLoggedIn.value       = settings["logged_in"]?.toBoolean() ?: false
        _userName.value          = settings["username"] ?: ""
        _userEmail.value         = settings["email"] ?: ""
        _serverUrl.value         = settings["server_url"] ?: "http://localhost:8080"
        _autoBackupEnabled.value = settings["auto_backup"]?.toBoolean() ?: false

        // SharedPreferences (không phải DB — đọc nhanh)
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        _lastBackupTime.value = prefs.getLong("last_backup_time", 0L)
        prefs.getString("drive_access_token", null)?.let { _driveAccessToken.value = it }
    }

    private suspend fun prepopulateDataIfNeeded() {
        // Dùng .first() thay vì .collect {} — chỉ đọc 1 lần, không giữ coroutine chạy mãi
        val categories = repository.categories.first()
        if (categories.isEmpty()) {
            listOf(
                CategoryEntity("Ăn uống", "EXPENSE", true),
                CategoryEntity("Di chuyển", "EXPENSE", true),
                CategoryEntity("Mua sắm", "EXPENSE", true),
                CategoryEntity("Giải trí", "EXPENSE", true),
                CategoryEntity("Nhà ở", "EXPENSE", true),
                CategoryEntity("Y tế", "EXPENSE", true),
                CategoryEntity("Khác", "EXPENSE", true),
                CategoryEntity("Lương", "INCOME", true),
                CategoryEntity("Thưởng", "INCOME", true),
                CategoryEntity("Đầu tư", "INCOME", true),
                CategoryEntity("Quà tặng", "INCOME", true)
            ).forEach { repository.insertCategory(it) }
        }
        val accounts = repository.accounts.first()
        if (accounts.isEmpty()) {
            listOf(
                AccountEntity("Tiền mặt", 0.0),
                AccountEntity("Tài khoản ngân hàng", 0.0),
                AccountEntity("Ví MoMo", 0.0)
            ).forEach { repository.insertAccount(it) }
        }
    }

    // OCR: analyze a real image from camera or gallery
    suspend fun analyzeImage(context: Context, uri: Uri): OcrResult {
        return try {
            val suggestion = expenseAnalyzer.analyze(context, uri, emptyList())
            OcrResult(
                amount = suggestion.amount.toDouble(),
                currency = "VND",
                title = suggestion.title,
                category = mapCategoryId(suggestion.categoryId),
                merchantName = suggestion.title,
                items = emptyList(),
                type = if (suggestion.ocrEngine.contains("income")) "INCOME" else "EXPENSE",
                needsReview = suggestion.needsReview,
                documentType = if (suggestion.amount == 0L && suggestion.labels.isNotEmpty()) "non_receipt" else "receipt"
            )
        } catch (e: Exception) {
            OcrResult(amount = 0.0, currency = "VND", title = "", category = "Khác")
        }
    }

    private fun mapCategoryId(categoryId: String): String = when (categoryId) {
        "food" -> "\u0102n u\u1ed1ng"
        "travel" -> "Di chuy\u1ec3n"
        "food_and_drink", "coffee" -> "Ăn uống"
        "transport" -> "Di chuyển"
        "shopping" -> "Mua sắm"
        "bills", "utilities" -> "Nhà ở"
        "health" -> "Y tế"
        "entertainment" -> "Giải trí"
        "income", "salary" -> "Lương"
        else -> "Khác"
    }

    fun changeLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            _currentLanguage.value = lang
            repository.saveSetting("lang", lang.name)
        }
    }

    fun login(name: String, email: String) {
        viewModelScope.launch {
            _isLoggedIn.value = true
            _userName.value = name
            _userEmail.value = email
            repository.saveSetting("logged_in", "true")
            repository.saveSetting("username", name)
            repository.saveSetting("email", email)
            triggerSync()
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
            _userName.value = ""
            _userEmail.value = ""
            repository.saveSetting("logged_in", "false")
            repository.saveSetting("username", "")
            repository.saveSetting("email", "")
        }
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            _serverUrl.value = url
            repository.saveSetting("server_url", url)
        }
    }

    // ── Google Sign-In callbacks ─────────────────────────────────────────────

    /**
     * Gọi sau khi Google Sign-In thành công.
     * Tự động lấy Drive access token thật qua GoogleAuthUtil.
     * Nếu user chưa cấp quyền Drive → gọi onDrivePermissionNeeded với intent consent screen.
     */
    fun onGoogleSignInSuccess(
        context: Context,
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        onDrivePermissionNeeded: (android.content.Intent) -> Unit = {}
    ) {
        viewModelScope.launch {
            // Lưu thông tin user ngay
            _isLoggedIn.value = true
            _userName.value = account.displayName ?: ""
            _userEmail.value = account.email ?: ""
            repository.saveSetting("logged_in", "true")
            repository.saveSetting("username", account.displayName ?: "")
            repository.saveSetting("email", account.email ?: "")

            // Lấy Drive OAuth access token thật (chạy trên IO)
            fetchDriveToken(context, account, onDrivePermissionNeeded)
        }
    }

    private suspend fun fetchDriveToken(
        context: Context,
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        onDrivePermissionNeeded: (android.content.Intent) -> Unit
    ) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val driveScope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context, account.account!!, driveScope
                )
                _driveAccessToken.value = token
                // Lưu vào SharedPreferences cho BackupWorker
                context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                    .edit().putString("drive_access_token", token).apply()
                android.util.Log.d("DriveAuth", "Drive access token OK, length=${token.length}, prefix=${token.take(20)}")
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                // Cần user cấp quyền Drive lần đầu
                android.util.Log.w("DriveAuth", "Need Drive permission: ${e.message}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    e.intent?.let { onDrivePermissionNeeded(it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("DriveAuth", "Token error: ${e.message}")
            }
        }
    }

    /** Gọi lại sau khi user cấp quyền Drive xong */
    fun refreshDriveToken(context: Context) {
        viewModelScope.launch {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(context) ?: return@launch
            fetchDriveToken(context, account) {}
        }
    }

    fun onGoogleSignOut() {
        viewModelScope.launch {
            _driveAccessToken.value = null
            _isLoggedIn.value = false
            _userName.value = ""
            _userEmail.value = ""
            // Hủy auto backup khi sign out
            if (_autoBackupEnabled.value) {
                BackupWorker.cancel(getApplication())
                _autoBackupEnabled.value = false
                repository.saveSetting("auto_backup", "false")
            }
            getApplication<android.app.Application>()
                .getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                .edit().remove("drive_access_token").apply()
            repository.saveSetting("logged_in", "false")
            repository.saveSetting("username", "")
            repository.saveSetting("email", "")
        }
    }

    // ── Backup / Restore ─────────────────────────────────────────────────────

    /** Luôn fetch fresh token trước khi backup/restore để tránh dùng token hết hạn */
    private suspend fun getFreshDriveToken(context: Context): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context) ?: return@withContext null
                val driveScope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
                // Invalidate cached token cũ (nếu có) để buộc fetch mới
                val oldToken = _driveAccessToken.value
                if (oldToken != null) {
                    try {
                        com.google.android.gms.auth.GoogleAuthUtil.invalidateToken(context, oldToken)
                    } catch (_: Exception) {}
                }
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context, account.account!!, driveScope
                )
                _driveAccessToken.value = token
                context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                    .edit().putString("drive_access_token", token).apply()
                android.util.Log.d("DriveAuth", "Fresh token OK, length=${token.length}, prefix=${token.take(20)}")
                token
            } catch (e: Exception) {
                android.util.Log.e("DriveAuth", "getFreshToken error: ${e.message}")
                null
            }
        }
    }

    fun backupNow(context: Context) {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupMessage.value = null
            val token = getFreshDriveToken(context)
            if (token == null) {
                _isBackingUp.value = false
                _backupMessage.value = "Không lấy được token. Vui lòng đăng nhập lại."
                return@launch
            }
            val result = DriveBackupManager.backup(context, token)
            _isBackingUp.value = false
            _backupMessage.value = result.message
            if (result.success) {
                val now = System.currentTimeMillis()
                _lastBackupTime.value = now
                context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                    .edit().putLong("last_backup_time", now).apply()
            }
        }
    }

    fun restoreFromDrive(context: Context) {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupMessage.value = null
            val token = getFreshDriveToken(context)
            if (token == null) {
                _isBackingUp.value = false
                _backupMessage.value = "Không lấy được token. Vui lòng đăng nhập lại."
                return@launch
            }
            val result = DriveBackupManager.restore(context, token)
            _isBackingUp.value = false
            _backupMessage.value = result.message
        }
    }

    fun setAutoBackup(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            _autoBackupEnabled.value = enabled
            repository.saveSetting("auto_backup", enabled.toString())
            if (enabled) BackupWorker.schedule(context)
            else BackupWorker.cancel(context)
        }
    }

    fun formatBackupTime(timestamp: Long, language: AppLanguage): String {
        if (timestamp == 0L) return Localization.getString("no_backup_yet", language)
        return SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun triggerSync() {
        viewModelScope.launch {
            val unsynced = repository.getUnsyncedTransactions()
            if (unsynced.isEmpty()) {
                _syncMessage.emit("Tất cả dữ liệu đã được đồng bộ!")
                return@launch
            }
            val result = SimpleSyncManager.performSync(repository, _serverUrl.value)
            _syncMessage.emit(result)
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String, accountName: String, date: Long, note: String, imageUri: String? = null) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title, amount = amount, type = type,
                    category = category, accountName = accountName,
                    date = date, note = note, isSynced = false, imageUri = imageUri
                )
            )
            triggerSync()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            triggerSync()
        }
    }

    fun addAccount(name: String, balance: Double, currency: String = "VND") {
        viewModelScope.launch { repository.insertAccount(AccountEntity(name, balance, currency)) }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun updateAccount(oldAccount: AccountEntity, newName: String, newBalance: Double) {
        viewModelScope.launch {
            if (oldAccount.name != newName) {
                repository.insertAccount(AccountEntity(newName, newBalance, oldAccount.currency))
                repository.updateAccountReferences(oldAccount.name, newName)
                repository.deleteAccount(oldAccount)
            } else {
                repository.insertAccount(AccountEntity(oldAccount.name, newBalance, oldAccount.currency))
            }
        }
    }

    fun addBudget(name: String, categoryName: String, amount: Double, period: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(name = name, categoryName = categoryName, amount = amount, period = period, startDate = startDate, endDate = endDate))
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch { repository.deleteBudget(id) }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch { repository.insertCategory(CategoryEntity(name, type, false)) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { repository.deleteCategory(name) }
    }

    fun updateCategory(oldCategory: CategoryEntity, newName: String) {
        viewModelScope.launch {
            if (oldCategory.name != newName) {
                repository.insertCategory(CategoryEntity(newName, oldCategory.type, oldCategory.isDefault))
                repository.updateCategoryReferences(oldCategory.name, newName)
                repository.deleteCategory(oldCategory.name)
            }
        }
    }

    fun addRecurringTransaction(name: String, type: String, amount: Double, category: String, accountName: String, cycle: String, startDate: Long) {
        viewModelScope.launch {
            repository.insertRecurring(
                RecurringTransactionEntity(name = name, type = type, amount = amount, category = category, accountName = accountName, cycle = cycle, startDate = startDate)
            )
        }
    }

    fun deleteRecurringTransaction(id: Long) {
        viewModelScope.launch { repository.deleteRecurring(id) }
    }

    fun calculateSettlements(rawParticipants: String, payer: String, totalAmount: Double): SplitBillResult {
        val participants = rawParticipants.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (participants.isEmpty() || totalAmount <= 0.0) return SplitBillResult(emptyMap(), emptyList())

        val share = totalAmount / participants.size
        val balances = participants.associateWith { name ->
            val paid = if (name.equals(payer, ignoreCase = true)) totalAmount else 0.0
            paid - share
        }

        val creditors = balances.filter { it.value > 0.0 }.map { Pair(it.key, it.value) }.toMutableList()
        val debtors = balances.filter { it.value < 0.0 }.map { Pair(it.key, -it.value) }.toMutableList()
        val suggestions = mutableListOf<SettlementSuggestion>()
        var cIdx = 0; var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]; val debtor = debtors[dIdx]
            val transfer = minOf(creditor.second, debtor.second)
            suggestions.add(SettlementSuggestion(debtor.first, creditor.first, transfer))
            creditors[cIdx] = Pair(creditor.first, creditor.second - transfer)
            debtors[dIdx] = Pair(debtor.first, debtor.second - transfer)
            if (creditors[cIdx].second < 0.01) cIdx++
            if (debtors[dIdx].second < 0.01) dIdx++
        }

        return SplitBillResult(balances, suggestions)
    }
}

data class SplitBillResult(val balances: Map<String, Double>, val suggestions: List<SettlementSuggestion>)
data class SettlementSuggestion(val debtor: String, val creditor: String, val amount: Double)

object SimpleSyncManager {
    suspend fun performSync(repository: LuckyWalletRepository, serverUrl: String): String {
        val unsynced = repository.getUnsyncedTransactions()
        if (unsynced.isEmpty()) return "Tất cả dữ liệu đã được đồng bộ!"
        return try {
            val url = java.net.URL("${serverUrl.trimEnd('/')}/api/transactions/sync")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val body = org.json.JSONArray().apply {
                unsynced.forEach { tx ->
                    put(org.json.JSONObject().apply {
                        put("id", tx.id); put("title", tx.title); put("amount", tx.amount)
                        put("type", tx.type); put("category", tx.category)
                        put("accountName", tx.accountName); put("date", tx.date); put("note", tx.note)
                    })
                }
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code in 200..299) {
                repository.markTransactionsSynced(unsynced.map { it.id })
                "Đồng bộ thành công ${unsynced.size} giao dịch!"
            } else {
                "Lỗi server: $code"
            }
        } catch (e: Exception) {
            "Không thể kết nối máy chủ. Dữ liệu đã lưu an toàn tại thiết bị."
        }
    }
}
