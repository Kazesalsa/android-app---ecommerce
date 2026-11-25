# TechStore VN

Ứng dụng bán đồ công nghệ trên Android (Java), dùng Firebase Cloud Firestore để lưu dữ liệu và tích hợp thanh toán VietQR qua PayOS.

## Yêu cầu

- Android Studio bản mới (hỗ trợ AGP 9.5 và Android SDK 37)
- JDK 25 (Gradle cấu hình sẵn trong `gradle/gradle-daemon-jvm.properties`)
- Máy ảo hoặc điện thoại Android 7.0 (API 24) trở lên

## Cách cài đặt và chạy

### 1. Lấy code về máy

```bash
git clone https://github.com/Kazesalsa/android-app---ecommerce.git
cd android-app---ecommerce
git checkout feat/firebase-vouchers-cleanup
```

### 2. Thêm key thanh toán PayOS

Mở dự án bằng Android Studio để IDE tự tạo file `local.properties` ở thư mục gốc (chứa sẵn `sdk.dir`), sau đó mở file đó lên và thêm 3 key lấy từ trang quản trị PayOS:

```properties
PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...
PAYOS_CHECKSUM_KEY=...
```

Lưu ý:
- File `local.properties` nằm trong `.gitignore`, không đẩy file này lên Git.
- Nếu chỉ cần chạy app và đặt đơn bằng hình thức thanh toán khi nhận hàng (COD) thì có thể bỏ qua 3 dòng trên. Khi bấm thanh toán VietQR mới cần key PayOS.

### 3. Firebase

File `app/google-services.json` đã có sẵn trong repo để kết nối Firestore. Nếu bạn muốn đổi sang project Firebase của riêng mình thì thay file này và nhớ bật **Cloud Firestore** trên Firebase Console.

### 4. Build và chạy

Nhấn **Run** trong Android Studio, hoặc chạy bằng lệnh:

```bash
./gradlew assembleDebug
```

Chạy unit test:

```bash
./gradlew testDebugUnitTest
```

## Dùng thử nhanh

Tài khoản mẫu có sẵn trong app (có thể tự đăng ký tài khoản mới ở màn hình đăng nhập):
- Tài khoản: `tantai@techstore.vn`
- Mật khẩu: `123456`

Một vài mức giá đang để nhỏ hơn thực tế (từ 21.900đ đến 344.900đ) để tiện quét mã chuyển khoản thật khi test PayOS:
- Đơn từ 50.000đ được miễn phí giao hàng (dưới mức này phí ship là 300đ).
- Sau khi áp mã giảm giá, số tiền cần thanh toán tối thiểu là 2.000đ.

## Cách thêm voucher mới

Danh sách voucher không fix cứng trong code mà lấy trực tiếp từ Firestore. Muốn thêm mã giảm giá mới, vào **Firebase Console -> Firestore Database -> collection `vouchers`** rồi tạo document mới theo mẫu:

```json
{
  "code": "TECHNEW15",
  "title": "Giảm 15% đơn công nghệ",
  "description": "Giảm 15% tối đa 25.000 đ cho đơn từ 50.000 đ",
  "discountType": "PERCENT",
  "discountValue": 15,
  "maxDiscountAmount": 25000,
  "minOrderAmount": 50000,
  "badgeLabel": "-15%",
  "secretOnly": false,
  "active": true,
  "usageLimit": 100,
  "usedCount": 0,
  "expiryMillis": 1893456000000
}
```

Giải thích nhanh vài trường hay dùng:
- `discountType`: nhận 1 trong 3 giá trị `PERCENT` (giảm theo %), `FIXED` (giảm số tiền cố định), hoặc `FREESHIP` (miễn phí vận chuyển).
- `secretOnly`: để `true` nếu muốn ẩn mã khỏi danh sách, người dùng phải tự nhập tay mã mới áp được.
- `active`: đổi thành `false` để tạm tắt voucher.
- `usageLimit`: tổng số lượt dùng tối đa (để `0` là không giới hạn).
- `expiryMillis`: thời gian hết hạn theo Unix timestamp tính bằng mili-giây (để `0` là không giới hạn).

## Lưu ý khi chỉnh sửa code

- Các key dùng cho `Intent` (`putExtra` / `getExtra`) khai báo tập trung trong `app/src/main/java/com/example/btday4/util/Constants.java` (tiền tố `EXTRA_`), hạn chế viết chuỗi cứng trực tiếp trong các Activity/Fragment.
