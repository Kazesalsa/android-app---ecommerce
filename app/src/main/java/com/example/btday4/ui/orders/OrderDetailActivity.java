package com.example.btday4.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.PaymentEntity;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.PayOsRepository;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.databinding.ActivityOrderDetailBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.model.PayOsPaymentInfo;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.ui.checkout.OrderItemAdapter;
import com.example.btday4.ui.checkout.QrPaymentActivity;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.products.ProductDetailActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;

/**
 * Full-bleed order detail screen serving post-checkout confirmation, PayOS QR resumption,
 * voucher discount breakdown, cancellation with restock & voucher restore, and "Mua lại".
 */
public class OrderDetailActivity extends AppCompatActivity {

    private ActivityOrderDetailBinding binding;
    private OrderItemAdapter itemAdapter;
    private String orderId;
    private boolean isNewOrder;
    private OrderWithItems currentOrder;
    private PaymentEntity latestPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(binding.orderDetailRoot, binding.toolbarOrderDetail, null);

        isNewOrder = getIntent().getBooleanExtra(Constants.EXTRA_IS_NEW_ORDER, false);
        orderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);

        binding.toolbarOrderDetail.setNavigationOnClickListener(v -> finish());
        binding.btnContinueShopping.setVisibility(isNewOrder ? View.VISIBLE : View.GONE);

        binding.btnPayQrNow.setOnClickListener(v -> {
            if (ValidationUtils.isNotBlank(orderId)) {
                Intent qrIntent = new Intent(this, QrPaymentActivity.class);
                qrIntent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
                qrIntent.putExtra(Constants.EXTRA_IS_NEW_ORDER, false);
                startActivity(qrIntent);
            }
        });

        binding.btnContinueShopping.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(Constants.EXTRA_OPEN_PRODUCTS, true);
            startActivity(intent);
            finish();
        });

        binding.btnBuyAgain.setOnClickListener(v -> buyAgain());
        binding.btnCancelOrder.setOnClickListener(v -> UiUtils.showConfirmDialog(
                this,
                "Hủy đơn hàng này?",
                "Sản phẩm sẽ được hoàn lại kho và mã ưu đãi (nếu có) sẽ được khôi phục.",
                getString(R.string.btn_cancel_order),
                this::confirmCancel
        ));

        itemAdapter = new OrderItemAdapter(item -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.getProductId());
            intent.putExtra(Constants.EXTRA_OPEN_REVIEW, true);
            startActivity(intent);
        });
        binding.rvOrderDetailItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderDetailItems.setAdapter(itemAdapter);

        loadOrderData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            String newId = intent.getStringExtra(Constants.EXTRA_ORDER_ID);
            if (ValidationUtils.isNotBlank(newId)) {
                orderId = newId;
            }
            if (intent.getBooleanExtra(Constants.EXTRA_IS_NEW_ORDER, false)) {
                isNewOrder = true;
                binding.btnContinueShopping.setVisibility(View.VISIBLE);
            }
            loadOrderData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ValidationUtils.isNotBlank(orderId)) {
            loadOrderData();
        }
    }

    private void loadOrderData() {
        OrderRepository.getOrderDetail(this, orderId, orderWithItems -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (orderWithItems == null || orderWithItems.order == null) {
                finish();
                return;
            }
            currentOrder = orderWithItems;
            bindOrder(orderWithItems);
        });

        PayOsRepository.getLatestPaymentForOrder(this, orderId, payment -> {
            if (isFinishing() || isDestroyed() || binding == null) {
                return;
            }
            latestPayment = payment;
            if (payment != null && payment.getOrderCode() > 0L) {
                binding.tvDetailPayOsRef.setVisibility(View.VISIBLE);
                binding.tvDetailPayOsRef.setText("Mã giao dịch PayOS: " + payment.getOrderCode());
            } else {
                binding.tvDetailPayOsRef.setVisibility(View.GONE);
            }
        });
    }

    private void bindOrder(OrderWithItems data) {
        OrderEntity order = data.order;
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        PaymentMethod method = PaymentMethod.fromCode(order.getPaymentMethod());

        boolean showSuccessBanner = isNewOrder
                && (status == OrderStatus.PAID || status == OrderStatus.PROCESSING || status == OrderStatus.DELIVERED);
        binding.bannerOrderSuccess.setVisibility(showSuccessBanner ? View.VISIBLE : View.GONE);
        if (status == OrderStatus.PAID) {
            binding.tvOrderSuccessTitle.setText("Thanh toán QR thành công");
            binding.tvOrderSuccessSubtitle.setText("Giao dịch VietQR đã được PayOS ghi nhận tự động.");
        } else {
            binding.tvOrderSuccessTitle.setText("Đặt hàng thành công");
            binding.tvOrderSuccessSubtitle.setText("Đơn hàng của bạn đang được chuẩn bị và giao tới nơi.");
        }

        binding.tvDetailOrderCode.setText(order.getId());
        binding.tvDetailOrderStatus.setText(status.getLabel());
        applyStatusBadgeStyle(status);

        binding.tvDetailOrderDate.setText("Ngày đặt: " + FormatUtils.formatDateTime(order.getCreatedAt()));
        binding.tvDetailPaymentMethod.setText("Thanh toán: " + method.getTitle());

        boolean canResumeQr = method == PaymentMethod.VIETQR && status == OrderStatus.PENDING_PAYMENT;
        boolean canCancel = OrderRepository.CANCELLABLE_STATUSES.contains(status.getCode());
        binding.btnPayQrNow.setVisibility(canResumeQr ? View.VISIBLE : View.GONE);
        binding.btnBuyAgain.setVisibility(!isNewOrder && !canResumeQr ? View.VISIBLE : View.GONE);
        binding.btnCancelOrder.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        binding.tvDetailRecipientName.setText(order.getRecipientName());
        binding.tvDetailRecipientPhone.setText(order.getRecipientPhone());
        binding.tvDetailAddress.setText(order.getShippingAddress());
        if (ValidationUtils.isNotBlank(order.getNote())) {
            binding.tvDetailNote.setVisibility(View.VISIBLE);
            binding.tvDetailNote.setText("Ghi chú: " + order.getNote());
        } else {
            binding.tvDetailNote.setVisibility(View.GONE);
        }

        boolean allowReview = status != OrderStatus.CANCELLED && status != OrderStatus.PENDING_PAYMENT;
        itemAdapter.setReviewAllowed(allowReview);
        itemAdapter.submitItems(data.safeItems());

        binding.tvDetailSubtotal.setText(FormatUtils.formatVnd(order.getSubtotalAmount()));
        boolean isFreeShipping = order.getShippingFee() == 0L;
        binding.tvDetailShippingFee.setText(
                isFreeShipping
                        ? getString(R.string.free_shipping)
                        : FormatUtils.formatVnd(order.getShippingFee())
        );
        binding.tvDetailShippingFee.setTextColor(
                ContextCompat.getColor(this, isFreeShipping ? R.color.teal_trust : R.color.text_primary)
        );

        if (order.getVoucherDiscount() > 0L) {
            binding.rowDetailVoucherDiscount.setVisibility(View.VISIBLE);
            String label = order.getVoucherCode().isEmpty()
                    ? "Voucher giảm giá"
                    : "Voucher giảm giá (" + order.getVoucherCode() + ")";
            binding.tvDetailVoucherLabel.setText(label);
            binding.tvDetailVoucherAmount.setText("-" + FormatUtils.formatVnd(order.getVoucherDiscount()));
        } else {
            binding.rowDetailVoucherDiscount.setVisibility(View.GONE);
        }

        binding.tvDetailTotal.setText(FormatUtils.formatVnd(order.getTotalAmount()));
    }

    private void confirmCancel() {
        binding.btnCancelOrder.setEnabled(false);
        boolean hasActiveQr = currentOrder != null
                && OrderStatus.fromCode(currentOrder.order.getStatus()) == OrderStatus.PENDING_PAYMENT
                && latestPayment != null;
        if (hasActiveQr) {
            PayOsRepository.cancelPayment(this, latestPayment, "Khách hàng hủy đơn", new PayOsRepository.PaymentCallback() {
                @Override
                public void onSuccess(PaymentEntity payment) {
                    boolean cancelled = PayOsPaymentInfo.STATUS_CANCELLED.equalsIgnoreCase(payment.getStatus());
                    if (cancelled && currentOrder != null) {
                        VoucherRepository.restoreVoucherIfCancelled(
                                OrderDetailActivity.this,
                                currentOrder.order.getUsername(),
                                currentOrder.order.getVoucherCode()
                        );
                    }
                    onCancelFinished(cancelled);
                }

                @Override
                public void onError(String message) {
                    onCancelFinished(false);
                }
            });
        } else {
            OrderRepository.cancelOrder(this, orderId, this::onCancelFinished);
        }
    }

    private void onCancelFinished(boolean cancelled) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        binding.btnCancelOrder.setEnabled(true);
        if (cancelled) {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ORDER,
                    "Đã hủy đơn hàng",
                    "Đơn " + orderId + " đã hủy và hoàn sản phẩm về kho.",
                    orderId,
                    null,
                    null
            );
        } else {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ALERT,
                    "Không thể hủy đơn",
                    "Không thể hủy đơn hàng ở trạng thái hiện tại."
            );
        }
        loadOrderData();
    }

    private void buyAgain() {
        if (currentOrder == null) {
            return;
        }
        binding.btnBuyAgain.setEnabled(false);
        CartRepository.addItems(this, currentOrder.safeItems(), added -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.btnBuyAgain.setEnabled(true);
            if (added == 0) {
                InAppNotifier.show(
                        binding.getRoot(),
                        AppNotification.TYPE_ALERT,
                        "Hết hàng",
                        "Các sản phẩm trong đơn hiện đã hết hàng."
                );
                return;
            }
            InAppNotifier.recordOnly(
                    this,
                    AppNotification.TYPE_CART,
                    "Đã thêm lại vào giỏ hàng",
                    "Đã thêm " + added + " sản phẩm từ đơn " + orderId + " vào giỏ.",
                    orderId
            );
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(Constants.EXTRA_OPEN_CART, true);
            startActivity(intent);
            finish();
        });
    }

    private void applyStatusBadgeStyle(OrderStatus status) {
        if (status == OrderStatus.PAID || status == OrderStatus.DELIVERED || status == OrderStatus.PROCESSING) {
            binding.tvDetailOrderStatus.setBackgroundResource(R.drawable.bg_freeship_tag);
            binding.tvDetailOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_trust));
        } else if (status == OrderStatus.CANCELLED) {
            binding.tvDetailOrderStatus.setBackgroundResource(R.drawable.bg_pill_subtle);
            binding.tvDetailOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        } else {
            binding.tvDetailOrderStatus.setBackgroundResource(R.drawable.bg_discount_tag);
            binding.tvDetailOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }
}
