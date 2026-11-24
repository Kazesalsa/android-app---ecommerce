package com.example.btday4.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityLoginBinding;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;

/**
 * Full-bleed launcher authentication screen with inline blur/submit validation and zero popups.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UserSession userSession;

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

        UiUtils.clearErrorOnType(binding.etUsername, binding.tilUsername);
        UiUtils.clearErrorOnType(binding.etPassword, binding.tilPassword);

        // Validate non-empty fields on blur without premature errors while typing
        binding.etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && binding.etPassword.getText() != null) {
                String value = binding.etPassword.getText().toString();
                if (!value.isEmpty() && !ValidationUtils.isValidPassword(value)) {
                    binding.tilPassword.setError(getString(R.string.error_password_short));
                }
            }
        });

        binding.tvFillDemo.setOnClickListener(v -> {
            binding.etUsername.setText("tantai@techstore.vn");
            binding.etPassword.setText("123456");
            binding.tilUsername.setError(null);
            binding.tilPassword.setError(null);
        });

        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = binding.etUsername.getText() == null
                ? ""
                : binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText() == null
                ? ""
                : binding.etPassword.getText().toString();

        boolean valid = true;
        if (!ValidationUtils.isNotBlank(username)) {
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

        if (!valid) {
            return;
        }

        binding.btnLogin.setEnabled(false);
        userSession.saveLogin(username, password, binding.cbRememberMe.isChecked(), () -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            navigateToMain();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
