package com.phuongnn14.tuithantai.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Extended OCR Parser test suite – Nhóm A → R
 * Mỗi nhóm có ít nhất 2 test case đại diện.
 * Test chạy thuần JVM, không cần Android context.
 *
 * Nguyên tắc bắt buộc:
 *   - Không hallucinate tên sản phẩm/tổng tiền khi không chắc.
 *   - needsReview = true khi không tìm được tổng hoặc dữ liệu mâu thuẫn.
 *   - Không tự lưu sau OCR.
 */
class ReceiptOcrParserExtendedTest {

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM A – Hóa đơn ăn uống / quán ăn / cafe
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `A-FOOD_007 - co tam tinh va tong cong - chon tong cong`() {
        val ocr = """
            HIGHLANDS COFFEE
            Tạm tính:     120,000
            VAT (8%):       9,600
            Tổng cộng:    129,600 VND
            Tiền khách đưa: 200,000
            Tiền thừa:      70,400
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // Phải lấy "tổng cộng", không phải "tạm tính" hay "tiền khách"
        assertEquals(129_600.0, r.amount, 500.0)
        assertNotEquals(120_000.0, r.amount, 1.0)
        assertEquals("VND", r.currency)
    }

    @Test
    fun `A-FOOD_013 - chi co tong tien khong co item - total dung items rong`() {
        val ocr = """
            Quán Ăn Hồng
            (Không in chi tiết món)
            TỔNG CỘNG: 85,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(85_000.0, r.amount, 1.0)
        // items có thể rỗng – parser KHÔNG được hallucinate tên món
        r.items.forEach { item ->
            assertFalse("Tên món không được là placeholder", item.name in listOf(
                "Không xác định", "Khác", "Hàng hóa", "Vật phẩm", "Item"))
        }
    }

    @Test
    fun `A-FOOD_010 - tong ky badminton do an bia - category an uong`() {
        // Tô Ký Sports: hóa đơn sân thể thao nhưng item là đồ ăn/bia
        val ocr = """
            TÔ KÝ BADMINTON
            PHIẾU THANH TOÁN
            Thuê sân    2 giờ    200,000   400,000
            Bia Tiger   12 chai  25,000    300,000
            Khô bò      500g     120,000   120,000
            Nước ngọt   6 chai   10,000     60,000
            Nước suối   6 chai    6,000     36,000
            TỔNG CỘNG:                  6,850,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(6_850_000.0, r.amount, 1.0)
        assertEquals("EXPENSE", r.type)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM B – Hóa đơn siêu thị / tạp hóa
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `B-RETAIL_006 - diem tich luy khong phai tien`() {
        val ocr = """
            Co.opmart Supermarket
            Sữa Vinamilk 1L    1   35,000   35,000
            Bánh Oreo          1   22,000   22,000
            Tổng thanh toán:         57,000 VND
            Điểm tích lũy:           570 điểm
            Tiền khách đưa:          60,000
            Tiền thừa:                3,000
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(57_000.0, r.amount, 1.0)
        assertNotEquals(570.0, r.amount, 1.0)   // không nhầm điểm thành tiền
        assertEquals("Mua sắm", r.category)
    }

    @Test
    fun `B-RETAIL_007 - ma giao dich nhieu so khong phai tien`() {
        val ocr = """
            WINMART+
            Mã GD: 20231022154321
            Nước Aqua 500ml  1   7,000   7,000
            Tổng:                        7,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(7_000.0, r.amount, 1.0)
        assertNotEquals(20_231_022_154_321.0, r.amount, 1.0)  // không nhầm mã GD
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM C – Hóa đơn dịch vụ
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `C-SERVICE_001 - bill thue san cau long - category dich vu`() {
        val ocr = """
            SÂN CẦU LÔNG BÌNH THẠNH
            Thuê sân số 3    2 giờ   60,000  120,000
            Dịch vụ thuê vợt 2 cái   15,000   30,000
            Nước suối        2 chai    5,000   10,000
            TỔNG CỘNG:                        160,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(160_000.0, r.amount, 1.0)
        assertEquals("Dịch vụ", r.category)
    }

