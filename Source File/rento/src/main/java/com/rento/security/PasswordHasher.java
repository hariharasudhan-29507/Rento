package com.rento.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing and verification using BCrypt.
 */
public class PasswordHasher {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash a plain text password.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify a password against a stored hash.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            System.err.println("[PasswordHasher] Verification failed: " + e.getMessage());
            return false;
        }
    }
}
