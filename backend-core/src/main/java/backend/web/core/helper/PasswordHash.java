package backend.web.core.helper;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import backend.web.core.utility.LogUtils;

public class PasswordHash {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        try {
            var salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            var hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            var encoder = Base64.getEncoder();

            return String.join("$", "pbkdf2_sha256", String.valueOf(ITERATIONS), encoder.encodeToString(salt), encoder.encodeToString(hash));
        } catch (Exception e) {
            LogUtils.error("Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public boolean verify(String password, String storedPassword) {
        try {
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
            var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            var actualHash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            LogUtils.error("Error verifying password", e);
            return false;
        }
    }
}
