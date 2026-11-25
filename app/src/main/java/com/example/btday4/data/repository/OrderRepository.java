package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.model.Voucher;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages order placement, atomic stock reservation/restocking, and order history 100% in Cloud Firestore.
 */
public final class OrderRepository {

    public interface PlaceOrderCallback {
        void onSuccess(OrderEntity order);

        void onError(String message);
    }

    /**
     * Statuses a shopper may still cancel; cancelling from any of them returns items to stock.
     */
    public static final List<String> CANCELLABLE_STATUSES = Arrays.asList(
            OrderStatus.PROCESSING.getCode(),
            OrderStatus.PENDING_PAYMENT.getCode(),
            "PENDING_CONFIRMATION"
    );

    private static final Random RANDOM = new Random();

    private OrderRepository() {
    }

    private static final class InsufficientStockException extends RuntimeException {
        final String userMessage;

        InsufficientStockException(String userMessage) {
            super(userMessage);
            this.userMessage = userMessage;
        }
    }

    private static final class VoucherInvalidException extends RuntimeException {
        final String userMessage;

        VoucherInvalidException(String userMessage) {
            super(userMessage);
            this.userMessage = userMessage;
        }
    }

    public static void placeOrder(
            Context context,
            String username,
            String recipientName,
            String recipientPhone,
            String shippingAddress,
            String note,
            PaymentMethod paymentMethod,
            List<OrderItemEntity> previewItems,
            boolean removeFromCartAfterOrder,
            PlaceOrderCallback callback
    ) {
        placeOrder(
                context,
                username,
                recipientName,
                recipientPhone,
                shippingAddress,
                note,
                paymentMethod,
                previewItems,
                "",
                0L,
                removeFromCartAfterOrder,
                callback
        );
    }

    public static void placeOrder(
            Context context,
            String username,
            String recipientName,
            String recipientPhone,
            String shippingAddress,
            String note,
            PaymentMethod paymentMethod,
            List<OrderItemEntity> previewItems,
            String voucherCode,
            long voucherDiscount,
            boolean removeFromCartAfterOrder,
            PlaceOrderCallback callback
    ) {
        if (!ValidationUtils.isNotBlank(username) || previewItems == null || previewItems.isEmpty()) {
            callback.onError("Không thể tạo đơn hàng, vui lòng thử lại");
            return;
        }

        long now = System.currentTimeMillis();
        String orderId = generateOrderId(now);

        long subtotal = 0L;
        List<OrderItemEntity> persistedItems = new ArrayList<>();
        for (OrderItemEntity item : previewItems) {
            subtotal += item.lineSubtotal();
            persistedItems.add(new OrderItemEntity(
                    orderId,
                    item.getProductId(),
                    item.getProductName(),
                    item.getProductImageUrl(),
                    item.getUnitPrice(),
                    item.getQuantity()
            ));
        }

        long shippingFee = FormatUtils.computeShippingFee(subtotal);
        String cleanVoucherCode = voucherCode == null ? "" : voucherCode.trim().toUpperCase(Locale.US);
        PaymentMethod method = paymentMethod == null ? PaymentMethod.COD : paymentMethod;
        OrderStatus initialStatus = method == PaymentMethod.VIETQR
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.PROCESSING;

        OrderEntity order = new OrderEntity(
                orderId,
                username.trim(),
                recipientName.trim(),
                ValidationUtils.normalizePhone(recipientPhone),
                shippingAddress.trim(),
                note == null ? "" : note.trim(),
                method.getCode(),
                subtotal,
                shippingFee,
                subtotal + shippingFee,
                "",
                0L,
                initialStatus.getCode(),
                now
        );

        if (cleanVoucherCode.isEmpty()) {
            runPlaceOrderTransaction(
                    context,
                    order,
                    persistedItems,
                    "",
                    "",
                    removeFromCartAfterOrder,
                    callback
            );
            return;
        }

        VoucherRepository.findByCode(cleanVoucherCode, matched -> {
            if (matched == null) {
                callback.onError("Mã giảm giá \"" + cleanVoucherCode + "\" không còn tồn tại trên hệ thống");
                return;
            }
            runPlaceOrderTransaction(
                    context,
                    order,
                    persistedItems,
                    matched.getCode(),
                    matched.getDocId(),
                    removeFromCartAfterOrder,
                    callback
            );
        });
    }

