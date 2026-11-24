package com.example.btday4.ui.cart;

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
import com.example.btday4.databinding.FragmentCartBinding;
import com.example.btday4.model.CartItem;
import com.example.btday4.ui.checkout.CheckoutActivity;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.products.ProductDetailActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shopping cart screen backed by Room SQLite with per-item selection, free-shipping progress bar,
 * smooth quantity controls, and checkout of only the selected items.
 */
public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartAdapter adapter;
    private final List<CartItem> cartItems = new ArrayList<>();
    /** Items are selected by default; only explicit opt-outs are tracked. */
    private final Set<String> deselectedIds = new HashSet<>();

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> refreshCart()
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

        adapter = new CartAdapter(new CartAdapter.CartActionListener() {
            @Override
            public void onItemClick(CartItem item) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.getProduct().getId());
                startActivity(intent);
            }

            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
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
                CartRepository.removeItem(requireContext(), item.getProduct().getId(), CartFragment.this::refreshCart);
            }
        });

        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(adapter);

        binding.btnBrowseProducts.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProductsTab();
            }
        });

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
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putStringArrayListExtra(Constants.EXTRA_SELECTED_PRODUCT_IDS, selectedIds);
            checkoutLauncher.launch(intent);
        });

        refreshCart();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCart();
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

    public void refreshCart() {
        if (!isAdded() || binding == null || adapter == null) {
            return;
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
            updateSelectionSummary();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCartBadge();
            }
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

    /** Recomputes total, checkout button, select-all state and shipping progress for the selection. */
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

        binding.tvCartGrandTotal.setText(FormatUtils.formatVnd(subtotal));
        binding.btnCheckout.setText("Mua hàng (" + selectedQty + ")");
        binding.btnCheckout.setEnabled(!selected.isEmpty());
        binding.cbSelectAll.setChecked(deselectedIds.isEmpty());

        long remaining = Constants.FREE_SHIPPING_THRESHOLD - subtotal;
        int teal = ContextCompat.getColor(requireContext(), R.color.teal_trust);
        binding.tvShippingProgressText.setTextColor(teal);
        binding.progressFreeShipping.setIndicatorColor(teal);
        if (remaining <= 0L) {
            binding.tvShippingProgressText.setText("Đơn hàng của bạn được miễn phí vận chuyển");
            binding.progressFreeShipping.setProgress(100);
        } else {
            int pct = (int) Math.min(100L, (subtotal * 100L) / Constants.FREE_SHIPPING_THRESHOLD);
            binding.tvShippingProgressText.setText(
                    "Mua thêm " + FormatUtils.formatVnd(remaining) + " để miễn phí vận chuyển"
            );
            binding.progressFreeShipping.setProgress(pct);
        }
    }
}
