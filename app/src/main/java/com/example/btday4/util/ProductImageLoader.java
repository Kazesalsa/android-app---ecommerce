package com.example.btday4.util;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.btday4.R;

/**
 * Loads product images from URLs via Glide with a clean studio placeholder and fallback icon.
 */
public final class ProductImageLoader {

    private ProductImageLoader() {
    }

    public static void load(ImageView target, String imageUrl) {
        if (target == null) {
            return;
        }
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            target.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        Glide.with(target)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(target);
    }
}
