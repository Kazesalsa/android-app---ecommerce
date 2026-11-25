package com.example.btday4.model;

import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import java.util.Collections;
import java.util.List;

/**
 * Domain model grouping an OrderEntity with its purchased line items.
 */
public class OrderWithItems {

    public OrderEntity order;
    public List<OrderItemEntity> items;

    public List<OrderItemEntity> safeItems() {
        return items == null ? Collections.emptyList() : items;
    }

    public int totalQuantity() {
        int total = 0;
        for (OrderItemEntity item : safeItems()) {
            total += item.getQuantity();
        }
        return total;
    }
}
