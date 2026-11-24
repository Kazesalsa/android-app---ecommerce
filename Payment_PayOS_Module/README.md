# 💳 MODULE THANH TOÁN TRỰC TUYẾN DYNAMIC VIETQR (PAYOS)
> **Hệ thống:** Bách Hóa Xanh Online (FreshMart)  
> **Tác giả:** Nhóm Phát Triển & AI Assistant Antigravity  
> **Công nghệ:** Java 17, Jakarta EE 10, Hibernate 6, Neon PostgreSQL, React 19, Vite, PayOS Gateway.

---

## 📁 1. Cấu Trúc Thư Mục Module

```text
Payment_PayOS_Module/
├── README.md                              # Tài liệu kỹ thuật chi tiết toàn bộ module
├── payos_config.env                       # Thông tin cấu hình Client ID, API Key, Checksum Key
│
├── database/
│   └── payment_schema.sql                 # Cấu trúc bảng "ThanhToan", "DonHang" và Enum PostgreSQL
│
├── backend/
│   ├── config/
│   │   └── PayOSConfig.java               # Singleton khởi tạo SDK PayOS
│   ├── controller/
│   │   └── PaymentController.java         # Servlet điều hướng /payment/create và /payment/webhook
│   ├── service/
│   │   └── PaymentService.java            # Nghiệp vụ tạo link PayOS (Raw HTTP) & xác thực Webhook
│   ├── repository/
│   │   └── PaymentRepository.java         # Tầng truy xuất dữ liệu JPA bảng ThanhToan
│   ├── dto/
│   │   └── PaymentResponse.java           # Data Transfer Object trả về checkoutUrl, qrCode,...
│   ├── entity/
│   │   └── Payment.java                   # Entity ánh xạ bảng "ThanhToan" trong CSDL
│   └── snippet_jwt_filter.java            # Hướng dẫn cấu hình whitelist webhook trong JwtAuthFilter
│
└── frontend/
    ├── VietQRModal/
    │   ├── index.jsx                      # Component Popup mã QR động tích hợp sẵn Polling 3s
    │   └── VietQRModal.module.scss        # Giao diện SCSS cho popup VietQR
    ├── checkout_payment_integration.js    # Code tích hợp thanh toán tại màn hình Checkout
    └── orders_return_url_integration.js   # Code bắt kết quả redirect từ PayOS tại màn hình Đơn hàng
```

---

## 🔑 2. Thông Tin Cấu Hình & Tài Khoản PayOS

| Tham số | Giá trị thực tế của dự án | Mục đích sử dụng |
| :--- | :--- | :--- |
| **`PAYOS_CLIENT_ID`** | `51427319-9751-42d2-b369-28838940e205` | Định danh kênh thanh toán của Merchant |
| **`PAYOS_API_KEY`** | `d975a665-079b-47a1-a269-d779e1a546d3` | Khóa bí mật dùng để xác thực request gửi tới API PayOS |
| **`PAYOS_CHECKSUM_KEY`** | `eaa65f16a5dd2dd9b4e569223f28e07341055aa0a5796a3554b1ef2c0037a3bd` | Khóa băm HMAC-SHA256 để tạo chữ ký và kiểm tra Webhook |
| **`PAYOS_RETURN_URL`** | `http://localhost:5173/orders?status=PAID` | Link PayOS tự redirect khách quay lại sau khi trả tiền |
| **`PAYOS_CANCEL_URL`** | `http://localhost:5173/checkout?cancel=true` | Link PayOS redirect khách quay lại nếu bấm Hủy thanh toán |
| **`Webhook URL`** | `https://bach-hoa-xanh-online.onrender.com/payment/webhook` | Cấu hình tại Dashboard PayOS để server PayOS bắn thông báo ngầm |

---

## 🔄 3. Hai Cơ Chế Thanh Toán Được Hỗ Trợ

### 🌟 Cách 2: Chuyển hướng sang Cổng PayOS (Khuyên dùng - Đang chạy chính thức)
1. Khách hàng bấm **"Đặt hàng"** với phương thức VietQR.
2. Frontend gọi `POST /payment/create/{orderId}` -> Backend trả về `checkoutUrl`.
3. Trình duyệt chuyển hướng thẳng sang trang PayOS: `window.location.href = checkoutUrl;`.
4. Khách quét mã QR trên trang PayOS -> PayOS báo thành công -> PayOS **tự động redirect khách quay lại** `PAYOS_RETURN_URL` (`/orders?status=PAID`).
5. Trang `/orders` bắt URL params và hiển thị Toast xanh: `🎉 Thanh toán đơn hàng qua PayOS thành công!`.
6. Đồng thời, Server PayOS âm thầm bắn Webhook về Backend để cập nhật Database thành `DATHANHTOAN`.

