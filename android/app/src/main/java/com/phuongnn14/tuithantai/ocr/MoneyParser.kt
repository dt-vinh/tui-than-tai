package com.phuongnn14.tuithantai.ocr

/**
 * Parses money amounts from OCR text strings.
 * Handles VND (dot/comma/space thousands, K suffix, OCR O→0) and USD (decimal).
 * Returns null for time strings (17:45) and date strings (05/06/2026).
 */
object MoneyParser {

    enum class Currency { VND, USD }

    data class AmountResult(val amount: Double, val currency: Currency)

    private val TIME_PATTERN = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")
    private val DATE_PATTERN = Regex("""^\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}$""")

    fun parse(raw: String, defaultCurrency: Currency = Currency.VND): AmountResult? {
        val s = raw.trim()
        if (s.isBlank()) return null
        if (TIME_PATTERN.matches(s)) return null
        if (DATE_PATTERN.matches(s)) return null

        val currency = detectCurrency(s, defaultCurrency)

        var cleaned = s
            .removePrefix("$")
            .replace(Regex("""(?i)\s*\bUSD\b\s*$"""), "")
            .replace(Regex("""(?i)\s*\bVND\b\s*$"""), "")
            .replace(Regex("""(?i)\s*vnđ\s*$"""), "")
            .replace(Regex("""\s*[đĐ]\s*$"""), "")   // Vietnamese đ
            .replace(Regex("""(?<=[0-9])\s*d\s*$"""), "") // ASCII d after digit (not USD)
            .trim()

        val kMultiplier = cleaned.endsWith("K", ignoreCase = true)
        if (kMultiplier) cleaned = cleaned.dropLast(1).trim()

        cleaned = fixOcrDigits(cleaned)

        return when (currency) {
            Currency.VND -> parseVnd(cleaned, kMultiplier)?.let { AmountResult(it.toDouble(), Currency.VND) }
            Currency.USD -> parseUsd(cleaned)?.let { AmountResult(it, Currency.USD) }
        }
    }

    private fun detectCurrency(raw: String, default: Currency): Currency = when {
        raw.startsWith("$") -> Currency.USD
        raw.contains(Regex("""(?i)\bUSD\b""")) -> Currency.USD
        raw.contains(Regex("""(?i)(VND|vnđ|[đd])\s*$""")) -> Currency.VND
        raw.endsWith("K", ignoreCase = true) -> Currency.VND
        else -> default
    }

    /**
     * Replaces 'O' with '0' when the string is in a numeric context
     * (all chars are digits, commas, dots, spaces, or uppercase O).
     * Handles OCR artifacts like "6,85O,OOO".
     */
    internal fun fixOcrDigits(s: String): String {
        val hasDigit = s.any { it.isDigit() }
        val isNumericContext = s.all { it.isDigit() || it in ",./ " || it == 'O' }
        return if (hasDigit && isNumericContext) s.replace('O', '0') else s
    }

    private fun parseVnd(s: String, kMultiplier: Boolean): Long? {
        // Detect and strip thousands separators
        val normalized = when {
            s.contains(' ') -> s.replace(" ", "")
            // Dot thousands: last segment after dot has exactly 3 digits
            s.contains('.') && s.substringAfterLast('.').length == 3 ->
                s.replace(".", "")
            // Comma thousands: last segment after comma has exactly 3 digits
            s.contains(',') && s.substringAfterLast(',').length == 3 ->
                s.replace(",", "")
            else -> s
        }
        val value = normalized.toLongOrNull() ?: return null
        if (value <= 0) return null
        return if (kMultiplier) value * 1_000L else value
    }

    private fun parseUsd(s: String): Double? {
        // Vietnamese locale: comma as decimal separator (12,50 → 12.50)
        val normalized = if (s.contains(',') && s.substringAfterLast(',').length <= 2) {
            s.replace(',', '.')
        } else {
            s.replace(",", "")
        }
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }
}
