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

    /**
     * Returns -1 when the username already exists so registration never overwrites an account.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
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

    @Query("UPDATE users SET password = :passwordHash WHERE username = :username")
    void updatePassword(String username, String passwordHash);

    @Query("UPDATE users SET login_time = :loginTime WHERE username = :username")
    void updateLoginTime(String username, String loginTime);
}
