package com.example.btday4.util;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.btday4.R;
import java.lang.ref.WeakReference;

/**
 * Floating notification card anchored at the top-right edge for cart and quick
 * feedback.
 */
public final class TopRightPopup {

    private static final long AUTO_DISMISS_MS = 3_200L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static WeakReference<PopupWindow> activePopupRef;
    private static Runnable pendingDismiss;

    private TopRightPopup() {
    }

    public static void showCartAdded(
            Activity activity,
            String title,
            String message,
            @Nullable String productImageUrl,
            @Nullable String actionLabel,
            @Nullable Runnable onAction) {
        show(activity, title, message, productImageUrl, actionLabel, true, onAction);
    }

    public static void showInfo(
            Activity activity,
            String title,
            String message) {
        show(activity, title, message, null, null, true, null);
    }

    public static void showWarning(
            Activity activity,
            String title,
            String message) {
        show(activity, title, message, null, null, false, null);
    }

    public static void dismiss() {
        dismissCurrentImmediate();
    }

    private static void show(
            Activity activity,
            String title,
            String message,
            @Nullable String productImageUrl,
            @Nullable String actionLabel,
            boolean isSuccess,
            @Nullable Runnable onAction) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        View decorView = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        if (decorView == null || decorView.getWindowToken() == null) {
            return;
        }

        dismissCurrentImmediate();

        View root = LayoutInflater.from(activity).inflate(R.layout.layout_top_right_popup, null, false);
        View card = root.findViewById(R.id.popupCard);
        View thumbWrap = root.findViewById(R.id.popupThumbWrap);
        ImageView imgProduct = root.findViewById(R.id.imgPopupProduct);
        View iconWrap = root.findViewById(R.id.popupIconWrap);
        ImageView imgIcon = root.findViewById(R.id.imgPopupIcon);
        ImageView imgMiniBadge = root.findViewById(R.id.imgPopupMiniBadge);
        TextView tvTitle = root.findViewById(R.id.tvPopupTitle);
        TextView tvMessage = root.findViewById(R.id.tvPopupMessage);
        TextView btnAction = root.findViewById(R.id.btnPopupAction);
        ImageButton btnClose = root.findViewById(R.id.btnPopupClose);
        View progressBar = root.findViewById(R.id.viewPopupProgress);

        tvTitle.setText(title);
        tvMessage.setText(message);

        boolean hasThumb = isSuccess && productImageUrl != null && !productImageUrl.trim().isEmpty();
        if (hasThumb) {
            thumbWrap.setVisibility(View.VISIBLE);
            imgMiniBadge.setVisibility(View.VISIBLE);
            iconWrap.setVisibility(View.GONE);
            ProductImageLoader.load(imgProduct, productImageUrl);
        } else {
            thumbWrap.setVisibility(View.GONE);
            imgMiniBadge.setVisibility(View.GONE);
            iconWrap.setVisibility(View.VISIBLE);
            if (isSuccess) {
                iconWrap.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.teal_trust_bg)));
                imgIcon.setImageResource(R.drawable.ic_check_circle);
                imgIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.teal_trust)));
            } else {
                iconWrap.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.warning_bg)));
                imgIcon.setImageResource(R.drawable.ic_nav_cart);
                imgIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.warning_amber)));
            }
        }

        progressBar.setBackgroundColor(ContextCompat.getColor(
                activity,
                isSuccess ? R.color.teal_trust : R.color.warning_amber));

        PopupWindow popup = new PopupWindow(
                root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(false);
        popup.setClippingEnabled(false);

        if (actionLabel != null && !actionLabel.trim().isEmpty() && onAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(actionLabel);
            View.OnClickListener actionClick = v -> {
                dismissCurrentImmediate();
                onAction.run();
            };
            btnAction.setOnClickListener(actionClick);
            card.setOnClickListener(actionClick);
        } else {
            btnAction.setVisibility(View.GONE);
            card.setOnClickListener(v -> dismissAnimated(popup, card));
        }

        btnClose.setOnClickListener(v -> dismissAnimated(popup, card));

        float density = activity.getResources().getDisplayMetrics().density;
        int marginEnd = Math.round(8 * density);
        int topOffset = resolveTopOffset(decorView, density);

        activePopupRef = new WeakReference<>(popup);
        popup.showAtLocation(decorView, Gravity.TOP | Gravity.END, marginEnd, topOffset);

        float slideDist = 100f * density;
        card.setTranslationX(slideDist);
        card.setScaleX(0.94f);
        card.setScaleY(0.94f);
        card.setAlpha(0f);
        card.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(280L)
                .setInterpolator(new OvershootInterpolator(0.85f))
                .start();

        progressBar.setPivotX(0f);
        progressBar.setScaleX(1f);
        progressBar.animate()
                .scaleX(0f)
                .setDuration(AUTO_DISMISS_MS)
                .setInterpolator(new LinearInterpolator())
                .start();

        pendingDismiss = () -> dismissAnimated(popup, card);
        MAIN_HANDLER.postDelayed(pendingDismiss, AUTO_DISMISS_MS);
    }

    private static int resolveTopOffset(View decorView, float density) {
        int statusTop = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
        if (insets != null) {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusTop = sys.top;
        }
        return statusTop + Math.round(12 * density);
    }

    private static void dismissAnimated(PopupWindow popup, View card) {
        if (pendingDismiss != null) {
            MAIN_HANDLER.removeCallbacks(pendingDismiss);
            pendingDismiss = null;
        }
        if (popup == null || !popup.isShowing()) {
            return;
        }
        float slideDist = 100f * card.getResources().getDisplayMetrics().density;
        card.animate()
                .translationX(slideDist)
                .alpha(0f)
                .setDuration(180L)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    try {
                        if (popup.isShowing()) {
                            popup.dismiss();
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                })
                .start();
    }

    private static void dismissCurrentImmediate() {
        if (pendingDismiss != null) {
            MAIN_HANDLER.removeCallbacks(pendingDismiss);
            pendingDismiss = null;
        }
        if (activePopupRef != null) {
            PopupWindow prev = activePopupRef.get();
            if (prev != null && prev.isShowing()) {
                try {
                    prev.dismiss();
                } catch (IllegalArgumentException ignored) {
                }
            }
            activePopupRef = null;
        }
    }
}
