package com.phuongnn14.tuithantai.ui

import java.util.regex.Pattern

data class OcrItem(
    val name: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)

/**
 * Kết quả parse OCR từ ảnh hóa đơn.
 *
 * @param amount       Tổng tiền cần thanh toán (0.0 nếu không tìm được).
 * @param currency     "VND" hoặc "USD".
 * @param title        Tiêu đề giao dịch (tên cửa hàng hoặc tên sản phẩm đầu tiên).
 * @param category     Danh mục (Ăn uống / Mua sắm / Y tế / Di chuyển / Nhà ở /
 *                     Dịch vụ / Giải trí / Du lịch / Lương).
 * @param merchantName Tên cửa hàng nếu đọc được.
 * @param items        Danh sách món hàng.
 * @param type         "EXPENSE" hoặc "INCOME".
 * @param needsReview  true nếu parser không chắc – yêu cầu user kiểm tra lại.
 * @param documentType "receipt" | "bank_transfer" | "wallet" | "non_receipt".
 */
data class OcrResult(
    val amount: Double,
    val currency: String,
    val title: String,
    val category: String,
    val merchantName: String = "",
    val items: List<OcrItem> = emptyList(),
    val type: String = "EXPENSE",
    val needsReview: Boolean = false,
    val documentType: String = "receipt"
)

object ReceiptOcrParser {

    fun parseText(text: String): OcrResult = parseTextHeuristics(text)

