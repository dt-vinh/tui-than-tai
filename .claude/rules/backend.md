---
paths:
  - "backend/**/*"
---

# Backend rules

- Backend is PC-hosted Node.js + SQLite + uploads.
- Use environment variables for secrets such as `JWT_SECRET`.
- Do not hardcode private Cloudflare Tunnel URL or secrets.
- Maintain `/health` endpoint.
- Ensure APIs support idempotent sync/upsert by client UUID.
- Keep local data safe if backend is unavailable.
