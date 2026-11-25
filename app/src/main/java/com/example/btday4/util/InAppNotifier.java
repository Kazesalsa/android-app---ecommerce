package com.example.btday4.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.btday4.R;
import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.model.AppNotification;

/**
 * Custom top-sliding in-app notification banner & Notification Center recorder replacing default
 * black Android Snackbars across the entire application.
 */
public final class InAppNotifier {

    private static final String TAG_ACTIVE_BANNER = "btday4_active_top_banner";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final long DEFAULT_DURATION_MS = 2_800L;
    private static final long ACTION_DURATION_MS = 3_800L;
    private static Runnable activeDismissTask;

    private InAppNotifier() {
    }

    public static void show(
            @Nullable View contextView,
            @NonNull String type,
            @NonNull String title,
            @NonNull CharSequence message
    ) {
        show(contextView, type, title, message, "", null, null);
    }

    public static void show(
            @Nullable View contextView,
            @NonNull String type,
            @NonNull String title,
            @NonNull CharSequence message,
            @Nullable String targetId,
            @Nullable String actionLabel,
            @Nullable Runnable action
    ) {
        if (contextView == null || message.length() == 0) {
            return;
        }
        Context context = contextView.getContext();
        recordNotification(context, type, title, message.toString(), targetId);
        showBannerOnActivity(findActivity(context), type, title, message, actionLabel, action);
    }

    public static void showBannerOnly(
            @Nullable Activity activity,
            @NonNull String type,
            @NonNull String title,
            @NonNull CharSequence message,
            @Nullable String actionLabel,
            @Nullable Runnable action
    ) {
        if (activity == null || message.length() == 0) {
            return;
        }
        showBannerOnActivity(activity, type, title, message, actionLabel, action);
    }

    /**
     * Records a notification in the Notification Center without showing a duplicate banner on the
     * current Activity (useful right before launching a new Activity that will display its own UI).
     */
    public static void recordOnly(
            @Nullable Context context,
            @NonNull String type,
            @NonNull String title,
            @NonNull String message,
            @Nullable String targetId
    ) {
        if (context == null || message.trim().isEmpty()) {
            return;
        }
        recordNotification(context, type, title, message, targetId);
    }

    private static void recordNotification(
            Context context,
            String type,
            String title,
            String message,
            @Nullable String targetId
    ) {
        UserSession session = new UserSession(context);
        NotificationRepository.push(context, session.getUsername(), type, title, message, targetId);
    }

    private static void showBannerOnActivity(
            @Nullable Activity activity,
            @NonNull String type,
            @NonNull String title,
            @NonNull CharSequence message,
            @Nullable String actionLabel,
            @Nullable Runnable action
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        View decorContent = activity.findViewById(android.R.id.content);
        if (!(decorContent instanceof ViewGroup)) {
            return;
        }
        ViewGroup container = (ViewGroup) decorContent;

        // Cancel any pending dismiss timer and remove existing banner immediately so rapid taps never leak or stack.
        if (activeDismissTask != null) {
            MAIN_HANDLER.removeCallbacks(activeDismissTask);
            activeDismissTask = null;
        }
        View previous = container.findViewWithTag(TAG_ACTIVE_BANNER);
        if (previous != null) {
            previous.animate().cancel();
            container.removeView(previous);
        }

        View banner = LayoutInflater.from(activity).inflate(R.layout.view_in_app_banner, container, false);
        banner.setTag(TAG_ACTIVE_BANNER);

        View iconBg = banner.findViewById(R.id.flBannerIconBg);
        ImageView iconView = banner.findViewById(R.id.imgBannerIcon);
        TextView titleView = banner.findViewById(R.id.tvBannerTitle);
        TextView msgView = banner.findViewById(R.id.tvBannerMessage);
        TextView actionBtn = banner.findViewById(R.id.btnBannerAction);
        View closeBtn = banner.findViewById(R.id.btnBannerClose);

        styleBannerIcon(activity, type, iconBg, iconView);
        titleView.setText(title);
        msgView.setText(message);

        final Runnable dismissTask = () -> dismissBanner(container, banner);

        if (actionLabel != null && !actionLabel.trim().isEmpty() && action != null) {
            actionBtn.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.GONE);
            actionBtn.setText(actionLabel);
            actionBtn.setOnClickListener(v -> {
                MAIN_HANDLER.removeCallbacks(dismissTask);
                dismissBanner(container, banner);
                action.run();
            });
        } else {
            actionBtn.setVisibility(View.GONE);
            closeBtn.setVisibility(View.VISIBLE);
            closeBtn.setOnClickListener(v -> {
                MAIN_HANDLER.removeCallbacks(dismissTask);
                dismissBanner(container, banner);
            });
        }

