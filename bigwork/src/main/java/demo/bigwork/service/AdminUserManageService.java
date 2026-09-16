package demo.bigwork.service;

import demo.bigwork.model.enums.UserRole;
import demo.bigwork.model.vo.AdminProductSummaryVO;
import demo.bigwork.model.vo.AdminUserSummaryVO;
import demo.bigwork.model.vo.OrderResponseVO;

import java.util.List;

public interface AdminUserManageService {

    /**
     * @param keyword optional phone/address substring filter (case-insensitive).
     *                Fields are encrypted at rest, so this filters in-memory after
     *                decryption rather than via SQL LIKE. Null/blank = no filter.
     */
    List<AdminUserSummaryVO> listUsersByRole(UserRole role, String keyword);

    List<OrderResponseVO> listOrdersByBuyer(Long buyerId);

    List<OrderResponseVO> listOrdersBySeller(Long sellerId);

    List<AdminProductSummaryVO> listProductsBySeller(Long sellerId);

    void deleteUserByAdmin(Long userId);
}