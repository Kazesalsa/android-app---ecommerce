package com.example.btday4.ui.products;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.data.local.Product;
import com.example.btday4.databinding.ItemProductBinding;
import com.example.btday4.model.ProductWithRating;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.ValidationUtils;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView ListAdapter rendering 2-column flagship tech tiles with studio image pedestals,
 * semantic rating/new badges, live stock DiffUtil updates, and 1-tap quick add-to-cart.
 */
public class ProductAdapter extends ListAdapter<ProductWithRating, ProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnQuickAddCartListener {
        void onQuickAddCart(Product product);
    }

    private final OnProductClickListener listener;
    @Nullable
    private final OnQuickAddCartListener quickAddListener;

    public ProductAdapter(OnProductClickListener listener) {
        this(listener, null);
    }

    public ProductAdapter(
            OnProductClickListener listener,
            @Nullable OnQuickAddCartListener quickAddListener
    ) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.quickAddListener = quickAddListener;
    }

    private static final DiffUtil.ItemCallback<ProductWithRating> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProductWithRating>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ProductWithRating oldItem,
                        @NonNull ProductWithRating newItem
                ) {
                    return Objects.equals(oldItem.product.getId(), newItem.product.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ProductWithRating oldItem,
                        @NonNull ProductWithRating newItem
                ) {
                    return Objects.equals(oldItem.product.getName(), newItem.product.getName())
                            && Objects.equals(oldItem.product.getPrice(), newItem.product.getPrice())
                            && Objects.equals(oldItem.product.getOriginalPrice(), newItem.product.getOriginalPrice())
                            && Objects.equals(oldItem.product.getImageUrl(), newItem.product.getImageUrl())
                            && oldItem.product.getStock() == newItem.product.getStock()
                            && Double.compare(oldItem.avgRating, newItem.avgRating) == 0
                            && oldItem.reviewCount == newItem.reviewCount;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(
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

        private final ItemProductBinding binding;

        ViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProductWithRating item) {
            Product product = item.product;
            ProductImageLoader.load(binding.imgProduct, product.getImageUrl());

            String brandLabel = ValidationUtils.isNotBlank(product.getBrand())
                    ? product.getBrand()
                    : product.getCategory();
            binding.tvProductBrand.setText(
                    brandLabel == null ? "" : brandLabel.toUpperCase(Locale.getDefault())
            );
            binding.tvProductName.setText(product.getName());
            binding.tvProductPrice.setText(product.getPrice());

            int stock = product.getStock();
            boolean soldOut = stock <= 0;
            binding.tvSoldOutOverlay.setVisibility(soldOut ? View.VISIBLE : View.GONE);
            binding.imgProduct.setAlpha(soldOut ? 0.55f : 1.0f);

            if (stock >= 1 && stock <= 5) {
                binding.tvLowStockTag.setVisibility(View.VISIBLE);
                binding.tvLowStockTag.setText("Còn " + stock);
            } else {
                binding.tvLowStockTag.setVisibility(View.GONE);
            }

            int discountPercent = FormatUtils.computeDiscountPercent(
                    product.getPrice(),
                    product.getOriginalPrice()
            );
            if (discountPercent > 0) {
                binding.tvDiscountBadge.setVisibility(View.VISIBLE);
                binding.tvDiscountBadge.setText("-" + discountPercent + "%");
                binding.tvProductOriginalPrice.setVisibility(View.VISIBLE);
                binding.tvProductOriginalPrice.setText(product.getOriginalPrice());
                binding.tvProductOriginalPrice.setPaintFlags(
                        binding.tvProductOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                binding.tvDiscountBadge.setVisibility(View.GONE);
                binding.tvProductOriginalPrice.setVisibility(View.INVISIBLE);
            }

            if (item.reviewCount > 0) {
                binding.imgRatingStar.setVisibility(View.VISIBLE);
                binding.tvProductRating.setText(FormatUtils.formatRating(item.avgRating));
                binding.tvProductReviewCount.setText("(" + item.reviewCount + ")");
            } else {
                binding.imgRatingStar.setVisibility(View.GONE);
                binding.tvProductRating.setText("Mới");
                binding.tvProductReviewCount.setText("");
            }

            binding.btnQuickAddCart.setEnabled(!soldOut);
            binding.btnQuickAddCart.setAlpha(soldOut ? 0.3f : 1.0f);
            binding.btnQuickAddCart.setOnClickListener(v -> {
                if (!soldOut && quickAddListener != null) {
                    quickAddListener.onQuickAddCart(product);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}
