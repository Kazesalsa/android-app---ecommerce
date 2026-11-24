package com.example.btday4.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.FragmentProductsBinding;
import com.example.btday4.model.ProductWithRating;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.GridSpacingItemDecoration;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.UiUtils;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TikTok Shop / Shopee Mall inspired Store feed with pinned search + cart header,
 * flagship spotlight banner, 4-tab inline sort bar (zero popups), and 2-column product grid.
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

    private final List<ProductWithRating> allProducts = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private String selectedCategory = "";
    private String searchQuery = "";
    private SortOption currentSort = SortOption.DEFAULT;
    private Product spotlightProduct;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userSession = new UserSession(requireContext());
        refreshGreeting();

        adapter = new ProductAdapter(this::openProductDetail);
        int spacingPx = Math.round(10 * getResources().getDisplayMetrics().density);
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.addItemDecoration(new GridSpacingItemDecoration(2, spacingPx));
        binding.rvProducts.setAdapter(adapter);

        binding.btnHomeCart.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCartTab();
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearch.setText(""));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                binding.btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndSort();
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

        refreshCatalog();
        refreshHeaderCartBadge();

        productsListener = FirestoreHelper.listenProducts(new FirestoreHelper.FirestoreCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> remoteProducts) {
                if (!isAdded() || binding == null || remoteProducts == null || remoteProducts.isEmpty()) {
                    return;
                }
                ProductRepository.syncFromRemote(requireContext(), remoteProducts, synced -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    updateProductData(synced);
                });
            }

            @Override
            public void onError(Exception e) {
                // Keep local Room SQLite catalog intact when offline.
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshGreeting();
        refreshCatalog();
        refreshHeaderCartBadge();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshGreeting();
            refreshCatalog();
            refreshHeaderCartBadge();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
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

    private void updateProductData(List<ProductWithRating> products) {
        allProducts.clear();
        if (products != null) {
            allProducts.addAll(products);
        }
        bindSpotlightProduct();
        rebuildCategoryChipsIfNeeded();
        applyFilterAndSort();
    }

    private void bindSpotlightProduct() {
        spotlightProduct = null;
        for (ProductWithRating item : allProducts) {
            if ("iphone_16_pro_max".equals(item.product.getId())) {
                spotlightProduct = item.product;
                break;
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
    }

    private void updateSortTabStyles() {
        if (binding == null) {
            return;
        }
        UiUtils.styleUnderlineTab(binding.tvSortDefault, binding.indSortDefault, currentSort == SortOption.DEFAULT);
        UiUtils.styleUnderlineTab(binding.tvSortRating, binding.indSortRating, currentSort == SortOption.RATING_DESC);
        UiUtils.styleUnderlineTab(binding.tvSortPriceAsc, binding.indSortPriceAsc, currentSort == SortOption.PRICE_ASC);
        UiUtils.styleUnderlineTab(binding.tvSortPriceDesc, binding.indSortPriceDesc, currentSort == SortOption.PRICE_DESC);
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
        binding.rowTrustStrip.setVisibility(bannerVisibility);

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

        if (currentSort == SortOption.PRICE_ASC) {
            filtered.sort(Comparator.comparingLong(i -> FormatUtils.parseVnd(i.product.getPrice())));
        } else if (currentSort == SortOption.PRICE_DESC) {
            filtered.sort((a, b) -> Long.compare(
                    FormatUtils.parseVnd(b.product.getPrice()),
                    FormatUtils.parseVnd(a.product.getPrice())
            ));
        } else if (currentSort == SortOption.RATING_DESC) {
            filtered.sort((a, b) -> {
                int cmp = Double.compare(b.avgRating, a.avgRating);
                return cmp != 0 ? cmp : Integer.compare(b.reviewCount, a.reviewCount);
            });
        }

        adapter.submitList(filtered);
        binding.tvProductCount.setText(filtered.size() + " sản phẩm");
        binding.llEmptyProducts.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openProductDetail(Product selected) {
        if (selected == null || !isAdded()) {
            return;
        }
        Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, selected.getId());
        startActivity(intent);
    }

    private void refreshGreeting() {
        if (binding != null && userSession != null) {
            binding.tvWelcomeUser.setText("Xin chào, " + userSession.getDisplayName());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
