package demo.bigwork.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Derives independent subkeys from one master key via HMAC-SHA256, rather than
 * reusing a single key for multiple purposes (envelope/key-separation): data
 * encryption, IV derivation, and search-token hashing each get their own subkey.
 */
@Component
public class EncryptionKeyProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] DATA_KEY_CONTEXT = "bigwork-data-encryption-key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV_KEY_CONTEXT = "bigwork-iv-derivation-key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TOKEN_KEY_CONTEXT = "bigwork-token-search-key".getBytes(StandardCharsets.UTF_8);

    private final SecretKey dataKey;
    private final SecretKey ivHmacKey;
    private final SecretKey tokenKey;

    public EncryptionKeyProvider(@Value("${security.data-encryption-key}") String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalArgumentException("security.data-encryption-key must not be blank");
        }
        byte[] masterKeyBytes;
        try {
            masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("security.data-encryption-key must be valid Base64", e);
        }

        this.dataKey = new SecretKeySpec(hmac(masterKeyBytes, DATA_KEY_CONTEXT), "AES");
        this.ivHmacKey = new SecretKeySpec(hmac(masterKeyBytes, IV_KEY_CONTEXT), HMAC_ALGORITHM);
        this.tokenKey = new SecretKeySpec(hmac(masterKeyBytes, TOKEN_KEY_CONTEXT), HMAC_ALGORITHM);
    }

    public SecretKey getDataKey() {
        return dataKey;
    }

    public SecretKey getIvHmacKey() {
        return ivHmacKey;
    }

    public SecretKey getTokenKey() {
        return tokenKey;
    }

    private static byte[] hmac(byte[] keyBytes, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to derive encryption subkey", e);
        }
    }
}
