package demo.bigwork.model.po;

import demo.bigwork.model.enums.SearchableField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PO (Entity) - 對應 `user_search_tokens` 資料表
 * 每一列存一個 n-gram 的 HMAC 雜湊值（blind index），支援對加密欄位做「模糊查詢」，
 * 詳見 demo.bigwork.crypto.NGramTokenizer / SearchTokenHasher 與 CLAUDE.md 的說明。
 */
@Entity
@Table(name = "user_search_tokens")
@Data
@NoArgsConstructor
public class UserSearchTokenPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 20)
    private SearchableField field;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    public UserSearchTokenPO(Long userId, SearchableField field, String tokenHash) {
        this.userId = userId;
        this.field = field;
        this.tokenHash = tokenHash;
    }
}
