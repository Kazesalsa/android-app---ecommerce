package com.example.btday4.ui.products;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.R;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.databinding.ItemReviewBinding;
import com.example.btday4.util.FormatUtils;
import java.util.Objects;

/**
 * RecyclerView ListAdapter displaying product reviews with initials avatar and owner edit/delete actions.
 */
public class ReviewAdapter extends ListAdapter<ReviewEntity, ReviewAdapter.ViewHolder> {

    public interface ReviewOwnerActionListener {
        void onEditReview(ReviewEntity review);

        void onDeleteReview(ReviewEntity review);
    }

    private final String currentUsername;
    private final ReviewOwnerActionListener listener;

    public ReviewAdapter(String currentUsername, ReviewOwnerActionListener listener) {
        super(DIFF_CALLBACK);
        this.currentUsername = currentUsername == null ? "" : currentUsername;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ReviewEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReviewEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ReviewEntity oldItem,
                        @NonNull ReviewEntity newItem
                ) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ReviewEntity oldItem,
                        @NonNull ReviewEntity newItem
                ) {
                    return oldItem.getRating() == newItem.getRating()
                            && oldItem.getCreatedAt() == newItem.getCreatedAt()
                            && Objects.equals(oldItem.getAuthorName(), newItem.getAuthorName())
                            && Objects.equals(oldItem.getComment(), newItem.getComment());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemReviewBinding binding;

        ViewHolder(ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReviewEntity review) {
            Context context = binding.getRoot().getContext();
            binding.tvReviewAvatar.setText(FormatUtils.extractInitials(review.getAuthorName()));
            binding.tvReviewAuthor.setText(review.getAuthorName());
            binding.tvReviewDate.setText(FormatUtils.formatDate(review.getCreatedAt()));

            boolean isOwner = !currentUsername.isEmpty() && currentUsername.equals(review.getUsername());
            binding.tvReviewOwnBadge.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            binding.llOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);

            String comment = review.getComment();
            if (comment != null && !comment.trim().isEmpty()) {
                binding.tvReviewComment.setVisibility(View.VISIBLE);
                binding.tvReviewComment.setText(comment.trim());
            } else {
                binding.tvReviewComment.setVisibility(View.GONE);
            }

            renderStars(context, binding.llReviewStars, review.getRating(), 13);

            binding.btnEditOwnReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditReview(review);
                }
            });
            binding.btnDeleteOwnReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteReview(review);
                }
            });
        }
    }

    public static void renderStars(Context context, LinearLayout container, int filledStars, int sizeDp) {
        container.removeAllViews();
        int sizePx = Math.round(sizeDp * context.getResources().getDisplayMetrics().density);
        int marginPx = Math.round(1.5f * context.getResources().getDisplayMetrics().density);
        int amber = ContextCompat.getColor(context, R.color.star_amber);
        int empty = ContextCompat.getColor(context, R.color.star_empty);

        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            if (i > 1) {
                lp.setMarginStart(marginPx);
            }
            star.setLayoutParams(lp);
            star.setImageResource(i <= filledStars ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            star.setColorFilter(i <= filledStars ? amber : empty);
            container.addView(star);
        }
    }
}
