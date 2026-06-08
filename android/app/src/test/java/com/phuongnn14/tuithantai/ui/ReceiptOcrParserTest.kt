package com.phuongnn14.tuithantai.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Test ReceiptOcrParser với nhiều loại hóa đơn thực tế từ Việt Nam.
 * Mỗi test giả lập chính xác text mà ML Kit OCR sẽ đọc ra từ ảnh.
 */
class ReceiptOcrParserTest {

    // ────────────────────────────────────────────────────────────
    // 1. TIỆM CÀ PHÊ GIA ĐÌNH – Bill thực tế từ ảnh người dùng
    // ────────────────────────────────────────────────────────────

    @Test
    fun `cafe gia dinh - tong 100000 khong lay tien nhan 200000`() {
        val ocr = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Địa chỉ: 45 Đường Hoàng Diệng, Diệu,
            Q. Bình Thạnh, TP.HCM
            SỐ HĐ: 112346
            NGÀY: 22/10/2023
            GIỜ: 10:30
            TÊN MÓN SL ĐƠN GIÁ TỔNG
            Cà Phê Sữa Đá (L) 2 35,000 70,000
            Bánh Croissant 1 25,000 25,000
            Nước Suối (L) 1 5,000 5,000
            TỔNG CỘNG (TOTAL): 100,000 VND
            THANH TOÁN: TIỀN MẶT (CASH)
            Tiền nhận: 200,000 VND
            Tiền thối: 100,000 VND
            Cảm ơn sự ủng hộ của Quý khách!
            Hẹn gặp lại!
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("Tổng phải là 100,000 không phải 200,000", 100_000.0, result.amount, 1.0)
        assertEquals("VND", result.currency)
        assertEquals("EXPENSE", result.type)
        assertEquals("TIỆM CÀ PHÊ GIA ĐÌNH", result.merchantName)
        assertTrue("Phải có items", result.items.isNotEmpty())
    }

    @Test
    fun `cafe gia dinh - parse dung 3 mon`() {
        val ocr = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            TÊN MÓN SL ĐƠN GIÁ TỔNG
            Cà Phê Sữa Đá (L) 2 35,000 70,000
            Bánh Croissant 1 25,000 25,000
            Nước Suối (L) 1 5,000 5,000
            TỔNG CỘNG (TOTAL): 100,000 VND
            Tiền nhận: 200,000 VND
            Tiền thối: 100,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(3, result.items.size)
        val caphe = result.items.find { it.name.contains("Cà Phê", ignoreCase = true) || it.name.contains("Ca Phe", ignoreCase = true) }
        assertNotNull("Phải tìm được Cà Phê", caphe)
        caphe?.let {
            assertEquals(2, it.quantity)
            assertEquals(35_000.0, it.price, 500.0)
            assertEquals(70_000.0, it.total, 500.0)
        }
    }

