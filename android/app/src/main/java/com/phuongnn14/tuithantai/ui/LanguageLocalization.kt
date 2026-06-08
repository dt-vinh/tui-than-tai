package com.phuongnn14.tuithantai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage {
    VIETNAMESE,
    ENGLISH
}

object Localization {
    private val vi = mapOf(
        "app_name" to "Túi Thần Tài",
        "home" to "Trang chủ",
        "history" to "Lịch sử",
        "reports" to "Báo cáo",
        "tools" to "Công cụ",
        "settings" to "Cài đặt",

        "current_balance" to "Tổng số dư khả dụng",
        "income" to "Thu nhập",
        "expense" to "Chi tiêu",
        "recent_transactions" to "Giao dịch gần đây",
        "no_transactions" to "Chưa có giao dịch nào được ghi chép.",
        "quick_actions" to "Phím tắt nhanh",
        "scan_receipt" to "Quét AI",
        "add_expense" to "Thêm chi",
        "add_income" to "Thêm thu",

        "all" to "Tất cả",
        "waiting_sync" to "Chờ đồng bộ",
        "sync_now" to "Đồng bộ ngay",
        "sync_status" to "Đồng bộ",
        "synced" to "Đã đồng bộ",
        "pending" to "Chưa đồng bộ",
        "transaction_detail" to "Chi tiết giao dịch",
        "delete_confirm" to "Bạn có chắc chắn muốn xóa giao dịch này?",
        "delete" to "Xóa",
        "edit" to "Sửa",

        "amount" to "Số tiền",
        "title" to "Tên danh mục/Sản phẩm",
        "category" to "Danh mục",
        "account" to "Tài khoản",
        "date" to "Ngày giao dịch",
        "note" to "Ghi chú (Tùy chọn)",
        "save" to "Lưu",
        "fill_all_fields" to "Vui lòng nhập đầy đủ các trường thông tin.",
        "input_title_error" to "Vui lòng nhập tên khoản chi.",
        "input_title_income_error" to "Vui lòng nhập tên khoản thu.",

        "income_expense_chart" to "Biểu đồ Thu/Chi",
        "advanced_sync_options" to "[+] Tùy chọn nâng cao (Đồng bộ PC)",
        "hide_advanced_sync" to "[-] Ẩn tùy chọn nâng cao (Đồng bộ PC)",

        "Ăn uống" to "Ăn uống",
        "Di chuyển" to "Di chuyển",
        "Mua sắm" to "Mua sắm",
        "Giải trí" to "Giải trí",
        "Nhà ở" to "Nhà ở",
        "Y tế" to "Y tế",
        "Khác" to "Khác",
        "Lương" to "Lương",
        "Thưởng" to "Thưởng",
        "Đầu tư" to "Đầu tư",
        "Quà tặng" to "Quà tặng",

        "Tiền mặt" to "Tiền mặt",
        "Tài khoản ngân hàng" to "Tài khoản ngân hàng",
        "Ví MoMo" to "Ví MoMo",

        "report_dashboard" to "Bảng phân tích tài chính",
        "time_filter" to "Chọn thời gian",
        "today" to "Hôm nay",
        "this_week" to "Tuần này",
        "this_month" to "Tháng này",
        "this_year" to "Năm nay",
        "custom" to "Tùy chọn",
        "remaining" to "Còn lại",
        "category_stats" to "Phân tích theo hạng mục",

        "account_title" to "Quản lý Tài khoản",
        "add_account" to "Thêm tài khoản mới",
        "edit_account" to "Sửa tài khoản",
        "account_name" to "Tên tài khoản (Ví dụ: Tiền mặt)",
        "initial_balance" to "Số dư ban đầu",
        "default_currency" to "Đồng tiền mặc định",
        "edit_category" to "Sửa danh mục",
        "save_changes" to "Lưu thay đổi",
        "delete_account_confirm" to "Bạn có chắc chắn muốn xóa tài khoản này? Tất cả các khoản chi tiêu dưới tài khoản này vẫn sẽ giữ nguyên nhưng tên liên kết có thể mất.",
        "delete_category_confirm" to "Bạn có chắc chắn muốn xóa danh mục này?",

        "budget_title" to "Quản lý Ngân sách và Hạn mức",
        "add_budget" to "Thêm hạn mức chi",
        "budget_name" to "Tên ngân sách",
        "budget_amount" to "Hạn mức tối đa",
        "budget_period" to "Chu kỳ ngân sách",
        "weekly" to "Hàng tuần",
        "monthly" to "Hàng tháng",
        "start_date" to "Ngày bắt đầu",
        "end_date" to "Ngày kết thúc",

        "category_title" to "Quản lý Danh mục",
        "add_category" to "Thêm danh mục mới",
        "expense_categories" to "Danh mục Chi",
        "income_categories" to "Danh mục Thu",

        "recurring_title" to "Giao dịch Định kỳ tự động",
        "add_recurring" to "Thêm chu kỳ giao dịch",
        "recurring_name" to "Tên giao dịch định kỳ",
        "cycle_type" to "Tần suất lặp",
        "daily" to "Hàng ngày",
        "monthly_cycle" to "Hàng tháng",
        "yearly" to "Hàng năm",

        "split_bill_title" to "Chia tiền nhóm & Bạn bè",
        "add_split" to "Tạo bill chia sẻ",
        "participants" to "Danh sách người tham gia (cách nhau bằng dấu phẩy)",
        "payer" to "Người chi trả thực tế",
        "total_amount" to "Tổng tiền hóa đơn",
        "share_per_person" to "Phần chia mỗi người",
        "settlements" to "Gợi ý phương án thanh toán",
        "owes" to "nợ",
        "to" to "cho",

        "auth_title" to "Tài khoản Thần Tài",
        "user_profile" to "Hồ sơ người dùng",
        "name" to "Họ tên người dùng",
        "email" to "Địa chỉ Email",
        "google_login" to "Liên kết Google Account",
        "login" to "Đăng nhập",
        "register" to "Đăng ký thành viên",
        "logout" to "Đăng xuất",
        "server_url" to "Địa chỉ máy chủ Backend PC",
        "not_logged_in" to "Chưa đăng nhập",
        "logged_in_as" to "Tài khoản:",

        "language" to "Ngôn ngữ / Language",
        "change_lang_confirm" to "Xác nhận chuyển đổi ngôn ngữ",
        "change_lang_body" to "Bạn có chắc chắn muốn chuyển ngôn ngữ sang Tiếng Việt?",
        "confirm" to "Xác nhận",
        "cancel" to "Hủy bỏ",

        "ocr_running" to "AI đang đọc hóa đơn...",
        "ocr_desc" to "Trình xử lý AI thông minh sẽ trích xuất tên sản phẩm và số tiền chi tiêu từ ảnh hóa đơn.",
        "ocr_draft_label" to "Duyệt thông tin quét hóa đơn (Bản nháp)",
        "take_photo" to "Chụp ảnh hóa đơn",
        "mock_select_receipt_label" to "Chọn hóa đơn mẫu để thử quét AI:",
        "pick_from_gallery" to "Chọn từ thư viện"
    )

