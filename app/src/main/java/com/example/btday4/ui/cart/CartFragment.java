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
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

/**
 * Shopping cart screen backed by Room SQLite with free-shipping progress bar, smooth quantity
 * controls, undo removal, and navigation to CheckoutActivity.
 */
public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartAdapter adapter;
    private final List<CartItem> cartItems = new ArrayList<>();

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
            public void onRemoveItem(CartItem item) {
                String productId = item.getProduct().getId();
                String removedName = item.getProduct().getName();
                int previousQty = item.getQuantity();
                CartRepository.removeItem(requireContext(), productId, () -> {
                    refreshCart();
                    if (isAdded() && binding != null) {
                        Snackbar.make(
                                        binding.getRoot(),
                                        "Đã xóa \"" + removedName + "\"",
                                        Snackbar.LENGTH_LONG
                                )
                                .setAction("Hoàn tác", v ->
                                        CartRepository.addItem(
                                                requireContext(),
                                                productId,
                                                previousQty,
                                                CartFragment.this::refreshCart
                                        )
                                )
                                .show();
                    }
                });
            }
        });

        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(adapter);

        binding.btnBrowseProducts.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProductsTab();
            }
        });

        binding.btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                return;
            }
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
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
            adapter.submitList(new ArrayList<>(cartItems));

            int totalQty = 0;
            for (CartItem item : cartItems) {
                totalQty += item.getQuantity();
            }
            long subtotal = CartRepository.calculateSubtotal(cartItems);
            boolean isEmpty = cartItems.isEmpty();

            binding.tvCartSummary.setText("(" + totalQty + " sản phẩm)");
            binding.tvCartGrandTotal.setText(FormatUtils.formatVnd(subtotal));
            binding.btnCheckout.setText("Thanh toán (" + totalQty + ")");
            binding.llCartEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.llCartTotalBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.llShippingProgress.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (!isEmpty) {
                long remaining = Constants.FREE_SHIPPING_THRESHOLD - subtotal;
                int teal = ContextCompat.getColor(requireContext(), R.color.teal_trust);
                if (remaining <= 0L) {
                    binding.tvShippingProgressText.setText("Đơn hàng của bạn được miễn phí vận chuyển");
                    binding.tvShippingProgressText.setTextColor(teal);
                    binding.progressFreeShipping.setProgress(100);
                    binding.progressFreeShipping.setIndicatorColor(teal);
                } else {
                    int pct = (int) Math.min(100L, (subtotal * 100L) / Constants.FREE_SHIPPING_THRESHOLD);
                    binding.tvShippingProgressText.setText(
                            "Mua thêm " + FormatUtils.formatVnd(remaining) + " để miễn phí vận chuyển"
                    );
                    binding.tvShippingProgressText.setTextColor(teal);
                    binding.progressFreeShipping.setProgress(pct);
                    binding.progressFreeShipping.setIndicatorColor(teal);
                }
            }

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCartBadge();
            }
        });
    }
}
