package com.example.btday4.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.btday4.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    /**
     * Styles a Shopee-like underline tab: coral bold label + visible indicator when active.
     */
    public static void styleUnderlineTab(TextView label, View indicator, boolean active) {
        Context context = label.getContext();
        label.setTextColor(ContextCompat.getColor(context, active ? R.color.primary : R.color.text_secondary));
        label.setTypeface(ResourcesCompat.getFont(context, active ? R.font.nunito_bold : R.font.nunito_semibold));
        indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Simple confirmation popup for destructive actions (logout, cancel order).
     */
    public static void showConfirmDialog(
            Context context,
            String title,
            String message,
            String confirmLabel,
            Runnable onConfirm
    ) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(confirmLabel, (d, which) -> onConfirm.run())
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.danger_red));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
    }
}
