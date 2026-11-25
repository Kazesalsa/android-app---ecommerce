package com.example.btday4.util;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.btday4.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

/**
 * UI utility helpers for Android 15+ (targetSdk 37) edge-to-edge & IME keyboard insets,
 * status-bar icon contrast, and underline tab styling.
 */
public final class UiUtils {

    private UiUtils() {
    }

    public static void applySystemBarInsets(View rootView) {
        if (rootView == null) {
            return;
        }
        final int baseLeft = rootView.getPaddingLeft();
        final int baseTop = rootView.getPaddingTop();
        final int baseRight = rootView.getPaddingRight();
        final int baseBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(sys.bottom, ime.bottom);
            v.setPadding(baseLeft + sys.left, baseTop + sys.top, baseRight + sys.right, baseBottom + bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void applyEdgeToEdgeBars(
            View rootView,
            @Nullable View topHeader,
            @Nullable View bottomBar
    ) {
        if (rootView == null) {
            return;
        }
        final int topBase = topHeader != null ? topHeader.getPaddingTop() : 0;
        final int topLeft = topHeader != null ? topHeader.getPaddingLeft() : 0;
        final int topRight = topHeader != null ? topHeader.getPaddingRight() : 0;
        final int topBottom = topHeader != null ? topHeader.getPaddingBottom() : 0;

        final int botLeft = bottomBar != null ? bottomBar.getPaddingLeft() : 0;
        final int botTop = bottomBar != null ? bottomBar.getPaddingTop() : 0;
        final int botRight = bottomBar != null ? bottomBar.getPaddingRight() : 0;
        final int botBase = bottomBar != null ? bottomBar.getPaddingBottom() : 0;

        final int rootBaseBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(sys.bottom, ime.bottom);

            if (topHeader != null) {
                topHeader.setPadding(topLeft + sys.left, topBase + sys.top, topRight + sys.right, topBottom);
                v.setPadding(0, 0, 0, bottomBar == null ? rootBaseBottom + bottomInset : 0);
            } else {
                v.setPadding(sys.left, sys.top, sys.right, bottomBar == null ? rootBaseBottom + bottomInset : 0);
            }

            if (bottomBar != null) {
                bottomBar.setPadding(botLeft + sys.left, botTop, botRight + sys.right, botBase + bottomInset);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void applyMainShellInsets(View rootView, View bottomBar) {
        if (rootView == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardOpen = ime.bottom > sys.bottom;
            v.setPadding(sys.left, sys.top, sys.right, keyboardOpen ? ime.bottom : 0);
            if (bottomBar != null) {
                bottomBar.setVisibility(keyboardOpen ? View.GONE : View.VISIBLE);
                bottomBar.setPadding(0, 0, 0, sys.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        if (bottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, windowInsets) -> WindowInsetsCompat.CONSUMED);
        }
    }

    public static void setStatusBarLightIcons(Activity activity, boolean darkIconsOnLightBg) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(darkIconsOnLightBg);
        controller.setAppearanceLightNavigationBars(true);
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

    public static void styleUnderlineTab(TextView label, View indicator, boolean active) {
        Context context = label.getContext();
        label.setTextColor(ContextCompat.getColor(context, active ? R.color.primary : R.color.text_secondary));
        label.setTypeface(ResourcesCompat.getFont(
                context,
                active ? R.font.be_vietnam_pro_bold : R.font.be_vietnam_pro_medium
        ));
        indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Confirmation popup for destructive actions (logout, cancel order) — kept per user instruction.
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
