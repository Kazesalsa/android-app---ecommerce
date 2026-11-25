package com.example.btday4.ui.profile;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityChangePasswordBinding;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;

/**
 * Full-screen password update flow with edge-to-edge + IME-safe insets and clear loading state.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(
                binding.changePasswordRoot,
                binding.toolbarChangePassword,
                binding.llChangePassBottomBar
        );

        userSession = new UserSession(this);

        binding.toolbarChangePassword.setNavigationOnClickListener(v -> finish());
        UiUtils.clearErrorOnType(binding.etCurrentPassword, binding.tilCurrentPassword);
        UiUtils.clearErrorOnType(binding.etNewPassword, binding.tilNewPassword);
        UiUtils.clearErrorOnType(binding.etConfirmPassword, binding.tilConfirmPassword);

        binding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitPasswordChange();
                return true;
            }
            return false;
        });

        binding.btnSubmitPasswordChange.setOnClickListener(v -> submitPasswordChange());
    }

    private void submitPasswordChange() {
        String current = binding.etCurrentPassword.getText() == null
                ? ""
                : binding.etCurrentPassword.getText().toString();
        String next = binding.etNewPassword.getText() == null
                ? ""
                : binding.etNewPassword.getText().toString();
        String confirm = binding.etConfirmPassword.getText() == null
                ? ""
                : binding.etConfirmPassword.getText().toString();

        boolean valid = true;
        if (current.isEmpty()) {
            binding.tilCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(next)) {
            binding.tilNewPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!next.equals(confirm)) {
            binding.tilConfirmPassword.setError("Mật khẩu nhập lại không khớp");
            valid = false;
        }
        if (!valid) {
            return;
        }

        binding.btnSubmitPasswordChange.setEnabled(false);
        binding.btnSubmitPasswordChange.setText("Đang cập nhật...");
        userSession.changePassword(current, next, success -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (Boolean.TRUE.equals(success)) {
                setResult(RESULT_OK);
                finish();
            } else {
                binding.btnSubmitPasswordChange.setEnabled(true);
                binding.btnSubmitPasswordChange.setText("Cập nhật mật khẩu");
                binding.tilCurrentPassword.setError("Mật khẩu hiện tại không đúng");
            }
        });
    }
}
