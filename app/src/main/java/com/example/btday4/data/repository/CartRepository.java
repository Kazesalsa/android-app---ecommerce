package com.example.btday4.data.repository;

import android.content.Context;
import android.util.Log;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.model.CartItem;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages the signed-in account's shopping cart in Cloud Firestore (users/{username}/cart/{productId}),
 * capping every line at the product's available stock.
 */
public final class CartRepository {

    private static final String TAG = "CartRepository";

    private CartRepository() {
    }

    /**
     * Highest quantity a shopper may hold for a product: limited by both stock and the cart cap.
     */
    public static int maxPurchasable(Product product) {
        return product == null ? 0 : Math.min(Constants.MAX_CART_QUANTITY, Math.max(0, product.getStock()));
    }

    public static void getItems(Context context, Consumer<List<CartItem>> callback) {
        String username = currentUser(context);
        if (username.isEmpty()) {
            callback.accept(new ArrayList<>());
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> cartTask = cartRef(db, username).get();
        Task<QuerySnapshot> productsTask = db.collection(Constants.COLLECTION_PRODUCTS).get();

        Tasks.whenAllSuccess(cartTask, productsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot cartSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot productSnap = (QuerySnapshot) results.get(1);

                    Map<String, Product> productMap = new HashMap<>();
                    for (DocumentSnapshot doc : productSnap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            if (p.getId().isEmpty()) {
                                p.setId(doc.getId());
                            }
                            productMap.put(p.getId(), p);
                        }
                    }

                    List<DocumentSnapshot> cartDocs = new ArrayList<>(cartSnap.getDocuments());
                    cartDocs.sort((a, b) -> Long.compare(longField(b, "updatedAt"), longField(a, "updatedAt")));

                    List<CartItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : cartDocs) {
                        int qty = (int) longField(doc, "quantity");
                        String productId = doc.getString("productId");
                        if (productId == null || productId.isEmpty()) {
                            productId = doc.getId();
                        }
                        Product product = productMap.get(productId);
                        if (product != null && qty > 0) {
                            items.add(new CartItem(product, qty));
                        }
                    }
                    callback.accept(items);
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
    }

