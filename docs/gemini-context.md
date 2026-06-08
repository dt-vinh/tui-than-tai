# Tui Than Tai — Codebase Context for Gemini

## Tổng quan dự án

Ứng dụng Android quản lý tài chính cá nhân (Kotlin + Jetpack Compose).
Tính năng cốt lõi: chụp ảnh hóa đơn/bill/chuyển khoản → OCR → tự động điền form chi tiêu.

**Stack:**
- Android Kotlin + Jetpack Compose + Material3
- CameraX (chụp ảnh)
- MLKit Text Recognition (OCR local)
- Gemini Vision API (OCR fallback)
- Room Database
- JUnit unit tests (pure JVM)

---

## Luồng OCR chính

```
Camera → chụp ảnh → ExpenseAnalyzer.analyze()
    → OcrEngineSelector.recognize()   // MLKit hoặc RapidOCR
    → OcrAnalyzer.analyze(rawText)    // parse text → OcrResult
    → if (confidence < 0.55 || totalAmount == null):
          GeminiReceiptAnalyzer.analyze(bitmap)  // Gemini Vision fallback
    → ExpenseSuggestion → fillDraftForm
```

---

## File: ExpenseAnalyzer.kt

```kotlin
class ExpenseAnalyzer(private val engineSelector: OcrEngineSelector? = null) {
    companion object {
        private const val GEMINI_FALLBACK_THRESHOLD = 0.55
    }
    private val ocrAnalyzer = OcrAnalyzer()
    private val gemini by lazy { GeminiReceiptAnalyzer(BuildConfig.GEMINI_API_KEY) }

    suspend fun analyze(context: Context, imageUri: Uri, categories: List<CategoryEntity>): ExpenseSuggestion {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
            ?: return ExpenseSuggestion(needsReview = true)

        val engineResult = OcrEngineSelector(context).recognize(bitmap)
        val result = ocrAnalyzer.analyze(engineResult.rawText)

        val finalResult = if (result.totalAmount == null || result.confidence < GEMINI_FALLBACK_THRESHOLD) {
            gemini.analyze(bitmap) ?: result
        } else result

        return ExpenseSuggestion(
            title = finalResult.merchantName ?: inferTitle(engineResult.rawText, emptyList()),
            amount = finalResult.totalAmount?.toLong() ?: 0L,
            categoryId = finalResult.categoryId,
            ocrText = engineResult.rawText,
            ocrEngine = engineResult.engineName + if (finalResult !== result) "+gemini" else ""
        )
    }
}
```

---

## File: OcrAnalyzer.kt

Parse raw OCR text → OcrResult. Không có Android dependency — unit-testable thuần JVM.

```kotlin
class OcrAnalyzer {

    // Các dòng chứa ID/số tham chiếu, KHÔNG phải số tiền
    private val ID_LINE_TOKENS = setOf(
        "ma giao dich", "transaction id", "so tham chieu", "reference",
        "ma don", "order id", "invoice number", "so hoa don",
        "bien so", "ma kh", "so dien thoai", "phone",
        "tai khoan", "so tai khoan", "so the", "so tk",  // số TK ngân hàng
        "account number", "card number",
        "thoi gian", "trang thai", "ngan hang", "nguoi nhan", "nguoi chuyen",
        "noi dung", "tin nhan"
    )

    private fun buildCandidates(lines: List<String>, currency: MoneyParser.Currency): List<TotalResolver.Candidate> {
        val result = mutableListOf<TotalResolver.Candidate>()
        for ((index, line) in lines.withIndex()) {
            val norm = normalizeText(line)
            if (ID_LINE_TOKENS.any { norm.contains(it) }) continue  // skip ID lines
            val amountStr = extractAmountStringFromLine(line) ?: continue
            val parsed = MoneyParser.parse(amountStr, currency) ?: continue
            val rawDigits = amountStr.filter { it.isDigit() }
            if (rawDigits.length > 10) continue  // >10 chữ số = số TK hoặc mã GD
            result.add(TotalResolver.Candidate(label = line, amount = parsed.amount, lineIndex = index))
        }
        return result
    }

    private fun extractAmountStringFromLine(line: String): String? {
        val match = Regex(
            // QUAN TRỌNG: [đĐdkK] — MLKit thường đọc 'đ' → ASCII 'd'
            // Nếu không có 'd', "9.385.662d" bị lookahead chặn → chỉ match "9.385" → SAI
            """(?<![A-Za-z])([$]?\s*[0-9][0-9OoО,.\s]*[0-9]\s*(?:VND|vnđ|[đĐdkK])?|[0-9]+[Kk])(?![A-Za-z0-9])"""
        ).findAll(line).lastOrNull()
        return match?.value?.trim()
    }
}
```

