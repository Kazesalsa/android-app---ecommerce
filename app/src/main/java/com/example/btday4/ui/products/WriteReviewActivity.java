package com.example.btday4.ui.products;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.btday4.R;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityWriteReviewBinding;
import com.example.btday4.util.Constants;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.UiUtils;
import com.google.android.material.chip.Chip;

/**
 * Full-screen native review composer replacing the previous BottomSheet popup.
 * Features large 48dp star targets, quick-tag suggestions, and keyboard-safe scroll form.
 */
public class WriteReviewActivity extends AppCompatActivity {

    private static final String[] RATING_CAPTIONS = {
            "",
            "Không hài lòng",
            "Chưa hài lòng",
            "Bình thường",
            "Hài lòng",
            "Rất hài lòng"
    };

    private ActivityWriteReviewBinding binding;
    private ImageButton[] starButtons;
    private int selectedRating = 5;
    private String productId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.writeReviewRoot);

        productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        String productName = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_NAME);
        String productImageUrl = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_IMAGE_URL);
        boolean isEdit = getIntent().getBooleanExtra(Constants.EXTRA_IS_EDIT_REVIEW, false);
        int initialRating = getIntent().getIntExtra(Constants.EXTRA_INITIAL_RATING, 5);
        String initialComment = getIntent().getStringExtra(Constants.EXTRA_INITIAL_COMMENT);

        if (productId == null || productId.trim().isEmpty()) {
            finish();
            return;
        }

        binding.toolbarWriteReview.setNavigationOnClickListener(v -> finish());
        binding.toolbarWriteReview.setTitle(isEdit ? R.string.btn_edit_review : R.string.btn_write_review);
        binding.btnSubmitReview.setText(isEdit ? "Cập nhật đánh giá" : "Gửi đánh giá");

        binding.tvReviewProductName.setText(productName == null ? "" : productName);
        ProductImageLoader.load(binding.imgReviewProduct, productImageUrl);

        if (initialComment != null && !initialComment.isEmpty()) {
            binding.etReviewComment.setText(initialComment);
            binding.etReviewComment.setSelection(initialComment.length());
        }

        UiUtils.clearErrorOnType(binding.etReviewComment, binding.tilReviewComment);

        starButtons = new ImageButton[]{
                binding.star1, binding.star2, binding.star3, binding.star4, binding.star5
        };
        for (int i = 0; i < starButtons.length; i++) {
            final int ratingValue = i + 1;
            starButtons[i].setOnClickListener(v -> updateStarSelection(ratingValue));
        }
        updateStarSelection(Math.max(1, Math.min(5, initialRating)));

        setupQuickTags();
        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void setupQuickTags() {
        int count = binding.chipGroupQuickTags.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.chipGroupQuickTags.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> appendQuickTag(chip.getText().toString()));
            }
        }
    }

    private void appendQuickTag(String tagText) {
        String current = binding.etReviewComment.getText() == null
                ? ""
                : binding.etReviewComment.getText().toString().trim();
        if (current.contains(tagText)) {
            return;
        }
        String next = current.isEmpty() ? tagText : current + ", " + tagText.toLowerCase();
        binding.etReviewComment.setText(next);
        binding.etReviewComment.setSelection(next.length());
        binding.tilReviewComment.setError(null);
    }

    private void updateStarSelection(int rating) {
        selectedRating = rating;
        int amber = ContextCompat.getColor(this, R.color.star_amber);
        int empty = ContextCompat.getColor(this, R.color.star_empty);
        for (int i = 0; i < starButtons.length; i++) {
            boolean filled = (i + 1) <= rating;
            starButtons[i].setImageResource(filled ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            starButtons[i].setColorFilter(filled ? amber : empty);
        }
        binding.tvRatingCaption.setText(RATING_CAPTIONS[rating]);
    }

    private void submitReview() {
        String comment = binding.etReviewComment.getText() == null
                ? ""
                : binding.etReviewComment.getText().toString().trim();
        if (comment.length() < 3) {
            binding.tilReviewComment.setError("Vui lòng nhập nhận xét tối thiểu 3 ký tự");
            return;
        }
        binding.btnSubmitReview.setEnabled(false);
        UserSession session = new UserSession(this);
        ReviewRepository.saveReview(
                this,
                productId,
                session.getUsername(),
                session.getDisplayName(),
                selectedRating,
                comment,
                () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    setResult(RESULT_OK);
                    finish();
                }
        );
    }
}