    private val en = mapOf(
        "app_name" to "Lucky Wallet",
        "home" to "Home",
        "history" to "History",
        "reports" to "Reports",
        "tools" to "Tools",
        "settings" to "Settings",

        "current_balance" to "Total Available Balance",
        "income" to "Income",
        "expense" to "Expense",
        "recent_transactions" to "Recent Transactions",
        "no_transactions" to "No transactions recorded yet.",
        "quick_actions" to "Quick Actions",
        "scan_receipt" to "Scan AI",
        "add_expense" to "Add Expense",
        "add_income" to "Add Income",

        "all" to "All",
        "waiting_sync" to "Pending Sync",
        "sync_now" to "Sync Now",
        "sync_status" to "Sync Status",
        "synced" to "Synced",
        "pending" to "Pending",
        "transaction_detail" to "Transaction Detail",
        "delete_confirm" to "Are you sure you want to delete this transaction?",
        "delete" to "Delete",
        "edit" to "Edit",

        "amount" to "Amount",
        "title" to "Title/Item details",
        "category" to "Category",
        "account" to "Account",
        "date" to "Date",
        "note" to "Note (Optional)",
        "save" to "Save",
        "fill_all_fields" to "Please fill in all details.",
        "input_title_error" to "Please enter transaction title.",
        "input_title_income_error" to "Please enter income title.",

        "income_expense_chart" to "Income/Expense Chart",
        "advanced_sync_options" to "[+] Advanced Options (PC Sync)",
        "hide_advanced_sync" to "[-] Hide advanced options",

        "Ăn uống" to "Food & Drink",
        "Di chuyển" to "Transport",
        "Mua sắm" to "Shopping",
        "Giải trí" to "Entertainment",
        "Nhà ở" to "Housing",
        "Y tế" to "Healthcare",
        "Khác" to "Others",
        "Lương" to "Salary",
        "Thưởng" to "Bonus",
        "Đầu tư" to "Investment",
        "Quà tặng" to "Gift",

        "Tiền mặt" to "Cash",
        "Tài khoản ngân hàng" to "Bank Account",
        "Ví MoMo" to "MoMo Wallet",

        "report_dashboard" to "Financial Analytics Dashboard",
        "time_filter" to "Select Period",
        "today" to "Today",
        "this_week" to "This Week",
        "this_month" to "This Month",
        "this_year" to "This Year",
        "custom" to "Custom",
        "remaining" to "Remaining",
        "category_stats" to "Breakdown by Category",

        "account_title" to "Manage Accounts",
        "add_account" to "Add New Account",
        "edit_account" to "Edit Account",
        "account_name" to "Account Name (e.g., Cash)",
        "initial_balance" to "Initial Balance",
        "default_currency" to "Default Currency",
        "edit_category" to "Edit Category",
        "save_changes" to "Save Changes",
        "delete_account_confirm" to "Are you sure you want to delete this account? Existing transactions will remain as history.",
        "delete_category_confirm" to "Are you sure you want to delete this category?",

        "budget_title" to "Budgets & Limits",
        "add_budget" to "Add Budget Limit",
        "budget_name" to "Budget Name",
        "budget_amount" to "Budget Threshold",
        "budget_period" to "Budget Cycle",
        "weekly" to "Weekly",
        "monthly" to "Monthly",
        "start_date" to "Start Date",
        "end_date" to "End Date",

        "category_title" to "Categories Management",
        "add_category" to "Add New Category",
        "expense_categories" to "Expense Categories",
        "income_categories" to "Income Categories",

        "recurring_title" to "Automated Recurring Transactions",
        "add_recurring" to "Add Recurring Rule",
        "recurring_name" to "Schedule Name",
        "cycle_type" to "Interval Frequency",
        "daily" to "Daily",
        "monthly_cycle" to "Monthly",
        "yearly" to "Yearly",

        "split_bill_title" to "Split Bills with Friends",
        "add_split" to "Create Split Bill",
        "participants" to "Participants list (separated by commas)",
        "payer" to "Payer",
        "total_amount" to "Total Amount",
        "share_per_person" to "Share Per Person",
        "settlements" to "Suggested Debt Settlements",
        "owes" to "owes",
        "to" to "to",

        "auth_title" to "Lucky Account",
        "user_profile" to "User Profile",
        "name" to "Full Name",
        "email" to "Email Address",
        "google_login" to "Link with Google Account",
        "login" to "Login",
        "register" to "Register Account",
        "logout" to "Logout",
        "server_url" to "Backend Host URL",
        "not_logged_in" to "Not Logged In",
        "logged_in_as" to "Logged in:",

        "language" to "Ngôn ngữ / Language",
        "change_lang_confirm" to "Change Language",
        "change_lang_body" to "Are you sure you want to change the language to English?",
        "confirm" to "Confirm",
        "cancel" to "Cancel",

        "ocr_running" to "AI is analyzing receipt...",
        "ocr_desc" to "Smart AI scanner will automatically extract amounts and item titles safely.",
        "ocr_draft_label" to "Review Scanned Draft",
        "take_photo" to "Take Photo",
        "mock_select_receipt_label" to "Select sample receipt to test AI parsing:",
        "pick_from_gallery" to "Pick from Gallery"
    )

    fun getString(key: String, lang: AppLanguage): String {
        return if (lang == AppLanguage.VIETNAMESE) {
            vi[key] ?: key
        } else {
            en[key] ?: vi[key] ?: key
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.VIETNAMESE }

@Composable
fun LocalizedText(key: String, language: AppLanguage) {
    androidx.compose.material3.Text(
        text = Localization.getString(key, language)
    )
}
