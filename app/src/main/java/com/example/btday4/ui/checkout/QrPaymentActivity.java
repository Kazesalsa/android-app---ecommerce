package com.example.btday4.ui.checkout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.PaymentEntity;
import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.PayOsRepository;
import com.example.btday4.databinding.ActivityQrPaymentBinding;
import com.example.btday4.model.NotificationItem;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.PayOsPaymentInfo;
import com.example.btday4.ui.orders.OrderDetailActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.QrCodeGenerator;
import com.example.btday4.util.UiUtils;
import com.google.android.material.snackbar.Snackbar;
import java.util.Locale;

/**
 * Full-screen PayOS VietQR payment screen with offline ZXing QR bitmap rendering, live status
 * polling, countdown timer, copyable transfer details, and inline two-step cancel (zero popups).
 */
public class QrPaymentActivity extends AppCompatActivity {

    private ActivityQrPaymentBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String orderId = "";
    private boolean isNewOrder = false;
    private OrderEntity currentOrder;
    private PaymentEntity currentPayment;
    private boolean isPollingActive = false;
    private boolean isCheckingStatus = false;
    private boolean hasCompletedTransition = false;

    private final Runnable pollStatusRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || isFinishing() || isDestroyed() || currentPayment == null) {
                return;
            }
            checkStatusOnServer(false);
            mainHandler.postDelayed(this, Constants.PAYOS_POLL_INTERVAL_MS);
        }
    };

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || isFinishing() || isDestroyed() || currentPayment == null) {
                return;
            }
            long remainingSec = currentPayment.getExpiredAtSeconds() - (System.currentTimeMillis() / 1000L);
            if (remainingSec <= 0L) {
                stopTimers();
                binding.tvQrCountdown.setText("00:00");
                showErrorState(
                        "Mã QR đã hết hạn",
                        "Thời gian thanh toán mã QR đã kết thúc. Bạn có thể tạo mã QR mới cho đơn hàng này."
                );
                return;
            }
            long minutes = remainingSec / 60L;
            long seconds = remainingSec % 60L;
            binding.tvQrCountdown.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            mainHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.qrPaymentRoot);

        orderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);
        isNewOrder = getIntent().getBooleanExtra(Constants.EXTRA_IS_NEW_ORDER, false);
        if (orderId == null || orderId.trim().isEmpty()) {
            finish();
            return;
        }

        binding.toolbarQrPayment.setNavigationOnClickListener(v -> handleBackNavigation());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
        binding.btnRetryQr.setOnClickListener(v -> initPayOsPayment(true));
        binding.btnViewOrderFromError.setOnClickListener(v -> openOrderDetailAndFinish(false));

        binding.btnCopyAccount.setOnClickListener(v -> {
            if (currentPayment != null) {
                copyToClipboard("Số tài khoản", currentPayment.getAccountNumber());
            }
        });
        binding.btnCopyAmount.setOnClickListener(v -> {
            if (currentPayment != null) {
                copyToClipboard("Số tiền", String.valueOf(currentPayment.getAmount()));
            }
        });
        binding.btnCopyDescription.setOnClickListener(v -> {
            if (currentPayment != null) {
                copyToClipboard("Nội dung chuyển khoản", currentPayment.getDescription());
            }
        });

        binding.btnCheckStatus.setOnClickListener(v -> checkStatusOnServer(true));
        binding.btnOpenPayOsWeb.setOnClickListener(v -> openCheckoutUrlInBrowser());

        // Inline 2-step cancel confirmation (no AlertDialog popup)
        binding.btnCancelPayment.setOnClickListener(v -> showCancelConfirm(true));
        binding.btnKeepPayment.setOnClickListener(v -> showCancelConfirm(false));
        binding.btnConfirmCancelPayment.setOnClickListener(v -> confirmCancelPayment());

        loadOrderAndCreateQr();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentPayment != null && !hasCompletedTransition && binding.scrollQrContent.getVisibility() == View.VISIBLE) {
            startTimers();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimers();
    }

    @Override
    protected void onDestroy() {
        stopTimers();
        super.onDestroy();
    }

    private void loadOrderAndCreateQr() {
        showLoadingState();
        OrderRepository.getOrderDetail(this, orderId, orderWithItems -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (orderWithItems == null || orderWithItems.order == null) {
                finish();
                return;
            }
            currentOrder = orderWithItems.order;
            OrderStatus status = OrderStatus.fromCode(currentOrder.getStatus());
            if (status == OrderStatus.PAID) {
                openOrderDetailAndFinish(true);
                return;
            }
            initPayOsPayment(false);
        });
    }

    private void initPayOsPayment(boolean forceNew) {
        if (currentOrder == null) {
            return;
        }
        stopTimers();
        showLoadingState();
        PayOsRepository.createOrResumePayment(this, currentOrder, forceNew, new PayOsRepository.PaymentCallback() {
            @Override
            public void onSuccess(PaymentEntity payment) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                currentPayment = payment;
                if (PayOsPaymentInfo.STATUS_PAID.equalsIgnoreCase(payment.getStatus())) {
                    openOrderDetailAndFinish(true);
                    return;
                }
                bindPaymentUi(payment);
                showActiveQrState();
                startTimers();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showErrorState("Không thể kết nối PayOS", message);
            }
        });
    }

    private void bindPaymentUi(PaymentEntity payment) {
        binding.tvQrOrderCode.setText(payment.getOrderId());
        String formattedAmount = FormatUtils.formatVnd(payment.getAmount());
        binding.tvQrAmount.setText(formattedAmount);
        binding.tvQrTransferAmount.setText(formattedAmount);
        binding.tvQrBankName.setText(QrCodeGenerator.resolveBankName(payment.getBankBin()));
        binding.tvQrAccountName.setText(
                payment.getAccountName() == null || payment.getAccountName().isEmpty()
                        ? "Tài khoản PayOS"
                        : payment.getAccountName()
        );
        binding.tvQrAccountNumber.setText(payment.getAccountNumber());
        binding.tvQrDescription.setText(payment.getDescription());

        int qrSizePx = Math.round(240 * getResources().getDisplayMetrics().density);
        Bitmap qrBitmap = QrCodeGenerator.encode(payment.getQrCode(), qrSizePx);
        if (qrBitmap != null) {
            binding.imgQrCode.setImageBitmap(qrBitmap);
        }

        boolean hasCheckoutUrl = payment.getCheckoutUrl() != null && !payment.getCheckoutUrl().trim().isEmpty();
        binding.btnOpenPayOsWeb.setVisibility(hasCheckoutUrl ? View.VISIBLE : View.GONE);
    }

    private void checkStatusOnServer(boolean userTriggered) {
        if (currentPayment == null || isCheckingStatus || hasCompletedTransition) {
            return;
        }
        isCheckingStatus = true;
        if (userTriggered) {
            binding.btnCheckStatus.setEnabled(false);
            binding.tvQrStatusText.setText("Đang kiểm tra giao dịch với PayOS...");
        }

        PayOsRepository.refreshPaymentStatus(this, currentPayment, new PayOsRepository.PaymentCallback() {
            @Override
            public void onSuccess(PaymentEntity updated) {
                isCheckingStatus = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                currentPayment = updated;
                binding.btnCheckStatus.setEnabled(true);

                if (PayOsPaymentInfo.STATUS_PAID.equalsIgnoreCase(updated.getStatus())) {
                    stopTimers();
                    openOrderDetailAndFinish(true);
                } else if (PayOsPaymentInfo.STATUS_CANCELLED.equalsIgnoreCase(updated.getStatus())
                        || PayOsPaymentInfo.STATUS_EXPIRED.equalsIgnoreCase(updated.getStatus())) {
                    stopTimers();
                    showErrorState(
                            "Giao dịch QR đã đóng",
                            "Liên kết thanh toán này đã bị hủy hoặc hết hạn trên PayOS."
                    );
                } else {
                    binding.tvQrStatusText.setText("Đang tự động kiểm tra thanh toán...");
                    if (userTriggered) {
                        Snackbar.make(
                                binding.getRoot(),
                                "Chưa ghi nhận chuyển khoản. Hệ thống vẫn đang tự động theo dõi.",
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                }
            }

            @Override
            public void onError(String message) {
                isCheckingStatus = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                binding.btnCheckStatus.setEnabled(true);
                binding.tvQrStatusText.setText("Đang tự động kiểm tra thanh toán...");
                if (userTriggered) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmCancelPayment() {
        if (currentPayment == null) {
            openOrderDetailAndFinish(false);
            return;
        }
        stopTimers();
        binding.btnConfirmCancelPayment.setEnabled(false);
        binding.btnKeepPayment.setEnabled(false);
        binding.btnCheckStatus.setEnabled(false);

        PayOsRepository.cancelPayment(
                this,
                currentPayment,
                "Khach hang huy thanh toan QR",
                new PayOsRepository.PaymentCallback() {
                    @Override
                    public void onSuccess(PaymentEntity updated) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        boolean paid = PayOsPaymentInfo.STATUS_PAID.equalsIgnoreCase(updated.getStatus());
                        openOrderDetailAndFinish(paid);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        binding.btnConfirmCancelPayment.setEnabled(true);
                        binding.btnKeepPayment.setEnabled(true);
                        binding.btnCheckStatus.setEnabled(true);
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        startTimers();
                    }
                }
        );
    }

    private void openCheckoutUrlInBrowser() {
        if (currentPayment == null || currentPayment.getCheckoutUrl() == null
                || currentPayment.getCheckoutUrl().trim().isEmpty()) {
            return;
        }
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentPayment.getCheckoutUrl()));
            startActivity(browserIntent);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Không tìm thấy trình duyệt để mở trang PayOS", Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void handleBackNavigation() {
        if (isNewOrder) {
            openOrderDetailAndFinish(false);
        } else {
            finish();
        }
    }

    private void openOrderDetailAndFinish(boolean markSuccessBanner) {
        if (hasCompletedTransition) {
            return;
        }
        hasCompletedTransition = true;
        stopTimers();

        if (markSuccessBanner) {
            String totalSuffix = currentOrder != null
                    ? " (" + FormatUtils.formatVnd(currentOrder.getTotalAmount()) + ")"
                    : "";
            NotificationRepository.addNotification(
                    this,
                    NotificationItem.Type.PAYMENT,
                    "Thanh toán thành công",
                    "Đơn hàng #" + orderId + totalSuffix + " đã được thanh toán qua VietQR.",
                    orderId
            );
        }

        if (!isNewOrder && !markSuccessBanner) {
            finish();
            return;
        }
        Intent detailIntent = new Intent(this, OrderDetailActivity.class);
        detailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        detailIntent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
        detailIntent.putExtra(Constants.EXTRA_IS_NEW_ORDER, markSuccessBanner);
        startActivity(detailIntent);
        finish();
    }

    private void showLoadingState() {
        binding.llQrLoading.setVisibility(View.VISIBLE);
        binding.llQrError.setVisibility(View.GONE);
        binding.scrollQrContent.setVisibility(View.GONE);
        binding.dividerQrBottom.setVisibility(View.GONE);
        binding.llQrBottomBar.setVisibility(View.GONE);
    }

    private void showActiveQrState() {
        binding.llQrLoading.setVisibility(View.GONE);
        binding.llQrError.setVisibility(View.GONE);
        binding.scrollQrContent.setVisibility(View.VISIBLE);
        binding.dividerQrBottom.setVisibility(View.VISIBLE);
        binding.llQrBottomBar.setVisibility(View.VISIBLE);
        showCancelConfirm(false);
    }

    private void showErrorState(String title, String message) {
        binding.llQrLoading.setVisibility(View.GONE);
        binding.llQrError.setVisibility(View.VISIBLE);
        binding.scrollQrContent.setVisibility(View.GONE);
        binding.dividerQrBottom.setVisibility(View.GONE);
        binding.llQrBottomBar.setVisibility(View.GONE);
        binding.tvQrErrorTitle.setText(title);
        binding.tvQrErrorMessage.setText(message);
    }

    private void showCancelConfirm(boolean show) {
        TransitionManager.beginDelayedTransition(binding.qrContentContainer);
        binding.btnCancelPayment.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.llCancelConfirmBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startTimers() {
        if (isPollingActive) {
            return;
        }
        isPollingActive = true;
        mainHandler.removeCallbacks(countdownRunnable);
        mainHandler.removeCallbacks(pollStatusRunnable);
        mainHandler.post(countdownRunnable);
        mainHandler.postDelayed(pollStatusRunnable, Constants.PAYOS_POLL_INTERVAL_MS);
    }

    private void stopTimers() {
        isPollingActive = false;
        mainHandler.removeCallbacks(countdownRunnable);
        mainHandler.removeCallbacks(pollStatusRunnable);
    }

    private void copyToClipboard(String label, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text.trim()));
            Snackbar.make(binding.getRoot(), "Đã sao chép " + label.toLowerCase(Locale.getDefault()), Snackbar.LENGTH_SHORT)
                    .show();
        }
    }
}
