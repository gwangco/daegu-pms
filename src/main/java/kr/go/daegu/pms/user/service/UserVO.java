package kr.go.daegu.pms.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private String userId;
    @ToString.Exclude
    private String password;
    private String userName;
    private String email;
    private String phone;
    private String companyName;
    private String businessNo;
    private String role; // ROLE_COMPANY, ROLE_EVALUATOR, ROLE_MANAGER, ROLE_ADMIN
    private boolean enabled;
    private int loginFailCount;
    private LocalDateTime lockedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /** 화면 표시용 권한 이름 */
    public String getRoleLabel() {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case "ROLE_ADMIN" -> "총괄관리자";
            case "ROLE_MANAGER" -> "사업담당자";
            case "ROLE_EVALUATOR" -> "평가위원";
            case "ROLE_COMPANY" -> "기업회원";
            default -> role;
        };
    }
}
