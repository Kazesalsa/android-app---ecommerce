package com.example.btday4.data.remote;

import com.example.btday4.BuildConfig;
import com.example.btday4.model.PayOsPaymentInfo;
import com.example.btday4.util.Constants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Lightweight PayOS v2 REST client built on standard HttpURLConnection + HMAC-SHA256 signing.
 * Follows the protocol defined in Payment_PayOS_Module without external HTTP SDK bloat.
 */
public final class PayOsClient {

    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_DESCRIPTION_LENGTH = 25;

    public static final class PayOsException extends Exception {
        public PayOsException(String message) {
            super(message);
        }

        public PayOsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private PayOsClient() {
    }

    public static boolean isConfigured() {
        return isNotBlank(BuildConfig.PAYOS_CLIENT_ID)
                && isNotBlank(BuildConfig.PAYOS_API_KEY)
                && isNotBlank(BuildConfig.PAYOS_CHECKSUM_KEY);
    }

    /**
     * Creates a new PayOS payment link and returns VietQR + bank transfer metadata.
     */
    public static PayOsPaymentInfo createPaymentRequest(
            String orderId,
            long amount,
            String buyerName,
            String buyerPhone,
            String buyerAddress
    ) throws PayOsException {
        ensureConfigured();
        if (amount <= 0L) {
            throw new PayOsException("Số tiền thanh toán không hợp lệ");
        }

        long orderCode = generateOrderCode();
        String description = buildTransferDescription(orderId);
        String returnUrl = Constants.PAYOS_RETURN_URL;
        String cancelUrl = Constants.PAYOS_CANCEL_URL;
        long expiredAtSeconds = (System.currentTimeMillis() / 1000L) + Constants.PAYOS_LINK_TTL_SECONDS;
        String signature = signPaymentRequest(
                amount,
                cancelUrl,
                description,
                orderCode,
                returnUrl,
                BuildConfig.PAYOS_CHECKSUM_KEY
        );

        try {
            JSONObject body = new JSONObject();
            body.put("orderCode", orderCode);
            body.put("amount", amount);
            body.put("description", description);
            body.put("returnUrl", returnUrl);
            body.put("cancelUrl", cancelUrl);
            body.put("expiredAt", expiredAtSeconds);
            if (isNotBlank(buyerName)) {
                body.put("buyerName", buyerName.trim());
            }
            if (isNotBlank(buyerPhone)) {
                body.put("buyerPhone", buyerPhone.trim());
            }
            if (isNotBlank(buyerAddress)) {
                body.put("buyerAddress", buyerAddress.trim());
            }
            body.put("signature", signature);

            String rawResponse = executeRequest(
                    "POST",
                    Constants.PAYOS_API_BASE_URL + "/v2/payment-requests",
                    body.toString()
            );
            return parsePaymentResponse(rawResponse, orderCode, amount, description, expiredAtSeconds);
        } catch (PayOsException e) {
            throw e;
        } catch (JSONException e) {
            throw new PayOsException("Không thể đọc phản hồi từ cổng PayOS", e);
        } catch (IOException e) {
            throw new PayOsException("Không thể kết nối cổng PayOS. Vui lòng kiểm tra Wi-Fi hoặc mạng.", e);
        }
    }

    /**
     * Queries the live payment status of a PayOS orderCode (PENDING, PAID, CANCELLED, EXPIRED).
     */
    public static PayOsPaymentInfo getPaymentStatus(long orderCode) throws PayOsException {
        ensureConfigured();
        try {
            String rawResponse = executeRequest(
                    "GET",
                    Constants.PAYOS_API_BASE_URL + "/v2/payment-requests/" + orderCode,
                    null
            );
            return parsePaymentResponse(rawResponse, orderCode, 0L, "", 0L);
        } catch (PayOsException e) {
            throw e;
        } catch (JSONException e) {
            throw new PayOsException("Phản hồi trạng thái PayOS không hợp lệ", e);
        } catch (IOException e) {
            throw new PayOsException("Không thể kiểm tra trạng thái thanh toán. Kiểm tra kết nối mạng.", e);
        }
    }

