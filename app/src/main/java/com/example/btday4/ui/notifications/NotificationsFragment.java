package com.example.btday4.ui.notifications;

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
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.FragmentNotificationsBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.orders.OrderDetailActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.ValidationUtils;
import java.util.List;

/**
 * Bottom Navigation "Thông báo" screen displaying all in-app events (orders, vouchers, cart, account)
 * with unread badges, mark-all-read, clear-all, and contextual deep-linking.
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private UserSession userSession;
    private NotificationAdapter adapter;
    private final NotificationRepository.Listener repoListener = this::refreshNotifications;

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
        userSession = new UserSession(requireContext());

        adapter = new NotificationAdapter(this::handleNotificationClick);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnMarkAllRead.setOnClickListener(v -> {
            NotificationRepository.markAllRead(requireContext(), userSession.getUsername());
            refreshNotifications();
        });

        binding.btnClearNotifications.setOnClickListener(v -> {
            NotificationRepository.clearAll(requireContext(), userSession.getUsername());
            refreshNotifications();
        });

        NotificationRepository.addListener(repoListener);
        if (!isHidden()) {
            refreshNotifications();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            refreshNotifications();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshNotifications();
        }
    }

    @Override
    public void onDestroyView() {
        NotificationRepository.removeListener(repoListener);
        super.onDestroyView();
        binding = null;
    }

    public void refreshNotifications() {
        if (!isAdded() || binding == null || adapter == null) {
            return;
        }
        if (userSession == null) {
            userSession = new UserSession(requireContext());
        }
        List<AppNotification> items = NotificationRepository.getNotifications(
                requireContext(),
                userSession.getUsername()
        );
        adapter.submitList(items);

        boolean empty = items.isEmpty();
        binding.llNotificationsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        boolean hasUnread = false;
        for (AppNotification item : items) {
            if (!item.isRead()) {
                hasUnread = true;
                break;
            }
        }
        binding.btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        binding.btnClearNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void handleNotificationClick(AppNotification item) {
        if (!isAdded() || userSession == null) {
            return;
        }
        NotificationRepository.markRead(requireContext(), userSession.getUsername(), item.getId());
        String type = item.getType();
        if (AppNotification.TYPE_ORDER.equals(type) && ValidationUtils.isNotBlank(item.getTargetId())) {
            Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ORDER_ID, item.getTargetId());
            intent.putExtra(Constants.EXTRA_IS_NEW_ORDER, false);
            startActivity(intent);
        } else if (AppNotification.TYPE_VOUCHER.equals(type)) {
            startActivity(new Intent(requireContext(), VoucherWalletActivity.class));
        } else if (AppNotification.TYPE_CART.equals(type) && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openCartTab();
        } else if (AppNotification.TYPE_ACCOUNT.equals(type) && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openProfileTab();
        }
    }
}
