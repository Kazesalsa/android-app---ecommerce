package com.example.btday4.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityLoginBinding;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.material.snackbar.Snackbar;

/**
 * Full-bleed launcher authentication screen with inline Đăng nhập / Đăng ký tabs, real password
 * verification, and inline field errors (zero popups).
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UserSession userSession;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSession = new UserSession(this);
        if (userSession.isLoggedIn() && userSession.isRemembered()) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.loginRoot);

        if (userSession.isRemembered() && !userSession.getUsername().isEmpty()) {
            binding.etUsername.setText(userSession.getUsername());
            binding.cbRememberMe.setChecked(true);
        }

        UiUtils.clearErrorOnType(binding.etDisplayName, binding.tilDisplayName);
        UiUtils.clearErrorOnType(binding.etUsername, binding.tilUsername);
        UiUtils.clearErrorOnType(binding.etPassword, binding.tilPassword);
        UiUtils.clearErrorOnType(binding.etConfirmPassword, binding.tilConfirmPassword);

        binding.tabModeLogin.setOnClickListener(v -> setRegisterMode(false));
        binding.tabModeRegister.setOnClickListener(v -> setRegisterMode(true));

        binding.tvFillDemo.setOnClickListener(v -> {
            binding.etUsername.setText(Constants.DEMO_USERNAME);
            binding.etPassword.setText(Constants.DEMO_PASSWORD);
            binding.tilUsername.setError(null);
            binding.tilPassword.setError(null);
        });

        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && !registerMode) {
                submit();
                return true;
            }
            return false;
        });
        binding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        binding.btnLogin.setOnClickListener(v -> submit());
        applyModeUi();
    }

    private void setRegisterMode(boolean enabled) {
        if (registerMode == enabled) {
            return;
        }
        registerMode = enabled;
        TransitionManager.beginDelayedTransition(binding.llAuthForm);
        binding.tilDisplayName.setError(null);
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        binding.etConfirmPassword.setText("");
        applyModeUi();
    }

    private void applyModeUi() {
        UiUtils.styleUnderlineTab(binding.tvTabLogin, binding.indTabLogin, !registerMode);
        UiUtils.styleUnderlineTab(binding.tvTabRegister, binding.indTabRegister, registerMode);

        binding.tvAuthTitle.setText(registerMode ? R.string.auth_title_register : R.string.auth_title_login);
        binding.tvAuthSubtitle.setText(registerMode ? R.string.auth_subtitle_register : R.string.auth_subtitle_login);
        binding.tilDisplayName.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        binding.tilConfirmPassword.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        binding.tvFillDemo.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        binding.tilPassword.setHelperText(registerMode ? getString(R.string.error_password_short) : null);
        binding.etPassword.setImeOptions(registerMode ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        binding.btnLogin.setText(registerMode ? R.string.btn_create_account : R.string.btn_login);
    }

    private void submit() {
        String displayName = textOf(binding.etDisplayName).trim();
        String username = textOf(binding.etUsername).trim();
        String password = textOf(binding.etPassword);
        String confirm = textOf(binding.etConfirmPassword);

        boolean valid = true;
        if (registerMode && !ValidationUtils.isNotBlank(displayName)) {
            binding.tilDisplayName.setError(getString(R.string.error_display_name_empty));
            valid = false;
        }
        if (!ValidationUtils.isNotBlank(username)) {
            binding.tilUsername.setError(getString(R.string.error_username_empty));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_empty));
            valid = false;
        } else if (registerMode && !ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (registerMode && !confirm.equals(password)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        binding.btnLogin.setEnabled(false);
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
        switch (result) {
            case SUCCESS:
                navigateToMain();
                break;
            case ACCOUNT_NOT_FOUND:
                binding.tilUsername.setError(getString(R.string.error_account_not_found));
                Snackbar.make(binding.getRoot(), R.string.error_account_not_found, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_register, v -> setRegisterMode(true))
                        .show();
                break;
            case WRONG_PASSWORD:
                binding.tilPassword.setError(getString(R.string.error_wrong_password));
                break;
            case ACCOUNT_EXISTS:
                binding.tilUsername.setError(getString(R.string.error_account_exists));
                Snackbar.make(binding.getRoot(), R.string.error_account_exists, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_login, v -> setRegisterMode(false))
                        .show();
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
