package com.phuongnn14.tuithantai.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.phuongnn14.tuithantai.capture.CaptureMode
import com.phuongnn14.tuithantai.capture.ExpenseCaptureResult
import com.phuongnn14.tuithantai.capture.MlKitObjectClassifier
import com.phuongnn14.tuithantai.capture.MoneyPresenceDetector
import com.phuongnn14.tuithantai.capture.OcrService
import com.phuongnn14.tuithantai.capture.TransactionType as CaptureTransactionType
import com.phuongnn14.tuithantai.data.AccountEntity
import com.phuongnn14.tuithantai.data.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

private val ScanGreen = Color(0xFF006B45)
private val ScanGreenDark = Color(0xFF063D35)
private val ScanCanvas = Color(0xFFF6F8F3)
private val ScanInk = Color(0xFF1B221F)
private val ScanMuted = Color(0xFF7A827D)
private val ScanDanger = Color(0xFFDC656C)
private val ScanDark = Color(0xFF101112)
private val ScanShape = RoundedCornerShape(8.dp)
private val ScanLargeShape = RoundedCornerShape(28.dp)

@Composable
fun MoneyScanCameraScreen(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onResult: (ExpenseCaptureResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrService = remember { OcrService() }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var manualText by remember { mutableStateOf("") }
    var lastRawText by remember { mutableStateOf("") }
    var lastAmount by remember { mutableStateOf<Long?>(null) }
    var consecutiveHits by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    fun finishWithRawText(rawText: String, sourceUri: Uri? = null) {
        val result = MoneyPresenceDetector.detect(rawText, sourceUri?.toString())
            ?: MoneyPresenceDetector.uncertainDraft(rawText, sourceUri?.toString())
        onResult(result)
    }

    fun processUri(uri: Uri) {
        scope.launch {
            isProcessing = true
            errorText = null
            try {
                val rawText = ocrService.recognizeUri(context, uri)
                lastRawText = rawText
                finishWithRawText(rawText, uri)
            } catch (e: Exception) {
                errorText = "Không đọc được ảnh này. Bạn có thể chụp lại hoặc nhập thủ công."
            } finally {
                isProcessing = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processUri(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ScanCanvas) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Hủy", tint = ScanGreenDark)
                    }
                    Text("Quét AI", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ScanGreenDark)
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Chọn ảnh", tint = ScanGreenDark)
                    }
                }

                Text(
                    text = "Chụp hoặc chọn hóa đơn để AI phân tích nội dung",
                    color = ScanMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ScanLargeShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.95f)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasCameraPermission) {
                                        MoneyAnalysisCameraPreview(
                                            ocrService = ocrService,
                                            onImageCaptureReady = { imageCapture = it },
                                            onRawText = { rawText ->
                                                if (completed || isProcessing || rawText.isBlank()) return@MoneyAnalysisCameraPreview
                                                lastRawText = rawText
                                                val result = MoneyPresenceDetector.detect(rawText)
                                                val detectedAmount = result?.amount
                                                if (result != null && detectedAmount != null && result.confidence >= 0.75f) {
                                                    if (lastAmount == detectedAmount) {
                                                        consecutiveHits += 1
                                                    } else {
                                                        lastAmount = detectedAmount
                                                        consecutiveHits = 1
                                                    }
                                                    if (consecutiveHits >= 2) {
                                                        completed = true
                                                        isProcessing = true
                                                        scope.launch {
                                                            delay(350)
                                                            onResult(result)
                                                            isProcessing = false
                                                        }
                                                    }
                                                } else {
                                                    consecutiveHits = 0
                                                }
                                            }
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Cần quyền camera", color = Color.White)
                                        }
                                    }

                                    ScanFrameOverlay()
                                    if (isProcessing) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = Color.White)
                                                Spacer(Modifier.height(10.dp))
                                                Text("Đang quét...", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                                            shape = CircleShape,
                                            color = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ScanGreen, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Tự động nhận diện", color = ScanGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = ScanShape
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ScanGreen)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Chọn từ thư viện", color = ScanGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Nhập văn bản giao dịch tự chọn:", fontWeight = FontWeight.Bold, color = ScanInk)
                                    OutlinedTextField(
                                        value = manualText,
                                        onValueChange = { manualText = it },
                                        modifier = Modifier.fillMaxWidth().height(132.dp),
                                        placeholder = { Text("Ví dụ: chuyển khoản MB Bank 50.000đ") },
                                        supportingText = { Text("${manualText.length}/200") },
                                        maxLines = 5
                                    )
                                    Button(
                                        onClick = {
                                            val trimmed = manualText.trim()
                                            if (trimmed.isNotEmpty()) finishWithRawText(trimmed)
                                        },
                                        enabled = manualText.isNotBlank(),
                                        modifier = Modifier.align(Alignment.End),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreen)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Phân tích")
                                    }
                                }

                                errorText?.let {
                                    WarningStrip(text = it)
                                }

                                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                                    Text("Hủy bỏ", color = ScanGreenDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    if (lastRawText.isNotBlank()) {
                        item {
                            RawOcrPreview(rawText = lastRawText)
                        }
                    }
                }

                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        scope.launch {
                            isProcessing = true
                            errorText = null
                            try {
                                val uri = capturePhoto(context, capture, "money_scan")
                                val rawText = ocrService.recognizeUri(context, uri)
                                lastRawText = rawText
                                finishWithRawText(rawText, uri)
                            } catch (e: Exception) {
                                errorText = "Không chụp được ảnh. Hãy thử lại hoặc chọn từ thư viện."
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = hasCameraPermission && !isProcessing,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreen)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Chụp ảnh hóa đơn", fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ObjectCaptureCameraScreen(
    transactionType: CaptureTransactionType,
    onDismiss: () -> Unit,
    onResult: (ExpenseCaptureResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrService = remember { OcrService() }
    val classifier = remember { MlKitObjectClassifier() }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    fun processUri(uri: Uri) {
        scope.launch {
            isProcessing = true
            errorText = null
            try {
                val rawText = runCatching { ocrService.recognizeUri(context, uri) }.getOrDefault("")
                val bitmap = loadBitmap(context, uri)
                val objectResult = bitmap?.let { classifier.classify(it) }
                val moneyResult = MoneyPresenceDetector.detect(rawText, uri.toString())
                val amount = moneyResult?.amount.takeIf { moneyResult?.confidence != null && moneyResult.confidence >= 0.75f }
                val productNote = objectResult?.productNote
                    ?: moneyResult?.productNote
                    ?: if (rawText.isBlank()) "" else rawText.lineSequence().firstOrNull()?.take(48)
                val category = CategoryResolverBridge.resolve(rawText, objectResult?.productNote)

                onResult(
                    ExpenseCaptureResult(
                        mode = CaptureMode.OBJECT_CAPTURE,
                        transactionType = transactionType,
                        amount = amount,
                        productNote = productNote,
                        merchantName = moneyResult?.merchantName,
                        categoryName = category,
                        confidence = maxOf(objectResult?.confidence ?: 0f, moneyResult?.confidence ?: 0f),
                        rawOcrText = rawText,
                        sourceImageUri = uri.toString(),
                        needsReview = true
                    )
                )
            } catch (e: Exception) {
                errorText = "Không phân tích được ảnh vật thể. Hãy chụp lại hoặc nhập thủ công."
            } finally {
                isProcessing = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processUri(it) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = ScanDark) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Hủy", tint = Color.White)
                    }
                    Text("Chụp vật thể", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CaptureOnlyCameraPreview(onImageCaptureReady = { imageCapture = it })
                    } else {
                        Text("Cần quyền camera", color = Color.White)
                    }
                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(10.dp))
                            Text("Đang phân tích...", color = Color.White)
                        }
                    }
                }

                errorText?.let { WarningStrip(text = it) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thư viện")
                    }
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            scope.launch {
                                isProcessing = true
                                errorText = null
                                try {
                                    val uri = capturePhoto(context, capture, "object_capture")
                                    processUri(uri)
                                } catch (e: Exception) {
                                    errorText = "Không chụp được ảnh."
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = hasCameraPermission && !isProcessing,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ScanDanger)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chụp")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureResultConfirmationScreen(
    result: ExpenseCaptureResult,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        accountName: String,
        date: Long,
        note: String,
        imageUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val txTypeValue = result.transactionType.name
    var amountText by remember(result) { mutableStateOf(result.amount?.let { formatVndInput(it.toString()) } ?: "") }
    var title by remember(result) {
        mutableStateOf(
            result.productNote
                ?: result.merchantName
                ?: if (result.mode == CaptureMode.OBJECT_CAPTURE) "" else "Giao dịch quét AI"
        )
    }
    var category by remember(result) { mutableStateOf(result.categoryName ?: "Khác") }
    var account by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.name ?: "Tiền mặt") }
    var note by remember(result) { mutableStateOf(result.rawOcrText.orEmpty()) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var previewBitmap by remember(result.sourceImageUri) { mutableStateOf<Bitmap?>(null) }

    val currentCategories = remember(categories, txTypeValue, category) {
        val values = categories.filter { it.type == txTypeValue }.map { it.name }
        (listOf(category) + values + "Khác").distinct().filter { it.isNotBlank() }
    }

    LaunchedEffect(result.sourceImageUri) {
        previewBitmap = result.sourceImageUri?.let { loadBitmap(context, Uri.parse(it)) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = ScanDark) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) { Text("Hủy", color = Color.White.copy(alpha = 0.8f)) }
                        Text("Xác nhận giao dịch", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Chụp lại", tint = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF202226)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bitmap = previewBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(72.dp))
                        }

                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xBB5B373A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val signedPrefix = if (txTypeValue == "EXPENSE") "-" else "+"
                                val amountValue = parseVndInput(amountText)
                                Text(
                                    text = if (amountValue > 0.0) "$signedPrefix ${formatMoney(amountValue, result.currency, language)}" else "Cần nhập số tiền",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = title.ifBlank { "Chưa có ghi chú" },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AssistChipLike(icon = Icons.Default.AutoAwesome, label = if (result.confidence >= 0.75f) "Tự động nhận diện" else "Cần kiểm tra lại")
                        AssistChipLike(icon = Icons.Default.Restaurant, label = category)
                        AssistChipLike(icon = Icons.Default.AccountBalanceWallet, label = account)
                    }
                }

                item {
                    Card(
                        shape = ScanShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (result.confidence < 0.75f || result.amount == null) {
                                WarningStrip("AI chưa đủ chắc về số tiền. Hãy kiểm tra trước khi lưu.")
                            }

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = formatVndInput(it); inlineError = null },
                                label = { Text("Số tiền") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                            )
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; inlineError = null },
                                label = { Text("Ghi chú / sản phẩm") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )

                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Danh mục") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                    currentCategories.forEach { value ->
                                        DropdownMenuItem(text = { Text(value) }, onClick = { category = value; categoryExpanded = false })
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = !accountExpanded }) {
                                OutlinedTextField(
                                    value = account,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ví") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(text = { Text(acc.name) }, onClick = { account = acc.name; accountExpanded = false })
                                    }
                                }
                            }

                            FilledTonalButton(onClick = { date = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hôm nay - ${formatDate(date)}")
                            }

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Ghi chú chi tiết / raw OCR") },
                                modifier = Modifier.fillMaxWidth().height(118.dp),
                                maxLines = 5
                            )

                            result.rawOcrText?.takeIf { it.isNotBlank() }?.let { raw ->
                                RawOcrPreview(rawText = raw)
                            }

                            inlineError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                            HorizontalDivider()
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f).height(52.dp), shape = CircleShape) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chụp lại")
                                }
                                Button(
                                    onClick = {
                                        val amount = parseVndInput(amountText)
                                        when {
                                            amount <= 0.0 -> inlineError = "Vui lòng nhập số tiền chính xác."
                                            title.isBlank() -> inlineError = "Vui lòng nhập ghi chú/sản phẩm."
                                            account.isBlank() -> inlineError = "Vui lòng chọn ví."
                                            else -> onConfirm(title.trim(), amount, txTypeValue, category, account, date, note, result.sourceImageUri)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanDanger)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Xác nhận")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneyAnalysisCameraPreview(
    ocrService: OcrService,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onRawText: (String) -> Unit
) {
    CameraPreview(
        onImageCaptureReady = onImageCaptureReady,
        ocrService = ocrService,
        onRawText = onRawText
    )
}

@Composable
private fun CaptureOnlyCameraPreview(onImageCaptureReady: (ImageCapture) -> Unit) {
    CameraPreview(onImageCaptureReady = onImageCaptureReady, ocrService = null, onRawText = {})
}

@Composable
private fun CameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit,
    ocrService: OcrService?,
    onRawText: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnRawText by rememberUpdatedState(onRawText)
    val latestOnImageCaptureReady by rememberUpdatedState(onImageCaptureReady)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analysisBusy = remember { AtomicBoolean(false) }
    val lastAnalysisAt = remember { AtomicLong(0L) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                latestOnImageCaptureReady(imageCapture)

                val useCases = mutableListOf(preview, imageCapture)
                if (ocrService != null) {
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysisAt.get() < 650L || !analysisBusy.compareAndSet(false, true)) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalysisAt.set(now)
                        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val raw = ocrService.recognizeImageProxy(imageProxy)
                                if (raw.isNotBlank()) latestOnRawText(raw)
                            } catch (_: Exception) {
                            } finally {
                                imageProxy.close()
                                analysisBusy.set(false)
                            }
                        }
                    }
                    useCases += imageAnalysis
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases.toTypedArray()
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
private fun ScanFrameOverlay() {
    Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Transparent)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .background(ScanDanger.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun WarningStrip(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color(0xFFE65100), fontSize = 12.sp)
        }
    }
}

@Composable
private fun RawOcrPreview(rawText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ScanShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAF8)),
        border = BorderStroke(1.dp, Color(0xFFE0E6E0))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ScanGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Text OCR đọc được", fontWeight = FontWeight.Bold, color = ScanInk)
            }
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 170.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = rawText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = ScanInk
                )
            }
        }
    }
}

@Composable
private fun AssistChipLike(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(shape = CircleShape, color = Color(0xFF183424), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private suspend fun capturePhoto(context: Context, imageCapture: ImageCapture, prefix: String): Uri {
    val photoFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    return suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        photoFile
                    )
                    if (cont.isActive) cont.resume(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.cancel(exception)
                }
            }
        )
    }
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

private object CategoryResolverBridge {
    fun resolve(rawOcrText: String?, objectHint: String?): String =
        com.phuongnn14.tuithantai.capture.CategoryResolver.resolve(rawOcrText, objectHint)
}
