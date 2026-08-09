package com.phuongnn14.tuithantai.backup

object DriveAuthSupport {
    const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"

    fun signInErrorMessage(statusCode: Int): String = when (statusCode) {
        10 -> "Google Sign-In chưa được cấu hình đúng cho chữ ký của bản cài đặt. Hãy thêm SHA-1 App Signing trên Firebase rồi thử lại."
        7 -> "Không thể kết nối Google. Hãy kiểm tra mạng rồi thử lại."
        12501 -> "Bạn đã hủy đăng nhập Google."
        4 -> "Phiên Google đã hết hạn. Vui lòng đăng nhập lại."
        else -> "Đăng nhập Google thất bại (mã $statusCode). Vui lòng thử lại."
    }
}