    // ─────────────────────────────────────────────────────────────────────────
    // Bỏ dấu tiếng Việt → ASCII để so khớp keyword bất kể dấu
    // vd: "Tổng cộng" → "tong cong", "Tiền nhận" → "tien nhan"
    // ─────────────────────────────────────────────────────────────────────────
    fun normalize(s: String): String {
        // NFD tách dấu tiếng Việt thành combining marks → xóa hết → còn ASCII.
        // Xử lý đúng mọi biến thể: ả ạ ư ơ ằ ấ ... không cần liệt kê thủ công.
        var r = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        r = r.replace(Regex("\\p{Mn}+"), "")   // xóa Unicode non-spacing marks
        r = r.replace('đ', 'd')                  // đ không decompose trong NFD
        return r
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tiền xử lý dòng: chuyển "125K" → "125000", "125 000" → "125000"
    // ─────────────────────────────────────────────────────────────────────────
    private fun preprocessAmounts(line: String): String {
        var s = line
        // ── 1tr2 format: "1tr2" → 1200000 (trước simple tr) ─────────────────
        s = s.replace(Regex("""([0-9]+)\s*[Tt][Rr]\s*([0-9]+)\b""")) { mr ->
            val main  = mr.groupValues[1].toLongOrNull() ?: return@replace mr.value
            val frac  = mr.groupValues[2]
            val fracV = when (frac.length) {
                1    -> frac.toLong() * 100_000L
                2    -> frac.toLong() * 10_000L
                else -> frac.toLongOrNull() ?: 0L
            }
            (main * 1_000_000L + fracV).toString()
        }
        // ── tr / triệu: "1.2tr", "1 triệu" → × 1,000,000 ───────────────────
        s = s.replace(
            Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(?:[Tt][Rr](?:i[eêệ]u|ieu)?|[Tt]ri[eêệ]u)\b""")
        ) { mr ->
            val raw = mr.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace mr.value
            (raw * 1_000_000.0).toLong().toString()
        }
        // ── củ / cu: "2 củ", "2 củ 5" → × 1,000,000 ────────────────────────
        s = s.replace(Regex("""([0-9]+)\s*(?:củ|cu|[Cc][Uu])\b(?:\s*([0-9]+))?""")) { mr ->
            val main = mr.groupValues[1].toLongOrNull() ?: return@replace mr.value
            val frac = mr.groupValues[2].toLongOrNull() ?: 0L
            (main * 1_000_000L + frac * 100_000L).toString()
        }
        // ── K suffix: 125K / 125k / 6.850K → × 1,000 ───────────────────────
        s = s.replace(Regex("""([0-9]+(?:[.,][0-9]+)?)[Kk]\b""")) { mr ->
            val raw = mr.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace mr.value
            raw.times(1000).toLong().toString()
        }
        // ── Space-thousands: "125 000" → "125000" ────────────────────────────
        s = s.replace(Regex("""([0-9]{1,3}) ([0-9]{3})\b""")) { mr ->
            mr.groupValues[1] + mr.groupValues[2]
        }
        return s
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phát hiện loại tài liệu
    // ─────────────────────────────────────────────────────────────────────────
    private fun detectDocumentType(fullNorm: String): String {
        if (fullNorm.contains("momo") || fullNorm.contains("zalopay")
            || fullNorm.contains("vnpay") || fullNorm.contains("vi dien tu")
            || fullNorm.contains("grabpay") || fullNorm.contains("shopeepay")
            || fullNorm.contains("vietqr") || fullNorm.contains("qr nhan tien")
            || fullNorm.contains("qr thanh toan")) return "wallet"

        val bankNames = listOf("vietcombank", "bidv", "techcombank", "mbbank", "mb bank",
            "vpbank", "agribank", "vib", "acb", "sacombank", "shinhanbank",
            "ocb", "hdbank", "tpbank", "msb", "seabank")
        val hasBankContext = bankNames.any { fullNorm.contains(it) }
            || fullNorm.contains("chuyen khoan") || fullNorm.contains("bien dong so du")
            || fullNorm.contains("so tk") || fullNorm.contains("den tk")
        val hasTxContext = fullNorm.contains("so tien") || fullNorm.contains("so du")
            || fullNorm.contains("nhan duoc") || fullNorm.contains("da chuyen")
            || fullNorm.contains("transfer") || fullNorm.contains("thanh cong")
        if (hasBankContext && hasTxContext) return "bank_transfer"
        return "receipt"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phát hiện giao dịch INCOME từ thông báo ngân hàng/ví
    // ─────────────────────────────────────────────────────────────────────────
    private fun detectIsIncome(fullNorm: String, lines: List<String>): Boolean {
        if (fullNorm.contains("nhan duoc tien") || fullNorm.contains("ban nhan duoc")
            || fullNorm.contains("tien vao tai khoan") || fullNorm.contains("tien vao tk")
            || fullNorm.contains("bien dong so du") || fullNorm.contains("credit")
            || fullNorm.contains("luong") || fullNorm.contains("salary")
            || fullNorm.contains("payslip") || fullNorm.contains("payroll")
            || fullNorm.contains("hoan tien") || fullNorm.contains("refund")
            || fullNorm.contains("qr nhan tien")) return true
        // Dấu + trước số tiền lớn (ví dụ "+500,000đ", "+11.267đ")
        // Yêu cầu amount ≥ 1000 để loại "+100 Xu" (điểm thưởng) ra khỏi income
        val plusAmountPattern = Pattern.compile("""\+\s*([0-9][0-9.,]*)""")
        lines.forEach { line ->
            val m = plusAmountPattern.matcher(line)
            while (m.find()) {
                val digits = m.group(1).replace(".", "").replace(",", "")
                val v = digits.toLongOrNull() ?: return@forEach
                if (v >= 1000) return true
            }
        }
        return false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse chính
    // ─────────────────────────────────────────────────────────────────────────
    fun parseTextHeuristics(raw: String): OcrResult {
        val lines = raw.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val fullNorm = normalize(lines.joinToString(" "))

        // ── Tiền tệ ──────────────────────────────────────────────────────────
        val currency = if (raw.contains("$") && !raw.contains("₫")
            && !raw.contains("VND", ignoreCase = true)) "USD" else "VND"
        val isVnd = currency == "VND"

        // ── Loại tài liệu ────────────────────────────────────────────────────
        val documentType = detectDocumentType(fullNorm)

        // ── Tên cửa hàng ─────────────────────────────────────────────────────
        val merchantName = lines.firstOrNull { line ->
            line.length in 3..60
                && !line.any { c -> c.isDigit() }
                && normalize(line).let { n ->
                    !n.startsWith("dia chi") && !n.startsWith("address")
                        && !n.startsWith("ngay") && !n.startsWith("date")
                }
        }?.trim() ?: ""

        // ── Ignore keywords khi tìm total ────────────────────────────────────
        val ignoreTotalNorm = listOf(
            "tien nhan", "tien thoi", "tien thua", "tien tra lai", "tien khach",
            "received", "change", "cash tendered", "cash back", "tra lai",
            "customer paid", "tien du", "so du", "diem tich luy", "diem thuong",
            "khuyen mai", "ma giao dich", "so hoa don"
        )

        // ── Total keywords (normalized, ưu tiên) ─────────────────────────────
        val totalKeywordsNorm = listOf(
            "tong cong", "tong tien thanh toan", "total amount", "grand total",
            "thanh tien", "tong thanh toan", "tong don hang",
            "tong luong", "net pay", "amount due", "can thanh toan",
            "so tien chuyen", "so tien giao dich", "so tien",
            "total", "tong"
        )

        // Danh sách dòng bỏ qua khi tìm tổng tiền
        val ignoreCurrencyTag = ignoreTotalNorm + listOf(
            "thuong xu", "tong phi", "so tk", "so tai khoan", "ma gd", "ma giao dich"
        )

        var detectedTotal = 0.0

        // ════════════════════════════════════════════════════════════════════
        // STAGE 0 – ƯU TIÊN TUYỆT ĐỐI: tìm số có ký hiệu tiền tường minh
        // ════════════════════════════════════════════════════════════════════
        // Rule của người dùng: "thằng nào có VNĐ/Đ/VND/₫ kèm theo thì là số tiền".
        // Số TK (9890947259883), mã GD (131591237691) không bao giờ kèm ký hiệu tiền
        // → không bị nhầm.
        // Áp dụng cho VND; USD vẫn dùng keyword scan.
        if (isVnd) {
            // Trên normalized text: đ → d, VNĐ/VND → vnd, ₫ giữ nguyên (U+20AB)
            // Pattern: <số>[+-dấu cách]*vnd | <số>d\b
            val vndTagPat = Pattern.compile(
                """([+-]?\s*[0-9][0-9.,]*)\s*(?:vnd\b|d\b)"""
            )
            val dongPrefixPat = Pattern.compile("""₫\s*([0-9][0-9.,]*)""")

            data class TaggedAmt(val amount: Double, val score: Double)
            val tagged = mutableListOf<TaggedAmt>()

            for ((idx, rawLine) in lines.withIndex()) {
                val processed  = preprocessAmounts(rawLine)
                val norm       = normalize(processed)
                // Bỏ dòng tiền thừa, khách đưa, số dư, mã GD...
                if (ignoreCurrencyTag.any { norm.contains(it) }) continue

                // ── Trailing: "125.000d", "2,000,000 vnd", "+1.408d" ──────
                val m = vndTagPat.matcher(norm)
                while (m.find()) {
                    val digits = m.group(1)?.replace(Regex("[^0-9]"), "") ?: continue
                    val v = digits.toDoubleOrNull() ?: continue
                    if (v < 1000.0) continue
                    var score = 0.5
                    if (totalKeywordsNorm.any { norm.contains(it) }) score += 0.4
                    if (idx.toDouble() / lines.size > 0.65) score += 0.1
                    tagged += TaggedAmt(v, score)
                }

                // ── Prefix ₫: "₫125.000" (không bị normalize) ─────────────
                if ('₫' in rawLine || '₫' in processed) {
                    val m2 = dongPrefixPat.matcher(processed)
                    while (m2.find()) {
                        val raw2 = m2.group(1)?.replace(".", "")?.replace(",", "") ?: continue
                        val v = raw2.toDoubleOrNull() ?: continue
                        if (v < 1000.0) continue
                        var score = 0.5
                        if (totalKeywordsNorm.any { norm.contains(it) }) score += 0.4
                        tagged += TaggedAmt(v, score)
                    }
                }
            }

            if (tagged.isNotEmpty()) {
                detectedTotal = tagged.maxByOrNull { it.score }!!.amount
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // STAGE 1 – Fallback: keyword scan (dùng khi không có ký hiệu tiền)
        // ════════════════════════════════════════════════════════════════════
        if (detectedTotal == 0.0) {
            outer@ for (keyword in totalKeywordsNorm) {
                for (line in lines) {
                    val norm = normalize(line)
                    if (!norm.contains(keyword)) continue
                    if (ignoreTotalNorm.any { norm.contains(it) }) continue
                    val num = extractRightmostNumber(preprocessAmounts(line), isVnd)
                    if (num != null && num > 0.0) { detectedTotal = num; break@outer }
                }
            }
        }

        // ── Fallback: dòng "+" prefix (bank credit: +500,000đ) ───────────
        if (detectedTotal == 0.0) {
            val creditLine = lines.firstOrNull { line ->
                line.trimStart().startsWith("+") && line.any { it.isDigit() }
            }
            if (creditLine != null) {
                val cleaned = creditLine.replace("+", "")
                detectedTotal = extractRightmostNumber(preprocessAmounts(cleaned), isVnd) ?: 0.0
            }
        }

        // ── Fallback: dòng "thanh toán / payment" ────────────────────────
        if (detectedTotal == 0.0) {
            val payLine = lines.firstOrNull { line ->
                val norm = normalize(line)
                (norm.contains("thanh toan") || norm.contains("payment"))
                    && ignoreTotalNorm.none { norm.contains(it) }
            }
            if (payLine != null) {
                detectedTotal = extractRightmostNumber(preprocessAmounts(payLine), isVnd) ?: 0.0
            }
        }

        // ── Parse line items ──────────────────────────────────────────────────
        val headerKeywordsNorm = listOf(
            "ten mon", "mo ta", "description", "item", "san pham",
            "so luong", "don gia", "thanh tien", "tong",
            "dia chi", "ngay", "gio", "so hd", "vnd", "cash", "tien", "cam on",
            "thank", "hen gap", "sdt", "tel", "fax", "hotline", "website", "email"
        )

        val skipPrefixesNorm = listOf(
            "tong", "thanh tien", "thanh toan", "total", "subtotal",
            "tien nhan", "tien thoi", "tien khach", "tien thua", "tien tra",
            "tien du", "tien",
            "tax", "vat", "thue", "discount", "giam gia", "phi giao",
            "cam on", "thank", "hen", "note", "ghi chu",
            "dia chi", "ngay", "gio",
            "so hd", "so phieu", "so tk", "so tien", "so tai khoan",
            "ma gd", "ma giao dich", "ma don", "ma hoa don",
            "cash", "change",
            "da thu", "da nhan", "khach hang", "benh nhan", "nhan vien", "ma nv",
            "diem tich", "diem thuong", "khuyen mai",
            "con lai", "con no",
            "tu tk", "den tk", "so dien", "chi so",
            // ── Thông tin giao dịch (không phải line item) ───────────────────
            "tai khoan",    // "Tài khoản: 9890947259883" → số TK, không phải tiền
            "thoi gian",    // "Thời gian   20:04 - 01/06/2026" → ngày giờ
            "trang thai",   // "Trạng thái   Thành công"
            "nguoi nhan",   // "Người nhận   ..."
            "nguoi chuyen", // "Người chuyển ..."
            "ngan hang",    // "Ngân hàng   Vietinbank"
            "noi dung",     // "Nội dung     chuyen khoan"
            "so tham chieu","so chung tu",
            "phi", "tong phi"
        )

        val numPattern = Pattern.compile("[0-9]+([.,][0-9]+)*")
        val parsedItems = mutableListOf<OcrItem>()
        val minThreshold = if (isVnd) 1000.0 else 0.01

        for (rawLine in lines) {
            val line = preprocessAmounts(rawLine)
            val norm = normalize(line)

            if (!line.any { it.isDigit() }
                && headerKeywordsNorm.any { norm.contains(it) }) continue
            if (skipPrefixesNorm.any { norm.startsWith(it) }) continue
            if (line.length < 3) continue

            val matcher = numPattern.matcher(line)
            val nums = mutableListOf<Double>()
            while (matcher.find()) {
                val raw2 = matcher.group()
                val parsed = if (isVnd) raw2.replace(".", "").replace(",", "").toDoubleOrNull()
                             else raw2.replace(",", "").toDoubleOrNull()
                if (parsed != null && parsed > 0.0) nums.add(parsed)
            }
            if (nums.isEmpty()) continue

            val firstNumIdx = line.indexOfFirst { it.isDigit() }

            // Format SL-đầu: "1   Cơm Tấm Sườn  35,000  35,000"
            if (firstNumIdx == 0 && nums.size >= 2) {
                val possibleQty = nums[0]
                if (possibleQty in 1.0..20.0 && possibleQty == Math.floor(possibleQty)) {
                    val afterQty = line.trimStart()
                        .drop(possibleQty.toInt().toString().length).trimStart()
                    val nextNumIdx = afterQty.indexOfFirst { it.isDigit() }
                    if (nextNumIdx > 1) {
                        val namePart = afterQty.substring(0, nextNumIdx).trim()
                        if (namePart.length >= 2) {
                            val nameNorm = normalize(namePart)
                            if (!skipPrefixesNorm.any { nameNorm.startsWith(it) }
                                && !nameNorm.contains("ngay") && !nameNorm.contains("gio")) {
                                val total = nums.last()
                                if (total >= minThreshold) {
                                    val unitPrice = if (nums.size >= 3) nums[nums.size - 2]
                                                    else total / possibleQty
                                    parsedItems.add(OcrItem(
                                        name = namePart,
                                        quantity = possibleQty.toInt(),
                                        price = unitPrice,
                                        total = total))
                                    continue
                                }
                            }
                        }
                    }
                }
            }

            if (firstNumIdx <= 0) continue
            val namePart = line.substring(0, firstNumIdx).trim()
            if (namePart.length < 2) continue

            val nameNorm = normalize(namePart)
            if (skipPrefixesNorm.any { nameNorm.startsWith(it) }) continue
            if (nameNorm.contains("ngay") || nameNorm.contains("gio")
                || nameNorm.contains("tu:") || nameNorm.contains("don hang")
                || nameNorm.contains("hd:") || nameNorm.contains("so:")) continue
            if (namePart.contains("#") || namePart.contains("@")) continue

            when (nums.size) {
                1 -> if (nums[0] >= minThreshold)
                        parsedItems.add(OcrItem(namePart, 1, nums[0], nums[0]))
                2 -> {
                    val q = nums[0]; val t = nums[1]
                    if (t < minThreshold) continue
                    if (q in 1.0..99.0 && q == Math.floor(q))
                        parsedItems.add(OcrItem(namePart, q.toInt(), t / q, t))
                    else
                        parsedItems.add(OcrItem(namePart, 1, t, t))
                }
                3 -> {
                    val q = nums[0]; val p = nums[1]; val t = nums[2]
                    if (t < minThreshold) continue
                    if (q in 1.0..99.0 && q == Math.floor(q))
                        parsedItems.add(OcrItem(namePart, q.toInt(), p, t))
                    else
                        parsedItems.add(OcrItem(namePart, 1, t, t))
                }
                else -> {
                    val q = nums[nums.size - 3]
                    val p = nums[nums.size - 2]
                    val t = nums[nums.size - 1]
                    if (t < minThreshold) continue
                    if (q in 1.0..99.0 && q == Math.floor(q))
                        parsedItems.add(OcrItem(namePart, q.toInt(), p, t))
                    else
                        parsedItems.add(OcrItem(namePart, 1, t, t))
                }
            }
        }

        // ── Tổng cuối ────────────────────────────────────────────────────────
        val itemsSum = parsedItems.sumOf { it.total }

        // ── Last-resort: nếu chưa tìm được total, quét toàn bộ text tìm số hợp lệ ──
        // Bỏ qua số dài > 10 chữ số (số tài khoản, mã GD...)
        var lastResortTotal = 0.0
        var lastResortUsed = false
        if (detectedTotal == 0.0 && parsedItems.isEmpty()) {
            for (rawLine in lines) {
                if (lastResortTotal > 0.0) break
                val line = preprocessAmounts(rawLine)
                val norm = normalize(line)
                // Bỏ qua dòng bắt đầu bằng skip prefix
                if (skipPrefixesNorm.any { norm.startsWith(it) }) continue
                val matcher = numPattern.matcher(line)
                while (matcher.find()) {
                    val raw2 = matcher.group()
                    // Bỏ qua số quá dài (số TK, mã GD)
                    val digits = raw2.replace(".", "").replace(",", "")
                    if (digits.length > 10) continue
                    val parsed = if (isVnd) digits.toDoubleOrNull()
                                 else raw2.replace(",", "").toDoubleOrNull()
                    val min = if (isVnd) 1000.0 else 0.01
                    if (parsed != null && parsed >= min) {
                        lastResortTotal = parsed
                        lastResortUsed = true
                        break
                    }
                }
            }
        }

        val finalTotal = when {
            detectedTotal > 0.0 -> detectedTotal
            parsedItems.isNotEmpty() -> itemsSum
            lastResortTotal > 0.0 -> lastResortTotal
            else -> 0.0
        }

        // ── needs_review ─────────────────────────────────────────────────────
        val discrepancy = if (detectedTotal > 0 && parsedItems.isNotEmpty())
            Math.abs(itemsSum - detectedTotal) / detectedTotal else 0.0
        val hasUnclearStatus = fullNorm.contains("cho thanh toan")
            || fullNorm.contains("dang xu ly") || fullNorm.contains("pending")
            || fullNorm.contains("cho xac nhan")
        val needsReview = finalTotal == 0.0
            || discrepancy > 0.10          // items sum lệch >10% so với dòng total
            || documentType == "non_receipt"
            || hasUnclearStatus            // trạng thái chưa rõ (chờ thanh toán, đang xử lý)

        // ── Tiêu đề ──────────────────────────────────────────────────────────
        val title = if (parsedItems.isNotEmpty())
            parsedItems.take(2).joinToString(", ") { it.name }
        else merchantName

        // ── Thu nhập? ────────────────────────────────────────────────────────
        val isIncome = detectIsIncome(fullNorm, lines)

        // ── Danh mục ─────────────────────────────────────────────────────────
        val category = when {
            isIncome && (fullNorm.contains("luong") || fullNorm.contains("salary")
                || fullNorm.contains("payslip") || fullNorm.contains("payroll")) -> "Lương"

            isIncome && (fullNorm.contains("hoan tien") || fullNorm.contains("refund")) -> "Hoàn tiền"

            fullNorm.contains("phong kham") || fullNorm.contains("benh vien")
                || fullNorm.contains("benh nhan") || fullNorm.contains("sieu am")
                || fullNorm.contains("xet nghiem") || fullNorm.contains("thuoc")
                || fullNorm.contains("nha thuoc") || fullNorm.contains("y te") -> "Y tế"

            fullNorm.contains("ve may bay") || fullNorm.contains("khach san")
                || fullNorm.contains("hotel") || fullNorm.contains("resort")
                || fullNorm.contains("homestay") || fullNorm.contains("du lich") -> "Du lịch"

            fullNorm.contains("rap chieu") || fullNorm.contains("ve xem")
                || fullNorm.contains("cinema") || fullNorm.contains("game")
                || fullNorm.contains("giai tri") -> "Giải trí"

            fullNorm.contains("cat toc") || fullNorm.contains("goi dau")
                || fullNorm.contains("spa") || fullNorm.contains("massage")
                || fullNorm.contains("giat la") || fullNorm.contains("rua xe")
                || fullNorm.contains("sua chua") || fullNorm.contains("thue san")
                || fullNorm.contains("phong gym") || fullNorm.contains("gym") -> "Dịch vụ"

            fullNorm.contains("mart") || fullNorm.contains("sieu thi")
                || fullNorm.contains("supermarket") || fullNorm.contains("vinmart")
                || fullNorm.contains("coopmart") || fullNorm.contains("lotte")
                || fullNorm.contains("winmart") -> "Mua sắm"

            fullNorm.contains("dien luc") || fullNorm.contains("hoa don dien")
                || fullNorm.contains("hoa don nuoc") || fullNorm.contains("tien dien")
                || fullNorm.contains("tien nuoc") || fullNorm.contains("internet")
                || fullNorm.contains("cuoc dien thoai") || fullNorm.contains("bills") -> "Nhà ở"

            fullNorm.contains("grab") || fullNorm.contains("taxi")
                || fullNorm.contains("xe om") || fullNorm.contains("bai giu xe")
                || fullNorm.contains("bai xe") || fullNorm.contains("xe may")
                || fullNorm.contains("xang") || fullNorm.contains("do xang")
                || fullNorm.contains("bus") || fullNorm.contains("xe buyt") -> "Di chuyển"

            fullNorm.contains("cafe") || fullNorm.contains("ca phe")
                || fullNorm.contains("quan an") || fullNorm.contains("nha hang")
                || fullNorm.contains("croissant") || fullNorm.contains("coffee")
                || fullNorm.contains("tra sua") || fullNorm.contains("bun")
                || fullNorm.contains("pho") || fullNorm.contains("com ")
                || fullNorm.contains("banh mi") || fullNorm.contains("bia") -> "Ăn uống"

            fullNorm.contains("shop") || fullNorm.contains("mua")
                || fullNorm.contains("market") -> "Mua sắm"

            else -> "Ăn uống"
        }

        val type = if (isIncome || category == "Lương" || category == "Hoàn tiền") "INCOME" else "EXPENSE"

        return OcrResult(
            amount = finalTotal,
            currency = currency,
            title = title,
            category = category,
            merchantName = merchantName,
            items = parsedItems,
            type = type,
            needsReview = needsReview,
            documentType = documentType
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lấy số cuối cùng bên phải trên một dòng (VND: bỏ qua < 1000)
    // ─────────────────────────────────────────────────────────────────────────
    fun extractRightmostNumber(line: String, isVnd: Boolean): Double? {
        val pattern = Pattern.compile("[0-9]+([.,][0-9]+)*")
        val matcher = pattern.matcher(line)
        val matches = mutableListOf<String>()
        while (matcher.find()) matches.add(matcher.group())
        for (match in matches.reversed()) {
            val parsed = if (isVnd) match.replace(".", "").replace(",", "").toDoubleOrNull()
                         else match.replace(",", "").toDoubleOrNull()
            if (parsed == null || parsed <= 0.0) continue
            if (isVnd && parsed < 1000.0) continue
            return parsed
        }
        return null
    }
}
