package com.example.btday4.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.btday4.R;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.databinding.FragmentProfileBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.ui.auth.LoginActivity;
import com.example.btday4.ui.orders.OrderHistoryActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.AvatarImageLoader;
import com.example.btday4.util.Constants;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Account Hub displaying circular user avatar with instant gallery photo picker, membership tier,
 * 3-column stats (Orders / Vouchers / Reviews), 3-status order shortcuts, and personal details.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserSession userSession;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    refreshProfileData();
                    showProfileNotification("Đã lưu thông tin hồ sơ");
                }
            }
    );

    private final ActivityResultLauncher<Intent> changePasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    showProfileNotification("Đã cập nhật mật khẩu thành công");
                }
            }
    );

    private final ActivityResultLauncher<String> quickAvatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handlePickedAvatarUri
    );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userSession = new UserSession(requireContext());

        View.OnClickListener openEditor = v ->
                editProfileLauncher.launch(new Intent(requireContext(), EditProfileActivity.class));

        binding.flProfileAvatar.setOnClickListener(v -> quickAvatarPickerLauncher.launch("image/*"));
        binding.btnEditProfileHeader.setOnClickListener(openEditor);
        binding.btnEditPersonalCard.setOnClickListener(openEditor);

        binding.rowProfileFullName.setOnClickListener(openEditor);
        binding.rowProfileEmail.setOnClickListener(openEditor);
        binding.rowProfilePhone.setOnClickListener(openEditor);
        binding.rowProfileDemographics.setOnClickListener(openEditor);
        binding.rowProfileAddress.setOnClickListener(openEditor);

        binding.rowMyOrders.setOnClickListener(v -> openOrdersWithFilter(null));
        binding.btnViewAllOrders.setOnClickListener(v -> openOrdersWithFilter(null));
        binding.shortcutPendingPayment.setOnClickListener(v -> openOrdersWithFilter(OrderStatus.PENDING_PAYMENT));
        binding.shortcutPaid.setOnClickListener(v -> openOrdersWithFilter(OrderStatus.PROCESSING));
        binding.shortcutCancelled.setOnClickListener(v -> openOrdersWithFilter(OrderStatus.CANCELLED));

        View.OnClickListener openVouchers = v ->
                startActivity(new Intent(requireContext(), VoucherWalletActivity.class));
        binding.statVouchers.setOnClickListener(openVouchers);
        binding.rowVoucherWallet.setOnClickListener(openVouchers);

        binding.rowMyReviews.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyReviewsActivity.class))
        );
        binding.rowChangePassword.setOnClickListener(v ->
                changePasswordLauncher.launch(new Intent(requireContext(), ChangePasswordActivity.class))
        );

        binding.btnLogout.setOnClickListener(v -> UiUtils.showConfirmDialog(
                requireContext(),
                "Đăng xuất tài khoản?",
                "Phiên làm việc hiện tại sẽ kết thúc và quay về màn hình đăng nhập.",
                getString(R.string.btn_logout),
                this::performLogout
        ));

        if (!isHidden()) {
            refreshProfileData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            refreshProfileData();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshProfileData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void handlePickedAvatarUri(@Nullable Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }
        android.content.Context appCtx = requireContext().getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            String encoded = AvatarImageLoader.encodeUriToDataUri(appCtx, uri);
            if (encoded == null || !isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                userSession.updateAvatarUrl(encoded, () -> {
                    if (isAdded() && binding != null) {
                        refreshProfileData();
                        showProfileNotification("Đã cập nhật ảnh đại diện");
                    }
                });
            });
        });
    }

    public void refreshProfileData() {
        if (!isAdded() || binding == null || userSession == null) {
            return;
        }
        binding.tvProfileRememberMe.setText(userSession.isRemembered() ? "Ghi nhớ: Bật" : "Ghi nhớ: Tắt");

        String username = userSession.getUsername();
        String cachedName = userSession.getDisplayName();
        String cachedAvatar = userSession.getAvatarUrl();
        AvatarImageLoader.load(binding.imgProfileAvatar, binding.tvAvatarInitials, cachedAvatar, cachedName);
        binding.tvProfileName.setText(cachedName);

        OrderRepository.getOrdersForUser(requireContext(), username, this::bindOrderStatsAndTier);

        VoucherRepository.countAvailableForUser(requireContext(), username, count -> {
            if (isAdded() && binding != null) {
                binding.tvVouchersCount.setText(String.valueOf(count));
            }
        });

        ReviewRepository.countUserReviews(requireContext(), username, count -> {
            if (isAdded() && binding != null) {
                binding.tvReviewsCount.setText(String.valueOf(count));
            }
        });

        userSession.loadCurrentUser(user -> {
            if (!isAdded() || binding == null) {
                return;
            }
            String resolvedUsername = user != null && ValidationUtils.isNotBlank(user.getUsername())
                    ? user.getUsername()
                    : userSession.getUsername();
            String displayName = user != null && ValidationUtils.isNotBlank(user.getDisplayName())
                    ? user.getDisplayName()
                    : userSession.getDisplayName();
            String email = user != null ? safe(user.getEmail()) : "";
            String phone = user != null ? safe(user.getPhone()) : "";
            String gender = user != null ? safe(user.getGender()) : "";
            String birthDate = user != null ? safe(user.getBirthDate()) : "";
            String address = user != null ? safe(user.getAddress()) : "";
            String loginTime = user != null ? safe(user.getLoginTime()) : "";
            String avatarUrl = user != null && ValidationUtils.isNotBlank(user.getAvatarUrl())
                    ? user.getAvatarUrl()
                    : userSession.getAvatarUrl();

            AvatarImageLoader.load(binding.imgProfileAvatar, binding.tvAvatarInitials, avatarUrl, displayName);
            binding.tvProfileName.setText(displayName);
            if (ValidationUtils.isNotBlank(resolvedUsername)) {
                binding.tvProfileUsername.setVisibility(View.VISIBLE);
                binding.tvProfileUsername.setText(resolvedUsername.trim());
            } else {
                binding.tvProfileUsername.setVisibility(View.GONE);
            }

            bindCompletePersonalInfo(displayName, email, phone, gender, birthDate, address);

            binding.tvProfileLoginTime.setText(
                    "Đăng nhập: " + (ValidationUtils.isNotBlank(loginTime) ? loginTime : "Phiên hiện tại")
            );
        });
    }

    private void bindOrderStatsAndTier(List<OrderWithItems> orders) {
        if (!isAdded() || binding == null) {
            return;
        }
        int totalOrders = orders == null ? 0 : orders.size();
        binding.tvOrdersCount.setText(String.valueOf(totalOrders));

        int pendingPay = 0;
        int activeCount = 0;
        long totalSpend = 0L;

        if (orders != null) {
            for (OrderWithItems item : orders) {
                OrderStatus st = OrderStatus.fromCode(item.order.getStatus());
                if (st == OrderStatus.PENDING_PAYMENT) {
                    pendingPay++;
                } else if (st == OrderStatus.PROCESSING || st == OrderStatus.PAID || st == OrderStatus.DELIVERED) {
                    activeCount++;
                }
                if (st != OrderStatus.CANCELLED) {
                    totalSpend += item.order.getTotalAmount();
                }
            }
        }

        bindMiniBadge(binding.tvBadgePendingPay, pendingPay);
        bindMiniBadge(binding.tvBadgePaid, activeCount);

        String tier;
        if (totalSpend >= 500_000L || totalOrders >= 5) {
            tier = "★ Thành viên Kim Cương";
        } else if (totalSpend >= 150_000L || totalOrders >= 2) {
            tier = "★ Thành viên Vàng";
        } else if (totalOrders >= 1) {
            tier = "Thành viên Thân thiết";
        } else {
            tier = "Thành viên Mới";
        }
        binding.tvMemberTier.setText(tier);
    }

    private void bindMiniBadge(TextView badge, int count) {
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void bindCompletePersonalInfo(
            String displayName,
            String email,
            String phone,
            String gender,
            String birthDate,
            String address
    ) {
        bindCleanValue(binding.tvProfileFullNameDetail, displayName);
        bindCleanValue(binding.tvProfileEmail, email);
        bindCleanValue(binding.tvProfilePhone, phone);
        bindCleanValue(binding.tvProfileGender, gender);
        bindCleanValue(binding.tvProfileBirthDate, birthDate);
        bindCleanValue(binding.tvProfileAddress, address);
    }

    private void bindCleanValue(TextView view, String value) {
        boolean filled = ValidationUtils.isNotBlank(value);
        view.setText(filled ? value.trim() : "");
        view.setVisibility(filled ? View.VISIBLE : View.GONE);
    }

    private void openOrdersWithFilter(@Nullable OrderStatus filter) {
        Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
        if (filter != null) {
            intent.putExtra(Constants.EXTRA_ORDER_STATUS_FILTER, filter.getCode());
        }
        startActivity(intent);
    }

    private void showProfileNotification(String message) {
        if (!isAdded() || binding == null) {
            return;
        }
        InAppNotifier.show(binding.getRoot(), AppNotification.TYPE_ACCOUNT, "Tài khoản", message);
    }

    private void performLogout() {
        userSession.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
