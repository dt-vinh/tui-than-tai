# PRD_FOR_CLAUDE_CODE - Tui Than Tai / Lucky Wallet

## 1. Product summary

Tui Than Tai / Lucky Wallet is an Android-first Vietnamese/English personal finance app. It supports expense and income tracking, OCR/photo scan, accounts, budgets, categories, recurring transactions, split bills, dashboard reports, and automatic PC-backend sync.

Core principle: offline-first. The app must save locally first, then sync in the background.

## 2. Backend model

- Backend runs on the current PC, not on a managed cloud server.
- Backend is Node.js API with SQLite and local receipt upload folder.
- Local backend URL: `http://localhost:8080` on the PC.
- Android device must call a configured public HTTPS Cloudflare Tunnel domain, not phone `localhost`.
- User must not manually configure backend or press sync.
- App should auto-sync whenever logged in, online, and backend is reachable.
- If backend is down, local app must continue working.

## 3. Main app navigation

Bottom navigation has 5 tabs:

1. Home / Trang chủ - icon `home`.
2. Transaction History / Lịch sử giao dịch - icon `receipt_long`.
3. Reports / Báo cáo - icon `bar_chart`.
4. Tools / Công cụ - icon `grid_view`.
5. Settings / Cài đặt - icon `settings`.

Scan is not its own bottom tab. It is a FAB/quick action on Home and an entry in Tools.

## 4. Visual design

Colors:

- Primary: `#F59E0B` yellow/gold.
- Income: `#16A34A` green.
- Expense: `#EF4444` red.
- Background: `#FFF7ED` or `#FAFAF9`.
- Card: `#FFFFFF`.
- Text main: `#0F172A`.
- Text secondary: `#64748B`.
- Border: `#E2E8F0`.
- Warning/pending sync: `#F97316`.

Use rounded Material-style icons.

## 5. Auth requirements

### Register

- Required fields: Name and Email.
- Google account linking/sign-in is supported.
- After successful registration, user is automatically logged in and navigated to Home.
- After login, no login button appears on Home, Reports, or Tools.

### Login

- Login by email or Google.
- After login, app stores session/token and auto-syncs pending data.
- Logout is only available in Settings.

### Settings account card

If logged in:
- Show avatar initial.
- Show name.
- Show email.
- Show status "Đã đăng nhập".
- Show Logout button.

If logged out:
- Show "Bạn chưa đăng nhập".
- Show Login/Register CTA.

## 6. Language requirements

Supported languages:

- Tiếng Việt.
- English.

Language is changed from Settings. When user selects a different language, show a confirmation popup before applying.

Vietnamese popup:
- Title: `Đổi ngôn ngữ?`
- Content: `Ứng dụng sẽ chuyển sang Tiếng Việt. Một số màn hình có thể được tải lại.`
- Buttons: `Hủy`, `Đồng ý`.

English popup:
- Title: `Change language?`
- Content: `The app will switch to English. Some screens may reload.`
- Buttons: `Cancel`, `Confirm`.

## 7. Money formatting

- VND + Vietnamese: `1.250.000 ₫`, no decimals.
- USD + English: `$1,250.50`.
- USD + Vietnamese: `1.250,50 USD`.
- Expenses are red and display negative sign.
- Income is green and display positive sign.

OCR rules:
- If OCR sees `đ`, `₫`, `VND`, use VND.
- If OCR sees `$`, use USD.
- If no symbol is detected, use app/account default currency.

## 8. Home screen

Home shows:

1. Header: greeting and today's date from device calendar.
2. Balance card: current total balance across all accounts.
3. Income card: selected-period income, green.
4. Expense card: selected-period expense, red.
5. Calendar strip based on current date.
6. Quick actions:
   - Scan ảnh.
   - Thêm chi tiêu.
   - Thêm thu nhập.
7. Recent transactions.
8. Floating add/scan action.

Rules:
- Income increases account balance.
- Expense decreases account balance.
- Pending sync transactions still count locally.
- Deleted transactions do not count.

## 9. Add transaction

Entry points:

- Home quick action Scan.
- Home quick action Add Expense.
- Home quick action Add Income.
- Tools Scan.
- Transaction History add button.

If user presses general `+`, show bottom sheet:

1. Scan receipt/photo - default type expense.
2. Add expense - red.
3. Add income - green.

Expense form fields:
- Amount.
- Expense name.
- Expense category.
- Account to deduct from.
- Transaction date.
- Note.

Income form fields:
- Amount.
- Income name.
- Income category.
- Receiving account.
- Transaction date.
- Note.

Save rules:
- Expense deducts account balance.
- Income increases account balance.
- Save local first.
- Enqueue auto-sync.

## 10. Scan / OCR flow

Camera screen:
- Fullscreen black background.
- Close top-left.
- Title "Scan ảnh".
- Camera preview center.
- Round capture button bottom center, yellow center.
- Optional gallery and flash icons.

After capture:
- Navigate to Review screen.
- Show image preview.
- Show analyzing/loading state.
- Run OCR/image labeling.
- Fill fields only when confident.

Unknown image rule:
- If app cannot identify the product/service/object, leave transaction/product name blank.
- Never fill: `Unknown`, `Other`, `Item`, `Không xác định`, `Khác`, `Hàng hóa`, `Vật phẩm`.
- Category may default to Other, but name stays blank.
- If user saves with blank name, show inline error: `Vui lòng nhập tên khoản chi.`

Product/service extraction rule:
- When scanning, look for OCR text around notes, product lines, service names, message/note fields, receipt descriptions.
- Extract the likely product/service paid for or received.
- If multiple products are present, prefer merchant/service summary or first meaningful line item. If uncertain, leave blank.

