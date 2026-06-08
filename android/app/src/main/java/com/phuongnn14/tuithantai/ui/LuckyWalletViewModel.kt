package com.phuongnn14.tuithantai.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phuongnn14.tuithantai.data.*
import com.phuongnn14.tuithantai.ml.ExpenseAnalyzer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val expenseAnalyzer = ExpenseAnalyzer()

    init {
        loadSettings()
        prepopulateDataIfNeeded()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSetting("lang")?.let {
                runCatching { _currentLanguage.value = AppLanguage.valueOf(it) }
            }
            repository.getSetting("logged_in")?.let { _isLoggedIn.value = it.toBoolean() }
            repository.getSetting("username")?.let { _userName.value = it }
            repository.getSetting("email")?.let { _userEmail.value = it }
            repository.getSetting("server_url")?.let { _serverUrl.value = it }
        }
    }

    private fun prepopulateDataIfNeeded() {
        viewModelScope.launch {
            repository.categories.collect { list ->
                if (list.isEmpty()) {
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
            }
        }
        viewModelScope.launch {
            repository.accounts.collect { list ->
                if (list.isEmpty()) {
                    listOf(
                        AccountEntity("Tiền mặt", 0.0),
                        AccountEntity("Tài khoản ngân hàng", 0.0),
                        AccountEntity("Ví MoMo", 0.0)
                    ).forEach { repository.insertAccount(it) }
                }
            }
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
                type = if (suggestion.ocrEngine.contains("income")) "INCOME" else "EXPENSE"
            )
        } catch (e: Exception) {
            OcrResult(amount = 0.0, currency = "VND", title = "", category = "Khác")
        }
    }

    private fun mapCategoryId(categoryId: String): String = when (categoryId) {
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
