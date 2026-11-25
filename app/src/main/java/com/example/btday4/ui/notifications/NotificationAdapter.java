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
import com.example.btday4.model.AppNotification;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter rendering user notifications with type-themed icons and unread indicators.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification item);
    }

    private final List<AppNotification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppNotification> newItems) {
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
                false
        );
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

        void bind(AppNotification item) {
            Context context = binding.getRoot().getContext();
            binding.tvNotifTitle.setText(item.getTitle());
            binding.tvNotifMessage.setText(item.getMessage());
            binding.tvNotifTime.setText(FormatUtils.formatDateTime(item.getCreatedAt()));

            boolean unread = !item.isRead();
            binding.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            binding.cardNotificationItem.setStrokeColor(ContextCompat.getColor(
                    context,
                    unread ? R.color.primary_border_soft : R.color.border_light
            ));

            int bgColor;
            int fgColor;
            int iconRes;
            String type = item.getType();
            if (AppNotification.TYPE_VOUCHER.equals(type)) {
                bgColor = ContextCompat.getColor(context, R.color.primary_soft);
                fgColor = ContextCompat.getColor(context, R.color.primary);
                iconRes = R.drawable.ic_voucher;
            } else if (AppNotification.TYPE_ORDER.equals(type)) {
                bgColor = ContextCompat.getColor(context, R.color.teal_trust_bg);
                fgColor = ContextCompat.getColor(context, R.color.teal_trust);
                iconRes = R.drawable.ic_receipt;
            } else if (AppNotification.TYPE_CART.equals(type)) {
                bgColor = ContextCompat.getColor(context, R.color.teal_trust_bg);
                fgColor = ContextCompat.getColor(context, R.color.teal_trust);
                iconRes = R.drawable.ic_shopping_bag;
            } else if (AppNotification.TYPE_ALERT.equals(type)) {
                bgColor = ContextCompat.getColor(context, R.color.primary_soft);
                fgColor = ContextCompat.getColor(context, R.color.danger_red);
                iconRes = R.drawable.ic_nav_bell_filled;
            } else {
                bgColor = ContextCompat.getColor(context, R.color.bg_subtle);
                fgColor = ContextCompat.getColor(context, R.color.text_primary);
                iconRes = R.drawable.ic_nav_profile_outline;
            }
            binding.flNotifIconBg.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            binding.imgNotifIcon.setImageResource(iconRes);
            binding.imgNotifIcon.setImageTintList(ColorStateList.valueOf(fgColor));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }
    }
}