    private static void runPlaceOrderTransaction(
            Context context,
            OrderEntity order,
            List<OrderItemEntity> persistedItems,
            String voucherCode,
            String voucherDocId,
            boolean removeFromCartAfterOrder,
            PlaceOrderCallback callback
    ) {
        long now = order.getCreatedAt();
        long subtotal = order.getSubtotalAmount();
        long shippingFee = order.getShippingFee();
        String cleanVoucherCode = voucherCode == null ? "" : voucherCode.trim().toUpperCase(Locale.US);
        String cleanVoucherDocId = voucherDocId == null ? "" : voucherDocId.trim();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            // 1. Read all referenced products and optional voucher docs first (required by Firestore transactions).
            Map<String, Product> loadedProducts = new HashMap<>();
            Map<String, DocumentReference> productRefs = new HashMap<>();
            for (OrderItemEntity item : persistedItems) {
                if (productRefs.containsKey(item.getProductId())) {
                    continue;
                }
                DocumentReference ref = db.collection(Constants.COLLECTION_PRODUCTS).document(item.getProductId());
                productRefs.put(item.getProductId(), ref);
                DocumentSnapshot snap = transaction.get(ref);
                loadedProducts.put(item.getProductId(), snap.exists() ? snap.toObject(Product.class) : null);
            }

            DocumentReference voucherRef = null;
            DocumentReference userVoucherRef = null;
            Voucher liveVoucher = null;
            long verifiedDiscount = 0L;

            if (!cleanVoucherCode.isEmpty() && !cleanVoucherDocId.isEmpty()) {
                voucherRef = db.collection(Constants.COLLECTION_VOUCHERS).document(cleanVoucherDocId);
                userVoucherRef = db.collection(Constants.COLLECTION_USERS)
                        .document(order.getUsername())
                        .collection(Constants.COLLECTION_USER_VOUCHERS)
                        .document(cleanVoucherCode);

                DocumentSnapshot voucherSnap = transaction.get(voucherRef);
                DocumentSnapshot userVoucherSnap = transaction.get(userVoucherRef);

                liveVoucher = VoucherRepository.parseVoucher(voucherSnap);
                if (liveVoucher == null) {
                    throw new VoucherInvalidException("Mã giảm giá \"" + cleanVoucherCode + "\" không còn tồn tại");
                }
                if (!liveVoucher.isActive()) {
                    throw new VoucherInvalidException("Mã giảm giá \"" + cleanVoucherCode + "\" hiện đang tạm ngưng");
                }
                if (liveVoucher.isExpired(now)) {
                    throw new VoucherInvalidException("Mã giảm giá \"" + cleanVoucherCode + "\" đã hết hạn");
                }
                if (!liveVoucher.hasRemainingUses()) {
                    throw new VoucherInvalidException("Mã giảm giá \"" + cleanVoucherCode + "\" đã hết lượt sử dụng");
                }
                if (userVoucherSnap != null && userVoucherSnap.exists()
                        && Boolean.TRUE.equals(userVoucherSnap.getBoolean("used"))) {
                    throw new VoucherInvalidException("Bạn đã sử dụng mã \"" + cleanVoucherCode + "\" trước đó");
                }
                verifiedDiscount = liveVoucher.computeDiscount(subtotal, shippingFee);
                if (verifiedDiscount <= 0L) {
                    throw new VoucherInvalidException("Đơn hàng chưa đủ điều kiện áp dụng mã \"" + cleanVoucherCode + "\"");
                }
            }

            // 2. Verify stock availability for every item.
            Map<String, Integer> remainingStock = new HashMap<>();
            for (OrderItemEntity item : persistedItems) {
                Product product = loadedProducts.get(item.getProductId());
                int currentStock = remainingStock.containsKey(item.getProductId())
                        ? remainingStock.get(item.getProductId())
                        : (product == null ? -1 : product.getStock());
                if (product == null || currentStock < item.getQuantity()) {
                    throw new InsufficientStockException(buildOutOfStockMessage(product));
                }
                remainingStock.put(item.getProductId(), currentStock - item.getQuantity());
            }

            // 3. Decrement stock, redeem voucher atomically, persist order, and clear ordered cart lines.
            for (Map.Entry<String, Integer> entry : remainingStock.entrySet()) {
                transaction.update(productRefs.get(entry.getKey()), "stock", entry.getValue());
            }

            if (liveVoucher != null && voucherRef != null && userVoucherRef != null && verifiedDiscount > 0L) {
                order.setVoucherCode(liveVoucher.getCode());
                order.setVoucherDiscount(verifiedDiscount);
                order.setTotalAmount(Math.max(Constants.MIN_PAYABLE_TOTAL, subtotal + shippingFee - verifiedDiscount));

                Map<String, Object> userVoucherPayload = new HashMap<>();
                userVoucherPayload.put("code", liveVoucher.getCode());
                userVoucherPayload.put("docId", cleanVoucherDocId);
                userVoucherPayload.put("claimed", true);
                userVoucherPayload.put("used", true);
                userVoucherPayload.put("usedOrderId", order.getId());
                userVoucherPayload.put("updatedAt", now);
                transaction.set(userVoucherRef, userVoucherPayload, SetOptions.merge());

                if (liveVoucher.getUsageLimit() > 0) {
                    transaction.update(voucherRef, "usedCount", liveVoucher.getUsedCount() + 1);
                }
            }

            Map<String, Object> orderPayload = buildOrderPayload(order, persistedItems);
            if (!cleanVoucherDocId.isEmpty() && order.getVoucherDiscount() > 0L) {
                orderPayload.put("voucherDocId", cleanVoucherDocId);
            }
            DocumentReference orderRef = db.collection(Constants.COLLECTION_ORDERS).document(order.getId());
            transaction.set(orderRef, orderPayload);

            if (removeFromCartAfterOrder) {
                for (String productId : productRefs.keySet()) {
                    DocumentReference cartItemRef = db.collection(Constants.COLLECTION_USERS)
                            .document(order.getUsername())
                            .collection(Constants.COLLECTION_CART)
                            .document(productId);
                    transaction.delete(cartItemRef);
                }
            }
            return order;
        }).addOnSuccessListener(created -> {
            if (context != null && !created.getVoucherCode().isEmpty()) {
                if (created.getVoucherCode().equalsIgnoreCase(
                        VoucherRepository.getSelectedCartVoucherCode(context, created.getUsername()))) {
                    VoucherRepository.setSelectedCartVoucherCode(context, created.getUsername(), "");
                }
            }
            callback.onSuccess(created);
        }).addOnFailureListener(e -> {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof InsufficientStockException) {
                callback.onError(((InsufficientStockException) cause).userMessage);
            } else if (e instanceof InsufficientStockException) {
                callback.onError(((InsufficientStockException) e).userMessage);
            } else if (cause instanceof VoucherInvalidException) {
                callback.onError(((VoucherInvalidException) cause).userMessage);
            } else if (e instanceof VoucherInvalidException) {
                callback.onError(((VoucherInvalidException) e).userMessage);
            } else {
                callback.onError("Không thể đặt hàng: " + e.getMessage());
            }
        });
    }

    /**
     * Cancels a cancellable order and atomically restocks its items in Cloud Firestore.
     */
    public static void cancelOrder(Context context, String orderId, Consumer<Boolean> callback) {
        if (!ValidationUtils.isNotBlank(orderId)) {
            callback.accept(false);
            return;
        }
        getOrderDetail(context, orderId, existing ->
                runCancelAndRestockTransaction(orderId)
                        .addOnSuccessListener(cancelled -> {
                            boolean ok = Boolean.TRUE.equals(cancelled);
                            if (ok && existing != null && existing.order != null) {
                                VoucherRepository.restoreVoucherIfCancelled(
                                        context,
                                        existing.order.getUsername(),
                                        existing.order.getVoucherCode()
                                );
                            }
                            callback.accept(ok);
                        })
                        .addOnFailureListener(e -> callback.accept(false))
        );
    }

    /**
     * Blocking variant for background threads (used by PayOsRepository after cancelling a QR link).
     */
    public static boolean cancelAndRestockSync(String orderId) {
        if (!ValidationUtils.isNotBlank(orderId)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(Tasks.await(runCancelAndRestockTransaction(orderId)));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static com.google.android.gms.tasks.Task<Boolean> runCancelAndRestockTransaction(String orderId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference orderRef = db.collection(Constants.COLLECTION_ORDERS).document(orderId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot orderDoc = transaction.get(orderRef);
            OrderWithItems orderWithItems = parseOrderDocument(orderDoc);
            if (orderWithItems == null || orderWithItems.order == null) {
                return false;
            }
            if (!CANCELLABLE_STATUSES.contains(orderWithItems.order.getStatus())) {
                return false;
            }

            Map<String, Integer> qtyToRestock = new HashMap<>();
            for (OrderItemEntity item : orderWithItems.safeItems()) {
                if (!item.getProductId().isEmpty() && item.getQuantity() > 0) {
                    qtyToRestock.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                }
            }

            Map<DocumentReference, Integer> newStocks = new HashMap<>();
            for (Map.Entry<String, Integer> entry : qtyToRestock.entrySet()) {
                DocumentReference productRef = db.collection(Constants.COLLECTION_PRODUCTS).document(entry.getKey());
                DocumentSnapshot productDoc = transaction.get(productRef);
                if (productDoc.exists()) {
                    Long currentStock = productDoc.getLong("stock");
                    int base = currentStock == null ? 0 : currentStock.intValue();
                    newStocks.put(productRef, base + entry.getValue());
                }
            }

            String appliedCode = orderWithItems.order.getVoucherCode();
            String appliedDocId = nonEmpty(orderDoc.getString("voucherDocId"), appliedCode);
            DocumentReference userVoucherRef = null;
            DocumentReference voucherRef = null;
            boolean shouldRestoreUserVoucher = false;
            int restoredUsedCount = -1;

            if (ValidationUtils.isNotBlank(appliedCode)) {
                String cleanCode = appliedCode.trim().toUpperCase(Locale.US);
                if (ValidationUtils.isNotBlank(orderWithItems.order.getUsername())) {
                    userVoucherRef = db.collection(Constants.COLLECTION_USERS)
                            .document(orderWithItems.order.getUsername().trim())
                            .collection(Constants.COLLECTION_USER_VOUCHERS)
                            .document(cleanCode);
                    DocumentSnapshot userVoucherDoc = transaction.get(userVoucherRef);
                    if (userVoucherDoc.exists() && Boolean.TRUE.equals(userVoucherDoc.getBoolean("used"))) {
                        shouldRestoreUserVoucher = true;
                    }
                }
                if (ValidationUtils.isNotBlank(appliedDocId)) {
                    voucherRef = db.collection(Constants.COLLECTION_VOUCHERS).document(appliedDocId.trim());
                    DocumentSnapshot voucherDoc = transaction.get(voucherRef);
                    if (voucherDoc.exists()) {
                        Long usedVal = voucherDoc.getLong("usedCount");
                        if (usedVal != null && usedVal > 0L) {
                            restoredUsedCount = (int) (usedVal - 1L);
                        }
                    }
                }
            }

            transaction.update(orderRef, "status", OrderStatus.CANCELLED.getCode());
            for (Map.Entry<DocumentReference, Integer> entry : newStocks.entrySet()) {
                transaction.update(entry.getKey(), "stock", entry.getValue());
            }
            if (shouldRestoreUserVoucher && userVoucherRef != null) {
                Map<String, Object> restorePayload = new HashMap<>();
                restorePayload.put("used", false);
                restorePayload.put("updatedAt", System.currentTimeMillis());
                transaction.set(userVoucherRef, restorePayload, SetOptions.merge());
            }
            if (voucherRef != null && restoredUsedCount >= 0) {
                transaction.update(voucherRef, "usedCount", restoredUsedCount);
            }
            return true;
        });
    }

    public static void getOrdersForUser(
            Context context,
            String username,
            Consumer<List<OrderWithItems>> callback
    ) {
        if (!ValidationUtils.isNotBlank(username)) {
            callback.accept(new ArrayList<>());
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<OrderWithItems> orders = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        OrderWithItems parsed = parseOrderDocument(doc);
                        if (parsed != null) {
                            orders.add(parsed);
                        }
                    }
                    orders.sort((a, b) -> Long.compare(b.order.getCreatedAt(), a.order.getCreatedAt()));
                    callback.accept(orders);
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
    }

    public static void getOrderDetail(
            Context context,
            String orderId,
            Consumer<OrderWithItems> callback
    ) {
        if (!ValidationUtils.isNotBlank(orderId)) {
            callback.accept(null);
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> callback.accept(parseOrderDocument(doc)))
                .addOnFailureListener(e -> callback.accept(null));
    }

    private static Map<String, Object> buildOrderPayload(OrderEntity order, List<OrderItemEntity> items) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", order.getId());
        payload.put("username", order.getUsername());
        payload.put("recipientName", order.getRecipientName());
        payload.put("recipientPhone", order.getRecipientPhone());
        payload.put("shippingAddress", order.getShippingAddress());
        payload.put("note", order.getNote());
        payload.put("paymentMethod", order.getPaymentMethod());
        payload.put("subtotalAmount", order.getSubtotalAmount());
        payload.put("shippingFee", order.getShippingFee());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("voucherCode", order.getVoucherCode());
        payload.put("voucherDiscount", order.getVoucherDiscount());
        payload.put("status", order.getStatus());
        payload.put("createdAt", order.getCreatedAt());

        List<Map<String, Object>> serializedItems = new ArrayList<>(items.size());
        for (OrderItemEntity item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", item.getOrderId());
            row.put("productId", item.getProductId());
            row.put("productName", item.getProductName());
            row.put("productImageUrl", item.getProductImageUrl());
            row.put("unitPrice", item.getUnitPrice());
            row.put("quantity", item.getQuantity());
            serializedItems.add(row);
        }
        payload.put("items", serializedItems);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private static OrderWithItems parseOrderDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        String id = nonEmpty(doc.getString("id"), doc.getId());
        OrderEntity order = new OrderEntity(
                id,
                nonEmpty(doc.getString("username"), ""),
                nonEmpty(doc.getString("recipientName"), ""),
                nonEmpty(doc.getString("recipientPhone"), ""),
                nonEmpty(doc.getString("shippingAddress"), ""),
                doc.getString("note"),
                nonEmpty(doc.getString("paymentMethod"), PaymentMethod.COD.getCode()),
                longVal(doc.get("subtotalAmount")),
                longVal(doc.get("shippingFee")),
                longVal(doc.get("totalAmount")),
                nonEmpty(doc.getString("voucherCode"), ""),
                longVal(doc.get("voucherDiscount")),
                nonEmpty(doc.getString("status"), OrderStatus.PROCESSING.getCode()),
                longVal(doc.get("createdAt"))
        );

        List<OrderItemEntity> items = new ArrayList<>();
        Object rawItems = doc.get("items");
        if (rawItems instanceof List<?>) {
            for (Object entry : (List<?>) rawItems) {
                if (entry instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) entry;
                    items.add(new OrderItemEntity(
                            id,
                            nonEmpty(stringVal(map.get("productId")), ""),
                            nonEmpty(stringVal(map.get("productName")), ""),
                            stringVal(map.get("productImageUrl")),
                            longVal(map.get("unitPrice")),
                            (int) Math.max(1L, longVal(map.get("quantity")))
                    ));
                }
            }
        }

        OrderWithItems result = new OrderWithItems();
        result.order = order;
        result.items = items;
        return result;
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long longVal(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static String buildOutOfStockMessage(Product product) {
        if (product == null) {
            return "Sản phẩm không còn được bán, vui lòng cập nhật giỏ hàng";
        }
        return product.getStock() > 0
                ? "\"" + product.getName() + "\" chỉ còn " + product.getStock() + " sản phẩm trong kho"
                : "\"" + product.getName() + "\" đã hết hàng";
    }

    private static String generateOrderId(long epochMillis) {
        String datePart = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(epochMillis));
        int suffix = 1000 + RANDOM.nextInt(9000);
        return "DH-" + datePart + "-" + suffix;
    }
}
