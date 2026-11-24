package com.example.btday4.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Small UI utility helpers to avoid repetitive system-bar and form listener boilerplate.
 */
public final class UiUtils {

    private UiUtils() {
    }

    public static void applySystemBarInsets(View rootView) {
        if (rootView == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
    }

    public static void clearErrorOnType(EditText editText, TextInputLayout layout) {
        if (editText == null || layout == null) {
            return;
        }
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
