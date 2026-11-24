package com.example.btday4.model;

/**
 * Standardized order lifecycle statuses with display labels.
 */
public enum OrderStatus {
    PENDING_CONFIRMATION("PENDING_CONFIRMATION", "Chờ xác nhận"),
    PENDING_PAYMENT("PENDING_PAYMENT", "Chờ thanh toán QR"),
    PAID("PAID", "Đã thanh toán"),
    PROCESSING("PROCESSING", "Đang chuẩn bị"),
    DELIVERED("DELIVERED", "Đã giao hàng"),
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
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING_CONFIRMATION;
    }
}
