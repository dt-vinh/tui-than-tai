# OCR Input Casebook - Túi Thần Tài / Lucky Wallet
Phiên bản: v1.0
Phạm vi: Chỉ tập trung vào OCR/AI Scan. Không mô tả các module khác của app.

## 1. Mục tiêu
Tài liệu này là casebook để Claude Code/Codex triển khai OCR theo hướng ít sai nguy hiểm: đọc đúng tổng tiền, không chọn nhầm số, không bịa tên hàng hóa, biết khi nào cần user review.

## 2. Căn cứ research ngắn
- Google ML Kit Text Recognition trên Android trả kết quả theo cấu trúc text/block/line/element, nên OCR parser phải tận dụng line và layout thay vì chỉ dùng raw text.
- Các API receipt/invoice extraction phổ biến đều coi merchant, ngày, line items, tax, total, currency/payment method là field cốt lõi; vì vậy output của app phải là structured JSON, không chỉ raw text.
- Receipt thực tế có nhiều chất lượng và layout khác nhau: giấy in nhiệt mờ, nhàu, dài, ảnh nghiêng, ảnh cắt, screenshot thanh toán, e-invoice, hóa đơn VAT.
- Payment confirmation/screenshot ngân hàng là input quan trọng cho app tài chính cá nhân, cần extract amount, date, beneficiary/sender, transaction reference và direction tiền vào/tiền ra.

## 3. Triết lý OCR
OCR không phải là bước tự động lưu giao dịch. OCR chỉ tạo đề xuất. Nếu không chắc, để trống và đánh dấu needs_review.

### 3.1. Không hallucinate
Nếu không đọc rõ tên sản phẩm/dịch vụ, trường name/title phải để rỗng. Không được tự điền: Không xác định, Khác, Hàng hóa, Vật phẩm, Sản phẩm, Receipt, Bill.

### 3.2. Ưu tiên xử lý
1. Tổng tiền cuối cùng. 2. Currency. 3. Loại giao dịch income/expense. 4. Danh mục. 5. Merchant/counterparty. 6. Line items.

