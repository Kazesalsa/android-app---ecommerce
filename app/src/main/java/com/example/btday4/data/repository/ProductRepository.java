package com.example.btday4.data.repository;

import android.content.Context;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.model.ProductWithRating;
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
import java.util.function.Consumer;

/**
 * Cloud Firestore-backed product repository providing catalog queries with live rating stats.
 */
public final class ProductRepository {

    public static final long LEGACY_UNSCALED_PRICE_THRESHOLD = 1_000_000L;

    private ProductRepository() {
    }

    public static void getAllWithRating(Context context, Consumer<List<ProductWithRating>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> productsTask = db.collection(Constants.COLLECTION_PRODUCTS).get();
        Task<QuerySnapshot> reviewsTask = db.collection(Constants.COLLECTION_REVIEWS).get();

        Tasks.whenAllSuccess(productsTask, reviewsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot productSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot reviewSnap = (QuerySnapshot) results.get(1);
                    List<Product> products = new ArrayList<>();
                    for (DocumentSnapshot doc : productSnap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            if (p.getId().isEmpty()) {
                                p.setId(doc.getId());
                            }
                            products.add(p);
                        }
                    }
                    callback.accept(combineWithRatings(products, extractReviews(reviewSnap)));
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
    }

    public static List<ProductWithRating> withoutRatings(List<Product> products) {
        return combineWithRatings(products == null ? new ArrayList<>() : products, new ArrayList<>());
    }

    public static void combineWithLiveRatings(
            List<Product> products,
            Consumer<List<ProductWithRating>> callback
    ) {
        if (products == null || products.isEmpty()) {
            callback.accept(new ArrayList<>());
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_REVIEWS)
                .get()
                .addOnSuccessListener(reviewSnap ->
                        callback.accept(combineWithRatings(products, extractReviews(reviewSnap))))
                .addOnFailureListener(e ->
                        callback.accept(combineWithRatings(products, new ArrayList<>())));
    }

    public static void findById(Context context, String id, Consumer<Product> callback) {
        if (!ValidationUtils.isNotBlank(id)) {
            callback.accept(null);
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PRODUCTS)
                .document(id.trim())
                .get()
                .addOnSuccessListener(doc -> {
                    Product p = doc.exists() ? doc.toObject(Product.class) : null;
                    if (p != null && p.getId().isEmpty()) {
                        p.setId(doc.getId());
                    }
                    callback.accept(p);
                })
                .addOnFailureListener(e -> callback.accept(null));
    }

    private static List<ReviewEntity> extractReviews(QuerySnapshot snapshot) {
        List<ReviewEntity> reviews = new ArrayList<>();
        if (snapshot == null) {
            return reviews;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            ReviewEntity r = doc.toObject(ReviewEntity.class);
            if (r != null) {
                reviews.add(r);
            }
        }
        return reviews;
    }

    private static List<ProductWithRating> combineWithRatings(
            List<Product> products,
            List<ReviewEntity> reviews
    ) {
        Map<String, int[]> stats = new HashMap<>(); // [sum, count]
        for (ReviewEntity r : reviews) {
            if (r == null || r.getProductId().isEmpty()) {
                continue;
            }
            int[] pair = stats.computeIfAbsent(r.getProductId(), k -> new int[2]);
            pair[0] += r.getRating();
            pair[1] += 1;
        }
        List<ProductWithRating> list = new ArrayList<>(products.size());
        for (Product p : products) {
            int[] pair = stats.get(p.getId());
            int count = pair == null ? 0 : pair[1];
            double avg = count == 0 ? 0.0 : (double) pair[0] / count;
            list.add(new ProductWithRating(p, avg, count));
        }
        return list;
    }

