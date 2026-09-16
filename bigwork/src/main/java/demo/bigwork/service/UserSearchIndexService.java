package demo.bigwork.service;

import demo.bigwork.model.po.UserPO;

import java.util.Set;

/**
 * Maintains the blind-index (demo.bigwork.crypto.NGramTokenizer/SearchTokenHasher)
 * that lets admin search narrow candidates for encrypted phone/address without ever
 * running SQL LIKE against ciphertext. See CLAUDE.md's "Field-level encryption &
 * masking" section for the full design.
 */
public interface UserSearchIndexService {

    /** Below this keyword length, n-grams aren't meaningful — callers should fall back to a full decrypt-and-filter scan. */
    int MIN_TOKEN_SEARCH_LENGTH = 3;

    /** Regenerates this user's search tokens from their current (decrypted) phone/defaultAddress. Call after any write to either field. */
    void reindex(UserPO user);

    /** Candidate user IDs whose phone or defaultAddress *may* contain keyword. Always confirm with a real decrypt+contains check — this is a narrowing step, not a final answer. */
    Set<Long> findCandidateUserIds(String keyword);
}
