package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.data.local.VoucherEntity;
import com.example.btday4.util.Constants;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Cloud Firestore-backed repository for reusable discount vouchers (`vouchers/{code}`).
 */
public final class VoucherRepository {

    private static final String TAG = "VoucherRepository";
    private static final String PREF_VOUCHER = "btday4_voucher_pref";
    private static final String KEY_PENDING_CODE = "pending_voucher_code";

    private VoucherRepository() {
    }

    public static List<VoucherEntity> createDefaultVouchers() {
        List<VoucherEntity> list = new ArrayList<>();
        list.add(new VoucherEntity(
                "TECH10",
                "Giảm 10% đơn công nghệ",
                "Giảm 10% tối đa 50.000 ₫ cho đơn hàng từ 50.000 ₫.",
                VoucherEntity.TYPE_PERCENT,
                10L,
                50_000L,
                50_000L,
                100,
                0,
                true,
                0L
        ));
        list.add(new VoucherEntity(
                "GIAM20K",
                "Giảm thẳng 20.000 ₫",
                "Áp dụng cho đơn hàng công nghệ từ 100.000 ₫ trở lên.",
                VoucherEntity.TYPE_FIXED,
                20_000L,
                0L,
                100_000L,
                50,
                0,
                true,
                0L
        ));
        list.add(new VoucherEntity(
                "WELCOME5K",
                "Ưu đãi thành viên 5.000 ₫",
                "Giảm ngay 5.000 ₫ cho mọi đơn hàng từ 20.000 ₫.",
                VoucherEntity.TYPE_FIXED,
                5_000L,
                0L,
                20_000L,
                200,
                0,
                true,
                0L
        ));
        return list;
    }

    public static void seedVouchersIfEmpty(@Nullable Runnable onDone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference col = db.collection(Constants.COLLECTION_VOUCHERS);
        col.limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        if (onDone != null) {
                            onDone.run();
                        }
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (VoucherEntity voucher : createDefaultVouchers()) {
                        batch.set(col.document(voucher.getCode()), voucher);
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (onDone != null) {
                                    onDone.run();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to seed vouchers", e);
                                if (onDone != null) {
                                    onDone.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check vouchers collection", e);
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    public static ListenerRegistration listenVouchers(@NonNull Consumer<List<VoucherEntity>> callback) {
        return FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_VOUCHERS)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Voucher snapshot listen error", e);
                        callback.accept(new ArrayList<>());
                        return;
                    }
                    List<VoucherEntity> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            VoucherEntity v = parseVoucher(doc);
                            if (v != null && !v.getCode().isEmpty()) {
                                list.add(v);
                            }
                        }
                    }
                    sortVouchers(list);
                    callback.accept(list);
                });
    }

    public static void getAllVouchers(@NonNull Consumer<List<VoucherEntity>> callback) {
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_VOUCHERS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<VoucherEntity> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        VoucherEntity v = parseVoucher(doc);
                        if (v != null && !v.getCode().isEmpty()) {
                            list.add(v);
                        }
                    }
                    sortVouchers(list);
                    callback.accept(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load vouchers", e);
                    callback.accept(new ArrayList<>());
                });
    }

    public static void findByCode(String rawCode, @NonNull Consumer<VoucherEntity> callback) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.US);
        if (code.isEmpty()) {
            callback.accept(null);
            return;
        }
        CollectionReference col = FirebaseFirestore.getInstance().collection(Constants.COLLECTION_VOUCHERS);
        col.document(code).get()
                .addOnSuccessListener(doc -> {
                    VoucherEntity direct = parseVoucher(doc);
                    if (direct != null) {
                        callback.accept(direct);
                        return;
                    }
                    col.whereEqualTo("code", code).limit(1).get()
                            .addOnSuccessListener(query -> {
                                if (query != null && !query.isEmpty()) {
                                    callback.accept(parseVoucher(query.getDocuments().get(0)));
                                } else {
                                    callback.accept(null);
                                }
                            })
                            .addOnFailureListener(e -> callback.accept(null));
                })
                .addOnFailureListener(e -> callback.accept(null));
    }

    @Nullable
    public static VoucherEntity parseVoucher(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        VoucherEntity v = new VoucherEntity();
        v.setDocId(doc.getId());
        String code = doc.getString("code");
        v.setCode((code == null || code.trim().isEmpty()) ? doc.getId() : code);
        v.setTitle(doc.getString("title"));
        v.setDescription(doc.getString("description"));
        v.setDiscountType(doc.getString("discountType"));
        v.setDiscountValue(longVal(doc.get("discountValue")));
        v.setMaxDiscount(longVal(doc.get("maxDiscount")));
        v.setMinOrderAmount(longVal(doc.get("minOrderAmount")));
        v.setUsageLimit((int) longVal(doc.get("usageLimit")));
        v.setUsedCount((int) longVal(doc.get("usedCount")));
        Boolean activeVal = doc.getBoolean("active");
        v.setActive(activeVal == null || activeVal);
        v.setExpiresAt(longVal(doc.get("expiresAt")));
        return v;
    }

    private static void sortVouchers(List<VoucherEntity> list) {
        long now = System.currentTimeMillis();
        list.sort((a, b) -> {
            boolean aAvail = a.isAvailable(now);
            boolean bAvail = b.isAvailable(now);
            if (aAvail != bAvail) {
                return aAvail ? -1 : 1;
            }
            return Long.compare(a.getMinOrderAmount(), b.getMinOrderAmount());
        });
    }

    private static long longVal(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static void setPendingVoucherCode(Context context, String code) {
        if (context == null) {
            return;
        }
        String clean = code == null ? "" : code.trim().toUpperCase(Locale.US);
        context.getApplicationContext()
                .getSharedPreferences(PREF_VOUCHER, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_CODE, clean)
                .apply();
    }

    public static String getPendingVoucherCode(Context context) {
        if (context == null) {
            return "";
        }
        String code = context.getApplicationContext()
                .getSharedPreferences(PREF_VOUCHER, Context.MODE_PRIVATE)
                .getString(KEY_PENDING_CODE, "");
        return code == null ? "" : code.trim().toUpperCase(Locale.US);
    }

    public static void clearPendingVoucherCode(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREF_VOUCHER, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_CODE)
                .apply();
    }
}
