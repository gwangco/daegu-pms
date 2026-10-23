package kr.go.daegu.pms.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /** 가입 승인 대기 (가입 화면으로 들어온 기업회원). 총괄관리자가 승인하기 전에는 로그인할 수 없다 */
    public static final String APPROVAL_REQUESTED = "REQUESTED";
    /** 승인됨 (관리자가 만든 계정은 처음부터 이 상태) */
    public static final String APPROVAL_APPROVED = "APPROVED";

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
    private String approvalStatus;
    private LocalDateTime approvedAt;
    private LocalDateTime termsAgreedAt;
    private LocalDateTime privacyAgreedAt;
    private int loginFailCount;
    private LocalDateTime lockedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /**
     * 로그인할 수 있는 승인 상태인가.
     * 값이 비어 있으면(조회 SQL 이 승인 상태를 빠뜨린 경우) 승인되지 않은 것으로 본다 — 실수하면 로그인이 막히는 쪽으로
     */
    public boolean isApproved() {
        return APPROVAL_APPROVED.equals(approvalStatus);
    }

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
