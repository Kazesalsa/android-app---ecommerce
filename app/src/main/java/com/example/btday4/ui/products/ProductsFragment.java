package com.example.btday4.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.FragmentProductsBinding;
import com.example.btday4.databinding.ItemHomeVoucherBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.ProductWithRating;
import com.example.btday4.model.Voucher;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.tracking.RecommendationEngine;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.AvatarImageLoader;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.GridSpacingItemDecoration;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.UiUtils;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Flagship TechStore feed featuring user avatar header, quick voucher wallet
 * access,
 * AI-ranked spotlight hero, horizontal claimable voucher strip, 4-tab sort bar,
 * and 2-column grid.
 */
public class ProductsFragment extends Fragment {

    private enum SortOption {
        DEFAULT,
        RATING_DESC,
        PRICE_ASC,
        PRICE_DESC
    }

    private FragmentProductsBinding binding;
    private UserSession userSession;
    private ProductAdapter adapter;
    private ListenerRegistration productsListener;
    private ListenerRegistration homeVouchersListener;

    private final List<ProductWithRating> allProducts = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final Map<String, Double> recommendationScores = new HashMap<>();
    private String currentUserGender = "";
    private String selectedCategory = "";
    private String searchQuery = "";
    private SortOption currentSort = SortOption.DEFAULT;
    private Product spotlightProduct;
    private int lastFilteredCount = 0;
    private final Handler searchTrackHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchTrack;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userSession = new UserSession(requireContext());
        refreshGreeting();
        refreshUserGender();

        adapter = new ProductAdapter(this::openProductDetail, this::quickAddToCart);
        int spacingPx = Math.round(10 * getResources().getDisplayMetrics().density);
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.addItemDecoration(new GridSpacingItemDecoration(2, spacingPx));
        binding.rvProducts.setAdapter(adapter);

