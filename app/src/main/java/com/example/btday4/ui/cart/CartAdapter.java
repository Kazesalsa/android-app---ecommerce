package com.example.btday4.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.databinding.ItemCartBinding;
import com.example.btday4.model.CartItem;
import com.example.btday4.util.ProductImageLoader;
import java.util.Objects;

/**
 * RecyclerView ListAdapter for cart items with per-line purchase selection, using DiffUtil to avoid
 * redrawing images on quantity changes.
 */
public class CartAdapter extends ListAdapter<CartItem, CartAdapter.ViewHolder> {

    public interface CartActionListener {
        void onItemClick(CartItem item);

        void onQuantityChanged(CartItem item, int newQuantity);

        void onRemoveItem(CartItem item);

        boolean isSelected(CartItem item);

        void onSelectionChanged(CartItem item, boolean selected);
    }

    private final CartActionListener listener;

    public CartAdapter(CartActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CartItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
                    return Objects.equals(oldItem.getProduct().getId(), newItem.getProduct().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
                    return oldItem.getQuantity() == newItem.getQuantity()
                            && oldItem.getProduct().getStock() == newItem.getProduct().getStock()
                            && Objects.equals(oldItem.getProduct().getPrice(), newItem.getProduct().getPrice())
                            && Objects.equals(oldItem.getProduct().getName(), newItem.getProduct().getName());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItem item) {
            Product product = item.getProduct();
            int maxPurchasable = CartRepository.maxPurchasable(product);
            ProductImageLoader.load(binding.imgCartProduct, product.getImageUrl());
            binding.tvCartProductName.setText(product.getName());
            binding.tvCartUnitPrice.setText("Đơn giá: " + product.getPrice());
            binding.tvCartSubtotal.setText(item.getSubtotalText());
            binding.tvCartQty.setText(String.valueOf(item.getQuantity()));

            if (maxPurchasable == 0) {
                binding.tvCartStockWarning.setVisibility(View.VISIBLE);
                binding.tvCartStockWarning.setText(R.string.out_of_stock);
            } else if (item.getQuantity() >= product.getStock()) {
                binding.tvCartStockWarning.setVisibility(View.VISIBLE);
                binding.tvCartStockWarning.setText("Chỉ còn " + product.getStock() + " sản phẩm");
            } else {
                binding.tvCartStockWarning.setVisibility(View.GONE);
            }
            binding.cbCartSelect.setOnCheckedChangeListener(null);
            binding.cbCartSelect.setEnabled(maxPurchasable > 0);
            binding.cbCartSelect.setChecked(maxPurchasable > 0 && listener.isSelected(item));
            binding.cbCartSelect.setOnCheckedChangeListener((button, checked) ->
                    listener.onSelectionChanged(item, checked)
            );

            boolean canIncrease = item.getQuantity() < maxPurchasable;
            binding.btnCartPlus.setEnabled(canIncrease);
            binding.btnCartPlus.setAlpha(canIncrease ? 1f : 0.35f);

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            binding.btnCartMinus.setOnClickListener(v ->
                    listener.onQuantityChanged(item, item.getQuantity() - 1)
            );
            binding.btnCartPlus.setOnClickListener(v ->
                    listener.onQuantityChanged(item, item.getQuantity() + 1)
            );
            binding.btnCartRemove.setOnClickListener(v ->
                    listener.onRemoveItem(item)
            );
        }
    }
}
