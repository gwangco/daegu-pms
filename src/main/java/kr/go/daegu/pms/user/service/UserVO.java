package kr.go.daegu.pms.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private String userId;
    private String password;
    private String userName;
    private String email;
    private String phone;
    private String companyName;
    private String businessNo;
    private String role; // ROLE_COMPANY, ROLE_EVALUATOR, ROLE_MANAGER, ROLE_ADMIN
    private boolean enabled;
    private LocalDateTime createdAt;
}
