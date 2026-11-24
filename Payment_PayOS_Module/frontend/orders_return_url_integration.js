// =========================================================================
// FRONTEND SNIPPET: BẮT KẾT QUẢ REDIRECT TỪ PAYOS TẠI TRANG ĐƠN HÀNG
// File nguồn: src/modules/containers/orders/index.jsx
// =========================================================================

import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import useToastStore from '../../../stores/useToastStore';

export const usePayOSReturnHandler = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const { addToast } = useToastStore();

  useEffect(() => {
    const status = searchParams.get('status');
    const code = searchParams.get('code');
    const cancel = searchParams.get('cancel');

    // PayOS trả về: ?status=PAID&code=00&orderCode=...
    if (status === 'PAID' || code === '00') {
      addToast({
        type: 'success',
        message: '🎉 Thanh toán đơn hàng qua PayOS thành công!'
      });
      // Làm sạch các query params trên thanh địa chỉ URL
      setSearchParams({}, { replace: true });
    } else if (cancel === 'true') {
      addToast({
        type: 'warning',
        message: '⚠️ Bạn đã huỷ giao dịch thanh toán.'
      });
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, addToast, setSearchParams]);
};
