package com.example.btday4.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.btday4.model.Voucher;
import com.example.btday4.util.Constants;
import com.example.btday4.util.ValidationUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 100% Cloud Firestore-backed repository for vouchers (`vouchers/{id}`) and per-user wallet state
 * (`users/{username}/user_vouchers/{code}`). All vouchers are authored and managed exclusively on Firebase.
 */
public final class VoucherRepository {

    private static final String TAG = "VoucherRepository";
    private static final String PREF_UI_SELECTION = "btday4_voucher_ui_selection";

    public enum ClaimResult {
        CLAIMED,
        ALREADY_CLAIMED,
        ALREADY_USED,
        EXPIRED,
        INACTIVE,
        OUT_OF_STOCK,
        NOT_FOUND,
        ERROR
    }

    public static final class ClaimOutcome {
        public final ClaimResult result;
        @Nullable
        public final Voucher voucher;

        public ClaimOutcome(ClaimResult result, @Nullable Voucher voucher) {
            this.result = result;
            this.voucher = voucher;
        }
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
            return claimed
                    && !used
                    && voucher.isActive()
                    && !voucher.isExpired(nowMillis)
                    && voucher.hasRemainingUses();
        }
    }

    private VoucherRepository() {
    }

    /**
     * Parses a Firestore voucher document defensively so manually authored Firebase Console docs
     * work even when optional fields (code, badgeLabel, active, usageLimit, expiryMillis) are omitted.
     */
    @Nullable
    public static Voucher parseVoucher(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        return parseVoucherMap(doc.getId(), doc.getData());
    }

    @Nullable
    public static Voucher parseVoucherMap(@Nullable String docId, @Nullable Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        String cleanDocId = docId == null ? "" : docId.trim();
        String rawCode = stringVal(data.get("code"));
        String code = rawCode.isEmpty() ? cleanDocId.toUpperCase(Locale.US) : rawCode.trim().toUpperCase(Locale.US);
        if (code.isEmpty()) {
            return null;
        }

        String title = stringVal(data.get("title"));
        String description = stringVal(data.get("description"));
        String discountType = stringVal(data.get("discountType"));
        long discountValue = longVal(data.get("discountValue"));

        long maxDiscount = longVal(data.get("maxDiscountAmount"));
        if (maxDiscount == 0L && data.containsKey("maxDiscount")) {
            maxDiscount = longVal(data.get("maxDiscount"));
        }

        long minOrder = longVal(data.get("minOrderAmount"));
        long expiry = longVal(data.get("expiryMillis"));
        if (expiry == 0L && data.containsKey("expiresAt")) {
            expiry = longVal(data.get("expiresAt"));
        }

        String badgeLabel = stringVal(data.get("badgeLabel"));
        boolean secretOnly = boolVal(data.get("secretOnly"), false);
        boolean active = boolVal(data.get("active"), true);
        int usageLimit = (int) longVal(data.get("usageLimit"));
        int usedCount = (int) longVal(data.get("usedCount"));

        Voucher voucher = new Voucher(
                code,
                title,
                description,
                discountType,
                discountValue,
                maxDiscount,
                minOrder,
                expiry,
                badgeLabel,
                secretOnly,
                active,
                usageLimit,
                usedCount
        );
        voucher.setDocId(cleanDocId.isEmpty() ? code : cleanDocId);
        return voucher;
    }

    /**
     * One-shot fetch combining live Firestore `vouchers` and `users/{username}/user_vouchers`.
     */
    public static void getVouchersForUser(
            Context context,
            String username,
            @NonNull Consumer<List<VoucherItem>> callback
    ) {
        String cleanUser = normalizeUser(username);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> vouchersTask = db.collection(Constants.COLLECTION_VOUCHERS).get();

        if (cleanUser.isEmpty()) {
            vouchersTask
                    .addOnSuccessListener(snap -> callback.accept(composeItems(
                            extractCatalog(snap),
                            new HashSet<>(),
                            new HashSet<>()
                    )))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load vouchers from Firestore", e);
                        callback.accept(new ArrayList<>());
                    });
            return;
        }

        Task<QuerySnapshot> userVouchersTask = userVouchersRef(db, cleanUser).get();
        Tasks.whenAllSuccess(vouchersTask, userVouchersTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot voucherSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot userSnap = (QuerySnapshot) results.get(1);
                    Set<String> claimed = new HashSet<>();
                    Set<String> used = new HashSet<>();
                    extractUserVoucherSets(userSnap, claimed, used);
                    callback.accept(composeItems(extractCatalog(voucherSnap), claimed, used));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load vouchers/user_vouchers from Firestore", e);
                    callback.accept(new ArrayList<>());
                });
    }

    /**
     * Real-time Firestore listener on both `vouchers` and `users/{username}/user_vouchers`.
     * Callers MUST call `remove()` on the returned ListenerRegistration in `onStop`/`onDestroyView`.
     */
    public static ListenerRegistration listenVouchersForUser(
            String username,
            @NonNull Consumer<List<VoucherItem>> callback
    ) {
        String cleanUser = normalizeUser(username);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Map<String, Voucher> latestCatalog = new LinkedHashMap<>();
        final Set<String> latestClaimed = new HashSet<>();
        final Set<String> latestUsed = new HashSet<>();
        final boolean[] catalogLoaded = {false};
        final boolean[] userLoaded = {cleanUser.isEmpty()};

        Runnable emitIfReady = () -> {
            if (catalogLoaded[0] && userLoaded[0]) {
                callback.accept(composeItems(latestCatalog, latestClaimed, latestUsed));
            }
        };

        ListenerRegistration catalogReg = db.collection(Constants.COLLECTION_VOUCHERS)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Voucher catalog listener error", e);
                        return;
                    }
                    latestCatalog.clear();
                    latestCatalog.putAll(extractCatalog(snap));
                    catalogLoaded[0] = true;
                    emitIfReady.run();
                });

        ListenerRegistration userReg = null;
        if (!cleanUser.isEmpty()) {
            userReg = userVouchersRef(db, cleanUser)
                    .addSnapshotListener((snap, e) -> {
                        if (e != null) {
                            Log.e(TAG, "User vouchers listener error", e);
                            return;
                        }
                        latestClaimed.clear();
                        latestUsed.clear();
                        extractUserVoucherSets(snap, latestClaimed, latestUsed);
                        userLoaded[0] = true;
                        emitIfReady.run();
                    });
        }

        final ListenerRegistration finalUserReg = userReg;
        return () -> {
            catalogReg.remove();
            if (finalUserReg != null) {
                finalUserReg.remove();
            }
        };
    }

    public static void countAvailableForUser(
            Context context,
            String username,
            @NonNull Consumer<Integer> callback
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

    /**
     * Resolves a voucher exclusively from Cloud Firestore `vouchers` (by doc ID or `code` field).
     */
    public static void findByCode(@Nullable String rawCode, @NonNull Consumer<Voucher> callback) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            callback.accept(null);
            return;
        }
        String code = rawCode.trim().toUpperCase(Locale.US);
        CollectionReference col = FirebaseFirestore.getInstance().collection(Constants.COLLECTION_VOUCHERS);
        col.document(code).get()
                .addOnSuccessListener(doc -> {
                    Voucher direct = parseVoucher(doc);
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
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to query voucher by code field", e);
                                callback.accept(null);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch voucher doc", e);
                    callback.accept(null);
                });
    }

    /**
     * Claims a voucher exclusively against Cloud Firestore `vouchers` and `users/{username}/user_vouchers`.
     */
    public static void claimByCode(
            Context context,
            String username,
            String rawCode,
            @NonNull Consumer<ClaimOutcome> callback
    ) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            callback.accept(new ClaimOutcome(ClaimResult.NOT_FOUND, null));
            return;
        }
        String cleanUser = normalizeUser(username);
        if (cleanUser.isEmpty()) {
            callback.accept(new ClaimOutcome(ClaimResult.ERROR, null));
            return;
        }

        findByCode(rawCode, matched -> {
            if (matched == null) {
                callback.accept(new ClaimOutcome(ClaimResult.NOT_FOUND, null));
                return;
            }
            long now = System.currentTimeMillis();
            if (!matched.isActive()) {
                callback.accept(new ClaimOutcome(ClaimResult.INACTIVE, matched));
                return;
            }
            if (matched.isExpired(now)) {
                callback.accept(new ClaimOutcome(ClaimResult.EXPIRED, matched));
                return;
            }
            if (!matched.hasRemainingUses()) {
                callback.accept(new ClaimOutcome(ClaimResult.OUT_OF_STOCK, matched));
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userVoucherDoc = userVouchersRef(db, cleanUser).document(matched.getCode());
            userVoucherDoc.get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && snap.exists()) {
                            if (Boolean.TRUE.equals(snap.getBoolean("used"))) {
                                callback.accept(new ClaimOutcome(ClaimResult.ALREADY_USED, matched));
                                return;
                            }
                            if (Boolean.TRUE.equals(snap.getBoolean("claimed"))) {
                                callback.accept(new ClaimOutcome(ClaimResult.ALREADY_CLAIMED, matched));
                                return;
                            }
                        }
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("code", matched.getCode());
                        payload.put("docId", matched.getDocId());
                        payload.put("claimed", true);
                        payload.put("used", false);
                        payload.put("updatedAt", System.currentTimeMillis());

                        userVoucherDoc.set(payload, SetOptions.merge())
                                .addOnSuccessListener(unused ->
                                        callback.accept(new ClaimOutcome(ClaimResult.CLAIMED, matched)))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to claim voucher in Firestore", e);
                                    callback.accept(new ClaimOutcome(ClaimResult.ERROR, matched));
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check user voucher status", e);
                        callback.accept(new ClaimOutcome(ClaimResult.ERROR, matched));
                    });
        });
    }

    public static void restoreVoucherIfCancelled(Context context, String username, @Nullable String rawCode) {
        if (!ValidationUtils.isNotBlank(rawCode)) {
            return;
        }
        String cleanUser = normalizeUser(username);
        if (cleanUser.isEmpty()) {
            return;
        }
        String code = rawCode.trim().toUpperCase(Locale.US);
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("claimed", true);
        payload.put("used", false);
        payload.put("updatedAt", System.currentTimeMillis());
        userVouchersRef(FirebaseFirestore.getInstance(), cleanUser)
                .document(code)
                .set(payload, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to restore voucher", e));
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
        String userKey = normalizeUser(username);
        return context.getApplicationContext()
                .getSharedPreferences(PREF_UI_SELECTION, Context.MODE_PRIVATE)
                .getString(Constants.KEY_SELECTED_CART_VOUCHER + "_" + userKey, "");
    }

    public static void setSelectedCartVoucherCode(Context context, String username, @Nullable String code) {
        if (context == null) {
            return;
        }
        String userKey = normalizeUser(username);
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.US);
        context.getApplicationContext()
                .getSharedPreferences(PREF_UI_SELECTION, Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.KEY_SELECTED_CART_VOUCHER + "_" + userKey, normalized)
                .apply();
    }

    private static Map<String, Voucher> extractCatalog(@Nullable QuerySnapshot snapshot) {
        Map<String, Voucher> catalog = new LinkedHashMap<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Voucher v = parseVoucher(doc);
                if (v != null && !v.getCode().isEmpty()) {
                    catalog.put(v.getCode(), v);
                }
            }
        }
        return catalog;
    }

    private static void extractUserVoucherSets(
            @Nullable QuerySnapshot userSnap,
            Set<String> claimedOut,
            Set<String> usedOut
    ) {
        if (userSnap == null) {
            return;
        }
        for (DocumentSnapshot doc : userSnap.getDocuments()) {
            String rawCode = doc.getString("code");
            String code = (rawCode == null || rawCode.trim().isEmpty())
                    ? doc.getId().trim().toUpperCase(Locale.US)
                    : rawCode.trim().toUpperCase(Locale.US);
            if (code.isEmpty()) {
                continue;
            }
            if (Boolean.TRUE.equals(doc.getBoolean("claimed"))) {
                claimedOut.add(code);
            }
            if (Boolean.TRUE.equals(doc.getBoolean("used"))) {
                usedOut.add(code);
            }
        }
    }

    private static List<VoucherItem> composeItems(
            Map<String, Voucher> catalog,
            Set<String> claimed,
            Set<String> used
    ) {
        long now = System.currentTimeMillis();
        List<VoucherItem> list = new ArrayList<>();
        for (Voucher v : catalog.values()) {
            boolean isClaimed = claimed.contains(v.getCode());
            boolean isUsed = used.contains(v.getCode());
            if (!v.isActive() && !isUsed) {
                continue;
            }
            if (v.isSecretOnly() && !isClaimed && !isUsed) {
                continue;
            }
            list.add(new VoucherItem(v, isClaimed, isUsed));
        }
        list.sort((a, b) -> {
            boolean aReady = !a.used && !a.voucher.isExpired(now) && a.voucher.hasRemainingUses();
            boolean bReady = !b.used && !b.voucher.isExpired(now) && b.voucher.hasRemainingUses();
            if (aReady != bReady) {
                return aReady ? -1 : 1;
            }
            if (a.claimed != b.claimed) {
                return a.claimed ? -1 : 1;
            }
            return Long.compare(a.voucher.getMinOrderAmount(), b.voucher.getMinOrderAmount());
        });
        return list;
    }

    private static CollectionReference userVouchersRef(FirebaseFirestore db, String username) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(username)
                .collection(Constants.COLLECTION_USER_VOUCHERS);
    }

    private static String normalizeUser(@Nullable String username) {
        if (username == null) {
            return "";
        }
        String clean = username.trim();
        return "guest".equalsIgnoreCase(clean) ? "" : clean;
    }

    private static String stringVal(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static long longVal(@Nullable Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static boolean boolVal(@Nullable Object value, boolean defaultVal) {
        return value instanceof Boolean ? (Boolean) value : defaultVal;
    }
}
