package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.CartDao;
import com.example.btday4.data.local.CartItemEntity;
import com.example.btday4.model.CartItem;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages shopping cart items persisted in Room SQLite.
 */
public final class CartRepository {

    private CartRepository() {
    }

    public static void getItems(Context context, Consumer<List<CartItem>> callback) {
        AppDatabase.runAsync(context, db -> {
            List<CartDao.CartRow> rows = db.cartDao().getAllWithProduct();
            List<CartItem> items = new ArrayList<>();
            for (CartDao.CartRow row : rows) {
                if (row.product != null && row.entry != null && row.entry.getQuantity() > 0) {
                    items.add(new CartItem(row.product, row.entry.getQuantity()));
                }
            }
            AppDatabase.postToMain(() -> callback.accept(items));
        });
    }

    public static void addItem(Context context, String productId, int quantity, Runnable onDone) {
        if (productId == null || quantity <= 0) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        AppDatabase.runAsync(context, db -> {
            CartDao dao = db.cartDao();
            CartItemEntity existing = dao.findByProductId(productId);
            int updatedQty = existing == null
                    ? Math.min(Constants.MAX_CART_QUANTITY, quantity)
                    : Math.min(Constants.MAX_CART_QUANTITY, existing.getQuantity() + quantity);
            dao.upsert(new CartItemEntity(productId, updatedQty, System.currentTimeMillis()));
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public static void updateQuantity(Context context, String productId, int newQuantity, Runnable onDone) {
        if (productId == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        AppDatabase.runAsync(context, db -> {
            CartDao dao = db.cartDao();
            if (newQuantity <= 0) {
                dao.deleteByProductId(productId);
            } else {
                dao.upsert(new CartItemEntity(
                        productId,
                        Math.min(Constants.MAX_CART_QUANTITY, newQuantity),
                        System.currentTimeMillis()
                ));
            }
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public static void removeItem(Context context, String productId, Runnable onDone) {
        updateQuantity(context, productId, 0, onDone);
    }

    public static void clearCart(Context context, Runnable onDone) {
        AppDatabase.runAsync(context, db -> {
            db.cartDao().clearAll();
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public static void getTotalQuantity(Context context, Consumer<Integer> callback) {
        AppDatabase.runAsync(context, db -> {
            int total = db.cartDao().getTotalQuantity();
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
}
