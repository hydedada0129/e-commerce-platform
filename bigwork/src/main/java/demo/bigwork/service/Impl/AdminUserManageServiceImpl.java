package demo.bigwork.service.Impl;

import demo.bigwork.dao.OrderDAO;
import demo.bigwork.dao.ProductDAO;
import demo.bigwork.dao.UserDAO;
import demo.bigwork.model.enums.UserRole;
import demo.bigwork.model.po.OrderPO;
import demo.bigwork.model.po.ProductPO;
import demo.bigwork.model.po.UserPO;
import demo.bigwork.model.vo.AdminProductSummaryVO;
import demo.bigwork.model.vo.AdminUserSummaryVO;
import demo.bigwork.model.vo.OrderResponseVO;
import demo.bigwork.service.AdminUserManageService;
import demo.bigwork.service.UserSearchIndexService;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminUserManageServiceImpl implements AdminUserManageService {

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final UserSearchIndexService userSearchIndexService;

    public AdminUserManageServiceImpl(UserDAO userDAO,
                                      OrderDAO orderDAO,
                                      ProductDAO productDAO,
                                      UserSearchIndexService userSearchIndexService) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.userSearchIndexService = userSearchIndexService;
    }

    @Override
    public List<AdminUserSummaryVO> listUsersByRole(UserRole role, String keyword) {
        List<UserPO> users = fetchCandidates(role, keyword).stream()
                .filter(u -> matchesKeyword(u, keyword))
                .collect(Collectors.toList());
        // 依建立時間由新到舊排序（避免 null NPE）
        users.sort(Comparator.comparing(UserPO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return users.stream()
                .map(AdminUserSummaryVO::new)
                .collect(Collectors.toList());
    }

    /**
     * phone/defaultAddress are encrypted at rest so SQL can't filter by keyword directly.
     * For keywords long enough to tokenize (see UserSearchIndexService.MIN_TOKEN_SEARCH_LENGTH),
     * narrow via the blind-index token table first and only decrypt that (usually much smaller)
     * candidate set. Shorter keywords fall back to a full per-role scan.
     */
    private List<UserPO> fetchCandidates(UserRole role, String keyword) {
        if (keyword == null || keyword.isBlank()
                || keyword.trim().length() < UserSearchIndexService.MIN_TOKEN_SEARCH_LENGTH) {
            return userDAO.findByRole(role);
        }
        Set<Long> candidateIds = userSearchIndexService.findCandidateUserIds(keyword);
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        return userDAO.findAllById(candidateIds).stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    /** Final confirmation on decrypted plaintext — the token index only narrows candidates, it never guarantees a match. */
    private boolean matchesKeyword(UserPO user, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.toLowerCase();
        String phone = user.getPhone();
        String address = user.getDefaultAddress();
        return (phone != null && phone.toLowerCase().contains(needle))
                || (address != null && address.toLowerCase().contains(needle));
    }

    @Override
    public List<OrderResponseVO> listOrdersByBuyer(Long buyerId) {
        List<OrderPO> list = orderDAO.findByBuyer_UserId(buyerId);
        list.forEach(o -> Hibernate.initialize(o.getItems()));
        return list.stream()
                .map(OrderResponseVO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponseVO> listOrdersBySeller(Long sellerId) {
        List<OrderPO> list = orderDAO.findBySeller_UserId(sellerId);
        list.forEach(o -> Hibernate.initialize(o.getItems()));
        return list.stream()
                .map(OrderResponseVO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminProductSummaryVO> listProductsBySeller(Long sellerId) {
        List<ProductPO> list = productDAO.findBySeller_UserId(sellerId);
        return list.stream()
                .map(AdminProductSummaryVO::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUserByAdmin(Long userId) {
        try {
            userDAO.deleteById(userId);
        } catch (DataIntegrityViolationException ex) {
            // 有外鍵約束（還有訂單或商品），丟一個比較好懂的訊息給前端
            throw new IllegalStateException("該會員仍有相關訂單或商品，無法直接刪除", ex);
        }
    }
}