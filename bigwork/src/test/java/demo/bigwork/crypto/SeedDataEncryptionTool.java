package demo.bigwork.crypto;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Not a real test — a manual one-off tool to (re)generate ciphertext for the
 * SQL/*.sql seed dumps whenever DATA_ENCRYPTION_KEY (the documented local-dev key,
 * see CLAUDE.md) or the seed plaintext values change. Run explicitly via:
 *   ./mvnw test -Dtest=SeedDataEncryptionTool -Dsurefire.failIfNoSpecifiedTests=false
 * then paste the printed values into the relevant SQL/*.sql INSERT statements.
 *
 * Encrypts per-row (even for repeated plaintext, e.g. the same phone number across
 * multiple seed users) rather than once per distinct value, since random-IV
 * encryption is deliberately chosen so equal plaintexts do NOT produce equal
 * ciphertext — reusing one ciphertext across rows would defeat that property.
 */
@Disabled("Manual migration tool, not part of the automated suite")
class SeedDataEncryptionTool {

    private static final String LOCAL_DEV_KEY = "1AVbxqZzcA/iddvq+xr5rVEWJ61bADcOvmCJMGuChus=";

    @Test
    void printSeedCiphertext() {
        EncryptionKeyProvider keyProvider = new EncryptionKeyProvider(LOCAL_DEV_KEY);
        AesGcmCipher cipher = new AesGcmCipher();
        RandomIvStringConverter random = new RandomIvStringConverter(keyProvider, cipher);
        DeterministicStringConverter deterministic = new DeterministicStringConverter(keyProvider, cipher);

        System.out.println("=== users (in original INSERT order) ===");
        printUserRow(random, deterministic, 3, "0912345678", "高雄市", null);
        printUserRow(random, deterministic, 4, "0912345678", "taipe", null);
        printUserRow(random, deterministic, 9, "0912345678", "taipei", null);
        printUserRow(random, deterministic, 10, "0912345678", "台中", null);
        printUserRow(random, deterministic, 11, "0912345678", "花蓮", null);
        printUserRow(random, deterministic, 16, "0912345678", "台南", "");
        printUserRow(random, deterministic, 17, null, null, "ADM002");
        printUserRow(random, deterministic, 18, "0987654321", "TW", "ADM003");

        System.out.println("=== bank_accounts (in original INSERT order) ===");
        printBankAccountRow(random, 1, "88889999");
        printBankAccountRow(random, 2, "77778888");
    }

    @Test
    void printSearchTokenSqlInserts() {
        SearchTokenHasher hasher = new SearchTokenHasher(new EncryptionKeyProvider(LOCAL_DEV_KEY));

        StringBuilder sql = new StringBuilder();
        printUserTokens(hasher, sql, 3, "0912345678", "高雄市");
        printUserTokens(hasher, sql, 4, "0912345678", "taipe");
        printUserTokens(hasher, sql, 9, "0912345678", "taipei");
        printUserTokens(hasher, sql, 10, "0912345678", "台中");
        printUserTokens(hasher, sql, 11, "0912345678", "花蓮");
        printUserTokens(hasher, sql, 16, "0912345678", "台南");
        printUserTokens(hasher, sql, 17, null, null);
        printUserTokens(hasher, sql, 18, "0987654321", "TW");

        // trim trailing ",\n"
        String values = sql.substring(0, sql.length() - 2);
        System.out.println("INSERT INTO `user_search_tokens` (`user_id`, `field`, `token_hash`) VALUES\n" + values + ";");
    }

    private static void printUserTokens(SearchTokenHasher hasher, StringBuilder sql, long userId, String phone, String address) {
        for (String hash : orderedHashes(hasher, phone)) {
            sql.append("(").append(userId).append(",'PHONE','").append(hash).append("'),\n");
        }
        for (String hash : orderedHashes(hasher, address)) {
            sql.append("(").append(userId).append(",'ADDRESS','").append(hash).append("'),\n");
        }
    }

    private static Set<String> orderedHashes(SearchTokenHasher hasher, String plaintext) {
        return hasher.hashAll(plaintext);
    }

    private static void printUserRow(RandomIvStringConverter random, DeterministicStringConverter deterministic,
                                      long userId, String phone, String address, String adminCode) {
        String phoneCt = phone == null ? "NULL" : "'" + random.convertToDatabaseColumn(phone) + "'";
        String addressCt = address == null ? "NULL" : "'" + random.convertToDatabaseColumn(address) + "'";
        String adminCodeCt = adminCode == null ? "NULL" : "'" + deterministic.convertToDatabaseColumn(adminCode) + "'";
        System.out.println("user_id=" + userId + " phone=" + phoneCt + " default_address=" + addressCt + " admin_code=" + adminCodeCt);
    }

    private static void printBankAccountRow(RandomIvStringConverter random, long accountId, String accountNumber) {
        System.out.println("account_id=" + accountId + " account_number='" + random.convertToDatabaseColumn(accountNumber) + "'");
    }
}
