package com.example.btday4.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.example.btday4.data.local.OrderEntity;
import com.example.btday4.data.local.OrderItemEntity;
import java.util.Collections;
import java.util.List;

/**
 * Room relation grouping an OrderEntity with its purchased line items.
 */
public class OrderWithItems {

    @Embedded
    public OrderEntity order;

    @Relation(parentColumn = "id", entityColumn = "order_id")
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
