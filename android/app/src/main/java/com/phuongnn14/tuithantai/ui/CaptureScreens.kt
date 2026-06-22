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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .padding(bottom = 144.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "H\u1ee7y", tint = ScanGreenDark)
                            }
                            Text("Qu\u00e9t AI", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ScanGreenDark)
                            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Ch\u1ecdn \u1ea3nh", tint = ScanGreenDark)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Ch\u1ee5p ho\u1eb7c ch\u1ecdn h\u00f3a \u0111\u01a1n \u0111\u1ec3 AI ph\u00e2n t\u00edch n\u1ed9i dung",
                            color = ScanMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(330.dp)
                                        .clip(RoundedCornerShape(18.dp))
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
                                            Text("C\u1ea7n quy\u1ec1n camera", color = Color.White)
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
                                                Text("\u0110ang qu\u00e9t...", color = Color.White, fontWeight = FontWeight.Bold)
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
                                                Text("T\u1ef1 \u0111\u1ed9ng nh\u1eadn di\u1ec7n", color = ScanGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = ScanShape
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ScanGreen)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Ch\u1ecdn t\u1eeb th\u01b0 vi\u1ec7n", color = ScanGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Text("Nh\u1eadp v\u0103n b\u1ea3n giao d\u1ecbch t\u1ef1 ch\u1ecdn:", fontWeight = FontWeight.Bold, color = ScanInk, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = manualText,
                                    onValueChange = { manualText = it },
                                    modifier = Modifier.fillMaxWidth().height(86.dp),
                                    placeholder = { Text("V\u00ed d\u1ee5: chuy\u1ec3n kho\u1ea3n MB Bank 50.000\u0111", fontSize = 13.sp) },
                                    supportingText = { Text("${manualText.length}/200", fontSize = 11.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                    maxLines = 3
                                )
                                Button(
                                    onClick = {
                                        val trimmed = manualText.trim()
                                        if (trimmed.isNotEmpty()) finishWithRawText(trimmed)
                                    },
                                    enabled = manualText.isNotBlank(),
                                    modifier = Modifier.align(Alignment.End).height(40.dp),
                                    shape = ScanShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreen)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ph\u00e2n t\u00edch", fontSize = 13.sp)
                                }

                                errorText?.let { WarningStrip(text = it) }
                            }
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
                                errorText = "Kh\u00f4ng ch\u1ee5p \u0111\u01b0\u1ee3c \u1ea3nh. H\u00e3y th\u1eed l\u1ea1i ho\u1eb7c ch\u1ecdn t\u1eeb th\u01b0 vi\u1ec7n."
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = hasCameraPermission && !isProcessing,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 58.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = ScanShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenDark)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Ch\u1ee5p \u1ea3nh h\u00f3a \u0111\u01a1n", fontWeight = FontWeight.Bold)
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
        Surface(modifier = Modifier.fillMaxSize(), color = ScanCanvas) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "H\u1ee7y", tint = ScanGreenDark)
                    }
                    Text("Ch\u1ee5p v\u1eadt th\u1ec3", color = ScanGreenDark, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(10.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            CaptureOnlyCameraPreview(onImageCaptureReady = { imageCapture = it })
                        } else {
                            Text("C\u1ea7n quy\u1ec1n camera", color = Color.White)
                        }
                        if (isProcessing) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(Modifier.height(10.dp))
                                    Text("\u0110ang ph\u00e2n t\u00edch...", color = Color.White)
                                }
                            }
                        }
                    }
                }

                errorText?.let { WarningStrip(text = it) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ScanShape
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Th\u01b0 vi\u1ec7n")
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
                                    errorText = "Kh\u00f4ng ch\u1ee5p \u0111\u01b0\u1ee3c \u1ea3nh."
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = hasCameraPermission && !isProcessing,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ScanShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ScanGreenDark)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ch\u1ee5p")
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
                ?: if (result.mode == CaptureMode.OBJECT_CAPTURE) "" else "Giao d\u1ecbch qu\u00e9t AI"
        )
    }
    var category by remember(result) { mutableStateOf(result.categoryName ?: "Kh\u00e1c") }
    var account by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.name ?: "Ti\u1ec1n m\u1eb7t") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var previewBitmap by remember(result.sourceImageUri) { mutableStateOf<Bitmap?>(null) }

    val currentCategories = remember(categories, txTypeValue, category) {
        val values = categories.filter { it.type == txTypeValue }.map { it.name }
        (listOf(category) + values + "Kh\u00e1c").distinct().filter { it.isNotBlank() }
    }

    LaunchedEffect(result.sourceImageUri) {
        previewBitmap = result.sourceImageUri?.let { loadBitmap(context, Uri.parse(it)) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = ScanCanvas) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "H\u1ee7y", tint = ScanGreenDark)
                        }
                        Text("Duy\u1ec7t giao d\u1ecbch", color = ScanGreenDark, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Qu\u00e9t l\u1ea1i", tint = ScanGreenDark)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE9EEE8)),
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
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ScanGreen, modifier = Modifier.size(38.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (result.confidence >= 0.75f) "AI \u0111\u00e3 \u0111i\u1ec1n nh\u00e1p" else "C\u1ea7n ki\u1ec3m tra l\u1ea1i",
                                    color = if (result.confidence >= 0.75f) ScanGreen else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = formatVndInput(it); inlineError = null },
                                    label = { Text("S\u1ed1 ti\u1ec1n") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                                )
                                FilledTonalButton(
                                    onClick = { date = System.currentTimeMillis() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ScanShape
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ng\u00e0y ${formatDate(date)}", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (result.confidence < 0.75f || result.amount == null) {
                                WarningStrip("AI ch\u01b0a \u0111\u1ee7 ch\u1eafc v\u1ec1 s\u1ed1 ti\u1ec1n. H\u00e3y ki\u1ec3m tra tr\u01b0\u1edbc khi l\u01b0u.")
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; inlineError = null },
                                label = { Text("S\u1ea3n ph\u1ea9m") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )

                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Danh m\u1ee5c") },
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
                                    label = { Text("T\u00e0i kho\u1ea3n") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(text = { Text(acc.name) }, onClick = { account = acc.name; accountExpanded = false })
                                    }
                                }
                            }

                            result.rawOcrText?.takeIf { it.isNotBlank() }?.let { raw ->
                                RawOcrPreview(rawText = raw)
                            }

                            inlineError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                            HorizontalDivider()
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f).height(48.dp), shape = ScanShape) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Qu\u00e9t l\u1ea1i")
                                }
                                Button(
                                    onClick = {
                                        val amount = parseVndInput(amountText)
                                        when {
                                            amount <= 0.0 -> inlineError = "Vui l\u00f2ng nh\u1eadp s\u1ed1 ti\u1ec1n ch\u00ednh x\u00e1c."
                                            title.isBlank() -> inlineError = "Vui l\u00f2ng nh\u1eadp s\u1ea3n ph\u1ea9m."
                                            account.isBlank() -> inlineError = "Vui l\u00f2ng ch\u1ecdn t\u00e0i kho\u1ea3n."
                                            else -> onConfirm(title.trim(), amount, txTypeValue, category, account, date, result.rawOcrText.orEmpty(), result.sourceImageUri)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = ScanShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanGreenDark)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("L\u01b0u")
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