Amount extraction rule:
- Prefer OCR lines near: `Tổng`, `Tổng cộng`, `Thành tiền`, `Thanh toán`, `Total`, `Amount`, `Paid`.
- If no amount is detected, leave amount blank with hint.

## 11. Transaction History

Rename old `Sổ` to `Lịch sử giao dịch`.

Top filters:

1. Tất cả - active yellow.
2. Chi tiêu - active red.
3. Thu nhập - active green.
4. Chờ đồng bộ - active orange.

Rows show:
- Category icon.
- Transaction name.
- Category.
- Account.
- Date/time.
- Amount.
- Sync status.

Filter rules:
- All: all non-deleted transactions.
- Expense: type = expense.
- Income: type = income.
- Pending sync: syncStatus in `pending`, `failed`, `deleted_pending`.

On row tap: open transaction detail with edit and delete.

## 12. Reports / Dashboard

Reports is a dashboard, not just category list.

Show:

1. Time filter chips: Today, This week, This month, This year, Custom.
2. Cards:
   - Income, green.
   - Expense, red.
   - Remaining = Income - Expense; green if >= 0, red if < 0.
3. Bar chart with income and expense by time.
4. Category statistics.

Bar chart:
- X axis: time bucket.
- Y axis: amount.
- Two bars per bucket: income green and expense red.
- Empty state when no data.

Data source: local database. Pending sync data still counts.

## 13. Tools

Tools cards:

1. Scan / AI ghi chi - yellow - `Chụp ảnh hóa đơn hoặc đồ vật để tạo giao dịch`.
2. Tài khoản - yellow - `Quản lý tài khoản và số tiền hiện có`.
3. Ngân sách - green - `Thêm, sửa, xóa ngân sách chi tiêu`.
4. Danh mục chi - red - `Danh sách các khoản chi, có thể thêm/sửa/xóa`.
5. Danh mục thu - green - `Danh sách các khoản thu, có thể thêm/sửa/xóa`.
6. Giao dịch định kỳ - purple - `Tự động tạo giao dịch theo lịch cài đặt`.
7. Chia tiền - blue - `Tạo khoản chi với bạn bè và tính ai đang âm/dương`.

No manual Sync card for users. Sync is automatic.

## 14. Accounts

Accounts screen:
- List account cards.
- Add button.
- Each card: account name, current amount, currency, wallet/bank icon, edit/delete menu.

Add account form required fields:
- Account name.
- Current amount.
- Currency default VND.

Saving account updates Home balance.

Edit account:
- Edit name and current amount.

Delete account:
- Confirm popup.
- Do not delete old transactions.
- Do not allow deleting default account without replacement.

## 15. Budgets

Budgets support add/edit/delete.

Budget card shows:
- Name.
- Expense category.
- Limit.
- Spent.
- Remaining.
- Progress.
- Edit/delete.

Progress color:
- 0-70% green.
- 71-99% orange.
- >=100% red.

Budgets apply to expenses only.

## 16. Expense categories

Expense categories support add/edit/delete.

Default examples:
- Ăn uống.
- Cafe.
- Đi lại.
- Mua sắm.
- Hóa đơn cố định.
- Nhà cửa.
- Sức khỏe.
- Giải trí.
- Du lịch.
- Gia đình.
- Quà tặng.
- Sửa chữa.
- Khác.

Add fields:
- Category name.
- Icon.
- Color.
- OCR keywords, optional.

If deleting a category with transactions, ask to move old transactions to Other.

## 17. Income categories

Income categories support add/edit/delete.

Default examples:
- Lương.
- Thưởng.
- Kinh doanh.
- Đầu tư.
- Quà tặng.
- Hoàn tiền.
- Lãi tiết kiệm.
- Khác.

Income categories only appear in income forms.
Expense categories only appear in expense forms.

## 18. Recurring transactions

Recurring transactions support add/edit/delete.

Fields:
- Name.
- Type: income or expense.
- Amount.
- Matching category.
- Account.
- Cycle: daily, weekly, monthly, yearly.
- Start date.
- Execution time.
- Enable/disable auto-create.

At scheduled time:
- Automatically create transaction.
- Income adds money.
- Expense deducts money.
- Add to Transaction History.
- Enqueue auto-sync.

Editing recurring rule only affects future generated transactions.
Deleting recurring rule does not delete already generated transactions.

## 19. Split bill

Split bill supports creating shared expenses with friends.

Create form:
- Split name.
- Participants.
- Payers: person + amount paid.
- Split method: equal by default.

Formula:
- Total = sum paid amounts.
- Share per person = Total / participants.
- Balance = paid by person - share per person.

Display:
- Positive balance green.
- Negative balance red.
- Zero gray.

Show settlement suggestion: who pays whom and how much.

Save split bill:
- Create split bill record.
- Optionally create a personal expense for amount paid by current user.
- Auto-sync with backend PC.

## 20. Settings

Cards:

1. Account.
2. Language.
3. Automatic sync status.
4. App information.
5. Logout.

After login:
- Show name and email.
- Show status logged in.
- No login button.

Sync card:
- `Đồng bộ tự động`.
- Show synced/pending/error state.
- No manual sync button.

Logout:
- Red logout button.
- Confirmation popup.
- Does not delete local transaction history.

## 21. Release criteria

- Debug APK builds.
- App installs on OPPO A31.
- App starts without crashing.
- Camera permission flow works.
- Manual expense/income works.
- Scan unknown image leaves name blank.
- Home balance/income/expense update correctly.
- Transaction History filters work.
- Reports dashboard renders bar chart.
- Add account works.
- Budgets/categories/recurring/split bill screens are functional or clearly marked if deferred.
- Backend PC sync does not block local usage.
- No hardcoded secrets.

