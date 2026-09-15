package demo.bigwork.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HMAC-SHA256's each n-gram under its own derived subkey (never the data-encryption
 * key), producing a blind index: a hash reveals nothing about the plaintext, but the
 * same n-gram always hashes the same way, so DB-level equality lookups on the hash
 * narrow candidates without ever exposing plaintext to SQL.
 */
@Component
public class SearchTokenHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EncryptionKeyProvider keyProvider;

    public SearchTokenHasher(EncryptionKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String hash(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keyProvider.getTokenKey());
            byte[] digest = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to hash search token", e);
        }
    }

    public Set<String> hashAll(String plaintext) {
        return NGramTokenizer.tokenize(plaintext).stream()
                .map(this::hash)
                .collect(Collectors.toSet());
    }
}
