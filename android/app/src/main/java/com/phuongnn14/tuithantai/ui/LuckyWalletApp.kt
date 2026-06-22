package com.phuongnn14.tuithantai.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phuongnn14.tuithantai.R
import com.phuongnn14.tuithantai.capture.ExpenseCaptureResult
import com.phuongnn14.tuithantai.capture.TransactionType as CaptureTransactionType
import com.phuongnn14.tuithantai.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private val BrandGreen = Color(0xFF0B6B4B)
private val BrandGreenDark = Color(0xFF063D35)
private val BrandMint = Color(0xFFE8F6ED)
private val BrandGold = Color(0xFFE5B54A)
private val BrandGoldSoft = Color(0xFFFFF1C7)
private val BrandCanvas = Color(0xFFF7F8F2)
private val BrandSurface = Color.White
private val BrandSurfaceAlt = Color(0xFFEAF0EA)
private val BrandInk = Color(0xFF17211C)
private val BrandMuted = Color(0xFF69736D)
private val BrandExpense = Color(0xFFC73D32)
private val BrandExpenseSoft = Color(0xFFFBE8E5)
private val BrandIncome = Color(0xFF278443)
private val BrandIncomeSoft = Color(0xFFE8F6ED)
private val BrandBorder = Color(0xFFDDE7DE)
private val BrandBlue = Color(0xFF1D7890)
private val BrandBlueSoft = Color(0xFFE7F0F7)
private val AppShape = RoundedCornerShape(8.dp)

// ─── Formatters ───────────────────────────────────────────────────────────────

fun formatMoney(amount: Double, currency: String, language: AppLanguage): String {
    return if (currency == "VND") {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount).replace(",", ".")
        "$formatted ₫"
    } else {
        val formatter = DecimalFormat("#,##0.00")
        val formatted = formatter.format(amount)
        if (language == AppLanguage.VIETNAMESE) {
            "${formatted.replace(".", ",").replace(",", ".")} USD"
        } else {
            "$$formatted"
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatVndInput(input: String): String {
    val cleanInput = if (input.endsWith(".0")) input.dropLast(2) else input
    val digits = cleanInput.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return try {
        val parsed = digits.toDouble()
        DecimalFormat("#,###").format(parsed).replace(",", ".")
    } catch (e: Exception) {
        digits
    }
}

fun parseVndInput(input: String): Double =
    input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

@Composable
private fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(AppShape)
    )
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = BrandSurface,
    borderColor: Color = BrandBorder,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = AppShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = BrandInk
    )
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, containerColor = containerColor, borderColor = Color.Transparent) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.72f), AppShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Black)
        }
    }
}

// ─── App Root ─────────────────────────────────────────────────────────────────

@Composable
fun LuckyWalletApp(viewModel: LuckyWalletViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.syncMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = BrandCanvas,
        bottomBar = { LuckyNavigationBar(navController = navController, language = language) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(viewModel = viewModel, navController = navController, language = language)
            }
            composable("history") { HistoryScreen(viewModel = viewModel, language = language) }
            composable("reports") { ReportsScreen(viewModel = viewModel, language = language) }
            composable("tools") { ToolsScreen(viewModel = viewModel, language = language) }
            composable("settings") { SettingsScreen(viewModel = viewModel, language = language) }
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
fun LuckyNavigationBar(navController: NavHostController, language: AppLanguage) {
    val items = listOf(
        NavigationItem("home", "home", Icons.Default.Home, Icons.Outlined.Home),
        NavigationItem("history", "history", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
        NavigationItem("reports", "reports", Icons.Default.BarChart, Icons.Outlined.BarChart),
        NavigationItem("tools", "tools", Icons.Default.GridView, Icons.Outlined.GridView),
        NavigationItem("settings", "settings", Icons.Default.Settings, Icons.Outlined.Settings)
    )
    NavigationBar(
        containerColor = BrandSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).clip(AppShape)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = Localization.getString(item.langKey, language)
                    )
                },
                label = {
                    Text(
                        text = Localization.getString(item.langKey, language),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandGreenDark,
                    selectedTextColor = BrandGreenDark,
                    indicatorColor = BrandGoldSoft,
                    unselectedIconColor = BrandMuted,
                    unselectedTextColor = BrandMuted
                )
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val langKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ─── Home Screen ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: LuckyWalletViewModel,
    navController: NavHostController,
    language: AppLanguage
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val username by viewModel.userName.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList<CategoryEntity>())

    val currency = "VND"
    val totalBalance = accounts.sumOf { it.balance }
    val incomeSum = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    var showAddTxDialog by remember { mutableStateOf<String?>(null) }
    var showScanDialog by remember { mutableStateOf(false) }
    var pendingCaptureResult by remember { mutableStateOf<ExpenseCaptureResult?>(null) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandCanvas),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val displayName = if (isLoggedIn && username.isNotEmpty()) username
                              else Localization.getString("not_logged_in", language)
            AppCard(modifier = Modifier.fillMaxWidth(), containerColor = BrandSurface) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BrandMark(size = 52.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = Localization.getString("app_name", language),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = BrandInk
                        )
                        Text(
                            text = "${Localization.getString("logged_in_as", language)} $displayName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted
                        )
                        Text(
                            text = "${Localization.getString("date", language)}: ${formatDate(System.currentTimeMillis())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandMuted
                        )
                    }
                }
            }
        }

        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = BrandGreenDark,
                borderColor = BrandGreenDark
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = Localization.getString("current_balance", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.76f)
                        )
                        Text(
                            text = formatMoney(totalBalance, currency, language),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier.size(58.dp).background(BrandGold, AppShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(34.dp))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = Localization.getString("income", language),
                    value = "+${formatMoney(incomeSum, currency, language)}",
                    icon = Icons.Default.TrendingUp,
                    accent = BrandIncome,
                    containerColor = BrandIncomeSoft,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = Localization.getString("expense", language),
                    value = "-${formatMoney(expenseSum, currency, language)}",
                    icon = Icons.Default.TrendingDown,
                    accent = BrandExpense,
                    containerColor = BrandExpenseSoft,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionHeader(Localization.getString("quick_actions", language))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showScanDialog = true },
                    shape = AppShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("scan_receipt", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { showAddTxDialog = "EXPENSE" },
                    shape = AppShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandExpense),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("add_expense", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { showAddTxDialog = "INCOME" },
                    shape = AppShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIncome),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("add_income", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        item {
            SectionHeader(Localization.getString("recent_transactions", language))
        }
        val recents = transactions.take(5)
        if (recents.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth(), containerColor = BrandSurface) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandMuted, modifier = Modifier.size(34.dp))
                        Text(
                            Localization.getString("no_transactions", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recents) { tx ->
                Box(modifier = Modifier.clickable { selectedTx = tx }) {
                    TransactionRow(tx = tx, language = language)
                }
            }
        }
    } // end LazyColumn

    FloatingActionButton(
        onClick = { showScanDialog = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = "Quét AI")
    }
    } // end Box

    showAddTxDialog?.let { type ->
        AddTransactionDialog(type = type, viewModel = viewModel, language = language, onDismiss = { showAddTxDialog = null })
    }
    if (showScanDialog) {
        MoneyScanCameraScreen(
            language = language,
            onDismiss = { showScanDialog = false },
            onResult = { result ->
                pendingCaptureResult = result
                showScanDialog = false
            }
        )
    }
    pendingCaptureResult?.let { result ->
        CaptureResultConfirmationScreen(
            result = result,
            accounts = accounts,
            categories = categories,
            language = language,
            onDismiss = { pendingCaptureResult = null },
            onRetry = {
                pendingCaptureResult = null
                showScanDialog = true
            },
            onConfirm = { title, amount, txType, category, accountName, date, note, imageUri ->
                viewModel.addTransaction(title, amount, txType, category, accountName, date, note, imageUri)
                pendingCaptureResult = null
            }
        )
    }
    selectedTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { selectedTx = null },
            title = { Text(Localization.getString("transaction_detail", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tx.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val isIncome = tx.type == "INCOME"
                    val amountText = if (isIncome) "+${formatMoney(tx.amount, "VND", language)}" else "-${formatMoney(tx.amount, "VND", language)}"
                    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                    Text(amountText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = amountColor)
                    HorizontalDivider()
                    Text("${Localization.getString("category", language)}: ${Localization.getString(tx.category, language)}")
                    Text("${Localization.getString("account", language)}: ${Localization.getString(tx.accountName, language)}")
                    Text("${Localization.getString("date", language)}: ${formatDate(tx.date)}")
                    if (tx.note.isNotEmpty()) Text("${Localization.getString("note", language)}: ${tx.note}")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTransaction(tx); selectedTx = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Localization.getString("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTx = null }) {
                    Text(Localization.getString("cancel", language))
                }
            }
        )
    }
}

