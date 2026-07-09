# START HERE - Claude Code task for Tui Than Tai

You are Claude Code working in the repository root of `dt-vinh/tui-than-tai`.

Your job is to build, debug, and improve the Android app so it can run on a physical OPPO A31, using the PRD in `docs/PRD_FOR_CLAUDE_CODE.md`.

## Hard requirements

1. First inspect the repo. Do not edit code until you have read:
   - `README.md`
   - `docs/PRD_FOR_CLAUDE_CODE.md`
   - `android/settings.gradle*`
   - `android/build.gradle*`
   - `android/app/build.gradle*`
   - main Android source entry points
   - backend package/scripts if relevant
2. Summarize current architecture.
3. Produce a step-by-step implementation/debug plan.
4. Then ask me to confirm before large edits.
5. Build debug APK with:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

6. If build fails, inspect exact errors, fix root cause, and rebuild.
7. After APK builds, give me commands to install and test on OPPO A31:

```powershell
adb devices
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb logcat
```

8. Use the logcat output I paste back to debug runtime crashes.

## Product requirements to enforce

- Register requires name + email; Google linking/sign-in supported or scaffolded safely if not implemented.
- Successful registration auto-logs in.
- After login, do not show login buttons on Home/Reports/Tools.
- Logout is in Settings only.
- Languages: Vietnamese and English only; language change requires confirmation popup.
- Home shows balance, income green, expense red, current device date calendar strip, quick actions, recent transactions.
- Rename "Sổ" to "Lịch sử giao dịch" with filters: All, Expense, Income, Pending sync.
- Reports is Dashboard with time filter and bar chart income/expense.
- Accounts: add/edit/delete with name + current amount.
- Budgets: add/edit/delete.
- Expense categories and income categories: add/edit/delete.
- Recurring transactions: add/edit/delete and auto-create transactions at configured time.
- Split bill: participants, payers, amount paid, calculate each person's positive/negative balance.
- Scan unknown image: leave name blank; never fill Unknown/Other/Khác/Hàng hóa.
- Sync is automatic with PC backend; user does not manually sync.
- Offline-first is mandatory.

## Verification

For every completed step, show:
- Files changed.
- Command run.
- Result/output.
- Remaining issues.

Do not claim success without build/test evidence.
