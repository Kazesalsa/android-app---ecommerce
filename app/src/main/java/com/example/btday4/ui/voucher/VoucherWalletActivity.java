package com.example.btday4.ui.voucher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.ClaimResult;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.ActivityVoucherWalletBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.Voucher;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.UiUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen Voucher Wallet & Checkout/Cart Voucher Picker synchronized live with Cloud Firestore
 * (`vouchers` and `users/{username}/user_vouchers`).
 */
public class VoucherWalletActivity extends AppCompatActivity {

    private enum FilterTab {
        ALL,
        SAVED,
        FREESHIP
    }

    private ActivityVoucherWalletBinding binding;
    private UserSession userSession;
    private VoucherAdapter adapter;
    private ListenerRegistration vouchersListener;

    private boolean pickerMode = false;
    private long orderSubtotal = 0L;
    private String selectedCode = "";
    private FilterTab activeTab = FilterTab.ALL;
    private final List<VoucherItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);

        userSession = new UserSession(this);
        pickerMode = getIntent().getBooleanExtra(Constants.EXTRA_PICKER_MODE, false);
        orderSubtotal = Math.max(0L, getIntent().getLongExtra(Constants.EXTRA_ORDER_SUBTOTAL, 0L));
        String initialVoucher = getIntent().getStringExtra(Constants.EXTRA_VOUCHER_CODE);
        selectedCode = initialVoucher == null ? "" : initialVoucher.trim().toUpperCase(Locale.US);

        UiUtils.applyEdgeToEdgeBars(
                binding.voucherWalletRoot,
                binding.toolbarVouchers,
                pickerMode ? binding.llPickerBottomBar : null
        );

        binding.toolbarVouchers.setTitle(pickerMode ? R.string.voucher_picker_title : R.string.voucher_wallet_title);
        binding.toolbarVouchers.setNavigationOnClickListener(v -> finish());
        binding.llPickerBottomBar.setVisibility(pickerMode ? View.VISIBLE : View.GONE);

        long baseShip = FormatUtils.computeShippingFee(orderSubtotal);
        adapter = new VoucherAdapter(
                pickerMode,
                orderSubtotal,
                baseShip,
                selectedCode,
                new VoucherAdapter.VoucherActionListener() {
                    @Override
                    public void onClaimVoucher(Voucher voucher) {
                        VoucherRepository.claimByCode(
                                VoucherWalletActivity.this,
                                userSession.getUsername(),
                                voucher.getCode(),
                                outcome -> {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    if (outcome.result == ClaimResult.CLAIMED
                                            || outcome.result == ClaimResult.ALREADY_CLAIMED) {
                                        InAppNotifier.show(
                                                binding.getRoot(),
                                                AppNotification.TYPE_VOUCHER,
                                                "Đã lưu mã ưu đãi",
                                                "Đã lưu mã " + voucher.getCode() + " vào kho voucher",
                                                voucher.getCode(),
                                                null,
                                                null
                                        );
                                    } else {
                                        showClaimError(voucher.getCode(), outcome.result);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onSelectOrUseVoucher(Voucher voucher) {
                        VoucherRepository.claimByCode(
                                VoucherWalletActivity.this,
                                userSession.getUsername(),
                                voucher.getCode(),
                                outcome -> {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    if (outcome.result != ClaimResult.CLAIMED
                                            && outcome.result != ClaimResult.ALREADY_CLAIMED) {
                                        showClaimError(voucher.getCode(), outcome.result);
                                        return;
                                    }
                                    if (pickerMode) {
                                        returnSelectedCode(voucher.getCode());
                                    } else {
                                        VoucherRepository.setSelectedCartVoucherCode(
                                                VoucherWalletActivity.this,
                                                userSession.getUsername(),
                                                voucher.getCode()
                                        );
                                        Intent intent = new Intent(VoucherWalletActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        intent.putExtra(Constants.EXTRA_OPEN_PRODUCTS, true);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                        );
                    }
                }
        );

        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVouchers.setAdapter(adapter);

        binding.tabVoucherAll.setOnClickListener(v -> selectTab(FilterTab.ALL));
        binding.tabVoucherSaved.setOnClickListener(v -> selectTab(FilterTab.SAVED));
        binding.tabVoucherFreeShip.setOnClickListener(v -> selectTab(FilterTab.FREESHIP));

        binding.btnApplyPromoCode.setOnClickListener(v -> handlePromoCodeSubmit());
        binding.etPromoCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handlePromoCodeSubmit();
                return true;
            }
            return false;
        });

        binding.btnClearSelectedVoucher.setOnClickListener(v -> returnSelectedCode(""));
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningVouchers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListeningVouchers();
    }

    private void startListeningVouchers() {
        stopListeningVouchers();
        vouchersListener = VoucherRepository.listenVouchersForUser(
                userSession.getUsername(),
                items -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    allItems.clear();
                    allItems.addAll(items);
                    applyFilter();
                }
        );
    }

    private void stopListeningVouchers() {
        if (vouchersListener != null) {
            vouchersListener.remove();
            vouchersListener = null;
        }
    }

    private void handlePromoCodeSubmit() {
        String raw = binding.etPromoCode.getText() == null
                ? ""
                : binding.etPromoCode.getText().toString().trim().toUpperCase(Locale.US);
        if (raw.isEmpty()) {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ALERT,
                    "Chưa nhập mã",
                    "Vui lòng nhập mã voucher từ hệ thống"
            );
            return;
        }

        binding.btnApplyPromoCode.setEnabled(false);
        VoucherRepository.claimByCode(this, userSession.getUsername(), raw, outcome -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.btnApplyPromoCode.setEnabled(true);

            Voucher matched = outcome.voucher;
            if (outcome.result != ClaimResult.CLAIMED && outcome.result != ClaimResult.ALREADY_CLAIMED) {
                showClaimError(raw, outcome.result);
                return;
            }
            if (matched == null) {
                showClaimError(raw, ClaimResult.NOT_FOUND);
                return;
            }

            binding.etPromoCode.setText("");

            if (pickerMode) {
                long ship = FormatUtils.computeShippingFee(orderSubtotal);
                if (matched.computeDiscount(orderSubtotal, ship) > 0L) {
                    returnSelectedCode(matched.getCode());
                    return;
                }
                InAppNotifier.show(
                        binding.getRoot(),
                        AppNotification.TYPE_VOUCHER,
                        "Đã lưu mã " + matched.getCode(),
                        "Đơn hàng hiện tại chưa đủ điều kiện áp dụng mã này.",
                        matched.getCode(),
                        null,
                        null
                );
            } else {
                String msg = outcome.result == ClaimResult.CLAIMED
                        ? "Đã mở khóa & lưu mã " + matched.getCode() + " vào kho voucher"
                        : "Mã " + matched.getCode() + " đã có sẵn trong kho";
                InAppNotifier.show(
                        binding.getRoot(),
                        AppNotification.TYPE_VOUCHER,
                        "Kho Voucher",
                        msg,
                        matched.getCode(),
                        null,
                        null
                );
            }
        });
    }

    private void showClaimError(String code, ClaimResult result) {
        String title;
        String message;
        switch (result) {
            case ALREADY_USED:
                title = "Mã đã sử dụng";
                message = "Mã \"" + code + "\" đã được sử dụng trước đó";
                break;
            case EXPIRED:
                title = "Mã hết hạn";
                message = "Mã \"" + code + "\" đã hết hạn";
                break;
            case INACTIVE:
                title = "Mã tạm ngưng";
                message = "Mã \"" + code + "\" hiện không còn hiệu lực";
                break;
            case OUT_OF_STOCK:
                title = "Hết lượt sử dụng";
                message = "Mã \"" + code + "\" đã đạt giới hạn lượt dùng";
                break;
            case ERROR:
                title = "Không thể kết nối";
                message = "Không thể kiểm tra mã trên Firebase, vui lòng thử lại";
                break;
            case NOT_FOUND:
            default:
                title = "Mã không hợp lệ";
                message = "Mã \"" + code + "\" không tồn tại trên hệ thống";
                break;
        }
        InAppNotifier.show(binding.getRoot(), AppNotification.TYPE_ALERT, title, message);
    }

    private void returnSelectedCode(String code) {
        Intent data = new Intent();
        data.putExtra(Constants.EXTRA_VOUCHER_CODE, code);
        setResult(RESULT_OK, data);
        finish();
    }

    private void selectTab(FilterTab tab) {
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        styleSegment(binding.tabVoucherAll, activeTab == FilterTab.ALL);
        styleSegment(binding.tabVoucherSaved, activeTab == FilterTab.SAVED);
        styleSegment(binding.tabVoucherFreeShip, activeTab == FilterTab.FREESHIP);
        applyFilter();
    }

    private void styleSegment(TextView view, boolean active) {
        view.setBackgroundResource(active ? R.drawable.bg_segmented_active : 0);
        view.setTextColor(ContextCompat.getColor(this, active ? R.color.text_primary : R.color.text_secondary));
        view.setTypeface(ResourcesCompat.getFont(
                this,
                active ? R.font.be_vietnam_pro_bold : R.font.be_vietnam_pro_medium
        ));
    }

    private void applyFilter() {
        List<VoucherItem> filtered = new ArrayList<>();
        for (VoucherItem item : allItems) {
            if (activeTab == FilterTab.SAVED && !item.claimed) {
                continue;
            }
            if (activeTab == FilterTab.FREESHIP && !item.voucher.isFreeShipType()) {
                continue;
            }
            filtered.add(item);
        }
        adapter.submitList(filtered, selectedCode);
        binding.llVouchersEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
