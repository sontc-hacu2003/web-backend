package backend.web.core.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import backend.web.core.model.response.base.BaseEncryptedResponse;
import org.springframework.stereotype.Service;

import backend.web.core.model.request.base.EncryptedRequest;

@Service
public class MiddlewareCryptoService {
    private static final String EC_ALGORITHM = "EC";
    private static final String ECDH_ALGORITHM = "ECDH";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int RAW_PUBLIC_KEY_LENGTH_BYTES = 65;
    private static final int COORDINATE_LENGTH_BYTES = 32;

    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final KeyPair serverKeyPair;

    // TODO: In production with multiple instances, load key pair from config/vault/HSM
    //       instead of generating new keys on each startup to avoid decrypt failures
    public MiddlewareCryptoService() {
        try {
            var generator = KeyPairGenerator.getInstance(EC_ALGORITHM);
            generator.initialize(256);
            this.serverKeyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can not initialize middleware key pair", e);
        }
    }

    public String getPublicKey() {
        return encoder.encodeToString(toRawPublicKey((ECPublicKey) serverKeyPair.getPublic()));
    }

    public String decrypt(EncryptedRequest request) {
        try {
            var aesKey = deriveAesKey(request.p());
            var cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, decoder.decode(request.s())));
            var decrypted = cipher.doFinal(decoder.decode(request.d()));

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid encrypted request", e);
        }
    }

    public BaseEncryptedResponse encrypt(String responseBody, String clientPublicKey) {
        try {
            var iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            var aesKey = deriveAesKey(clientPublicKey);
            var cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            var encrypted = cipher.doFinal(responseBody.getBytes(StandardCharsets.UTF_8));

            return new BaseEncryptedResponse(encoder.encodeToString(encrypted), encoder.encodeToString(iv));
        } catch (Exception e) {
            throw new IllegalStateException("Can not encrypt response", e);
        }
    }

    // P-256 ECDH produces a 32-byte shared secret matching AES-256 key size.
    // Compatible with Web Crypto API's deriveKey() which also uses the raw ECDH secret.
    private SecretKeySpec deriveAesKey(String clientPublicKey) throws Exception {
        var agreement = KeyAgreement.getInstance(ECDH_ALGORITHM);
        agreement.init(serverKeyPair.getPrivate());
        agreement.doPhase(readRawPublicKey(clientPublicKey), true);

        return new SecretKeySpec(agreement.generateSecret(), AES_ALGORITHM);
    }

    private PublicKey readRawPublicKey(String publicKey) throws Exception {
        var rawPublicKey = decoder.decode(publicKey);
        if (rawPublicKey.length != RAW_PUBLIC_KEY_LENGTH_BYTES || rawPublicKey[0] != 0x04) {
            throw new IllegalArgumentException("Invalid public key");
        }

        var serverPublicKey = (ECPublicKey) serverKeyPair.getPublic();
        var params = serverPublicKey.getParams();
        var x = new BigInteger(1, Arrays.copyOfRange(rawPublicKey, 1, 1 + COORDINATE_LENGTH_BYTES));
        var y = new BigInteger(1, Arrays.copyOfRange(rawPublicKey, 1 + COORDINATE_LENGTH_BYTES, RAW_PUBLIC_KEY_LENGTH_BYTES));
        var point = new ECPoint(x, y);

        return KeyFactory.getInstance(EC_ALGORITHM).generatePublic(new ECPublicKeySpec(point, params));
    }

    private byte[] toRawPublicKey(ECPublicKey publicKey) {
        var rawPublicKey = new byte[RAW_PUBLIC_KEY_LENGTH_BYTES];
        rawPublicKey[0] = 0x04;
        copyCoordinate(publicKey.getW().getAffineX(), rawPublicKey, 1);
        copyCoordinate(publicKey.getW().getAffineY(), rawPublicKey, 1 + COORDINATE_LENGTH_BYTES);

        return rawPublicKey;
    }

    private void copyCoordinate(BigInteger value, byte[] target, int offset) {
        var source = value.toByteArray();
        var sourceOffset = Math.max(0, source.length - COORDINATE_LENGTH_BYTES);
        var sourceLength = Math.min(source.length, COORDINATE_LENGTH_BYTES);
        var targetOffset = offset + COORDINATE_LENGTH_BYTES - sourceLength;

        System.arraycopy(source, sourceOffset, target, targetOffset, sourceLength);
    }
}
