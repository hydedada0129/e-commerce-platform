package demo.bigwork.crypto;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Splits a string into sliding-window trigrams for searchable-encryption indexing.
 * Pure string logic — no key material, no I/O — kept separate from {@link SearchTokenHasher}
 * so tokenizing rules can be tested without any crypto setup.
 */
public final class NGramTokenizer {

    private static final int N = 3;

    private NGramTokenizer() {
    }

    public static Set<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        String normalized = value.toLowerCase();
        Set<String> ngrams = new LinkedHashSet<>();
        if (normalized.length() <= N) {
            ngrams.add(normalized);
            return ngrams;
        }
        for (int i = 0; i <= normalized.length() - N; i++) {
            ngrams.add(normalized.substring(i, i + N));
        }
        return ngrams;
    }
}
