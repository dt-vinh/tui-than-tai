---
name: tui-than-tai-build-debug
description: Build and debug the Tui Than Tai Android app, especially for OPPO A31 physical device testing.
---

# Skill: Build and debug Tui Than Tai Android app

Use this skill when the user asks to build, install, debug, or fix runtime/build errors for the Android app.

## Inputs to inspect

- `README.md`
- `docs/PRD_FOR_CLAUDE_CODE.md`
- `android/settings.gradle*`
- `android/build.gradle*`
- `android/app/build.gradle*`
- Android source entry points
- `scripts/run-full-tests.ps1`

## Build command

```powershell
cd android
.\gradlew.bat :app:assembleDebug --stacktrace
```

## Full verification command

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-full-tests.ps1
```

## OPPO A31 install/debug commands

```powershell
adb devices
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb logcat
```

## Required behavior

- Fix compile errors by root cause.
- Do not delete core app features to make build pass.
- Preserve offline-first behavior.
- Verify with build output.
- Ask user for logcat if crash occurs on device.

## Output format

Always return:

1. What was inspected.
2. What failed or passed.
3. Files changed.
4. Exact command output or relevant excerpt.
5. Next step.