    public static List<Product> createDefaultProducts() {
        List<Product> list = new ArrayList<>();

        list.add(new Product(
                "iphone_15",
                "iPhone 15 128GB",
                "199.900 ₫",
                "229.900 ₫",
                "Chip A16 Bionic, 6.1\" Super Retina XDR, Camera 48MP, USB-C",
                "Điện thoại",
                "https://cdn.tgdd.vn/Products/Images/42/281570/iphone-15-xanh-thumb-600x600.jpg",
                "Apple",
                "Thiết kế mặt lưng kính pha màu sang trọng kết hợp khung nhôm hàng không nhẹ bền. Màn hình Super Retina XDR 6.1 inch đi kèm Dynamic Island hiển thị thông báo trực quan, cảm biến chính 48MP hỗ trợ chụp chân dung sắc nét và cổng USB-C đồng bộ tiện lợi.",
                "Chip xử lý:Apple A16 Bionic 6 nhân|Màn hình:6.1\" OLED Super Retina XDR|Bộ nhớ:128 GB|Camera sau:48MP + 12MP Ultra Wide|Cổng kết nối:USB-C|Chuẩn kháng nước:IP68",
                "12 tháng chính hãng Apple VN",
                18
        ));

        list.add(new Product(
                "iphone_16_pro_max",
                "iPhone 16 Pro Max 256GB",
                "344.900 ₫",
                "349.900 ₫",
                "Chip A18 Pro, 6.9\" ProMotion 120Hz, Khung Titan, Camera 48MP Fusion",
                "Điện thoại",
                "https://cdn.tgdd.vn/Products/Images/42/329151/iphone-16-pro-max-sa-mac-thumb-600x600.jpg",
                "Apple",
                "Khung Titan cấp độ 5 bền bỉ cùng viền màn hình siêu mỏng 6.9 inch ProMotion 120Hz. Chip A18 Pro cho hiệu năng đồ họa vượt trội, nút Camera Control chuyên dụng cho phép tinh chỉnh khẩu độ, thu phóng tức thì và quay video 4K 120fps Dolby Vision.",
                "Chip xử lý:Apple A18 Pro|Màn hình:6.9\" Super Retina XDR 120Hz|Bộ nhớ:256 GB|Camera sau:48MP Fusion + 48MP Ultra Wide + Tele 5x|Chất liệu:Titan cấp độ 5|Pin:Phát video lên đến 33 giờ",
                "12 tháng chính hãng Apple VN",
                12
        ));

        list.add(new Product(
                "macbook_air_m3",
                "MacBook Air 13 inch M3",
                "279.900 ₫",
                "299.900 ₫",
                "Apple M3 8-core, RAM 16GB, SSD 256GB, Liquid Retina 13.6\"",
                "Laptop",
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/331644/macbook-air-13-inch-m3-tgdd-bac-1-638660181778087730.jpg",
                "Apple",
                "Mỏng chỉ 11.3mm và nặng 1.24kg nhưng mang sức mạnh của chip Apple M3 cùng 16GB bộ nhớ thống nhất. Hoạt động hoàn toàn yên tĩnh không quạt tản nhiệt, hỗ trợ xuất 2 màn hình rời và thời lượng pin thực tế suốt ngày dài làm việc.",
                "Vi xử lý:Apple M3 (8-core CPU, 8-core GPU)|RAM:16 GB Unified Memory|Ổ cứng:256 GB SSD|Màn hình:13.6\" Liquid Retina 500 nits|Trọng lượng:1.24 kg|Pin:Lên đến 18 giờ",
                "12 tháng chính hãng Apple VN",
                10
        ));

        list.add(new Product(
                "ipad_air_6",
                "iPad Air 6 M2 11 inch",
                "169.900 ₫",
                "184.900 ₫",
                "Chip Apple M2, Liquid Retina 11\", hỗ trợ Apple Pencil Pro",
                "Tablet",
                "https://cdn.tgdd.vn/Products/Images/522/325523/ipad-air-11-inch-m2-lte-grey-thumb.jpg",
                "Apple",
                "Cân bằng hoàn hảo giữa tính di động và hiệu năng với chip Apple M2. Camera trước được đặt theo cạnh ngang tối ưu cho họp trực tuyến, tương thích mượt mà với Apple Pencil Pro và Magic Keyboard cho học tập lẫn thiết kế.",
                "Vi xử lý:Apple M2 8 nhân|Màn hình:11\" Liquid Retina True Tone|Bộ nhớ:128 GB|Kết nối:Wi-Fi 6E, USB-C|Tương thích:Apple Pencil Pro, Magic Keyboard|Trọng lượng:462 g",
                "12 tháng chính hãng Apple VN",
                14
        ));

        list.add(new Product(
                "airpods_pro_2",
                "AirPods Pro 2 (USB-C)",
                "54.900 ₫",
                "61.900 ₫",
                "Chống ồn chủ động ANC 2X, Chip H2, MagSafe USB-C IP54",
                "Phụ kiện",
                "https://cdn.tgdd.vn/Products/Images/54/315014/tai-nghe-bluetooth-airpods-pro-2nd-gen-usb-c-charge-apple-1.jpg",
                "Apple",
                "Trang bị chip âm thanh H2 giúp khử ồn chủ động gấp đôi thế hệ trước cùng chế độ Âm thanh thích ứng tự cân bằng theo môi trường. Hộp sạc MagSafe USB-C tích hợp loa định vị Tìm kiếm và chuẩn kháng bụi nước IP54.",
                "Chip âm thanh:Apple H2|Chống ồn:ANC 2X & Xuyên âm thích ứng|Thời lượng pin:6 giờ (30 giờ với hộp sạc)|Hộp sạc:MagSafe USB-C|Kháng bụi nước:IP54",
                "12 tháng chính hãng Apple VN",
                25
        ));

        list.add(new Product(
                "apple_watch_s10",
                "Apple Watch Series 10",
                "109.900 ₫",
                "119.900 ₫",
                "Màn hình OLED góc rộng, theo dõi sức khỏe, sạc nhanh 30 phút",
                "Phụ kiện",
                "https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/329157/apple-watch-s10-hong-1-638616619897644968.jpg",
                "Apple",
                "Thế hệ Apple Watch mỏng nhất từ trước đến nay với màn hình OLED góc nhìn rộng sáng rõ hơn khi nhìn nghiêng. Tích hợp theo dõi nhịp tim, phân tích giấc ngủ chuyên sâu, cảm biến độ sâu khi bơi và sạc đạt 80% chỉ trong 30 phút.",
                "Màn hình:Wide-angle OLED Always-On|Chip xử lý:Apple S10 SiP|Cảm biến:Nhịp tim, ECG, nhiệt độ nước|Sạc nhanh:80% trong 30 phút|Chống nước:50m (WR50)",
                "12 tháng chính hãng Apple VN",
                16
        ));

        list.add(new Product(
                "keychron_k8",
                "Bàn phím cơ Keychron K8",
                "21.900 ₫",
                "24.900 ₫",
                "Gateron G Pro, TKL 87 phím, Bluetooth 5.1 & USB-C",
                "Phụ kiện",
                "https://cdn2.cellphones.com.vn/insecure/rs:fill:0:358/q:90/plain/https://cellphones.com.vn/media/catalog/product/b/a/ban-phim-keychron-k8-1.jpg",
                "Keychron",
                "Bàn phím cơ layout Tenkeyless 87 phím gọn gàng, hỗ trợ chuyển đổi nhanh giữa macOS và Windows bằng công tắc gạt vật lý. Kết nối tối đa 3 thiết bị qua Bluetooth 5.1 ổn định hoặc dùng dây Type-C có độ trễ thấp.",
                "Layout:TKL 87 phím|Switch:Gateron G Pro (Hot-swappable)|Kết nối:Bluetooth 5.1 & USB-C|Hệ điều hành:macOS / Windows / Linux|Pin:4000 mAh",
                "12 tháng chính hãng",
                20
        ));

        list.add(new Product(
                "logitech_mx_master_3s",
                "Chuột Logitech MX Master 3S",
                "23.900 ₫",
                "26.900 ₫",
                "Cảm biến 8000 DPI trên kính, cuộn MagSpeed, click yên tĩnh",
                "Phụ kiện",
                "https://cdn2.cellphones.com.vn/insecure/rs:fill:0:358/q:90/plain/https://cellphones.com.vn/media/catalog/product/c/h/chuot-khong-day-bluetooth-logitech-mx-master-3s_6_.png",
                "Logitech",
                "Chuột công thái học chuẩn mực cho lập trình và thiết kế. Nút bấm Quiet Clicks giảm 90% tiếng ồn, con lăn điện từ MagSpeed cuộn 1000 dòng mỗi giây và cảm biến Darkfield 8000 DPI hoạt động chính xác ngay cả trên mặt kính.",
                "Cảm biến:Darkfield 8000 DPI|Con lăn:MagSpeed điện từ + cuộn ngang|Kết nối:Bluetooth & Logi Bolt (3 thiết bị)|Cổng sạc:USB-C (dùng đến 70 ngày)|Trọng lượng:141 g",
                "12 tháng chính hãng Logitech",
                22
        ));

        list.add(new Product(
                "lg_ultrafine_4k",
                "Màn hình LG UltraFine 27\" 4K",
                "98.900 ₫",
                "112.900 ₫",
                "IPS 27\" 4K UHD, 99% sRGB, HDR400, sạc ngược USB-C 96W",
                "Phụ kiện",
                "https://cdn2.cellphones.com.vn/x/media/catalog/product/m/_/m_n_h_nh_lg_ultrafine_27u711b-b_27_inch_-_1.png",
                "LG",
                "Màn hình 27 inch độ phân giải 4K UHD sắc nét với tấm nền IPS đạt chuẩn màu 99% sRGB và VESA DisplayHDR 400. Chỉ cần một sợi cáp USB-C duy nhất để vừa truyền hình ảnh 4K vừa cấp nguồn sạc nhanh 96W cho MacBook hoặc laptop.",
                "Kích thước:27 inch IPS|Độ phân giải:3840 × 2160 (4K UHD)|Độ phủ màu:99% sRGB, HDR400|Cổng kết nối:USB-C (PD 96W), HDMI, DisplayPort|Chân đế:Điều chỉnh độ cao, xoay dọc Pivot",
                "24 tháng chính hãng LG VN",
                9
        ));

        return list;
    }
}
