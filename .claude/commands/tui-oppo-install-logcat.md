Prepare OPPO A31 install and logcat debugging.

1. Confirm debug APK path.
2. Ask user to connect OPPO A31 with USB debugging enabled.
3. Run or instruct user to run:

```powershell
adb devices
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb shell monkey -p <applicationId> 1
adb logcat
```

When user pastes logcat, identify the crash/error, patch code, rebuild, and repeat until app opens.
