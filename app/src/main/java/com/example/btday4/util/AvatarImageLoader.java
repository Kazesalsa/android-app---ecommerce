package com.example.btday4.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.bumptech.glide.Glide;
import com.example.btday4.R;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Loads circular user avatars supporting built-in vector presets ("preset:1".."preset:6"),
 * Base64 gallery photos ("data:image/..."), remote HTTPS URLs, and clean initials fallback.
 */
public final class AvatarImageLoader {

    public static final String[] PRESET_KEYS = {
            "preset:1",
            "preset:2",
            "preset:3",
            "preset:4",
            "preset:5",
            "preset:6"
    };

    private static final int[] PRESET_DRAWABLES = {
            R.drawable.ic_avatar_preset_1,
            R.drawable.ic_avatar_preset_2,
            R.drawable.ic_avatar_preset_3,
            R.drawable.ic_avatar_preset_4,
            R.drawable.ic_avatar_preset_5,
            R.drawable.ic_avatar_preset_6
    };

    private static final int TARGET_AVATAR_PX = 192;

    private AvatarImageLoader() {
    }

    @DrawableRes
    public static int resolvePresetDrawable(@Nullable String avatarValue) {
        if (avatarValue == null) {
            return 0;
        }
        String trimmed = avatarValue.trim();
        for (int i = 0; i < PRESET_KEYS.length; i++) {
            if (PRESET_KEYS[i].equalsIgnoreCase(trimmed)) {
                return PRESET_DRAWABLES[i];
            }
        }
        return 0;
    }

    public static void load(
            @Nullable ImageView avatarImage,
            @Nullable TextView initialsView,
            @Nullable String avatarValue,
            @Nullable String displayName
    ) {
        if (initialsView != null) {
            initialsView.setText(FormatUtils.extractInitials(displayName));
        }
        if (avatarImage == null) {
            if (initialsView != null) {
                initialsView.setVisibility(View.VISIBLE);
            }
            return;
        }
        Glide.with(avatarImage).clear(avatarImage);

        String clean = avatarValue == null ? "" : avatarValue.trim();
        if (clean.isEmpty()) {
            showInitials(avatarImage, initialsView);
            return;
        }

        int presetRes = resolvePresetDrawable(clean);
        if (presetRes != 0) {
            avatarImage.setImageResource(presetRes);
            showImage(avatarImage, initialsView);
            return;
        }

        if (clean.startsWith("data:image")) {
            Bitmap decoded = decodeDataUri(clean);
            if (decoded != null) {
                RoundedBitmapDrawable circular = RoundedBitmapDrawableFactory.create(
                        avatarImage.getResources(),
                        cropCenterSquare(decoded)
                );
                circular.setCircular(true);
                circular.setAntiAlias(true);
                avatarImage.setImageDrawable(circular);
                showImage(avatarImage, initialsView);
                return;
            }
            showInitials(avatarImage, initialsView);
            return;
        }

        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            showImage(avatarImage, initialsView);
            Glide.with(avatarImage)
                    .load(clean)
                    .circleCrop()
                    .error(R.drawable.ic_avatar_preset_1)
                    .into(avatarImage);
            return;
        }

        showInitials(avatarImage, initialsView);
    }

    /**
     * Reads a gallery image Uri, center-crops it to 192x192, and encodes as a compact JPEG Data URI
     * (~8-16 KB) suitable for direct persistence in Firestore & SharedPreferences.
     */
    @Nullable
    public static String encodeUriToDataUri(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream boundsStream = context.getContentResolver().openInputStream(imageUri)) {
                if (boundsStream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(boundsStream, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sample = 1;
            int minSide = Math.min(bounds.outWidth, bounds.outHeight);
            while (minSide / (sample * 2) >= TARGET_AVATAR_PX) {
                sample *= 2;
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sample;
            Bitmap decoded;
            try (InputStream imgStream = context.getContentResolver().openInputStream(imageUri)) {
                if (imgStream == null) {
                    return null;
                }
                decoded = BitmapFactory.decodeStream(imgStream, null, decodeOpts);
            }
            if (decoded == null) {
                return null;
            }

            Bitmap square = cropCenterSquare(decoded);
            Bitmap scaled = Bitmap.createScaledBitmap(square, TARGET_AVATAR_PX, TARGET_AVATAR_PX, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out);
            String base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Bitmap decodeDataUri(String dataUri) {
        try {
            int comma = dataUri.indexOf(',');
            if (comma < 0 || comma + 1 >= dataUri.length()) {
                return null;
            }
            byte[] bytes = Base64.decode(dataUri.substring(comma + 1), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap cropCenterSquare(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == h) {
            return src;
        }
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        return Bitmap.createBitmap(src, x, y, size, size);
    }

    private static void showImage(ImageView image, @Nullable TextView initials) {
        image.setVisibility(View.VISIBLE);
        if (initials != null) {
            initials.setVisibility(View.GONE);
        }
    }

    private static void showInitials(ImageView image, @Nullable TextView initials) {
        image.setVisibility(View.GONE);
        if (initials != null) {
            initials.setVisibility(View.VISIBLE);
        }
    }
}
