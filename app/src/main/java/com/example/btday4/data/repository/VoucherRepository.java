package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.model.Voucher;
import com.example.btday4.util.Constants;
import com.example.btday4.util.ValidationUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages promotional vouchers, user wallet claims, manual promo codes, and redemption state
 * with instant local responsiveness and Cloud Firestore synchronization.
 */
public final class VoucherRepository {

    private static final String PREF_VOUCHERS = "btday4_voucher_wallet";
    private static final long NINETY_DAYS_MS = 90L * 24L * 60L * 60L * 1_000L;

    private static final Set<String> STARTER_CLAIMED_CODES = new HashSet<>(Arrays.asList(
            "TECHNEW15",
            "FREESHIP"
    ));

    public enum ClaimResult {
        CLAIMED,
        ALREADY_CLAIMED,
        ALREADY_USED,
        EXPIRED,
        NOT_FOUND
    }

    public static final class VoucherItem {
        public final Voucher voucher;
        public final boolean claimed;
        public final boolean used;

        public VoucherItem(Voucher voucher, boolean claimed, boolean used) {
            this.voucher = voucher;
            this.claimed = claimed;
            this.used = used;
        }

        public boolean isAvailableToUse(long nowMillis) {
            return claimed && !used && !voucher.isExpired(nowMillis);
        }
    }

    private VoucherRepository() {
    }

    public static List<Voucher> createDefaultVouchers() {
        // Fixed future milestone (end of 2027) so seeded vouchers stay deterministic across devices.
        long expiry = System.currentTimeMillis() + NINETY_DAYS_MS;
        List<Voucher> list = new ArrayList<>();

        list.add(new Voucher(
                "TECHNEW15",
                "Giảm 15% đơn công nghệ",
                "Giảm 15% tối đa 25.000 ₫ cho đơn từ 50.000 ₫",
                Voucher.TYPE_PERCENT,
                15L,
                25_000L,
                50_000L,
                expiry,
                "-15%",
                false
        ));

        list.add(new Voucher(
                "FLASH5K",
                "Ưu đãi phụ kiện giảm 5K",
                "Giảm ngay 5.000 ₫ cho mọi đơn từ 20.000 ₫",
                Voucher.TYPE_FIXED,
                5_000L,
                0L,
                20_000L,
                expiry,
                "-5K",
                false
        ));

        list.add(new Voucher(
                "GIAM10K",
                "Giảm trực tiếp 10.000 ₫",
                "Áp dụng tất cả sản phẩm cho đơn từ 40.000 ₫",
                Voucher.TYPE_FIXED,
                10_000L,
                0L,
                40_000L,
                expiry,
                "-10K",
                false
        ));

        list.add(new Voucher(
                "APPLE30K",
                "Đặc quyền Flagship giảm 30K",
                "Giảm 30.000 ₫ cho đơn Điện thoại, Laptop, Tablet từ 180.000 ₫",
                Voucher.TYPE_FIXED,
                30_000L,
                0L,
                180_000L,
                expiry,
                "-30K",
                false
        ));

        list.add(new Voucher(
                "SIEUSALE50K",
                "Siêu Sale đơn lớn giảm 50K",
                "Giảm sâu 50.000 ₫ cho đơn hàng từ 300.000 ₫",
                Voucher.TYPE_FIXED,
                50_000L,
                0L,
                300_000L,
                expiry,
                "-50K",
                false
        ));

        list.add(new Voucher(
                "FREESHIP",
                "Miễn phí vận chuyển toàn quốc",
                "Hỗ trợ 100% phí giao hàng cho đơn từ 20.000 ₫",
                Voucher.TYPE_FREESHIP,
                100L,
                0L,
                20_000L,
                expiry,
                "FREE\nSHIP",
                false
        ));

        list.add(new Voucher(
                "VIP20",
                "Đặc quyền VIP giảm 20%",
                "Giảm 20% tối đa 40.000 ₫ cho đơn từ 100.000 ₫",
                Voucher.TYPE_PERCENT,
                20L,
                40_000L,
                100_000L,
                expiry,
                "-20%",
                true
        ));

        return list;
    }

