package backend.web.core.helper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHash {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHash() {
    }

    public static String hash(String password) {
        var salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        var hash = deriveKey(password, salt, ITERATIONS, KEY_LENGTH);
        var encoder = Base64.getEncoder();

        return String.join("$", "pbkdf2_sha256", String.valueOf(ITERATIONS),
                encoder.encodeToString(salt), encoder.encodeToString(hash));
    }

    public static boolean verify(String password, String storedPassword) {
        if (password == null || storedPassword == null) {
            return false;
        }

        var parts = storedPassword.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }

        var iterations = Integer.parseInt(parts[1]);
        var decoder = Base64.getDecoder();
        var salt = decoder.decode(parts[2]);
        var expectedHash = decoder.decode(parts[3]);
        var actualHash = deriveKey(password, salt, iterations, expectedHash.length * 8);

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] deriveKey(String password, byte[] salt, int iterations, int keyLength) {
        try {
            var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
