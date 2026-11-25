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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen Voucher Wallet & Checkout/Cart Voucher Picker supporting promo code entry,
 * segmented filtering, and instant ticket claiming/selection with custom top notifications.
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
                                voucher.getCode()
                        );
                        loadVouchers();
                        InAppNotifier.show(
                                binding.getRoot(),
                                AppNotification.TYPE_VOUCHER,
                                "Đã lưu mã ưu đãi",
                                "Đã lưu mã " + voucher.getCode() + " vào kho voucher",
                                voucher.getCode(),
                                null,
                                null
                        );
                    }

                    @Override
                    public void onSelectOrUseVoucher(Voucher voucher) {
                        VoucherRepository.claimByCode(
                                VoucherWalletActivity.this,
                                userSession.getUsername(),
                                voucher.getCode()
                        );
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

        loadVouchers();
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
                    "Vui lòng nhập mã voucher (VD: TECHNEW15 hoặc VIP20)"
            );
            return;
        }
        ClaimResult result = VoucherRepository.claimByCode(this, userSession.getUsername(), raw);
        Voucher matched = VoucherRepository.findByCode(raw);
        if (result == ClaimResult.NOT_FOUND || matched == null) {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ALERT,
                    "Mã không hợp lệ",
                    "Mã \"" + raw + "\" không tồn tại"
            );
            return;
        }
        if (result == ClaimResult.ALREADY_USED) {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ALERT,
                    "Mã đã sử dụng",
                    "Mã \"" + raw + "\" đã được sử dụng trước đó"
            );
            return;
        }
        if (result == ClaimResult.EXPIRED) {
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ALERT,
                    "Mã hết hạn",
                    "Mã \"" + raw + "\" đã hết hạn"
            );
            return;
        }

        binding.etPromoCode.setText("");
        loadVouchers();

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
            String msg = result == ClaimResult.CLAIMED
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

    private void loadVouchers() {
        VoucherRepository.getVouchersForUser(this, userSession.getUsername(), items -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            allItems.clear();
            allItems.addAll(items);
            applyFilter();
        });
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
