package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.btday4.data.local.AppDatabase;
import com.example.btday4.data.local.UserEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.util.Constants;
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

    private final Context appContext;
    private final SharedPreferences prefs;

    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String username, String password, boolean rememberMe, Runnable onDone) {
        String cleanUsername = username == null ? "" : username.trim();
        String defaultDisplayName = deriveDisplayName(cleanUsername);
        String loginTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String cleanPassword = password == null ? "" : password;

        prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USERNAME, cleanUsername)
                .putString(Constants.KEY_DISPLAY_NAME, defaultDisplayName)
                .putBoolean(Constants.KEY_REMEMBER_ME, rememberMe)
                .apply();

        AppDatabase.runAsync(appContext, db -> {
            UserEntity existing = db.userDao().findByUsername(cleanUsername);
            String finalDisplayName = existing != null && ValidationUtils.isNotBlank(existing.getDisplayName())
                    ? existing.getDisplayName()
                    : defaultDisplayName;
            String existingPhone = existing != null && existing.getPhone() != null ? existing.getPhone() : "";
            String existingAddress = existing != null && existing.getAddress() != null ? existing.getAddress() : "";
            String existingEmail = existing != null && ValidationUtils.isNotBlank(existing.getEmail())
                    ? existing.getEmail()
                    : (ValidationUtils.isValidEmail(cleanUsername) ? cleanUsername : "");
            String existingGender = existing != null && existing.getGender() != null ? existing.getGender() : "";
            String existingBirthDate = existing != null && existing.getBirthDate() != null ? existing.getBirthDate() : "";

            prefs.edit().putString(Constants.KEY_DISPLAY_NAME, finalDisplayName).apply();

            UserEntity entity = new UserEntity(
                    cleanUsername,
                    finalDisplayName,
                    cleanPassword,
                    existingPhone,
                    existingAddress,
                    loginTime,
                    existingEmail,
                    existingGender,
                    existingBirthDate
            );
            db.userDao().upsert(entity);
            FirestoreHelper.saveUser(entity, null);

            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
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
            if (callback != null) {
                callback.accept(false);
            }
            return;
        }
        AppDatabase.runAsync(appContext, db -> {
            UserEntity user = db.userDao().findByUsername(username);
            if (user == null || user.getPassword() == null || !user.getPassword().equals(currentPassword)) {
                if (callback != null) {
                    AppDatabase.postToMain(() -> callback.accept(false));
                }
                return;
            }
            db.userDao().updatePassword(username, newPassword);
            user.setPassword(newPassword);
            FirestoreHelper.saveUser(user, null);
            if (callback != null) {
                AppDatabase.postToMain(() -> callback.accept(true));
            }
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

    public void logout(Runnable onDone) {
        boolean keepSavedUsername = isRemembered();
        String currentUsername = getUsername();

        SharedPreferences.Editor editor = prefs.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, false);
        if (!keepSavedUsername) {
            editor.remove(Constants.KEY_USERNAME);
            editor.remove(Constants.KEY_DISPLAY_NAME);
        }
        editor.apply();

        AppDatabase.runAsync(appContext, db -> {
            if (!currentUsername.isEmpty() && !keepSavedUsername) {
                db.userDao().deleteByUsername(currentUsername);
            }
            if (onDone != null) {
                AppDatabase.postToMain(onDone);
            }
        });
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
