package com.example.btday4.util;

/**
 * Lớp cấu hình hằng số (Constants) dùng chung cho toàn bộ dự án.
 *
 * QUY TẮC CHUNG CHO TOÀN BỘ DỰ ÁN:
 * 1. Tuyệt đối không truyền trực tiếp chuỗi (hardcoded string literals như "product", "id",...)
 *    vào các hàm của Intent như: putExtra, getStringExtra, getBooleanExtra,...
 * 2. Tất cả các biến đại diện cho Intent Extra bắt buộc phải được khai báo tại file này
 *    dưới dạng public static final String với tiền tố EXTRA_.
 * 3. Khi truyền dữ liệu bằng putExtra hoặc nhận dữ liệu bằng get*Extra, luôn sử dụng biến đại diện.
 */
public final class Constants {

    private Constants() {
    }

    // ==========================================
    // INTENT EXTRA KEYS (Biến đại diện cho putExtra)
    // ==========================================
    public static final String EXTRA_PRODUCT = "product";
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_PRODUCT_NAME = "product_name";
    public static final String EXTRA_PRODUCT_IMAGE_URL = "product_image_url";
    public static final String EXTRA_QUANTITY = "quantity";
    public static final String EXTRA_SELECTED_PRODUCT_IDS = "selected_product_ids";
    public static final String EXTRA_OPEN_CART = "open_cart";
    public static final String EXTRA_OPEN_PRODUCTS = "open_products";
    public static final String EXTRA_OPEN_REVIEW = "open_review";
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_IS_NEW_ORDER = "is_new_order";
    public static final String EXTRA_INITIAL_RATING = "initial_rating";
    public static final String EXTRA_INITIAL_COMMENT = "initial_comment";
    public static final String EXTRA_IS_EDIT_REVIEW = "is_edit_review";

    // ==========================================
    // FIRESTORE COLLECTIONS
    // ==========================================
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_ORDERS = "orders";
    public static final String COLLECTION_PAYMENTS = "payments";

    // ==========================================
    // SHARED PREFERENCES KEYS
    // ==========================================
    public static final String PREF_NAME = "btday4_user_session";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_REMEMBER_ME = "remember_me";

    // ==========================================
    // DEMO ACCOUNT (seeded with a hashed password)
    // ==========================================
    public static final String DEMO_USERNAME = "tantai@techstore.vn";
    public static final String DEMO_PASSWORD = "123456";
    public static final String DEMO_DISPLAY_NAME = "Tantai";

    // ==========================================
    // BUSINESS RULES (Shipping, Cart & PayOS)
    // ==========================================
    public static final long FREE_SHIPPING_THRESHOLD = 50_000L;
    public static final long STANDARD_SHIPPING_FEE = 300L;
    public static final int MAX_CART_QUANTITY = 99;

    public static final String PAYOS_API_BASE_URL = "https://api-merchant.payos.vn";
    public static final String PAYOS_RETURN_URL = "https://btday4.app/payment/return";
    public static final String PAYOS_CANCEL_URL = "https://btday4.app/payment/cancel";
    public static final long PAYOS_LINK_TTL_SECONDS = 15L * 60L;
    public static final long PAYOS_POLL_INTERVAL_MS = 4_000L;
}