    /**
     * Cancels an active unpaid PayOS payment link.
     */
    public static PayOsPaymentInfo cancelPaymentRequest(long orderCode, String reason) throws PayOsException {
        ensureConfigured();
        try {
            JSONObject body = new JSONObject();
            body.put("cancellationReason", isNotBlank(reason) ? reason.trim() : "Khach hang huy thanh toan QR");
            String rawResponse = executeRequest(
                    "POST",
                    Constants.PAYOS_API_BASE_URL + "/v2/payment-requests/" + orderCode + "/cancel",
                    body.toString()
            );
            return parsePaymentResponse(rawResponse, orderCode, 0L, "", 0L);
        } catch (PayOsException e) {
            throw e;
        } catch (JSONException e) {
            throw new PayOsException("Phản hồi hủy giao dịch PayOS không hợp lệ", e);
        } catch (IOException e) {
            throw new PayOsException("Không thể hủy giao dịch trên PayOS. Kiểm tra kết nối mạng.", e);
        }
    }

    /**
     * Computes PayOS v2 HMAC-SHA256 request signature using the canonical alphabetical query string:
     * amount=&cancelUrl=&description=&orderCode=&returnUrl=
     */
    public static String signPaymentRequest(
            long amount,
            String cancelUrl,
            String description,
            long orderCode,
            String returnUrl,
            String checksumKey
    ) {
        String canonical = "amount=" + amount
                + "&cancelUrl=" + safe(cancelUrl)
                + "&description=" + safe(description)
                + "&orderCode=" + orderCode
                + "&returnUrl=" + safe(returnUrl);
        return hmacSha256Hex(canonical, checksumKey);
    }

    /**
     * Builds an ASCII-safe transfer description within PayOS's 25-character ceiling.
     */
    public static String buildTransferDescription(String orderId) {
        String cleaned = safe(orderId).replaceAll("[^A-Za-z0-9]", "");
        String candidate = cleaned.isEmpty() ? "TT DON HANG" : "TT " + cleaned;
        if (candidate.length() > MAX_DESCRIPTION_LENGTH) {
            return candidate.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        return candidate;
    }

    /**
     * Generates a strictly positive unique numeric orderCode safely within JS MAX_SAFE_INTEGER.
     */
    public static long generateOrderCode() {
        long secondsPart = (System.currentTimeMillis() / 1000L) % 10_000_000_000L;
        int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
        return secondsPart * 1000L + randomPart;
    }

    /**
     * Parses a PayOS v2 JSON envelope ({code, desc, data}) into a typed PayOsPaymentInfo.
     */
    public static PayOsPaymentInfo parsePaymentResponse(
            String rawJson,
            long fallbackOrderCode,
            long fallbackAmount,
            String fallbackDescription,
            long fallbackExpiredAtSeconds
    ) throws JSONException, PayOsException {
        JSONObject root = new JSONObject(rawJson);
        String code = root.optString("code", "");
        String desc = root.optString("desc", "Lỗi không xác định từ PayOS");
        if (!"00".equals(code)) {
            throw new PayOsException("PayOS (" + code + "): " + desc);
        }

        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new PayOsException("PayOS không trả về dữ liệu thanh toán");
        }

        long orderCode = data.optLong("orderCode", fallbackOrderCode);
        String paymentLinkId = data.optString("paymentLinkId", data.optString("id", ""));
        long amount = data.optLong("amount", fallbackAmount);
        String description = data.optString("description", fallbackDescription);
        String checkoutUrl = data.optString("checkoutUrl", "");
        String qrCode = data.optString("qrCode", "");
        String bin = data.optString("bin", "");
        String accountNumber = data.optString("accountNumber", "");
        String accountName = data.optString("accountName", "");
        String status = data.optString("status", PayOsPaymentInfo.STATUS_PENDING);
        long expiredAt = data.optLong("expiredAt", fallbackExpiredAtSeconds);

        return new PayOsPaymentInfo(
                orderCode,
                paymentLinkId,
                amount,
                description,
                checkoutUrl,
                qrCode,
                bin,
                accountNumber,
                accountName,
                status,
                expiredAt
        );
    }

    private static String executeRequest(String method, String endpoint, String jsonBody)
            throws IOException, PayOsException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("x-client-id", BuildConfig.PAYOS_CLIENT_ID);
            conn.setRequestProperty("x-api-key", BuildConfig.PAYOS_API_KEY);
            conn.setRequestProperty("Accept", "application/json");

            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                    os.flush();
                }
            }

            int statusCode = conn.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 400
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody = readStream(stream);
            if (responseBody.isEmpty()) {
                throw new PayOsException("PayOS phản hồi rỗng (HTTP " + statusCode + ")");
            }
            return responseBody;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static void ensureConfigured() throws PayOsException {
        if (!isConfigured()) {
            throw new PayOsException("Chưa cấu hình đầy đủ PAYOS_CLIENT_ID / API_KEY / CHECKSUM_KEY");
        }
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(safe(secret).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(safe(data).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format(Locale.US, "%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256 signature", e);
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
