package demo.bigwork.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTokenHasherTest {

    private static final String MASTER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final SearchTokenHasher hasher = new SearchTokenHasher(new EncryptionKeyProvider(MASTER_KEY));

    @Test
    void hash_sameInput_isDeterministic() {
        assertThat(hasher.hash("091")).isEqualTo(hasher.hash("091"));
    }

    @Test
    void hash_differentInput_producesDifferentHash() {
        assertThat(hasher.hash("091")).isNotEqualTo(hasher.hash("912"));
    }

    @Test
    void hash_doesNotLeakPlaintext() {
        assertThat(hasher.hash("091")).doesNotContain("091");
    }

    @Test
    void hashAll_matchesTokenizerOutput() {
        Set<String> hashes = hasher.hashAll("0912345678");

        assertThat(hashes).hasSize(8); // 8 sliding trigrams, all distinct for this input
        assertThat(hashes).allMatch(h -> h.length() == 64); // hex-encoded SHA-256 = 64 chars
    }

    @Test
    void hashAll_blankInput_returnsEmptySet() {
        assertThat(hasher.hashAll(null)).isEmpty();
        assertThat(hasher.hashAll("")).isEmpty();
    }

    @Test
    void differentMasterKey_producesDifferentHash() {
        SearchTokenHasher other = new SearchTokenHasher(new EncryptionKeyProvider(
                Base64.getEncoder().encodeToString("different-master-key-material-32".getBytes())));

        assertThat(hasher.hash("091")).isNotEqualTo(other.hash("091"));
    }
}
