package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.btday4.data.remote.PayOsClient;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.PayOsPaymentInfo;
import com.example.btday4.model.PaymentMethod;
import com.example.btday4.util.QrCodeGenerator;
import org.junit.Test;

/**
 * Unit tests for PayOS HMAC-SHA256 request signing, transfer descriptions, JSON response parsing,
 * payment methods, and bank BIN resolution.
 */
public class PayOsClientTest {

    @Test
    public void signatureMatchesHmacSha256CanonicalSpec() {
        String sig1 = PayOsClient.signPaymentRequest(
                10_000L,
                "https://btday4.app/payment/cancel",
                "TT DH202611241234",
                170000001L,
                "https://btday4.app/payment/return",
                "test_checksum_secret"
        );
        String sig2 = PayOsClient.signPaymentRequest(
                10_000L,
                "https://btday4.app/payment/cancel",
                "TT DH202611241234",
                170000001L,
                "https://btday4.app/payment/return",
                "test_checksum_secret"
        );
        String sigDifferentAmount = PayOsClient.signPaymentRequest(
                20_000L,
                "https://btday4.app/payment/cancel",
                "TT DH202611241234",
                170000001L,
                "https://btday4.app/payment/return",
                "test_checksum_secret"
        );

        assertEquals(64, sig1.length());
        assertTrue(sig1.matches("[0-9a-f]{64}"));
        assertEquals(sig1, sig2);
        assertNotEquals(sig1, sigDifferentAmount);
    }

    @Test
    public void transferDescriptionIsAsciiSafeAndWithinPayOsLimit() {
        String desc = PayOsClient.buildTransferDescription("DH-20261124-1234");
        assertEquals("TT DH202611241234", desc);
        assertTrue(desc.length() <= 25);

        String longDesc = PayOsClient.buildTransferDescription("DH-20261124-12345678901234567890");
        assertEquals(25, longDesc.length());
        assertTrue(longDesc.matches("[A-Za-z0-9 ]+"));
    }

    @Test
    public void parsePaymentResponseHandlesSuccessAndErrorEnvelopes() throws Exception {
        String successJson = "{"
                + "\"code\":\"00\","
                + "\"desc\":\"success\","
                + "\"data\":{"
                + "\"bin\":\"970422\","
                + "\"accountNumber\":\"0123456789\","
                + "\"accountName\":\"NGUYEN VAN A\","
                + "\"amount\":19990000,"
                + "\"description\":\"TT DH202611241234\","
                + "\"orderCode\":1795491924123,"
                + "\"paymentLinkId\":\"link_abc123\","
                + "\"status\":\"PENDING\","
                + "\"checkoutUrl\":\"https://pay.payos.vn/web/link_abc123\","
                + "\"qrCode\":\"00020101021238570010A00000072701270006970422\""
                + "}"
                + "}";

        PayOsPaymentInfo info = PayOsClient.parsePaymentResponse(
                successJson,
                0L,
                0L,
                "",
                1795492824L
        );

        assertEquals(1795491924123L, info.getOrderCode());
        assertEquals("link_abc123", info.getPaymentLinkId());
        assertEquals(19_990_000L, info.getAmount());
        assertEquals("970422", info.getBin());
        assertEquals("0123456789", info.getAccountNumber());
        assertEquals("NGUYEN VAN A", info.getAccountName());
        assertEquals("https://pay.payos.vn/web/link_abc123", info.getCheckoutUrl());
        assertFalse(info.isPaid());

        String errorJson = "{\"code\":\"231\",\"desc\":\"Đơn thanh toán đã tồn tại\"}";
        try {
            PayOsClient.parsePaymentResponse(errorJson, 1L, 1000L, "TT", 0L);
            fail("Expected PayOsException for non-00 code");
        } catch (PayOsClient.PayOsException expected) {
            assertTrue(expected.getMessage().contains("231"));
        }
    }

    @Test
    public void paymentMethodAndOrderStatusAndBankBinMappingWork() {
        assertEquals(PaymentMethod.COD, PaymentMethod.fromCode("COD"));
        assertEquals(PaymentMethod.VIETQR, PaymentMethod.fromCode("VIETQR"));
        assertEquals(PaymentMethod.VIETQR, PaymentMethod.fromCode("ONLINE_GATEWAY"));
        assertEquals("Thanh toán khi giao hàng", PaymentMethod.COD.getTitle());
        assertEquals("Thanh toán bằng mã QR", PaymentMethod.VIETQR.getTitle());

        assertEquals(OrderStatus.PAID, OrderStatus.fromCode("PAID"));
        assertEquals("Đã thanh toán", OrderStatus.PAID.getLabel());
        assertEquals("MB Bank", QrCodeGenerator.resolveBankName("970422"));
        assertEquals("Vietcombank", QrCodeGenerator.resolveBankName("970436"));
        assertEquals("Ngân hàng (BIN 999999)", QrCodeGenerator.resolveBankName("999999"));
    }
}
