# CLAUDE.md - Tui Than Tai / Lucky Wallet

## Project context

You are working on `dt-vinh/tui-than-tai`, a production-oriented Android expense/income capture app for Vietnamese and English users.

Repository URL: https://github.com/dt-vinh/tui-than-tai

Current repo areas:
- `android/`: Kotlin Jetpack Compose Android app scaffold.
- `backend/`: self-hosted Node.js API, SQLite database, local receipt uploads.
- `docs/`: PRD, workflows, test cases.
- `scripts/run-full-tests.ps1`: backend tests, Android JVM tests, debug/release builds, emulator smoke.

Canonical product spec for this session:
- Read `docs/PRD_FOR_CLAUDE_CODE.md` first.
- If the file `PRD_chuan_hoa_Tui_Than_Tai_v1.0.docx` exists in the repo or attached context, treat it as the source PRD, but use `docs/PRD_FOR_CLAUDE_CODE.md` as the implementation-ready summary.

## Primary goals

1. Build a working debug APK.
2. Install and debug on OPPO A31.
3. Implement PRD-critical flows without breaking offline-first behavior.
4. Keep backend as PC-hosted Node.js + SQLite + uploads, public through Cloudflare Tunnel.
5. Ensure app auto-syncs; users should not manually configure backend or press sync.

## Required local commands

PowerShell commands from repo root:

```powershell
cd backend
npm start
```

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-full-tests.ps1
```

For OPPO A31 debugging:

```powershell
adb devices
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb logcat
```

## Android device target

Target physical device: OPPO A31.
Treat it as a real lower/mid-range Android device; prioritize:
- Runtime stability.
- Camera permission and CameraX compatibility.
- UI on 720px-class width.
- Smooth Compose screens.
- Offline-first behavior.
- App not crashing when backend PC/tunnel is unavailable.

## Product rules

### Auth

- Register requires only `name` and `email`.
- User may link/sign in with Google.
- After successful registration, auto-login.
- After login, do not show login buttons on Home, Reports, Tools.
- Logout is only in Settings.

### Language

- Only two languages: Vietnamese and English.
- Changing language must show a confirmation popup before applying.

### Money display

- VND in Vietnamese: `1.250.000 ₫`, no decimals.
- USD in English: `$1,250.50`.
- USD in Vietnamese: `1.250,50 USD`.
- Expense is red and negative.
- Income is green and positive.

### Scan/OCR

- Scan defaults to expense.
- OCR only suggests; user must confirm.
- If image/object is unknown, leave product/transaction name blank.
- Do not auto-fill names like "Unknown", "Other", "Item", "Không xác định", "Khác", "Hàng hóa".
- Extract possible product/service names from OCR notes, product lines, messages, services, and receipts.
- If amount cannot be detected, leave amount blank and show input hint.

### Home

- Show balance, income, expense, current date calendar strip, quick actions, recent transactions.
- Income: green.
- Expense: red.
- Date/time must be based on device current date.

### Transaction history

- Rename old "Sổ" to "Lịch sử giao dịch".
- Four filters: All, Expense, Income, Pending sync.

### Reports

- Dashboard with time filter.
- Bar chart with income and expense by time.
- Cards: Income, Expense, Remaining = Income - Expense.

### Tools

- Scan / AI ghi chi.
- Accounts.
- Budgets.
- Expense categories.
- Income categories.
- Recurring transactions.
- Split bill.
- Do not expose manual sync as a user task.

### Sync/backend

- Backend is PC-hosted; assume PC is always on but still code defensively.
- User does not need to know backend URL.
- User does not manually sync.
- Local-first is mandatory: save to Room first, then background sync.
- If backend is unavailable: keep local data, mark pending/failed, retry automatically.
- Never block transaction creation on backend availability.

## Workflow rules for Claude

- Start with exploration: read README, docs, Gradle files, Android entry points, backend package scripts.
- Before editing, provide a plan and list exact files to modify.
- Prefer small, verifiable changes.
- After each change, run the narrowest relevant command first.
- Provide evidence: exact command, exit status, and relevant output.
- If a command fails, do not guess. Inspect logs and fix root cause.
- Never commit secrets, credentials, keystore files, or private tunnel URLs.

