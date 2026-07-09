# Prompt 04 - Test and release checklist for OPPO A31 internal build

Run an internal QA pass for the Android debug build on OPPO A31.

Checklist:

## Build
- `:app:assembleDebug` passes.
- APK exists at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Install
- `adb devices` sees OPPO A31.
- `adb install -r` succeeds.
- App opens without crash.

## Smoke test
- Splash/onboarding works.
- Register name + email works or is safely scaffolded.
- Google sign-in button exists or is clearly disabled/scaffolded.
- After login, Home no longer shows login CTA.
- Logout only in Settings.
- Language change Vietnamese/English shows confirm popup.
- Home shows balance, income, expense, date strip.
- Add expense reduces account balance and shows red negative amount.
- Add income increases account balance and shows green positive amount.
- Transaction History filters: All, Expense, Income, Pending sync.
- Reports Dashboard shows income/expense bar chart and filter.
- Add account with name + current amount.
- Budget add/edit/delete.
- Expense category add/edit/delete.
- Income category add/edit/delete.
- Recurring transaction creates scheduled transaction or has a safe implemented scaffold.
- Split bill calculates positive/negative balances.
- Scan unknown image leaves name blank.
- Backend off: app still saves locally.
- Backend on: pending data syncs automatically.

## Evidence required

For each failure:
- Screen/flow.
- Expected result.
- Actual result.
- Logcat excerpt.
- Suspected file/module.
- Fix applied.
- Verification command/result.
