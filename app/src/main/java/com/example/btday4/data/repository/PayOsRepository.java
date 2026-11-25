package com.example.btday4.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.PaymentEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.data.remote.PayOsClient;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.PayOsPaymentInfo;
import com.example.btday4.util.Constants;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Coordinates PayOS network operations on a dedicated I/O executor backed 100% by Cloud Firestore.
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
     * Loads the latest saved payment record for an order from Cloud Firestore.
     */
    public static void getLatestPaymentForOrder(
            Context context,
            String orderId,
            Consumer<PaymentEntity> callback
    ) {
        if (orderId == null || orderId.trim().isEmpty()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PAYMENTS)
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (callback != null) {
                        callback.accept(pickLatest(snapshot));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.accept(null);
                    }
                });
    }

    /**
     * Reuses a still-valid pending PayOS QR for the order if one exists, otherwise generates
     * a fresh PayOS payment link and persists it in Firestore.
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
        NETWORK_EXECUTOR.execute(() -> {
            try {
                PaymentEntity existing = findLatestSync(order.getId());
                long nowSeconds = System.currentTimeMillis() / 1000L;

                if (!forceNew && existing != null && existing.isStillReusable(nowSeconds)) {
                    try {
                        PayOsPaymentInfo live = PayOsClient.getPaymentStatus(existing.getOrderCode());
                        PaymentEntity synced = applyStatusUpdate(existing, live.getStatus());
                        if (!synced.getStatus().equalsIgnoreCase(PayOsPaymentInfo.STATUS_CANCELLED)
                                && !synced.getStatus().equalsIgnoreCase(PayOsPaymentInfo.STATUS_EXPIRED)) {
                            notifySuccess(callback, synced);
                            return;
                        }
                    } catch (PayOsClient.PayOsException ignored) {
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
     * Polls PayOS for the current payment status and marks the order PAID in Firestore when confirmed.
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
        NETWORK_EXECUTOR.execute(() -> {
            try {
                PayOsPaymentInfo live = PayOsClient.getPaymentStatus(payment.getOrderCode());
                PaymentEntity updated = applyStatusUpdate(payment, live.getStatus());
                notifySuccess(callback, updated);
            } catch (PayOsClient.PayOsException e) {
                notifyError(callback, e.getMessage());
            } catch (Exception e) {
                notifyError(callback, "Lỗi kiểm tra trạng thái thanh toán");
            }
        });
    }

    /**
     * Cancels an active PayOS QR link and marks both the payment and order as CANCELLED in Firestore.
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
        NETWORK_EXECUTOR.execute(() -> {
            try {
                try {
                    PayOsClient.cancelPaymentRequest(payment.getOrderCode(), reason);
                } catch (PayOsClient.PayOsException e) {
                    try {
                        PayOsPaymentInfo live = PayOsClient.getPaymentStatus(payment.getOrderCode());
                        if (live.isPaid()) {
                            PaymentEntity paidEntity = applyStatusUpdate(payment, PayOsPaymentInfo.STATUS_PAID);
                            notifySuccess(callback, paidEntity);
                            return;
                        }
                    } catch (PayOsClient.PayOsException ignored) {
                        // Fall through to local cancellation if PayOS link is already cancelled/expired.
                    }
                }

                PaymentEntity cancelled = applyStatusUpdate(payment, PayOsPaymentInfo.STATUS_CANCELLED);
                notifySuccess(callback, cancelled);
            } catch (Exception e) {
                notifyError(callback, "Không thể hủy giao dịch: " + e.getMessage());
            }
        });
    }

    private static PaymentEntity findLatestSync(String orderId) {
        try {
            QuerySnapshot snap = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection(Constants.COLLECTION_PAYMENTS)
                            .whereEqualTo("orderId", orderId)
                            .get()
            );
            return pickLatest(snap);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PaymentEntity pickLatest(QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        PaymentEntity latest = null;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            PaymentEntity candidate = doc.toObject(PaymentEntity.class);
            if (candidate == null) {
                continue;
            }
            if (latest == null || candidate.getCreatedAtMillis() > latest.getCreatedAtMillis()) {
                latest = candidate;
            }
        }
        return latest;
    }

    private static PaymentEntity applyStatusUpdate(PaymentEntity original, String newStatus) {
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

        FirestoreHelper.savePayment(updated, null);
        if (PayOsPaymentInfo.STATUS_PAID.equals(normalized)) {
            FirestoreHelper.updateOrderStatus(updated.getOrderId(), OrderStatus.PAID.getCode(), null);
        } else if (PayOsPaymentInfo.STATUS_CANCELLED.equals(normalized)) {
            OrderRepository.cancelAndRestockSync(updated.getOrderId());
        }
        return updated;
    }

    private static void notifySuccess(PaymentCallback callback, PaymentEntity payment) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(payment));
        }
    }

    private static void notifyError(PaymentCallback callback, String message) {
        if (callback != null) {
            String safeMessage = message == null || message.trim().isEmpty()
                    ? "Không thể kết nối cổng thanh toán PayOS"
                    : message;
            new Handler(Looper.getMainLooper()).post(() -> callback.onError(safeMessage));
        }
    }
}
