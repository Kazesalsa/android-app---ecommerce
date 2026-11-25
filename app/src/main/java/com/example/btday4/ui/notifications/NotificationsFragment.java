package com.example.btday4.ui.notifications;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.databinding.FragmentNotificationsBinding;
import com.example.btday4.model.NotificationItem;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.orders.OrderDetailActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.TopRightPopup;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

/**
 * Displays the user's chronological list of promo, cart, order, and payment notifications
 * synchronized live from Cloud Firestore (`users/{username}/notifications`).
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationAdapter adapter;
    private ListenerRegistration notificationsListener;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new NotificationAdapter(this::handleNotificationClick);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnMarkAllRead.setOnClickListener(v ->
                NotificationRepository.markAllAsRead(requireContext(), null));
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
        notificationsListener = NotificationRepository.listenNotifications(requireContext(), items -> {
            if (binding == null || !isAdded()) {
                return;
            }
            bindNotifications(items);
        });
    }

    private void stopListening() {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    private void bindNotifications(List<NotificationItem> items) {
        adapter.submitList(items);

        int unread = 0;
        for (NotificationItem item : items) {
            if (!item.isRead()) {
                unread++;
            }
        }

        boolean isEmpty = items.isEmpty();
        binding.rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.llEmptyNotifications.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.btnMarkAllRead.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        binding.tvUnreadSummary.setText(unread > 0 ? "(" + unread + " chưa đọc)" : "");

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNotificationBadge(unread);
        }
    }

    private void handleNotificationClick(NotificationItem item) {
        if (!item.isRead()) {
            NotificationRepository.markAsRead(requireContext(), item.getId());
        }
        if (item.getType() == NotificationItem.Type.CART) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCartTab();
            }
        } else if ((item.getType() == NotificationItem.Type.PAYMENT
                || item.getType() == NotificationItem.Type.ORDER)
                && !item.getTargetId().isEmpty()) {
            Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ORDER_ID, item.getTargetId());
            startActivity(intent);
        } else if (item.getType() == NotificationItem.Type.PROMO) {
            if (!item.getPromoCode().trim().isEmpty()) {
                String code = item.getPromoCode().trim();
                ClipboardManager clipboard = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("promo_code", code));
                }
                if (getActivity() != null) {
                    TopRightPopup.showInfo(
                            getActivity(),
                            "Đã sao chép mã " + code,
                            "Mở Kho Voucher để xem chi tiết hoặc áp dụng khi thanh toán.");
                }
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openVouchersTab();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
        binding = null;
    }
}
