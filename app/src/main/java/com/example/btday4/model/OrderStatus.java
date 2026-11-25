package com.example.btday4.model;

/**
 * Standardized order lifecycle statuses (without manual admin confirmation step).
 */
public enum OrderStatus {
    PENDING_PAYMENT("PENDING_PAYMENT", "Chờ thanh toán QR"),
    PROCESSING("PROCESSING", "Đang giao hàng"),
    PAID("PAID", "Đã thanh toán"),
    DELIVERED("DELIVERED", "Hoàn tất"),
    CANCELLED("CANCELLED", "Đã hủy");

    private final String code;
    private final String label;

    OrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static OrderStatus fromCode(String code) {
        if ("PENDING_CONFIRMATION".equals(code)) {
            return PROCESSING;
        }
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PROCESSING;
    }
}
