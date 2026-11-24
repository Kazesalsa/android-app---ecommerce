package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.PaymentEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.data.remote.PayOsClient;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.PayOsPaymentInfo;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Coordinates PayOS network operations on a dedicated I/O executor while keeping Room SQLite
 * and Firestore synchronized atomically.
 */
public final class PayOsRepository {

    public interface PaymentCallback {
        void onSuccess(PaymentEntity payment);

        void onError(String message);
    }

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newCachedThreadPool();

    private PayOsRepository() {
    }

    /**
     * Loads the latest saved payment record for an order from local Room SQLite.
     */
    public static void getLatestPaymentForOrder(
            Context context,
            String orderId,
            Consumer<PaymentEntity> callback
    ) {
        AppDatabase.runAsync(context, db -> {
            PaymentEntity payment = db.paymentDao().findLatestByOrderId(orderId);
            if (callback != null) {
                AppDatabase.postToMain(() -> callback.accept(payment));
            }
        });
    }

    /**
     * Reuses a still-valid pending PayOS QR for the order if one exists, otherwise generates
     * a fresh PayOS payment link and persists it locally and in Firestore.
     */
    public static void createOrResumePayment(
            Context context,
            OrderEntity order,
            boolean forceNew,
            PaymentCallback callback
    ) {
        if (order == null) {
            notifyError(callback, "Không tìm thấy thông tin đơn hàng");
            return;
        }
        Context appContext = context.getApplicationContext();
        NETWORK_EXECUTOR.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                PaymentEntity existing = db.paymentDao().findLatestByOrderId(order.getId());
                long nowSeconds = System.currentTimeMillis() / 1000L;

                if (!forceNew && existing != null && existing.isStillReusable(nowSeconds)) {
                    // Check live PayOS status in case the user already transferred while away.
                    try {
                        PayOsPaymentInfo live = PayOsClient.getPaymentStatus(existing.getOrderCode());
                        PaymentEntity synced = applyStatusUpdate(db, existing, live.getStatus());
                        if (!synced.getStatus().equalsIgnoreCase(PayOsPaymentInfo.STATUS_CANCELLED)
                                && !synced.getStatus().equalsIgnoreCase(PayOsPaymentInfo.STATUS_EXPIRED)) {
                            notifySuccess(callback, synced);
                            return;
                        }
                    } catch (PayOsClient.PayOsException ignored) {
                        // Still show the valid cached QR if status check transiently failed.
                        notifySuccess(callback, existing);
                        return;
                    }
                }

                PayOsPaymentInfo created = PayOsClient.createPaymentRequest(
                        order.getId(),
                        order.getTotalAmount(),
                        order.getRecipientName(),
                        order.getRecipientPhone(),
                        order.getShippingAddress()
                );

                long nowMillis = System.currentTimeMillis();
                PaymentEntity entity = new PaymentEntity(
                        created.getOrderCode(),
                        order.getId(),
                        created.getPaymentLinkId(),
                        created.getAmount(),
                        created.getDescription(),
                        created.getCheckoutUrl(),
                        created.getQrCode(),
                        created.getBin(),
                        created.getAccountNumber(),
                        created.getAccountName(),
                        created.getStatus(),
                        created.getExpiredAtSeconds(),
                        nowMillis,
                        0L
                );

                db.paymentDao().upsert(entity);
                FirestoreHelper.savePayment(entity, null);
                notifySuccess(callback, entity);
            } catch (PayOsClient.PayOsException e) {
                notifyError(callback, e.getMessage());
            } catch (Exception e) {
                notifyError(callback, "Không thể khởi tạo mã QR PayOS: " + e.getMessage());
            }
        });
    }

    /**
     * Polls PayOS for the current payment status and atomically marks the order PAID when confirmed.
     */
    public static void refreshPaymentStatus(
            Context context,
            PaymentEntity payment,
            PaymentCallback callback
    ) {
        if (payment == null) {
            notifyError(callback, "Không có giao dịch PayOS để kiểm tra");
            return;
        }
        Context appContext = context.getApplicationContext();
        NETWORK_EXECUTOR.execute(() -> {
            try {
                PayOsPaymentInfo live = PayOsClient.getPaymentStatus(payment.getOrderCode());
                AppDatabase db = AppDatabase.getInstance(appContext);
                PaymentEntity updated = applyStatusUpdate(db, payment, live.getStatus());
                notifySuccess(callback, updated);
            } catch (PayOsClient.PayOsException e) {
                notifyError(callback, e.getMessage());
            } catch (Exception e) {
                notifyError(callback, "Lỗi kiểm tra trạng thái thanh toán");
            }
        });
    }

    /**
     * Cancels an active PayOS QR link and marks both the payment and order as CANCELLED.
     */
    public static void cancelPayment(
            Context context,
            PaymentEntity payment,
            String reason,
            PaymentCallback callback
    ) {
        if (payment == null) {
            notifyError(callback, "Không có giao dịch để hủy");
            return;
        }
        Context appContext = context.getApplicationContext();
        NETWORK_EXECUTOR.execute(() -> {
            try {
                try {
                    PayOsClient.cancelPaymentRequest(payment.getOrderCode(), reason);
                } catch (PayOsClient.PayOsException e) {
                    // Check if PayOS already marked it PAID before letting cancellation proceed.
                    try {
                        PayOsPaymentInfo live = PayOsClient.getPaymentStatus(payment.getOrderCode());
                        if (live.isPaid()) {
                            AppDatabase db = AppDatabase.getInstance(appContext);
                            PaymentEntity paidEntity = applyStatusUpdate(db, payment, PayOsPaymentInfo.STATUS_PAID);
                            notifySuccess(callback, paidEntity);
                            return;
                        }
                    } catch (PayOsClient.PayOsException ignored) {
                        // Fall through to local cancellation if PayOS link is already cancelled/expired.
                    }
                }

                AppDatabase db = AppDatabase.getInstance(appContext);
                PaymentEntity cancelled = applyStatusUpdate(db, payment, PayOsPaymentInfo.STATUS_CANCELLED);
                notifySuccess(callback, cancelled);
            } catch (Exception e) {
                notifyError(callback, "Không thể hủy giao dịch: " + e.getMessage());
            }
        });
    }

    private static PaymentEntity applyStatusUpdate(
            AppDatabase db,
            PaymentEntity original,
            String newStatus
    ) {
        String normalized = newStatus == null || newStatus.trim().isEmpty()
                ? original.getStatus()
                : newStatus.trim().toUpperCase();
        long paidAt = PayOsPaymentInfo.STATUS_PAID.equals(normalized)
                ? (original.getPaidAtMillis() > 0L ? original.getPaidAtMillis() : System.currentTimeMillis())
                : original.getPaidAtMillis();

        PaymentEntity updated = new PaymentEntity(
                original.getOrderCode(),
                original.getOrderId(),
                original.getPaymentLinkId(),
                original.getAmount(),
                original.getDescription(),
                original.getCheckoutUrl(),
                original.getQrCode(),
                original.getBankBin(),
                original.getAccountNumber(),
                original.getAccountName(),
                normalized,
                original.getExpiredAtSeconds(),
                original.getCreatedAtMillis(),
                paidAt
        );

        String targetOrderStatus = null;
        if (PayOsPaymentInfo.STATUS_PAID.equals(normalized)) {
            targetOrderStatus = OrderStatus.PAID.getCode();
        } else if (PayOsPaymentInfo.STATUS_CANCELLED.equals(normalized)) {
            targetOrderStatus = OrderStatus.CANCELLED.getCode();
        }

        final String finalOrderStatus = targetOrderStatus;
        db.runInTransaction(() -> {
            db.paymentDao().upsert(updated);
            if (finalOrderStatus != null) {
                db.orderDao().updateStatus(updated.getOrderId(), finalOrderStatus);
            }
        });

        FirestoreHelper.savePayment(updated, null);
        if (finalOrderStatus != null) {
            FirestoreHelper.updateOrderStatus(updated.getOrderId(), finalOrderStatus, null);
        }
        return updated;
    }

    private static void notifySuccess(PaymentCallback callback, PaymentEntity payment) {
        if (callback != null) {
            AppDatabase.postToMain(() -> callback.onSuccess(payment));
        }
    }

    private static void notifyError(PaymentCallback callback, String message) {
        if (callback != null) {
            String safeMessage = message == null || message.trim().isEmpty()
                    ? "Không thể kết nối cổng thanh toán PayOS"
                    : message;
            AppDatabase.postToMain(() -> callback.onError(safeMessage));
        }
    }
}
