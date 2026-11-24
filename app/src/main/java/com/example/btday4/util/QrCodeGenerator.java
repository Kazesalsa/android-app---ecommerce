package com.example.btday4.util;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates crisp offline VietQR Bitmaps from PayOS EMVCo QR payloads and maps Vietnam NAPAS BINs.
 */
public final class QrCodeGenerator {

    private static final int QR_FOREGROUND = 0xFF161823;
    private static final int QR_BACKGROUND = 0xFFFFFFFF;
    private static final Map<String, String> BANK_NAMES_BY_BIN = new HashMap<>();

    static {
        BANK_NAMES_BY_BIN.put("970422", "MB Bank");
        BANK_NAMES_BY_BIN.put("970436", "Vietcombank");
        BANK_NAMES_BY_BIN.put("970415", "VietinBank");
        BANK_NAMES_BY_BIN.put("970418", "BIDV");
        BANK_NAMES_BY_BIN.put("970407", "Techcombank");
        BANK_NAMES_BY_BIN.put("970416", "ACB");
        BANK_NAMES_BY_BIN.put("970432", "VPBank");
        BANK_NAMES_BY_BIN.put("970423", "TPBank");
        BANK_NAMES_BY_BIN.put("970403", "Sacombank");
        BANK_NAMES_BY_BIN.put("970437", "HDBank");
        BANK_NAMES_BY_BIN.put("970441", "VIB");
        BANK_NAMES_BY_BIN.put("970448", "OCB");
        BANK_NAMES_BY_BIN.put("970426", "MSB");
        BANK_NAMES_BY_BIN.put("970443", "SHB");
        BANK_NAMES_BY_BIN.put("970405", "Agribank");
        BANK_NAMES_BY_BIN.put("970454", "BVBank (VietCapital)");
        BANK_NAMES_BY_BIN.put("970431", "Eximbank");
        BANK_NAMES_BY_BIN.put("970429", "SCB");
        BANK_NAMES_BY_BIN.put("970449", "LPBank");
        BANK_NAMES_BY_BIN.put("970438", "BaoViet Bank");
        BANK_NAMES_BY_BIN.put("970414", "OceanBank");
        BANK_NAMES_BY_BIN.put("970428", "Nam A Bank");
        BANK_NAMES_BY_BIN.put("970440", "SeABank");
        BANK_NAMES_BY_BIN.put("970433", "VietBank");
        BANK_NAMES_BY_BIN.put("970406", "DongA Bank");
        BANK_NAMES_BY_BIN.put("970452", "KienlongBank");
    }

    private QrCodeGenerator() {
    }

    /**
     * Renders a VietQR EMVCo string into a crisp square Bitmap without requiring external image APIs.
     */
    public static Bitmap encode(String content, int sizePx) {
        if (content == null || content.trim().isEmpty() || sizePx <= 0) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(
                    content.trim(),
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    hints
            );
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int rowOffset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[rowOffset + x] = matrix.get(x, y) ? QR_FOREGROUND : QR_BACKGROUND;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    /**
     * Resolves a Vietnam NAPAS bank BIN code returned by PayOS into a readable bank name.
     */
    public static String resolveBankName(String bin) {
        if (bin == null || bin.trim().isEmpty()) {
            return "Ngân hàng VietQR";
        }
        String trimmed = bin.trim();
        String name = BANK_NAMES_BY_BIN.get(trimmed);
        return name != null ? name : "Ngân hàng (BIN " + trimmed + ")";
    }
}
