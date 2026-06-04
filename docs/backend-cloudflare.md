# Backend And Cloudflare Tunnel

## Local Backend

The backend runs on this PC at `http://localhost:8080`.

```powershell
cd backend
npm start
```

Data files:

- SQLite database: `backend/data/tui-than-tai.sqlite`
- Receipt uploads: `backend/uploads/`

## Public Tunnel

Use Cloudflare Tunnel to publish the local backend through a stable HTTPS hostname, for example:

```text
https://api.your-domain.com -> http://localhost:8080
```

Production requirements:

- The PC must stay powered on.
- Cloudflare Tunnel service must restart automatically after reboot.
- The database and uploads folder need scheduled backups.
- The domain must remain stable; do not publish an app build with a temporary tunnel URL.
- The backend secret must be set through environment variable `JWT_SECRET`.

## Required Environment

```powershell
$env:PORT = "8080"
$env:JWT_SECRET = "replace-with-long-random-secret"
$env:PUBLIC_API_BASE_URL = "https://api.your-domain.com"
npm start
```

## Health Check

```powershell
curl http://localhost:8080/health
```