// ─── Transaction Row ──────────────────────────────────────────────────────────

@Composable
fun TransactionRow(tx: TransactionEntity, language: AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = AppShape,
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandMuted)
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString(tx.category, language), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandMuted)
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString(tx.accountName, language), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val isIncome = tx.type == "INCOME"
                val amountText = if (isIncome) "+${formatMoney(tx.amount, "VND", language)}" else "-${formatMoney(tx.amount, "VND", language)}"
                val amountColor = if (isIncome) BrandIncome else BrandExpense
                Text(amountText, fontWeight = FontWeight.Bold, color = amountColor, style = MaterialTheme.typography.titleMedium)
                Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                // sync badge removed
            }
        }
    }
}

// ─── History Screen ───────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var currentFilter by remember { mutableStateOf("ALL") }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val filtered = when (currentFilter) {
        "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
        "INCOME" -> transactions.filter { it.type == "INCOME" }
        else -> transactions
    }

    Column(modifier = Modifier.fillMaxSize().background(BrandCanvas).padding(16.dp)) {
        SectionHeader(Localization.getString("history", language))
        Spacer(Modifier.height(12.dp))

        ScrollableTabRow(
            selectedTabIndex = when (currentFilter) { "EXPENSE" -> 1; "INCOME" -> 2; else -> 0 },
            edgePadding = 0.dp,
            containerColor = BrandCanvas,
            contentColor = BrandGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = currentFilter == "ALL", onClick = { currentFilter = "ALL" }) { Text(Localization.getString("all", language), modifier = Modifier.padding(12.dp)) }
            Tab(selected = currentFilter == "EXPENSE", onClick = { currentFilter = "EXPENSE" }) { Text(Localization.getString("expense", language), modifier = Modifier.padding(12.dp)) }
            Tab(selected = currentFilter == "INCOME", onClick = { currentFilter = "INCOME" }) { Text(Localization.getString("income", language), modifier = Modifier.padding(12.dp)) }
        }

        Spacer(Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(Localization.getString("no_transactions", language), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered) { tx ->
                    Box(modifier = Modifier.clickable { selectedTx = tx }) {
                        TransactionRow(tx = tx, language = language)
                    }
                }
            }
        }
    }

    selectedTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { selectedTx = null },
            title = { Text(Localization.getString("transaction_detail", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tx.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${Localization.getString("amount", language)}: ${formatMoney(tx.amount, "VND", language)}")
                    Text("${Localization.getString("category", language)}: ${tx.category}")
                    Text("${Localization.getString("account", language)}: ${tx.accountName}")
                    Text("${Localization.getString("date", language)}: ${formatDate(tx.date)}")
                    if (tx.note.isNotEmpty()) Text("${Localization.getString("note", language)}: ${tx.note}")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.deleteTransaction(tx); selectedTx = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(Localization.getString("delete", language))
                }
            },
            dismissButton = { TextButton(onClick = { selectedTx = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }
}

// ─── Reports Screen ───────────────────────────────────────────────────────────

@Composable
fun ReportsScreen(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var activeTimeFilter by remember { mutableStateOf("THIS_MONTH") }

    val now = System.currentTimeMillis()
    val startOfTime = startOfReportPeriod(now, activeTimeFilter)
    val periodTx = transactions.filter { it.date >= startOfTime }
    val incomeSum = periodTx.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseSum = periodTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val remaining = incomeSum - expenseSum
    val dailyCashFlow = remember(periodTx, now) { buildDailyCashFlow(periodTx, now, language) }
    val catSums = periodTx
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandCanvas),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (language == AppLanguage.VIETNAMESE) "Báo cáo dễ hiểu" else "Clear reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = BrandInk
                )
                Text(
                    text = if (language == AppLanguage.VIETNAMESE)
                        "Nhìn nhanh tiền vào, tiền ra và hạng mục đang tiêu nhiều."
                    else "A quick look at cash in, cash out, and where spending goes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODAY" to "today", "WEEK" to "this_week", "THIS_MONTH" to "this_month", "YEAR" to "this_year")
                    .forEach { (key, langKey) ->
                        ElevatedAssistChip(
                            onClick = { activeTimeFilter = key },
                            label = { Text(Localization.getString(langKey, language)) },
                            colors = if (activeTimeFilter == key)
                                AssistChipDefaults.elevatedAssistChipColors(containerColor = BrandGoldSoft, labelColor = BrandGreenDark)
                            else AssistChipDefaults.elevatedAssistChipColors()
                        )
                    }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = if (language == AppLanguage.VIETNAMESE) "Còn lại" else "Left",
                    value = formatMoney(remaining, "VND", language),
                    icon = if (remaining >= 0.0) Icons.Default.Savings else Icons.Default.Warning,
                    accent = if (remaining >= 0.0) BrandIncome else BrandExpense,
                    containerColor = if (remaining >= 0.0) BrandIncomeSoft else BrandExpenseSoft,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = Localization.getString("expense", language),
                    value = formatMoney(expenseSum, "VND", language),
                    icon = Icons.Default.TrendingDown,
                    accent = BrandExpense,
                    containerColor = BrandExpenseSoft,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            WeeklyCashFlowCard(dailyCashFlow, incomeSum, expenseSum, language)
        }

        item {
            CategoryBreakdownCard(catSums, language)
        }
    }
}

private data class DailyCashFlow(
    val label: String,
    val income: Double,
    val expense: Double
)

private val ReportCategoryColors = listOf(
    BrandExpense,
    BrandGold,
    BrandBlue,
    BrandIncome,
    Color(0xFF7C5CC4),
    Color(0xFFCF7A31),
    BrandMuted
)

private fun startOfReportPeriod(now: Long, activeTimeFilter: String): Long {
    return Calendar.getInstance().run {
        timeInMillis = now
        set(Calendar.MILLISECOND, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.HOUR_OF_DAY, 0)
        when (activeTimeFilter) {
            "TODAY" -> Unit
            "WEEK" -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            "THIS_MONTH" -> set(Calendar.DAY_OF_MONTH, 1)
            else -> set(Calendar.DAY_OF_YEAR, 1)
        }
        timeInMillis
    }
}

private fun buildDailyCashFlow(
    transactions: List<TransactionEntity>,
    now: Long,
    language: AppLanguage
): List<DailyCashFlow> {
    val dayFormatter = SimpleDateFormat(
        if (language == AppLanguage.VIETNAMESE) "dd/MM" else "MM/dd",
        Locale.getDefault()
    )
    return (6 downTo 0).map { offset ->
        val dayStart = Calendar.getInstance().run {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -offset)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.HOUR_OF_DAY, 0)
            timeInMillis
        }
        val dayEnd = Calendar.getInstance().run {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
            timeInMillis
        }
        val dayTx = transactions.filter { it.date in dayStart until dayEnd }
        DailyCashFlow(
            label = dayFormatter.format(Date(dayStart)),
            income = dayTx.filter { it.type == "INCOME" }.sumOf { it.amount },
            expense = dayTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        )
    }
}