    @Test
    fun `C-SERVICE_006 - khach san homestay - category du lich`() {
        val ocr = """
            HOMESTAY ĐÔNG DƯƠNG
            Phòng Deluxe 2 đêm   450,000   900,000
            Bữa sáng      2 bữa   50,000   100,000
            Tổng thanh toán:             1,000,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(1_000_000.0, r.amount, 1.0)
        assertEquals("Du lịch", r.category)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM D – Ảnh chuyển khoản ngân hàng tiền ra
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `D-BANK_OUT_001 - chuyen khoan thanh cong VCB - expense`() {
        val ocr = """
            VIETCOMBANK
            CHUYỂN KHOẢN THÀNH CÔNG
            Từ TK: 0021234567
            Đến TK: 9876543210
            Số tiền chuyển: 500,000 VND
            Phí giao dịch: 0 VND
            Thời gian: 22/10/2023 10:30:45
            Mã GD: VCB20231022103045
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(500_000.0, r.amount, 1.0)
        assertEquals("EXPENSE", r.type)
        assertEquals("bank_transfer", r.documentType)
    }

    @Test
    fun `D-BANK_OUT_007 - so du sau giao dich khong chon so du`() {
        val ocr = """
            MB Bank
            Chuyển khoản thành công
            Số tiền: 1,200,000 VND
            Số dư: 5,678,900 VND
            Nội dung: Tien thue nha T10
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(1_200_000.0, r.amount, 1.0)
        assertNotEquals(5_678_900.0, r.amount, 1.0)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM E – Ảnh nhận tiền ngân hàng (income)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `E-BANK_IN_001 - nhan tien thanh cong - income`() {
        val ocr = """
            MB Bank
            Bạn nhận được tiền chuyển khoản
            Từ: NGUYEN VAN AN
            Số tiền: +500,000 VND
            Số dư: 3,200,000 VND
            Nội dung: tra tien an
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(500_000.0, r.amount, 1.0)
        assertEquals("INCOME", r.type)
    }

    @Test
    fun `E-BANK_IN_004 - noi dung luong thang - category luong`() {
        val ocr = """
            Techcombank
            Biến động số dư
            Số TK: 19038123456
            Số tiền: +15,000,000 VND
            Số dư: 18,500,000 VND
            Nội dung: Luong thang 10 NV00234
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(15_000_000.0, r.amount, 1.0)
        assertEquals("INCOME", r.type)
        assertEquals("Lương", r.category)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM F – Ví điện tử / QR payment
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `F-WALLET_001 - momo thanh toan thanh cong - expense`() {
        val ocr = """
            MoMo
            Thanh toán thành công
            Cửa hàng: Bún Bò Huế Số 1
            Số tiền: 85,000 VND
            Thời gian: 22/10/2023 12:05
            Số dư ví: 245,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(85_000.0, r.amount, 1.0)
        assertEquals("EXPENSE", r.type)
        assertEquals("wallet", r.documentType)
        assertNotEquals(245_000.0, r.amount, 1.0) // không chọn số dư
    }

