Inspect and validate PC-hosted backend and automatic sync.

Use `docs/PRD_FOR_CLAUDE_CODE.md` sections Backend and Sync. Inspect `backend/`, Android networking, Room/sync workers, and config.

Validate:
- Backend starts with `npm start`.
- `/health` works.
- SQLite and uploads paths are correct.
- Android saves local first.
- Sync is automatic, no manual user sync required.
- Backend unavailable does not block local transaction creation.
