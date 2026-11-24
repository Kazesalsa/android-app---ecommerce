package com.example.btday4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.btday4.util.PasswordHasher;
import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void hashedPasswordVerifiesOnlyWithOriginalInput() {
        String stored = PasswordHasher.hash("123456");

        assertTrue(PasswordHasher.isHashed(stored));
        assertFalse(stored.contains("123456"));
        assertTrue(PasswordHasher.matches("123456", stored));
        assertFalse(PasswordHasher.matches("1234567", stored));
        assertFalse(PasswordHasher.matches("", stored));
    }

    @Test
    public void sameInputProducesDifferentSaltedHashes() {
        assertNotEquals(PasswordHasher.hash("secret"), PasswordHasher.hash("secret"));
    }

    @Test
    public void legacyPlaintextRowsStillMatchForTransparentUpgrade() {
        assertFalse(PasswordHasher.isHashed("abcd"));
        assertTrue(PasswordHasher.matches("abcd", "abcd"));
        assertFalse(PasswordHasher.matches("abce", "abcd"));
        assertFalse(PasswordHasher.matches("abcd", "sha256$zz$zz"));
        assertFalse(PasswordHasher.matches("abcd", null));
    }
}
