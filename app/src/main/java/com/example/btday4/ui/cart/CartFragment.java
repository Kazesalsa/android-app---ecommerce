package com.example.btday4.ui.cart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.FragmentCartBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.CartItem;
import com.example.btday4.model.Voucher;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.ui.checkout.CheckoutActivity;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.products.ProductDetailActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.InAppNotifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shopping cart screen with per-item selection, free-shipping threshold cue, live voucher picker
 * integration, Undo-on-remove Snackbar, and checkout of selected items.
 */
public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartAdapter adapter;
    private UserSession userSession;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final List<VoucherItem> voucherItems = new ArrayList<>();
    /** Items are selected by default; only explicit opt-outs are tracked. */
    private final Set<String> deselectedIds = new HashSet<>();

    private String selectedVoucherCode = "";
    private String appliedVoucherCode = "";
    private boolean userExplicitlyClearedVoucher = false;

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> refreshCart()
    );

    private final ActivityResultLauncher<Intent> voucherPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!isAdded() || userSession == null) {
                    return;
                }
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String picked = result.getData().getStringExtra(Constants.EXTRA_VOUCHER_CODE);
                    selectedVoucherCode = picked == null ? "" : picked.trim().toUpperCase(Locale.US);
                    userExplicitlyClearedVoucher = selectedVoucherCode.isEmpty();
                    VoucherRepository.setSelectedCartVoucherCode(
                            requireContext(),
                            userSession.getUsername(),
                            selectedVoucherCode
                    );
                    loadVouchersAndRefreshSummary();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userSession = new UserSession(requireContext());
        selectedVoucherCode = VoucherRepository.getSelectedCartVoucherCode(
                requireContext(),
                userSession.getUsername()
        );

        adapter = new CartAdapter(new CartAdapter.CartActionListener() {
            @Override
            public void onItemClick(CartItem item) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.getProduct().getId());
                startActivity(intent);
            }

            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                if (newQuantity <= 0) {
                    removeItemWithUndo(item);
                    return;
                }
                BehaviorTracker tracker = BehaviorTracker.getInstance();
                if (tracker != null) {
                    tracker.trackAction(
                            "MainActivity:Cart",
                            BehaviorTracker.EVENT_UPDATE_CART,
                            item.getProduct().getId(),
                            "qty=" + newQuantity
                    );
                }
                CartRepository.updateQuantity(
                        requireContext(),
                        item.getProduct().getId(),
                        newQuantity,
                        CartFragment.this::refreshCart
                );
            }

            @Override
            public boolean isSelected(CartItem item) {
                return !deselectedIds.contains(item.getProduct().getId());
            }

            @Override
            public void onSelectionChanged(CartItem item, boolean selected) {
                String productId = item.getProduct().getId();
                if (selected) {
                    deselectedIds.remove(productId);
                } else {
                    deselectedIds.add(productId);
                }
                updateSelectionSummary();
            }

            @Override
            public void onRemoveItem(CartItem item) {
                removeItemWithUndo(item);
            }
        });

        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(adapter);

        binding.btnBrowseProducts.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProductsTab();
            }
        });

        binding.btnCartOpenWallet.setOnClickListener(v -> openVoucherPicker());
        binding.rowCartVoucher.setOnClickListener(v -> openVoucherPicker());

        binding.cbSelectAll.setOnClickListener(v -> {
            deselectedIds.clear();
            if (!binding.cbSelectAll.isChecked()) {
                for (CartItem item : cartItems) {
                    deselectedIds.add(item.getProduct().getId());
                }
            }
            adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            updateSelectionSummary();
        });

        binding.btnCheckout.setOnClickListener(v -> {
            ArrayList<String> selectedIds = new ArrayList<>();
            for (CartItem item : getSelectedItems()) {
                selectedIds.add(item.getProduct().getId());
            }
            if (selectedIds.isEmpty()) {
                return;
            }
            BehaviorTracker tracker = BehaviorTracker.getInstance();
            if (tracker != null) {
                tracker.trackAction(
                        "MainActivity:Cart",
                        BehaviorTracker.EVENT_CHECKOUT_START,
                        String.join(",", selectedIds),
                        "items=" + selectedIds.size()
                );
            }
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putStringArrayListExtra(Constants.EXTRA_SELECTED_PRODUCT_IDS, selectedIds);
            if (!appliedVoucherCode.isEmpty()) {
                intent.putExtra(Constants.EXTRA_VOUCHER_CODE, appliedVoucherCode);
            } else if (userExplicitlyClearedVoucher) {
                intent.putExtra(Constants.EXTRA_VOUCHER_CODE, "");
            }
            checkoutLauncher.launch(intent);
        });

        if (!isHidden()) {
            refreshCart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            refreshCart();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshCart();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openVoucherPicker() {
        long subtotal = CartRepository.calculateSubtotal(getSelectedItems());
        Intent intent = new Intent(requireContext(), VoucherWalletActivity.class);
        intent.putExtra(Constants.EXTRA_PICKER_MODE, true);
        intent.putExtra(Constants.EXTRA_ORDER_SUBTOTAL, subtotal);
        intent.putExtra(Constants.EXTRA_VOUCHER_CODE, appliedVoucherCode);
        voucherPickerLauncher.launch(intent);
    }

    private void removeItemWithUndo(CartItem item) {
        final String productId = item.getProduct().getId();
        final String productName = item.getProduct().getName();
        final int removedQty = Math.max(1, item.getQuantity());

        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.trackAction(
                    "MainActivity:Cart",
                    BehaviorTracker.EVENT_REMOVE_CART,
                    productId,
                    productName
            );
        }

        CartRepository.removeItem(requireContext(), productId, () -> {
            refreshCart();
            if (!isAdded() || binding == null) {
                return;
            }
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_CART,
                    "Đã xóa khỏi giỏ hàng",
                    "Đã xóa " + productName,
                    productId,
                    "Hoàn tác",
                    () -> {
                        if (!isAdded()) {
                            return;
                        }
                        CartRepository.addItem(requireContext(), productId, removedQty, added -> refreshCart());
                    }
            );
        });
    }

    public void refreshCart() {
        if (!isAdded() || binding == null || adapter == null) {
            return;
        }
        if (userSession == null) {
            userSession = new UserSession(requireContext());
        }
        if (!userExplicitlyClearedVoucher) {
            String stored = VoucherRepository.getSelectedCartVoucherCode(
                    requireContext(),
                    userSession.getUsername()
            );
            if (!stored.isEmpty()) {
                selectedVoucherCode = stored;
            }
        }

        CartRepository.getItems(requireContext(), items -> {
            if (!isAdded() || binding == null) {
                return;
            }
            cartItems.clear();
            cartItems.addAll(items);
            Set<String> cartIds = new HashSet<>();
            int totalQty = 0;
            for (CartItem item : cartItems) {
                cartIds.add(item.getProduct().getId());
                totalQty += item.getQuantity();
            }
            deselectedIds.retainAll(cartIds);
            adapter.submitList(new ArrayList<>(cartItems));

            boolean isEmpty = cartItems.isEmpty();
            binding.tvCartSummary.setText("(" + totalQty + " sản phẩm)");
            binding.llCartEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.llCartTotalBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.llShippingProgress.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            loadVouchersAndRefreshSummary();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCartBadge();
            }
        });
    }

    private void loadVouchersAndRefreshSummary() {
        if (!isAdded() || userSession == null) {
            return;
        }
        VoucherRepository.getVouchersForUser(requireContext(), userSession.getUsername(), items -> {
            if (!isAdded() || binding == null) {
                return;
            }
            voucherItems.clear();
            voucherItems.addAll(items);
            updateSelectionSummary();
        });
    }

    private List<CartItem> getSelectedItems() {
        List<CartItem> selected = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (!deselectedIds.contains(item.getProduct().getId())
                    && CartRepository.maxPurchasable(item.getProduct()) > 0) {
                selected.add(item);
            }
        }
        return selected;
    }

    @Nullable
    private VoucherItem findAvailableItem(String code, long nowMillis) {
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

    /** Recomputes total, voucher discount, checkout button, select-all state and shipping progress. */
    private void updateSelectionSummary() {
        if (binding == null || cartItems.isEmpty()) {
            return;
        }
        List<CartItem> selected = getSelectedItems();
        int selectedQty = 0;
        for (CartItem item : selected) {
            selectedQty += item.getQuantity();
        }
        long subtotal = CartRepository.calculateSubtotal(selected);
        long baseShip = FormatUtils.computeShippingFee(subtotal);
        long now = System.currentTimeMillis();

        appliedVoucherCode = "";
        Voucher activeVoucher = null;
        VoucherItem chosenItem = findAvailableItem(selectedVoucherCode, now);
        if (chosenItem != null) {
            activeVoucher = chosenItem.voucher;
        } else if (!userExplicitlyClearedVoucher) {
            activeVoucher = VoucherRepository.findBestApplicable(voucherItems, subtotal, baseShip, now);
        }

        long discount = 0L;
        if (activeVoucher != null && subtotal > 0L) {
            discount = activeVoucher.computeDiscount(subtotal, baseShip);
            if (discount > 0L) {
                appliedVoucherCode = activeVoucher.getCode();
                binding.tvCartVoucherStatus.setText(
                        activeVoucher.getCode() + " · -" + FormatUtils.formatVnd(discount)
                );
                binding.tvCartVoucherStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary)
                );
            } else if (activeVoucher.amountNeededToUnlock(subtotal) > 0L) {
                binding.tvCartVoucherStatus.setText(
                        "Mua thêm " + FormatUtils.formatVnd(activeVoucher.amountNeededToUnlock(subtotal))
                                + " dùng " + activeVoucher.getCode()
                );
                binding.tvCartVoucherStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.teal_trust)
                );
            } else {
                binding.tvCartVoucherStatus.setText("Chọn hoặc nhập mã");
                binding.tvCartVoucherStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_muted)
                );
            }
        } else {
            binding.tvCartVoucherStatus.setText(
                    userExplicitlyClearedVoucher ? "Không dùng mã" : "Chọn hoặc nhập mã"
            );
            binding.tvCartVoucherStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_muted)
            );
        }

        long displayedTotal = subtotal;
        if (subtotal > 0L && activeVoucher != null && discount > 0L) {
            if (activeVoucher.isFreeShipType()) {
                binding.tvCartSavedNote.setVisibility(View.VISIBLE);
                binding.tvCartSavedNote.setText("Miễn phí vận chuyển");
            } else {
                displayedTotal = Math.max(Constants.MIN_PAYABLE_TOTAL, subtotal - discount);
                binding.tvCartSavedNote.setVisibility(View.VISIBLE);
                binding.tvCartSavedNote.setText("Tiết kiệm " + FormatUtils.formatVnd(discount));
            }
        } else {
            binding.tvCartSavedNote.setVisibility(View.GONE);
        }

        binding.tvCartGrandTotal.setText(FormatUtils.formatVnd(displayedTotal));
        binding.btnCheckout.setText("Mua hàng (" + selectedQty + ")");
        binding.btnCheckout.setEnabled(!selected.isEmpty());
        binding.cbSelectAll.setChecked(deselectedIds.isEmpty());

        long remaining = Constants.FREE_SHIPPING_THRESHOLD - subtotal;
        int teal = ContextCompat.getColor(requireContext(), R.color.teal_trust);
        binding.tvShippingProgressText.setTextColor(teal);
        binding.progressFreeShipping.setIndicatorColor(teal);
        if (remaining <= 0L) {
            binding.tvShippingProgressText.setText("Đơn hàng đã đủ điều kiện miễn phí vận chuyển");
            binding.progressFreeShipping.setVisibility(View.GONE);
        } else {
            int pct = (int) Math.min(100L, (subtotal * 100L) / Constants.FREE_SHIPPING_THRESHOLD);
            binding.tvShippingProgressText.setText(
                    "Mua thêm " + FormatUtils.formatVnd(remaining) + " để miễn phí vận chuyển"
            );
            binding.progressFreeShipping.setVisibility(View.VISIBLE);
            binding.progressFreeShipping.setProgress(pct);
        }
    }
}
