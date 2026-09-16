package demo.bigwork.crypto;

import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** For fields never queried by exact match: bank account number, phone, address. */
@Component
@Converter(autoApply = false)
public class RandomIvStringConverter extends AbstractEncryptedStringConverter {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RandomIvStringConverter(EncryptionKeyProvider keyProvider, AesGcmCipher cipher) {
        super(keyProvider, cipher);
    }

    @Override
    protected byte[] deriveIv(String plaintext) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }
}
