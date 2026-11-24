package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages order placement (atomic Room transaction + Firestore sync) and order history queries.
 */
public final class OrderRepository {

    private static final Random RANDOM = new Random();

    private OrderRepository() {
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
            boolean clearCartAfterOrder,
            Consumer<OrderEntity> onCreated
    ) {
        if (!ValidationUtils.isNotBlank(username) || previewItems == null || previewItems.isEmpty()) {
            if (onCreated != null) {
                onCreated.accept(null);
            }
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
            db.runInTransaction(() -> {
                db.orderDao().insertOrder(order);
                db.orderDao().insertOrderItems(persistedItems);
                if (clearCartAfterOrder) {
                    db.cartDao().clearAll();
                }
            });
            FirestoreHelper.saveOrder(order, persistedItems, null);
            if (onCreated != null) {
                AppDatabase.postToMain(() -> onCreated.accept(order));
            }
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

    private static String generateOrderId(long epochMillis) {
        String datePart = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(epochMillis));
        int suffix = 1000 + RANDOM.nextInt(9000);
        return "DH-" + datePart + "-" + suffix;
    }
}
