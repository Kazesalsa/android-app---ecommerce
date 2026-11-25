package com.example.btday4.ui.vouchers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.data.local.VoucherEntity;
import com.example.btday4.databinding.ItemVoucherBinding;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for ticket-style voucher cards in the Vouchers tab.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public interface Listener {
        void onUseVoucher(VoucherEntity voucher);

        void onCopyVoucher(VoucherEntity voucher);
    }

    private final List<VoucherEntity> items = new ArrayList<>();
    private final Listener listener;

    public VoucherAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<VoucherEntity> vouchers) {
        items.clear();
        if (vouchers != null) {
            items.addAll(vouchers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVoucherBinding binding = ItemVoucherBinding.inflate(
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

        private final ItemVoucherBinding binding;

        ViewHolder(ItemVoucherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VoucherEntity voucher) {
            Context context = binding.getRoot().getContext();
            long now = System.currentTimeMillis();
            boolean available = voucher.isAvailable(now);

            binding.tvVoucherValue.setText(voucher.formatShortBadge());
            binding.tvVoucherCodeStub.setText(voucher.getCode());
            binding.tvVoucherTitle.setText(
                    voucher.getTitle().isEmpty() ? "Mã " + voucher.getCode() : voucher.getTitle()
            );

            String desc = voucher.getDescription();
            if (desc.isEmpty()) {
                desc = voucher.isPercent() && voucher.getMaxDiscount() > 0L
                        ? "Giảm tối đa " + FormatUtils.formatVnd(voucher.getMaxDiscount())
                        : "Áp dụng trực tiếp tại bước thanh toán.";
            }
            binding.tvVoucherDesc.setText(desc);

            String minOrderText = voucher.getMinOrderAmount() > 0L
                    ? "Đơn từ " + FormatUtils.formatVnd(voucher.getMinOrderAmount())
                    : "Không yêu cầu tối thiểu";
            String quotaText = voucher.getUsageLimit() <= 0
                    ? "Không giới hạn lượt"
                    : "Còn " + voucher.getRemainingUses() + "/" + voucher.getUsageLimit() + " lượt";
            binding.tvVoucherMeta.setText(minOrderText + " • " + quotaText);

            binding.llVoucherStub.setBackgroundResource(
                    available ? R.drawable.bg_voucher_stub_left : R.drawable.bg_voucher_stub_disabled
            );

            if (!voucher.isActive()) {
                binding.tvVoucherExpiry.setText("Tạm ngưng");
                binding.tvVoucherExpiry.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
            } else if (voucher.isExpired(now)) {
                binding.tvVoucherExpiry.setText("Đã hết hạn");
                binding.tvVoucherExpiry.setTextColor(ContextCompat.getColor(context, R.color.danger_red));
            } else if (!voucher.hasRemainingUses()) {
                binding.tvVoucherExpiry.setText("Hết lượt sử dụng");
                binding.tvVoucherExpiry.setTextColor(ContextCompat.getColor(context, R.color.danger_red));
            } else if (voucher.getExpiresAt() > 0L) {
                binding.tvVoucherExpiry.setText("HSD: " + FormatUtils.formatDate(voucher.getExpiresAt()));
                binding.tvVoucherExpiry.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
            } else {
                binding.tvVoucherExpiry.setText("Còn hiệu lực");
                binding.tvVoucherExpiry.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
            }

            binding.btnCopyVoucher.setVisibility(available ? View.VISIBLE : View.GONE);
            binding.btnUseVoucher.setVisibility(available ? View.VISIBLE : View.GONE);
            binding.cardVoucher.setAlpha(available ? 1.0f : 0.72f);

            binding.btnCopyVoucher.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCopyVoucher(voucher);
                }
            });
            binding.btnUseVoucher.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUseVoucher(voucher);
                }
            });
        }
    }
}
