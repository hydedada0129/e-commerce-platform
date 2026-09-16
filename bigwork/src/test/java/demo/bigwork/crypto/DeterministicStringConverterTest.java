package demo.bigwork.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicStringConverterTest {

    private static final String MASTER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private DeterministicStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DeterministicStringConverter(new EncryptionKeyProvider(MASTER_KEY), new AesGcmCipher());
    }

    @Test
    void roundTrip_returnsOriginalValue() {
        String plaintext = "ADM002";

        String stored = converter.convertToDatabaseColumn(plaintext);
        String recovered = converter.convertToEntityAttribute(stored);

        assertThat(recovered).isEqualTo(plaintext);
    }

    @Test
    void sameInput_producesIdenticalCiphertextEachTime() {
        String plaintext = "ADM002";

        String stored1 = converter.convertToDatabaseColumn(plaintext);
        String stored2 = converter.convertToDatabaseColumn(plaintext);

        // Must be deterministic: this is what lets `WHERE admin_code = ?` and the
        // DB UNIQUE constraint keep working transparently against ciphertext.
        assertThat(stored1).isEqualTo(stored2);
    }

    @Test
    void differentInput_producesDifferentCiphertext() {
        String stored1 = converter.convertToDatabaseColumn("ADM002");
        String stored2 = converter.convertToDatabaseColumn("ADM003");

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
}