@Composable
private fun WeeklyCashFlowCard(
    dailyCashFlow: List<DailyCashFlow>,
    incomeSum: Double,
    expenseSum: Double,
    language: AppLanguage
) {
    val maxValue = maxOf(dailyCashFlow.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0, 1.0)

    AppCard(modifier = Modifier.fillMaxWidth(), containerColor = BrandSurface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Dòng tiền 7 ngày" else "7-day cash flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = BrandInk
                    )
                    Text(
                        text = if (language == AppLanguage.VIETNAMESE) "Cột xanh là tiền vào, cột đỏ là tiền ra." else "Green bars are income, red bars are spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(incomeSum, "VND", language), color = BrandIncome, fontWeight = FontWeight.Bold)
                    Text(formatMoney(expenseSum, "VND", language), color = BrandExpense, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(148.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyCashFlow.forEach { point ->
                    DailyBar(point, maxValue)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ReportLegendDot(BrandIncome, Localization.getString("income", language))
                ReportLegendDot(BrandExpense, Localization.getString("expense", language))
            }
        }
    }
}

@Composable
private fun DailyBar(point: DailyCashFlow, maxValue: Double) {
    val incomeHeight = ((point.income / maxValue) * 92).coerceIn(0.0, 92.0).dp
    val expenseHeight = ((point.expense / maxValue) * 92).coerceIn(0.0, 92.0).dp
    val minVisible = 4.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Box(modifier = Modifier.height(104.dp), contentAlignment = Alignment.BottomCenter) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(if (point.income > 0.0) incomeHeight.coerceAtLeast(minVisible) else 2.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (point.income > 0.0) BrandIncome else BrandBorder)
                )
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(if (point.expense > 0.0) expenseHeight.coerceAtLeast(minVisible) else 2.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (point.expense > 0.0) BrandExpense else BrandBorder)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(point.label, style = MaterialTheme.typography.labelSmall, color = BrandMuted)
    }
}

