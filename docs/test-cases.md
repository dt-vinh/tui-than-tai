# Test Cases

| ID | Area | Scenario | Expected Result |
| --- | --- | --- | --- |
| CAM-01 | Camera | Open camera with permission granted | Preview appears and capture button is enabled |
| CAM-02 | Camera | Deny camera permission | App shows permission error and manual entry remains available |
| CAM-03 | Camera | Capture a clear receipt | App moves to review and attaches local image |
| CAM-04 | Camera | Retake photo | Old pending preview is discarded and new image is used |
| OCR-01 | OCR | Vietnamese receipt with one total | Amount is suggested and user can edit before saving |
| OCR-02 | OCR | English receipt with total/paid keywords | Amount is suggested and title is populated from text |
| OCR-03 | OCR | Receipt with many prices | App chooses likely largest/total candidate and marks it as suggestion |
| OCR-04 | OCR | Blurry receipt | App does not block save; amount/category can be entered manually |
| OCR-05 | OCR | Object photo without text | Category is suggested from image labels; amount stays manual |
| CAT-01 | Category | Photo of food/coffee | Category defaults to Food & drink or Coffee when confidence is sufficient |
| CAT-02 | Category | Unknown object | Category defaults to Other |
| CAT-03 | Category | User changes suggested category | Saved expense uses the user-selected category |
| OFF-01 | Offline | Capture expense while offline | Expense saves locally with pending sync status |
| OFF-02 | Offline | Restart app while offline | Saved expense remains visible |
| OFF-03 | Offline | Reconnect network | WorkManager retries sync |
| SYNC-01 | Backend | Register new account | User receives access and refresh tokens |
| SYNC-02 | Backend | Login with wrong password | API returns 401 without leaking account details |
| SYNC-03 | Backend | Push new expense | Backend stores expense and returns server version |
| SYNC-04 | Backend | Pull after sync | App receives current server records |
| SYNC-05 | Backend | Delete expense locally | Backend receives tombstone with deletedAt |
| DATA-01 | Room | Save expense with image path | Record persists with receipt path |
| DATA-02 | Room | Empty database | Home shows empty state and total 0 |
| LANG-01 | Language | Device Vietnamese | App shows Vietnamese strings by default |
| LANG-02 | Language | Switch to English | App updates labels to English |
| LANG-03 | Language | Unsupported locale | App falls back safely |
| UI-01 | UI | Small screen | Buttons/text do not overlap |
| UI-02 | UI | Loading OCR | Review screen remains usable |
| REL-01 | Release | Build debug APK | Gradle build succeeds |
| REL-02 | Release | Build release AAB | Gradle bundle succeeds after signing config is supplied |
| REL-03 | Store | Check target SDK | App targets API 35+ |
| REL-04 | Store | Data safety | Camera, network, and user data disclosures are documented |
