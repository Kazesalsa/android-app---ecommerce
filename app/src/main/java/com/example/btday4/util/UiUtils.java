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

    /**
     * Applies system-bar insets once on the root view and consumes them so child views
     * (such as Material toolbars or bottom bars) do not double-pad.
     */
    public static void applySystemBarInsets(View rootView) {
        if (rootView == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Applies top/side system-bar insets to the main shell while routing the bottom gesture/nav
     * inset exclusively to the bottom bar so it sits flush with zero double-gap underneath.
     */
    public static void applyMainShellInsets(View rootView, View bottomBar) {
        if (rootView == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            if (bottomBar != null) {
                bottomBar.setPadding(0, 0, 0, insets.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        if (bottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, windowInsets) -> WindowInsetsCompat.CONSUMED);
        }
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