@Composable
private fun CategoryBreakdownCard(
    categorySums: List<Pair<String, Double>>,
    language: AppLanguage
) {
    val totalExpense = categorySums.sumOf { it.second }
    val topCategories = categorySums.take(6)

    AppCard(modifier = Modifier.fillMaxWidth(), containerColor = BrandSurface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (language == AppLanguage.VIETNAMESE) "Tiền đi đâu?" else "Where did money go?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = BrandInk
                )
                Text(
                    text = if (language == AppLanguage.VIETNAMESE) "Hạng mục lớn nhất nằm trên cùng để dễ quyết định." else "Largest categories stay on top for quick decisions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            if (topCategories.isEmpty()) {
                EmptyReportState(language)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpenseDonutChart(
                        slices = topCategories,
                        totalExpense = totalExpense,
                        language = language,
                        modifier = Modifier.size(142.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        topCategories.take(3).forEachIndexed { index, (category, amount) ->
                            ReportLegendDot(
                                color = ReportCategoryColors[index % ReportCategoryColors.size],
                                label = "${Localization.getString(category, language)} ${((amount / totalExpense) * 100).toInt()}%"
                            )
                        }
                    }
                }

                topCategories.forEachIndexed { index, (category, amount) ->
                    CategoryProgressRow(
                        rank = index + 1,
                        category = Localization.getString(category, language),
                        amount = amount,
                        total = totalExpense,
                        color = ReportCategoryColors[index % ReportCategoryColors.size],
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseDonutChart(
    slices: List<Pair<String, Double>>,
    totalExpense: Double,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f

            drawArc(
                color = BrandSurfaceAlt,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            slices.forEachIndexed { index, (_, amount) ->
                val sweep = ((amount / totalExpense) * 360f).toFloat().coerceAtLeast(2f)
                drawArc(
                    color = ReportCategoryColors[index % ReportCategoryColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (language == AppLanguage.VIETNAMESE) "Đã chi" else "Spent",
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted
            )
            Text(
                text = formatMoney(totalExpense, "VND", language),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = BrandInk,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryProgressRow(
    rank: Int,
    category: String,
    amount: Double,
    total: Double,
    color: Color,
    language: AppLanguage
) {
    val percent = if (total <= 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape)
            .background(BrandCanvas)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(AppShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(rank.toString(), color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(category, fontWeight = FontWeight.SemiBold, color = BrandInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${(percent * 100).toInt()}%", color = BrandMuted, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(BrandBorder)) {
                Box(modifier = Modifier.fillMaxWidth(percent).height(8.dp).background(color))
            }
        }
        Text(formatMoney(amount, "VND", language), color = BrandExpense, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReportLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = BrandMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyReportState(language: AppLanguage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(AppShape)
            .background(BrandCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = BrandMuted, modifier = Modifier.size(34.dp))
            Text(
                text = if (language == AppLanguage.VIETNAMESE) "Nhập vài giao dịch để xem báo cáo." else "Add a few transactions to see reports.",
                color = BrandMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Tools Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToolsScreen(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    var activeSubTool by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = activeSubTool,
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        label = "tools_nav"
    ) { tool ->
        if (tool == null) {
            MainToolsDashboard(onSelectTool = { activeSubTool = it }, language = language)
        } else {
            Column(modifier = Modifier.fillMaxSize().background(BrandCanvas)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    IconButton(onClick = { activeSubTool = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandGreenDark)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (tool) {
                            "ACCOUNTS" -> Localization.getString("account_title", language)
                            "BUDGETS" -> Localization.getString("budget_title", language)
                            "CATEGORIES" -> Localization.getString("category_title", language)
                            "RECURRING" -> Localization.getString("recurring_title", language)
                            else -> Localization.getString("split_bill_title", language)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandInk
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (tool) {
                        "ACCOUNTS" -> AccountsTool(viewModel = viewModel, language = language)
                        "BUDGETS" -> BudgetsTool(viewModel = viewModel, language = language)
                        "CATEGORIES" -> CategoriesTool(viewModel = viewModel, language = language)
                        "RECURRING" -> RecurringTool(viewModel = viewModel, language = language)
                        "SPLIT" -> SplitBillTool(viewModel = viewModel, language = language)
                    }
                }
            }
        }
    }
}

@Composable
fun MainToolsDashboard(onSelectTool: (String) -> Unit, language: AppLanguage) {
    val items = listOf(
        "ACCOUNTS" to "account_title",
        "BUDGETS" to "budget_title",
        "CATEGORIES" to "category_title",
        "RECURRING" to "recurring_title",
        "SPLIT" to "split_bill_title"
    )
    val subtitles = mapOf(
        "ACCOUNTS" to if (language == AppLanguage.VIETNAMESE) "Tiền mặt, ngân hàng, ví điện tử." else "Cash, bank and e-wallets.",
        "BUDGETS" to if (language == AppLanguage.VIETNAMESE) "Theo dõi hạn mức theo tháng." else "Track monthly limits.",
        "CATEGORIES" to if (language == AppLanguage.VIETNAMESE) "Tùy chỉnh nhóm thu chi." else "Customize spending groups.",
        "RECURRING" to if (language == AppLanguage.VIETNAMESE) "Tiền nhà, Internet, điện nước." else "Rent, internet and utilities.",
        "SPLIT" to if (language == AppLanguage.VIETNAMESE) "Chia hóa đơn với bạn bè." else "Split shared bills."
    )
    val icons = mapOf(
        "ACCOUNTS" to Icons.Default.AccountBalanceWallet,
        "BUDGETS" to Icons.Default.TrackChanges,
        "CATEGORIES" to Icons.Default.Category,
        "RECURRING" to Icons.Default.Repeat,
        "SPLIT" to Icons.Default.Groups
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark(size = 42.dp)
                Column {
                    SectionHeader(Localization.getString("tools", language))
                    Text(
                        if (language == AppLanguage.VIETNAMESE) "Các công cụ phụ trợ cho ví của bạn." else "Companion tools for your wallet.",
                        color = BrandMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        items(items) { (key, langKey) ->
            AppCard(modifier = Modifier.fillMaxWidth().clickable { onSelectTool(key) }) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(42.dp).background(BrandMint, AppShape), contentAlignment = Alignment.Center) {
                            Icon(icons[key] ?: Icons.Default.Apps, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(23.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(Localization.getString(langKey, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BrandInk)
                            Text(subtitles[key].orEmpty(), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandMuted)
                }
            }
        }
    }
}

// ─── Accounts Tool ────────────────────────────────────────────────────────────

@Composable
fun AccountsTool(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.getString("add_account", language), fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(Localization.getString("account_name", language)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = formatVndInput(it) }, label = { Text(Localization.getString("initial_balance", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (name.isNotEmpty()) { viewModel.addAccount(name, parseVndInput(balance)); name = ""; balance = "" }
                }, modifier = Modifier.fillMaxWidth()) { Text(Localization.getString("save", language)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts) { acc ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(formatMoney(acc.balance, acc.currency, language), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { editingAccount = acc }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { accountToDelete = acc }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    editingAccount?.let { acc ->
        var editName by remember(acc) { mutableStateOf(acc.name) }
        var editBalance by remember(acc) { mutableStateOf(formatVndInput(acc.balance.toLong().toString())) }
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text(Localization.getString("edit_account", language), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text(Localization.getString("account_name", language)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editBalance, onValueChange = { editBalance = formatVndInput(it) }, label = { Text(Localization.getString("initial_balance", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { if (editName.isNotEmpty()) { viewModel.updateAccount(acc, editName, parseVndInput(editBalance)); editingAccount = null } }) {
                    Text(Localization.getString("save_changes", language))
                }
            },
            dismissButton = { TextButton(onClick = { editingAccount = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }

    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("${Localization.getString("delete", language)}: ${acc.name}", fontWeight = FontWeight.Bold) },
            text = { Text(Localization.getString("delete_account_confirm", language)) },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = { viewModel.deleteAccount(acc); accountToDelete = null }) {
                    Text(Localization.getString("delete", language), color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { accountToDelete = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }
}

// ─── Budgets Tool ─────────────────────────────────────────────────────────────

@Composable
fun BudgetsTool(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val budgets by viewModel.budgetsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var name by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.getString("add_budget", language), fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(Localization.getString("budget_name", language)) }, modifier = Modifier.fillMaxWidth())
                val expenseCats = categories.filter { it.type == "EXPENSE" }
                if (categoryName.isEmpty() && expenseCats.isNotEmpty()) categoryName = expenseCats.first().name
                var expandedCat by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${Localization.getString("category", language)}: ${Localization.getString(categoryName, language)}")
                    }
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        expenseCats.forEach { cat ->
                            DropdownMenuItem(text = { Text(Localization.getString(cat.name, language)) }, onClick = { categoryName = cat.name; expandedCat = false })
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = formatVndInput(it) }, label = { Text(Localization.getString("budget_amount", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val limitAmt = parseVndInput(amount)
                    if (name.isNotEmpty() && categoryName.isNotEmpty() && limitAmt > 0.0) {
                        viewModel.addBudget(name, categoryName, limitAmt, "MONTHLY", System.currentTimeMillis(), 0)
                        name = ""; amount = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(Localization.getString("save", language)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(budgets) { budget ->
                val sumExpenses = transactions.filter { it.type == "EXPENSE" && it.category == budget.categoryName }.sumOf { it.amount }
                val ratio = sumExpenses / budget.amount
                val progressColor = when { ratio < 0.70 -> Color(0xFF2E7D32); ratio < 1.00 -> Color(0xFFE65100); else -> Color(0xFFC62828) }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(budget.name, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteBudget(budget.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("${Localization.getString("category", language)}: ${Localization.getString(budget.categoryName, language)}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = progressColor, trackColor = Color.LightGray
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${formatMoney(sumExpenses, "VND", language)} / ${formatMoney(budget.amount, "VND", language)}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = progressColor
                        )
                    }
                }
            }
        }
    }
}

// ─── Categories Tool ──────────────────────────────────────────────────────────

@Composable
fun CategoriesTool(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var tabSelected by remember { mutableStateOf("EXPENSE") }
    var name by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        TabRow(selectedTabIndex = if (tabSelected == "EXPENSE") 0 else 1) {
            Tab(selected = tabSelected == "EXPENSE", onClick = { tabSelected = "EXPENSE" }) { Text(Localization.getString("expense_categories", language), modifier = Modifier.padding(12.dp)) }
            Tab(selected = tabSelected == "INCOME", onClick = { tabSelected = "INCOME" }) { Text(Localization.getString("income_categories", language), modifier = Modifier.padding(12.dp)) }
        }
        Spacer(Modifier.height(12.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Localization.getString("add_category", language), fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(Localization.getString("category", language)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (name.isNotEmpty()) { viewModel.addCategory(name, tabSelected); name = "" } }, modifier = Modifier.fillMaxWidth()) {
                    Text(Localization.getString("save", language))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories.filter { it.type == tabSelected }) { cat ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(Localization.getString(cat.name, language), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { editingCategory = cat }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { categoryToDelete = cat }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    editingCategory?.let { cat ->
        var editName by remember(cat) { mutableStateOf(cat.name) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(Localization.getString("edit_category", language), fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text(Localization.getString("category", language)) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { if (editName.isNotEmpty()) { viewModel.updateCategory(cat, editName); editingCategory = null } }) { Text(Localization.getString("save_changes", language)) } },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("${Localization.getString("delete", language)}: ${cat.name}", fontWeight = FontWeight.Bold) },
            text = { Text(Localization.getString("delete_category_confirm", language)) },
            confirmButton = { Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = { viewModel.deleteCategory(cat.name); categoryToDelete = null }) { Text(Localization.getString("delete", language), color = Color.White) } },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }
}

// ─── Recurring Tool ───────────────────────────────────────────────────────────

@Composable
fun RecurringTool(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val recurringList by viewModel.recurringTransactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList<RecurringTransactionEntity>())
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList<CategoryEntity>())
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList<AccountEntity>())

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Localization.getString("add_recurring", language), fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { selectedType = "EXPENSE" }, colors = ButtonDefaults.elevatedButtonColors(containerColor = if (selectedType == "EXPENSE") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface)) {
                        Text(Localization.getString("expense", language), color = if (selectedType == "EXPENSE") Color(0xFFC62828) else Color.Gray)
                    }
                    ElevatedButton(onClick = { selectedType = "INCOME" }, colors = ButtonDefaults.elevatedButtonColors(containerColor = if (selectedType == "INCOME") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface)) {
                        Text(Localization.getString("income", language), color = if (selectedType == "INCOME") Color(0xFF2E7D32) else Color.Gray)
                    }
                }
                OutlinedTextField(value = name, onValueChange = { v -> name = v }, label = { Text(Localization.getString("recurring_name", language)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { v -> amount = formatVndInput(v) }, label = { Text(Localization.getString("amount", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                val subsetCats = categories.filter { cat -> cat.type == selectedType }
                if (categoryName.isEmpty() && subsetCats.isNotEmpty()) categoryName = subsetCats.first().name
                var expandedCat by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${Localization.getString("category", language)}: ${Localization.getString(categoryName, language)}")
                    }
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        for (cat in subsetCats) {
                            DropdownMenuItem(text = { Text(Localization.getString(cat.name, language)) }, onClick = { categoryName = cat.name; expandedCat = false })
                        }
                    }
                }

                if (accountName.isEmpty() && accounts.isNotEmpty()) accountName = accounts.first().name
                var expandedAcc by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expandedAcc = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${Localization.getString("account", language)}: ${Localization.getString(accountName, language)}")
                    }
                    DropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                        for (acc in accounts) {
                            DropdownMenuItem(text = { Text(Localization.getString(acc.name, language)) }, onClick = { accountName = acc.name; expandedAcc = false })
                        }
                    }
                }

                Button(onClick = {
                    val numAmt = parseVndInput(amount)
                    if (name.isNotEmpty() && numAmt > 0.0 && categoryName.isNotEmpty() && accountName.isNotEmpty()) {
                        viewModel.addRecurringTransaction(name, selectedType, numAmt, categoryName, accountName, "MONTHLY_CYCLE", System.currentTimeMillis())
                        name = ""; amount = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(Localization.getString("save", language)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recurringList) { rec ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(rec.name, fontWeight = FontWeight.Bold)
                        Text("${Localization.getString("category", language)}: ${Localization.getString(rec.category, language)}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoney(rec.amount, "VND", language), color = if (rec.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteRecurringTransaction(rec.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ─── Split Bill Tool ──────────────────────────────────────────────────────────

@Composable
fun SplitBillTool(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    var rawParticipants by remember { mutableStateOf("Bảo, An, Vinh") }
    var payer by remember { mutableStateOf("An") }
    var amount by remember { mutableStateOf("150.000") }
    var resultState by remember { mutableStateOf<SplitBillResult?>(null) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = rawParticipants, onValueChange = { rawParticipants = it }, label = { Text(Localization.getString("participants", language)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = payer, onValueChange = { payer = it }, label = { Text(Localization.getString("payer", language)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = formatVndInput(it) }, label = { Text(Localization.getString("total_amount", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Button(onClick = { resultState = viewModel.calculateSettlements(rawParticipants, payer, parseVndInput(amount)) }, modifier = Modifier.fillMaxWidth()) {
            Text(Localization.getString("add_split", language))
        }
        resultState?.let { res ->
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(Localization.getString("share_per_person", language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                items(res.balances.toList()) { (name, bal) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, fontWeight = FontWeight.Medium)
                        val text = when { bal > 0 -> "+${formatMoney(bal, "VND", language)}"; bal < 0 -> "-${formatMoney(-bal, "VND", language)}"; else -> "0 ₫" }
                        val color = when { bal > 0 -> Color(0xFF2E7D32); bal < 0 -> Color(0xFFC62828); else -> Color.Gray }
                        Text(text, color = color, fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(Modifier.height(12.dp)); Text(Localization.getString("settlements", language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                if (res.suggestions.isEmpty()) {
                    item { Text("Không cần giao dịch thanh toán thêm.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray) }
                } else {
                    items(res.suggestions) { sug ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${sug.debtor} ${Localization.getString("owes", language)} ${sug.creditor}", fontWeight = FontWeight.Medium)
                                Text(formatMoney(sug.amount, "VND", language), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(viewModel: LuckyWalletViewModel, language: AppLanguage) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val username by viewModel.userName.collectAsStateWithLifecycle()
    val email by viewModel.userEmail.collectAsStateWithLifecycle()
    val driveToken by viewModel.driveAccessToken.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()

    var langToSwitchTo by remember { mutableStateOf<AppLanguage?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val scope = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Launcher xử lý kết quả Drive permission (UserRecoverableAuthException)
    val drivePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // User vừa cấp quyền Drive → thử lấy token lại
        coroutineScope.launch {
            viewModel.refreshDriveToken(context)
        }
    }

    // Google Sign-In launcher
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            // Lấy Drive access token thật (không phải idToken)
            coroutineScope.launch {
                viewModel.onGoogleSignInSuccess(
                    context = context,
                    account = account,
                    onDrivePermissionNeeded = { intent -> drivePermissionLauncher.launch(intent) }
                )
            }
        }.onFailure { e ->
            android.util.Log.e("GoogleSignIn", "Sign-in failed: ${e.message}")
        }
    }

    fun launchGoogleSignIn() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark(size = 42.dp)
                Column {
                    SectionHeader(Localization.getString("settings", language))
                    Text(
                        if (language == AppLanguage.VIETNAMESE) "Tài khoản, sao lưu và ngôn ngữ." else "Account, backup and language.",
                        color = BrandMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── Sao lưu và khôi phục ──────────────────────────────────────────
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), containerColor = BrandSurface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).background(BrandMint, AppShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.getString("backup_title", language), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = BrandInk)
                    }

                    // Google Sign-In / Sign-Out
                    if (!isLoggedIn || driveToken == null) {
                        Text(Localization.getString("sign_in_required", language), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            shape = AppShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Localization.getString("google_sign_in_btn", language))
                        }
                    } else {
                        // Đã đăng nhập
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BrandIncome, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(username, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(email, style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                            }
                            TextButton(onClick = {
                                com.google.android.gms.auth.api.signin.GoogleSignIn
                                    .getClient(context, com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .signOut()
                                viewModel.onGoogleSignOut()
                            }) { Text(Localization.getString("google_sign_out", language), color = MaterialTheme.colorScheme.error) }
                        }

                        HorizontalDivider()

                        // Thời gian backup gần nhất
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandMuted)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${Localization.getString("last_backup", language)}: ${viewModel.formatBackupTime(lastBackupTime, language)}",
                                style = MaterialTheme.typography.bodySmall, color = BrandMuted
                            )
                        }

                        // Sao lưu ngay
                        Button(
                            onClick = { viewModel.backupNow(context) },
                            enabled = !isBackingUp,
                            shape = AppShape,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(Localization.getString("backup_running", language))
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Localization.getString("backup_now", language))
                            }
                        }

                        // Khôi phục
                        OutlinedButton(
                            onClick = { showRestoreConfirm = true },
                            enabled = !isBackingUp,
                            shape = AppShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Localization.getString("restore_now", language))
                        }

                        // Tự động sao lưu 8 PM
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Localization.getString("auto_backup", language), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { viewModel.setAutoBackup(context, it) }
                            )
                        }

                        // Kết quả backup
                        backupMessage?.let { msg ->
                            val isSuccess = msg.startsWith("Đã sao lưu") || msg.startsWith("Backup") || msg.startsWith("Đã khôi phục") || msg.startsWith("Restored")
                            Card(
                                shape = AppShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) BrandIncomeSoft else BrandExpenseSoft
                                )
                            ) {
                                Text(
                                    msg,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) BrandIncome else BrandExpense
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Ngôn ngữ ─────────────────────────────────────────────────────
        item {
            AppCard(containerColor = BrandSurface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Localization.getString("language", language), fontWeight = FontWeight.Bold, color = BrandInk)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedCard(
                            onClick = { if (language != AppLanguage.VIETNAMESE) langToSwitchTo = AppLanguage.VIETNAMESE },
                            shape = AppShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = if (language == AppLanguage.VIETNAMESE) BrandGoldSoft else BrandSurfaceAlt),
                            modifier = Modifier.weight(1f)
                        ) { Text("Tiếng Việt", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                        ElevatedCard(
                            onClick = { if (language != AppLanguage.ENGLISH) langToSwitchTo = AppLanguage.ENGLISH },
                            shape = AppShape,
                            colors = CardDefaults.elevatedCardColors(containerColor = if (language == AppLanguage.ENGLISH) BrandGoldSoft else BrandSurfaceAlt),
                            modifier = Modifier.weight(1f)
                        ) { Text("English", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // Dialog xác nhận khôi phục
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(Localization.getString("restore_confirm_title", language)) },
            text = { Text(Localization.getString("restore_confirm_body", language)) },
            confirmButton = {
                Button(
                    onClick = { showRestoreConfirm = false; viewModel.restoreFromDrive(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(Localization.getString("confirm", language)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(Localization.getString("cancel", language)) }
            }
        )
    }

    langToSwitchTo?.let { targetLang ->
        AlertDialog(
            onDismissRequest = { langToSwitchTo = null },
            title = { Text(if (targetLang == AppLanguage.VIETNAMESE) "Xác nhận chuyển đổi" else "Confirm selection") },
            text = { Text(if (targetLang == AppLanguage.VIETNAMESE) "Bạn có chắc chắn muốn chuyển ngôn ngữ sang Tiếng Việt?" else "Are you sure you want to change language to English?") },
            confirmButton = { Button(onClick = { viewModel.changeLanguage(targetLang); langToSwitchTo = null }) { Text(Localization.getString("confirm", language)) } },
            dismissButton = { TextButton(onClick = { langToSwitchTo = null }) { Text(Localization.getString("cancel", language)) } }
        )
    }
}

// ─── Add Transaction Dialog ───────────────────────────────────────────────────

@Composable
fun AddTransactionDialog(
    type: String,
    viewModel: LuckyWalletViewModel,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList<CategoryEntity>())
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList<AccountEntity>())
    val subsetCats = categories.filter { cat -> cat.type == type }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Kh\u00e1c") }
    var accountSelection by remember { mutableStateOf("Ti\u1ec1n m\u1eb7t") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var expCatDropdown by remember { mutableStateOf(false) }
    var expAccDropdown by remember { mutableStateOf(false) }
    var showObjectCapture by remember { mutableStateOf(false) }

    val hasNoAccounts = accounts.isEmpty()

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.name == accountSelection }) accountSelection = accounts.first().name
    }
    LaunchedEffect(subsetCats) {
        if (subsetCats.isNotEmpty() && subsetCats.none { it.name == categorySelection }) categorySelection = subsetCats.first().name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "INCOME") Localization.getString("add_income", language) else Localization.getString("add_expense", language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasNoAccounts) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (language == AppLanguage.VIETNAMESE) "B\u1ea1n c\u1ea7n th\u00eam \u00edt nh\u1ea5t m\u1ed9t t\u00e0i kho\u1ea3n trong tab C\u00f4ng c\u1ee5 tr\u01b0\u1edbc khi ghi giao d\u1ecbch."
                            else "Add at least one account in Tools before recording transactions.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = formatVndInput(it); inlineError = null },
                        label = { Text(Localization.getString("amount", language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; inlineError = null },
                        label = { Text(if (language == AppLanguage.VIETNAMESE) "S\u1ea3n ph\u1ea9m" else Localization.getString("title", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = inlineError != null
                    )
                    FilledTonalButton(onClick = { date = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth(), shape = AppShape) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (language == AppLanguage.VIETNAMESE) "Ng\u00e0y ${formatDate(date)}" else formatDate(date))
                    }
                    Box {
                        OutlinedButton(onClick = { expCatDropdown = true }, modifier = Modifier.fillMaxWidth(), shape = AppShape) {
                            Text("${Localization.getString("category", language)}: ${Localization.getString(categorySelection, language)}")
                        }
                        DropdownMenu(expanded = expCatDropdown, onDismissRequest = { expCatDropdown = false }) {
                            for (cat in subsetCats) {
                                DropdownMenuItem(text = { Text(Localization.getString(cat.name, language)) }, onClick = { categorySelection = cat.name; expCatDropdown = false })
                            }
                        }
                    }
                    Box {
                        Button(onClick = { expAccDropdown = true }, modifier = Modifier.fillMaxWidth(), shape = AppShape, colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)) {
                            Text("${Localization.getString("account", language)}: ${Localization.getString(accountSelection, language)}")
                        }
                        DropdownMenu(expanded = expAccDropdown, onDismissRequest = { expAccDropdown = false }) {
                            for (acc in accounts) {
                                DropdownMenuItem(text = { Text(Localization.getString(acc.name, language)) }, onClick = { accountSelection = acc.name; expAccDropdown = false })
                            }
                        }
                    }
                    OutlinedButton(onClick = { showObjectCapture = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = AppShape) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (language == AppLanguage.VIETNAMESE) "Qu\u00e9t b\u1eb1ng AI" else "Scan with AI")
                    }
                    inlineError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = {
            if (!hasNoAccounts) {
                Button(onClick = {
                    val numAmount = parseVndInput(amount)
                    when {
                        title.isBlank() -> inlineError = Localization.getString(if (type == "INCOME") "input_title_income_error" else "input_title_error", language)
                        numAmount <= 0.0 -> inlineError = if (language == AppLanguage.VIETNAMESE) "Vui l\u00f2ng nh\u1eadp s\u1ed1 ti\u1ec1n ch\u00ednh x\u00e1c." else "Enter a valid amount."
                        else -> {
                            viewModel.addTransaction(title, numAmount, type, categorySelection, accountSelection, date, "")
                            onDismiss()
                        }
                    }
                }, shape = AppShape, colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark)) { Text(Localization.getString("save", language)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Localization.getString("cancel", language)) } }
    )

    if (showObjectCapture) {
        ObjectCaptureCameraScreen(
            transactionType = if (type == "INCOME") CaptureTransactionType.INCOME else CaptureTransactionType.EXPENSE,
            onDismiss = { showObjectCapture = false },
            onResult = { result ->
                result.amount?.let { amount = formatVndInput(it.toString()) }
                result.productNote?.takeIf { it.isNotBlank() }?.let { title = it }
                result.categoryName?.takeIf { candidate -> subsetCats.any { it.name == candidate } }?.let { categorySelection = it }
                showObjectCapture = false
            }
        )
    }
}

@Composable
fun ThermalReceiptCard(ocrResult: OcrResult, language: AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFBF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E0D8))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                ocrResult.merchantName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = Color.Black
            )
            Text(
                if (language == AppLanguage.VIETNAMESE) "HÓA ĐƠN THANH TOÁN" else "RECEIPT",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.LightGray, maxLines = 1)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (language == AppLanguage.VIETNAMESE) "TÊN MÓN" else "ITEM", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Gray)
                Text(if (language == AppLanguage.VIETNAMESE) "TỔNG" else "TOTAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Gray)
            }
            if (ocrResult.items.isNotEmpty()) {
                ocrResult.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("${item.quantity}x", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.DarkGray, modifier = Modifier.padding(end = 6.dp))
                            Text(item.name, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Black, maxLines = 2)
                        }
                        Text(formatMoney(item.total, ocrResult.currency, language), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Black)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ocrResult.title.ifEmpty { if (language == AppLanguage.VIETNAMESE) "Giao dịch" else "Transaction" }, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Black)
                    Text(formatMoney(ocrResult.amount, ocrResult.currency, language), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Black)
                }
            }
            Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.LightGray, maxLines = 1, modifier = Modifier.padding(vertical = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (language == AppLanguage.VIETNAMESE) "TỔNG CỘNG" else "TOTAL", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Black)
                Text(formatMoney(ocrResult.amount, ocrResult.currency, language), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color.Red)
            }
        }
    }
}

// ─── Scan AI Dialog — sử dụng OCR thật + mẫu demo ────────────────────────────

@Composable
fun ScanAiDialog(
    viewModel: LuckyWalletViewModel,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    var parsedDraftResult by remember { mutableStateOf<OcrResult?>(null) }
    var showDraftReview by remember { mutableStateOf(false) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var customOcrText by remember { mutableStateOf("") }

    var draftAmount by remember { mutableStateOf("") }
    var draftTitle by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Khác") }
    var draftAccount by remember { mutableStateOf("Tiền mặt") }
    var draftType by remember { mutableStateOf("EXPENSE") }
    var inlineErrorOnDraft by remember { mutableStateOf<String?>(null) }
    var ocrWarning by remember { mutableStateOf<String?>(null) }

    // Danh sách món có thể sửa được sau scan
    data class EditableItem(val id: Int, val name: String, val qty: Int, val price: String)
    var editableItems by remember { mutableStateOf<List<EditableItem>>(emptyList()) }
    var editableItemsManualTotal by remember { mutableStateOf(false) } // true nếu user đã sửa tổng tay

    fun recalcTotalFromItems(items: List<EditableItem>): Double =
        items.sumOf { item -> parseVndInput(item.price) * item.qty }

    fun syncTotalFromItems(items: List<EditableItem>) {
        if (!editableItemsManualTotal) {
            val sum = recalcTotalFromItems(items)
            if (sum > 0.0) draftAmount = formatVndInput(sum.toLong().toString())
        }
    }

    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList<CategoryEntity>())
    val accounts by viewModel.accountsFlow.collectAsStateWithLifecycle(initialValue = emptyList<AccountEntity>())
    val currentTypeCats = categories.filter { cat -> cat.type == draftType }
    var expandedDraftCat by remember { mutableStateOf(false) }
    var expandedDraftAcc by remember { mutableStateOf(false) }

    // ── Real gallery picker → OCR engine ──
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isOcrRunning = true
                ocrWarning = null
                try {
                    val result = viewModel.analyzeImage(context, it)
                    parsedDraftResult = result
                    draftAmount = formatVndInput(result.amount.toLong().toString())
                    draftTitle = result.title
                    draftCategory = result.category.ifBlank { "Khác" }
                    draftType = result.type
                    if (result.amount == 0.0 || result.title.isBlank()) {
                        ocrWarning = if (language == AppLanguage.VIETNAMESE)
                            "Không đọc được rõ hóa đơn. Vui lòng kiểm tra lại thủ công."
                        else "Receipt not fully parsed. Please review manually."
                    }
                    showDraftReview = true
                } catch (e: Exception) {
                    Log.e("ScanAiDialog", "OCR error: ${e.message}")
                    ocrWarning = if (language == AppLanguage.VIETNAMESE) "Lỗi OCR: ${e.message}" else "OCR error: ${e.message}"
                } finally {
                    isOcrRunning = false
                }
            }
        }
    }

    fun fillDraftFromOcr(result: OcrResult) {
        parsedDraftResult = result
        draftAmount = formatVndInput(result.amount.toLong().toString())
        draftTitle = result.title
        draftCategory = result.category.ifBlank { "Khác" }
        draftType = result.type
        editableItemsManualTotal = false
        editableItems = result.items.mapIndexed { idx, item ->
            EditableItem(
                id = idx,
                name = item.name,
                qty = item.quantity,
                price = formatVndInput(item.price.toLong().toString())
            )
        }
        showDraftReview = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(if (showDraftReview) Localization.getString("ocr_draft_label", language) else Localization.getString("scan_receipt", language))
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
                if (showDraftReview) {
                    // ── Draft Review Form ──
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        // Tên cửa hàng
                        parsedDraftResult?.merchantName?.takeIf { it.isNotEmpty() }?.let { merchant ->
                            Text(merchant, fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        // OCR warning banner
                        ocrWarning?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(it, fontSize = 11.sp, color = Color(0xFFE65100))
                                }
                            }
                        }

                        // ── Danh sách món có thể sửa ──
                        if (editableItems.isNotEmpty()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Header
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (language == AppLanguage.VIETNAMESE) "Tên món" else "Item", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                                        Text("SL", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                        Text(if (language == AppLanguage.VIETNAMESE) "Đơn giá" else "Price", fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
                                        Spacer(modifier = Modifier.width(32.dp))
                                    }
                                    HorizontalDivider()

                                    for ((idx, item) in editableItems.withIndex()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tên món - sửa được
                                            OutlinedTextField(
                                                value = item.name,
                                                onValueChange = { newName ->
                                                    editableItems = editableItems.toMutableList().also { list ->
                                                        list[idx] = list[idx].copy(name = newName)
                                                    }
                                                },
                                                modifier = Modifier.weight(1.8f),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            // Số lượng - +/-
                                            Column(modifier = Modifier.weight(0.7f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconButton(onClick = {
                                                    editableItems = editableItems.toMutableList().also { list ->
                                                        list[idx] = list[idx].copy(qty = list[idx].qty + 1)
                                                    }
                                                    syncTotalFromItems(editableItems)
                                                }, modifier = Modifier.size(20.dp)) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                                Text("${item.qty}", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                                IconButton(onClick = {
                                                    if (item.qty > 1) {
                                                        editableItems = editableItems.toMutableList().also { list ->
                                                            list[idx] = list[idx].copy(qty = list[idx].qty - 1)
                                                        }
                                                        syncTotalFromItems(editableItems)
                                                    }
                                                }, modifier = Modifier.size(20.dp)) {
                                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            // Giá - sửa được
                                            OutlinedTextField(
                                                value = item.price,
                                                onValueChange = { newPrice ->
                                                    editableItems = editableItems.toMutableList().also { list ->
                                                        list[idx] = list[idx].copy(price = formatVndInput(newPrice))
                                                    }
                                                    syncTotalFromItems(editableItems)
                                                },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            // Xóa món
                                            IconButton(onClick = {
                                                editableItems = editableItems.toMutableList().also { it.removeAt(idx) }
                                                syncTotalFromItems(editableItems)
                                            }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    HorizontalDivider()
                                    // Thêm món
                                    TextButton(
                                        onClick = {
                                            val newId = (editableItems.maxOfOrNull { it.id } ?: 0) + 1
                                            editableItems = editableItems + EditableItem(newId, "", 1, "")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (language == AppLanguage.VIETNAMESE) "Thêm món" else "Add item", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            // Chưa có món nào → nút thêm tay
                            OutlinedButton(
                                onClick = {
                                    editableItems = listOf(EditableItem(1, "", 1, ""))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (language == AppLanguage.VIETNAMESE) "Thêm món thủ công" else "Add items manually")
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Type selector
                        Row(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .background(if (draftType == "EXPENSE") MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                                    .clickable { draftType = "EXPENSE"; if (categories.none { cat -> cat.type == "EXPENSE" && cat.name == draftCategory }) draftCategory = "Khác" },
                                contentAlignment = Alignment.Center
                            ) { Text(Localization.getString("expense", language), fontWeight = FontWeight.Bold, color = if (draftType == "EXPENSE") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .background(if (draftType == "INCOME") Color(0xFFD1E7DD) else Color.Transparent)
                                    .clickable { draftType = "INCOME"; if (categories.none { cat -> cat.type == "INCOME" && cat.name == draftCategory }) draftCategory = "Lương" },
                                contentAlignment = Alignment.Center
                            ) { Text(Localization.getString("income", language), fontWeight = FontWeight.Bold, color = if (draftType == "INCOME") Color(0xFF0F5132) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                        }

                        OutlinedTextField(
                            value = draftAmount,
                            onValueChange = { v -> draftAmount = formatVndInput(v); editableItemsManualTotal = true },
                            label = { Text(Localization.getString("amount", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (editableItems.isNotEmpty() && !editableItemsManualTotal) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = draftTitle, onValueChange = { draftTitle = it; inlineErrorOnDraft = null },
                            label = { Text(Localization.getString("title", language)) },
                            placeholder = { Text(if (language == AppLanguage.VIETNAMESE) "Trống khi không trích chọn được" else "Empty if not extracted") },
                            modifier = Modifier.fillMaxWidth(), isError = inlineErrorOnDraft != null
                        )
                        inlineErrorOnDraft?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                        Box {
                            OutlinedButton(onClick = { expandedDraftCat = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("${Localization.getString("category", language)}: ${Localization.getString(draftCategory, language)}")
                            }
                            DropdownMenu(expanded = expandedDraftCat, onDismissRequest = { expandedDraftCat = false }) {
                                for (cat in currentTypeCats) {
                                    DropdownMenuItem(text = { Text(Localization.getString(cat.name, language)) }, onClick = { draftCategory = cat.name; expandedDraftCat = false })
                                }
                            }
                        }

                        if (accounts.isNotEmpty() && accounts.none { acc -> acc.name == draftAccount }) draftAccount = accounts.first().name
                        Box {
                            Button(onClick = { expandedDraftAcc = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("${Localization.getString("account", language)}: ${Localization.getString(draftAccount, language)}")
                            }
                            DropdownMenu(expanded = expandedDraftAcc, onDismissRequest = { expandedDraftAcc = false }) {
                                for (acc in accounts) {
                                    DropdownMenuItem(text = { Text(Localization.getString(acc.name, language)) }, onClick = { draftAccount = acc.name; expandedDraftAcc = false })
                                }
                            }
                        }
                    }
                } else {
                    // ── Scan / Choose Screen ──
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isOcrRunning) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text(Localization.getString("ocr_running", language), fontWeight = FontWeight.Bold)
                                    Text(Localization.getString("ocr_desc", language), fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            ocrWarning?.let { warning ->
                                item {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(warning, fontSize = 12.sp, color = Color(0xFFE65100))
                                        }
                                    }
                                }
                            }
                            // ── Real Camera Preview ──
                            if (hasCameraPermission) {
                                item {
                                    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AppShape)
                                            .background(BrandSurface)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(250.dp)
                                                .clip(AppShape)
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CameraPreviewContainer(onImageCaptureReady = { imageCaptureRef = it })
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            // ── Chụp ảnh thật → OCR thật ──
                                            Button(
                                                onClick = {
                                                    val capture = imageCaptureRef ?: return@Button
                                                    // Đặt ngay trước launch để camera ẩn ngay tức thì,
                                                    // không chờ coroutine dispatch
                                                    isOcrRunning = true
                                                    ocrWarning = null
                                                    scope.launch {
                                                        try {
                                                            val photoFile = java.io.File(
                                                                context.cacheDir,
                                                                "receipt_${System.currentTimeMillis()}.jpg"
                                                            )
                                                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                                            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                                                                capture.takePicture(
                                                                    outputOptions,
                                                                    ContextCompat.getMainExecutor(context),
                                                                    object : ImageCapture.OnImageSavedCallback {
                                                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                                            if (cont.isActive) cont.resume(Unit) {}
                                                                        }
                                                                        override fun onError(exc: ImageCaptureException) {
                                                                            if (cont.isActive) cont.cancel(exc)
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.provider",
                                                                photoFile
                                                            )
                                                            val result = viewModel.analyzeImage(context, uri)
                                                            fillDraftFromOcr(result)
                                                            if (result.amount == 0.0 || result.title.isBlank()) {
                                                                ocrWarning = if (language == AppLanguage.VIETNAMESE)
                                                                    "Không đọc được rõ hóa đơn. Kiểm tra lại thủ công."
                                                                else "Receipt not fully parsed. Please review manually."
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ScanAiDialog", "Capture error: ${e.message}")
                                                            ocrWarning = if (language == AppLanguage.VIETNAMESE)
                                                                "Không lấy được ảnh. Bạn có thể chụp lại hoặc chọn ảnh từ thư viện."
                                                            else "Could not capture the photo. Try again or pick from gallery."
                                                            showDraftReview = false
                                                        } finally {
                                                            isOcrRunning = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenDark),
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = AppShape
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(Localization.getString("take_photo", language), maxLines = 1)
                                            }
                                            // Gallery picker → OCR thật
                                            OutlinedButton(
                                                onClick = { galleryLauncher.launch("image/*") },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = AppShape
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(Localization.getString("pick_from_gallery", language), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    // No camera permission: show gallery only
                                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = AppShape) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(Localization.getString("pick_from_gallery", language), maxLines = 1)
                                    }
                                }
                            }

                            // ── Manual text input ──
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(if (language == AppLanguage.VIETNAMESE) "Nhập văn bản giao dịch tự chọn:" else "Paste custom receipt text:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = customOcrText, onValueChange = { customOcrText = it },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                            placeholder = { Text(if (language == AppLanguage.VIETNAMESE) "Ví dụ: chuyển khoản MB Bank 50.000đ" else "e.g., MB Bank transfer 50,000 VND", fontSize = 10.sp) }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val text = customOcrText.trim()
                                                if (text.isNotEmpty()) {
                                                    scope.launch {
                                                        isOcrRunning = true
                                                        kotlinx.coroutines.delay(400)
                                                        val result = ReceiptOcrParser.parseText(text)
                                                        isOcrRunning = false
                                                        fillDraftFromOcr(result)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            enabled = customOcrText.trim().isNotEmpty()
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (language == AppLanguage.VIETNAMESE) "Phân tích" else "Analyze", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // ── Demo Receipts ──
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(Localization.getString("mock_select_receipt_label", language), fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }

                            val demoReceipts = if (language == AppLanguage.VIETNAMESE) listOf(
                                "QUÁN ĂN ĐÔNG VUI\n1 Cơm Tấm Sườn 35.000\n1 Trà Đá 5.000\nTỔNG CỘNG: 40.000 VND" to "Hóa đơn Quán Ăn Đông Vui (40.000₫)",
                                "Cộng Hòa Cafe\n2 Bạc Xỉu: 70.000 d\nTổng cộng: 70.000 ₫" to "Hóa đơn cà phê (70.000₫)",
                                "MB Bank: Giao dich thanh cong\nSo tien: 150.000 VND\nNoi dung: Chuyen khoan an uong" to "Chuyển khoản MB Bank (150.000₫)",
                                "TIỆM CÀ PHÊ GIA ĐÌNH\nHĐ LƯƠNG/PAYROLL\nTỔNG LƯƠNG TRẢ: 28.900.000 VND" to "Bảng lương (28.900.000₫ — cần kiểm tra)",
                                "Target Store\n1 Milk: \$4.50\n1 Bread: \$2.50\nTotal: \$7.00" to "Hóa đơn tiếng Anh USD (\$7.00)",
                                "Co.opmart\nSP9901: 15.000d\nSP1202: 45.000đ\nTổng cộng: 60.000 ₫" to "Siêu thị (tên SP không rõ, cần kiểm tra)"
                            ) else listOf(
                                "QUÁN ĂN ĐÔNG VUI\n1 Cơm Tấm Sườn 35.000\n1 Trà Đá 5.000\nTỔNG CỘNG: 40.000 VND" to "Dong Vui Restaurant (40,000₫)",
                                "Cộng Hòa Cafe\n2 Bạc Xỉu: 70.000 d\nTổng cộng: 70.000 ₫" to "Coffee receipt (70,000₫)",
                                "MB Bank: Transaction Successful\nAmount: 150.000 VND\nDetails: Food transfer" to "MB Bank transfer (150,000₫)",
                                "TIỆM CÀ PHÊ GIA ĐÌNH\nPAYROLL\nTOTAL PAID: 28.900.000 VND" to "Payroll sheet (28,900,000₫ — needs review)",
                                "Target Store\n1 Milk: \$4.50\n1 Bread: \$2.50\nTotal: \$7.00" to "Foreign USD receipt (\$7.00)",
                                "Co.opmart\nSP9901: 15.000d\nSP1202: 45.000đ\nTotal: 60.000₫" to "Supermarket (item names unclear)"
                            )

                            items(demoReceipts) { (receiptText, label) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        scope.launch {
                                            isOcrRunning = true
                                            kotlinx.coroutines.delay(800)
                                            val result = ReceiptOcrParser.parseText(receiptText)
                                            isOcrRunning = false
                                            fillDraftFromOcr(result)
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(receiptText, fontSize = 10.sp, color = Color.Gray, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showDraftReview) {
                Button(onClick = {
                    val finalAmt = parseVndInput(draftAmount)
                    if (draftTitle.isBlank()) { inlineErrorOnDraft = Localization.getString("input_title_error", language); return@Button }
                    viewModel.addTransaction(draftTitle, finalAmt, draftType, draftCategory, draftAccount, System.currentTimeMillis(), "")
                    onDismiss()
                }) { Text(Localization.getString("save", language)) }
            }
        },
        dismissButton = {
            if (showDraftReview) {
                TextButton(onClick = { showDraftReview = false; parsedDraftResult = null; ocrWarning = null }) {
                    Text(if (language == AppLanguage.VIETNAMESE) "Quét lại" else "Scan again")
                }
            } else {
                TextButton(onClick = onDismiss) { Text(Localization.getString("cancel", language)) }
            }
        }
    )
}

// ─── Camera Preview + Capture ────────────────────────────────────────────────

@Composable
fun CameraPreviewContainer(
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) { onImageCaptureReady(imageCapture) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                    )
                } catch (e: Exception) { Log.e("Camera", "Bind failed: ${e.message}") }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
