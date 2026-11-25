package com.example.btday4.ui.vouchers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.data.local.VoucherEntity;
import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.databinding.FragmentVouchersBinding;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.TopRightPopup;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

/**
 * Displays live Cloud Firestore vouchers with one-tap copy and
 * apply-on-checkout shortcuts.
 */
public class VouchersFragment extends Fragment {

    private FragmentVouchersBinding binding;
    private VoucherAdapter adapter;
    private ListenerRegistration vouchersListener;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentVouchersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new VoucherAdapter(new VoucherAdapter.Listener() {
            @Override
            public void onUseVoucher(VoucherEntity voucher) {
                useVoucherNow(voucher);
            }

            @Override
            public void onCopyVoucher(VoucherEntity voucher) {
                copyVoucherCode(voucher);
            }
        });
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListening();
    }

    private void startListening() {
        stopListening();
        if (binding != null) {
            binding.pbVouchersLoading.setVisibility(View.VISIBLE);
        }
        vouchersListener = VoucherRepository.listenVouchers(vouchers -> {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.pbVouchersLoading.setVisibility(View.GONE);
            bindVouchers(vouchers);
        });
    }

    private void stopListening() {
        if (vouchersListener != null) {
            vouchersListener.remove();
            vouchersListener = null;
        }
    }

    private void bindVouchers(List<VoucherEntity> vouchers) {
        adapter.submitList(vouchers);
        int total = vouchers == null ? 0 : vouchers.size();
        binding.tvVoucherCount.setText("(" + total + " ưu đãi)");
        binding.rvVouchers.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
        binding.llEmptyVouchers.setVisibility(total == 0 ? View.VISIBLE : View.GONE);

        if (vouchers != null && !vouchers.isEmpty()) {
            NotificationRepository.syncNewVouchers(requireContext(), vouchers, () -> {
                if (isAdded() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshNotificationBadge();
                }
            });
        }
    }

    private void copyVoucherCode(VoucherEntity voucher) {
        if (voucher == null || voucher.getCode().isEmpty()) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("voucher_code", voucher.getCode()));
        }
        if (getActivity() != null) {
            TopRightPopup.showInfo(
                    getActivity(),
                    "Đã sao chép " + voucher.getCode(),
                    "Nhập mã này ở bước Thanh toán để được giảm " + voucher.formatShortBadge() + ".");
        }
    }

    private void useVoucherNow(VoucherEntity voucher) {
        if (voucher == null || voucher.getCode().isEmpty()) {
            return;
        }
        VoucherRepository.setPendingVoucherCode(requireContext(), voucher.getCode());
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            TopRightPopup.showInfo(
                    main,
                    "Đã chọn mã " + voucher.getCode(),
                    "Tự động áp dụng khi bạn tiến hành thanh toán đơn hàng.");
            main.openCartTab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
        binding = null;
    }
}
