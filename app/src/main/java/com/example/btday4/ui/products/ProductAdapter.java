package com.example.btday4.ui.products;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.data.local.Product;
import com.example.btday4.databinding.ItemProductBinding;
import com.example.btday4.model.ProductWithRating;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.ValidationUtils;
import java.util.Objects;

/**
 * RecyclerView ListAdapter rendering 2-column commerce feed tiles with coral pricing,
 * dynamic Freeship badge, and live ratings.
 */
public class ProductAdapter extends ListAdapter<ProductWithRating, ProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
            binding.tvProductBrand.setText(brandLabel);
            binding.tvProductName.setText(product.getName());
            binding.tvProductPrice.setText(product.getPrice());

            long priceAmount = FormatUtils.parseVnd(product.getPrice());
            boolean eligibleFreeShip = FormatUtils.computeShippingFee(priceAmount) == 0L;
            binding.tvFreeshipTag.setVisibility(eligibleFreeShip ? View.VISIBLE : View.GONE);

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
                binding.tvProductRating.setText(FormatUtils.formatRating(item.avgRating));
                binding.tvProductReviewCount.setText("(" + item.reviewCount + ")");
            } else {
                binding.tvProductRating.setText("Mới");
                binding.tvProductReviewCount.setText("");
            }

            binding.tvProductStockMini.setText(product.getStock() > 0 ? "Còn " + product.getStock() : "Hết hàng");

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}