        banner.setOnClickListener(v -> {
            MAIN_HANDLER.removeCallbacks(dismissTask);
            dismissBanner(container, banner);
        });

        float density = activity.getResources().getDisplayMetrics().density;
        int sideMargin = Math.round(14f * density);
        int baseTopMargin = Math.round(10f * density);

        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(decorContent);
        int statusBarTop = 0;
        if (rootInsets != null) {
            Insets sys = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            statusBarTop = sys.top;
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP;
        lp.setMargins(sideMargin, statusBarTop + baseTopMargin, sideMargin, 0);
        banner.setLayoutParams(lp);

        banner.setAlpha(0f);
        banner.setTranslationY(-120f * density);
        container.addView(banner);

        banner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        long stayMs = (action != null) ? ACTION_DURATION_MS : DEFAULT_DURATION_MS;
        activeDismissTask = dismissTask;
        MAIN_HANDLER.postDelayed(dismissTask, stayMs);
    }

    private static void dismissBanner(ViewGroup container, View banner) {
        if (activeDismissTask != null) {
            MAIN_HANDLER.removeCallbacks(activeDismissTask);
            activeDismissTask = null;
        }
        if (banner.getParent() == null) {
            return;
        }
        float density = banner.getResources().getDisplayMetrics().density;
        banner.animate()
                .alpha(0f)
                .translationY(-100f * density)
                .setDuration(180L)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (banner.getParent() == container) {
                        container.removeView(banner);
                    }
                })
                .start();
    }

    private static void styleBannerIcon(
            Context context,
            String type,
            View iconBg,
            ImageView iconView
    ) {
        int bgColor;
        int fgColor;
        int iconRes;
        if (AppNotification.TYPE_VOUCHER.equals(type)) {
            bgColor = ContextCompat.getColor(context, R.color.primary_soft);
            fgColor = ContextCompat.getColor(context, R.color.primary);
            iconRes = R.drawable.ic_voucher;
        } else if (AppNotification.TYPE_ALERT.equals(type)) {
            bgColor = ContextCompat.getColor(context, R.color.primary_soft);
            fgColor = ContextCompat.getColor(context, R.color.danger_red);
            iconRes = R.drawable.ic_nav_bell_filled;
        } else if (AppNotification.TYPE_CART.equals(type)) {
            bgColor = ContextCompat.getColor(context, R.color.teal_trust_bg);
            fgColor = ContextCompat.getColor(context, R.color.teal_trust);
            iconRes = R.drawable.ic_shopping_bag;
        } else if (AppNotification.TYPE_ORDER.equals(type)) {
            bgColor = ContextCompat.getColor(context, R.color.teal_trust_bg);
            fgColor = ContextCompat.getColor(context, R.color.teal_trust);
            iconRes = R.drawable.ic_receipt;
        } else {
            bgColor = ContextCompat.getColor(context, R.color.teal_trust_bg);
            fgColor = ContextCompat.getColor(context, R.color.teal_trust);
            iconRes = R.drawable.ic_check_circle;
        }
        iconBg.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        iconView.setImageResource(iconRes);
        iconView.setImageTintList(ColorStateList.valueOf(fgColor));
    }

    @Nullable
    private static Activity findActivity(@Nullable Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