        binding.flHeaderAvatar.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfileTab();
            }
        });

        View.OnClickListener openVouchers = v -> startActivity(
                new Intent(requireContext(), VoucherWalletActivity.class));
        binding.btnHeaderVouchers.setOnClickListener(openVouchers);
        binding.btnOpenVoucherWallet.setOnClickListener(openVouchers);

        binding.btnHomeCart.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCartTab();
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearch.setText(""));

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                flushPendingSearchTrack();
                return true;
            }
            return false;
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                binding.btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSort();
                scheduleSearchTracking(searchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.cardSpotlight.setOnClickListener(v -> {
            if (spotlightProduct != null) {
                openProductDetail(spotlightProduct);
            }
        });

        binding.tabSortDefault.setOnClickListener(v -> setSortOption(SortOption.DEFAULT));
        binding.tabSortRating.setOnClickListener(v -> setSortOption(SortOption.RATING_DESC));
        binding.tabSortPriceAsc.setOnClickListener(v -> setSortOption(SortOption.PRICE_ASC));
        binding.tabSortPriceDesc.setOnClickListener(v -> setSortOption(SortOption.PRICE_DESC));
        updateSortTabStyles();

        binding.btnResetFilter.setOnClickListener(v -> resetFilters());

        startListeningHomeVouchers();

        productsListener = FirestoreHelper.listenProducts(new FirestoreHelper.FirestoreCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> remoteProducts) {
                if (!isAdded() || binding == null || remoteProducts == null || remoteProducts.isEmpty()) {
                    return;
                }
                if (allProducts.isEmpty()) {
                    updateProductData(ProductRepository.withoutRatings(remoteProducts));
                }
                ProductRepository.combineWithLiveRatings(remoteProducts, synced -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    updateProductData(synced);
                });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && binding != null) {
                    binding.progressProducts.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshGreeting();
        refreshHeaderCartBadge();
        refreshHomeVouchers();
    }

    @Override
    public void onPause() {
        super.onPause();
        flushPendingSearchTrack();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            flushPendingSearchTrack();
        } else {
            refreshGreeting();
            refreshHeaderCartBadge();
            refreshHomeVouchers();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearchTrack != null) {
            searchTrackHandler.removeCallbacks(pendingSearchTrack);
            pendingSearchTrack = null;
        }
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
        if (homeVouchersListener != null) {
            homeVouchersListener.remove();
            homeVouchersListener = null;
        }
        binding = null;
    }

    public void refreshCatalog() {
        if (!isAdded() || binding == null) {
            return;
        }
        ProductRepository.getAllWithRating(requireContext(), products -> {
            if (!isAdded() || binding == null) {
                return;
            }
            updateProductData(products);
        });
    }

    public void refreshHeaderCartBadge() {
        if (!isAdded() || binding == null) {
            return;
        }
        CartRepository.getTotalQuantity(requireContext(), count -> {
            if (!isAdded() || binding == null) {
                return;
            }
            if (count > 0) {
                binding.tvHomeCartBadge.setVisibility(View.VISIBLE);
                binding.tvHomeCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.tvHomeCartBadge.setVisibility(View.GONE);
            }
        });
    }

    private void startListeningHomeVouchers() {
        if (homeVouchersListener != null) {
            homeVouchersListener.remove();
            homeVouchersListener = null;
        }
        if (!isAdded() || binding == null || userSession == null) {
            return;
        }
        homeVouchersListener = VoucherRepository.listenVouchersForUser(
                userSession.getUsername(),
                this::renderHomeVouchers);
    }

    private void refreshHomeVouchers() {
        if (!isAdded() || binding == null || userSession == null) {
            return;
        }
        VoucherRepository.getVouchersForUser(requireContext(), userSession.getUsername(), this::renderHomeVouchers);
    }

    private void renderHomeVouchers(List<VoucherItem> list) {
        if (!isAdded() || binding == null) {
            return;
        }
        binding.llHomeVouchers.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        long now = System.currentTimeMillis();
        int shown = 0;
        for (VoucherItem row : list) {
            if (row.used || !row.voucher.isActive() || row.voucher.isExpired(now) || !row.voucher.hasRemainingUses()) {
                continue;
            }
            Voucher v = row.voucher;
            ItemHomeVoucherBinding itemBinding = ItemHomeVoucherBinding.inflate(
                    inflater,
                    binding.llHomeVouchers,
                    false);
            itemBinding.llMiniStub.setBackgroundResource(
                    v.isFreeShipType()
                            ? R.drawable.bg_voucher_stub_teal
                            : R.drawable.bg_voucher_stub_primary);
            itemBinding.tvMiniBadge.setText(v.getBadgeLabel());
            itemBinding.tvMiniTitle.setText(v.getTitle());
            itemBinding.tvMiniCondition.setText("Đơn từ " + FormatUtils.formatVnd(v.getMinOrderAmount()));

            if (row.claimed) {
                itemBinding.btnMiniClaim.setText("Đã lưu");
                itemBinding.btnMiniClaim.setBackgroundResource(R.drawable.bg_discount_tag);
                itemBinding.btnMiniClaim.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary));
            } else {
                itemBinding.btnMiniClaim.setText("Lưu");
                itemBinding.btnMiniClaim.setBackgroundResource(R.drawable.bg_discount_solid);
                itemBinding.btnMiniClaim.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.white));
            }

            itemBinding.getRoot().setOnClickListener(click -> {
                if (!row.claimed) {
                    VoucherRepository.claimByCode(
                            requireContext(),
                            userSession.getUsername(),
                            v.getCode(),
                            outcome -> {
                                if (!isAdded() || binding == null) {
                                    return;
                                }
                                if (outcome.result == VoucherRepository.ClaimResult.CLAIMED
                                        || outcome.result == VoucherRepository.ClaimResult.ALREADY_CLAIMED) {
                                    showHomeNotification(
                                            AppNotification.TYPE_VOUCHER,
                                            "Đã lưu mã ưu đãi",
                                            "Đã lưu mã " + v.getCode() + " vào kho voucher",
                                            null,
                                            null);
                                } else {
                                    showHomeNotification(
                                            AppNotification.TYPE_ALERT,
                                            "Không thể lưu mã",
                                            "Mã " + v.getCode() + " hiện không khả dụng trên hệ thống",
                                            null,
                                            null);
                                }
                            });
                } else {
                    startActivity(new Intent(requireContext(), VoucherWalletActivity.class));
                }
            });

            binding.llHomeVouchers.addView(itemBinding.getRoot());
            shown++;
            if (shown >= 5) {
                break;
            }
        }
        binding.sectionHomeVouchers.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
    }

    private void quickAddToCart(Product product) {
        if (!isAdded() || product == null) {
            return;
        }
        CartRepository.addItem(requireContext(), product.getId(), 1, added -> {
            if (!isAdded() || binding == null) {
                return;
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCartBadge();
            } else {
                refreshHeaderCartBadge();
            }
            if (added > 0) {
                BehaviorTracker tracker = BehaviorTracker.getInstance();
                if (tracker != null) {
                    tracker.trackAction(
                            "MainActivity:Products",
                            BehaviorTracker.EVENT_ADD_TO_CART,
                            product.getId(),
                            product.getName() + "|qty=" + added);
                }
                showHomeNotification(
                        AppNotification.TYPE_CART,
                        "Đã thêm vào giỏ hàng",
                        "Đã thêm " + product.getName() + " vào giỏ",
                        "Xem giỏ",
                        () -> {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).openCartTab();
                            }
                        });
            } else {
                showHomeNotification(
                        AppNotification.TYPE_ALERT,
                        "Giới hạn tồn kho",
                        "Đã đạt số lượng tồn kho tối đa trong giỏ",
                        null,
                        null);
            }
        });
    }

    private void showHomeNotification(
            String type,
            String title,
            String message,
            @Nullable String actionLabel,
            @Nullable Runnable action) {
        if (!isAdded() || binding == null) {
            return;
        }
        InAppNotifier.show(binding.getRoot(), type, title, message, "", actionLabel, action);
    }

    private void updateProductData(List<ProductWithRating> products) {
        allProducts.clear();
        if (products != null) {
            allProducts.addAll(products);
        }
        if (binding != null) {
            binding.progressProducts.setVisibility(View.GONE);
        }
        bindSpotlightProduct();
        rebuildCategoryChipsIfNeeded();
        applyFilterAndSort();
        refreshRecommendationScores();
    }

    private void refreshUserGender() {
        if (userSession == null) {
            return;
        }
        userSession.loadCurrentUser(user -> {
            if (!isAdded() || binding == null) {
                return;
            }
            String loadedGender = user != null && user.getGender() != null ? user.getGender() : "";
            if (!loadedGender.equals(currentUserGender)) {
                currentUserGender = loadedGender;
                refreshRecommendationScores();
            }
            refreshGreeting();
        });
    }

    private void refreshRecommendationScores() {
        if (!isAdded() || binding == null || allProducts.isEmpty() || userSession == null) {
            return;
        }
        RecommendationEngine.loadScores(
                requireContext(),
                userSession.getUsername(),
                currentUserGender,
                allProducts,
                scores -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    recommendationScores.clear();
                    recommendationScores.putAll(scores);
                    bindSpotlightProduct();
                    applyFilterAndSort();
                });
    }

    private void bindSpotlightProduct() {
        spotlightProduct = null;
        if (!recommendationScores.isEmpty()) {
            double bestScore = Double.NEGATIVE_INFINITY;
            for (ProductWithRating item : allProducts) {
                if (item.product.getStock() <= 0) {
                    continue;
                }
                double score = recommendationScores.getOrDefault(item.product.getId(), Double.NEGATIVE_INFINITY);
                if (score > bestScore) {
                    bestScore = score;
                    spotlightProduct = item.product;
                }
            }
        }
        if (spotlightProduct == null) {
            for (ProductWithRating item : allProducts) {
                if ("iphone_16_pro_max".equals(item.product.getId())) {
                    spotlightProduct = item.product;
                    break;
                }
            }
        }
        if (spotlightProduct == null && !allProducts.isEmpty()) {
            spotlightProduct = allProducts.get(0).product;
        }
        if (spotlightProduct != null) {
            binding.tvSpotlightName.setText(spotlightProduct.getName());
            binding.tvSpotlightSubtitle.setText(spotlightProduct.getShortInfo());
            binding.tvSpotlightPrice.setText(spotlightProduct.getPrice());
            ProductImageLoader.load(binding.imgSpotlight, spotlightProduct.getImageUrl());
        }
    }

    private void rebuildCategoryChipsIfNeeded() {
        Set<String> uniqueCategories = new LinkedHashSet<>();
        uniqueCategories.add(getString(R.string.category_all));
        for (ProductWithRating item : allProducts) {
            String cat = item.product.getCategory();
            if (cat != null && !cat.trim().isEmpty()) {
                uniqueCategories.add(cat.trim());
            }
        }
        List<String> updated = new ArrayList<>(uniqueCategories);
        if (updated.equals(categories)) {
            return;
        }
        categories.clear();
        categories.addAll(updated);

        String allLabel = getString(R.string.category_all);
        if (selectedCategory.isEmpty() || !categories.contains(selectedCategory)) {
            selectedCategory = allLabel;
        }

        binding.chipGroupCategories.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String category : categories) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_filter_chip, binding.chipGroupCategories, false);
            chip.setId(View.generateViewId());
            chip.setText(category);
            chip.setChecked(category.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                applyFilterAndSort();
                BehaviorTracker tracker = BehaviorTracker.getInstance();
                if (tracker != null) {
                    tracker.trackCategoryFilter(category);
                }
            });
            binding.chipGroupCategories.addView(chip);
        }
    }

    private void setSortOption(SortOption option) {
        if (currentSort == option) {
            return;
        }
        currentSort = option;
        updateSortTabStyles();
        applyFilterAndSort();
        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.trackSort(option.name());
        }
    }

    private void updateSortTabStyles() {
        if (binding == null) {
            return;
        }
        UiUtils.styleUnderlineTab(binding.tvSortDefault, binding.indSortDefault, currentSort == SortOption.DEFAULT);
        UiUtils.styleUnderlineTab(binding.tvSortRating, binding.indSortRating, currentSort == SortOption.RATING_DESC);
        UiUtils.styleUnderlineTab(binding.tvSortPriceAsc, binding.indSortPriceAsc, currentSort == SortOption.PRICE_ASC);
        UiUtils.styleUnderlineTab(binding.tvSortPriceDesc, binding.indSortPriceDesc,
                currentSort == SortOption.PRICE_DESC);
    }

    private void resetFilters() {
        searchQuery = "";
        selectedCategory = getString(R.string.category_all);
        currentSort = SortOption.DEFAULT;
        binding.etSearch.setText("");
        updateSortTabStyles();
        if (binding.chipGroupCategories.getChildCount() > 0) {
            View first = binding.chipGroupCategories.getChildAt(0);
            if (first instanceof Chip) {
                ((Chip) first).setChecked(true);
            }
        }
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        if (binding == null) {
            return;
        }
        String allLabel = getString(R.string.category_all);
        boolean isDefaultBrowse = searchQuery.isEmpty()
                && (selectedCategory.isEmpty() || selectedCategory.equals(allLabel))
                && currentSort == SortOption.DEFAULT;
        int bannerVisibility = isDefaultBrowse && spotlightProduct != null ? View.VISIBLE : View.GONE;
        binding.cardSpotlight.setVisibility(bannerVisibility);
        binding.sectionHomeVouchers.setVisibility(isDefaultBrowse ? View.VISIBLE : View.GONE);

        List<ProductWithRating> filtered = new ArrayList<>();
        for (ProductWithRating item : allProducts) {
            Product p = item.product;
            if (!selectedCategory.isEmpty() && !selectedCategory.equals(allLabel)) {
                if (p.getCategory() == null || !selectedCategory.equalsIgnoreCase(p.getCategory().trim())) {
                    continue;
                }
            }
            if (!searchQuery.isEmpty()) {
                String haystack = (safe(p.getName()) + " " + safe(p.getBrand()) + " "
                        + safe(p.getCategory()) + " " + safe(p.getShortInfo()))
                        .toLowerCase(Locale.getDefault());
                if (!haystack.contains(searchQuery)) {
                    continue;
                }
            }
            filtered.add(item);
        }

        if (currentSort == SortOption.DEFAULT && !recommendationScores.isEmpty()) {
            filtered.sort((a, b) -> Double.compare(
                    recommendationScores.getOrDefault(b.product.getId(), 0.0),
                    recommendationScores.getOrDefault(a.product.getId(), 0.0)));
        } else if (currentSort == SortOption.PRICE_ASC) {
            filtered.sort(Comparator.comparingLong(i -> FormatUtils.parseVnd(i.product.getPrice())));
        } else if (currentSort == SortOption.PRICE_DESC) {
            filtered.sort((a, b) -> Long.compare(
                    FormatUtils.parseVnd(b.product.getPrice()),
                    FormatUtils.parseVnd(a.product.getPrice())));
        } else if (currentSort == SortOption.RATING_DESC) {
            filtered.sort((a, b) -> {
                int cmp = Double.compare(b.avgRating, a.avgRating);
                return cmp != 0 ? cmp : Integer.compare(b.reviewCount, a.reviewCount);
            });
        }

        lastFilteredCount = filtered.size();
        adapter.submitList(filtered);
        binding.llEmptyProducts.setVisibility(
                filtered.isEmpty() && !allProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void scheduleSearchTracking(String query) {
        if (pendingSearchTrack != null) {
            searchTrackHandler.removeCallbacks(pendingSearchTrack);
            pendingSearchTrack = null;
        }
        if (query.length() < 2) {
            return;
        }
        pendingSearchTrack = () -> {
            BehaviorTracker tracker = BehaviorTracker.getInstance();
            if (tracker != null) {
                tracker.trackSearch(query, lastFilteredCount);
            }
            pendingSearchTrack = null;
        };
        searchTrackHandler.postDelayed(pendingSearchTrack, 1_000L);
    }

    private void flushPendingSearchTrack() {
        if (pendingSearchTrack != null) {
            searchTrackHandler.removeCallbacks(pendingSearchTrack);
            pendingSearchTrack.run();
        }
    }

    private void openProductDetail(Product selected) {
        if (selected == null || !isAdded()) {
            return;
        }
        flushPendingSearchTrack();
        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, selected.getId());
        startActivity(intent);
    }

    private void refreshGreeting() {
        if (binding != null && userSession != null) {
            String name = userSession.getDisplayName();
            binding.tvWelcomeUser.setText("Xin chào, " + name);
            AvatarImageLoader.load(
                    binding.imgHeaderAvatar,
                    binding.tvHeaderAvatarInitials,
                    userSession.getAvatarUrl(),
                    name);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
