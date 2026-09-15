package demo.bigwork.crypto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NGramTokenizerTest {

    @Test
    void tokenize_longString_returnsSlidingTrigrams() {
        Set<String> tokens = NGramTokenizer.tokenize("0912345678");

        assertThat(tokens).containsExactlyInAnyOrder(
                "091", "912", "123", "234", "345", "456", "567", "678");
    }

    @Test
    void tokenize_isCaseInsensitive() {
        assertThat(NGramTokenizer.tokenize("TaiPei")).isEqualTo(NGramTokenizer.tokenize("taipei"));
    }

    @Test
    void tokenize_shorterThanN_returnsWholeStringAsSingleToken() {
        Set<String> tokens = NGramTokenizer.tokenize("TW");

        assertThat(tokens).containsExactly("tw");
    }

    @Test
    void tokenize_exactlyN_returnsSingleToken() {
        Set<String> tokens = NGramTokenizer.tokenize("abc");

        assertThat(tokens).containsExactly("abc");
    }

    @Test
    void tokenize_nullOrBlank_returnsEmptySet() {
        assertThat(NGramTokenizer.tokenize(null)).isEmpty();
        assertThat(NGramTokenizer.tokenize("")).isEmpty();
    }

    @Test
    void tokenize_duplicateNgramsWithinString_deduplicated() {
        // "aaaa" -> "aaa","aaa","aaa" (3 overlapping windows, all identical)
        Set<String> tokens = NGramTokenizer.tokenize("aaaa");

        assertThat(tokens).containsExactly("aaa");
    }
}
