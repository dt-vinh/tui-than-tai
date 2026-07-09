# CLAUDE.local.md template - do not commit

Use this as a local-only file if needed. Rename to `CLAUDE.local.md` and add to `.gitignore`.

## Local machine notes

- Windows user path:
- Android SDK path:
- Java 17 path:
- Node.js version:
- OPPO A31 connected by USB:
- Cloudflare Tunnel domain:

## Private values

Do not paste secrets into committed files. Use environment variables.

PowerShell example:

```powershell
$env:JWT_SECRET = "replace-with-real-secret"
$env:PORT = "8080"
$env:PUBLIC_API_BASE_URL = "https://your-cloudflare-domain.example"
```
