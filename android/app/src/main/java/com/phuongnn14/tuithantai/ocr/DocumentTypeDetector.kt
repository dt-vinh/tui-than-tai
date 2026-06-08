package com.phuongnn14.tuithantai.ocr

import java.text.Normalizer

object DocumentTypeDetector {

    fun detect(normalizedText: String): DocumentType {
        val t = normalizedText

        // No monetary content → not_receipt
        if (!t.contains(Regex("""[0-9]"""))) return DocumentType.NOT_RECEIPT

        return when {
            // Payment confirmation / bank transfer
            t.containsAny(
                "chuyen khoan thanh cong", "nhan tien thanh cong",
                "transfer successful", "ma giao dich", "so tham chieu",
                "ben nhan", "ben chuyen", "transaction id"
            ) -> DocumentType.PAYMENT_CONFIRMATION

            // E-invoice Vietnam
            t.containsAny(
                "hoa don dien tu", "ma so thue", "mst", "ky hieu hoa don",
                "so hoa don", "ma co quan thue", "e-invoice"
            ) -> DocumentType.E_INVOICE

            // Payroll / salary sheet — multiple employees, not a consumer receipt
            t.containsAny(
                "luong/payroll", "hd luong", "bang luong", "tong luong tra",
                "luong co ban", "phu cap", "tang ca", "payroll",
                "salary sheet", "bang tinh luong"
            ) -> DocumentType.STATEMENT

            // Bank statement
            t.containsAny(
                "sao ke", "so du cuoi ky", "danh sach giao dich",
                "statement", "opening balance", "closing balance"
            ) -> DocumentType.STATEMENT

            // Utility bill
            t.containsAny(
                "tien dien", "hoa don dien", "tien nuoc", "hoa don nuoc",
                "phi internet", "phi truyen hinh", "ky cuoc", "ma khach hang"
            ) -> DocumentType.UTILITY_BILL

            // Temporary receipt
            t.containsAny("hoa don tam tinh", "tam tinh", "provisional") ->
                DocumentType.TEMPORARY_RECEIPT

            // Default to POS receipt
            else -> DocumentType.POS_RECEIPT
        }
    }

    private fun String.containsAny(vararg tokens: String): Boolean =
        tokens.any { this.contains(it) }
}

internal fun normalize(s: String): String = Normalizer
    .normalize(s, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace('đ', 'd').replace('Đ', 'D')
    .lowercase()
