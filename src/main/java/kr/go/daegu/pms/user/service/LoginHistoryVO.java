package kr.go.daegu.pms.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 접속 기록 (login_history) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryVO {
    private Long historyId;
    private String userId;       // 없는 아이디로 시도한 경우 null
    private String userName;     // 조인용
    private String resultCode;
    private String clientIp;
    private LocalDateTime loginAt;

    public String getResultLabel() {
        return LoginResult.labelOf(resultCode);
    }

    public boolean isFailure() {
        return !LoginResult.SUCCESS.name().equals(resultCode) && !LoginResult.LOGOUT.name().equals(resultCode);
    }
}
