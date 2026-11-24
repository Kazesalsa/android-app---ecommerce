package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Room DAO for storing and querying PayOS QR payment records linked to orders.
 */
@Dao
public interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PaymentEntity payment);

    @Query("SELECT * FROM payments WHERE order_id = :orderId ORDER BY created_at DESC LIMIT 1")
    PaymentEntity findLatestByOrderId(String orderId);

    @Query("SELECT * FROM payments WHERE order_code = :orderCode LIMIT 1")
    PaymentEntity findByOrderCode(long orderCode);

    @Query("UPDATE payments SET status = :status, paid_at = :paidAtMillis WHERE order_code = :orderCode")
    void updateStatus(long orderCode, String status, long paidAtMillis);
}
