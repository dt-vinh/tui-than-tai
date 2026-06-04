# PRD: Tui Than Tai / Lucky Wallet

## Summary

Tui Than Tai is a simple Android app for recording expenses from photos. Users can take a picture of a receipt, invoice, or purchased object; the app extracts useful information with on-device ML Kit, suggests a category and amount, then lets the user confirm before saving. The app works offline for capture and manual entry, then syncs later to a self-hosted backend running on this PC through Cloudflare Tunnel.

The app ships in Vietnamese and English. Vietnamese is the primary market language; English is included for bilingual users and Store readiness.

## Goals

- Make expense entry fast enough to use immediately after buying something.
- Let users save expenses without network access.
- Use photo/OCR/category suggestions as assistive features, never as silent automation.
- Keep the first public version focused: capture, confirm, list, reports, settings, login/sync.
- Support public users through a stable HTTPS backend exposed by Cloudflare Tunnel.

## Video Research Findings

Source videos were downloaded from the provided Google Photos albums and extracted into `research/videos/unzipped/`. Contact sheets are available in `research/frames/`.

Cap money patterns observed:

- First setup includes name and currency selection.
- Home is calendar-first with wallet/account chips and a floating add action.
- Capture flow uses camera with an amount overlay, category/account/date selectors, and a confirm button.
- Review screens summarize weekly/monthly spending.
- Bottom navigation includes home, statistics, transactions/tools, accounts/budget, and settings.
- Advanced areas include subscription prompts, budgets, split bills, recurring transactions, sharing, language/currency, and account management.

MISA patterns observed:

- Onboarding promotes scan receipt, voice AI, and quick reports.
- Authentication includes email/password and Google login.
- Expense entry uses a clear amount field, category grid, wallet/account selector, date, and note.
- Categories visible include food/drink, home, fixed bill, coffee, travel, parents/family, gifts, repair, and more.
- Reports include charts and filters by time.
- Advanced areas include dynamic category editing, wallets, debt/loans, reminders, bank connection, AI assistant, premium upsell, and settings/language.

Product decision:

- Keep v1 simpler than both reference apps.
- Include scan/photo entry, manual entry, category selection, wallet/account, date, note, list, reports, settings, language, and sync.
- Exclude v1 voice AI, bank connection, loans/debt, split bills, recurring transactions, premium upsell, and chatbot.

## Users

- Primary: Vietnamese personal finance users who want quick expense capture without learning accounting terms.
- Secondary: bilingual users who switch between Vietnamese and English.
- Early operators: the owner running the backend from this PC.

## Functional Requirements

- Camera capture:
  - User can open camera from home.
  - User can capture an image, retake, or continue to review.
  - Captured image is stored locally and attached to the expense.
- OCR and category suggestion:
  - ML Kit Text Recognition extracts receipt text.
  - ML Kit Image Labeling suggests category from objects.
  - Amount extraction looks for VND-style prices and total/payment keywords.
  - User must confirm or edit amount, category, title, wallet, date, and note before save.
- Offline:
  - Manual entry works without network.
  - Captured expense is saved to Room first.
  - Unsynced items show pending status.
  - WorkManager retries sync when network is available.
- Sync:
  - User can register/login.
  - Expenses and receipt images sync to backend.
  - Last-write-wins is acceptable for v1 conflicts.
  - Deleted records are tombstoned with `deletedAt`.
- Reports:
  - Home shows current month total and recent expenses.
  - Reports screen shows monthly total by category.
- Settings:
  - Switch language between Vietnamese, English, and system default.
  - Configure backend URL before production release.
  - Show sync status.

## Category Taxonomy

Initial categories are derived from the videos and normalized for a simpler app:

| ID | Vietnamese | English | Example keywords |
| --- | --- | --- | --- |
| food | An uong | Food & drink | banh mi, cafe, coffee, food, restaurant |
| coffee | Cafe | Coffee | cafe, coffee, tra sua |
| transport | Di lai | Transport | taxi, grab, bus, fuel |
| shopping | Mua sam | Shopping | shop, clothes, market |
| bills | Hoa don co dinh | Bills | electricity, water, internet, phone |
| home | Nha cua | Home | household, gia dung, furniture |
| health | Suc khoe | Health | medicine, pharmacy, clinic |
| entertainment | Giai tri | Entertainment | movie, game, karaoke |
| travel | Du lich | Travel | hotel, flight, trip |
| family | Gia dinh | Family | parents, bo me, family |
| gifts | Qua tang | Gifts | gift, donate |
| repair | Sua chua | Repair | repair, service |
| other | Khac | Other | fallback |

The Android app stores Vietnamese and English names so the same category records render in both languages.

## Non-Functional Requirements

- Android target SDK: 35+ for Google Play submission.
- Minimum SDK: 26.
- Local database: Room.
- Offline scheduling: WorkManager.
- Settings: DataStore.
- Camera: CameraX.
- ML: Google ML Kit Text Recognition and Image Labeling.
- Backend: Node.js 24+, built-in SQLite, local file storage.
- Public backend access: Cloudflare Tunnel with stable HTTPS hostname.

## Privacy and Security

- Receipt images and expense data are sensitive personal data.
- Store receipt images locally and upload only after user account sync is configured.
- Backend passwords must use salted PBKDF2 hashes.
- API auth uses signed JWT-like tokens with short access token lifetime and longer refresh tokens.
- The PC backend must have backup, OS updates, HTTPS tunnel, and restricted filesystem access before Store launch.

## Release Criteria

- Debug and release Android builds complete successfully.
- Core flow works offline: capture/manual save, app restart, pending sync.
- Backend health, auth, expense CRUD, receipt upload, and sync endpoints pass tests.
- UI passes Vietnamese and English smoke tests.
- Store checklist includes camera, network, data safety, privacy policy, and target API compliance.
