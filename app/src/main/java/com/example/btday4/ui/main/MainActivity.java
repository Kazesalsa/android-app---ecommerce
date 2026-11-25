package com.example.btday4.ui.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.btday4.R;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.NotificationRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.databinding.ActivityMainBinding;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.ui.auth.LoginActivity;
import com.example.btday4.ui.cart.CartFragment;
import com.example.btday4.ui.notifications.NotificationsFragment;
import com.example.btday4.ui.products.ProductsFragment;
import com.example.btday4.ui.profile.ProfileFragment;
import com.example.btday4.ui.vouchers.VouchersFragment;
import com.example.btday4.util.Constants;
import com.example.btday4.util.TopRightPopup;
import com.example.btday4.util.UiUtils;
import com.google.android.material.badge.BadgeDrawable;

/**
 * Main container hosting Store, Cart, and Profile tabs with Material 3
 * BottomNavigationView.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_PRODUCTS = "tag_products";
    private static final String TAG_VOUCHERS = "tag_vouchers";
    private static final String TAG_CART = "tag_cart";
    private static final String TAG_NOTIFICATIONS = "tag_notifications";
    private static final String TAG_PROFILE = "tag_profile";
    private static final String STATE_SELECTED_NAV_ID = "state_selected_nav_id";

    private ActivityMainBinding binding;
    private ProductsFragment productsFragment;
    private VouchersFragment vouchersFragment;
    private CartFragment cartFragment;
    private NotificationsFragment notificationsFragment;
    private ProfileFragment profileFragment;
    private int selectedNavId = R.id.nav_products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserSession userSession = new UserSession(this);
        if (!userSession.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applyMainShellInsets(binding.main, binding.bottomNav);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            productsFragment = new ProductsFragment();
            vouchersFragment = new VouchersFragment();
            cartFragment = new CartFragment();
            notificationsFragment = new NotificationsFragment();
            profileFragment = new ProfileFragment();
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, profileFragment, TAG_PROFILE)
                    .hide(profileFragment)
                    .add(R.id.fragmentContainer, notificationsFragment, TAG_NOTIFICATIONS)
                    .hide(notificationsFragment)
                    .add(R.id.fragmentContainer, cartFragment, TAG_CART)
                    .hide(cartFragment)
                    .add(R.id.fragmentContainer, vouchersFragment, TAG_VOUCHERS)
                    .hide(vouchersFragment)
                    .add(R.id.fragmentContainer, productsFragment, TAG_PRODUCTS)
                    .commit();
        } else {
            selectedNavId = savedInstanceState.getInt(STATE_SELECTED_NAV_ID, R.id.nav_products);
            Fragment existingProducts = fm.findFragmentByTag(TAG_PRODUCTS);
            Fragment existingVouchers = fm.findFragmentByTag(TAG_VOUCHERS);
            Fragment existingCart = fm.findFragmentByTag(TAG_CART);
            Fragment existingNotifications = fm.findFragmentByTag(TAG_NOTIFICATIONS);
            Fragment existingProfile = fm.findFragmentByTag(TAG_PROFILE);
            productsFragment = existingProducts instanceof ProductsFragment
                    ? (ProductsFragment) existingProducts
                    : new ProductsFragment();
            vouchersFragment = existingVouchers instanceof VouchersFragment
                    ? (VouchersFragment) existingVouchers
                    : new VouchersFragment();
            cartFragment = existingCart instanceof CartFragment
                    ? (CartFragment) existingCart
                    : new CartFragment();
            notificationsFragment = existingNotifications instanceof NotificationsFragment
                    ? (NotificationsFragment) existingNotifications
                    : new NotificationsFragment();
            profileFragment = existingProfile instanceof ProfileFragment
                    ? (ProfileFragment) existingProfile
                    : new ProfileFragment();
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            selectTab(item.getItemId());
            return true;
        });

        if (getIntent() != null) {
            if (getIntent().getBooleanExtra(Constants.EXTRA_OPEN_CART, false)) {
                selectedNavId = R.id.nav_cart;
            } else if (getIntent().getBooleanExtra(Constants.EXTRA_OPEN_VOUCHERS, false)) {
                selectedNavId = R.id.nav_vouchers;
            } else if (getIntent().getBooleanExtra(Constants.EXTRA_OPEN_PRODUCTS, false)) {
                selectedNavId = R.id.nav_products;
            }
        }

        if (selectedNavId != R.id.nav_products) {
            binding.bottomNav.setSelectedItemId(selectedNavId);
        }

        FirestoreHelper.seedProductsIfEmpty(null);
        FirestoreHelper.seedReviewsIfEmpty(null);
        NotificationRepository.seedNotificationsIfEmpty(this, () -> {
            if (!isFinishing() && !isDestroyed()) {
                refreshNotificationBadge();
            }
            VoucherRepository.seedVouchersIfEmpty(() -> VoucherRepository.getAllVouchers(vouchers -> {
                if (!isFinishing() && !isDestroyed()) {
                    NotificationRepository.syncNewVouchers(this, vouchers, () -> {
                        if (!isFinishing() && !isDestroyed()) {
                            refreshNotificationBadge();
                        }
                    });
                }
            }));
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra(Constants.EXTRA_OPEN_CART, false)) {
            openCartTab();
        } else if (intent.getBooleanExtra(Constants.EXTRA_OPEN_VOUCHERS, false)) {
            openVouchersTab();
        } else if (intent.getBooleanExtra(Constants.EXTRA_OPEN_PRODUCTS, false)) {
            openProductsTab();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartBadge();
        refreshNotificationBadge();
        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.onMainResumed(tabNameFor(selectedNavId));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.onMainPaused();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        TopRightPopup.dismiss();
    }

    public void openProductsTab() {
        if (binding != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_products);
        }
    }

    public void openCartTab() {
        if (binding != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_cart);
        }
    }

    public void openVouchersTab() {
        if (binding != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_vouchers);
        }
    }

    public void refreshCartBadge() {
        if (binding == null) {
            return;
        }
        CartRepository.getTotalQuantity(this, count -> {
            if (isFinishing() || isDestroyed() || binding == null) {
                return;
            }
            BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.nav_cart);
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
                badge.setBackgroundColor(ContextCompat.getColor(this, R.color.badge_bg));
                badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                badge.setVisible(false);
                badge.clearNumber();
            }
            if (productsFragment != null && productsFragment.isAdded()) {
                productsFragment.updateHeaderCartBadge(count);
            }
        });
    }

    public void refreshNotificationBadge() {
        if (binding == null) {
            return;
        }
        NotificationRepository.getUnreadCount(this, this::updateNotificationBadge);
    }

    public void updateNotificationBadge(int unread) {
        if (isFinishing() || isDestroyed() || binding == null) {
            return;
        }
        BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.nav_notifications);
        if (unread > 0) {
            badge.setVisible(true);
            badge.setNumber(unread);
            badge.setBackgroundColor(ContextCompat.getColor(this, R.color.badge_bg));
            badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_NAV_ID, selectedNavId);
    }

    private void selectTab(int navId) {
        selectedNavId = navId;
        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.onTabSelected(tabNameFor(navId));
        }
        Fragment target;
        if (navId == R.id.nav_vouchers) {
            target = vouchersFragment;
        } else if (navId == R.id.nav_cart) {
            target = cartFragment;
        } else if (navId == R.id.nav_notifications) {
            target = notificationsFragment;
        } else if (navId == R.id.nav_profile) {
            target = profileFragment;
        } else {
            target = productsFragment;
            productsFragment.refreshCatalog();
        }
        showOnlyFragment(target);
    }

    private void showOnlyFragment(Fragment target) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment[] all = { productsFragment, vouchersFragment, cartFragment, notificationsFragment, profileFragment };
        for (Fragment fragment : all) {
            if (fragment == target) {
                if (!fragment.isAdded()) {
                    tx.add(R.id.fragmentContainer, fragment, tagFor(fragment));
                }
                tx.show(fragment);
            } else if (fragment.isAdded()) {
                tx.hide(fragment);
            }
        }
        tx.commit();
    }

    private String tabNameFor(int navId) {
        if (navId == R.id.nav_vouchers) {
            return "Vouchers";
        }
        if (navId == R.id.nav_cart) {
            return "Cart";
        }
        if (navId == R.id.nav_notifications) {
            return "Notifications";
        }
        if (navId == R.id.nav_profile) {
            return "Profile";
        }
        return "Products";
    }

    private String tagFor(Fragment fragment) {
        if (fragment instanceof VouchersFragment) {
            return TAG_VOUCHERS;
        }
        if (fragment instanceof CartFragment) {
            return TAG_CART;
        }
        if (fragment instanceof NotificationsFragment) {
            return TAG_NOTIFICATIONS;
        }
        if (fragment instanceof ProfileFragment) {
            return TAG_PROFILE;
        }
        return TAG_PRODUCTS;
    }
}
