package com.example.btday4.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.ReviewRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room database singleton managing products, cart items, users, reviews, and orders.
 */
@Database(
        entities = {
                Product.class,
                CartItemEntity.class,
                UserEntity.class,
                ReviewEntity.class,
                OrderEntity.class,
                OrderItemEntity.class,
                PaymentEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    private static boolean seeded = false;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE products ADD COLUMN image_url TEXT");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `products_new` ("
                            + "`id` TEXT NOT NULL, "
                            + "`name` TEXT, "
                            + "`price` TEXT, "
                            + "`original_price` TEXT, "
                            + "`short_info` TEXT, "
                            + "`category` TEXT, "
                            + "`image_url` TEXT, "
                            + "PRIMARY KEY(`id`))"
            );
            db.execSQL(
                    "INSERT INTO `products_new` (`id`, `name`, `price`, `original_price`, `short_info`, `category`, `image_url`) "
                            + "SELECT `id`, `name`, `price`, `original_price`, `short_info`, `category`, `image_url` FROM `products`"
            );
            db.execSQL("DROP TABLE `products`");
            db.execSQL("ALTER TABLE `products_new` RENAME TO `products`");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT");
            db.execSQL("ALTER TABLE users ADD COLUMN address TEXT");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE products ADD COLUMN brand TEXT");
            db.execSQL("ALTER TABLE products ADD COLUMN description TEXT");
            db.execSQL("ALTER TABLE products ADD COLUMN specs TEXT");
            db.execSQL("ALTER TABLE products ADD COLUMN warranty TEXT");
            db.execSQL("ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 15");

            db.execSQL("ALTER TABLE users ADD COLUMN email TEXT");
            db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT");
            db.execSQL("ALTER TABLE users ADD COLUMN birth_date TEXT");

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reviews` ("
                            + "`id` TEXT NOT NULL, "
                            + "`product_id` TEXT NOT NULL, "
                            + "`username` TEXT NOT NULL, "
                            + "`author_name` TEXT NOT NULL, "
                            + "`rating` INTEGER NOT NULL, "
                            + "`comment` TEXT, "
                            + "`created_at` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`id`), "
                            + "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reviews_product_id` ON `reviews` (`product_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reviews_username` ON `reviews` (`username`)");

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orders` ("
                            + "`id` TEXT NOT NULL, "
                            + "`username` TEXT NOT NULL, "
                            + "`recipient_name` TEXT NOT NULL, "
                            + "`recipient_phone` TEXT NOT NULL, "
                            + "`shipping_address` TEXT NOT NULL, "
                            + "`note` TEXT, "
                            + "`payment_method` TEXT NOT NULL, "
                            + "`subtotal_amount` INTEGER NOT NULL, "
                            + "`shipping_fee` INTEGER NOT NULL, "
                            + "`total_amount` INTEGER NOT NULL, "
                            + "`status` TEXT NOT NULL, "
                            + "`created_at` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`id`))"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_username` ON `orders` (`username`)");

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_items` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`order_id` TEXT NOT NULL, "
                            + "`product_id` TEXT NOT NULL, "
                            + "`product_name` TEXT NOT NULL, "
                            + "`product_image_url` TEXT, "
                            + "`unit_price` INTEGER NOT NULL, "
                            + "`quantity` INTEGER NOT NULL, "
                            + "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_order_id` ON `order_items` (`order_id`)");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` ("
                            + "`order_code` INTEGER NOT NULL, "
                            + "`order_id` TEXT NOT NULL, "
                            + "`payment_link_id` TEXT, "
                            + "`amount` INTEGER NOT NULL, "
                            + "`description` TEXT, "
                            + "`checkout_url` TEXT, "
                            + "`qr_code` TEXT, "
                            + "`bank_bin` TEXT, "
                            + "`account_number` TEXT, "
                            + "`account_name` TEXT, "
                            + "`status` TEXT NOT NULL, "
                            + "`expired_at` INTEGER NOT NULL, "
                            + "`created_at` INTEGER NOT NULL, "
                            + "`paid_at` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`order_code`), "
                            + "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_order_id` ON `payments` (`order_id`)");
        }
    };

    public abstract ProductDao productDao();

    public abstract CartDao cartDao();

    public abstract UserDao userDao();

    public abstract ReviewDao reviewDao();

    public abstract OrderDao orderDao();

    public abstract PaymentDao paymentDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "btday4_room.db"
                            )
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5,
                                    MIGRATION_5_6
                            )
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Runs a database operation sequentially on a background thread after ensuring default data is seeded.
     */
    public static void runAsync(Context context, DbTask task) {
        Context appContext = context.getApplicationContext();
        DB_EXECUTOR.execute(() -> {
            AppDatabase db = getInstance(appContext);
            if (!seeded) {
                seedInitialDataIfNeeded(db);
                seeded = true;
            }
            task.run(db);
        });
    }

    private static void seedInitialDataIfNeeded(AppDatabase db) {
        ProductDao productDao = db.productDao();
        List<Product> defaults = ProductRepository.createDefaultProducts();
        if (productDao.count() == 0) {
            productDao.insertMissing(defaults);
        } else {
            Product sample = productDao.findById("iphone_15");
            boolean needsPriceScaleSync = sample != null
                    && com.example.btday4.util.FormatUtils.parseVnd(sample.getPrice())
                    >= ProductRepository.LEGACY_UNSCALED_PRICE_THRESHOLD;
            if (productDao.countMissingDetails() > 0 || needsPriceScaleSync) {
                productDao.insertMissing(defaults);
                productDao.updateAll(defaults);
            }
        }

        ReviewDao reviewDao = db.reviewDao();
        if (reviewDao.countAll() == 0) {
            reviewDao.insertMissing(ReviewRepository.createDefaultReviews());
        }
    }

    public static void postToMain(Runnable uiTask) {
        MAIN_HANDLER.post(uiTask);
    }

    public interface DbTask {
        void run(AppDatabase db);
    }
}
