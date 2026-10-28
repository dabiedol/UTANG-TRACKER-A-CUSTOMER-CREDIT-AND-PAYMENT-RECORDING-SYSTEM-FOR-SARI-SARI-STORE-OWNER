package utangtracker.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot hash password", ex);
        }
    }

    public static boolean matches(String password, String salt, String expectedHash) {
        return hash(password, salt).equals(expectedHash);
    }

    /** Hashes a recovery PIN. Stored as "salt$hash" so it is never kept in plain text. */
    public static String hashPin(String pin) {
        String salt = newSalt();
        return salt + "$" + hash(pin, salt);
    }

    public static boolean isHashedPin(String stored) {
        return stored != null && stored.indexOf('$') > 0;
    }

    public static boolean matchesPin(String pin, String stored) {
        if (pin == null || !isHashedPin(stored)) {
            return false;
        }
        int split = stored.indexOf('$');
        return matches(pin, stored.substring(0, split), stored.substring(split + 1));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