    public static void seedVouchersIfEmpty() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference col = db.collection(Constants.COLLECTION_VOUCHERS);
            col.limit(1).get().addOnSuccessListener(snapshot -> {
                if (snapshot != null && !snapshot.isEmpty()) {
                    return;
                }
                WriteBatch batch = db.batch();
                for (Voucher v : createDefaultVouchers()) {
                    batch.set(col.document(v.getCode()), v);
                }
                batch.commit();
            });
        } catch (Exception ignored) {
            // Safe offline fallback when Firebase is unavailable in unit tests.
        }
    }

    public static void getVouchersForUser(
            Context context,
            String username,
            Consumer<List<VoucherItem>> callback
    ) {
        String cleanUser = normalizeUser(username);
        ensureStarterVouchers(context, cleanUser);

        Map<String, Voucher> catalog = buildDefaultCatalogMap();
        Set<String> localClaimed = getClaimedSet(context, cleanUser);
        Set<String> localUsed = getUsedSet(context, cleanUser);

        // Emit immediate local snapshot first so UI never flickers or stalls, then merge remote state if needed.
        callback.accept(composeItems(catalog, localClaimed, localUsed));

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Constants.COLLECTION_VOUCHERS).get().addOnSuccessListener(voucherSnap -> {
                if (voucherSnap != null && !voucherSnap.isEmpty()) {
                    for (DocumentSnapshot doc : voucherSnap.getDocuments()) {
                        Voucher remote = doc.toObject(Voucher.class);
                        if (remote != null && ValidationUtils.isNotBlank(remote.getCode())) {
                            catalog.put(remote.getCode().toUpperCase(Locale.US), remote);
                        }
                    }
                }
                if (cleanUser.isEmpty()) {
                    callback.accept(composeItems(catalog, localClaimed, localUsed));
                    return;
                }
                db.collection(Constants.COLLECTION_USERS)
                        .document(cleanUser)
                        .collection(Constants.COLLECTION_USER_VOUCHERS)
                        .get()
                        .addOnSuccessListener(userSnap -> {
                            boolean changed = false;
                            if (userSnap != null) {
                                for (DocumentSnapshot doc : userSnap.getDocuments()) {
                                    String code = doc.getId().trim().toUpperCase(Locale.US);
                                    Boolean claimed = doc.getBoolean("claimed");
                                    Boolean used = doc.getBoolean("used");
                                    if (Boolean.TRUE.equals(claimed) && localClaimed.add(code)) {
                                        changed = true;
                                    }
                                    if (Boolean.TRUE.equals(used) && localUsed.add(code)) {
                                        changed = true;
                                    }
                                }
                            }
                            if (changed) {
                                saveClaimedSet(context, cleanUser, localClaimed);
                                saveUsedSet(context, cleanUser, localUsed);
                            }
                            callback.accept(composeItems(catalog, localClaimed, localUsed));
                        });
            });
        } catch (Exception ignored) {
        }
    }

    public static void countAvailableForUser(
            Context context,
            String username,
            Consumer<Integer> callback
    ) {
        getVouchersForUser(context, username, items -> {
            long now = System.currentTimeMillis();
            int count = 0;
            for (VoucherItem item : items) {
                if (item.isAvailableToUse(now)) {
                    count++;
                }
            }
            callback.accept(count);
        });
    }

    @Nullable
    public static Voucher findByCode(@Nullable String rawCode) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            return null;
        }
        String code = rawCode.trim().toUpperCase(Locale.US);
        return buildDefaultCatalogMap().get(code);
    }

    public static ClaimResult claimByCode(Context context, String username, String rawCode) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            return ClaimResult.NOT_FOUND;
        }
        String cleanUser = normalizeUser(username);
        ensureStarterVouchers(context, cleanUser);

        String code = rawCode.trim().toUpperCase(Locale.US);
        Voucher matched = buildDefaultCatalogMap().get(code);
        if (matched == null) {
            return ClaimResult.NOT_FOUND;
        }
        if (matched.isExpired(System.currentTimeMillis())) {
            return ClaimResult.EXPIRED;
        }
        Set<String> used = getUsedSet(context, cleanUser);
        if (used.contains(code)) {
            return ClaimResult.ALREADY_USED;
        }
        Set<String> claimed = getClaimedSet(context, cleanUser);
        if (claimed.contains(code)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        claimed.add(code);
        saveClaimedSet(context, cleanUser, claimed);
        syncUserVoucherToFirestore(cleanUser, code, true, false);
        return ClaimResult.CLAIMED;
    }

    public static void markVoucherUsed(Context context, String username, @Nullable String rawCode) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            return;
        }
        String cleanUser = normalizeUser(username);
        String code = rawCode.trim().toUpperCase(Locale.US);
        Set<String> claimed = getClaimedSet(context, cleanUser);
        Set<String> used = getUsedSet(context, cleanUser);
        claimed.add(code);
        used.add(code);
        saveClaimedSet(context, cleanUser, claimed);
        saveUsedSet(context, cleanUser, used);

        if (code.equalsIgnoreCase(getSelectedCartVoucherCode(context, cleanUser))) {
            setSelectedCartVoucherCode(context, cleanUser, "");
        }
        syncUserVoucherToFirestore(cleanUser, code, true, true);
    }

    public static void restoreVoucherIfCancelled(Context context, String username, @Nullable String rawCode) {
        if (!ValidationUtils.isNotBlank(rawCode) || context == null) {
            return;
        }
        String cleanUser = normalizeUser(username);
        String code = rawCode.trim().toUpperCase(Locale.US);
        Set<String> used = getUsedSet(context, cleanUser);
        if (used.remove(code)) {
            saveUsedSet(context, cleanUser, used);
            syncUserVoucherToFirestore(cleanUser, code, true, false);
        }
    }

    @Nullable
    public static Voucher findBestApplicable(
            List<VoucherItem> items,
            long subtotal,
            long baseShippingFee,
            long nowMillis
    ) {
        if (items == null || items.isEmpty() || subtotal <= 0L) {
            return null;
        }
        Voucher best = null;
        long bestDiscount = 0L;
        for (VoucherItem item : items) {
            if (!item.isAvailableToUse(nowMillis)) {
                continue;
            }
            long discount = item.voucher.computeDiscount(subtotal, baseShippingFee);
            if (discount > bestDiscount) {
                bestDiscount = discount;
                best = item.voucher;
            }
        }
        return best;
    }

    @NonNull
    public static String getSelectedCartVoucherCode(Context context, String username) {
        if (context == null) {
            return "";
        }
        SharedPreferences prefs = prefs(context);
        return prefs.getString(Constants.KEY_SELECTED_CART_VOUCHER + "_" + normalizeUser(username), "");
    }

    public static void setSelectedCartVoucherCode(Context context, String username, @Nullable String code) {
        if (context == null) {
            return;
        }
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.US);
        prefs(context).edit()
                .putString(Constants.KEY_SELECTED_CART_VOUCHER + "_" + normalizeUser(username), normalized)
                .apply();
    }

    private static List<VoucherItem> composeItems(
            Map<String, Voucher> catalog,
            Set<String> claimed,
            Set<String> used
    ) {
        List<VoucherItem> list = new ArrayList<>();
        for (Voucher v : catalog.values()) {
            boolean isClaimed = claimed.contains(v.getCode());
            boolean isUsed = used.contains(v.getCode());
            if (v.isSecretOnly() && !isClaimed && !isUsed) {
                continue;
            }
            list.add(new VoucherItem(v, isClaimed, isUsed));
        }
        return list;
    }

    private static Map<String, Voucher> buildDefaultCatalogMap() {
        Map<String, Voucher> map = new LinkedHashMap<>();
        for (Voucher v : createDefaultVouchers()) {
            map.put(v.getCode(), v);
        }
        return map;
    }

    private static void ensureStarterVouchers(Context context, String cleanUser) {
        if (context == null) {
            return;
        }
        SharedPreferences p = prefs(context);
        String initKey = "starter_seeded_" + cleanUser;
        if (p.getBoolean(initKey, false)) {
            return;
        }
        Set<String> claimed = getClaimedSet(context, cleanUser);
        claimed.addAll(STARTER_CLAIMED_CODES);
        saveClaimedSet(context, cleanUser, claimed);
        p.edit().putBoolean(initKey, true).apply();
        for (String code : STARTER_CLAIMED_CODES) {
            syncUserVoucherToFirestore(cleanUser, code, true, false);
        }
    }

    private static Set<String> getClaimedSet(Context context, String cleanUser) {
        if (context == null) {
            return new HashSet<>(STARTER_CLAIMED_CODES);
        }
        Set<String> stored = prefs(context).getStringSet("claimed_" + cleanUser, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    private static void saveClaimedSet(Context context, String cleanUser, Set<String> codes) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putStringSet("claimed_" + cleanUser, new HashSet<>(codes)).apply();
    }

    private static Set<String> getUsedSet(Context context, String cleanUser) {
        if (context == null) {
            return new HashSet<>();
        }
        Set<String> stored = prefs(context).getStringSet("used_" + cleanUser, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    private static void saveUsedSet(Context context, String cleanUser, Set<String> codes) {
        if (context == null) {
            return;
        }
        prefs(context).edit().putStringSet("used_" + cleanUser, new HashSet<>(codes)).apply();
    }

    private static void syncUserVoucherToFirestore(
            String cleanUser,
            String code,
            boolean claimed,
            boolean used
    ) {
        if (cleanUser.isEmpty() || "guest".equals(cleanUser)) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("code", code);
            payload.put("claimed", claimed);
            payload.put("used", used);
            payload.put("updatedAt", System.currentTimeMillis());
            FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_USERS)
                    .document(cleanUser)
                    .collection(Constants.COLLECTION_USER_VOUCHERS)
                    .document(code)
                    .set(payload, SetOptions.merge());
        } catch (Exception ignored) {
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_VOUCHERS, Context.MODE_PRIVATE);
    }

    private static String normalizeUser(String username) {
        return username == null || username.trim().isEmpty()
                ? "guest"
                : username.trim().toLowerCase(Locale.US);
    }
}
