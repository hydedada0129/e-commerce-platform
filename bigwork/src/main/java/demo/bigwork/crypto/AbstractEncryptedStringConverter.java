package demo.bigwork.crypto;

import jakarta.persistence.AttributeConverter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Shared encrypt/decrypt plumbing for entity fields. Storage format is
 * base64(iv[12] || ciphertext+tag). Subclasses only decide how the IV is derived —
 * random (non-queryable fields) or deterministic (fields needing exact-match lookups).
 */
public abstract class AbstractEncryptedStringConverter implements AttributeConverter<String, String> {

    protected static final int IV_LENGTH_BYTES = 12;

    private final EncryptionKeyProvider keyProvider;
    private final AesGcmCipher cipher;

    protected AbstractEncryptedStringConverter(EncryptionKeyProvider keyProvider, AesGcmCipher cipher) {
        this.keyProvider = keyProvider;
        this.cipher = cipher;
    }

    protected abstract byte[] deriveIv(String plaintext);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        byte[] iv = deriveIv(attribute);
        byte[] ciphertext = cipher.encrypt(keyProvider.getDataKey(), iv, attribute.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        byte[] combined = Base64.getDecoder().decode(dbData);
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);
        try {
            byte[] plaintext = cipher.decrypt(keyProvider.getDataKey(), iv, ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt column value", e);
        }
    }
}
