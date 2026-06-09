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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phuongnn14.tuithantai.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

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
    NavigationBar(tonalElevation = 8.dp) {
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
                }
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

    val currency = "VND"
    val totalBalance = accounts.sumOf { it.balance }
    val incomeSum = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    var showAddTxDialog by remember { mutableStateOf<String?>(null) }
    var showScanDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting card
        item {
            val displayName = if (isLoggedIn && username.isNotEmpty()) username
                              else Localization.getString("not_logged_in", language)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${Localization.getString("logged_in_as", language)} $displayName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${Localization.getString("date", language)}: ${formatDate(System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Balance card
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.getString("current_balance", language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatMoney(totalBalance, currency, language),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Income / Expense summary
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.height(4.dp))
                        Text(Localization.getString("income", language), style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(2.dp))
                        Text("+${formatMoney(incomeSum, currency, language)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(Modifier.height(4.dp))
                        Text(Localization.getString("expense", language), style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                        Spacer(Modifier.height(2.dp))
                        Text("-${formatMoney(expenseSum, currency, language)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
        }

        // Quick actions
        item {
            Text(Localization.getString("quick_actions", language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showScanDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("scan_receipt", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { showAddTxDialog = "EXPENSE" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("add_expense", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { showAddTxDialog = "INCOME" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString("add_income", language), fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        // Recent transactions
        item {
            Text(Localization.getString("recent_transactions", language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        val recents = transactions.take(5)
        if (recents.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(Localization.getString("no_transactions", language), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        } else {
            items(recents) { tx -> TransactionRow(tx = tx, language = language) }
        }
    }

    showAddTxDialog?.let { type ->
        AddTransactionDialog(type = type, viewModel = viewModel, language = language, onDismiss = { showAddTxDialog = null })
    }
    if (showScanDialog) {
        ScanAiDialog(viewModel = viewModel, language = language, onDismiss = { showScanDialog = false })
    }
}

// ─── Transaction Row ──────────────────────────────────────────────────────────

@Composable
fun TransactionRow(tx: TransactionEntity, language: AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString(tx.category, language), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(Localization.getString(tx.accountName, language), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val isIncome = tx.type == "INCOME"
                val amountText = if (isIncome) "+${formatMoney(tx.amount, "VND", language)}" else "-${formatMoney(tx.amount, "VND", language)}"
                val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                Text(amountText, fontWeight = FontWeight.Bold, color = amountColor, style = MaterialTheme.typography.titleMedium)
                Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(Localization.getString("history", language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ScrollableTabRow(
            selectedTabIndex = when (currentFilter) { "EXPENSE" -> 1; "INCOME" -> 2; else -> 0 },
            edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()
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
    val calendar = Calendar.getInstance().also { it.timeInMillis = now }
    val startOfTime = when (activeTimeFilter) {
        "TODAY" -> calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        "WEEK" -> calendar.apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }.timeInMillis
        "THIS_MONTH" -> calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        else -> calendar.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
    }

    val periodTx = transactions.filter { it.date >= startOfTime }
    val incomeSum = periodTx.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseSum = periodTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val remaining = incomeSum - expenseSum

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(Localization.getString("report_dashboard", language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                                AssistChipDefaults.elevatedAssistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else AssistChipDefaults.elevatedAssistChipColors()
                        )
                    }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${Localization.getString("remaining", language)}: ${formatMoney(remaining, "VND", language)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${Localization.getString("income", language)}: ${formatMoney(incomeSum, "VND", language)}", color = Color(0xFF2E7D32))
                    Text("${Localization.getString("expense", language)}: ${formatMoney(expenseSum, "VND", language)}", color = Color(0xFFC62828))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Localization.getString("income_expense_chart", language), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomCenter) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            val maxVal = maxOf(incomeSum, expenseSum, 1000.0)
                            val barWidth = w / 5f; val spacerWidth = w / 12f
                            val incomeHeight = (incomeSum / maxVal * h * 0.85).toFloat()
                            val expenseHeight = (expenseSum / maxVal * h * 0.85).toFloat()
                            drawLine(color = Color.Gray, start = Offset(0f, h), end = Offset(w, h), strokeWidth = 2f)
                            drawRoundRect(color = Color(0xFF2E7D32), topLeft = Offset(w / 2f - barWidth - spacerWidth / 2f, h - incomeHeight), size = Size(barWidth, incomeHeight), cornerRadius = CornerRadius(10f, 10f))
                            drawRoundRect(color = Color(0xFFC62828), topLeft = Offset(w / 2f + spacerWidth / 2f, h - expenseHeight), size = Size(barWidth, expenseHeight), cornerRadius = CornerRadius(10f, 10f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Color(0xFF2E7D32), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(6.dp)); Text(Localization.getString("income", language), fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Color(0xFFC62828), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(6.dp)); Text(Localization.getString("expense", language), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item { Text(Localization.getString("category_stats", language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        val catSums = periodTx.filter { it.type == "EXPENSE" }.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }
        if (catSums.isEmpty()) {
            item { Text(Localization.getString("no_transactions", language), style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
        } else {
            items(catSums) { (cat, sum) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Localization.getString(cat, language), fontWeight = FontWeight.Medium)
                    Text(formatMoney(sum, "VND", language), color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
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
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    IconButton(onClick = { activeSubTool = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        fontWeight = FontWeight.Bold
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(Localization.getString("tools", language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { (key, langKey) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelectTool(key) }) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(Localization.getString(langKey, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
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

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(Localization.getString("settings", language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // ── Sao lưu và khôi phục ──────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.getString("backup_title", language), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }

                    // Google Sign-In / Sign-Out
                    if (!isLoggedIn || driveToken == null) {
                        Text(Localization.getString("sign_in_required", language), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Localization.getString("google_sign_in_btn", language))
                        }
                    } else {
                        // Đã đăng nhập
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(username, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${Localization.getString("last_backup", language)}: ${viewModel.formatBackupTime(lastBackupTime, language)}",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray
                            )
                        }

                        // Sao lưu ngay
                        Button(
                            onClick = { viewModel.backupNow(context) },
                            enabled = !isBackingUp,
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
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                            ) {
                                Text(
                                    msg,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Ngôn ngữ ─────────────────────────────────────────────────────
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(Localization.getString("language", language), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedCard(
                            onClick = { if (language != AppLanguage.VIETNAMESE) langToSwitchTo = AppLanguage.VIETNAMESE },
                            colors = CardDefaults.elevatedCardColors(containerColor = if (language == AppLanguage.VIETNAMESE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                            modifier = Modifier.weight(1f)
                        ) { Text("Tiếng Việt", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                        ElevatedCard(
                            onClick = { if (language != AppLanguage.ENGLISH) langToSwitchTo = AppLanguage.ENGLISH },
                            colors = CardDefaults.elevatedCardColors(containerColor = if (language == AppLanguage.ENGLISH) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
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
    var categorySelection by remember { mutableStateOf("Khác") }
    var accountSelection by remember { mutableStateOf("Tiền mặt") }
    var note by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var expCatDropdown by remember { mutableStateOf(false) }
    var expAccDropdown by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasNoAccounts) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (language == AppLanguage.VIETNAMESE) "Bạn cần thêm ít nhất một tài khoản tại mục 'Quản lý Tài khoản' (bên trong tab Công cụ) trước khi bắt đầu ghi chép chi tiêu!"
                            else "You need to add at least one account in 'Account Management' (inside Tools tab) before recording any transactions!",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedTextField(value = amount, onValueChange = { amount = formatVndInput(it) }, label = { Text(Localization.getString("amount", language)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it; inlineError = null }, label = { Text(Localization.getString("title", language)) }, modifier = Modifier.fillMaxWidth(), isError = inlineError != null)
                    inlineError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Box {
                        OutlinedButton(onClick = { expCatDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("${Localization.getString("category", language)}: ${Localization.getString(categorySelection, language)}")
                        }
                        DropdownMenu(expanded = expCatDropdown, onDismissRequest = { expCatDropdown = false }) {
                            for (cat in subsetCats) {
                                DropdownMenuItem(text = { Text(Localization.getString(cat.name, language)) }, onClick = { categorySelection = cat.name; expCatDropdown = false })
                            }
                        }
                    }
                    Box {
                        Button(onClick = { expAccDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("${Localization.getString("account", language)}: ${Localization.getString(accountSelection, language)}")
                        }
                        DropdownMenu(expanded = expAccDropdown, onDismissRequest = { expAccDropdown = false }) {
                            for (acc in accounts) {
                                DropdownMenuItem(text = { Text(Localization.getString(acc.name, language)) }, onClick = { accountSelection = acc.name; expAccDropdown = false })
                            }
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(Localization.getString("note", language)) }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (!hasNoAccounts) {
                Button(onClick = {
                    val numAmount = parseVndInput(amount)
                    if (title.isBlank()) { inlineError = Localization.getString(if (type == "INCOME") "input_title_income_error" else "input_title_error", language); return@Button }
                    if (numAmount > 0.0) { viewModel.addTransaction(title, numAmount, type, categorySelection, accountSelection, System.currentTimeMillis(), note); onDismiss() }
                }) { Text(Localization.getString("save", language)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Localization.getString("cancel", language)) } }
    )
}

// ─── Thermal Receipt Card ─────────────────────────────────────────────────────

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
        title = {
            Text(if (showDraftReview) Localization.getString("ocr_draft_label", language) else Localization.getString("scan_receipt", language))
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
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
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            // ── Real Camera Preview ──
                            if (hasCameraPermission) {
                                item {
                                    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        CameraPreviewContainer(onImageCaptureReady = { imageCaptureRef = it })
                                        Row(
                                            modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
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
                                                                "Lỗi: ${e.message}\nKiểm tra lại thủ công."
                                                            else "Error: ${e.message}\nPlease review manually."
                                                            // Vẫn chuyển sang draft dù lỗi,
                                                            // để camera không hiện lại
                                                            showDraftReview = true
                                                        } finally {
                                                            isOcrRunning = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.height(44.dp)
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text(Localization.getString("take_photo", language), fontSize = 12.sp)
                                            }
                                            // Gallery picker → OCR thật
                                            Button(
                                                onClick = { galleryLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.height(44.dp)
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (language == AppLanguage.VIETNAMESE) "Thư viện" else "Gallery", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    // No camera permission: show gallery only
                                    Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (language == AppLanguage.VIETNAMESE) "Chọn ảnh hóa đơn từ thư viện (OCR thật)" else "Pick receipt from gallery (Real OCR)")
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
