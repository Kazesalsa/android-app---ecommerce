package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.example.btday4.model.OrderWithItems;
import java.util.List;

/**
 * Room DAO for storing and querying placed orders and their line items.
 */
@Dao
public interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertOrder(OrderEntity order);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertOrderItems(List<OrderItemEntity> items);

    @Transaction
    @Query("SELECT * FROM orders WHERE username = :username ORDER BY created_at DESC")
    List<OrderWithItems> getOrdersByUsername(String username);

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    OrderWithItems findOrderWithItemsById(String orderId);

    @Query("SELECT COUNT(*) FROM orders WHERE username = :username")
    int countByUsername(String username);

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    void updateStatus(String orderId, String status);
}
