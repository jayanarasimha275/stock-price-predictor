package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cryptographic password hashing utility using PBKDF2WithHmacSHA256 with random 128-bit salts
 * and constant-time digest verification.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String rawPassword, String base64Salt) {
        if (rawPassword == null || base64Salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null.");
        }
        char[] chars = rawPassword.toCharArray();
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS);
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
                byte[] hash = skf.generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(hash);
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("PBKDF2 password hashing failed: " + ex.getMessage(), ex);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    public static boolean verifyPassword(String rawPassword, String expectedHash, String base64Salt) {
        if (rawPassword == null || expectedHash == null || base64Salt == null
                || expectedHash.isEmpty() || base64Salt.isEmpty()) {
            return false;
        }
        try {
            String computed = hashPassword(rawPassword, base64Salt);
            byte[] a = Base64.getDecoder().decode(computed);
            byte[] b = Base64.getDecoder().decode(expectedHash);
            return MessageDigest.isEqual(a, b);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
