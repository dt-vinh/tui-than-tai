package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.phuongnn14.tuithantai.ocr.AmountCandidate
import com.phuongnn14.tuithantai.ocr.DocumentType
import com.phuongnn14.tuithantai.ocr.LineItem
import com.phuongnn14.tuithantai.ocr.OcrResult
import com.phuongnn14.tuithantai.ocr.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini Vision fallback for receipt analysis.
 *
 * Called only when local OCR confidence is below threshold (< 0.55)
 * or total_amount is null after local parsing.
 *
 * Uses gemini-2.0-flash-lite (cheapest vision model) — sufficient for
 * receipt text extraction. Upgrade to gemini-1.5-pro for difficult images.
 *
 * The prompt instructs Gemini to follow the casebook rules:
 *  - Return strict JSON (no markdown)
 *  - No hallucinated names
 *  - Select TỔNG CỘNG / TỔNG THANH TOÁN as total, not Khách đưa / Tiền thừa
 *  - Category by line items > merchant > context
 */
class GeminiReceiptAnalyzer(private val apiKey: String) {

    private val TAG = "GeminiOCR"
    private val apiKeys: List<String> = apiKey
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    /** Returns null if API key is blank, request fails, or JSON is invalid. */
    suspend fun analyze(bitmap: Bitmap): OcrResult? {
        if (apiKeys.isEmpty()) {
            Log.w(TAG, "Gemini API key not set - skipping fallback")
            return null
        }
        return withContext(Dispatchers.IO) {
            for ((index, key) in apiKeys.withIndex()) {
                val result = runCatching { callGemini(bitmap, key) }
                    .onFailure { Log.e(TAG, "Gemini key ${index + 1} failed: ${it.message}") }
                    .getOrNull()
                if (result != null) return@withContext result
            }
            null
        }
    }

