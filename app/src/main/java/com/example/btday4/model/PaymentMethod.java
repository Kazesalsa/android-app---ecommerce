package com.example.btday4.model;

/**
 * Supported checkout payment methods: Cash on Delivery (COD) and VietQR via PayOS.
 */
public enum PaymentMethod {
    COD(
            "COD",
            "Thanh toán khi giao",
            "Trả tiền mặt trực tiếp khi nhận hàng"
    ),
    VIETQR(
            "VIETQR",
            "Thanh toán qua QR",
            "Quét mã VietQR qua PayOS, xác nhận tự động"
    );

    private final String code;
    private final String title;
    private final String subtitle;

    PaymentMethod(String code, String title, String subtitle) {
        this.code = code;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public static PaymentMethod fromCode(String code) {
        if (VIETQR.code.equals(code) || "ONLINE_GATEWAY".equals(code)) {
            return VIETQR;
        }
        return COD;
    }
}
