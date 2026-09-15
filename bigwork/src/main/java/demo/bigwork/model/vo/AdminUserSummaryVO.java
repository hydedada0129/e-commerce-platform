package demo.bigwork.model.vo;

import demo.bigwork.model.enums.UserRole;
import demo.bigwork.model.po.UserPO;
import demo.bigwork.util.MaskingUtil;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 後台「會員管理」列表用 VO
 * phone/defaultAddress 在此列表視圖中遮罩顯示（管理員瀏覽他人資料），
 * 與自助查詢（使用者查看自己的資料）顯示明碼的行為有意區分。
 */
@Data
public class AdminUserSummaryVO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String defaultAddress;
    private Timestamp createdAt;
    private UserRole role;

    public AdminUserSummaryVO(UserPO po) {
        this.userId = po.getUserId();
        this.name = po.getName();
        this.email = po.getEmail();
        this.phone = MaskingUtil.maskPhone(po.getPhone());
        this.defaultAddress = MaskingUtil.maskAddress(po.getDefaultAddress());
        this.createdAt = po.getCreatedAt();
        this.role = po.getRole();
    }
}