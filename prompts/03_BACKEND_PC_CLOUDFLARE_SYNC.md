# Prompt 03 - Backend PC and automatic sync

Set up and validate the backend sync model for Tui Than Tai.

Context:

- Backend runs on the user's current PC.
- Backend is Node.js + SQLite + local upload folder.
- Public access should use Cloudflare Tunnel domain.
- User must not manually sync in the app.
- App must save local first and sync in the background.

Tasks:

1. Inspect `backend/` package scripts, API endpoints, database paths, upload paths.
2. Inspect Android networking/sync code.
3. Confirm health endpoint and auth endpoints.
4. Ensure app does not require user to type backend URL in production UI.
5. Ensure sync states exist: pending, syncing, synced, failed, deleted_pending.
6. Ensure retry does not duplicate transactions; use UUID/upsert.
7. Ensure backend unavailable does not block creating income/expense.
8. Ensure errors are user-friendly, not raw `500`, `timeout`, or `ECONNREFUSED`.

Commands to run where applicable:

```powershell
cd backend
npm install
npm test
npm start
```

```powershell
curl http://localhost:8080/health
```

Android build:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

Do not hardcode secret values. Use environment variables for JWT_SECRET and configurable build config for public API base URL.
