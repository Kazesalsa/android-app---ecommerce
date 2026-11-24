package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.ReviewDao;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.model.UserReviewWithProduct;
import com.example.btday4.util.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages product reviews persisted in Room and synchronized to Firestore.
 */
public final class ReviewRepository {

    private ReviewRepository() {
    }

    public static void getReviewsForProduct(
            Context context,
            String productId,
            Consumer<List<ReviewEntity>> callback
    ) {
        if (!ValidationUtils.isNotBlank(productId)) {
            callback.accept(new ArrayList<>());
            return;
        }
        AppDatabase.runAsync(context, db -> {
            List<ReviewEntity> reviews = db.reviewDao().getByProductId(productId);
            AppDatabase.postToMain(() -> callback.accept(reviews));
        });
    }

    public static void getUserReviews(
            Context context,
            String username,
            Consumer<List<UserReviewWithProduct>> callback
    ) {
        if (!ValidationUtils.isNotBlank(username)) {
            callback.accept(new ArrayList<>());
            return;
        }
        AppDatabase.runAsync(context, db -> {
            List<UserReviewWithProduct> list = db.reviewDao().getUserReviewsWithProduct(username);
            AppDatabase.postToMain(() -> callback.accept(list));
        });
    }

    public static void countUserReviews(Context context, String username, Consumer<Integer> callback) {
        if (!ValidationUtils.isNotBlank(username)) {
            callback.accept(0);
            return;
        }
        AppDatabase.runAsync(context, db -> {
            int count = db.reviewDao().countByUsername(username);
            AppDatabase.postToMain(() -> callback.accept(count));
        });
    }

