package com.phuongnn14.tuithantai

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.phuongnn14.tuithantai.data.CategoryEntity
import com.phuongnn14.tuithantai.data.CategoryTotal
import com.phuongnn14.tuithantai.data.ExpenseEntity
import com.phuongnn14.tuithantai.data.ExpenseRepository
import com.phuongnn14.tuithantai.data.SyncStatus
import com.phuongnn14.tuithantai.ml.ExpenseAnalyzer
import com.phuongnn14.tuithantai.ml.ExpenseSuggestion
import com.phuongnn14.tuithantai.settings.SettingsStore
import com.phuongnn14.tuithantai.settings.UserSettings
import com.phuongnn14.tuithantai.sync.BackendClient
import com.phuongnn14.tuithantai.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TuiThanTaiApp(viewModel)
        }
    }
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TuiThanTaiApp
    private val repository = ExpenseRepository(app.database)
    private val settingsStore = SettingsStore(application)
    private val analyzer = ExpenseAnalyzer()

    val categories: StateFlow<List<CategoryEntity>> = repository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val monthlyTotal: StateFlow<Long> = repository.monthlyTotal().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L
    )
    val categoryTotals: StateFlow<List<CategoryTotal>> = repository.categoryTotals().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings()
    )

    init {
        viewModelScope.launch { repository.ensureSeedData() }
    }

    suspend fun analyzePhoto(context: Context, uri: Uri): ExpenseSuggestion {
        return analyzer.analyze(context, uri, categories.value)
    }

    fun saveExpense(draft: ExpenseDraft) {
        viewModelScope.launch {
            repository.saveExpense(
                title = draft.title,
                amount = draft.amountText.onlyDigits().toLongOrNull() ?: 0,
                categoryId = draft.categoryId,
                wallet = draft.wallet,
                note = draft.note,
                receiptPath = draft.photoUri?.toString(),
                ocrText = draft.ocrText
            )
            enqueueSync()
        }
    }

    fun login(email: String, password: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                BackendClient(settings.value.backendUrl).login(email, password)
            }.onSuccess {
                settingsStore.setAuth(it.accessToken, it.refreshToken, it.email)
                enqueueSync()
                onResult("Signed in")
            }.onFailure {
                onResult(it.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, name: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                BackendClient(settings.value.backendUrl).register(email, password, name)
            }.onSuccess {
                settingsStore.setAuth(it.accessToken, it.refreshToken, it.email)
                enqueueSync()
                onResult("Registered")
            }.onFailure {
                onResult(it.message ?: "Register failed")
            }
        }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch { settingsStore.setLanguage(value) }
    }

    fun setBackendUrl(value: String) {
        viewModelScope.launch { settingsStore.setBackendUrl(value) }
    }

    fun logout() {
        viewModelScope.launch { settingsStore.clearAuth() }
    }

    fun setOnboardingDone(value: Boolean = true) {
        viewModelScope.launch { settingsStore.setOnboardingDone(value) }
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork("expense-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

data class ExpenseDraft(
    val photoUri: Uri? = null,
    val title: String = "",
    val amountText: String = "",
    val categoryId: String = "other",
    val wallet: String = "Vi ca nhan",
    val note: String = "",
    val ocrText: String = "",
    val isAnalyzing: Boolean = false
)

enum class Screen {
    Onboarding,
    Auth,
    Home,
    Transactions,
    Reports,
    Sync,
    Tools,
    Wallets,
    Budgets,
    Categories,
    Recurring,
    SplitBill,
    Settings,
    Camera,
    Review
}

@Composable
fun TuiThanTaiApp(viewModel: ExpenseViewModel) {
    val settings by viewModel.settings.collectAsState()
    LaunchedEffect(settings.language) {
        val tags = when (settings.language) {
            "vi" -> "vi"
            "en" -> "en"
            else -> ""
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0F8F5F),
            secondary = Color(0xFFF5B84B),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF7FAF8)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF7FAF8)) {
            AppShell(viewModel, settings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: ExpenseViewModel, settings: UserSettings) {
    var screen by remember(settings.onboardingDone) {
        mutableStateOf(if (settings.onboardingDone) Screen.Home else Screen.Onboarding)
    }
    var draft by remember { mutableStateOf(ExpenseDraft()) }

    val categories by viewModel.categories.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()

    val mainTabs = setOf(Screen.Home, Screen.Transactions, Screen.Reports, Screen.Tools, Screen.Settings)
    val toolScreens = setOf(Screen.Wallets, Screen.Budgets, Screen.Categories, Screen.Recurring, Screen.SplitBill, Screen.Sync)
    val showBottom = screen in mainTabs
    val showTop = screen in mainTabs || screen in toolScreens || screen == Screen.Auth
    Scaffold(
        topBar = {
            if (showTop) {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle(screen), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (screen in toolScreens || screen == Screen.Auth) {
                            IconButton(onClick = { screen = if (screen == Screen.Auth) Screen.Home else Screen.Tools }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF7FAF8))
                )
            }
        },
        bottomBar = {
            if (showBottom) BottomNav(screen) { screen = it }
        },
        floatingActionButton = {
            if (screen == Screen.Home || screen == Screen.Transactions) {
                ExtendedFloatingActionButton(
                    onClick = { screen = Screen.Camera },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text(stringResource(R.string.capture)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.Onboarding -> OnboardingScreen(
                    onStart = {
                        viewModel.setOnboardingDone(true)
                        screen = Screen.Home
                    },
                    onAuth = {
                        viewModel.setOnboardingDone(true)
                        screen = Screen.Auth
                    }
                )
                Screen.Auth -> AuthScreen(viewModel, settings, onDone = { screen = Screen.Home })
                Screen.Home -> HomeScreen(
                    categories = categories,
                    expenses = expenses,
                    monthlyTotal = monthlyTotal,
                    settings = settings,
                    onManual = {
                        draft = ExpenseDraft()
                        screen = Screen.Review
                    },
                    onViewReports = { screen = Screen.Reports },
                    onViewTransactions = { screen = Screen.Transactions }
                )
                Screen.Transactions -> TransactionsScreen(categories, expenses, settings)
                Screen.Reports -> ReportsScreen(categories, categoryTotals, monthlyTotal, settings)
                Screen.Sync -> SyncScreen(viewModel, settings)
                Screen.Tools -> ToolsScreen(
                    onWallets = { screen = Screen.Wallets },
                    onBudgets = { screen = Screen.Budgets },
                    onCategories = { screen = Screen.Categories },
                    onRecurring = { screen = Screen.Recurring },
                    onSplitBill = { screen = Screen.SplitBill },
                    onSync = { screen = Screen.Sync },
                    onCapture = { screen = Screen.Camera }
                )
                Screen.Wallets -> WalletsScreen()
                Screen.Budgets -> BudgetsScreen(monthlyTotal)
                Screen.Categories -> CategoryManagerScreen(categories, settings)
                Screen.Recurring -> RecurringScreen()
                Screen.SplitBill -> SplitBillScreen()
                Screen.Settings -> SettingsScreen(viewModel, settings, onAuth = { screen = Screen.Auth }, onSync = { screen = Screen.Sync })
                Screen.Camera -> CameraScreen(
                    onClose = { screen = Screen.Home },
                    onCaptured = { uri ->
                        draft = ExpenseDraft(photoUri = uri, isAnalyzing = true)
                        screen = Screen.Review
                    },
                    onManual = {
                        draft = ExpenseDraft()
                        screen = Screen.Review
                    }
                )
                Screen.Review -> ReviewScreen(
                    draft = draft,
                    categories = categories,
                    settings = settings,
                    onDraftChange = { draft = it },
                    onClose = { screen = Screen.Home },
                    onAnalyze = { uri, context ->
                        val suggestion = viewModel.analyzePhoto(context, uri)
                        draft = draft.copy(
                            title = suggestion.title.ifBlank { draft.title },
                            amountText = if (suggestion.amount > 0) suggestion.amount.toString() else draft.amountText,
                            categoryId = suggestion.categoryId,
                            ocrText = suggestion.ocrText,
                            isAnalyzing = false
                        )
                    },
                    onSave = {
                        viewModel.saveExpense(draft)
                        screen = Screen.Home
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = current == Screen.Home,
            onClick = { onSelect(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home)) }
        )
        NavigationBarItem(
            selected = current == Screen.Transactions,
            onClick = { onSelect(Screen.Transactions) },
            icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
            label = { Text("Sổ") }
        )
        NavigationBarItem(
            selected = current == Screen.Reports,
            onClick = { onSelect(Screen.Reports) },
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text(stringResource(R.string.reports)) }
        )
        NavigationBarItem(
            selected = current == Screen.Tools,
            onClick = { onSelect(Screen.Tools) },
            icon = { Icon(Icons.Default.Tune, contentDescription = null) },
            label = { Text("Công cụ") }
        )
        NavigationBarItem(
            selected = current == Screen.Settings,
            onClick = { onSelect(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) }
        )
    }
}

@Composable
fun OnboardingScreen(onStart: () -> Unit, onAuth: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7FAF8))
    ) {
        item {
            Text("Túi thần tài", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Chụp hóa đơn, nhận gợi ý chi tiêu, lưu lại ngay cả khi không có mạng.", color = Color(0xFF65716B))
        }
        item { OnboardingCard(Icons.Default.Receipt, "Scan hóa đơn", "OCR đọc tổng tiền, tiêu đề và nội dung bill để bạn xác nhận nhanh.") }
        item { OnboardingCard(Icons.Default.CameraAlt, "Chụp đồ vật", "ML Kit nhận diện ảnh như bánh mì, cafe, đồ dùng để gợi ý hạng mục.") }
        item { OnboardingCard(Icons.Default.BarChart, "Báo cáo trực quan", "Xem chi tiêu theo tháng, hạng mục, ví và trạng thái đồng bộ.") }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Trải nghiệm ngay", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onAuth, modifier = Modifier.fillMaxWidth()) {
                Text("Đăng nhập / Đăng ký")
            }
        }
    }
}

