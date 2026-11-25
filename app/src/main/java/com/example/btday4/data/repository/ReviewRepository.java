package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.model.UserReviewWithProduct;
import com.example.btday4.util.Constants;
import com.example.btday4.util.ValidationUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages product reviews persisted 100% in Cloud Firestore.
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
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ReviewEntity> reviews = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ReviewEntity r = doc.toObject(ReviewEntity.class);
                        if (r != null) {
                            reviews.add(r);
                        }
                    }
                    reviews.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.accept(reviews);
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> reviewsTask = db.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("username", username)
                .get();
        Task<QuerySnapshot> productsTask = db.collection(Constants.COLLECTION_PRODUCTS).get();

        Tasks.whenAllSuccess(reviewsTask, productsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot reviewSnap = (QuerySnapshot) results.get(0);
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

                    List<UserReviewWithProduct> list = new ArrayList<>();
                    for (DocumentSnapshot doc : reviewSnap.getDocuments()) {
                        ReviewEntity r = doc.toObject(ReviewEntity.class);
                        if (r == null) {
                            continue;
                        }
                        UserReviewWithProduct item = new UserReviewWithProduct();
                        item.review = r;
                        Product p = productMap.get(r.getProductId());
                        item.productName = p != null ? p.getName() : r.getProductId();
                        item.productImageUrl = p != null ? p.getImageUrl() : "";
                        list.add(item);
                    }
                    list.sort((a, b) -> Long.compare(b.review.getCreatedAt(), a.review.getCreatedAt()));
                    callback.accept(list);
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
    }

    public static void countUserReviews(Context context, String username, Consumer<Integer> callback) {
        if (!ValidationUtils.isNotBlank(username)) {
            callback.accept(0);
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(snapshot -> callback.accept(snapshot.size()))
                .addOnFailureListener(e -> callback.accept(0));
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
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    String reviewId = "rev_" + UUID.randomUUID().toString();
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        reviewId = task.getResult().getDocuments().get(0).getId();
                    }
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
                    FirestoreHelper.saveReview(entity, new FirestoreHelper.FirestoreCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (onDone != null) {
                                onDone.run();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (onDone != null) {
                                onDone.run();
                            }
                        }
                    });
                });
    }

    public static void deleteReview(Context context, String reviewId, Runnable onDone) {
        if (!ValidationUtils.isNotBlank(reviewId)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        FirestoreHelper.deleteReview(reviewId, new FirestoreHelper.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (onDone != null) {
                    onDone.run();
                }
            }

            @Override
            public void onError(Exception e) {
                if (onDone != null) {
                    onDone.run();
                }
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
