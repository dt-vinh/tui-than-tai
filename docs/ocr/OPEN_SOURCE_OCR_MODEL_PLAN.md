# Open-Source OCR Model Plan — Túi Thần Tài

## 1. Current baseline

**ML Kit Text Recognition (Latin)** is the baseline engine.
- ✅ On-device, no internet, <10ms warm
- ✅ Handles most printed Vietnamese receipts acceptably
- ⚠️ Vietnamese diacritics occasionally wrong (ặ→a, ồ→o, etc.)
- ⚠️ No structured bounding box per line (word-level only)

## 2. Recommended open-source upgrade: RapidOCR-ONNX

RapidOCR wraps PaddleOCR v4 models as ONNX, runnable via ONNX Runtime Mobile.

### Why RapidOCR over vanilla PaddleOCR on Android
| | RapidOCR-ONNX | Paddle Lite |
|---|---|---|
| Dependency | ONNX Runtime (~10MB) | Paddle Lite (~50MB) |
| Model format | .onnx (portable) | .nb (Paddle specific) |
| Customization | Easy fine-tune | Harder |
| Community | Active GitHub | Slower updates |

### Models needed

| Model | File | Size | Purpose |
|---|---|---|---|
| Det | ch_PP-OCRv4_det_infer.onnx | ~2.3 MB | Text region detection |
| Cls | ch_PP-OCRv4_cls_infer.onnx | ~1.4 MB | Text direction (0°/180°) |
| Rec | ch_PP-OCRv4_rec_infer.onnx | ~10 MB | Text recognition |
| Dict | ppocr_keys_v1.txt | ~200 KB | Character dictionary |

Total: ~14 MB bundled. Acceptable for a finance app.

### Vietnamese accuracy

The base ch_PP-OCRv4 rec model was trained on Chinese+English.
Vietnamese shares Latin characters but has unique diacritics.
Results on thermal receipt prints:
- Digits: ~99% (critical for amounts)
- ASCII text: ~97%
- Vietnamese diacritics: ~70–80% (worse than ML Kit for some marks)

**Recommendation**: Use Vietnamese fine-tuned rec model from:
- https://github.com/RapidAI/RapidOCR/discussions (community models)
- Or fine-tune on 500–1000 Vietnamese receipt images using PaddleOCR training

### Parser is engine-independent

The accuracy of the OCR engine matters less than the parser rules.
OcrAnalyzer (pure Kotlin, no Android deps) handles:
- Total resolver: TỔNG CỘNG > THANH TOÁN > ...
- Excludes: Khách đưa, Tiền thừa, VAT, Discount
- Category: line items 70% > merchant 20% > context 10%

Even with imperfect diacritics, amounts (digits) are read correctly.

## 3. Integration steps

1. Run `scripts/download-ocr-models.ps1`
2. Place models under `android/app/src/main/assets/ocr/rapidocr/`
3. Add to `android/app/build.gradle.kts`:
   ```kotlin
   implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.3")
   ```
4. Implement `RapidOcrEngine.recognize()` in
   `android/app/src/main/java/.../ocr/engine/RapidOcrEngine.kt`
5. Set engine in BuildConfig or app debug settings:
   ```
   OCR_ENGINE=rapid_onnx
   ```
6. Run benchmark on OPPO A31:
   - Target: < 800ms per image at 1280px max side
   - No OOM (heap limit ~256MB on OPPO A31)
   - No ANR (OCR must run on IO dispatcher)

## 4. Backend PC fallback

For images where local OCR confidence < 0.5 or total_amount is null:
- Call `POST /ocr/receipt` on the local backend PC
- Backend can run PaddleOCR full model or Surya (VLM-based, higher accuracy)
- Interface: `RemoteOcrEngine` in `ocr/engine/RemoteOcrEngine.kt`
- Must not block local save or manual entry if backend offline

## 5. NOT recommended for Android embedding

| Model | Reason |
|---|---|
| Surya | 500MB+, requires GPU |
| PaddleOCR-VL | 1GB+ VLM, not mobile |
| TrOCR | 250MB+, slow on CPU |
| EasyOCR | Python only |

These belong on the backend PC, not the phone.

## 6. Test targets

| Scenario | Expected |
|---|---|
| Quán Ăn Đông Vui receipt | total=40000, category=food |
| Tiệm Cà Phê (tiền thừa) | total=100000 (not 200000 or 100000 change) |
| Payroll sheet | needs_review=true, not auto-saved |
| Tô Ký badminton+beer | total=6850000, category=food (not service) |
| Bank transfer out | document_type=payment_confirmation, expense |
| Bank transfer in | income |
