package com.example.btday4.ui.orders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.databinding.ItemOrderBinding;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ProductImageLoader;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView ListAdapter displaying placed orders in OrderHistoryActivity.
 */
public class OrderAdapter extends ListAdapter<OrderWithItems, OrderAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderWithItems orderWithItems);
    }

    private final OnOrderClickListener listener;

    public OrderAdapter(OnOrderClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<OrderWithItems> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OrderWithItems>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull OrderWithItems oldItem,
                        @NonNull OrderWithItems newItem
                ) {
                    return Objects.equals(oldItem.order.getId(), newItem.order.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull OrderWithItems oldItem,
                        @NonNull OrderWithItems newItem
                ) {
                    return Objects.equals(oldItem.order.getStatus(), newItem.order.getStatus())
                            && oldItem.order.getTotalAmount() == newItem.order.getTotalAmount()
                            && oldItem.order.getVoucherDiscount() == newItem.order.getVoucherDiscount()
                            && oldItem.totalQuantity() == newItem.totalQuantity();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
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

        private final ItemOrderBinding binding;

        ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderWithItems row) {
            OrderEntity order = row.order;
            List<OrderItemEntity> items = row.safeItems();
            Context context = binding.getRoot().getContext();

            OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
            binding.tvOrderCode.setText(order.getId());
            binding.tvOrderDate.setText(FormatUtils.formatDateTime(order.getCreatedAt()));
            binding.tvOrderStatus.setText(orderStatus.getLabel());
            if (orderStatus == OrderStatus.PAID
                    || orderStatus == OrderStatus.DELIVERED
                    || orderStatus == OrderStatus.PROCESSING) {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_freeship_tag);
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
            } else if (orderStatus == OrderStatus.CANCELLED) {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_pill_subtle);
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
            } else {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_discount_tag);
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
            }
            binding.tvOrderTotal.setText(FormatUtils.formatVnd(order.getTotalAmount()));

            PaymentMethod method = PaymentMethod.fromCode(order.getPaymentMethod());
            String voucherSuffix = order.getVoucherDiscount() > 0L && !order.getVoucherCode().isEmpty()
                    ? " · Mã " + order.getVoucherCode()
                    : "";
            binding.tvOrderItemCount.setText(
                    row.totalQuantity() + " sản phẩm · " + method.getTitle() + voucherSuffix
            );

            if (!items.isEmpty()) {
                OrderItemEntity first = items.get(0);
                ProductImageLoader.load(binding.imgOrderThumb, first.getProductImageUrl());
                if (items.size() > 1) {
                    binding.tvOrderSummaryTitle.setText(
                            first.getProductName() + " và " + (items.size() - 1) + " sản phẩm khác"
                    );
                } else {
                    binding.tvOrderSummaryTitle.setText(first.getProductName());
                }
            } else {
                binding.tvOrderSummaryTitle.setText("Đơn hàng");
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(row);
                }
            });
        }
    }
}
