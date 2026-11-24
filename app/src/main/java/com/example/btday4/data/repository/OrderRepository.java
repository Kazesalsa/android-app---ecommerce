package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages order placement (atomic Room transaction reserving stock + Firestore sync), cancellation
 * with restocking, and order history queries.
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
            OrderStatus.PENDING_CONFIRMATION.getCode(),
            OrderStatus.PENDING_PAYMENT.getCode()
    );

    private static final Random RANDOM = new Random();

    private OrderRepository() {
    }

    /**
     * Thrown inside the Room transaction to roll back every reservation when any line lacks stock.
     */
    private static final class InsufficientStockException extends RuntimeException {
        final String productId;

        InsufficientStockException(String productId) {
            super(productId);
            this.productId = productId;
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
        long total = subtotal + shippingFee;
        PaymentMethod method = paymentMethod == null ? PaymentMethod.COD : paymentMethod;
        OrderStatus initialStatus = method == PaymentMethod.VIETQR
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.PENDING_CONFIRMATION;

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
                total,
                initialStatus.getCode(),
                now
        );

        AppDatabase.runAsync(context, db -> {
            try {
                db.runInTransaction(() -> {
                    for (OrderItemEntity item : persistedItems) {
                        if (db.productDao().decrementStock(item.getProductId(), item.getQuantity()) == 0) {
                            throw new InsufficientStockException(item.getProductId());
                        }
                    }
                    db.orderDao().insertOrder(order);
                    db.orderDao().insertOrderItems(persistedItems);
                    if (removeFromCartAfterOrder) {
                        List<String> orderedIds = new ArrayList<>();
                        for (OrderItemEntity item : persistedItems) {
                            orderedIds.add(item.getProductId());
                        }
                        db.cartDao().deleteProducts(order.getUsername(), orderedIds);
                    }
                });
            } catch (InsufficientStockException e) {
                String message = buildOutOfStockMessage(db.productDao().findById(e.productId));
                AppDatabase.postToMain(() -> callback.onError(message));
                return;
            }
            for (OrderItemEntity item : persistedItems) {
                FirestoreHelper.adjustProductStock(item.getProductId(), -item.getQuantity());
            }
            FirestoreHelper.saveOrder(order, persistedItems, null);
            AppDatabase.postToMain(() -> callback.onSuccess(order));
        });
    }

    /**
     * Cancels a COD order awaiting confirmation (or an unpaid QR order without a PayOS link) and
     * returns its items to stock. Reports false when the order can no longer be cancelled.
     */
    public static void cancelOrder(Context context, String orderId, Consumer<Boolean> callback) {
        AppDatabase.runAsync(context, db -> {
            boolean cancelled = cancelAndRestock(db, orderId);
            AppDatabase.postToMain(() -> callback.accept(cancelled));
        });
    }

    /**
     * Atomically marks a cancellable order CANCELLED and restocks its items exactly once.
     * Must be called off the main thread.
     */
    public static boolean cancelAndRestock(AppDatabase db, String orderId) {
        OrderWithItems[] restocked = {null};
        db.runInTransaction(() -> {
            int changed = db.orderDao().transitionStatus(
                    orderId,
                    OrderStatus.CANCELLED.getCode(),
                    CANCELLABLE_STATUSES
            );
            if (changed == 0) {
                return;
            }
            OrderWithItems order = db.orderDao().findOrderWithItemsById(orderId);
            for (OrderItemEntity item : order.safeItems()) {
                db.productDao().incrementStock(item.getProductId(), item.getQuantity());
            }
            restocked[0] = order;
        });
        if (restocked[0] == null) {
            return false;
        }
        for (OrderItemEntity item : restocked[0].safeItems()) {
            FirestoreHelper.adjustProductStock(item.getProductId(), item.getQuantity());
        }
        FirestoreHelper.updateOrderStatus(orderId, OrderStatus.CANCELLED.getCode(), null);
        return true;
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
        AppDatabase.runAsync(context, db -> {
            List<OrderWithItems> orders = db.orderDao().getOrdersByUsername(username);
            AppDatabase.postToMain(() -> callback.accept(orders));
        });
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
        AppDatabase.runAsync(context, db -> {
            OrderWithItems order = db.orderDao().findOrderWithItemsById(orderId);
            AppDatabase.postToMain(() -> callback.accept(order));
        });
    }

    public static void countOrdersForUser(Context context, String username, Consumer<Integer> callback) {
        if (!ValidationUtils.isNotBlank(username)) {
            callback.accept(0);
            return;
        }
        AppDatabase.runAsync(context, db -> {
            int count = db.orderDao().countByUsername(username);
            AppDatabase.postToMain(() -> callback.accept(count));
        });
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
