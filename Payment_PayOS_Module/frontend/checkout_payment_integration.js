// =========================================================================
// FRONTEND SNIPPET: TÍCH HỢP THANH TOÁN TẠI TRANG CHECKOUT
// File nguồn: src/modules/containers/checkout/index.jsx
// =========================================================================

import orderService from '../../services/orderService';
import useToastStore from '../../stores/useToastStore';

/**
 * Xử lý khi người dùng chọn phương thức VIETQR (PayOS) và bấm "Đặt Hàng"
 */
export const handleCheckoutPayment = async (newOrder, paymentMethod) => {
  const { addToast } = useToastStore.getState();

  // 1. Nếu chọn VietQR (PayOS) -> Gọi Backend tạo link và chuyển hướng sang PayOS
  if (paymentMethod === 'VIETQR') {
    const orderId = newOrder.maDonHang;

    try {
      // Gọi API: POST /payment/create/{orderId}
      const payRes = await orderService.createPayment(orderId);
      const checkoutUrl = payRes?.data?.checkoutUrl || (typeof payRes?.data === 'string' ? payRes.data : null);

      if (checkoutUrl) {
        // [CÁCH 2 - KHUYÊN DÙNG]: Chuyển hướng trình duyệt trực tiếp sang cổng thanh toán PayOS
        window.location.href = checkoutUrl;
        return;
      } else {
        throw new Error('Không nhận được đường dẫn thanh toán từ PayOS');
      }
    } catch (payErr) {
      addToast({
        type: 'error',
        message: payErr.message || 'Lỗi khi tạo link thanh toán PayOS. Vui lòng thử lại!'
      });
    }
  } else {
    // 2. Phương thức COD (Tiền mặt khi nhận hàng) -> Thành công ngay
    addToast({
      type: 'success',
      message: '🎉 Đặt hàng COD thành công! Nhân viên sẽ liên hệ giao hàng sớm.'
    });
  }
};
