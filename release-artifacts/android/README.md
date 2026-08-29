# Android Release Artifacts

Each Play release is stored locally in a versioned directory:

`v<versionName>-code<versionCode>/`

The binary AAB, its SHA-256 file, and a tracked `RELEASE.md` manifest share that directory. AAB/APK files are intentionally ignored by Git; publish the verified AAB as a GitHub Release asset or upload it to Google Play Console.

Current release: `v0.19.0-code19`
