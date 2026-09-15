package demo.bigwork.util;

/**
 * Masks PII for admin-facing list views only — self-service views (a user viewing
 * their own data) show plaintext; only when an admin browses other users' records
 * does this apply, per the decrypt-then-mask-by-role pattern.
 */
public final class MaskingUtil {

    private MaskingUtil() {
    }

    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() <= 6) {
            return "*".repeat(phone.length());
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    public static String maskAddress(String address) {
        if (address == null) {
            return null;
        }
        if (address.length() <= 3) {
            return "*".repeat(address.length());
        }
        return address.substring(0, 3) + "****";
    }
}
