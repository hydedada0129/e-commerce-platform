package demo.bigwork.service;

import demo.bigwork.dao.OrderDAO;
import demo.bigwork.dao.ProductDAO;
import demo.bigwork.dao.UserDAO;
import demo.bigwork.model.enums.UserRole;
import demo.bigwork.model.po.UserPO;
import demo.bigwork.model.vo.AdminUserSummaryVO;
import demo.bigwork.service.Impl.AdminUserManageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Fields are encrypted at rest (see demo.bigwork.crypto), so `LIKE` can't run in SQL.
 * Keywords >= UserSearchIndexService.MIN_TOKEN_SEARCH_LENGTH narrow candidates via the
 * blind-index token table first (fewer rows to decrypt); shorter keywords fall back to
 * a full per-role decrypt-and-filter scan. Either way, matchesKeyword does the final
 * confirm on decrypted plaintext.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserManageServiceImplTest {

    @Mock
    private UserDAO userDAO;
    @Mock
    private OrderDAO orderDAO;
    @Mock
    private ProductDAO productDAO;
    @Mock
    private UserSearchIndexService userSearchIndexService;

    private AdminUserManageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserManageServiceImpl(userDAO, orderDAO, productDAO, userSearchIndexService);
    }

    @Test
    void listUsersByRole_blankKeyword_usesFullScan_returnsAllUsers() {
        when(userDAO.findByRole(UserRole.BUYER)).thenReturn(mutableListOf(
                user(1L, "0912345678", "台北市"),
                user(2L, "0987654321", "高雄市")));

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "");

        assertThat(result).hasSize(2);
    }

    @Test
    void listUsersByRole_shortKeyword_fallsBackToFullScan() {
        // "高雄" is 2 chars, below MIN_TOKEN_SEARCH_LENGTH -> full-scan fallback, no token lookup at all
        when(userDAO.findByRole(UserRole.BUYER)).thenReturn(mutableListOf(
                user(1L, "0912345678", "台北市"),
                user(2L, "0987654321", "高雄市")));

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "高雄");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        org.mockito.Mockito.verifyNoInteractions(userSearchIndexService);
    }

    @Test
    void listUsersByRole_shortKeyword_nullPhoneOrAddress_doesNotThrow() {
        // exercises matchesKeyword's null-check branch directly via the full-scan fallback path
        when(userDAO.findByRole(UserRole.ADMIN)).thenReturn(mutableListOf(
                user(1L, null, null)));

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.ADMIN, "AD");

        assertThat(result).isEmpty();
    }

    @Test
    void listUsersByRole_longKeyword_usesTokenIndexToNarrowCandidates() {
        // "0912" is 4 chars -> token-index path
        when(userSearchIndexService.findCandidateUserIds("0912")).thenReturn(Set.of(1L));
        when(userDAO.findAllById(Set.of(1L))).thenReturn(mutableListOf(
                user(1L, "0912345678", "台北市")));

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "0912");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        org.mockito.Mockito.verify(userDAO, org.mockito.Mockito.never()).findByRole(UserRole.BUYER);
    }

    @Test
    void listUsersByRole_longKeyword_noTokenCandidates_returnsEmptyWithoutDecrypting() {
        when(userSearchIndexService.findCandidateUserIds("0000")).thenReturn(Set.of());

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "0000");

        assertThat(result).isEmpty();
        org.mockito.Mockito.verify(userDAO, org.mockito.Mockito.never()).findAllById(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void listUsersByRole_longKeyword_candidateFromWrongRole_isExcluded() {
        // token index isn't role-aware, so the service must filter candidates by role itself
        when(userSearchIndexService.findCandidateUserIds("0912")).thenReturn(Set.of(1L));
        UserPO sellerWithMatchingPhone = user(1L, "0912345678", "台北市");
        sellerWithMatchingPhone.setRole(UserRole.SELLER);
        when(userDAO.findAllById(Set.of(1L))).thenReturn(mutableListOf(sellerWithMatchingPhone));

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "0912");

        assertThat(result).isEmpty();
    }

    @Test
    void listUsersByRole_longKeyword_tokenFalsePositive_excludedByFinalConfirm() {
        // token match is a narrowing step only — must still confirm the real substring match
        when(userSearchIndexService.findCandidateUserIds("091x")).thenReturn(Set.of(1L));
        when(userDAO.findAllById(Set.of(1L))).thenReturn(mutableListOf(
                user(1L, "0912345678", "台北市"))); // doesn't actually contain "091x"

        List<AdminUserSummaryVO> result = service.listUsersByRole(UserRole.BUYER, "091x");

        assertThat(result).isEmpty();
    }

    @SafeVarargs
    private static List<UserPO> mutableListOf(UserPO... users) {
        return new ArrayList<>(List.of(users));
    }

    private static UserPO user(Long id, String phone, String address) {
        UserPO po = new UserPO();
        po.setUserId(id);
        po.setName("user" + id);
        po.setPhone(phone);
        po.setDefaultAddress(address);
        po.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        po.setRole(UserRole.BUYER);
        return po;
    }
}
