package com.example.btday4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.btday4.data.local.Product;
import com.example.btday4.data.local.ReviewEntity;
import com.example.btday4.data.repository.CartRepository;
import com.example.btday4.data.repository.ProductRepository;
import com.example.btday4.data.repository.ReviewRepository;
import com.example.btday4.model.CartItem;
import com.example.btday4.util.FormatUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ProductRepositoryTest {

    @Test
    public void defaultSeedIncludesRichProductDetailsAndSpecs() {
        List<Product> products = ProductRepository.createDefaultProducts();
        assertFalse(products.isEmpty());

        Product iphone15 = products.stream()
                .filter(p -> "iphone_15".equals(p.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("iPhone 15 should be present in default seed", iphone15);
        assertEquals("iPhone 15 128GB", iphone15.getName());
        assertEquals("Apple", iphone15.getBrand());
        assertFalse(iphone15.getPrice().isEmpty());

        for (Product product : products) {
            assertNotNull(product.getImageUrl());
            assertFalse(product.getImageUrl().trim().isEmpty());
            assertNotNull(product.getDescription());
            assertFalse(product.getDescription().trim().isEmpty());
            Map<String, String> specs = product.parseSpecs();
            assertFalse("Specs should parse cleanly for " + product.getId(), specs.isEmpty());
            assertTrue("Warranty should be included in specs", specs.containsKey("Bảo hành"));
        }
    }

    @Test
    public void defaultReviewsReferenceValidSeedProductIds() {
        Set<String> productIds = new HashSet<>();
        for (Product product : ProductRepository.createDefaultProducts()) {
            productIds.add(product.getId());
        }
        List<ReviewEntity> reviews = ReviewRepository.createDefaultReviews();
        assertFalse(reviews.isEmpty());
        for (ReviewEntity review : reviews) {
            assertTrue(
                    "Review references unknown product: " + review.getProductId(),
                    productIds.contains(review.getProductId())
            );
            assertTrue(review.getRating() >= 1 && review.getRating() <= 5);
        }
    }

    @Test
    public void cartItemCalculatesVndSubtotalsAndGrandTotal() {
        List<Product> products = ProductRepository.createDefaultProducts();
        Product iphone15 = products.get(0);
        Product airpods = products.stream()
                .filter(p -> "airpods_pro_2".equals(p.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(airpods);

        CartItem item1 = new CartItem(iphone15, 2);
        CartItem item2 = new CartItem(airpods, 1);

        assertEquals(19990000L, FormatUtils.parseVnd("19.990.000 ₫"));
        assertEquals("39.980.000 ₫", item1.getSubtotalText());
        assertEquals("45.470.000 ₫", CartRepository.getGrandTotalText(Arrays.asList(item1, item2)));
    }
}
