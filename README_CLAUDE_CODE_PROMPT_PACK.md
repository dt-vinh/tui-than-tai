# Claude Code Prompt Pack - Tui Than Tai / Lucky Wallet

Mục tiêu: dùng Claude Code để clone/inspect repo, build APK debug, cài và debug app trên OPPO A31, sau đó triển khai PRD chuẩn hóa.

Repo: https://github.com/dt-vinh/tui-than-tai
Thiết bị test: OPPO A31 Android, ưu tiên test máy thật qua USB debugging.
Backend: Node.js API chạy trên PC hiện tại, SQLite + local uploads, public qua Cloudflare Tunnel; app phải offline-first và tự sync.

## Cách dùng nhanh

1. Clone repo:

```powershell
git clone https://github.com/dt-vinh/tui-than-tai.git
cd tui-than-tai
```

2. Copy toàn bộ nội dung folder này vào root repo. Kết quả nên có:

```text
CLAUDE.md
.claude/commands/*.md
.claude/skills/*/SKILL.md
.claude/rules/*.md
docs/PRD_FOR_CLAUDE_CODE.md
prompts/*.md
```

3. Mở Claude Code trong root repo:

```powershell
claude
```

4. Chạy lần lượt trong Claude Code:

```text
/init
```

Sau đó paste prompt:

```text
@prompts/00_START_HERE_CLAUDE_CODE.md
```

Hoặc dùng custom commands nếu Claude Code nhận diện:

```text
/tui-android-build-debug
/tui-oppo-install-logcat
/tui-backend-pc
/tui-prd-implement
```

## Nguyên tắc làm việc với Claude Code

- Bắt đầu bằng inspect repo, không sửa code ngay.
- Yêu cầu Claude lập plan, liệt kê file cần sửa, rồi mới implement.
- Mỗi lần sửa xong phải chạy lệnh kiểm chứng và đưa log làm bằng chứng.
- Không hardcode secret, token, domain riêng tư.
- Không xóa dữ liệu local nếu sync/backend lỗi.
- Không báo "đã xong" nếu chưa có build/test/log xác minh.