---

## File: TotalResolver.kt

Chọn số tiền tổng cuối từ danh sách candidates. **4-stage với Currency-First.**

```kotlin
object TotalResolver {
    data class Candidate(val label: String, val amount: Double, val lineIndex: Int = 0)

    private val PRIORITY_LABELS = listOf(
        "tong cong", "tong thanh toan", "tong tien thanh toan",
        "tong tam tinh", "thanh toan", "phai tra",
        "grand total", "amount due", "total"
    )

    private val EXCLUDE_LABELS = setOf(
        "tien hang", "tam tinh", "vat", "thue", "giam gia", "chiet khau",
        "khach dua", "tien thua", "tien thua tra khach",
        "change", "cash", "received", "balance", "so du", "tien mat"
    )

    fun resolve(candidates: List<Candidate>): Double? {
        if (candidates.isEmpty()) return null

        // Detect explicit VND/đ/d currency marker
        val vndTagRe = Regex("""(?:vnd|vnđ|[đd]\b|₫)""", RegexOption.IGNORE_CASE)

        data class Scored(val candidate: Candidate, val priority: Int, val excluded: Boolean, val hasVndTag: Boolean)
        val scored = candidates.map { c ->
            val norm = normalize(c.label)
            val priority = PRIORITY_LABELS.indexOfFirst { norm.contains(it) }
                .let { if (it >= 0) PRIORITY_LABELS.size - it else 0 }
            val excluded = EXCLUDE_LABELS.any { norm.contains(it) }
            val hasVndTag = vndTagRe.containsMatchIn(c.label)
            Scored(c, priority, excluded, hasVndTag)
        }

        // STAGE 0: VND-tagged + priority keyword  ("TỔNG CỘNG 2,000,000đ")
        scored.filter { it.hasVndTag && it.priority > 0 && !it.excluded }
            .maxByOrNull { it.priority }?.let { return it.candidate.amount }

        // STAGE 1: VND-tagged + non-excluded  (số có đ/VND marker > số không có)
        val vndCandidates = scored.filter { it.hasVndTag && !it.excluded }
        if (vndCandidates.isNotEmpty()) {
            vndCandidates.filter { it.priority > 0 }.maxByOrNull { it.priority }
                ?.let { return it.candidate.amount }
            vndCandidates.maxByOrNull { it.candidate.amount }
                ?.let { return it.candidate.amount }
        }

        // STAGE 2: priority label, non-excluded (không cần currency tag)
        scored.filter { it.priority > 0 && !it.excluded }
            .maxByOrNull { it.priority }?.let { return it.candidate.amount }

        // STAGE 3: largest non-excluded
        scored.filter { !it.excluded }.maxByOrNull { it.candidate.amount }
            ?.let { return it.candidate.amount }

        return candidates.maxByOrNull { it.amount }?.amount
    }
}
```

---

## File: MoneyParser.kt

Parse chuỗi số tiền từ OCR text → Long/Double.