### 📱 Cách 1: Quét mã Dynamic VietQR trực tiếp trên Web (Popup Modal)
1. Sau khi tạo đơn, giao diện mở popup `VietQRModal`.
2. Modal hiển thị ảnh mã QR VietQR động (ngân hàng MBBank, số tài khoản ảo `VQRQAMSGV1812`, số tiền và nội dung chuyển khoản).
3. Modal tự động kích hoạt **Polling ngầm mỗi 3 giây** gọi API `GET /order/{orderId}`.
4. Khi khách chuyển tiền xong -> Webhook PayOS bắn về cập nhật CSDL -> Lần poll tiếp theo phát hiện `trangThai === "DATHANHTOAN"` -> Modal tự động đóng lại và thông báo thành công.
5. Nếu test ở Localhost (chưa cắm Ngrok), người dùng có thể bấm nút **"Tôi đã chuyển khoản xong ✓"** để hoàn tất.

---

## 🛠️ 4. Chi Tiết Kỹ Thuật Backend

### A. Endpoint API
1. **`POST /payment/create/{orderId}`**:
   - **Bảo mật:** Yêu cầu Header `Authorization: Bearer <token>`.
   - **Xác thực:** Kiểm tra quyền sở hữu (Customer chỉ được thanh toán đơn của chính mình).
   - **Số tiền:** Ưu tiên lấy `tongTienSauGiamGia` (nếu có áp voucher).
   - **Kết quả:** Trả về đối tượng `PaymentResponse.PaymentLinkData` chứa `checkoutUrl`, `qrCode`, `accountNumber`,...
2. **`POST /payment/webhook`**:
   - **Bảo mật:** Miễn trừ JWT trong `JwtAuthFilter` (Public endpoint).
   - **Xác minh:** Dùng `PayOSConfig.getPayOS().verifyPaymentWebhookData(body)` để kiểm tra chữ ký số HMAC-SHA256 với `PAYOS_CHECKSUM_KEY`.
   - **Cập nhật CSDL:** Tìm `Payment` và `Order` tương ứng, cập nhật:
     - `Order.trangThai = "DATHANHTOAN"`
     - `Payment.trangThai = "THANHCONG"`
     - `Payment.soTien = amountPaid`
     - `Payment.ngayThanhToan = LocalDateTime.now()`

### B. Kinh Nghiệm Vàng: Vượt mặt Bug của SDK `payos-java`
- **Vấn đề của SDK 1.0.3:** Hàm `payos.createPaymentLink(...)` của thư viện PayOS tự động check chữ ký số của dữ liệu phản hồi (Response Data). Tuy nhiên thuật toán của SDK so sánh sai kiểu chuỗi đối với trường `expiredAt`, dẫn đến lỗi văng Exception: `"The data is unreliable because the signature of the response does not match the signature of the data"`.
- **Giải pháp tối ưu:** Trong `PaymentService.java`, chúng ta **tự tính chữ ký Request bằng `SignatureUtils.createSignatureOfPaymentRequest()`**, sau đó dùng `java.net.http.HttpClient` chuẩn của Java 11+ để POST trực tiếp lên `https://api-merchant.payos.vn/v2/payment-requests`. Cách này vừa an toàn 100% qua HTTPS, vừa dứt điểm hoàn toàn bug của SDK.

---

## 🗄️ 5. Chi Tiết Database (PostgreSQL)

### Bảng `ThanhToan`:
- `maTT` (VARCHAR 50, PK): Lưu mã giao dịch thanh toán hoặc `orderCode` của PayOS.
- `maDonHang` (VARCHAR 50, FK): Khóa ngoại liên kết tới bảng `DonHang`.
- `ngayThanhToan` (TIMESTAMP): Thời điểm thực hiện thanh toán.
- `soTien` (NUMERIC 14, 2): Số tiền thanh toán thực tế.
- `trangThai` (`enum_thanhtoan_trangthai`): `DANGXULY` (khởi tạo) -> `THANHCONG` (đã trả tiền).
- `Deleted` (BOOLEAN): Cờ xóa mềm.

### Bảng `DonHang`:
- Hệ thống sử dụng cột `ghiChu` trên `DonHang` để lưu phụ trợ mã số `orderCode` dạng số nguyên của PayOS. Khi nhận Webhook, hệ thống tìm theo `maTT` trước, nếu không thấy sẽ fallback tìm theo `ghiChu`, đảm bảo không bao giờ thất lạc đơn hàng.

---

## 🚀 6. Hướng Dẫn Tái Sử Dụng Cho Dự Án Khác

1. Chép thư mục `backend/` vào source code Backend của dự án mới.
2. Cài dependency Maven trong `pom.xml`:
   ```xml
   <dependency>
       <groupId>vn.payos</groupId>
       <artifactId>payos-java</artifactId>
       <version>1.0.3</version>
   </dependency>
   ```
3. Chạy file SQL trong `database/payment_schema.sql` vào cơ sở dữ liệu.
4. Đăng ký tài khoản trên [payos.vn](https://payos.vn), tạo 1 kênh thanh toán và lấy 3 key điền vào `.env`.
5. Đưa đường dẫn `/payment/webhook` vào danh sách Whitelist trong Filter bảo mật của bạn.
6. Frontend: Sử dụng đoạn code trong thư mục `frontend/` để gọi API và chuyển hướng.
