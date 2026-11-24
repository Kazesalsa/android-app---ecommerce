package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.UserEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.util.Constants;
import com.example.btday4.util.PasswordHasher;
import com.example.btday4.util.ValidationUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Manages active user session state and synchronizes user profile details across SharedPreferences,
 * Room SQLite, and Cloud Firestore.
 */
public class UserSession {

    public enum AuthResult {
        SUCCESS,
        ACCOUNT_NOT_FOUND,
        WRONG_PASSWORD,
        ACCOUNT_EXISTS
    }

    private final Context appContext;
    private final SharedPreferences prefs;

    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Verifies credentials against the stored salted hash. Legacy plaintext rows are re-hashed on
     * the first successful login.
     */
    public void login(String username, String password, boolean rememberMe, Consumer<AuthResult> callback) {
        String cleanUsername = username == null ? "" : username.trim();
        AppDatabase.runAsync(appContext, db -> {
            UserEntity user = db.userDao().findByUsername(cleanUsername);
            AuthResult result;
            if (user == null) {
                result = AuthResult.ACCOUNT_NOT_FOUND;
            } else if (!PasswordHasher.matches(password, user.getPassword())) {
                result = AuthResult.WRONG_PASSWORD;
            } else {
                if (!PasswordHasher.isHashed(user.getPassword())) {
                    db.userDao().updatePassword(user.getUsername(), PasswordHasher.hash(password));
                }
                startSession(db, user, rememberMe);
                result = AuthResult.SUCCESS;
            }
            AppDatabase.postToMain(() -> callback.accept(result));
        });
    }

    public void register(
            String username,
            String displayName,
            String password,
            boolean rememberMe,
            Consumer<AuthResult> callback
    ) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanDisplayName = ValidationUtils.isNotBlank(displayName)
                ? displayName.trim()
                : deriveDisplayName(cleanUsername);
        AppDatabase.runAsync(appContext, db -> {
            AuthResult result;
            if (db.userDao().findByUsername(cleanUsername) != null) {
                result = AuthResult.ACCOUNT_EXISTS;
            } else {
                UserEntity user = new UserEntity(
                        cleanUsername,
                        cleanDisplayName,
                        PasswordHasher.hash(password),
                        "",
                        "",
                        "",
                        ValidationUtils.isValidEmail(cleanUsername) ? cleanUsername : "",
                        "",
                        ""
                );
                db.userDao().insert(user);
                startSession(db, user, rememberMe);
                result = AuthResult.SUCCESS;
            }
            AppDatabase.postToMain(() -> callback.accept(result));
        });
    }

    private void startSession(AppDatabase db, UserEntity user, boolean rememberMe) {
        String loginTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        db.userDao().updateLoginTime(user.getUsername(), loginTime);
        user.setLoginTime(loginTime);
        String displayName = ValidationUtils.isNotBlank(user.getDisplayName())
                ? user.getDisplayName()
                : deriveDisplayName(user.getUsername());
        prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USERNAME, user.getUsername())
                .putString(Constants.KEY_DISPLAY_NAME, displayName)
                .putBoolean(Constants.KEY_REMEMBER_ME, rememberMe)
                .commit();
        FirestoreHelper.saveUser(user, null);
    }

    public void updateProfile(
            String displayName,
            String email,
            String phone,
            String gender,
            String birthDate,
            String address,
            Runnable onDone
    ) {
        String username = getUsername();
        if (username.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        String cleanName = displayName == null ? "" : displayName.trim();
        String finalName = cleanName.isEmpty() ? deriveDisplayName(username) : cleanName;
        String cleanEmail = email == null ? "" : email.trim();
        String cleanPhone = ValidationUtils.normalizePhone(phone);
        String cleanGender = gender == null ? "" : gender.trim();
        String cleanBirthDate = birthDate == null ? "" : birthDate.trim();
        String cleanAddress = address == null ? "" : address.trim();

        prefs.edit().putString(Constants.KEY_DISPLAY_NAME, finalName).apply();

        AppDatabase.runAsync(appContext, db -> {
            db.userDao().updateProfile(
                    username,
                    finalName,
                    cleanEmail,
                    cleanPhone,
                    cleanGender,
                    cleanBirthDate,
                    cleanAddress
            );
            UserEntity updated = db.userDao().findByUsername(username);
            if (updated != null) {
                FirestoreHelper.saveUser(updated, null);
            }
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public void updateShippingInfo(String displayName, String phone, String address, Runnable onDone) {
        String username = getUsername();
        if (username.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        String finalName = ValidationUtils.isNotBlank(displayName)
                ? displayName.trim()
                : getDisplayName();
        String cleanPhone = ValidationUtils.normalizePhone(phone);
        String cleanAddress = address == null ? "" : address.trim();

        prefs.edit().putString(Constants.KEY_DISPLAY_NAME, finalName).apply();

        AppDatabase.runAsync(appContext, db -> {
            db.userDao().updateShippingInfo(username, finalName, cleanPhone, cleanAddress);
            UserEntity updated = db.userDao().findByUsername(username);
            if (updated != null) {
                FirestoreHelper.saveUser(updated, null);
            }
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword, Consumer<Boolean> callback) {
        String username = getUsername();
        if (username.isEmpty() || !ValidationUtils.isValidPassword(newPassword)) {
            callback.accept(false);
            return;
        }
        AppDatabase.runAsync(appContext, db -> {
            UserEntity user = db.userDao().findByUsername(username);
            boolean verified = user != null && PasswordHasher.matches(currentPassword, user.getPassword());
            if (verified) {
                db.userDao().updatePassword(user.getUsername(), PasswordHasher.hash(newPassword));
            }
            AppDatabase.postToMain(() -> callback.accept(verified));
        });
    }

    public void loadCurrentUser(Consumer<UserEntity> callback) {
        String username = getUsername();
        if (username.isEmpty()) {
            callback.accept(null);
            return;
        }
        AppDatabase.runAsync(appContext, db -> {
            UserEntity user = db.userDao().findByUsername(username);
            AppDatabase.postToMain(() -> callback.accept(user));
        });
    }

    /**
     * Ends the session only; the registered account and its cart stay stored for the next login.
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, false);
        if (!isRemembered()) {
            editor.remove(Constants.KEY_USERNAME);
            editor.remove(Constants.KEY_DISPLAY_NAME);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    public boolean isRemembered() {
        return prefs.getBoolean(Constants.KEY_REMEMBER_ME, false);
    }

    public String getUsername() {
        return prefs.getString(Constants.KEY_USERNAME, "");
    }

    public String getDisplayName() {
        String name = prefs.getString(Constants.KEY_DISPLAY_NAME, "");
        return name.isEmpty() ? getUsername() : name;
    }

    private String deriveDisplayName(String username) {
        if (username.isEmpty()) {
            return "Người dùng";
        }
        String local = username.contains("@") ? username.substring(0, username.indexOf('@')) : username;
        String[] parts = local.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : username;
    }
}
