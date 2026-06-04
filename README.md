# Tui Than Tai / Lucky Wallet

Production-oriented Android expense capture app for Vietnamese and English users.

## What is included

- `docs/PRD.md` - Product requirements, scope, success criteria, privacy, rollout.
- `docs/workflow.md` - Video-derived workflow notes and proposed app flow.
- `docs/test-cases.md` - Functional, sync, OCR, offline, language, and release test cases.
- `design/mockup.html` - Figma-ready local mockup pack in HTML/CSS.
- `backend/` - Self-hosted Node.js API backed by SQLite and local receipt storage.
- `android/` - Kotlin Jetpack Compose Android app scaffold.
- `research/` - Downloaded Google Photos videos and extracted workflow frames.
- `scripts/run-full-tests.ps1` - Runs backend tests, Android JVM tests, debug/release builds, and emulator smoke.
- `scripts/push-to-github.ps1` - Initializes/pushes the source repo once Git and GitHub credentials are available.

## Local prerequisites

- Java 17
- Android SDK with API 35
- Node.js 24+
- Cloudflare Tunnel for public backend access

This workspace currently has Java 17, Android SDK API 35, Node.js 24, and a temporary Gradle 8.7 distribution under `.tools/`.

## Quick start

Backend:

```powershell
cd backend
npm start
```

Android:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

The Android app defaults to a placeholder API host. Configure the real Cloudflare Tunnel domain before Store release.

Full local verification:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-full-tests.ps1
```

GitHub push after installing Git and signing in/configuring credentials:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\push-to-github.ps1 -RepoUrl https://github.com/<owner>/<repo>.git
```
