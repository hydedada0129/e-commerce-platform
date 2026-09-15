package demo.bigwork.crypto;

import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * For fields needing exact-match DB lookups / UNIQUE constraints (admin_code): IV is
 * derived from HMAC-SHA256(ivKey, plaintext), so the same plaintext always yields the
 * same ciphertext under a fixed key — `WHERE admin_code = ?` and the DB UNIQUE index
 * keep working transparently against ciphertext.
 */
@Component
@Converter(autoApply = false)
public class DeterministicStringConverter extends AbstractEncryptedStringConverter {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EncryptionKeyProvider keyProvider;

    public DeterministicStringConverter(EncryptionKeyProvider keyProvider, AesGcmCipher cipher) {
        super(keyProvider, cipher);
        this.keyProvider = keyProvider;
    }

    @Override
    protected byte[] deriveIv(String plaintext) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keyProvider.getIvHmacKey());
            byte[] digest = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(digest, IV_LENGTH_BYTES);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to derive deterministic IV", e);
        }
    }
}
