// =========================================================================
// BACKEND SNIPPET: CẤU HÌNH BỎ QUA XÁC THỰC JWT CHO WEBHOOK PAYOS
// File nguồn: src/main/java/com/mycompany/bachhoaxanhonline/middleware/JwtAuthFilter.java
// =========================================================================

/*
 * LƯU Ý BẢO MẬT QUAN TRỌNG:
 * Máy chủ PayOS gửi thông báo thanh toán (IPN / Webhook) tự động qua HTTP POST.
 * Do máy chủ PayOS không có tài khoản khách hàng hay Bearer Token JWT của bạn,
 * endpoint /payment/webhook BẮT BUỘC phải được đưa vào danh sách trắng (Public / Whitelist)
 * trong Filter kiểm tra JWT.
 * 
 * Tính bảo mật của Webhook được đảm bảo 100% bằng cách đối chiếu CHỮ KÝ SỐ (Checksum Key / HMAC-SHA256)
 * bên trong PaymentService.java, chứ KHÔNG dùng Bearer Token.
 */

public boolean isPublicEndpoint(String path, String method) {
    // ... các route công khai khác ...

    // [BẮT BUỘC] Cho phép PayOS Webhook đi qua mà không cần Header Authorization JWT
    if (path.equals("/payment/webhook") || path.startsWith("/payment/webhook/")) {
        return true;
    }

    return false;
}