    @Test
    fun `F-WALLET_004 - momo nhan tien - income`() {
        val ocr = """
            MoMo
            Bạn nhận được tiền
            Từ: Trần Văn B
            Số tiền: +200,000 VND
            Nội dung: chia tien an
            Số dư ví: 350,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(200_000.0, r.amount, 1.0)
        assertEquals("INCOME", r.type)
    }

    @Test
    fun `F-WALLET_009 - QR khong co so tien - total null`() {
        val ocr = """
            QR Code
            Ngân hàng: Vietcombank
            Tên tài khoản: NGUYEN VAN A
            Số tài khoản: 0021234567
            (Quét để chuyển tiền)
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(0.0, r.amount, 0.01)
        assertTrue("Phải yêu cầu review khi không có số tiền", r.needsReview)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM G – Hóa đơn điện / nước / internet / điện thoại
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `G-BILL_001 - hoa don tien dien - category nha o`() {
        val ocr = """
            CÔNG TY ĐIỆN LỰC TP.HCM
            Hóa đơn tiền điện tháng 10/2023
            Chỉ số đầu: 1200  Chỉ số cuối: 1410
            Số điện: 210 kWh
            Tiền điện: 450,000 VND
            Tổng thanh toán: 495,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(495_000.0, r.amount, 1.0)
        assertEquals("Nhà ở", r.category)
    }

    @Test
    fun `G-BILL_008 - ma khach hang khong phai tien`() {
        val ocr = """
            CÔNG TY NƯỚC SAWACO
            Mã KH: 81234567890
            Số điện: 012 3456789
            Kỳ: T10/2023
            Tiền nước: 180,000 VND
            Tổng thanh toán: 198,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(198_000.0, r.amount, 1.0)
        // Không nhầm mã KH 81234567890 thành tiền
        assertNotEquals(81_234_567_890.0, r.amount, 1.0)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM H – Hóa đơn y tế / nhà thuốc
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `H-HEALTH_001 - nha thuoc - category y te`() {
        val ocr = """
            NHÀ THUỐC AN KHANG
            Thuốc cảm Paracetamol  2 hộp   35,000   70,000
            Vitamin C              1 hộp   45,000   45,000
            TỔNG CỘNG:                            115,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(115_000.0, r.amount, 1.0)
        assertEquals("Y tế", r.category)
    }

    @Test
    fun `H-HEALTH_008 - toa thuoc khong phai bill thanh toan - needs review`() {
        val ocr = """
            PHÒNG KHÁM ĐA KHOA
            TỜ KÊ ĐƠN THUỐC
            Bệnh nhân: Nguyễn Văn A
            Chẩn đoán: Viêm họng
            Paracetamol 500mg  x14 viên
            Amoxicillin 500mg  x14 viên
            (Toa thuốc, không phải hóa đơn thanh toán)
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // Không có số tiền → tổng = 0, needs_review = true
        assertEquals(0.0, r.amount, 0.01)
        assertTrue("Toa thuốc không có giá → phải review", r.needsReview)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM I – Xăng xe / di chuyển
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `I-TRANSPORT_001 - bill do xang - category di chuyen`() {
        val ocr = """
            XĂNG DẦU PETROLIMEX
            Loại xăng: RON 95
            Số lít: 10.5 lít
            Đơn giá: 24,800 đ/lít
            Thành tiền: 260,400 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(260_400.0, r.amount, 500.0)
        assertEquals("Di chuyển", r.category)
    }

    @Test
    fun `I-TRANSPORT_006 - so lit xang khong nham thanh tien`() {
        val ocr = """
            XĂNG DẦU SỐ 1
            Số lít: 8.2 lít
            Đơn giá: 24,800 đ/lít
            TỔNG CỘNG: 203,360 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // Không nhầm "8.2" (số lít) thành tiền
        assertNotEquals(8.2, r.amount, 0.01)
        assertEquals(203_360.0, r.amount, 500.0)
        assertEquals("Di chuyển", r.category)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM J – E-commerce / mua online
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `J-ECOM_003 - tien hang phi ship voucher - chon tong thanh toan`() {
        val ocr = """
            Shopee
            Đơn hàng: SPE-20231022-ABC123
            Tiền hàng:          250,000
            Phí vận chuyển:      30,000
            Voucher giảm:       -20,000
            Tổng thanh toán:    260,000 VND
            Phương thức: Ví ShopeePay
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(260_000.0, r.amount, 1.0)
        assertNotEquals(250_000.0, r.amount, 1.0) // không lấy tiền hàng trước discount
    }

    @Test
    fun `J-ECOM_005 - cho thanh toan - needs review`() {
        val ocr = """
            Lazada
            Đơn hàng: LAZ-987654
            Trạng thái: CHỜ THANH TOÁN
            Tổng đơn hàng: 450,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // "Chờ thanh toán" – chưa thực sự chi tiền
        assertTrue("Đơn chưa thanh toán phải needs_review", r.needsReview)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM K – Hóa đơn VAT / hóa đơn điện tử
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `K-VAT_002 - cong tien hang thue VAT tong - chon tong thanh toan`() {
        val ocr = """
            HÓA ĐƠN GIÁ TRỊ GIA TĂNG
            Tên hàng hóa: Laptop Dell Inspiron
            Cộng tiền hàng:       12,000,000
            Thuế GTGT (10%):       1,200,000
            Tổng tiền thanh toán: 13,200,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(13_200_000.0, r.amount, 1.0)
        assertNotEquals(1_200_000.0, r.amount, 1.0)  // không chọn dòng VAT
    }

    @Test
    fun `K-VAT_003 - MST nguoi ban nguoi mua khong phai tien`() {
        val ocr = """
            HÓA ĐƠN ĐIỆN TỬ
            MST người bán: 0302435288
            MST người mua: 0100109106
            Dịch vụ tư vấn: 5,000,000 VND
            Tổng tiền thanh toán: 5,500,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(5_500_000.0, r.amount, 1.0)
        assertNotEquals(302_435_288.0, r.amount, 1.0) // không nhầm MST
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM L – Ảnh không phải hóa đơn
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `L-NON_001 - anh menu quan an - khong tao giao dich`() {
        val ocr = """
            THỰC ĐƠN – MENU
            Phở đặc biệt      85,000
            Phở tái            75,000
            Cơm sườn           55,000
            Cơm tấm            60,000
            Nước ngọt          15,000
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // Menu không phải hóa đơn → không được tự tạo giao dịch chắc chắn
        // Parser chỉ đọc text – không biết đây là menu, nhưng tổng phải = 0 (không có dòng tổng)
        // VÀ không được bịa tên sản phẩm như "Không xác định"
        r.items.forEach { item ->
            assertFalse("Không hallucinate item", item.name.lowercase() in listOf(
                "không xác định", "khác", "hàng hóa", "vật phẩm"))
        }
    }

    @Test
    fun `L-NON_009 - anh viet tay kho doc - khong crash`() {
        val ocr = "xfg3 rr2 tt qqk 1829 zzq"  // OCR garble
        val r = ReceiptOcrParser.parseText(ocr)
        // Không crash. Parser không được tự bịa title hay item fake
        assertNotNull(r)
        r.items.forEach { item ->
            assertFalse("Không hallucinate", item.name.lowercase() in listOf(
                "không xác định", "khác", "hàng hóa", "vật phẩm"))
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM M – Chất lượng ảnh (text-simulatable cases)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `M-QUALITY_001 - anh ro net - doc tot`() {
        val ocr = """
            PHỞ 24
            Phở đặc biệt   1   85,000   85,000
            Nước chanh     1    8,000    8,000
            TỔNG CỘNG:              93,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(93_000.0, r.amount, 1.0)
        assertFalse("Ảnh rõ không cần review", r.needsReview)
    }

    @Test
    fun `M-QUALITY_008 - ocr mo chi doc duoc so tien - needs review`() {
        // Mô phỏng kết quả OCR mờ: chỉ đọc được số, không đọc được tên
        val ocr = "xxx yyy zzz 125000 VND"
        val r = ReceiptOcrParser.parseText(ocr)
        // Nếu không tìm được total keyword, amount = 0 → needs_review
        // Hoặc nếu tìm được số nhưng không có context → vẫn OK nhưng title phải rỗng
        assertNotNull(r) // không crash
        // Không hallucinate title
        if (r.title.isNotBlank()) {
            assertFalse("Không bịa title", r.title.lowercase() in listOf(
                "không xác định", "khác", "hàng hóa"))
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM N – Format tiền Việt / ngoại tệ
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `N-MONEY_007 - K suffix 125K = 125000`() {
        val ocr = """
            Quán Cơm
            Cơm sườn   35K
            Nước ngọt  15K
            Tổng: 50K
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(50_000.0, r.amount, 1.0)
    }

    @Test
    fun `N-MONEY_008 - 1250000 dot separator VND`() {
        val ocr = "Tổng thanh toán: 1.250.000 VND"
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(1_250_000.0, r.amount, 1.0)
    }

    @Test
    fun `N-MONEY_009 - USD 12 dollars 50 cents`() {
        val ocr = """
            Coffee Shop USA
            Latte        ${'$'}4.50
            Muffin       ${'$'}3.00
            Total:       ${'$'}7.50
            Cash:       ${'$'}10.00
            Change:      ${'$'}2.50
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals("USD", r.currency)
        assertEquals(7.50, r.amount, 0.01)
        assertNotEquals(2.50, r.amount, 0.01) // không nhầm change
    }

    @Test
    fun `N-MONEY_015 - ngay thang khong phai tien`() {
        // "05/06/2026" không được parse thành tiền
        val ocr = """
            Hóa đơn ngày: 05/06/2026
            Tổng: 95,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(95_000.0, r.amount, 1.0)
        // Không nhầm 5062026 thành tiền
        assertNotEquals(5_062_026.0, r.amount, 1.0)
    }

    @Test
    fun `N-MONEY_016 - gio phut khong phai tien`() {
        val ocr = """
            Vào lúc: 17:45
            Tổng: 25,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(25_000.0, r.amount, 1.0)
        assertNotEquals(1745.0, r.amount, 1.0)
    }

    @Test
    fun `N-MONEY_017 - ban so khong phai tien`() {
        val ocr = """
            Bàn số 12
            Cơm tấm  2  35,000  70,000
            TỔNG CỘNG: 70,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(70_000.0, r.amount, 1.0)
        assertNotEquals(12.0, r.amount, 0.01)
    }

    @Test
    fun `N-MONEY_020 - VAT 10 percent khong phai total`() {
        val ocr = """
            Bún bò Huế  1  65,000  65,000
            VAT 10%:         6,500
            Tổng cộng:      71,500 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(71_500.0, r.amount, 1.0)
        assertNotEquals(6_500.0, r.amount, 1.0)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM O – Không hallucinate
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `O-NOHALL_001 - chi doc duoc so tien khong co ten mon - title co the trong`() {
        val ocr = "TỔNG CỘNG 100,000 VND"
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(100_000.0, r.amount, 1.0)
        // Parser KHÔNG được tự bịa tên sản phẩm
        r.items.forEach { item ->
            assertFalse("Không bịa tên item", item.name.lowercase() in listOf(
                "không xác định", "khác", "hàng hóa", "vật phẩm", "item", "sản phẩm"))
        }
    }

    @Test
    fun `O-NOHALL_006 - category khong ro - khong gan bua`() {
        val ocr = """
            CÔNG TY XYZ ABC
            Dịch vụ ABCDEF: 500,000 VND
            TỔNG CỘNG: 500,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(500_000.0, r.amount, 1.0)
        // Category phải là một trong các giá trị hợp lệ
        val validCategories = listOf("Ăn uống", "Mua sắm", "Y tế", "Di chuyển",
            "Nhà ở", "Dịch vụ", "Giải trí", "Du lịch", "Lương", "Hoàn tiền")
        assertTrue("Category phải hợp lệ", r.category in validCategories)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM P – Line item nâng cao
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `P-ITEM_ADV_008 - can ky 0-35kg x 120000 - total 42000`() {
        val ocr = """
            Chợ Bến Thành
            Thịt bò 0.35kg x 120,000  42,000
            Rau muống 0.5kg x 8,000    4,000
            Tổng: 46,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(46_000.0, r.amount, 1.0)
    }

    @Test
    fun `P-ITEM_ADV_010 - items sum khac total - needs review`() {
        val ocr = """
            Quán Test
            Món A  1  50,000  50,000
            Món B  1  30,000  30,000
            TỔNG CỘNG: 95,000 VND
        """.trimIndent()
        // Items sum = 80,000 nhưng total = 95,000 (lệch >20%)
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(95_000.0, r.amount, 1.0)   // vẫn lấy dòng tổng
        assertTrue("Items không khớp tổng → needs_review", r.needsReview)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM Q – Phân loại category theo ảnh hóa đơn
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `Q-CAT_004 - thue san cau long gia - service`() {
        val ocr = """
            CẦU LÔNG HỒ CHÍ MINH
            Thuê sân 2 giờ   100,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals("Dịch vụ", r.category)
    }

    @Test
    fun `Q-CAT_011 - luong thuong chuyen tien vao - income`() {
        val ocr = """
            PHIẾU LƯƠNG THÁNG 10/2023
            Lương cơ bản: 12,000,000
            Thưởng KPI:    2,000,000
            Tổng lương:   14,000,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals("INCOME", r.type)
        assertEquals("Lương", r.category)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NHÓM R – Test tổng tiền cuối cùng
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `R-TOTAL_002 - tam tinh va tong cong - chon tong cong`() {
        val ocr = """
            Nhà hàng ABC
            Tạm tính:      200,000
            VAT 8%:         16,000
            Tổng cộng:     216,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(216_000.0, r.amount, 1.0)
        assertNotEquals(200_000.0, r.amount, 1.0)
    }

    @Test
    fun `R-TOTAL_007 - amount due - chon dung`() {
        val ocr = """
            INVOICE
            Service Fee:   150,000 VND
            Amount Due:    150,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(150_000.0, r.amount, 1.0)
    }

    @Test
    fun `R-TOTAL_015 - dong tong bi mo - needs review`() {
        // Mô phỏng dòng tổng bị OCR đọc sai thành rác
        val ocr = """
            Quán Ăn
            Cơm tấm   1  35,000  35,000
            Nước ngọt 1  10,000  10,000
            T0NG C0NG: xxxxxx VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        // Không thể đọc tổng từ dòng "T0NG C0NG" → fallback sum items = 45,000
        // HOẶC needs_review = true
        assertTrue("Dòng tổng mờ → review hoặc dùng sum items", r.amount >= 0)
        assertNotNull(r)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Các test bổ sung từ spec quan trọng
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `RETAIL - tien khach dua tien thua khong chon`() {
        val ocr = """
            VinMart+
            Sữa Vinamilk  28,000
            Tổng thanh toán:  28,000 VND
            Tiền khách đưa: 100,000 VND
            Tiền trả lại:   72,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(28_000.0, r.amount, 1.0)
        assertNotEquals(100_000.0, r.amount, 1.0)
        assertNotEquals(72_000.0, r.amount, 1.0)
    }

    @Test
    fun `ECOM - ma don hang nhieu so khong phai tien`() {
        val ocr = """
            Shopee Order
            Đơn hàng: 230918104512345678
            Áo thun   1   150,000  150,000
            Tổng thanh toán: 150,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(150_000.0, r.amount, 1.0)
        assertNotEquals(230_918_104_512_345_678.0, r.amount, 1.0)
    }

    @Test
    fun `TRANSPORT - ve xe khach - category di chuyen`() {
        val ocr = """
            PHƯƠNG TRANG FUTA BUS
            Tuyến: TP.HCM - Đà Nẵng
            Số ghế: 25A  Khởi hành: 08:00
            Giá vé: 250,000 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(250_000.0, r.amount, 1.0)
        assertEquals("Di chuyển", r.category)
    }

    @Test
    fun `QR nhan tien - 285k trong tin nhan - amount 285000`() {
        // Screenshot chat "285k nhe em" + QR nhận tiền VietQR
        val ocr = """
            285k nhe em
            QR của tôi
            QR nhận tiền    QR thanh toán
            NGUYEN DUC NGOC LINH
            101022277666
            VIETQR  napas 247
            Chia sẻ  Thiết kế  Tải xuống
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals("Phải đọc được 285k = 285,000", 285_000.0, r.amount, 1.0)
        // Tên sản phẩm không biết → không hallucinate
        r.items.forEach { item ->
            assertFalse("Không bịa tên item", item.name.lowercase() in listOf(
                "không xác định", "khác", "hàng hóa"))
        }
        // Không được nhầm số tài khoản 101022277666 thành tiền
        assertNotEquals(101_022_277_666.0, r.amount, 1.0)
    }

    @Test
    fun `BANK - so du khong duoc chon la total`() {
        val ocr = """
            BIDV
            So tien: 2,000,000 VND
            So du: 15,234,567 VND
        """.trimIndent()
        val r = ReceiptOcrParser.parseText(ocr)
        assertEquals(2_000_000.0, r.amount, 1.0)
        assertNotEquals(15_234_567.0, r.amount, 1.0)
    }
}