@Composable
fun OnboardingCard(icon: ImageVector, title: String, body: String) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFE7F5EE), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF0F8F5F))
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: ExpenseViewModel, settings: UserSettings, onDone: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tài khoản", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Đăng nhập để đồng bộ qua backend trên máy tính của bạn.", color = Color(0xFF65716B))
                    if (settings.userEmail.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Đang đăng nhập: ${settings.userEmail}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { viewModel.login(email, password) { message = it; if (it == "Signed in") onDone() } }, modifier = Modifier.weight(1f)) { Text("Đăng nhập") }
                Button(onClick = { viewModel.register(email, password, name.ifBlank { email }) { message = it; if (it == "Registered") onDone() } }, modifier = Modifier.weight(1f)) { Text("Đăng ký") }
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Bỏ qua, dùng offline") }
        }
        if (message.isNotBlank()) item { Text(message, color = Color(0xFF65716B)) }
    }
}

@Composable
fun TransactionsScreen(categories: List<CategoryEntity>, expenses: List<ExpenseEntity>, settings: UserSettings) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = true, onClick = {}, label = { Text("Tất cả") })
                FilterChip(selected = false, onClick = {}, label = { Text("Chi tiêu") })
                FilterChip(selected = false, onClick = {}, label = { Text("Chờ sync") })
            }
        }
        if (expenses.isEmpty()) {
            item { EmptyState() }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(expense, categories, settings)
            }
        }
    }
}

