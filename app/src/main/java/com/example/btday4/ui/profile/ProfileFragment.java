package com.example.btday4.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.btday4.R;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.FragmentProfileBinding;
import com.example.btday4.model.ProfileCompletion;
import com.example.btday4.ui.auth.LoginActivity;
import com.example.btday4.ui.orders.OrderHistoryActivity;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.material.snackbar.Snackbar;

/**
 * Full-bleed Account Hub with native list navigation to Orders, My Reviews, Edit Profile, and
 * Change Password, plus two-step inline logout confirmation (zero popups).
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserSession userSession;
    private String currentPassword = "";
    private boolean isPasswordVisible = false;

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
                if (result.getResultCode() == Activity.RESULT_OK) {
                    isPasswordVisible = false;
                    refreshProfileData();
                    if (isAdded() && binding != null) {
                        Snackbar.make(binding.getRoot(), "Đã cập nhật mật khẩu", Snackbar.LENGTH_SHORT).show();
                    }
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

        View.OnClickListener openEditProfile = v ->
                editProfileLauncher.launch(new Intent(requireContext(), EditProfileActivity.class));
        binding.btnEditProfileHeader.setOnClickListener(openEditProfile);
        binding.btnCompleteProfileNow.setOnClickListener(openEditProfile);
        binding.rowEditProfile.setOnClickListener(openEditProfile);

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

        binding.tvTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            updatePasswordDisplay();
        });

        // Two-step inline logout confirmation (no AlertDialog popup)
        binding.btnLogout.setOnClickListener(v -> showInlineLogoutConfirm(true));
        binding.btnCancelLogout.setOnClickListener(v -> showInlineLogoutConfirm(false));
        binding.btnConfirmLogout.setOnClickListener(v -> performLogout());

        refreshProfileData();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            showInlineLogoutConfirm(false);
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
            currentPassword = user != null && user.getPassword() != null ? user.getPassword() : "";

            binding.tvAvatarInitials.setText(FormatUtils.extractInitials(displayName));
            binding.tvProfileName.setText(displayName);
            binding.tvProfileUsername.setText(resolvedUsername.isEmpty() ? "(Chưa có)" : resolvedUsername);

            bindFieldValue(binding.tvProfileEmail, email);
            bindFieldValue(binding.tvProfilePhone, phone);
            bindFieldValue(binding.tvProfileGender, gender);
            bindFieldValue(binding.tvProfileBirthDate, birthDate);
            bindFieldValue(binding.tvProfileAddress, address);

            binding.tvProfileLoginTime.setText(
                    ValidationUtils.isNotBlank(loginTime) ? loginTime : "Vừa đăng nhập"
            );
            updatePasswordDisplay();

            ProfileCompletion completion = ProfileCompletion.evaluate(user);
            binding.tvShortcutCompletion.setText(completion.getPercent() + "%");
            if (completion.isComplete()) {
                binding.bannerProfileCompletion.setVisibility(View.GONE);
            } else {
                binding.bannerProfileCompletion.setVisibility(View.VISIBLE);
                binding.tvCompletionPercent.setText(completion.getPercent() + "%");
                binding.progressCompletion.setProgress(completion.getPercent());
                binding.tvCompletionMissing.setText("Còn thiếu: " + completion.getMissingSummary());
            }
        });
    }

    private void showInlineLogoutConfirm(boolean show) {
        if (binding == null) {
            return;
        }
        TransitionManager.beginDelayedTransition(binding.profileContentRoot);
        binding.btnLogout.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.llLogoutConfirmBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void bindFieldValue(TextView target, String value) {
        if (ValidationUtils.isNotBlank(value)) {
            target.setText(value.trim());
            target.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            target.setText(R.string.not_updated);
            target.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        }
    }

    private void updatePasswordDisplay() {
        if (binding == null) {
            return;
        }
        if (currentPassword.isEmpty()) {
            binding.tvProfilePassword.setText("(Không có)");
            binding.tvTogglePassword.setVisibility(View.GONE);
            return;
        }
        binding.tvTogglePassword.setVisibility(View.VISIBLE);
        if (isPasswordVisible) {
            binding.tvProfilePassword.setText(currentPassword);
            binding.tvTogglePassword.setText("Ẩn");
        } else {
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < currentPassword.length(); i++) {
                masked.append('•');
            }
            binding.tvProfilePassword.setText(masked.toString());
            binding.tvTogglePassword.setText("Hiện");
        }
    }

    private void performLogout() {
        userSession.logout(() -> {
            if (!isAdded()) {
                return;
            }
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