```kotlin
object MoneyParser {
    enum class Currency { VND, USD }
    data class AmountResult(val amount: Double, val currency: Currency)

    fun parse(raw: String, defaultCurrency: Currency = Currency.VND): AmountResult? {
        val s = raw.trim()
        if (s.isBlank()) return null
        if (TIME_PATTERN.matches(s)) return null   // "17:45" → null
        if (DATE_PATTERN.matches(s)) return null   // "05/06/2026" → null

        val currency = detectCurrency(s, defaultCurrency)
        var cleaned = s
            .replace(Regex("""(?i)\s*\bVND\b\s*$"""), "")
            .replace(Regex("""\s*[đĐ]\s*$"""), "")
            .replace(Regex("""(?<=[0-9])\s*d\s*$"""), "")  // ASCII d sau chữ số
            .trim()

        val kMultiplier = cleaned.endsWith("K", ignoreCase = true)
        if (kMultiplier) cleaned = cleaned.dropLast(1).trim()
        cleaned = fixOcrDigits(cleaned)  // O→0 trong context số

        return when (currency) {
            Currency.VND -> parseVnd(cleaned, kMultiplier)?.let { AmountResult(it.toDouble(), Currency.VND) }
            Currency.USD -> parseUsd(cleaned)?.let { AmountResult(it, Currency.USD) }
        }
    }

    private fun parseVnd(s: String, kMultiplier: Boolean): Long? {
        val normalized = when {
            s.contains(' ') -> s.replace(" ", "")
            // Dấu chấm nghìn VN: "9.385.662" → last segment "662" = 3 chữ số
            s.contains('.') && s.substringAfterLast('.').length == 3 -> s.replace(".", "")
            // Dấu phẩy nghìn: "9,385,662" → last segment "662" = 3 chữ số
            s.contains(',') && s.substringAfterLast(',').length == 3 -> s.replace(",", "")
            else -> s
        }
        val value = normalized.toLongOrNull() ?: return null
        if (value <= 0) return null
        return if (kMultiplier) value * 1_000L else value
    }
}
```

---

## File: GeminiReceiptAnalyzer.kt — System Prompt

```
You are the OCR reasoning layer for the Vietnamese personal finance app "Tui Than Tai".

TASK: Extract receipt/invoice/payment data from this image.

RULES:
1. Return strict JSON only — no markdown, no explanation, no code block.
2. Never hallucinate product names. If a name is unclear, set name="" and needs_review=true.
3. Never fill item names with: "Không xác định", "Khác", "Hàng hóa", "Vật phẩm".
4. Select FINAL PAYABLE AMOUNT as total_amount.
   DO NOT select: cash received (Khách đưa), change (Tiền thừa), VAT only, discount,
   subtotal, account balance, order ID, invoice number, phone number, or time.
5. Priority total keywords: TỔNG CỘNG, TỔNG THANH TOÁN, TOTAL, GRAND TOTAL, PHẢI TRẢ.
6. For VND: output integer without decimal (e.g. 40000 not 40000.0).
7. For USD: output with 2 decimal places (e.g. 12.50).
8. Category: line items 70% > merchant 20% > document title 10%.
9. NOT a receipt/payment/invoice → document_type="not_receipt", total_amount=null.

CATEGORY VALUES: food_and_drink, coffee, transport, shopping, bills, health, entertainment, travel, other

OUTPUT JSON SCHEMA:
{
  "document_type": "pos_receipt|temporary_receipt|e_invoice|payment_confirmation|utility_bill|statement|not_receipt",
  "transaction_type": "expense|income|unknown",
  "merchant_name": string_or_null,
  "counterparty_name": string_or_null,
  "category_id": string,
  "currency": "VND|USD|unknown",
  "total_amount": number_or_null,
  "items": [{"name": string, "quantity": number_or_null, "unit_price": number_or_null, "line_total": number_or_null, "confidence": 0.0-1.0, "needs_review": boolean, "raw_text": string}],
  "date_time": string_or_null,
  "payment_method": string_or_null,
  "confidence": 0.0-1.0,
  "needs_user_review": boolean,
  "review_fields": [string],
  "reason": string
}
```

---

## Các loại ảnh OCR thường gặp

