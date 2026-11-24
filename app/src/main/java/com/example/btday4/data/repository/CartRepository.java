package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.CartDao;
import com.example.btday4.data.local.CartItemEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.model.CartItem;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the signed-in account's shopping cart persisted in Room SQLite, capping every line
 * at the product's available stock.
 */
public final class CartRepository {

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
        AppDatabase.runAsync(context, db -> {
            List<CartItem> items = new ArrayList<>();
            for (CartDao.CartRow row : db.cartDao().getAllWithProduct(username)) {
                if (row.product != null && row.entry != null && row.entry.getQuantity() > 0) {
                    items.add(new CartItem(row.product, row.entry.getQuantity()));
                }
            }
            AppDatabase.postToMain(() -> callback.accept(items));
        });
    }

    /**
     * Adds up to the remaining purchasable quantity and reports how many units were actually added
     * (0 when the product is sold out or the cart already holds the maximum).
     */
    public static void addItem(Context context, String productId, int quantity, Consumer<Integer> onAdded) {
        String username = currentUser(context);
        AppDatabase.runAsync(context, db -> {
            int added = addWithinStock(db, username, productId, quantity);
            if (onAdded != null) {
                AppDatabase.postToMain(() -> onAdded.accept(added));
            }
        });
    }

    /**
     * Re-adds every line of a past order ("Mua lại") in one transaction; reports total units added.
     */
    public static void addItems(Context context, List<OrderItemEntity> items, Consumer<Integer> onAdded) {
        String username = currentUser(context);
        AppDatabase.runAsync(context, db -> {
            int[] total = {0};
            db.runInTransaction(() -> {
                for (OrderItemEntity item : items) {
                    total[0] += addWithinStock(db, username, item.getProductId(), item.getQuantity());
                }
            });
            AppDatabase.postToMain(() -> onAdded.accept(total[0]));
        });
    }

    public static void updateQuantity(Context context, String productId, int newQuantity, Runnable onDone) {
        String username = currentUser(context);
        AppDatabase.runAsync(context, db -> {
            if (!username.isEmpty()) {
                int capped = Math.min(newQuantity, maxPurchasable(db.productDao().findById(productId)));
                if (capped <= 0) {
                    db.cartDao().delete(username, productId);
                } else {
                    db.cartDao().upsert(new CartItemEntity(username, productId, capped, System.currentTimeMillis()));
                }
            }
            AppDatabase.postToMain(onDone);
        });
    }

    public static void removeItem(Context context, String productId, Runnable onDone) {
        updateQuantity(context, productId, 0, onDone);
    }

    public static void getTotalQuantity(Context context, Consumer<Integer> callback) {
        String username = currentUser(context);
        AppDatabase.runAsync(context, db -> {
            int total = db.cartDao().getTotalQuantity(username);
            AppDatabase.postToMain(() -> callback.accept(total));
        });
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

    private static int addWithinStock(AppDatabase db, String username, String productId, int quantity) {
        if (username.isEmpty() || productId == null || quantity <= 0) {
            return 0;
        }
        CartItemEntity existing = db.cartDao().find(username, productId);
        int current = existing == null ? 0 : existing.getQuantity();
        int target = Math.min(current + quantity, maxPurchasable(db.productDao().findById(productId)));
        if (target <= current) {
            return 0;
        }
        db.cartDao().upsert(new CartItemEntity(username, productId, target, System.currentTimeMillis()));
        return target - current;
    }

    private static String currentUser(Context context) {
        return new UserSession(context).getUsername();
    }
}
