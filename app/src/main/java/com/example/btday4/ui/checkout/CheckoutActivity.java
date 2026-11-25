package com.example.btday4.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.ActivityCheckoutBinding;
import com.example.btday4.model.CartItem;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.model.Voucher;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.ui.orders.OrderDetailActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-bleed checkout flow with inline expandable shipping address editor,
 * voucher redemption,
 * payment method selection (COD & PayOS VietQR), and transparent price
 * breakdown.
 */
public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private UserSession userSession;
    private OrderItemAdapter itemAdapter;

    private final List<OrderItemEntity> checkoutItems = new ArrayList<>();
    private final List<VoucherItem> voucherItems = new ArrayList<>();
    private boolean isCartCheckout = true;
    private boolean isEditingShipping = true;

    private String recipientName = "";
    private String recipientPhone = "";
    private String shippingAddress = "";
    private PaymentMethod selectedPaymentMethod = PaymentMethod.COD;

    private String selectedVoucherCode = "";
    private String appliedVoucherCode = "";
    private long appliedDiscount = 0L;
    private long currentSubtotal = 0L;
    private boolean userExplicitlyClearedVoucher = false;

    private final ActivityResultLauncher<Intent> voucherPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String picked = result.getData().getStringExtra(Constants.EXTRA_VOUCHER_CODE);
                    selectedVoucherCode = picked == null ? "" : picked.trim().toUpperCase(Locale.US);
                    userExplicitlyClearedVoucher = selectedVoucherCode.isEmpty();
                    if (isCartCheckout) {
                        VoucherRepository.setSelectedCartVoucherCode(
                                this,
                                userSession.getUsername(),
                                selectedVoucherCode);
                    }
                    loadVouchersAndRefreshPricing();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(
                binding.checkoutRoot,
                binding.toolbarCheckout,
                binding.llCheckoutBottomBar);

        userSession = new UserSession(this);

        if (getIntent().hasExtra(Constants.EXTRA_VOUCHER_CODE)) {
            String extraCode = getIntent().getStringExtra(Constants.EXTRA_VOUCHER_CODE);
            selectedVoucherCode = extraCode == null ? "" : extraCode.trim().toUpperCase(Locale.US);
            userExplicitlyClearedVoucher = selectedVoucherCode.isEmpty();
        } else {
            selectedVoucherCode = VoucherRepository.getSelectedCartVoucherCode(
                    this,
                    userSession.getUsername());
        }

        binding.toolbarCheckout.setNavigationOnClickListener(v -> finish());
        binding.btnPlaceOrder.setEnabled(false);

        UiUtils.clearErrorOnType(binding.etShippingName, binding.tilShippingName);
        UiUtils.clearErrorOnType(binding.etShippingPhone, binding.tilShippingPhone);
        UiUtils.clearErrorOnType(binding.etShippingAddress, binding.tilShippingAddress);

        itemAdapter = new OrderItemAdapter(null);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(itemAdapter);

        binding.btnToggleShippingForm.setOnClickListener(v -> {
            if (isEditingShipping) {
                if (hasValidShippingState()) {
                    setShippingEditMode(false, true);
                }
            } else {
                prefillShippingInputs();
                setShippingEditMode(true, true);
            }
        });

        binding.btnApplyShipping.setOnClickListener(v -> {
            if (validateAndCaptureShippingInputs()) {
                if (binding.cbSaveDefaultShipping.isChecked()) {
                    userSession.updateShippingInfo(recipientName, recipientPhone, shippingAddress, null);
                }
                setShippingEditMode(false, true);
            }
        });

        binding.cardCheckoutVoucher.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoucherWalletActivity.class);
            intent.putExtra(Constants.EXTRA_PICKER_MODE, true);
            intent.putExtra(Constants.EXTRA_ORDER_SUBTOTAL, currentSubtotal);
            intent.putExtra(Constants.EXTRA_VOUCHER_CODE, appliedVoucherCode);
            voucherPickerLauncher.launch(intent);
        });

        binding.optionCod.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.COD));
        binding.optionQr.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.VIETQR));
        binding.btnPlaceOrder.setOnClickListener(v -> submitOrder());
        selectPaymentMethod(PaymentMethod.COD);

        loadUserShippingDefaults();
        loadCheckoutItems();
    }

    private void loadUserShippingDefaults() {
        userSession.loadCurrentUser(user -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (user != null) {
                recipientName = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
                recipientPhone = user.getPhone() == null ? "" : user.getPhone().trim();
                shippingAddress = user.getAddress() == null ? "" : user.getAddress().trim();
            } else {
                recipientName = userSession.getDisplayName();
            }
            prefillShippingInputs();
            setShippingEditMode(!hasValidShippingState(), false);
        });
    }

    private void loadCheckoutItems() {
        String directProductId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        int directQuantity = Math.max(1, getIntent().getIntExtra(Constants.EXTRA_QUANTITY, 1));

        if (ValidationUtils.isNotBlank(directProductId)) {
            isCartCheckout = false;
            ProductRepository.findById(this, directProductId, product -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (product == null) {
                    finish();
                    return;
                }
                checkoutItems.clear();
                checkoutItems.add(new CartItem(product, directQuantity).toOrderItemSnapshot(""));
                loadVouchersAndRefreshPricing();
            });
        } else {
            isCartCheckout = true;
            List<String> selectedIds = getIntent().getStringArrayListExtra(Constants.EXTRA_SELECTED_PRODUCT_IDS);
            CartRepository.getItems(this, items -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                checkoutItems.clear();
                for (CartItem cartItem : items) {
                    if (selectedIds == null || selectedIds.contains(cartItem.getProduct().getId())) {
                        checkoutItems.add(cartItem.toOrderItemSnapshot(""));
                    }
                }
                if (checkoutItems.isEmpty()) {
                    finish();
                    return;
                }
                loadVouchersAndRefreshPricing();
            });
        }
    }

    private void loadVouchersAndRefreshPricing() {
        VoucherRepository.getVouchersForUser(this, userSession.getUsername(), items -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            voucherItems.clear();
            voucherItems.addAll(items);
            bindItemsAndPricing();
        });
    }

    @Nullable
    private VoucherItem findAvailableVoucher(String code, long nowMillis) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (VoucherItem item : voucherItems) {
            if (item.voucher.getCode().equalsIgnoreCase(code) && item.isAvailableToUse(nowMillis)) {
                return item;
            }
        }
        return null;
    }

    private boolean hasValidShippingState() {
        return ValidationUtils.isNotBlank(recipientName)
                && ValidationUtils.isValidPhone(recipientPhone)
                && shippingAddress.length() >= 6;
    }

    private void prefillShippingInputs() {
        binding.etShippingName.setText(recipientName);
        binding.etShippingPhone.setText(recipientPhone);
        binding.etShippingAddress.setText(shippingAddress);
    }

    private void setShippingEditMode(boolean editing, boolean animate) {
        isEditingShipping = editing;
        if (animate) {
            TransitionManager.beginDelayedTransition(binding.checkoutContent);
        }
        if (editing) {
            binding.llShippingSummary.setVisibility(View.GONE);
            binding.llShippingForm.setVisibility(View.VISIBLE);
            binding.btnToggleShippingForm.setVisibility(hasValidShippingState() ? View.VISIBLE : View.GONE);
            binding.btnToggleShippingForm.setText("Thu gọn");
        } else {
            binding.tvRecipientName.setText(recipientName);
            binding.tvRecipientPhone.setText(recipientPhone);
            binding.tvShippingAddress.setText(shippingAddress);
            binding.llShippingSummary.setVisibility(View.VISIBLE);
            binding.llShippingForm.setVisibility(View.GONE);
            binding.btnToggleShippingForm.setVisibility(View.VISIBLE);
            binding.btnToggleShippingForm.setText("Thay đổi");
        }
    }

    private boolean validateAndCaptureShippingInputs() {
        String name = binding.etShippingName.getText() == null
                ? ""
                : binding.etShippingName.getText().toString().trim();
        String phone = binding.etShippingPhone.getText() == null
                ? ""
                : ValidationUtils.normalizePhone(binding.etShippingPhone.getText().toString());
        String address = binding.etShippingAddress.getText() == null
                ? ""
                : binding.etShippingAddress.getText().toString().trim();

        boolean valid = true;
        if (!ValidationUtils.isNotBlank(name)) {
            binding.tilShippingName.setError("Vui lòng nhập họ tên người nhận");
            valid = false;
        } else {
            binding.tilShippingName.setError(null);
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilShippingPhone.setError("Số điện thoại không hợp lệ (VD: 0912345678)");
            valid = false;
        } else {
            binding.tilShippingPhone.setError(null);
        }
        if (address.length() < 6) {
            binding.tilShippingAddress.setError("Vui lòng nhập địa chỉ giao hàng cụ thể");
            valid = false;
        } else {
            binding.tilShippingAddress.setError(null);
        }

        if (!valid) {
            binding.scrollCheckout.smoothScrollTo(0, 0);
            return false;
        }

        recipientName = name;
        recipientPhone = phone;
        shippingAddress = address;
        return true;
    }

    private void bindItemsAndPricing() {
        itemAdapter.submitItems(checkoutItems);

        int totalQuantity = 0;
        long subtotal = 0L;
        for (OrderItemEntity item : checkoutItems) {
            totalQuantity += item.getQuantity();
            subtotal += item.lineSubtotal();
        }
        currentSubtotal = subtotal;
        long shippingFee = FormatUtils.computeShippingFee(subtotal);
        long now = System.currentTimeMillis();

        Voucher activeVoucher = null;
        VoucherItem chosen = findAvailableVoucher(selectedVoucherCode, now);
        if (chosen != null) {
            activeVoucher = chosen.voucher;
        } else if (!userExplicitlyClearedVoucher) {
            activeVoucher = VoucherRepository.findBestApplicable(voucherItems, subtotal, shippingFee, now);
        }

        appliedVoucherCode = "";
        appliedDiscount = 0L;
        if (activeVoucher != null && subtotal > 0L) {
            long discount = activeVoucher.computeDiscount(subtotal, shippingFee);
            if (discount > 0L) {
                appliedVoucherCode = activeVoucher.getCode();
                appliedDiscount = discount;
            }
        }

        if (!appliedVoucherCode.isEmpty() && activeVoucher != null) {
            binding.tvCheckoutVoucherSubtitle.setText(
                    "Đã áp dụng " + appliedVoucherCode + " · " + activeVoucher.getTitle());
            binding.tvCheckoutVoucherSubtitle.setTextColor(
                    ContextCompat.getColor(this, R.color.teal_trust));
            binding.tvCheckoutVoucherBadge.setText("-" + FormatUtils.formatVnd(appliedDiscount));
            binding.tvCheckoutVoucherBadge.setBackgroundResource(R.drawable.bg_discount_tag);
            binding.tvCheckoutVoucherBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));

            binding.rowCheckoutDiscount.setVisibility(View.VISIBLE);
            binding.tvCheckoutDiscountLabel.setText("Voucher giảm giá (" + appliedVoucherCode + ")");
            binding.tvCheckoutDiscountValue.setText("-" + FormatUtils.formatVnd(appliedDiscount));

            binding.tvBottomSavedNote.setVisibility(View.VISIBLE);
            binding.tvBottomSavedNote.setText("Tiết kiệm " + FormatUtils.formatVnd(appliedDiscount));
        } else {
            binding.tvCheckoutVoucherSubtitle.setText(
                    userExplicitlyClearedVoucher
                            ? "Không sử dụng mã ưu đãi cho đơn này"
                            : "Chọn hoặc nhập mã ưu đãi cho đơn hàng");
            binding.tvCheckoutVoucherSubtitle.setTextColor(
                    ContextCompat.getColor(this, R.color.text_secondary));
            binding.tvCheckoutVoucherBadge.setText("Chọn mã");
            binding.tvCheckoutVoucherBadge.setBackgroundResource(R.drawable.bg_discount_tag);
            binding.tvCheckoutVoucherBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));

            binding.rowCheckoutDiscount.setVisibility(View.GONE);
            binding.tvBottomSavedNote.setVisibility(View.GONE);
        }

        long grandTotal = subtotal > 0L
                ? Math.max(Constants.MIN_PAYABLE_TOTAL, subtotal + shippingFee - appliedDiscount)
                : 0L;

        binding.tvCheckoutItemsHeader.setText("Sản phẩm đặt mua (" + totalQuantity + ")");
        binding.tvSubtotalLabel.setText("Tạm tính (" + totalQuantity + " sản phẩm)");
        binding.tvCheckoutSubtotal.setText(FormatUtils.formatVnd(subtotal));

        if (shippingFee == 0L) {
            binding.tvCheckoutShipping.setText(R.string.free_shipping);
            binding.tvCheckoutShipping.setTextColor(ContextCompat.getColor(this, R.color.teal_trust));
            binding.tvFreeShippingHint.setVisibility(View.GONE);
        } else {
            binding.tvCheckoutShipping.setText(FormatUtils.formatVnd(shippingFee));
            binding.tvCheckoutShipping.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.tvFreeShippingHint.setVisibility(View.VISIBLE);
        }

        String totalFormatted = FormatUtils.formatVnd(grandTotal);
        binding.tvCheckoutTotal.setText(totalFormatted);
        binding.tvBottomTotal.setText(totalFormatted);
        binding.btnPlaceOrder.setEnabled(!checkoutItems.isEmpty());
    }

    private void selectPaymentMethod(PaymentMethod method) {
        selectedPaymentMethod = method;
        boolean isCod = method == PaymentMethod.COD;
        binding.rbCod.setChecked(isCod);
        binding.rbQr.setChecked(!isCod);
        binding.optionCod.setBackgroundResource(
                isCod ? R.drawable.bg_option_selected : R.drawable.bg_option_default);
        binding.optionQr.setBackgroundResource(
                isCod ? R.drawable.bg_option_default : R.drawable.bg_option_selected);
        binding.btnPlaceOrder.setText(isCod ? R.string.btn_place_order : R.string.btn_place_order_qr);
    }

    private void submitOrder() {
        if (checkoutItems.isEmpty()) {
            return;
        }
        if (isEditingShipping) {
            if (!validateAndCaptureShippingInputs()) {
                return;
            }
            if (binding.cbSaveDefaultShipping.isChecked()) {
                userSession.updateShippingInfo(recipientName, recipientPhone, shippingAddress, null);
            }
        } else if (!hasValidShippingState()) {
            setShippingEditMode(true, true);
            validateAndCaptureShippingInputs();
            return;
        }

        String note = binding.etDeliveryNote.getText() == null
                ? ""
                : binding.etDeliveryNote.getText().toString().trim();

        binding.btnPlaceOrder.setEnabled(false);
        OrderRepository.placeOrder(
                this,
                userSession.getUsername(),
                recipientName,
                recipientPhone,
                shippingAddress,
                note,
                selectedPaymentMethod,
                checkoutItems,
                appliedVoucherCode,
                appliedDiscount,
                isCartCheckout,
                new OrderRepository.PlaceOrderCallback() {
                    @Override
                    public void onSuccess(OrderEntity createdOrder) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        BehaviorTracker tracker = BehaviorTracker.getInstance();
                        if (tracker != null) {
                            StringBuilder purchasedIds = new StringBuilder();
                            for (OrderItemEntity item : checkoutItems) {
                                if (purchasedIds.length() > 0) {
                                    purchasedIds.append(',');
                                }
                                purchasedIds.append(item.getProductId());
                            }
                            String voucherMeta = createdOrder.getVoucherCode().isEmpty()
                                    ? ""
                                    : "|voucher=" + createdOrder.getVoucherCode()
                                            + "|discount=" + createdOrder.getVoucherDiscount();
                            tracker.trackAction(
                                    "CheckoutActivity",
                                    BehaviorTracker.EVENT_PURCHASE,
                                    createdOrder.getId(),
                                    "method=" + selectedPaymentMethod.name()
                                            + "|total=" + createdOrder.getTotalAmount()
                                            + "|items=" + checkoutItems.size()
                                            + "|products=" + purchasedIds
                                            + voucherMeta);
                        }
                        String orderNotifTitle = selectedPaymentMethod == PaymentMethod.VIETQR
                                ? "Đơn hàng chờ thanh toán QR"
                                : "Đặt hàng thành công";
                        String orderNotifBody = "Mã đơn " + createdOrder.getId() + " · Tổng thanh toán "
                                + FormatUtils.formatVnd(createdOrder.getTotalAmount());
                        InAppNotifier.recordOnly(
                                CheckoutActivity.this,
                                com.example.btday4.model.AppNotification.TYPE_ORDER,
                                orderNotifTitle,
                                orderNotifBody,
                                createdOrder.getId());
                        setResult(RESULT_OK);
                        Intent nextIntent = selectedPaymentMethod == PaymentMethod.VIETQR
                                ? new Intent(CheckoutActivity.this, QrPaymentActivity.class)
                                : new Intent(CheckoutActivity.this, OrderDetailActivity.class);
                        nextIntent.putExtra(Constants.EXTRA_ORDER_ID, createdOrder.getId());
                        nextIntent.putExtra(Constants.EXTRA_IS_NEW_ORDER, true);
                        startActivity(nextIntent);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        binding.btnPlaceOrder.setEnabled(true);
                        com.example.btday4.util.InAppNotifier.show(
                                binding.getRoot(),
                                com.example.btday4.model.AppNotification.TYPE_ALERT,
                                "Không thể đặt hàng",
                                message);
                    }
                });
    }
}