    @Test
    fun `cafe gia dinh - category an uong`() {
        val ocr = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Cà Phê Sữa Đá (L) 2 35,000 70,000
            TỔNG CỘNG: 100,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("Ăn uống", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 2. QUÁN ĂN ĐÔNG VUI – Hóa đơn nhà hàng đơn giản
    // ────────────────────────────────────────────────────────────

    @Test
    fun `dong vui - tong 40000 vnd`() {
        val ocr = """
            QUÁN ĂN ĐÔNG VUI
            SỐ HĐ: 004567
            NGÀY: 20/10/2023
            SL  TÊN MÓN       ĐƠN GIÁ  TỔNG
            1   Cơm Tấm Sườn  35,000   35,000
            1   Trà Đá         5,000    5,000
            TỔNG CỘNG (TOTAL): 40,000 VND
            THANH TOÁN: TIỀN MẶT
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(40_000.0, result.amount, 1.0)
        assertEquals("VND", result.currency)
        assertEquals(2, result.items.size)
    }

    // ────────────────────────────────────────────────────────────
    // 3. HIGHLANDS COFFEE – Chain cà phê lớn
    // ────────────────────────────────────────────────────────────

    @Test
    fun `highlands coffee - tong 139000`() {
        val ocr = """
            HIGHLANDS COFFEE
            227 Đồng Khởi, Q.1, TP.HCM
            HĐ: 20231022-0891
            Ngày: 22/10/2023 14:15
            Sản phẩm        SL  Đơn giá   Thành tiền
            Trà Đào Cam Sả   1   55,000     55,000
            Cold Brew        1   65,000     65,000
            Bánh Mì Phô Mai  1   19,000     19,000
            Thành tiền:              139,000
            VAT (8%):                 11,120
            Tổng cộng:               139,000 VND
            Tiền khách đưa:          200,000
            Tiền thừa:                61,000
            Cảm ơn!
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("Không được lấy tiền thừa 61,000", 139_000.0, result.amount, 500.0)
        assertEquals(3, result.items.size)
        assertEquals("Ăn uống", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 4. SIÊU THỊ CO.OPMART – Hóa đơn siêu thị nhiều mặt hàng
    // ────────────────────────────────────────────────────────────

    @Test
    fun `coopmart - tong 345000 nhieu mon`() {
        val ocr = """
            Co.opmart Supermarket
            45 Nguyễn Thị Minh Khai, Q.3
            HD: SM20231022-1234
            Ngày: 22/10/2023
            Mã SP    Tên sản phẩm             SL  Đơn giá   Thành tiền
            SP001    Mì Hảo Hảo (lốc 10)       1   35,000     35,000
            SP002    Nước mắm Phú Quốc 500ml    1   45,000     45,000
            SP003    Đường trắng 1kg             2   28,000     56,000
            SP004    Sữa tươi Vinamilk 1L        3   35,000    105,000
            SP005    Bánh Oreo                   2   22,000     44,000
            SP006    Xà phòng Lifebuoy           2   30,000     60,000
            Tổng tiền hàng:              345,000 VND
            Giảm giá thành viên:          -17,250
            Tổng thanh toán:             327,750 VND
            Tiền khách:                  350,000
            Tiền thừa:                    22,250
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        // Tổng thanh toán thực tế sau giảm giá
        assertTrue("Tổng phải > 0", result.amount > 0.0)
        assertNotEquals("Không được lấy tiền thừa 22,250", 22_250.0, result.amount, 1.0)
        assertEquals("Mua sắm", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 5. GRAB – Thông báo thanh toán Grab Food
    // ────────────────────────────────────────────────────────────

    @Test
    fun `grab food - thanh toan 85000`() {
        val ocr = """
            GrabFood
            Đơn hàng #GF-2310221456
            Ngày: 22/10/2023 12:00
            Từ: Bún Bò Huế Số 1
            Bún Bò Huế đặc biệt  x1   65,000
            Chả giò              x2   10,000    20,000
            Phí giao hàng:                       0
            Tổng đơn hàng:                   85,000 VND
            Thanh toán: Ví GrabPay
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(85_000.0, result.amount, 1.0)
        assertEquals("EXPENSE", result.type)
    }

    // ────────────────────────────────────────────────────────────
    // 6. MB BANK SMS – Chuyển khoản ngân hàng
    // ────────────────────────────────────────────────────────────

    @Test
    fun `mb bank transfer - so tien 500000`() {
        val ocr = """
            MB Bank
            Thong bao bien dong so du
            So TK: 0941234567
            So tien: 500,000 VND
            So du: 2,345,678 VND
            Noi dung: Chuyen khoan tien an
            Thoi gian: 22/10/2023 10:30:45
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        // Parser nên tìm được số tiền giao dịch, không phải số dư
        assertTrue("Phải có số tiền", result.amount > 0.0)
        assertNotEquals("Không được lấy số dư 2,345,678", 2_345_678.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 7. TARGET STORE – Hóa đơn USD nước ngoài
    // ────────────────────────────────────────────────────────────

    @Test
    fun `target store - usd receipt 7 dollars`() {
        val ocr = """
            Target Store
            123 Main St, Los Angeles, CA
            Receipt #: 45678
            Date: 10/22/2023
            Item            Qty  Price   Total
            Whole Milk 1gal  1   $4.50   $4.50
            Wheat Bread      1   $2.50   $2.50
            Total:                       $7.00
            Cash:                       $10.00
            Change:                      $3.00
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("USD", result.currency)
        assertEquals(7.0, result.amount, 0.1)
        assertNotEquals("Không lấy tiền thừa 3.00", 3.0, result.amount, 0.01)
    }

    // ────────────────────────────────────────────────────────────
    // 8. PHÒNG KHÁM – Hóa đơn y tế
    // ────────────────────────────────────────────────────────────

    @Test
    fun `phong kham - hoa don y te 850000`() {
        val ocr = """
            PHÒNG KHÁM ĐA KHOA TÂN PHƯỚC
            Địa chỉ: 78 Nguyễn Văn Trỗi, TP.HCM
            HD: PK20231022-089
            Bệnh nhân: Nguyễn Văn A
            Ngày: 22/10/2023
            Dịch vụ                       Số tiền
            Khám tổng quát                200,000
            Siêu âm ổ bụng               350,000
            Xét nghiệm máu               300,000
            TỔNG CỘNG:                   850,000 VND
            Đã thu:                       850,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(850_000.0, result.amount, 1.0)
        assertEquals("Y tế", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 9. XE ÔM / PARKING – Phiếu gửi xe
    // ────────────────────────────────────────────────────────────

    @Test
    fun `gui xe - tieu di chuyen 10000`() {
        val ocr = """
            BÃI GIỮ XE NGUYỄN TRI PHƯƠNG
            Số phiếu: 00456
            Vào: 08:30 22/10/2023
            Ra: 12:30 22/10/2023
            Loại xe: Xe máy
            Số tiền: 10,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(10_000.0, result.amount, 1.0)
        assertEquals("Di chuyển", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 10. ĐIỆN NƯỚC – Hóa đơn tiện ích
    // ────────────────────────────────────────────────────────────

    @Test
    fun `hoa don dien - nha o category`() {
        val ocr = """
            CÔNG TY ĐIỆN LỰC TP.HCM
            Hóa đơn tiền điện tháng 10/2023
            Khách hàng: Nguyễn Thị B
            Địa chỉ: 45 Lý Tự Trọng, Q.1
            Số điện sử dụng: 210 kWh
            Tiền điện: 450,000 VND
            Thuế VAT 10%: 45,000 VND
            Tổng thanh toán: 495,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(495_000.0, result.amount, 1.0)
        assertEquals("Nhà ở", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 11. PHỞ 24 – OCR text bị nhiễu, nhận dạng kém (góc chụp xéo)
    // ────────────────────────────────────────────────────────────

    @Test
    fun `pho 24 - ocr nhieu but van lay duoc tong`() {
        val ocr = """
            PH0 24 (OCR lỗi: 0 thay O)
            Ha N0i - H0 Chi Minh
            Ph0 Đặc Biệt  1  85.000  85.000
            Nư0c Lọc      1   5.000   5.000
            T0NG C0NG:           90.000 VND
            Tiền nhận: 100.000 VND
            Tiền thối:  10.000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        // Dấu . thay vì , (OCR nhầm)
        assertTrue("Phải parse được tổng > 0", result.amount > 0.0)
        assertNotEquals("Không lấy tiền thối 10,000", 10_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 12. LOTTE MART – Hóa đơn siêu thị với dấu chấm thay phẩy
    // ────────────────────────────────────────────────────────────

    @Test
    fun `lotte mart - dau cham thay phay 256000`() {
        val ocr = """
            LOTTE Mart
            Tầng B1, 469 Nguyễn Hữu Thọ, Q.7
            Ngày: 22/10/2023
            Sản phẩm             SL  Giá       Thành tiền
            Gạo Jasmine 5kg       1  120.000   120.000
            Trứng gà 10 quả       2   33.000    66.000
            Nước Dasani 6L        1   35.000    35.000
            Dầu ăn Simply 1L      1   35.000    35.000
            Tổng cộng:                          256.000 VND
            Tiền khách đưa:                     300.000 VND
            Tiền trả lại:                        44.000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(256_000.0, result.amount, 500.0)
        assertNotEquals("Không lấy tiền trả lại 44,000", 44_000.0, result.amount, 1.0)
        assertEquals("Mua sắm", result.category)
    }

    // ────────────────────────────────────────────────────────────
    // 13. VINMART+ – Hóa đơn nhỏ ít món
    // ────────────────────────────────────────────────────────────

    @Test
    fun `vinmart - 2 mon tong 55000`() {
        val ocr = """
            VinMart+
            12 Nguyễn Đình Chiểu, Q.1
            Sữa tươi Vinamilk   1  28,000  28,000
            Bánh mì sandwich    3   9,000  27,000
            Tổng:                          55,000
            Cash:                         100,000
            Change:                        45,000
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(55_000.0, result.amount, 500.0)
        assertNotEquals("Không lấy tiền thừa", 45_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 14. PIZZA HUT – Hóa đơn có VAT riêng dòng
    // ────────────────────────────────────────────────────────────

    @Test
    fun `pizza hut - tong co vat 276000`() {
        val ocr = """
            PIZZA HUT
            Vincom Center, 72 Lê Thánh Tôn, Q.1
            Marge Pizza Pepperoni  1  180,000  180,000
            Mozzarella Sticks      1   65,000   65,000
            Pepsi lon              2   15,000   30,000
            Subtotal:                           275,000
            VAT 10%:                             27,500
            Discount VIP 10%:                   -27,500
            Tổng thanh toán:                    275,000 VND
            Tiền khách:                         300,000
            Tiền thối:                           25,000
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(275_000.0, result.amount, 1_000.0)
        assertNotEquals("Không lấy tiền thối", 25_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 15. PHIẾU LƯƠNG – Income, không phải expense
    // ────────────────────────────────────────────────────────────

    @Test
    fun `phieu luong - type income 15800000`() {
        val ocr = """
            CÔNG TY CỔ PHẦN CÔNG NGHỆ ABC
            PHIẾU LƯƠNG THÁNG 10/2023
            Nhân viên: Trần Văn B  |  Mã NV: NV00234
            Lương cơ bản:                 15,000,000
            Phụ cấp ăn trưa:                 800,000
            Tổng lương:                   15,800,000 VND
            Đã khấu trừ thuế TNCN:           -500,000
            Thực nhận:                    15,300,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("INCOME", result.type)
        assertEquals("Lương", result.category)
        assertTrue("Tổng phải > 10 triệu", result.amount > 10_000_000.0)
    }

    // ────────────────────────────────────────────────────────────
    // 16. Bill chỉ có 1 dòng số, không có header
    // ────────────────────────────────────────────────────────────

    @Test
    fun `bill don gian 1 dong so - phai lay duoc so tien`() {
        val ocr = """
            Nước mía Thanh Hương
            Tổng: 15,000đ
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(15_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 17. GỌI MÓN TRÁNH BỊ NHẦM: "Tiền nhận" lớn hơn tổng
    // ────────────────────────────────────────────────────────────

    @Test
    fun `tong nho hon tien nhan - khong nham tong`() {
        val ocr = """
            Quán Cơm Bình Dân
            Cơm sườn     1  30,000  30,000
            Canh chua     1  10,000  10,000
            TỔNG CỘNG:          40,000 VND
            Tiền nhận: 500,000 VND
            Tiền thối: 460,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals("Phải lấy 40,000 không phải 500,000", 40_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 18. Bill không có dòng tổng – tính từ items
    // ────────────────────────────────────────────────────────────

    @Test
    fun `khong co dong tong - tinh tu items`() {
        val ocr = """
            Trà Sữa The Alley
            Trà Sữa Trân Châu  1  55,000  55,000
            Bánh Mochi          2  15,000  30,000
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        // 55,000 + 30,000 = 85,000 tính từ items
        assertEquals(85_000.0, result.amount, 1.0)
    }

    // ────────────────────────────────────────────────────────────
    // 19. Hóa đơn số tiền 0 / bị lỗi OCR hoàn toàn
    // ────────────────────────────────────────────────────────────

    @Test
    fun `ocr that bai - amount = 0 khong crash`() {
        val ocr = "xxxxxxxxxxx blurry image cannot read"
        val result = ReceiptOcrParser.parseText(ocr)
        // Không crash, amount = 0.0
        assertEquals(0.0, result.amount, 0.01)
        assertNotNull(result)
    }

    // ────────────────────────────────────────────────────────────
    // 20. STARBUCKS – Bill Tiếng Anh lẫn Tiếng Việt
    // ────────────────────────────────────────────────────────────

    @Test
    fun `starbucks bilingual - total 125000`() {
        val ocr = """
            Starbucks Coffee Vietnam
            Vincom Mega Mall Royal City
            Order #: 1234  Table: 05
            Caramel Macchiato (L)  1   85,000   85,000
            Cheese Cake            1   40,000   40,000
            Total / Tổng cộng:          125,000 VND
            Customer paid:              200,000 VND
            Change / Tiền thối:          75,000 VND
        """.trimIndent()
        val result = ReceiptOcrParser.parseText(ocr)
        assertEquals(125_000.0, result.amount, 1.0)
        assertNotEquals("Không lấy tiền thối 75,000", 75_000.0, result.amount, 1.0)
        assertEquals("Ăn uống", result.category)
    }
}