@Composable
fun ToolsScreen(
    onWallets: () -> Unit,
    onBudgets: () -> Unit,
    onCategories: () -> Unit,
    onRecurring: () -> Unit,
    onSplitBill: () -> Unit,
    onSync: () -> Unit,
    onCapture: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Luồng đầy đủ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Các module xuất hiện trong video, rút gọn cho bản đầu.", color = Color(0xFF65716B))
        }
        item { ToolTile(Icons.Default.CameraAlt, "Scan / AI ghi chi", "Chụp bill hoặc đồ vật để tạo khoản chi.", onCapture) }
        item { ToolTile(Icons.Default.AccountBalanceWallet, "Ví & tài khoản", "Ví cá nhân, Momo, ngân hàng, tiền mặt.", onWallets) }
        item { ToolTile(Icons.Default.Savings, "Ngân sách", "Theo dõi hạn mức ăn uống, mua sắm, đi lại.", onBudgets) }
        item { ToolTile(Icons.Default.Category, "Danh mục", "Danh mục lấy từ Cap money và Sổ thu chi MISA.", onCategories) }
        item { ToolTile(Icons.Default.Repeat, "Giao dịch định kỳ", "Tiền nhà, Internet, điện nước.", onRecurring) }
        item { ToolTile(Icons.Default.Groups, "Chia tiền", "Ghi nhận khoản chia với bạn bè.", onSplitBill) }
        item { ToolTile(Icons.Default.CloudUpload, "Đồng bộ", "Đăng nhập và đồng bộ với backend PC.", onSync) }
    }
}

