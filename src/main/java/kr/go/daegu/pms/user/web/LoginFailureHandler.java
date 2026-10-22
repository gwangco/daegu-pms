package kr.go.daegu.pms.user.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.go.daegu.pms.user.service.LoginPolicyService;
import kr.go.daegu.pms.user.service.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 실패: 비밀번호 오류는 실패 횟수를 올리고, 잠긴·중지된 계정은 시도만 기록한다.
 * 화면 안내는 /login?error=... 코드로만 넘긴다 (예외 메시지를 그대로 보여주지 않음).
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    public static final String ERROR_BAD_CREDENTIALS = "true";
    public static final String ERROR_LOCKED = "locked";
    public static final String ERROR_DISABLED = "disabled";

    private final LoginPolicyService loginPolicyService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String loginId = request.getParameter("userId");
        String clientIp = request.getRemoteAddr();
        String error;
        if (exception instanceof LockedException) {
            loginPolicyService.insertBlockedLogin(loginId, clientIp, LoginResult.LOCKED);
            error = ERROR_LOCKED;
        } else if (exception instanceof DisabledException) {
            loginPolicyService.insertBlockedLogin(loginId, clientIp, LoginResult.DISABLED);
            error = ERROR_DISABLED;
        } else {
            boolean lockedNow = loginPolicyService.insertPasswordFailure(loginId, clientIp);
            error = lockedNow ? ERROR_LOCKED : ERROR_BAD_CREDENTIALS;
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + error);
    }
}
