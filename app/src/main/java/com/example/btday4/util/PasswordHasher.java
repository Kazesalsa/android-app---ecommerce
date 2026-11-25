package com.example.btday4.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted, iterated SHA-256 password hashing. Stored format: {@code sha256$<saltHex>$<hashHex>}.
 */
public final class PasswordHasher {

    private static final String PREFIX = "sha256$";
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 10_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return PREFIX + toHex(salt) + "$" + toHex(digest(salt, password));
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /**
     * Verifies a password against a stored value, accepting legacy plaintext rows so they can be
     * upgraded transparently after a successful login.
     */
    public static boolean matches(String password, String stored) {
        if (password == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (!isHashed(stored)) {
            return MessageDigest.isEqual(
                    stored.getBytes(StandardCharsets.UTF_8),
                    password.getBytes(StandardCharsets.UTF_8)
            );
        }
        String[] parts = stored.substring(PREFIX.length()).split("\\$");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = fromHex(parts[0]);
        byte[] expected = fromHex(parts[1]);
        return salt != null && expected != null && MessageDigest.isEqual(expected, digest(salt, password));
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            byte[] result = sha.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < ITERATIONS; i++) {
                sha.reset();
                result = sha.digest(result);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }

    private static byte[] fromHex(String hex) {
        if (hex.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
