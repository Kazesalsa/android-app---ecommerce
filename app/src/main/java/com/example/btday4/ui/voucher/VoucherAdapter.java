package com.example.btday4.ui.voucher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.ItemVoucherBinding;
import com.example.btday4.model.Voucher;
import com.example.btday4.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders perforated ticket voucher cards for both Wallet mode and Checkout/Cart Picker mode.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public interface VoucherActionListener {
        void onClaimVoucher(Voucher voucher);

        void onSelectOrUseVoucher(Voucher voucher);
    }

    private final List<VoucherItem> items = new ArrayList<>();
    private final boolean pickerMode;
    private final long orderSubtotal;
    private final long baseShippingFee;
    private String selectedCode = "";
    private final VoucherActionListener listener;

    public VoucherAdapter(
            boolean pickerMode,
            long orderSubtotal,
            long baseShippingFee,
            String selectedCode,
            VoucherActionListener listener
    ) {
        this.pickerMode = pickerMode;
        this.orderSubtotal = orderSubtotal;
        this.baseShippingFee = baseShippingFee;
        this.selectedCode = selectedCode == null ? "" : selectedCode;
        this.listener = listener;
    }

    public void submitList(List<VoucherItem> list, String activeCode) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        this.selectedCode = activeCode == null ? "" : activeCode;
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

        void bind(VoucherItem row) {
            Context context = binding.getRoot().getContext();
            Voucher v = row.voucher;
            long now = System.currentTimeMillis();
            boolean expired = v.isExpired(now);

            binding.llVoucherStub.setBackgroundResource(
                    v.isFreeShipType() ? R.drawable.bg_voucher_stub_teal : R.drawable.bg_voucher_stub_primary
            );
            binding.tvVoucherBadge.setText(v.getBadgeLabel());
            binding.tvVoucherCodeStub.setText(v.getCode());
            binding.tvVoucherTitle.setText(v.getTitle());
            binding.tvVoucherCondition.setText(v.getDescription());

            boolean isSelected = !selectedCode.isEmpty() && selectedCode.equalsIgnoreCase(v.getCode());
            binding.cardVoucherRoot.setStrokeColor(ContextCompat.getColor(
                    context,
                    isSelected ? R.color.primary : R.color.border_light
            ));
            binding.cardVoucherRoot.setStrokeWidth(Math.round((isSelected ? 1.5f : 1f)
                    * context.getResources().getDisplayMetrics().density));

            float contentAlpha = (row.used || expired) ? 0.55f : 1.0f;
            binding.llVoucherStub.setAlpha(contentAlpha);
            binding.tvVoucherTitle.setAlpha(contentAlpha);

            if (row.used) {
                binding.tvVoucherMeta.setText("Đã sử dụng");
                binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                styleDisabledButton(context, "Đã dùng");
                return;
            }
            if (expired) {
                binding.tvVoucherMeta.setText("Đã hết hạn");
                binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                styleDisabledButton(context, "Hết hạn");
                return;
            }

            if (pickerMode) {
                long discount = v.computeDiscount(orderSubtotal, baseShippingFee);
                long shortfall = v.amountNeededToUnlock(orderSubtotal);
                if (shortfall > 0L) {
                    binding.tvVoucherMeta.setText("Mua thêm " + FormatUtils.formatVnd(shortfall) + " để mở khóa");
                    binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                    styleDisabledButton(context, "Chưa đủ");
                } else if (discount <= 0L && v.isFreeShipType()) {
                    binding.tvVoucherMeta.setText("Đơn đã được miễn phí vận chuyển tự động");
                    binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
                    styleDisabledButton(context, "Đã Freeship");
                } else {
                    binding.tvVoucherMeta.setText("Tiết kiệm " + FormatUtils.formatVnd(discount));
                    binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.teal_trust));
                    binding.btnVoucherAction.setEnabled(true);
                    if (isSelected) {
                        binding.btnVoucherAction.setText("Đang chọn");
                        binding.btnVoucherAction.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ink_dark))
                        );
                        binding.btnVoucherAction.setTextColor(ContextCompat.getColor(context, R.color.white));
                    } else {
                        binding.btnVoucherAction.setText(row.claimed ? "Dùng mã" : "Lưu & Dùng");
                        binding.btnVoucherAction.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                        );
                        binding.btnVoucherAction.setTextColor(ContextCompat.getColor(context, R.color.white));
                    }
                    binding.btnVoucherAction.setOnClickListener(view -> listener.onSelectOrUseVoucher(v));
                }
            } else {
                binding.tvVoucherMeta.setText("HSD: " + FormatUtils.formatDate(v.getExpiryMillis()));
                binding.tvVoucherMeta.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                binding.btnVoucherAction.setEnabled(true);
                if (!row.claimed) {
                    binding.btnVoucherAction.setText("Lưu mã");
                    binding.btnVoucherAction.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                    );
                    binding.btnVoucherAction.setTextColor(ContextCompat.getColor(context, R.color.white));
                    binding.btnVoucherAction.setOnClickListener(view -> listener.onClaimVoucher(v));
                } else {
                    binding.btnVoucherAction.setText("Dùng ngay");
                    binding.btnVoucherAction.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_soft))
                    );
                    binding.btnVoucherAction.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    binding.btnVoucherAction.setOnClickListener(view -> listener.onSelectOrUseVoucher(v));
                }
            }
        }

        private void styleDisabledButton(Context context, String label) {
            binding.btnVoucherAction.setEnabled(false);
            binding.btnVoucherAction.setText(label);
            binding.btnVoucherAction.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.btn_disabled_bg))
            );
            binding.btnVoucherAction.setTextColor(ContextCompat.getColor(context, R.color.btn_disabled_text));
            binding.btnVoucherAction.setOnClickListener(null);
        }
    }
}
