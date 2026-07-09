# download-ocr-models.ps1
# Downloads RapidOCR ONNX model files for Túi Thần Tài Android app.
# Run from repo root: .\scripts\download-ocr-models.ps1

$destDir = "android\app\src\main\assets\ocr\rapidocr"
New-Item -ItemType Directory -Force $destDir | Out-Null

Write-Host "Downloading RapidOCR ONNX models to $destDir ..."

$models = @(
    @{
        Name = "ch_PP-OCRv4_det_infer.onnx"
        Url  = "https://github.com/RapidAI/RapidOCR/releases/download/v1.4.0/ch_PP-OCRv4_det_infer.onnx"
        Size = "~2.3 MB"
    },
    @{
        Name = "ch_PP-OCRv4_cls_infer.onnx"
        Url  = "https://github.com/RapidAI/RapidOCR/releases/download/v1.4.0/ch_PP-OCRv4_cls_infer.onnx"
        Size = "~1.4 MB"
    },
    @{
        Name = "ch_PP-OCRv4_rec_infer.onnx"
        Url  = "https://github.com/RapidAI/RapidOCR/releases/download/v1.4.0/ch_PP-OCRv4_rec_infer.onnx"
        Size = "~10 MB"
    },
    @{
        Name = "ppocr_keys_v1.txt"
        Url  = "https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.7/ppocr/utils/ppocr_keys_v1.txt"
        Size = "~200 KB"
    }
)

foreach ($model in $models) {
    $dest = Join-Path $destDir $model.Name
    if (Test-Path $dest) {
        Write-Host "  SKIP (exists): $($model.Name)"
        continue
    }
    Write-Host "  Downloading $($model.Name) ($($model.Size)) ..."
    try {
        Invoke-WebRequest -Uri $model.Url -OutFile $dest -UseBasicParsing
        Write-Host "  OK: $($model.Name)"
    } catch {
        Write-Warning "  FAILED: $($model.Name) — $($_.Exception.Message)"
        Write-Warning "  Download manually from: $($model.Url)"
    }
}

$total = (Get-ChildItem $destDir -Filter "*.onnx" | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host ""
Write-Host "Done. Total model size: $([math]::Round($total, 1)) MB"
Write-Host "Next: implement RapidOcrEngine.recognize() in android/app/src/main/java/.../ocr/engine/RapidOcrEngine.kt"
Write-Host "See docs/ocr/OPEN_SOURCE_OCR_MODEL_PLAN.md for details."