    /**
     * Inserts or updates the current user's review for a product (one review per user per product).
     */
    public static void saveReview(
            Context context,
            String productId,
            String username,
            String authorName,
            int rating,
            String comment,
            Runnable onDone
    ) {
        if (!ValidationUtils.isNotBlank(productId) || !ValidationUtils.isNotBlank(username)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        AppDatabase.runAsync(context, db -> {
            ReviewDao dao = db.reviewDao();
            ReviewEntity existing = dao.findByProductAndUser(productId, username);
            String reviewId = existing != null ? existing.getId() : "rev_" + UUID.randomUUID().toString();
            String safeAuthor = ValidationUtils.isNotBlank(authorName) ? authorName.trim() : username;
            String safeComment = comment == null ? "" : comment.trim();

            ReviewEntity entity = new ReviewEntity(
                    reviewId,
                    productId,
                    username,
                    safeAuthor,
                    Math.max(1, Math.min(5, rating)),
                    safeComment,
                    System.currentTimeMillis()
            );
            dao.upsert(entity);
            FirestoreHelper.saveReview(entity, null);

            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public static void deleteReview(Context context, String reviewId, Runnable onDone) {
        if (!ValidationUtils.isNotBlank(reviewId)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        AppDatabase.runAsync(context, db -> {
            db.reviewDao().deleteById(reviewId);
            FirestoreHelper.deleteReview(reviewId, null);
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public static List<ReviewEntity> createDefaultReviews() {
        long now = System.currentTimeMillis();
        long day = 24L * 60L * 60L * 1000L;
        List<ReviewEntity> list = new ArrayList<>();

        list.add(new ReviewEntity(
                "seed_rev_1", "iphone_15", "minh.nguyen@demo.vn", "Nguyễn Nhật Minh", 5,
                "Máy cầm gọn tay, mặt lưng kính nhám không bám vân tay. Chuyển sang cổng USB-C dùng chung sạc với laptop rất tiện.",
                now - 2 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_2", "iphone_15", "thu.ha@demo.vn", "Trần Thu Hà", 5,
                "Camera 48MP chụp ban ngày lẫn thiếu sáng đều sắc nét, pin dùng cơ bản đủ trọn một ngày.",
                now - 6 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_3", "iphone_15", "quoc.anh@demo.vn", "Lê Quốc Anh", 4,
                "Mọi thứ ổn định và mượt, chỉ hơi tiếc màn hình vẫn là 60Hz.",
                now - 11 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_4", "iphone_16_pro_max", "hoang.nam@demo.vn", "Phạm Hoàng Nam", 5,
                "Viền màn hình cực mỏng, pin rất tốt. Quay 4K 120fps mượt rõ rệt so với đời cũ.",
                now - 3 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_5", "iphone_16_pro_max", "khanh.linh@demo.vn", "Đỗ Khánh Linh", 5,
                "Khung Titan nhẹ hơn hẳn bản thép trước đây, màu Sa Mạc ngoài đời rất tinh tế.",
                now - 9 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_6", "macbook_air_m3", "duc.thanh@demo.vn", "Vũ Đức Thành", 5,
                "Bản 16GB RAM dùng Android Studio và mở nhiều tab Chrome vẫn mượt, pin thực tế tầm 12-14 tiếng.",
                now - 4 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_7", "macbook_air_m3", "ngoc.mai@demo.vn", "Bùi Ngọc Mai", 4,
                "Máy nhẹ, hoàn thiện sắc sảo. Nếu có thêm cổng USB-A thì hoàn hảo nhưng dùng hub nhỏ cũng ổn.",
                now - 10 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_8", "ipad_air_6", "hai.dang@demo.vn", "Ngô Hải Đăng", 5,
                "Ghi chú học tập bằng Apple Pencil Pro rất nhạy, loa kép nghe rõ và màn hình trong trẻo.",
                now - 5 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_9", "ipad_air_6", "phuong.vy@demo.vn", "Lý Phương Vy", 4,
                "Hiệu năng chip M2 dư dả cho vẽ và chỉnh ảnh nhẹ, pin ổn.",
                now - 14 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_10", "airpods_pro_2", "tuan.kiet@demo.vn", "Phan Tuấn Kiệt", 5,
                "Chống ồn trên máy bay và quán cà phê hiệu quả rõ rệt, đeo lâu không bị cấn tai.",
                now - 2 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_11", "airpods_pro_2", "lan.anh@demo.vn", "Hoàng Lan Anh", 5,
                "Chuyển đổi giữa iPhone và MacBook gần như tức thì, mic gọi điện ngoài đường khá rõ.",
                now - 8 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_12", "apple_watch_s10", "bao.tram@demo.vn", "Nguyễn Bảo Trâm", 5,
                "Thân đồng hồ mỏng nhẹ đeo ngủ rất thoải mái, sạc nhanh buổi sáng 20 phút là đủ dùng.",
                now - 7 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_13", "keychron_k8", "huy.hoang@demo.vn", "Trịnh Huy Hoàng", 5,
                "Gõ êm, chuyển qua lại giữa Mac và Windows chỉ cần gạt công tắc bên hông rất tiện.",
                now - 6 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_14", "keychron_k8", "gia.bao@demo.vn", "Đặng Gia Bảo", 4,
                "Build chắc chắn, pin trâu. Nên dùng thêm kê tay vì độ cao phím hơi dốc nhẹ.",
                now - 15 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_15", "logitech_mx_master_3s", "thanh.tung@demo.vn", "Nguyễn Thanh Tùng", 5,
                "Click yên tĩnh hẳn, cuộn ngang trên bảng tính và timeline cực kỳ hữu ích.",
                now - 4 * day
        ));

        list.add(new ReviewEntity(
                "seed_rev_16", "lg_ultrafine_4k", "anh.khoa@demo.vn", "Võ Anh Khoa", 5,
                "Hiển thị chữ sắc nét như màn Retina, cắm 1 dây USB-C vừa xuất hình vừa sạc 96W giúp bàn làm việc tối giản hẳn.",
                now - 5 * day
        ));
        list.add(new ReviewEntity(
                "seed_rev_17", "lg_ultrafine_4k", "minh.thu@demo.vn", "Cao Minh Thư", 4,
                "Màu sắc chuẩn chỉnh, chân đế nâng hạ linh hoạt. Loa tích hợp ở mức đủ nghe.",
                now - 12 * day
        ));

        return list;
    }
}
