package com.example.btday4.data.remote;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.PaymentEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.local.UserEntity;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.util.Constants;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cloud Firestore sync helper using batch writes and lifecycle-safe snapshot listeners.
 */
public final class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";

    private FirestoreHelper() {
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }

    public static FirebaseFirestore getDb() {
        return FirebaseFirestore.getInstance();
    }

    /**
     * Seeds default products to Firestore or refreshes legacy unscaled remote prices using a single batch.
     */
    public static void seedProductsIfEmpty(FirestoreCallback<Boolean> callback) {
        FirebaseFirestore db = getDb();
        CollectionReference col = db.collection(Constants.COLLECTION_PRODUCTS);
        col.document("iphone_15").get()
                .addOnSuccessListener(doc -> {
                    boolean needsSeed = !doc.exists();
                    if (!needsSeed) {
                        Product remoteSample = doc.toObject(Product.class);
                        if (remoteSample != null
                                && com.example.btday4.util.FormatUtils.parseVnd(remoteSample.getPrice())
                                >= ProductRepository.LEGACY_UNSCALED_PRICE_THRESHOLD) {
                            needsSeed = true;
                        }
                    }
                    if (!needsSeed) {
                        if (callback != null) {
                            callback.onSuccess(false);
                        }
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (Product product : ProductRepository.createDefaultProducts()) {
                        batch.set(col.document(product.getId()), product);
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess(true);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to batch sync products", e);
                                if (callback != null) {
                                    callback.onError(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check remote products collection", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Listens for real-time product changes. Callers MUST remove the returned ListenerRegistration
     * when their view is destroyed.
     */
    public static ListenerRegistration listenProducts(FirestoreCallback<List<Product>> callback) {
        return getDb().collection(Constants.COLLECTION_PRODUCTS)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Product snapshot listen error", e);
                        if (callback != null) {
                            callback.onError(e);
                        }
                        return;
                    }
                    if (querySnapshot != null) {
                        List<Product> list = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Product p = doc.toObject(Product.class);
                            if (p != null && !p.getId().isEmpty()) {
                                list.add(p);
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(list);
                        }
                    }
                });
    }

    public static void saveUser(@NonNull UserEntity user, FirestoreCallback<Void> callback) {
        getDb().collection(Constants.COLLECTION_USERS)
                .document(user.getUsername())
                .set(user)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync user", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public static void saveReview(@NonNull ReviewEntity review, FirestoreCallback<Void> callback) {
        getDb().collection(Constants.COLLECTION_REVIEWS)
                .document(review.getId())
                .set(review)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync review", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public static void deleteReview(@NonNull String reviewId, FirestoreCallback<Void> callback) {
        getDb().collection(Constants.COLLECTION_REVIEWS)
                .document(reviewId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete remote review", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public static void saveOrder(
            @NonNull OrderEntity order,
            @NonNull List<OrderItemEntity> items,
            FirestoreCallback<Void> callback
    ) {
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
        payload.put("status", order.getStatus());
        payload.put("createdAt", order.getCreatedAt());
        payload.put("items", items);

        getDb().collection(Constants.COLLECTION_ORDERS)
                .document(order.getId())
                .set(payload)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync order", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public static void updateOrderStatus(
            @NonNull String orderId,
            @NonNull String status,
            FirestoreCallback<Void> callback
    ) {
        getDb().collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .update("status", status)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update remote order status", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Best-effort mirror of a local stock change using an atomic server-side increment.
     */
    public static void adjustProductStock(@NonNull String productId, int delta) {
        getDb().collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .update("stock", FieldValue.increment(delta))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync product stock", e));
    }

    public static void savePayment(@NonNull PaymentEntity payment, FirestoreCallback<Void> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderCode", payment.getOrderCode());
        payload.put("orderId", payment.getOrderId());
        payload.put("paymentLinkId", payment.getPaymentLinkId());
        payload.put("amount", payment.getAmount());
        payload.put("description", payment.getDescription());
        payload.put("checkoutUrl", payment.getCheckoutUrl());
        payload.put("bankBin", payment.getBankBin());
        payload.put("accountNumber", payment.getAccountNumber());
        payload.put("accountName", payment.getAccountName());
        payload.put("status", payment.getStatus());
        payload.put("expiredAtSeconds", payment.getExpiredAtSeconds());
        payload.put("createdAtMillis", payment.getCreatedAtMillis());
        payload.put("paidAtMillis", payment.getPaidAtMillis());

        getDb().collection(Constants.COLLECTION_PAYMENTS)
                .document(String.valueOf(payment.getOrderCode()))
                .set(payload)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync payment", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }
}
