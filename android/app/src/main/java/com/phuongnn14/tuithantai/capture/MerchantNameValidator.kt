package com.phuongnn14.tuithantai.capture

/** Rejects document headers and address lines that are often mistaken for merchant names. */
object MerchantNameValidator {
    private val documentTitles = listOf(
        "hoa don", "phieu tinh tien", "phieu tam tinh", "invoice", "receipt", "bill"
    )
    private val addressPrefixes = listOf(
        "dia chi", "address", "dc:", "d/c", "lo ", "ngo ", "duong ", "street ", "road "
    )
    private val leadingHouseNumber = Regex("""^\d{2,5}[a-z]?\d{0,3}\s+\p{L}.*""", RegexOption.IGNORE_CASE)

    fun clean(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalized = AmountExtractor.normalize(value)
        if (documentTitles.any { normalized.contains(it) }) return null
        if (addressPrefixes.any { normalized.startsWith(it) }) return null
        if (leadingHouseNumber.matches(normalized)) return null
        if (normalized.startsWith("sdt") || normalized.startsWith("phone")) return null
        return value
    }
}
