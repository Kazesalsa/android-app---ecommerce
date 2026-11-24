package com.example.btday4.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Room DAO for storing, reading, and updating user profile and credential records.
 */
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity findByUsername(String username);

    @Query(
            "UPDATE users SET display_name = :displayName, email = :email, phone = :phone, "
                    + "gender = :gender, birth_date = :birthDate, address = :address "
                    + "WHERE username = :username"
    )
    void updateProfile(
            String username,
            String displayName,
            String email,
            String phone,
            String gender,
            String birthDate,
            String address
    );

    @Query(
            "UPDATE users SET display_name = :displayName, phone = :phone, address = :address "
                    + "WHERE username = :username"
    )
    void updateShippingInfo(String username, String displayName, String phone, String address);

    @Query("UPDATE users SET password = :newPassword WHERE username = :username")
    void updatePassword(String username, String newPassword);

    @Query("DELETE FROM users WHERE username = :username")
    void deleteByUsername(String username);
}
