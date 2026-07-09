Build and debug the Android app for OPPO A31.

Read `docs/PRD_FOR_CLAUDE_CODE.md` and `CLAUDE.md`, inspect the Android project, then run:

```powershell
cd android
.\gradlew.bat :app:assembleDebug --stacktrace
```

If it fails, fix the root cause and rebuild. Show exact commands and outputs. Do not remove product functionality just to pass build.
