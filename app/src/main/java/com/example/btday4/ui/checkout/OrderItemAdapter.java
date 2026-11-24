package com.example.btday4.ui.checkout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.databinding.ItemOrderLineBinding;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ProductImageLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared compact line-item adapter reused cleanly by both CheckoutActivity and OrderDetailActivity.
 * When reviewClickListener is provided, each purchased item displays a direct "Đánh giá" action.
 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    public interface OnReviewItemClickListener {
        void onReviewItemClick(OrderItemEntity item);
    }

    private final List<OrderItemEntity> items = new ArrayList<>();
    @Nullable
    private final OnReviewItemClickListener reviewClickListener;

    public OrderItemAdapter(@Nullable OnReviewItemClickListener reviewClickListener) {
        this.reviewClickListener = reviewClickListener;
    }

    public void submitItems(List<OrderItemEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderLineBinding binding = ItemOrderLineBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position == items.size() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemOrderLineBinding binding;

        ViewHolder(ItemOrderLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderItemEntity item, boolean isLast) {
            ProductImageLoader.load(binding.imgOrderLineProduct, item.getProductImageUrl());
            binding.tvOrderLineName.setText(item.getProductName());
            binding.tvOrderLineMeta.setText(
                    FormatUtils.formatVnd(item.getUnitPrice()) + " × " + item.getQuantity()
            );
            binding.tvOrderLineSubtotal.setText(FormatUtils.formatVnd(item.lineSubtotal()));
            binding.dividerOrderLine.setVisibility(isLast ? View.GONE : View.VISIBLE);

            if (reviewClickListener != null) {
                binding.btnOrderLineReview.setVisibility(View.VISIBLE);
                binding.btnOrderLineReview.setOnClickListener(v -> reviewClickListener.onReviewItemClick(item));
            } else {
                binding.btnOrderLineReview.setVisibility(View.GONE);
                binding.btnOrderLineReview.setOnClickListener(null);
            }
        }
    }
}
