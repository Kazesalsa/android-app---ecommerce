package com.example.btday4.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.btday4.data.local.UserEntity;
import com.example.btday4.data.remote.FirestoreHelper;
import com.example.btday4.tracking.RecommendationEngine;
import com.example.btday4.util.Constants;
import com.example.btday4.util.PasswordHasher;
import com.example.btday4.util.ValidationUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Manages active user session state in SharedPreferences backed 100% by Cloud Firestore.
 */
public class UserSession {

    private static final String TAG = "UserSession";
    private static final ExecutorService AUTH_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public enum AuthResult {
        SUCCESS,
        ACCOUNT_NOT_FOUND,
        WRONG_PASSWORD,
        ACCOUNT_EXISTS,
        NETWORK_ERROR
    }

    private final Context appContext;
    private final SharedPreferences prefs;

    public UserSession(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void login(String username, String password, boolean rememberMe, Consumer<AuthResult> callback) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty()) {
            callback.accept(AuthResult.ACCOUNT_NOT_FOUND);
            return;
        }
        DocumentReference docRef = usersRef().document(cleanUsername);
        docRef.get().addOnSuccessListener(snapshot -> AUTH_EXECUTOR.execute(() -> {
            UserEntity user = snapshot.exists() ? snapshot.toObject(UserEntity.class) : null;
            if (user == null && Constants.DEMO_USERNAME.equals(cleanUsername)) {
                UserEntity demo = createDemoUser();
                if (!PasswordHasher.matches(password, demo.getPassword())) {
                    MAIN_HANDLER.post(() -> callback.accept(AuthResult.WRONG_PASSWORD));
                    return;
                }
                startSession(demo, rememberMe);
                MAIN_HANDLER.post(() -> callback.accept(AuthResult.SUCCESS));
                return;
            }
            if (user == null) {
                MAIN_HANDLER.post(() -> callback.accept(AuthResult.ACCOUNT_NOT_FOUND));
                return;
            }
            String storedPassword = user.getPassword();
            if (storedPassword == null || storedPassword.isEmpty()) {
                if (Constants.DEMO_USERNAME.equals(cleanUsername)) {
                    if (!Constants.DEMO_PASSWORD.equals(password)) {
                        MAIN_HANDLER.post(() -> callback.accept(AuthResult.WRONG_PASSWORD));
                        return;
                    }
                    user.setPassword(PasswordHasher.hash(password));
                } else if (ValidationUtils.isValidPassword(password)) {
                    user.setPassword(PasswordHasher.hash(password));
                } else {
                    MAIN_HANDLER.post(() -> callback.accept(AuthResult.WRONG_PASSWORD));
                    return;
                }
            } else if (!PasswordHasher.matches(password, storedPassword)) {
                MAIN_HANDLER.post(() -> callback.accept(AuthResult.WRONG_PASSWORD));
                return;
            } else if (!PasswordHasher.isHashed(storedPassword)) {
                user.setPassword(PasswordHasher.hash(password));
            }
            if (user.getUsername().isEmpty()) {
                user.setUsername(cleanUsername);
            }
            startSession(user, rememberMe);
            MAIN_HANDLER.post(() -> callback.accept(AuthResult.SUCCESS));
        })).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore login failed for " + cleanUsername, e);
            callback.accept(AuthResult.NETWORK_ERROR);
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
        DocumentReference docRef = usersRef().document(cleanUsername);
        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                callback.accept(AuthResult.ACCOUNT_EXISTS);
                return;
            }
            AUTH_EXECUTOR.execute(() -> {
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
                startSession(user, rememberMe);
                MAIN_HANDLER.post(() -> callback.accept(AuthResult.SUCCESS));
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore register failed for " + cleanUsername, e);
            callback.accept(AuthResult.NETWORK_ERROR);
        });
    }

    private void startSession(UserEntity user, boolean rememberMe) {
        String loginTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        user.setLoginTime(loginTime);
        String displayName = ValidationUtils.isNotBlank(user.getDisplayName())
                ? user.getDisplayName()
                : deriveDisplayName(user.getUsername());
        user.setDisplayName(displayName);
        prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USERNAME, user.getUsername())
                .putString(Constants.KEY_DISPLAY_NAME, displayName)
                .putBoolean(Constants.KEY_REMEMBER_ME, rememberMe)
                .commit();
        RecommendationEngine.clearCache();
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", finalName);
        updates.put("email", cleanEmail);
        updates.put("phone", cleanPhone);
        updates.put("gender", cleanGender);
        updates.put("birthDate", cleanBirthDate);
        updates.put("address", cleanAddress);

        usersRef().document(username)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (onDone != null) {
                        onDone.run();
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", finalName);
        updates.put("phone", cleanPhone);
        updates.put("address", cleanAddress);

        usersRef().document(username)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    public void changePassword(String currentPassword, String newPassword, Consumer<Boolean> callback) {
        String username = getUsername();
        if (username.isEmpty() || !ValidationUtils.isValidPassword(newPassword)) {
            callback.accept(false);
            return;
        }
        DocumentReference docRef = usersRef().document(username);
        docRef.get().addOnSuccessListener(snapshot -> AUTH_EXECUTOR.execute(() -> {
            UserEntity user = snapshot.exists() ? snapshot.toObject(UserEntity.class) : null;
            boolean verified = user != null && PasswordHasher.matches(currentPassword, user.getPassword());
            if (!verified) {
                MAIN_HANDLER.post(() -> callback.accept(false));
                return;
            }
            String hashedNew = PasswordHasher.hash(newPassword);
            docRef.update("password", hashedNew)
                    .addOnSuccessListener(unused -> callback.accept(true))
                    .addOnFailureListener(e -> callback.accept(false));
        })).addOnFailureListener(e -> callback.accept(false));
    }

    public void loadCurrentUser(Consumer<UserEntity> callback) {
        String username = getUsername();
        if (username.isEmpty()) {
            callback.accept(null);
            return;
        }
        usersRef().document(username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserEntity user = snapshot.exists() ? snapshot.toObject(UserEntity.class) : null;
                    callback.accept(user);
                })
                .addOnFailureListener(e -> callback.accept(null));
    }

    /**
     * Ends the session, flushes the user's tracking summary once to Firestore, and clears the ranking cache.
     */
    public void logout() {
        String username = getUsername();
        if (!username.isEmpty()) {
            RecommendationEngine.syncTrackingSummaryToFirestore(appContext, username);
        }
        SharedPreferences.Editor editor = prefs.edit().putBoolean(Constants.KEY_IS_LOGGED_IN, false);
        if (!isRemembered()) {
            editor.remove(Constants.KEY_USERNAME);
            editor.remove(Constants.KEY_DISPLAY_NAME);
        }
        editor.apply();
        VoucherRepository.clearPendingVoucherCode(appContext);
        RecommendationEngine.clearCache();
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

    private static com.google.firebase.firestore.CollectionReference usersRef() {
        return FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS);
    }

    private static UserEntity createDemoUser() {
        return new UserEntity(
                Constants.DEMO_USERNAME,
                Constants.DEMO_DISPLAY_NAME,
                PasswordHasher.hash(Constants.DEMO_PASSWORD),
                "",
                "",
                "",
                Constants.DEMO_USERNAME,
                "",
                ""
        );
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
