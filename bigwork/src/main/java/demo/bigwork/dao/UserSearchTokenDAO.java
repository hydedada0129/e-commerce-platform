package demo.bigwork.dao;

import demo.bigwork.model.enums.SearchableField;
import demo.bigwork.model.po.UserSearchTokenPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserSearchTokenDAO extends JpaRepository<UserSearchTokenPO, Long> {

    void deleteByUserId(Long userId);

    @Query("SELECT DISTINCT t.userId FROM UserSearchTokenPO t WHERE t.field = :field AND t.tokenHash IN :hashes")
    List<Long> findUserIdsByFieldAndTokenHashIn(@Param("field") SearchableField field,
                                                 @Param("hashes") Collection<String> hashes);
}
