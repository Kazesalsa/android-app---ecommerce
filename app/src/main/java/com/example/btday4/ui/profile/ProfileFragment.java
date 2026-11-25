package com.example.btday4.ui.profile;

import android.app.Activity;
import android.content.Intent;
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
import com.example.btday4.databinding.FragmentProfileBinding;
import com.example.btday4.ui.auth.LoginActivity;
import com.example.btday4.ui.orders.OrderHistoryActivity;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.material.snackbar.Snackbar;

/**
 * Full-bleed Account Hub displaying populated profile fields cleanly (hiding empty fields),
 * Orders, My Reviews, Edit Profile, Change Password, and logout with a confirmation popup.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserSession userSession;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    refreshProfileData();
                    if (isAdded() && binding != null) {
                        Snackbar.make(binding.getRoot(), "Đã lưu thông tin cá nhân", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> changePasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && isAdded() && binding != null) {
                    Snackbar.make(binding.getRoot(), "Đã cập nhật mật khẩu", Snackbar.LENGTH_SHORT).show();
                }
            }
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

        binding.btnEditProfileHeader.setOnClickListener(v ->
                editProfileLauncher.launch(new Intent(requireContext(), EditProfileActivity.class))
        );
        binding.rowMyOrders.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OrderHistoryActivity.class))
        );
        binding.btnViewAllOrders.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OrderHistoryActivity.class))
        );
        binding.rowMyReviews.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyReviewsActivity.class))
        );
        binding.rowChangePassword.setOnClickListener(v ->
                changePasswordLauncher.launch(new Intent(requireContext(), ChangePasswordActivity.class))
        );

        binding.btnLogout.setOnClickListener(v -> UiUtils.showConfirmDialog(
                requireContext(),
                "Đăng xuất?",
                "Bạn sẽ quay về màn hình đăng nhập.",
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

    public void refreshProfileData() {
        if (!isAdded() || binding == null || userSession == null) {
            return;
        }
        binding.tvProfileRememberMe.setText(userSession.isRemembered() ? "Đã bật" : "Không bật");

        String username = userSession.getUsername();
        OrderRepository.countOrdersForUser(requireContext(), username, count -> {
            if (isAdded() && binding != null) {
                binding.tvOrdersCount.setText(String.valueOf(count));
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
            String resolvedUsername = user != null ? user.getUsername() : userSession.getUsername();
            String displayName = user != null && ValidationUtils.isNotBlank(user.getDisplayName())
                    ? user.getDisplayName()
                    : userSession.getDisplayName();
            String email = user != null ? user.getEmail() : "";
            String phone = user != null ? user.getPhone() : "";
            String gender = user != null ? user.getGender() : "";
            String birthDate = user != null ? user.getBirthDate() : "";
            String address = user != null ? user.getAddress() : "";
            String loginTime = user != null ? user.getLoginTime() : "";

            binding.tvAvatarInitials.setText(FormatUtils.extractInitials(displayName));
            binding.tvProfileName.setText(displayName);
            if (ValidationUtils.isNotBlank(resolvedUsername)) {
                binding.tvProfileUsername.setVisibility(View.VISIBLE);
                binding.tvProfileUsername.setText(resolvedUsername.trim());
            } else {
                binding.tvProfileUsername.setVisibility(View.GONE);
            }

            bindPersonalInfoCard(email, phone, gender, birthDate, address);

            binding.tvProfileLoginTime.setText(
                    ValidationUtils.isNotBlank(loginTime) ? loginTime : "Vừa đăng nhập"
            );
        });
    }

    /**
     * Renders only populated personal fields with clean single dividers between visible rows,
     * hiding the entire card when no optional profile fields are set.
     */
    private void bindPersonalInfoCard(
            String email,
            String phone,
            String gender,
            String birthDate,
            String address
    ) {
        boolean anyVisible = false;
        anyVisible = bindOptionalField(binding.rowProfileEmail, null, binding.tvProfileEmail, email, anyVisible);
        anyVisible = bindOptionalField(binding.rowProfilePhone, binding.dividerProfilePhone, binding.tvProfilePhone, phone, anyVisible);
        anyVisible = bindOptionalField(binding.rowProfileGender, binding.dividerProfileGender, binding.tvProfileGender, gender, anyVisible);
        anyVisible = bindOptionalField(binding.rowProfileBirthDate, binding.dividerProfileBirthDate, binding.tvProfileBirthDate, birthDate, anyVisible);
        anyVisible = bindOptionalField(binding.rowProfileAddress, binding.dividerProfileAddress, binding.tvProfileAddress, address, anyVisible);

        binding.cardPersonalInfo.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    private boolean bindOptionalField(
            View rowContainer,
            @Nullable View dividerAbove,
            TextView valueView,
            String rawValue,
            boolean hasVisibleAbove
    ) {
        boolean hasValue = ValidationUtils.isNotBlank(rawValue);
        rowContainer.setVisibility(hasValue ? View.VISIBLE : View.GONE);
        if (dividerAbove != null) {
            dividerAbove.setVisibility(hasValue && hasVisibleAbove ? View.VISIBLE : View.GONE);
        }
        if (hasValue) {
            valueView.setText(rawValue.trim());
        }
        return hasVisibleAbove || hasValue;
    }

    private void performLogout() {
        userSession.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
