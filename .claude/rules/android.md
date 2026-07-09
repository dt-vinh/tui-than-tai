---
paths:
  - "android/**/*"
---

# Android rules

- Use Kotlin and Jetpack Compose patterns already present in the repo.
- Keep OPPO A31 in mind: avoid heavy UI, test on 720px-class width.
- Handle camera permission gracefully.
- Do not block local saves on network/backend.
- Keep user-facing errors friendly and localized.
- Build with `cd android && .\gradlew.bat :app:assembleDebug` after significant changes.