    /**
     * Adds up to the remaining purchasable quantity and reports how many units were actually added.
     * Uses collection get (safe when offline & empty) and optimistic local cache write so the UI
     * never hangs waiting for server ACK while Firestore syncs in the background.
     */
    public static void addItem(Context context, String productId, int quantity, Consumer<Integer> onAdded) {
        String username = currentUser(context);
        if (username.isEmpty() || productId == null || productId.trim().isEmpty() || quantity <= 0) {
            if (onAdded != null) {
                onAdded.accept(0);
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference productRef = db.collection(Constants.COLLECTION_PRODUCTS).document(productId);
        CollectionReference userCartRef = cartRef(db, username);
        DocumentReference itemRef = userCartRef.document(productId);

        Tasks.whenAllSuccess(productRef.get(), userCartRef.get())
                .addOnSuccessListener(results -> {
                    DocumentSnapshot productDoc = (DocumentSnapshot) results.get(0);
                    QuerySnapshot cartSnap = (QuerySnapshot) results.get(1);
                    Product product = productDoc.exists() ? productDoc.toObject(Product.class) : null;
                    int current = 0;
                    for (DocumentSnapshot doc : cartSnap.getDocuments()) {
                        if (productId.equals(doc.getId()) || productId.equals(doc.getString("productId"))) {
                            current = (int) longField(doc, "quantity");
                            break;
                        }
                    }
                    int target = Math.min(current + quantity, maxPurchasable(product));
                    if (target <= current) {
                        if (onAdded != null) {
                            onAdded.accept(0);
                        }
                        return;
                    }
                    int added = target - current;
                    // Firestore applies set() to local cache immediately and syncs to server when online;
                    // don't block UI callback on server ACK (which hangs indefinitely when offline).
                    itemRef.set(cartPayload(productId, target))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to sync cart item " + productId, e));
                    if (onAdded != null) {
                        onAdded.accept(added);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to read product/cart for " + productId, e);
                    if (onAdded != null) {
                        onAdded.accept(0);
                    }
                });
    }

    /**
     * Re-adds every line of a past order ("Mua lại") in one Firestore transaction; reports total units added.
     */
    public static void addItems(Context context, List<OrderItemEntity> items, Consumer<Integer> onAdded) {
        String username = currentUser(context);
        if (username.isEmpty() || items == null || items.isEmpty()) {
            if (onAdded != null) {
                onAdded.accept(0);
            }
            return;
        }
        Map<String, Integer> requestedByProduct = new LinkedHashMap<>();
        for (OrderItemEntity item : items) {
            if (item == null || item.getProductId().isEmpty() || item.getQuantity() <= 0) {
                continue;
            }
            requestedByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        if (requestedByProduct.isEmpty()) {
            if (onAdded != null) {
                onAdded.accept(0);
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            // Firestore requires all reads before any writes inside a transaction.
            Map<String, Product> products = new HashMap<>();
            Map<String, Integer> currentQty = new HashMap<>();
            for (String productId : requestedByProduct.keySet()) {
                DocumentSnapshot pDoc = transaction.get(
                        db.collection(Constants.COLLECTION_PRODUCTS).document(productId));
                DocumentSnapshot cDoc = transaction.get(cartRef(db, username).document(productId));
                if (pDoc.exists()) {
                    products.put(productId, pDoc.toObject(Product.class));
                }
                currentQty.put(productId, cDoc.exists() ? (int) longField(cDoc, "quantity") : 0);
            }

            int totalAdded = 0;
            for (Map.Entry<String, Integer> entry : requestedByProduct.entrySet()) {
                String productId = entry.getKey();
                int current = currentQty.getOrDefault(productId, 0);
                int target = Math.min(current + entry.getValue(), maxPurchasable(products.get(productId)));
                if (target > current) {
                    transaction.set(cartRef(db, username).document(productId), cartPayload(productId, target));
                    totalAdded += (target - current);
                }
            }
            return totalAdded;
        }).addOnSuccessListener(total -> {
            if (onAdded != null) {
                onAdded.accept(total == null ? 0 : total);
            }
        }).addOnFailureListener(e -> {
            if (onAdded != null) {
                onAdded.accept(0);
            }
        });
    }

    public static void updateQuantity(Context context, String productId, int newQuantity, Runnable onDone) {
        String username = currentUser(context);
        if (username.isEmpty() || productId == null || productId.trim().isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference itemRef = cartRef(db, username).document(productId);
        if (newQuantity <= 0) {
            itemRef.delete().addOnFailureListener(e -> Log.e(TAG, "Failed to delete cart item " + productId, e));
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        db.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(productDoc -> {
                    Product product = productDoc.exists() ? productDoc.toObject(Product.class) : null;
                    int capped = Math.min(newQuantity, maxPurchasable(product));
                    if (capped <= 0) {
                        itemRef.delete().addOnFailureListener(e -> Log.e(TAG, "Failed to delete cart item " + productId, e));
                    } else {
                        itemRef.set(cartPayload(productId, capped))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync cart item " + productId, e));
                    }
                    if (onDone != null) {
                        onDone.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    public static void removeItem(Context context, String productId, Runnable onDone) {
        updateQuantity(context, productId, 0, onDone);
    }

    public static void getTotalQuantity(Context context, Consumer<Integer> callback) {
        String username = currentUser(context);
        if (username.isEmpty()) {
            callback.accept(0);
            return;
        }
        cartRef(FirebaseFirestore.getInstance(), username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        total += (int) longField(doc, "quantity");
                    }
                    callback.accept(total);
                })
                .addOnFailureListener(e -> callback.accept(0));
    }

    public static long calculateSubtotal(List<CartItem> items) {
        if (items == null) {
            return 0L;
        }
        long total = 0L;
        for (CartItem item : items) {
            total += item.getSubtotalAmount();
        }
        return total;
    }

    public static String getGrandTotalText(List<CartItem> items) {
        return FormatUtils.formatVnd(calculateSubtotal(items));
    }

    private static CollectionReference cartRef(FirebaseFirestore db, String username) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(username)
                .collection(Constants.COLLECTION_CART);
    }

    private static Map<String, Object> cartPayload(String productId, int quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", productId);
        map.put("quantity", quantity);
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    private static long longField(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value == null ? 0L : value;
    }

    private static String currentUser(Context context) {
        return new UserSession(context).getUsername();
    }
}