## 4. Document types user có thể chụp
| Nhóm | Ví dụ | Output cốt lõi | Cảnh báo |
|---|---|---|---|
| POS giấy - quán ăn/cafe | Ảnh hóa đơn in nhiệt, có tên quán, bàn, giờ vào/giờ ra, mặt hàng, SL, đơn giá, tổng | merchant, date/time, items, total_amount, category=food_and_drink | Đừng chọn giờ/bàn làm tiền; ưu tiên Tổng cộng/Tổng thanh toán |
| Hóa đơn tạm tính nhà hàng | Có tiêu đề HÓA ĐƠN TẠM TÍNH, có Tiền hàng, Phí dịch vụ/VAT, Tổng tạm tính | items, subtotal, service_fee/tax nếu có, final total | Nếu chỉ có Tổng tạm tính cuối bill thì chọn làm total |
| Quán bia/đồ nhắm tại sân thể thao | Merchant là sân thể thao, item là bia, khô cá, trứng, đồ ăn | category=food_and_drink, merchant giữ nguyên | Không phân loại service chỉ vì merchant có chữ sân |
| Hóa đơn thuê sân/dịch vụ thể thao | Item có thuê sân, giờ sân, phí sân, pickleball, cầu lông | category=service hoặc entertainment | Line item quyết định category |
| Siêu thị/minimart dài | Nhiều item, mã hàng, VAT, khuyến mại, tổng, khách đưa, tiền thừa | items, discounts, tax, total_amount | Không chọn khách đưa/tiền thừa; line items có thể không cần 100% |
| Nhà thuốc | Tên thuốc, số lượng, đơn vị, total | category=health | Tên thuốc khó đọc thì để name rỗng hoặc needs_review |
| Cây xăng | Lít, đơn giá/lít, thành tiền, mã trụ bơm | category=transport, item=fuel | Không nhầm số lít với tiền |
| Taxi/Grab/Be/Gojek receipt | Điểm đi/đến, cước, phí, khuyến mại, tổng | category=transport, total_amount | Không parse địa chỉ thành item |
| Vé gửi xe/parking | Biển số, giờ vào/ra, phí gửi xe | category=transport hoặc service | Không nhầm biển số/giờ thành tiền |
| Hóa đơn điện/nước/internet/điện thoại | Kỳ cước, mã KH, tiền trước thuế, VAT, tổng thanh toán | category=bills, total_amount | Không chọn mã khách hàng/mã hóa đơn làm tiền |
| Hóa đơn điện tử Việt Nam - ảnh/PDF screenshot | Có ký hiệu, số hóa đơn, MST, tên hàng, VAT, tổng tiền thanh toán, QR/mã CQT | invoice_number, merchant, tax, total_amount, items nếu có | Không cần lưu MST/số hóa đơn vào title; tổng thanh toán là total |
| Màn hình chuyển khoản thành công - tiền ra | Có số tiền, người nhận, ngân hàng, nội dung, mã giao dịch, thời gian | transaction_type=expense, amount, counterparty, note | Không có line items; title có thể là nội dung CK nếu rõ |
| Màn hình nhận tiền - tiền vào | Có +amount hoặc “nhận tiền thành công”, người gửi, nội dung | transaction_type=income, amount, counterparty, note | Không gán expense khi có dấu +/nhận tiền |
| Ví điện tử Momo/ZaloPay/VNPay | Thanh toán thành công, merchant, amount, mã GD | expense, merchant, amount | Không nhầm số điện thoại/mã GD với amount |
| QR payment/order checkout screenshot | Có merchant, amount, nội dung thanh toán, chưa chắc đã thành công | amount candidate, status nếu có | Nếu chưa có trạng thái thành công thì needs_review |
| E-commerce order Shopee/Tiki/Lazada | Tổng tiền hàng, phí ship, voucher, thanh toán, nhiều item | shopping, items, shipping_fee, discount, total_amount | Chọn thành tiền thanh toán cuối cùng |
| Phiếu thu/biên lai dịch vụ | Có người nộp, nội dung thu, số tiền, chữ ký | expense/service hoặc income tùy ngữ cảnh | Nếu là phiếu thu của người bán, với user có thể là expense |
| Hóa đơn khách sạn/du lịch | Phòng, đêm, thuế, phí dịch vụ, tổng | travel, service, total_amount | Không nhầm số phòng/số đêm với tiền |
| Phiếu order bếp/bar chưa thanh toán | Có món và số lượng nhưng không có tổng hoặc chưa thanh toán | items maybe, total=null, needs_review | Không tự lưu nếu chưa có total |
| Ảnh sản phẩm/menu/bảng giá | Chỉ có tên món và giá, không phải giao dịch đã xảy ra | document_type=not_receipt, total=null | Không tạo giao dịch tự động |
| Tin nhắn SMS ngân hàng | Text screenshot có số tiền, biến động số dư, nội dung | income/expense, amount, balance nếu có | Phân biệt biến động với số dư cuối |
| Sao kê/bảng danh sách nhiều giao dịch | Nhiều dòng debit/credit, số dư | document_type=statement, needs_review | Không gộp toàn bộ thành 1 giao dịch nếu user không chọn dòng |
| Hóa đơn viết tay/chợ truyền thống | Tên món viết tay, cộng tay, số tiền | total nếu rõ, items confidence thấp | Không hallucinate item name |
| Biên lai y tế/phòng khám | Dịch vụ khám, thuốc, xét nghiệm, tổng | health, items/service, total | Phân loại health |
| Ảnh không liên quan | Cảnh vật, người, vật phẩm, không text tiền | not_receipt, no transaction | Trả total=null, title="", needs_review=true |

## 5. Money parsing cases
| Case | Input | Setting | Expected | Note |
|---|---|---|---|---|
| OCR-MONEY-001 | `125000` | VND default | `125000` | Plain integer VND |
| OCR-MONEY-002 | `125.000đ` | VND | `125000` | Dot thousands |
| OCR-MONEY-003 | `125,000 đ` | VND | `125000` | Comma thousands |
| OCR-MONEY-004 | `1 250 000 VND` | VND | `1250000` | Space thousands |
| OCR-MONEY-005 | `125K` | VND | `125000` | K suffix |
| OCR-MONEY-006 | `6,85O,OOO đ` | VND | `6850000` | OCR O->0 |
| OCR-MONEY-007 | `$12.50` | USD | `12.50` | Dollar sign |
| OCR-MONEY-008 | `12,50 USD` | USD vi | `12.50` | Vietnamese decimal comma |
| OCR-MONEY-009 | `17:45` | None | `null` | Time must not be amount |
| OCR-MONEY-010 | `05/06/2026` | None | `null` | Date must not be amount |

