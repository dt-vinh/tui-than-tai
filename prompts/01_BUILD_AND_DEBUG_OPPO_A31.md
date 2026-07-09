# Prompt 01 - Build and debug Android APK for OPPO A31

Inspect the Android project and build a debug APK for physical OPPO A31 testing.

Steps:

1. Inspect Android project structure, Gradle version, Android Gradle Plugin, Kotlin, Compose, minSdk, targetSdk, permissions, app ID.
2. Identify the main Activity and navigation entry.
3. Run:

```powershell
cd android
.\gradlew.bat :app:assembleDebug --stacktrace
```

4. If build fails:
   - Quote the first meaningful error.
   - Find the root cause.
   - Apply minimal fix.
   - Re-run build.

5. After successful build, prepare physical device commands:

```powershell
adb devices
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p <applicationId> 1
adb logcat -c
adb logcat
```

6. Ask me to paste logcat output if the app crashes on OPPO A31.
7. Debug runtime crashes until app opens successfully.

Constraints:

- Do not remove core features to make build pass.
- Do not hardcode private backend URL or secrets.
- Keep offline-first behavior.
- Keep OPPO A31 performance in mind.
