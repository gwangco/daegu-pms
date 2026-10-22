package kr.go.daegu.pms.user.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.go.daegu.pms.user.service.LoginPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** 로그아웃 기록. 세션을 지우기 전에 실행되도록 다른 로그아웃 처리보다 먼저 등록한다 */
@Component
@RequiredArgsConstructor
public class LogoutHistoryHandler implements LogoutHandler {

    private final LoginPolicyService loginPolicyService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            loginPolicyService.insertLogout(authentication.getName(), request.getRemoteAddr());
        }
    }
}
