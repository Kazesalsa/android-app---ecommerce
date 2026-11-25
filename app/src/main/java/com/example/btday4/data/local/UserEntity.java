package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Firestore POJO storing user credentials (hashed) and personal profile details.
 */
@IgnoreExtraProperties
public class UserEntity {

    @NonNull
    private String username;

    private String displayName;
    private String password;
    private String phone;
    private String address;
    private String loginTime;
    private String email;
    private String gender;
    private String birthDate;

    public UserEntity() {
        this.username = "";
    }

    public UserEntity(
            @NonNull String username,
            String displayName,
            String password,
            String phone,
            String address,
            String loginTime,
            String email,
            String gender,
            String birthDate
    ) {
        this.username = username;
        this.displayName = displayName;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.loginTime = loginTime;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Salted PBKDF2 hash stored in Firestore so accounts work across devices.
     * shortcut: replace with Firebase Authentication when Email/Password sign-in is enabled on Console.
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
}