@Composable
fun ToolTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFE7F5EE), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF0F8F5F))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF65716B))
        }
    }
}

@Composable
fun WalletsScreen() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WalletCard("Ví cá nhân", "Tiền mặt", 2450000, Icons.Default.AccountBalanceWallet) }
        item { WalletCard("Momo", "Ví điện tử", 520000, Icons.Default.CreditCard) }
        item { WalletCard("Bank", "Tài khoản ngân hàng", 3000000, Icons.Default.Paid) }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Thêm tài khoản", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun WalletCard(name: String, type: String, balance: Long, icon: ImageVector) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0F8F5F), modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text(type, color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
            }
            Text(formatVnd(balance), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BudgetsScreen(monthlyTotal: Long) {
    val budgets = listOf(
        Triple("Ăn uống", 800000L, 620000L),
        Triple("Mua sắm", 600000L, 340000L),
        Triple("Đi lại", 300000L, 185000L)
    )
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SummaryCard("Tổng chi theo ngân sách", formatVnd(monthlyTotal))
        }
        items(budgets) { item ->
            val progress = (item.third.toFloat() / item.second.toFloat()).coerceIn(0f, 1f)
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.first, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${formatVnd(item.third)} / ${formatVnd(item.second)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryManagerScreen(categories: List<CategoryEntity>, settings: UserSettings) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Danh mục mặc định", fontWeight = FontWeight.Bold)
            Text("Có thể mở rộng thành thêm/sửa/xóa ở bản production tiếp theo.", color = Color(0xFF65716B))
        }
        items(categories, key = { it.id }) { category ->
            ToolTile(Icons.Default.Category, category.nameFor(settings), category.keywords, onClick = {})
        }
    }
}

@Composable
fun RecurringScreen() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { RecurringCard("Internet", "Hàng tháng · ngày 05", 220000) }
        item { RecurringCard("Tiền nhà", "Hàng tháng · ngày 01", 3500000) }
        item { RecurringCard("Gói điện thoại", "Hàng tháng · ngày 12", 90000) }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Thêm giao dịch định kỳ", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun RecurringCard(title: String, subtitle: String, amount: Long) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF0F8F5F))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
            }
            Text(formatVnd(amount), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SplitBillScreen() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Chia tiền bữa ăn", fontWeight = FontWeight.Bold)
                    Text("Tổng: ${formatVnd(385250)} · 3 người", color = Color(0xFF65716B))
                    Spacer(Modifier.height(12.dp))
                    Text("Mỗi người: ${formatVnd(128417)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        item { SplitMember("Bạn", 128417, true) }
        item { SplitMember("Minh", 128417, false) }
        item { SplitMember("Linh", 128417, false) }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Groups, contentDescription = null)
                Text("Tạo khoản chia mới", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun SplitMember(name: String, amount: Long, paid: Boolean) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(formatVnd(amount), modifier = Modifier.padding(end = 10.dp))
            if (paid) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F8F5F))
        }
    }
}

