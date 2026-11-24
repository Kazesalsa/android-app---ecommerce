package com.example.btday4.model;

import com.example.btday4.data.local.OrderItemEntity;
import com.example.btday4.data.local.Product;
import com.example.btday4.util.FormatUtils;

/**
 * Domain model representing a product line item inside the shopping cart.
 */
public class CartItem {

    private final Product product;
    private final int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPriceAmount() {
        return FormatUtils.parseVnd(product.getPrice());
    }

    public long getSubtotalAmount() {
        return getUnitPriceAmount() * quantity;
    }

    public String getSubtotalText() {
        return FormatUtils.formatVnd(getSubtotalAmount());
    }

    public OrderItemEntity toOrderItemSnapshot(String orderId) {
        return new OrderItemEntity(
                orderId == null ? "" : orderId,
                product.getId(),
                product.getName() == null ? "" : product.getName(),
                product.getImageUrl(),
                getUnitPriceAmount(),
                quantity
        );
    }
}
