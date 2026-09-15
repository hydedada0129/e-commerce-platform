package demo.bigwork.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCipherTest {

    private final SecretKey key = randomAesKey();
    private final AesGcmCipher cipher = new AesGcmCipher();

    @Test
    void encryptThenDecrypt_returnsOriginalPlaintext() throws Exception {
        byte[] iv = randomIv();
        byte[] plaintext = "hello world".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = cipher.encrypt(key, iv, plaintext);
        byte[] decrypted = cipher.decrypt(key, iv, ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_emptyPlaintext_roundTrips() throws Exception {
        byte[] iv = randomIv();
        byte[] plaintext = new byte[0];

        byte[] ciphertext = cipher.encrypt(key, iv, plaintext);
        byte[] decrypted = cipher.decrypt(key, iv, ciphertext);

        assertThat(decrypted).isEmpty();
    }

    @Test
    void encrypt_withDifferentIv_producesDifferentCiphertext() {
        byte[] plaintext = "same plaintext".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext1 = cipher.encrypt(key, randomIv(), plaintext);
        byte[] ciphertext2 = cipher.encrypt(key, randomIv(), plaintext);

        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
    }

    @Test
    void encrypt_withSameIvAndKey_isDeterministic() {
        byte[] iv = randomIv();
        byte[] plaintext = "same plaintext".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext1 = cipher.encrypt(key, iv, plaintext);
        byte[] ciphertext2 = cipher.encrypt(key, iv, plaintext);

        assertThat(ciphertext1).isEqualTo(ciphertext2);
    }

    @Test
    void decrypt_withTamperedCiphertext_throwsAeadBadTagException() {
        byte[] iv = randomIv();
        byte[] ciphertext = cipher.encrypt(key, iv, "sensitive data".getBytes(StandardCharsets.UTF_8));
        ciphertext[0] ^= 0x01; // flip a bit

        assertThatThrownBy(() -> cipher.decrypt(key, iv, ciphertext))
                .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    void decrypt_withWrongKey_throwsAeadBadTagException() {
        byte[] iv = randomIv();
        byte[] ciphertext = cipher.encrypt(key, iv, "sensitive data".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> cipher.decrypt(randomAesKey(), iv, ciphertext))
                .isInstanceOf(AEADBadTagException.class);
    }

    private static SecretKey randomAesKey() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] randomIv() {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
