package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.phuongnn14.tuithantai.ocr.AmountCandidate
import com.phuongnn14.tuithantai.ocr.DocumentType
import com.phuongnn14.tuithantai.ocr.LineItem
import com.phuongnn14.tuithantai.ocr.OcrResult
import com.phuongnn14.tuithantai.ocr.TransactionType
import com.phuongnn14.tuithantai.capture.ReceiptTableInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini Vision semantic layer for receipt analysis.
 * Receives image pixels plus the on-device OCR table and uses Gemini 3.6 Flash
 * to reconstruct multilingual receipt rows before selecting the payable total.
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
    suspend fun analyze(bitmap: Bitmap, rawOcrText: String = ""): OcrResult? {
        if (apiKeys.isEmpty()) {
            Log.w(TAG, "Gemini API key not set - skipping fallback")
            return null
        }
        return withContext(Dispatchers.IO) {
            keyLoop@ for ((index, key) in apiKeys.withIndex()) {
                for (attempt in 0..1) {
                    try {
                        return@withContext callGemini(bitmap, rawOcrText, key)
                    } catch (error: Exception) {
                        Log.e(TAG, "Gemini key ${index + 1}, attempt ${attempt + 1} failed: ${error.message}")
                        if (!isRetryable(error) || attempt == 1) continue@keyLoop
                    }
                }
            }
            null
        }
    }

    private fun callGemini(bitmap: Bitmap, rawOcrText: String, apiKey: String): OcrResult {
        val jpegBase64 = bitmapToBase64(bitmap, maxSide = 1800)
        val receiptTable = ReceiptTableInterpreter.toMarkdown(rawOcrText)
        val prompt = "$SYSTEM_PROMPT\n\nML KIT OCR RECONSTRUCTED AS MARKDOWN TABLE:\n$receiptTable"

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
                            put("text", prompt)
                        })
                    })
                })
            })
            // Force JSON output
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
            })
        }

        val start = System.currentTimeMillis()
        val url = URL(ENDPOINT)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", apiKey)

        conn.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val text = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw GeminiHttpException(code, "Gemini HTTP $code: $err")
        }
        val elapsed = System.currentTimeMillis() - start
        Log.d(TAG, "Gemini response in ${elapsed}ms, body length=${text.length}")

        return parseGeminiResponse(text, rawOcrText)
    }

    private fun isRetryable(error: Exception): Boolean =
        error !is GeminiHttpException || error.statusCode == 429 || error.statusCode >= 500

    private class GeminiHttpException(val statusCode: Int, message: String) : IOException(message)

    private fun parseGeminiResponse(responseText: String, rawOcrText: String): OcrResult {
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

        val semanticFallback = ReceiptTableInterpreter.resolveFinalAmount(rawOcrText)?.amount?.toDouble() ?: 0.0
        val totalAmount = j.optDouble("total_amount").takeIf { !it.isNaN() && it > 0 }
            ?: semanticFallback
        val confidence = j.optDouble("confidence", 0.85)

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
            needsUserReview = false,
            reviewFields = emptyList(),
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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"

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

TASK: Read the image and the supplied ML Kit Markdown table together. Reconstruct the receipt row-by-row and column-by-column before selecting the final monetary amount.

RULES:
1. Return strict JSON only — no markdown, no explanation, no code block.
2. Never hallucinate product names. If a name is unclear, set name="" and needs_review=true.
3. Never fill item names with: "Không xác định", "Khác", "Hàng hóa", "Vật phẩm", "Sản phẩm", "Receipt", "Bill".
4. Interpret each OCR line as one table row. Product rows may contain product name, discount, unit price and line total. Keep those monetary cells attached to the same row.
5. Select FINAL PAYABLE AMOUNT by meaning, not confidence scoring. Final labels include: TIỀN CẦN THANH TOÁN, TỔNG THANH TOÁN, THANH TOÁN, TỔNG CỘNG, PHẢI TRẢ, TỔNG TIỀN, GRAND TOTAL, AMOUNT DUE.
5a. Apply the same semantic rule in every language. Examples: 应付金额/实付金额/总计 (Chinese), お支払金額/合計 (Japanese), 결제금액/합계 (Korean), कुल देय/देय राशि (Hindi), total a pagar, net à payer, zu zahlen, totale da pagare, valor total, jumlah bayar, ยอดชำระ/ยอดสุทธิ.
5b. TỔNG TIỀN HÀNG and THÀNH TIỀN are SUBTOTAL labels. When a receipt later applies discount, VAT, service charge or another adjustment, never return that subtotal; return the final payable row after the adjustments. Example: `Tổng tiền hàng 395,000`, `VAT 31,600`, `Tổng cộng 426,600` means total_amount=426600.
6. On a total row with multiple monetary cells, negative values are discounts/adjustments and the LAST POSITIVE monetary cell is the payable total. Example: `TỔNG TIỀN HÀNG | -32.000 | 666.100` means total_amount=666100, never 32000.
7. Do NOT select: cash received (Tiền nhận/Khách đưa), change (Tiền thừa), VAT only, discount, account balance, order ID, invoice number, serial number, phone number, quantity, date or time.
7a. Discount/change exclusions also apply across languages, including 折扣/优惠/找零, 値引/割引/お釣り, 할인/거스름돈, छूट, descuento/cambio, remise/monnaie, Rabatt/Rückgeld, desconto/troco, ส่วนลด/เงินทอน.
8. Always return a numeric total_amount. If no monetary value exists anywhere, return 0. Never return null.
9. For VND: output integer without decimal (e.g. 40000 not 40000.0).
10. For USD: output with 2 decimal places (e.g. 12.50).
11. Category classification: line items 70% weight > merchant 20% > document title 10%.
12. If merchant is a sports venue but items are food/drink → category = food_and_drink.
13. BANKNOTE / CURRENCY NOTE: If the image is a physical banknote or currency note, use the face value printed on the note.
14. PRICE TAG / PRODUCT LABEL: use the clearly displayed selling price.
15. E-COMMERCE ORDER SCREENSHOT: use the final checkout amount, not original/crossed-out or per-item price.
16. PAYMENT APP SCREENSHOT: use the transferred/paid amount, not account balance or reference number.

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