    private fun callGemini(bitmap: Bitmap, apiKey: String): OcrResult {
        val jpegBase64 = bitmapToBase64(bitmap, maxSide = 1280)

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        // Image part
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", jpegBase64)
                            })
                        })
                        // Text prompt part
                        put(JSONObject().apply {
                            put("text", SYSTEM_PROMPT)
                        })
                    })
                })
            })
            // Force JSON output
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.0)
            })
        }

        val start = System.currentTimeMillis()
        val url = URL("$ENDPOINT?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")

        conn.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val text = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            error("Gemini HTTP $code: $err")
        }
        val elapsed = System.currentTimeMillis() - start
        Log.d(TAG, "Gemini response in ${elapsed}ms, body length=${text.length}")

        return parseGeminiResponse(text)
    }

    private fun parseGeminiResponse(responseText: String): OcrResult {
        val root = JSONObject(responseText)
        val candidates = root.getJSONArray("candidates")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val jsonText = parts.getJSONObject(0).getString("text")
            .trimIndent()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val j = JSONObject(jsonText)

        val docType = when (j.optString("document_type")) {
            "pos_receipt" -> DocumentType.POS_RECEIPT
            "temporary_receipt" -> DocumentType.TEMPORARY_RECEIPT
            "e_invoice" -> DocumentType.E_INVOICE
            "payment_confirmation" -> DocumentType.PAYMENT_CONFIRMATION
            "utility_bill" -> DocumentType.UTILITY_BILL
            "statement" -> DocumentType.STATEMENT
            else -> DocumentType.NOT_RECEIPT
        }
        val txType = when (j.optString("transaction_type")) {
            "income" -> TransactionType.INCOME
            "expense" -> TransactionType.EXPENSE
            else -> TransactionType.UNKNOWN
        }

        val items = mutableListOf<LineItem>()
        j.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val name = item.optString("name", "")
                // Enforce no-hallucination: reject placeholder names
                if (name in FORBIDDEN_NAMES) continue
                items.add(
                    LineItem(
                        name = name,
                        quantity = item.optDouble("quantity").takeIf { !it.isNaN() },
                        unitPrice = item.optDouble("unit_price").takeIf { !it.isNaN() },
                        lineTotal = item.optDouble("line_total").takeIf { !it.isNaN() },
                        confidence = item.optDouble("confidence", 0.9).toFloat().toDouble(),
                        needsReview = item.optBoolean("needs_review", false),
                        rawText = item.optString("raw_text", "")
                    )
                )
            }
        }

        val reviewFields = mutableListOf<String>()
        j.optJSONArray("review_fields")?.let { arr ->
            for (i in 0 until arr.length()) reviewFields.add(arr.getString(i))
        }

        val totalAmount = j.optDouble("total_amount").takeIf { !it.isNaN() && it > 0 }
        val confidence = j.optDouble("confidence", 0.85)
        val needsReview = j.optBoolean("needs_user_review", false) ||
                          totalAmount == null ||
                          docType == DocumentType.STATEMENT

        return OcrResult(
            documentType = docType,
            transactionType = txType,
            merchantName = j.optString("merchant_name").takeIf { it.isNotBlank() && it !in FORBIDDEN_NAMES },
            counterpartyName = j.optString("counterparty_name").takeIf { it.isNotBlank() },
            categoryId = j.optString("category_id", "other").ifBlank { "other" },
            currency = j.optString("currency", "VND").ifBlank { "VND" },
            totalAmount = totalAmount,
            amountCandidates = emptyList(),
            items = items,
            dateTime = j.optString("date_time").takeIf { it.isNotBlank() },
            paymentMethod = j.optString("payment_method").takeIf { it.isNotBlank() },
            confidence = confidence,
            needsUserReview = needsReview,
            reviewFields = reviewFields,
            reason = j.optString("reason", "Analyzed by Gemini Vision")
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap, maxSide: Int): String {
        val scaled = if (maxOf(bitmap.width, bitmap.height) > maxSide) {
            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

        private val FORBIDDEN_NAMES = setOf(
            "Không xác định", "Khác", "Hàng hóa", "Vật phẩm",
            "Sản phẩm", "Receipt", "Bill", "Item", "Product"
        )

        /**
         * System prompt following the OCR casebook rules.
         * Gemini returns strict JSON only — no markdown wrapper.
         */
        private val SYSTEM_PROMPT = """
You are the OCR reasoning layer for the Vietnamese personal finance app "Tui Than Tai".

TASK: Extract the monetary amount and context from ANY image the user photographs — receipts, invoices, payment screenshots, banknotes, price tags, e-commerce order screens, bank transfer confirmations, utility bills, etc.

RULES:
1. Return strict JSON only — no markdown, no explanation, no code block.
2. Never hallucinate product names. If a name is unclear, set name="" and needs_review=true.
3. Never fill item names with: "Không xác định", "Khác", "Hàng hóa", "Vật phẩm", "Sản phẩm", "Receipt", "Bill".
4. Select FINAL PAYABLE AMOUNT as total_amount. Do NOT select: cash received (Tiền nhận/Khách đưa), change (Tiền thừa), VAT only, discount, subtotal, account balance, order ID, invoice number, serial number, phone number, or time.
5. Priority total keywords: TỔNG CỘNG, TỔNG THANH TOÁN, TỔNG SỐ TIỀN, TOTAL, GRAND TOTAL, AMOUNT DUE, PHẢI TRẢ.
6. For VND: output integer without decimal (e.g. 40000 not 40000.0).
7. For USD: output with 2 decimal places (e.g. 12.50).
8. Category classification: line items 70% weight > merchant 20% > document title 10%.
9. If merchant is a sports venue but items are food/drink → category = food_and_drink.
10. BANKNOTE / CURRENCY NOTE: If the image is a physical banknote or currency note (e.g. 5000đ, 10000đ, 100 USD bill), use document_type = "payment_confirmation", total_amount = the face value printed on the note (e.g. 10000 for a 10,000 VND note), transaction_type = "expense", category_id = "other", needs_user_review = false.
11. PRICE TAG / PRODUCT LABEL: If image shows a price tag or product label with a single price, use document_type = "pos_receipt", total_amount = that price, needs_user_review = false.
12. E-COMMERCE ORDER SCREENSHOT (Shopee, Lazada, Tiki, Amazon, etc.): total_amount = "Tổng số tiền" / "Order total" / final checkout amount — NOT the original/crossed-out price, NOT per-item price.
13. PAYMENT APP SCREENSHOT (MoMo, ZaloPay, VNPay, banking app): total_amount = the transferred/paid amount, NOT the account balance, NOT the reference number.
14. If image is completely unrelated to money (landscape photo, selfie, document with no numbers) → document_type = "not_receipt", total_amount = null, needs_user_review = true.
15. Payroll sheets, salary documents → document_type = "statement", needs_user_review = true.

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
        """.trimIndent()
    }
}
