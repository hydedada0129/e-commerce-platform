package demo.bigwork.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RandomIvStringConverterTest {

    private static final String MASTER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private RandomIvStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RandomIvStringConverter(new EncryptionKeyProvider(MASTER_KEY), new AesGcmCipher());
    }

    @Test
    void roundTrip_returnsOriginalValue() {
        String plaintext = "0912345678";

        String stored = converter.convertToDatabaseColumn(plaintext);
        String recovered = converter.convertToEntityAttribute(stored);

        assertThat(recovered).isEqualTo(plaintext);
    }

    @Test
    void storedValue_isNotPlaintext() {
        String plaintext = "0912345678";

        String stored = converter.convertToDatabaseColumn(plaintext);

        assertThat(stored).doesNotContain(plaintext);
    }

    @Test
    void sameInput_producesDifferentCiphertextEachTime() {
        String plaintext = "0912345678";

        String stored1 = converter.convertToDatabaseColumn(plaintext);
        String stored2 = converter.convertToDatabaseColumn(plaintext);

        assertThat(stored1).isNotEqualTo(stored2);
    }

    @Test
    void nullAttribute_convertsToNullColumn() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void nullColumn_convertsToNullAttribute() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tamperedStoredValue_throwsOnDecrypt() {
        String stored = converter.convertToDatabaseColumn("0912345678");
        byte[] raw = Base64.getDecoder().decode(stored);
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(RuntimeException.class);
    }
}
