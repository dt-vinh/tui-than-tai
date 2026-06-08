package com.phuongnn14.tuithantai.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Test parser với OCR text mô phỏng từ 5 screenshot thực tế người dùng chụp.
 *
 * Ảnh 1: MB Bank – chuyển khoản 2,000,000 VND thành công
 * Ảnh 2: MoMo – nạp tiền vào Túi Thần Tài -9,385,662đ
 * Ảnh 3: MoMo – chuyển đến Vietinbank -600,000đ
 * Ảnh 4: MoMo – tiền lời Túi+ +1,408đ (income nhỏ)
 * Ảnh 5: MoMo – tiền lời Túi Thần Tài ngày +11,267đ (income)
 */
class RealScreenshotOcrTest {

    // ─────────────────────────────────────────────────────────────────────────
    // ẢNH 1 – MB BANK: Giao dịch thành công 2,000,000 VND
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `anh1 MB Bank - giao dich thanh cong 2tr VND`() {
        // Gemini OCR thường trả nguyên dạng text trên màn hình
        val ocr = """
            Giao dịch thành công
            2,000,000 VND
            08/06/2026 11:06:58
            Đến: DU TIEN VINH
            Tài khoản: 9890947259883
            Tại: NHTMCP Quân Đội
            Lưu danh bạ
            Nội dung
            Nguyen Thi Hong Nhung chuyen khoan nhanh qua Zalo
            Số tham chiếu
            0200970488060811065820264kmg666815
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)

        // Số tiền phải đúng
        assertEquals("Phải là 2,000,000", 2_000_000.0, r.amount, 1.0)
        assertEquals("Tiền tệ VND", "VND", r.currency)

        // Không được nhầm số tài khoản 9890947259883 (13 chữ số)
        assertNotEquals(9_890_947_259_883.0, r.amount, 1.0)

        // documentType bank_transfer (vì có "chuyen khoan" + "thanh cong")
        assertEquals("bank_transfer", r.documentType)

        // Chuyển khoản đi → EXPENSE
        assertEquals("EXPENSE", r.type)
    }

    @Test
    fun `anh1 MB Bank - variant so VND khong dau phay`() {
        // Gemini đôi khi đọc "2000000 VND" không có dấu phẩy
        val ocr = """
            Giao dịch thành công
            2000000 VND
            08/06/2026 11:06:58
            Đến: DU TIEN VINH
            Tài khoản: 9890947259883
            Tại: NHTMCP Quân Đội
            Nội dung
            Nguyen Thi Hong Nhung chuyen khoan nhanh qua Zalo
            Số tham chiếu
            0200970488060811065820264kmg666815
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        // 2000000 có 7 chữ số ≤ 10 → phải nhận, không bị lọc như số TK
        assertEquals(2_000_000.0, r.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ẢNH 2 – MOMO: Nạp tiền vào Túi Thần Tài -9,385,662đ
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `anh2 MoMo - nap tien vao tui than tai 9385662`() {
        val ocr = """
            NẠP TIỀN VÀO TÚI THẦN TÀI
            -9.385.662đ
            Trạng thái    Thành công
            Thời gian     20:04 - 01/06/2026
            Mã giao dịch  131591237691
            Tài khoản/thẻ Ví MoMo
            Tổng phí      Miễn phí
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)

        assertEquals("Phải là 9,385,662", 9_385_662.0, r.amount, 1.0)
        assertEquals("VND", r.currency)

        // Không nhầm mã GD 131591237691 (12 chữ số)
        assertNotEquals(131_591_237_691.0, r.amount, 1.0)

        // MoMo → wallet
        assertEquals("wallet", r.documentType)

        // Nạp tiền đi (expense, không phải income)
        assertEquals("EXPENSE", r.type)
    }

    @Test
    fun `anh2 MoMo - variant triple dot VN style 9-385-662`() {
        // Một số OCR đọc dấu chấm nghìn kiểu VN thành dấu chấm thập phân
        // Parser phải vẫn ra đúng VND amount
        val ocr = """
            NẠP TIỀN VÀO TÚI THẦN TÀI
            -9.385.662đ
            Mã giao dịch  131591237691
            Tài khoản/thẻ Ví MoMo
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        // isVnd=true nên lấy digits thuần = 9385662
        assertEquals(9_385_662.0, r.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ẢNH 3 – MOMO: Chuyển đến Vietinbank -600,000đ
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `anh3 MoMo chuyen Vietinbank - 600000 EXPENSE`() {
        val ocr = """
            CHUYỂN ĐẾN NGUYEN MAI MINH THU (VIETINBANK)
            -600.000đ
            Trạng thái    Thành công
            Thời gian     15:15 - 05/06/2026
            Mã giao dịch  132134503406
            Tài khoản/thẻ Quỹ Phá đảo Huế
            Tổng phí      Miễn phí
            Thưởng xu     +100 Xu
            Danh mục      Học tập
            Số thẻ/TK     0346785686
            Ngân hàng     Ngân hàng TMCP Công Thương Việt Nam
            Người nhận    NGUYEN MAI MINH THU
            Số tiền       600.000đ
            Tin nhắn      tien qua MoMo
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)

        assertEquals("Phải là 600,000", 600_000.0, r.amount, 1.0)
        assertEquals("VND", r.currency)

        // Không nhầm mã GD 132134503406 (12 chữ số)
        assertNotEquals(132_134_503_406.0, r.amount, 1.0)

        // Không nhầm "+100 Xu" thành income (Xu không phải VND)
        assertNotEquals(100.0, r.amount, 1.0)

        // Không nhầm số TK "0346785686" thành amount
        assertNotEquals(346_785_686.0, r.amount, 1.0)

        assertEquals("EXPENSE", r.type)
        assertEquals("wallet", r.documentType)
    }

    @Test
    fun `anh3 MoMo chuyen - so tien cuoi dong la total dung`() {
        // Dòng "Số tiền  600.000đ" phải được parse qua keyword "so tien"
        val ocr = """
            -600.000đ
            Mã giao dịch  132134503406
            Số tiền       600.000đ
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(600_000.0, r.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ẢNH 4 – MOMO: Tặng thêm tiền lời từ Túi+ +1,408đ (INCOME nhỏ)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `anh4 MoMo tien loi tui plus - 1408 INCOME`() {
        val ocr = """
            TẶNG THÊM TIỀN LỜI TỪ TÚI+
            +1.408đ
            Trạng thái    Thành công
            Thời gian     16:10 - 08/06/2026
            Mã giao dịch  20682858029
            Tài khoản/thẻ Túi Thần Tài
            Tổng phí      Miễn phí
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)

        assertEquals("Phải là 1,408", 1_408.0, r.amount, 1.0)
        assertEquals("VND", r.currency)

        // +1.408đ → income (dấu + trước số)
        assertEquals("INCOME", r.type)

        // Không nhầm mã GD 20682858029 (11 chữ số) → > 10 → bị lọc ✓
        assertNotEquals(20_682_858_029.0, r.amount, 1.0)

        // Ảnh 4: OCR text không chứa "momo" → documentType có thể là "receipt" hoặc "wallet"
        // Điều quan trọng là amount và type đúng
    }

    @Test
    fun `anh4 MoMo tien loi - so nho 1408 VND van duoc parse`() {
        // 1408 < 1000? Không — 1408 > 1000 ✓
        // Nhưng creditLine fallback: extractRightmostNumber phải chấp nhận ≥ 1000
        val ocr = "+1.408đ"
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(1_408.0, r.amount, 1.0)
        assertEquals("INCOME", r.type)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ẢNH 5 – MOMO: Tiền lời Túi Thần Tài ngày +11,267đ
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `anh5 MoMo tien loi ngay - 11267 INCOME`() {
        val ocr = """
            TIỀN LỜI TÚI THẦN TÀI NGÀY 07/06/2026
            +11.267đ
            Trạng thái    Thành công
            Thời gian     00:53 - 08/06/2026
            Mã giao dịch  20666713831
            Tài khoản/thẻ Túi Thần Tài
            Tổng phí      Miễn phí
            Danh mục      Lợi nhuận
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)

        assertEquals("Phải là 11,267", 11_267.0, r.amount, 1.0)
        assertEquals("VND", r.currency)
        assertEquals("INCOME", r.type)
        // Ảnh 5: OCR text không có "momo" → documentType = "receipt" là OK

        // Không nhầm mã GD 20666713831 (11 chữ số) → bị lọc
        assertNotEquals(20_666_713_831.0, r.amount, 1.0)
    }

    @Test
    fun `anh5 MoMo - ngay trong tieu de khong lam nhieu so tien`() {
        // "NGÀY 07/06/2026" không được bị parse thành số tiền
        val ocr = """
            TIỀN LỜI TÚI THẦN TÀI NGÀY 07/06/2026
            +11.267đ
            Mã giao dịch  20666713831
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(11_267.0, r.amount, 1.0)
        // Không nhầm 7 (từ 07) hay 2026 hay 6 thành amount
        assertNotEquals(7.0, r.amount, 1.0)
        assertNotEquals(2026.0, r.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases chung cho MoMo format
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `momo - mien phi khong bi parse thanh amount`() {
        // "Tổng phí   Miễn phí" không có số → không ảnh hưởng amount
        val ocr = """
            +50.000đ
            Tổng phí      Miễn phí
            Thưởng xu     +100 Xu
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(50_000.0, r.amount, 1.0)
        assertEquals("INCOME", r.type)
    }

    @Test
    fun `momo - ma giao dich khong duoc chon lam amount`() {
        // Mã GD 11 chữ số phải bị lọc
        val ocr = """
            -250.000đ
            Mã giao dịch  20682858029
            Tài khoản/thẻ Ví MoMo
        """.trimIndent()

        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(250_000.0, r.amount, 1.0)
        assertNotEquals(20_682_858_029.0, r.amount, 1.0)
    }
}
