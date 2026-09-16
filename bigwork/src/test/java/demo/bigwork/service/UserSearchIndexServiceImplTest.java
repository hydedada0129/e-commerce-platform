package demo.bigwork.service;

import demo.bigwork.crypto.EncryptionKeyProvider;
import demo.bigwork.crypto.SearchTokenHasher;
import demo.bigwork.dao.UserSearchTokenDAO;
import demo.bigwork.model.enums.SearchableField;
import demo.bigwork.model.po.UserPO;
import demo.bigwork.model.po.UserSearchTokenPO;
import demo.bigwork.service.Impl.UserSearchIndexServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSearchIndexServiceImplTest {

    private static final String MASTER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Mock
    private UserSearchTokenDAO tokenDAO;

    private UserSearchIndexServiceImpl service;
    private SearchTokenHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new SearchTokenHasher(new EncryptionKeyProvider(MASTER_KEY));
        service = new UserSearchIndexServiceImpl(tokenDAO, hasher);
    }

    @Test
    void reindex_deletesOldTokensBeforeInsertingNew() {
        UserPO user = user(4L, "0912345678", "台北市");

        service.reindex(user);

        verify(tokenDAO, times(1)).deleteByUserId(4L);
        verify(tokenDAO, times(1)).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void reindex_insertsOneTokenPerDistinctTrigramPerField() {
        UserPO user = user(4L, "0912345678", "TW"); // phone: 8 distinct trigrams, address shorter than N -> 1 token

        ArgumentCaptor<List<UserSearchTokenPO>> captor = ArgumentCaptor.forClass(List.class);
        service.reindex(user);
        verify(tokenDAO).saveAll(captor.capture());

        List<UserSearchTokenPO> saved = captor.getValue();
        long phoneTokens = saved.stream().filter(t -> t.getField() == SearchableField.PHONE).count();
        long addressTokens = saved.stream().filter(t -> t.getField() == SearchableField.ADDRESS).count();

        assertThat(phoneTokens).isEqualTo(8);
        assertThat(addressTokens).isEqualTo(1);
        assertThat(saved).allMatch(t -> t.getUserId().equals(4L));
    }

    @Test
    void reindex_nullPhoneAndAddress_savesNoTokens() {
        UserPO user = user(17L, null, null);

        ArgumentCaptor<List<UserSearchTokenPO>> captor = ArgumentCaptor.forClass(List.class);
        service.reindex(user);
        verify(tokenDAO).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void findCandidateUserIds_combinesPhoneAndAddressMatches() {
        Set<String> phoneHashes = hasher.hashAll("0912345678");
        Set<String> addressHashes = hasher.hashAll("taipe");

        when(tokenDAO.findUserIdsByFieldAndTokenHashIn(org.mockito.ArgumentMatchers.eq(SearchableField.PHONE), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(4L, 10L));
        when(tokenDAO.findUserIdsByFieldAndTokenHashIn(org.mockito.ArgumentMatchers.eq(SearchableField.ADDRESS), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(4L));

        Set<Long> candidates = service.findCandidateUserIds("0912");

        assertThat(candidates).containsExactlyInAnyOrder(4L, 10L);
    }

    @Test
    void findCandidateUserIds_blankKeyword_returnsEmptySet() {
        assertThat(service.findCandidateUserIds("")).isEmpty();
        assertThat(service.findCandidateUserIds(null)).isEmpty();
    }

    private static UserPO user(Long id, String phone, String address) {
        UserPO po = new UserPO();
        po.setUserId(id);
        po.setPhone(phone);
        po.setDefaultAddress(address);
        return po;
    }
}
