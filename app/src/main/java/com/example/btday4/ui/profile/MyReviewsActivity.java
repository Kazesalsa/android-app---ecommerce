package com.example.btday4.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityMyReviewsBinding;
import com.example.btday4.databinding.ItemMyReviewBinding;
import com.example.btday4.model.UserReviewWithProduct;
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.ui.products.ProductDetailActivity;
import com.example.btday4.ui.products.ReviewAdapter;
import com.example.btday4.util.Constants;
import com.example.btday4.util.FormatUtils;
import com.example.btday4.util.ProductImageLoader;
import com.example.btday4.util.UiUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen user review history screen with product navigation and store CTA fallback.
 */
public class MyReviewsActivity extends AppCompatActivity {

    private ActivityMyReviewsBinding binding;
    private UserSession userSession;
    private MyReviewsAdapter adapter;
    private boolean hasLoadedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(binding.myReviewsRoot, binding.toolbarMyReviews, null);

        userSession = new UserSession(this);
        binding.toolbarMyReviews.setNavigationOnClickListener(v -> finish());

        binding.btnMyReviewsExplore.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(Constants.EXTRA_OPEN_PRODUCTS, true);
            startActivity(intent);
            finish();
        });

        adapter = new MyReviewsAdapter(productId -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, productId);
            startActivity(intent);
        });
        binding.rvMyReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyReviews.setAdapter(adapter);

        loadReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
    }

    private void loadReviews() {
        if (!hasLoadedOnce) {
            binding.progressMyReviews.setVisibility(View.VISIBLE);
            binding.llMyReviewsEmpty.setVisibility(View.GONE);
        }
        ReviewRepository.getUserReviews(this, userSession.getUsername(), reviews -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            hasLoadedOnce = true;
            binding.progressMyReviews.setVisibility(View.GONE);
            adapter.submitList(reviews);
            binding.llMyReviewsEmpty.setVisibility(
                    reviews == null || reviews.isEmpty() ? View.VISIBLE : View.GONE
            );
        });
    }

    private static class MyReviewsAdapter extends RecyclerView.Adapter<MyReviewsAdapter.ViewHolder> {

        interface OnOpenProductListener {
            void onOpenProduct(String productId);
        }

        private final List<UserReviewWithProduct> items = new ArrayList<>();
        private final OnOpenProductListener listener;

        MyReviewsAdapter(OnOpenProductListener listener) {
            this.listener = listener;
        }

        void submitList(List<UserReviewWithProduct> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMyReviewBinding itemBinding = ItemMyReviewBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserReviewWithProduct row = items.get(position);
            ProductImageLoader.load(holder.binding.imgMyReviewProduct, row.productImageUrl);
            holder.binding.tvMyReviewProductName.setText(row.productName);
            holder.binding.tvMyReviewDate.setText(FormatUtils.formatDate(row.review.getCreatedAt()));
            holder.binding.tvMyReviewComment.setText(row.review.getComment());
            ReviewAdapter.renderStars(
                    holder.itemView.getContext(),
                    holder.binding.llMyReviewStars,
                    row.review.getRating(),
                    13
            );
            holder.binding.getRoot().setOnClickListener(v -> listener.onOpenProduct(row.review.getProductId()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemMyReviewBinding binding;

            ViewHolder(ItemMyReviewBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