### 1. MB Bank chuyển khoản
```
Giao dịch thành công
2,000,000 VND
08/06/2026 11:06:58
Đến: DU TIEN VINH
Tài khoản: 9890947259883      ← SỐ TK, KHÔNG phải tiền
Tại: NHTMCP Quân Đội
Nội dung: Nguyen Thi Hong Nhung chuyen khoan nhanh qua Zalo
Số tham chiếu: 0200970488060811065820264kmg666815
```
→ total = 2,000,000 VND (EXPENSE / bank_transfer)

### 2. MoMo nạp tiền Túi Thần Tài
```
NẠP TIỀN VÀO TÚI THẦN TÀI
-9.385.662đ                   ← MLKit đọc thành "-9.385.662d"
Trạng thái    Thành công
Thời gian     20:04 - 01/06/2026
Mã giao dịch  131591237691    ← ID 12 chữ số, KHÔNG phải tiền
Tài khoản/thẻ Ví MoMo
Tổng phí      Miễn phí
```
→ total = 9,385,662 VND (EXPENSE / e_wallet)

### 3. MoMo chuyển đến ngân hàng
```
CHUYỂN ĐẾN NGUYEN MAI MINH THU (VIETINBANK)
-600.000đ
Trạng thái    Thành công
Mã giao dịch  132134503406
Thưởng xu     +100 Xu         ← Xu, KHÔNG phải VND income
Số tiền       600.000đ
```
→ total = 600,000 VND (EXPENSE)

### 4. MoMo tiền lời (income nhỏ)
```
TẶNG THÊM TIỀN LỜI TỪ TÚI+
+1.408đ                       ← income
Mã giao dịch  20682858029
Tài khoản/thẻ Túi Thần Tài
```
→ total = 1,408 VND (INCOME)

---

## Các lỗi OCR thường gặp (MLKit)

| Ký tự thật | MLKit đọc | Ví dụ |
|------------|-----------|-------|
| đ (U+0111) | d (ASCII) | "9.385.662đ" → "9.385.662d" |
| O (chữ O)  | 0 (số 0)  | Ngược lại cũng xảy ra trong số |
| l, I       | 1         | Trong dãy số |
| Ví MoMo   | VÍ MoMo   | Chữ hoa accent |

**QUAN TRỌNG về định dạng số VN:**
- `9.385.662` = 9 triệu 385 nghìn 662 (dấu chấm = phân cách nghìn)
- `600.000` = 600 nghìn (KHÔNG phải 600.0)
- Rule: nếu phần sau dấu chấm cuối có đúng 3 chữ số → dấu chấm nghìn → bỏ hết dấu chấm

---

## OcrResult model

```kotlin
data class OcrResult(
    val documentType: DocumentType,      // pos_receipt, payment_confirmation, ...
    val transactionType: TransactionType, // EXPENSE, INCOME, UNKNOWN
    val merchantName: String?,
    val counterpartyName: String?,
    val categoryId: String,              // food_and_drink, transport, shopping, ...
    val currency: String,                // "VND" hoặc "USD"
    val totalAmount: Double?,            // số tiền chính (null = không tìm được)
    val amountCandidates: List<AmountCandidate>,
    val items: List<LineItem>,
    val dateTime: String?,
    val paymentMethod: String?,          // cash, bank_transfer, e_wallet, ...
    val confidence: Double,              // 0.0 - 1.0
    val needsUserReview: Boolean,
    val reviewFields: List<String>,      // ["total_amount", "merchant_name", ...]
    val reason: String
)
```

---

## Nguyên tắc thiết kế

1. **OCR là gợi ý, không phải auto-save** — luôn cho user review trước khi lưu
2. **Currency-First** — số có VND/đ/d marker được ưu tiên tuyệt đối (tránh nhầm số TK)
3. **Lọc ID dài** — số >10 chữ số liên tiếp = số TK hoặc mã GD, bỏ qua
4. **Gemini fallback** — chỉ gọi khi local confidence < 0.55 hoặc không tìm được total
5. **Không hallucinate** — nếu không chắc, để trống + set needsUserReview=true
