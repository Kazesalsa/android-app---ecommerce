package com.example.btday4.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityLoginBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;

/**
 * Full-bleed launcher authentication screen with segmented Đăng nhập / Đăng ký pill switcher,
 * seamless status bar bleed, and custom top notification banner feedback.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UserSession userSession;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSession = new UserSession(this);
        if (userSession.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, false);
        UiUtils.applyEdgeToEdgeBars(binding.loginRoot, binding.llLoginBrandHeader, null);

        UiUtils.clearErrorOnType(binding.etDisplayName, binding.tilDisplayName);
        UiUtils.clearErrorOnType(binding.etUsername, binding.tilUsername);
        UiUtils.clearErrorOnType(binding.etPassword, binding.tilPassword);
        UiUtils.clearErrorOnType(binding.etConfirmPassword, binding.tilConfirmPassword);

        binding.tabModeLogin.setOnClickListener(v -> setRegisterMode(false));
        binding.tabModeRegister.setOnClickListener(v -> setRegisterMode(true));

        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (!registerMode && actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSubmit();
                return true;
            }
            return false;
        });

        binding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (registerMode && actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSubmit();
                return true;
            }
            return false;
        });

        binding.btnLogin.setOnClickListener(v -> attemptSubmit());
        styleAuthTabs();
    }

    private void setRegisterMode(boolean isRegister) {
        if (registerMode == isRegister) {
            return;
        }
        registerMode = isRegister;
        TransitionManager.beginDelayedTransition(binding.llAuthForm);

        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilDisplayName.setError(null);
        binding.tilConfirmPassword.setError(null);

        styleAuthTabs();
        if (registerMode) {
            binding.tvAuthTitle.setText(R.string.auth_title_register);
            binding.tvAuthSubtitle.setText(R.string.auth_subtitle_register);
            binding.tilDisplayName.setVisibility(View.VISIBLE);
            binding.tilConfirmPassword.setVisibility(View.VISIBLE);
            binding.etPassword.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            binding.btnLogin.setText(R.string.btn_create_account);
        } else {
            binding.tvAuthTitle.setText(R.string.auth_title_login);
            binding.tvAuthSubtitle.setText(R.string.auth_subtitle_login);
            binding.tilDisplayName.setVisibility(View.GONE);
            binding.tilConfirmPassword.setVisibility(View.GONE);
            binding.etPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
            binding.btnLogin.setText(R.string.btn_login);
        }
    }

    private void styleAuthTabs() {
        styleTab(binding.tabModeLogin, !registerMode);
        styleTab(binding.tabModeRegister, registerMode);
    }

    private void styleTab(TextView tab, boolean active) {
        tab.setBackgroundResource(active ? R.drawable.bg_segmented_active : 0);
        tab.setTextColor(ContextCompat.getColor(
                this,
                active ? R.color.text_primary : R.color.text_secondary
        ));
        tab.setTypeface(ResourcesCompat.getFont(
                this,
                active ? R.font.be_vietnam_pro_bold : R.font.be_vietnam_pro_medium
        ));
    }

    private void attemptSubmit() {
        String displayName = textOf(binding.etDisplayName).trim();
        String username = textOf(binding.etUsername).trim();
        String password = textOf(binding.etPassword);
        String confirmPassword = textOf(binding.etConfirmPassword);

        boolean valid = true;
        if (registerMode && displayName.isEmpty()) {
            binding.tilDisplayName.setError(getString(R.string.error_display_name_empty));
            valid = false;
        } else {
            binding.tilDisplayName.setError(null);
        }

        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.error_username_empty));
            valid = false;
        } else {
            binding.tilUsername.setError(null);
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_empty));
            valid = false;
        } else if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        } else {
            binding.tilPassword.setError(null);
        }

        if (registerMode) {
            if (!password.equals(confirmPassword)) {
                binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
                valid = false;
            } else {
                binding.tilConfirmPassword.setError(null);
            }
        }

        if (!valid) {
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText(registerMode ? "Đang tạo tài khoản..." : "Đang đăng nhập...");
        boolean remember = binding.cbRememberMe.isChecked();
        if (registerMode) {
            userSession.register(username, displayName, password, remember, this::handleAuthResult);
        } else {
            userSession.login(username, password, remember, this::handleAuthResult);
        }
    }

    private void handleAuthResult(UserSession.AuthResult result) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        binding.btnLogin.setEnabled(true);
        binding.btnLogin.setText(registerMode ? R.string.btn_create_account : R.string.btn_login);
        switch (result) {
            case SUCCESS:
                navigateToMain();
                break;
            case ACCOUNT_NOT_FOUND:
                binding.tilUsername.setError(getString(R.string.error_account_not_found));
                InAppNotifier.showBannerOnly(
                        this,
                        AppNotification.TYPE_ALERT,
                        "Tài khoản chưa đăng ký",
                        getString(R.string.error_account_not_found),
                        getString(R.string.btn_register),
                        () -> setRegisterMode(true)
                );
                break;
            case WRONG_PASSWORD:
                binding.tilPassword.setError(getString(R.string.error_wrong_password));
                break;
            case ACCOUNT_EXISTS:
                binding.tilUsername.setError(getString(R.string.error_account_exists));
                InAppNotifier.showBannerOnly(
                        this,
                        AppNotification.TYPE_ALERT,
                        "Tài khoản đã tồn tại",
                        getString(R.string.error_account_exists),
                        getString(R.string.btn_login),
                        () -> setRegisterMode(false)
                );
                break;
            case NETWORK_ERROR:
                InAppNotifier.showBannerOnly(
                        this,
                        AppNotification.TYPE_ALERT,
                        "Lỗi kết nối",
                        "Không thể kết nối Firebase (kiểm tra mạng hoặc Firestore Rules)",
                        null,
                        null
                );
                break;
        }
    }

    private static String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
