package com.example.btday4.util;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

/**
 * Loads product images from URLs via Glide with a warm alabaster studio placeholder.
 */
public final class ProductImageLoader {

    private static final ColorDrawable PLACEHOLDER = new ColorDrawable(Color.parseColor("#F4F4F6"));
    private static final ColorDrawable ERROR_FALLBACK = new ColorDrawable(Color.parseColor("#E6E7EC"));

    private ProductImageLoader() {
    }

    public static void load(ImageView target, String imageUrl) {
        if (target == null) {
            return;
        }
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            target.setImageDrawable(ERROR_FALLBACK);
            return;
        }
        Glide.with(target)
                .load(imageUrl)
                .placeholder(PLACEHOLDER)
                .error(ERROR_FALLBACK)
                .fitCenter()
                .into(target);
    }
}
