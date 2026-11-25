package com.example.btday4.ui.products;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.local.Product;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.data.repository.VoucherRepository.VoucherItem;
import com.example.btday4.databinding.ActivityProductDetailBinding;
import com.example.btday4.databinding.ItemRatingBarBinding;
import com.example.btday4.databinding.ItemSpecPillBinding;
import com.example.btday4.databinding.ItemSpecRowBinding;
import com.example.btday4.model.AppNotification;
import com.example.btday4.model.RatingSummary;
import com.example.btday4.model.Voucher;
import com.example.btday4.tracking.BehaviorTracker;
import com.example.btday4.ui.checkout.CheckoutActivity;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.voucher.VoucherWalletActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.InAppNotifier;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Edge-to-edge flagship product detail screen with key-spec highlight pills, zebra specifications,
 * voucher wallet cue, live rating distribution, and anchored bottom CTA bar.
 */
public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private UserSession userSession;
    private ReviewAdapter reviewAdapter;

    private Product product;
    private int quantity = 1;
    private final List<ReviewEntity> allReviews = new ArrayList<>();
    private final List<VoucherItem> storeVouchers = new ArrayList<>();
    private ListenerRegistration voucherCueListener;
    private ReviewEntity currentUserReview;
    private int selectedStarFilter = 0;
    private boolean openReviewComposerOnLoad = false;
    private long productViewStartElapsedMs = 0L;

    private final ActivityResultLauncher<Intent> writeReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadReviews();
                    InAppNotifier.show(
                            binding.getRoot(),
                            AppNotification.TYPE_ACCOUNT,
                            "Đánh giá sản phẩm",
                            "Đã lưu đánh giá của bạn"
                    );
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(binding.detailRoot, binding.flHeroTopBar, binding.llDetailBottomBar);

        userSession = new UserSession(this);
        openReviewComposerOnLoad = getIntent().getBooleanExtra(Constants.EXTRA_OPEN_REVIEW, false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnToolbarCart.setOnClickListener(v -> openCartAndFinish());
        binding.rowDetailVoucherCue.setOnClickListener(v ->
                startActivity(new Intent(this, VoucherWalletActivity.class))
        );
        binding.btnAddToCart.setEnabled(false);
        binding.btnBuyNow.setEnabled(false);

        reviewAdapter = new ReviewAdapter(
                userSession.getUsername(),
                new ReviewAdapter.ReviewOwnerActionListener() {
                    @Override
                    public void onEditReview(ReviewEntity review) {
                        openWriteReviewScreen(review);
                    }

                    @Override
                    public void onDeleteReview(ReviewEntity review) {
                        deleteReviewWithUndo(review);
                    }
                }
        );
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(reviewAdapter);

        setupReviewFilterChips();

        binding.rowRatingJump.setOnClickListener(v ->
                binding.scrollDetail.post(() ->
                        binding.scrollDetail.smoothScrollTo(0, binding.sectionReviews.getTop())
                )
        );

        binding.btnWriteReview.setOnClickListener(v -> openWriteReviewScreen(currentUserReview));

        binding.btnQtyMinus.setOnClickListener(v -> {
            if (product != null && quantity > 1) {
                quantity--;
                updateQuantityUi();
            }
        });

        binding.btnQtyPlus.setOnClickListener(v -> {
            if (product != null && quantity < CartRepository.maxPurchasable(product)) {
                quantity++;
                updateQuantityUi();
            }
        });

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnBuyNow.setOnClickListener(v -> launchQuickCheckout());

        String productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        ProductRepository.findById(this, productId, loaded -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (loaded == null) {
                finish();
                return;
            }
            product = loaded;
            bindProduct(loaded);
            loadReviews();
        });

        refreshCartBadge();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (voucherCueListener != null) {
            voucherCueListener.remove();
        }
        voucherCueListener = VoucherRepository.listenVouchersForUser(
                userSession.getUsername(),
                items -> {
                    if (isFinishing() || isDestroyed() || binding == null) {
                        return;
                    }
                    storeVouchers.clear();
                    if (items != null) {
                        storeVouchers.addAll(items);
                    }
                    bindFeaturedVoucherCue();
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        productViewStartElapsedMs = SystemClock.elapsedRealtime();
        refreshCartBadge();
        if (product != null) {
            ProductRepository.findById(this, product.getId(), latest -> {
                if (latest != null && !isFinishing() && !isDestroyed()) {
                    product = latest;
                    bindStockState(latest);
                    bindFeaturedVoucherCue();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (product != null && productViewStartElapsedMs > 0L) {
            long dwellMs = Math.max(0L, SystemClock.elapsedRealtime() - productViewStartElapsedMs);
            BehaviorTracker tracker = BehaviorTracker.getInstance();
            if (tracker != null) {
                tracker.trackProductView(product, dwellMs);
            }
            productViewStartElapsedMs = 0L;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (voucherCueListener != null) {
            voucherCueListener.remove();
            voucherCueListener = null;
        }
    }

    private void bindFeaturedVoucherCue() {
        if (binding == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long unitPrice = product == null ? 0L : FormatUtils.parseVnd(product.getPrice());
        long baseShip = FormatUtils.computeShippingFee(unitPrice);

        VoucherItem bestMatching = null;
        long bestDiscount = 0L;
        VoucherItem firstAvailable = null;

        for (VoucherItem row : storeVouchers) {
            Voucher v = row.voucher;
            if (row.used || !v.isActive() || v.isExpired(now) || !v.hasRemainingUses()) {
                continue;
            }
            if (firstAvailable == null) {
                firstAvailable = row;
            }
            if (unitPrice > 0L) {
                long discount = v.computeDiscount(unitPrice, baseShip);
                if (discount > bestDiscount) {
                    bestDiscount = discount;
                    bestMatching = row;
                }
            }
        }

        VoucherItem featured = bestMatching != null ? bestMatching : firstAvailable;
        if (featured == null) {
            binding.rowDetailVoucherCue.setVisibility(View.GONE);
            return;
        }

        Voucher v = featured.voucher;
        binding.rowDetailVoucherCue.setVisibility(View.VISIBLE);
        binding.tvDetailVoucherCueText.setText(v.getCode() + " · " + v.getTitle());
        binding.tvDetailVoucherCueAction.setText(featured.claimed ? "Xem mã ›" : "Nhận mã ›");
    }

    private void bindProduct(Product item) {
        ProductImageLoader.load(binding.imgDetailProduct, item.getImageUrl());
        bindFeaturedVoucherCue();

        binding.tvDetailBrand.setText(
                ValidationUtils.isNotBlank(item.getBrand())
                        ? item.getBrand().toUpperCase(Locale.getDefault())
                        : ""
        );
        binding.tvDetailCategory.setText(ValidationUtils.isNotBlank(item.getCategory()) ? item.getCategory() : "");
        binding.tvDetailName.setText(item.getName());
        binding.tvDetailPrice.setText(item.getPrice());

        int discountPercent = FormatUtils.computeDiscountPercent(item.getPrice(), item.getOriginalPrice());
        if (discountPercent > 0) {
            binding.tvDetailOriginalPrice.setVisibility(View.VISIBLE);
            binding.tvDetailOriginalPrice.setText(item.getOriginalPrice());
            binding.tvDetailOriginalPrice.setPaintFlags(
                    binding.tvDetailOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
            binding.tvDetailDiscount.setVisibility(View.VISIBLE);
            binding.tvDetailDiscount.setText("-" + discountPercent + "%");
        } else {
            binding.tvDetailOriginalPrice.setVisibility(View.GONE);
            binding.tvDetailDiscount.setVisibility(View.GONE);
        }

        String warrantyText = ValidationUtils.isNotBlank(item.getWarranty())
                ? item.getWarranty() + ", đổi trả dễ dàng trong 15 ngày"
                : "Bảo hành chính hãng, đổi trả dễ dàng trong 15 ngày";
        binding.tvWarrantySummary.setText(warrantyText);

        String fullDescription = ValidationUtils.isNotBlank(item.getDescription())
                ? item.getDescription()
                : item.getShortInfo();
        binding.tvDetailDescription.setText(fullDescription);

        bindSpecifications(item.parseSpecs());
        bindStockState(item);
    }

    private void bindStockState(Product item) {
        int maxPurchasable = CartRepository.maxPurchasable(item);
        boolean inStock = maxPurchasable > 0;
        binding.tvStockStatus.setText(inStock ? "Còn " + item.getStock() : getString(R.string.out_of_stock));
        binding.tvStockStatus.setTextColor(ContextCompat.getColor(
                this,
                inStock ? R.color.teal_trust : R.color.danger_red
        ));
        quantity = inStock ? Math.max(1, Math.min(quantity, maxPurchasable)) : 1;
        binding.btnAddToCart.setEnabled(inStock);
        binding.btnBuyNow.setEnabled(inStock);
        binding.btnBuyNow.setText(inStock ? R.string.btn_buy_now : R.string.out_of_stock);
        updateQuantityUi();
    }

    private void bindSpecifications(Map<String, String> specs) {
        binding.llSpecsContainer.removeAllViews();
        binding.llSpecHighlights.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int index = 0;
        int highlightCount = 0;
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            ItemSpecRowBinding row = ItemSpecRowBinding.inflate(inflater, binding.llSpecsContainer, false);
            row.tvSpecKey.setText(entry.getKey());
            row.tvSpecValue.setText(entry.getValue());
            if (index % 2 == 0) {
                row.rowSpecContainer.setBackgroundResource(R.drawable.bg_zebra_row);
            }
            binding.llSpecsContainer.addView(row.getRoot());

            if (highlightCount < 4 && !"Bảo hành".equalsIgnoreCase(entry.getKey())) {
                ItemSpecPillBinding pill = ItemSpecPillBinding.inflate(inflater, binding.llSpecHighlights, false);
                pill.tvSpecPillLabel.setText(entry.getKey());
                pill.tvSpecPillValue.setText(entry.getValue());
                binding.llSpecHighlights.addView(pill.getRoot());
                highlightCount++;
            }
            index++;
        }
        binding.hsvSpecHighlights.setVisibility(highlightCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void setupReviewFilterChips() {
        String[] labels = {"Tất cả", "5 ★", "4 ★", "3 ★", "2 ★", "1 ★"};
        int[] values = {0, 5, 4, 3, 2, 1};
        LayoutInflater inflater = LayoutInflater.from(this);
        binding.chipGroupReviewFilter.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            final int filterValue = values[i];
            Chip chip = (Chip) inflater.inflate(R.layout.item_filter_chip, binding.chipGroupReviewFilter, false);
            chip.setId(View.generateViewId());
            chip.setText(labels[i]);
            chip.setChecked(filterValue == selectedStarFilter);
            chip.setOnClickListener(v -> {
                selectedStarFilter = filterValue;
                applyReviewFilter();
            });
            binding.chipGroupReviewFilter.addView(chip);
        }
    }

    private void loadReviews() {
        if (product == null) {
            return;
        }
        ReviewRepository.getReviewsForProduct(this, product.getId(), reviews -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            allReviews.clear();
            allReviews.addAll(reviews);

            currentUserReview = null;
            String username = userSession.getUsername();
            for (ReviewEntity r : allReviews) {
                if (!username.isEmpty() && username.equals(r.getUsername())) {
                    currentUserReview = r;
                    break;
                }
            }

            binding.btnWriteReview.setText(
                    currentUserReview != null ? R.string.btn_edit_review : R.string.btn_write_review
            );

            RatingSummary summary = RatingSummary.fromReviews(allReviews);
            bindRatingSummary(summary);
            applyReviewFilter();

            if (openReviewComposerOnLoad) {
                openReviewComposerOnLoad = false;
                openWriteReviewScreen(currentUserReview);
            }
        });
    }

    private void bindRatingSummary(RatingSummary summary) {
        if (summary.getTotalCount() > 0) {
            String avgFormatted = FormatUtils.formatRating(summary.getAverageRating());
            binding.tvHeaderRating.setText(avgFormatted);
            binding.tvHeaderReviewCount.setText("(" + summary.getTotalCount() + " đánh giá)");
            binding.tvRatingAverage.setText(avgFormatted);
            binding.tvRatingTotalCount.setText(summary.getTotalCount() + " đánh giá");
        } else {
            binding.tvHeaderRating.setText("Chưa có");
            binding.tvHeaderReviewCount.setText("(0 đánh giá)");
            binding.tvRatingAverage.setText("0.0");
            binding.tvRatingTotalCount.setText("0 đánh giá");
        }

        binding.tvReviewsTitle.setText("Đánh giá (" + summary.getTotalCount() + ")");
        ReviewAdapter.renderStars(
                this,
                binding.llAverageStars,
                (int) Math.round(summary.getAverageRating()),
                14
        );

        binding.llRatingBars.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int star = 5; star >= 1; star--) {
            ItemRatingBarBinding bar = ItemRatingBarBinding.inflate(inflater, binding.llRatingBars, false);
            bar.tvBarStar.setText(star + "★");
            bar.progressStarBar.setProgress(summary.getPercentForStar(star));
            bar.tvBarCount.setText(String.valueOf(summary.getCountForStar(star)));
            binding.llRatingBars.addView(bar.getRoot());
        }
    }

    private void applyReviewFilter() {
        List<ReviewEntity> filtered = new ArrayList<>();
        for (ReviewEntity review : allReviews) {
            if (selectedStarFilter == 0 || review.getRating() == selectedStarFilter) {
                filtered.add(review);
            }
        }
        reviewAdapter.submitList(filtered);
        binding.tvEmptyReviews.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openWriteReviewScreen(ReviewEntity existing) {
        if (product == null) {
            return;
        }
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(Constants.EXTRA_PRODUCT_NAME, product.getName());
        intent.putExtra(Constants.EXTRA_PRODUCT_IMAGE_URL, product.getImageUrl());
        intent.putExtra(Constants.EXTRA_INITIAL_RATING, existing != null ? existing.getRating() : 5);
        intent.putExtra(Constants.EXTRA_INITIAL_COMMENT, existing != null ? existing.getComment() : "");
        intent.putExtra(Constants.EXTRA_IS_EDIT_REVIEW, existing != null);
        writeReviewLauncher.launch(intent);
    }

    private void deleteReviewWithUndo(ReviewEntity review) {
        final String savedProductId = review.getProductId();
        final String savedUsername = review.getUsername();
        final String savedAuthor = review.getAuthorName();
        final int savedRating = review.getRating();
        final String savedComment = review.getComment();

        ReviewRepository.deleteReview(this, review.getId(), () -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            loadReviews();
            InAppNotifier.show(
                    binding.getRoot(),
                    AppNotification.TYPE_ACCOUNT,
                    "Đã xóa đánh giá",
                    "Đã xóa đánh giá của bạn",
                    savedProductId,
                    "Hoàn tác",
                    () -> ReviewRepository.saveReview(
                            this,
                            savedProductId,
                            savedUsername,
                            savedAuthor,
                            savedRating,
                            savedComment,
                            this::loadReviews
                    )
            );
        });
    }

    private void updateQuantityUi() {
        if (product == null) {
            return;
        }
        int max = CartRepository.maxPurchasable(product);
        boolean canDecrease = max > 0 && quantity > 1;
        boolean canIncrease = max > 0 && quantity < max;
        binding.btnQtyMinus.setEnabled(canDecrease);
        binding.btnQtyMinus.setAlpha(canDecrease ? 1.0f : 0.35f);
        binding.btnQtyPlus.setEnabled(canIncrease);
        binding.btnQtyPlus.setAlpha(canIncrease ? 1.0f : 0.35f);

        binding.tvQuantity.setText(String.valueOf(quantity));
        binding.tvSelectedSubtotal.setText(FormatUtils.computeLineTotal(product.getPrice(), quantity));
    }

    private void addToCart() {
        if (product == null) {
            return;
        }
        binding.btnAddToCart.setEnabled(false);
        CartRepository.addItem(this, product.getId(), quantity, added -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.btnAddToCart.setEnabled(true);
            refreshCartBadge();
            if (added > 0) {
                BehaviorTracker tracker = BehaviorTracker.getInstance();
                if (tracker != null) {
                    tracker.trackAction(
                            "ProductDetailActivity",
                            BehaviorTracker.EVENT_ADD_TO_CART,
                            product.getId(),
                            product.getName() + "|qty=" + added
                    );
                }
            }
            String title = added > 0 ? "Đã thêm vào giỏ hàng" : "Giới hạn tồn kho";
            String type = added > 0 ? AppNotification.TYPE_CART : AppNotification.TYPE_ALERT;
            String message = added > 0
                    ? "Đã thêm " + added + " × " + product.getName() + " vào giỏ"
                    : "Giỏ hàng đã có tối đa " + CartRepository.maxPurchasable(product) + " sản phẩm còn trong kho";
            InAppNotifier.show(
                    binding.getRoot(),
                    type,
                    title,
                    message,
                    product.getId(),
                    "Xem giỏ",
                    this::openCartAndFinish
            );
        });
    }

    private void launchQuickCheckout() {
        if (product == null) {
            return;
        }
        BehaviorTracker tracker = BehaviorTracker.getInstance();
        if (tracker != null) {
            tracker.trackAction(
                    "ProductDetailActivity",
                    BehaviorTracker.EVENT_BUY_NOW,
                    product.getId(),
                    product.getName() + "|qty=" + quantity
            );
        }
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(Constants.EXTRA_QUANTITY, quantity);
        startActivity(intent);
    }

    private void openCartAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Constants.EXTRA_OPEN_CART, true);
        startActivity(intent);
        finish();
    }

    private void refreshCartBadge() {
        CartRepository.getTotalQuantity(this, count -> {
            if (isFinishing() || isDestroyed() || binding == null) {
                return;
            }
            if (count > 0) {
                binding.tvToolbarCartBadge.setVisibility(View.VISIBLE);
                binding.tvToolbarCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.tvToolbarCartBadge.setVisibility(View.GONE);
            }
        });
    }
}
