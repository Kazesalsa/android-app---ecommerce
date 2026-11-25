package com.example.btday4.ui.notifications;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.databinding.ItemNotificationBinding;
import com.example.btday4.model.NotificationItem;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for rendering in-app notifications (promos, cart,
 * orders, payments).
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface Listener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final Listener listener;

    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotificationBinding binding;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationItem item) {
            Context context = binding.getRoot().getContext();
            binding.tvNotifTitle.setText(item.getTitle());
            binding.tvNotifMessage.setText(item.getMessage());
            binding.tvNotifTime.setText(formatRelativeTime(item.getTimestamp()));

            boolean unread = !item.isRead();
            binding.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            binding.cardNotification.setCardBackgroundColor(ContextCompat.getColor(
                    context,
                    R.color.bg_card));
            binding.cardNotification.setStrokeColor(ContextCompat.getColor(
                    context,
                    unread ? R.color.primary_border_soft : R.color.divider_hairline));

            String promoCode = item.getPromoCode();
            if (promoCode != null && !promoCode.trim().isEmpty()) {
                binding.llPromoCodeRow.setVisibility(View.VISIBLE);
                binding.tvPromoCode.setText("Mã: " + promoCode.trim() + " — Chạm để sao chép");
            } else {
                binding.llPromoCodeRow.setVisibility(View.GONE);
            }

            styleType(context, item.getType());

            binding.cardNotification.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }

        private void styleType(Context context, NotificationItem.Type type) {
            switch (type) {
                case PROMO:
                    binding.iconBadgeWrap.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.primary_soft)));
                    binding.imgNotifType.setImageResource(R.drawable.ic_promo_tag);
                    binding.imgNotifType.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.primary)));
                    binding.tvNotifTypeLabel.setText("Mã giảm giá");
                    binding.tvNotifTypeLabel.setBackgroundResource(R.drawable.bg_discount_tag);
                    binding.tvNotifTypeLabel.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    break;
                case CART:
                    binding.iconBadgeWrap.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.teal_trust_bg)));
                    binding.imgNotifType.setImageResource(R.drawable.ic_nav_cart);
                    binding.imgNotifType.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.teal_trust)));
                    binding.tvNotifTypeLabel.setText("Giỏ hàng");
                    binding.tvNotifTypeLabel.setBackgroundResource(R.drawable.bg_freeship_tag);
                    binding.tvNotifTypeLabel.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
                    break;
                case PAYMENT:
                    binding.iconBadgeWrap.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.success_bg)));
                    binding.imgNotifType.setImageResource(R.drawable.ic_check_circle);
                    binding.imgNotifType.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.success_green)));
                    binding.tvNotifTypeLabel.setText("Thanh toán");
                    binding.tvNotifTypeLabel.setBackgroundResource(R.drawable.bg_freeship_tag);
                    binding.tvNotifTypeLabel.setTextColor(ContextCompat.getColor(context, R.color.success_green));
                    break;
                case ORDER:
                default:
                    binding.iconBadgeWrap.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.warning_bg)));
                    binding.imgNotifType.setImageResource(R.drawable.ic_receipt);
                    binding.imgNotifType.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.warning_amber)));
                    binding.tvNotifTypeLabel.setText("Đơn hàng");
                    binding.tvNotifTypeLabel.setBackgroundResource(R.drawable.bg_pill_subtle);
                    binding.tvNotifTypeLabel.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                    break;
            }
        }

        private String formatRelativeTime(long timestampMs) {
            if (timestampMs <= 0L) {
                return "";
            }
            long diff = Math.max(0L, System.currentTimeMillis() - timestampMs);
            long minutes = diff / 60_000L;
            if (minutes < 1L) {
                return "Vừa xong";
            }
            if (minutes < 60L) {
                return minutes + " phút trước";
            }
            long hours = minutes / 60L;
            if (hours < 24L) {
                return hours + " giờ trước";
            }
            return FormatUtils.formatShortDateTime(timestampMs);
        }
    }
}
