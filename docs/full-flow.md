# Full App Flow Implemented

This file records the full Android flow implemented after the video review.

## Entry Flow

1. Splash screen.
2. Onboarding:
   - Scan receipt.
   - Capture object.
   - Visual reports.
3. User chooses:
   - `Trải nghiệm ngay`: continue offline/local-first.
   - `Đăng nhập / Đăng ký`: open auth flow.

## Main Navigation

The app now has five bottom tabs:

- `Home`: month total, calendar strip, manual entry, capture FAB, recent expenses.
- `Sổ`: all transactions with quick filters.
- `Reports`: category totals and monthly summary.
- `Công cụ`: full video-derived module list.
- `Settings`: account/sync shortcut, language, backend config.

## Capture And Expense Flow

1. From Home FAB or Tools > Scan / AI ghi chi.
2. CameraX preview opens.
3. User captures receipt/object or chooses manual entry.
4. Review screen opens.
5. ML Kit tries to extract:
   - Amount.
   - Title.
   - Category.
   - OCR text.
6. User confirms/edits amount, title, category, wallet, note.
7. Expense saves to Room as pending sync.
8. WorkManager retries backend sync when online and logged in.

## Tools Flow

Tools tab includes the main modules seen in Cap money and MISA, simplified for this app:

- Scan / AI ghi chi.
- Ví & tài khoản.
- Ngân sách.
- Danh mục.
- Giao dịch định kỳ.
- Chia tiền.
- Đồng bộ.

## Emulator Verification

Verified on AVD `SafeSign_API35`:

- Installed `app-debug.apk`.
- Launched `com.phuongnn14.tuithantai/.MainActivity`.
- Confirmed foreground focus with `dumpsys window`.
- Captured screenshots:
  - `emulator-tui-than-tai-loaded.png`
  - `emulator-tui-than-tai-home-real.png`
  - `emulator-tui-than-tai-tools-real.png`

Note: the AVD initially showed Android system-service ANRs (`System UI`, `Digital Wellbeing`). Disabling `com.google.android.apps.wellbeing` and restarting the emulator resolved the blocker; app logcat showed no app crash.