## 6. Image quality cases
| Case | Input | Expected behavior |
|---|---|---|
| OCR-IMG-001 | Ảnh rõ, thẳng, đủ hóa đơn | Parse full fields, confidence high |
| OCR-IMG-002 | Ảnh nghiêng 5-15 độ | Auto-rotate/crop, confidence medium/high |
| OCR-IMG-003 | Ảnh nghiêng nặng >30 độ | Try deskew; if fail ask retake/needs_review |
| OCR-IMG-004 | Ảnh mờ do rung tay | Do not hallucinate; confidence low |
| OCR-IMG-005 | Ảnh tối/thiếu sáng | Preprocess contrast; still needs_review if unclear |
| OCR-IMG-006 | Hóa đơn bị cắt mất dòng tổng | total=null; ask user input |
| OCR-IMG-007 | Hóa đơn dài, chỉ chụp phần đầu | items maybe; total=null |
| OCR-IMG-008 | Hóa đơn dài, chụp toàn bộ nhưng chữ nhỏ | Prefer total; items optional confidence low |
| OCR-IMG-009 | Giấy in nhiệt mờ/phai | Extract only clear fields; no fake names |
| OCR-IMG-010 | Nền phức tạp, bill đặt trên bàn | Crop receipt; avoid reading background text |

## 7. Rule chọn tổng tiền cuối cùng
Keyword ưu tiên cao: TỔNG CỘNG, TỔNG THANH TOÁN, TỔNG TẠM TÍNH, THANH TOÁN, PHẢI TRẢ, GRAND TOTAL, TOTAL, AMOUNT DUE.
Keyword không chọn nếu có lựa chọn khác: Tiền hàng, Tạm tính, VAT, Thuế, Giảm giá, Chiết khấu, Khách đưa, Tiền thừa, Change, Cash, Received, Balance.

## 8. Rule phân loại danh mục
Phân loại theo thứ tự: line items 70%, merchant 20%, document title/context 10%. Ví dụ merchant là sân thể thao nhưng item là bia/khô cá/trứng thì category=food_and_drink.

## 9. Required JSON schema
```json
{"document_type":"pos_receipt|temporary_receipt|e_invoice|payment_confirmation|utility_bill|statement|not_receipt","transaction_type":"expense|income|unknown","merchant_name":null,"counterparty_name":null,"category_id":"other","currency":"VND|USD|unknown","total_amount":null,"amount_candidates":[],"items":[],"date_time":null,"payment_method":null,"confidence":0.0,"needs_user_review":true,"review_fields":[],"reason":""}
```

## 10. Golden examples
### 10.1. Hóa đơn sân cầu lông Tô Ký
Expected: merchant=Sân cầu lông & pickleball Tô Ký, total_amount=6850000, currency=VND, category=food_and_drink. Lý do: item là bia/đồ ăn/đồ nhắm, không có dòng thuê sân.

### 10.2. Bank transfer out
Expected: document_type=payment_confirmation, transaction_type=expense, không có line items, extract amount/beneficiary/bank/time/reference/note.

### 10.3. Bank transfer in
Expected: transaction_type=income nếu màn hình có dấu +, nhận tiền thành công, người gửi hoặc số dư tăng.

### 10.4. Product photo/not receipt
Expected: document_type=not_receipt, total_amount=null, title="", items=[], needs_user_review=true.

## 11. Test acceptance for Claude/Codex
- Có test cho money parser VND/USD.
- Có test cho total resolver không chọn khách đưa/tiền thừa/VAT/discount.
- Có test cho line item parser.
- Có test cho category classifier ưu tiên line items.
- Có test no hallucination: ảnh không rõ thì name/title rỗng.
- Có fixture Tô Ký.
- Build/test pass trước khi báo hoàn thành.

## 12. Nguồn tham khảo
- Google ML Kit Text Recognition Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Amazon Textract - Analyzing Invoices and Receipts: https://docs.aws.amazon.com/textract/latest/dg/invoices-receipts.html
- Microsoft Document Intelligence - Receipt model: https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/prebuilt/receipt?view=doc-intel-4.0.0
- Receipt OCR guide - structured fields and challenges: https://invoicedataextraction.com/blog/receipt-ocr-guide
- Receipt OCR API - messy receipts, faded/crumpled receipts, line items: https://structocr.com/developers/receipt-ocr
- Payment confirmation OCR fields: https://www.koncile.ai/en/extraction-ocr/payment-confirmation
- Vietnam e-invoice requirements overview: https://www.getharvest.com/invoices/receipt-generator-for-vietnam