@Composable
fun HomeScreen(
    categories: List<CategoryEntity>,
    expenses: List<ExpenseEntity>,
    monthlyTotal: Long,
    settings: UserSettings,
    onManual: () -> Unit,
    onViewReports: () -> Unit,
    onViewTransactions: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.hello), color = Color(0xFF65716B))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            SummaryCard(label = stringResource(R.string.month_total), amount = formatVnd(monthlyTotal))
        }
        item {
            CalendarStrip()
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onManual, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.manual_entry), modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(onClick = onViewReports, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.BarChart, contentDescription = null)
                    Text(stringResource(R.string.reports), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (expenses.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(expenses.take(20), key = { it.id }) { expense ->
                ExpenseRow(expense, categories, settings)
            }
            item {
                TextButton(onClick = onViewTransactions, modifier = Modifier.fillMaxWidth()) {
                    Text("Xem toàn bộ giao dịch")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun CalendarStrip() {
    val days = listOf("T2" to "1", "T3" to "2", "T4" to "3", "T5" to "4", "T6" to "5", "T7" to "6", "CN" to "7")
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tháng 6, 2026", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("VND", color = Color(0xFF0F8F5F), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.first, style = MaterialTheme.typography.labelSmall, color = Color(0xFF65716B))
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(if (index == 2) Color(0xFF0F8F5F) else Color(0xFFE8EEE9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.second, color = if (index == 2) Color.White else Color(0xFF1D2320), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, amount: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F8F5F)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.82f))
            Text(amount, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun EmptyState() {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF0F8F5F), modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.empty_state), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ExpenseRow(expense: ExpenseEntity, categories: List<CategoryEntity>, settings: UserSettings) {
    val category = categories.firstOrNull { it.id == expense.categoryId }
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(Color(0xFFE7F5EE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(category?.nameFor(settings)?.take(1) ?: "K", color = Color(0xFF0F8F5F), fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(expense.title, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(category?.nameFor(settings), if (expense.syncStatus != SyncStatus.Synced) stringResource(R.string.pending_sync) else null).joinToString(" · "),
                    color = Color(0xFF65716B),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(formatVnd(expense.amount), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ReportsScreen(
    categories: List<CategoryEntity>,
    totals: List<CategoryTotal>,
    monthlyTotal: Long,
    settings: UserSettings
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryCard(label = stringResource(R.string.month_total), amount = formatVnd(monthlyTotal)) }
        items(totals, key = { it.categoryId }) { total ->
            val category = categories.firstOrNull { it.id == total.categoryId }
            val percent = if (monthlyTotal == 0L) 0f else total.total.toFloat() / monthlyTotal.toFloat()
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category?.nameFor(settings) ?: total.categoryId, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(formatVnd(total.total), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFE8EEE9), RoundedCornerShape(12.dp))) {
                        Box(Modifier.fillMaxWidth(percent.coerceIn(0f, 1f)).height(10.dp).background(Color(0xFF0F8F5F), RoundedCornerShape(12.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun SyncScreen(viewModel: ExpenseViewModel, settings: UserSettings) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var backendUrl by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = backendUrl,
                onValueChange = {
                    backendUrl = it
                    viewModel.setBackendUrl(it)
                },
                label = { Text(stringResource(R.string.backend_url)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (settings.accessToken.isBlank()) {
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.login(email, password) { message = it } }) { Text("Login") }
                    Button(onClick = { viewModel.register(email, password, name.ifBlank { email }) { message = it } }) { Text("Register") }
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(settings.userEmail, fontWeight = FontWeight.Bold)
                        Text("Ready to sync with ${settings.backendUrl}", color = Color(0xFF65716B))
                        TextButton(onClick = viewModel::logout) { Text("Logout") }
                    }
                }
            }
        }
        if (message.isNotBlank()) item { Text(message, color = Color(0xFF65716B)) }
    }
}

@Composable
fun SettingsScreen(viewModel: ExpenseViewModel, settings: UserSettings, onAuth: () -> Unit, onSync: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tài khoản & đồng bộ", fontWeight = FontWeight.Bold)
                    Text(
                        if (settings.userEmail.isBlank()) "Chưa đăng nhập" else settings.userEmail,
                        color = Color(0xFF65716B)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Button(onClick = onAuth) { Text(if (settings.userEmail.isBlank()) "Đăng nhập" else "Tài khoản") }
                        TextButton(onClick = onSync) { Text(stringResource(R.string.sync)) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            LanguageOption(stringResource(R.string.system_default), settings.language == "system") { viewModel.setLanguage("system") }
            LanguageOption(stringResource(R.string.vietnamese), settings.language == "vi") { viewModel.setLanguage("vi") }
            LanguageOption(stringResource(R.string.english), settings.language == "en") { viewModel.setLanguage("en") }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cấu hình", fontWeight = FontWeight.Bold)
                    Text("Backend: ${settings.backendUrl}", color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
                    Text("Tiền tệ: VND", color = Color(0xFF65716B), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE7F5EE) else Color.White),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F8F5F))
        }
    }
}

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onManual: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF111715))) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
                Text(stringResource(R.string.capture), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onManual) { Text(stringResource(R.string.manual_entry), color = Color.White) }
            }
            if (permissionGranted) {
                CameraPreview(imageCapture, Modifier.weight(1f).fillMaxWidth().padding(vertical = 14.dp))
                Button(
                    onClick = {
                        val file = context.createReceiptFile()
                        val output = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onCaptured(Uri.fromFile(file))
                                }

                                override fun onError(exception: ImageCaptureException) = Unit
                            }
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(76.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F8F5F)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.camera_permission), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            previewView
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    draft: ExpenseDraft,
    categories: List<CategoryEntity>,
    settings: UserSettings,
    onDraftChange: (ExpenseDraft) -> Unit,
    onClose: () -> Unit,
    onAnalyze: suspend (Uri, Context) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(draft.photoUri) {
        val uri = draft.photoUri
        if (uri != null && draft.isAnalyzing) {
            onAnalyze(uri, context)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null) }
                Text(stringResource(R.string.review_expense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (draft.isAnalyzing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        item {
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = { onDraftChange(draft.copy(amountText = it.onlyDigits())) },
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(stringResource(R.string.category), fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = draft.categoryId == category.id,
                        onClick = { onDraftChange(draft.copy(categoryId = category.id)) },
                        label = { Text(category.nameFor(settings)) }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = draft.wallet,
                onValueChange = { onDraftChange(draft.copy(wallet = it)) },
                label = { Text(stringResource(R.string.wallet)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = draft.note,
                onValueChange = { onDraftChange(draft.copy(note = it)) },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (draft.ocrText.isNotBlank()) {
            item {
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(draft.ocrText.take(500), modifier = Modifier.padding(12.dp), color = Color(0xFF65716B))
                }
            }
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        onSave()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.save_expense), fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun CategoryEntity.nameFor(settings: UserSettings): String {
    return when (settings.language) {
        "en" -> nameEn
        "vi" -> nameVi
        else -> if (Locale.getDefault().language == "en") nameEn else nameVi
    }
}

@Composable
fun screenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Onboarding -> stringResource(R.string.app_name)
        Screen.Auth -> "Tài khoản"
        Screen.Home -> stringResource(R.string.app_name)
        Screen.Transactions -> "Sổ giao dịch"
        Screen.Reports -> stringResource(R.string.reports)
        Screen.Sync -> stringResource(R.string.sync)
        Screen.Tools -> "Công cụ"
        Screen.Wallets -> "Ví & tài khoản"
        Screen.Budgets -> "Ngân sách"
        Screen.Categories -> stringResource(R.string.category)
        Screen.Recurring -> "Định kỳ"
        Screen.SplitBill -> "Chia tiền"
        Screen.Settings -> stringResource(R.string.settings)
        Screen.Camera -> stringResource(R.string.capture)
        Screen.Review -> stringResource(R.string.review_expense)
    }
}

fun formatVnd(value: Long): String {
    return NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value) + " d"
}

fun String.onlyDigits(): String = filter { it.isDigit() }

fun Context.createReceiptFile(): File {
    val dir = File(filesDir, "receipts")
    dir.mkdirs()
    return File(dir, "${UUID.randomUUID()}.jpg")
}
