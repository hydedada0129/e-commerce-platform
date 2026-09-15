package demo.bigwork.model.enums;

/**
 * Which encrypted field a UserSearchTokenPO row indexes. Kept separate per field so a
 * phone n-gram can never collide with an address n-gram in the token lookup.
 */
public enum SearchableField {
    PHONE,
    ADDRESS
}
