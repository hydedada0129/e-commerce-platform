package demo.bigwork.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionKeyProviderTest {

    private static final String VALID_MASTER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void dataKeyAndIvHmacKey_areDifferentKeys() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider(VALID_MASTER_KEY);

        assertThat(provider.getDataKey().getEncoded())
                .isNotEqualTo(provider.getIvHmacKey().getEncoded());
    }

    @Test
    void tokenKey_isDifferentFromDataAndIvKeys() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider(VALID_MASTER_KEY);

        assertThat(provider.getTokenKey().getEncoded())
                .isNotEqualTo(provider.getDataKey().getEncoded())
                .isNotEqualTo(provider.getIvHmacKey().getEncoded());
    }

    @Test
    void sameProviderInstance_derivesSameKeysEachCall() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider(VALID_MASTER_KEY);

        assertThat(provider.getDataKey().getEncoded()).isEqualTo(provider.getDataKey().getEncoded());
        assertThat(provider.getIvHmacKey().getEncoded()).isEqualTo(provider.getIvHmacKey().getEncoded());
    }

    @Test
    void differentMasterKeys_deriveDifferentDataKeys() {
        EncryptionKeyProvider provider1 = new EncryptionKeyProvider(VALID_MASTER_KEY);
        EncryptionKeyProvider provider2 = new EncryptionKeyProvider(
                Base64.getEncoder().encodeToString("different-master-key-material-32".getBytes()));

        assertThat(provider1.getDataKey().getEncoded())
                .isNotEqualTo(provider2.getDataKey().getEncoded());
    }

    @Test
    void dataKey_is256Bits() {
        EncryptionKeyProvider provider = new EncryptionKeyProvider(VALID_MASTER_KEY);

        assertThat(provider.getDataKey().getEncoded()).hasSize(32);
    }

    @Test
    void nullOrBlankMasterKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new EncryptionKeyProvider(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EncryptionKeyProvider("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonBase64MasterKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new EncryptionKeyProvider("not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
