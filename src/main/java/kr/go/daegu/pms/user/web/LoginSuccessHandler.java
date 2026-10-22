package kr.go.daegu.pms.user.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.go.daegu.pms.user.service.LoginPolicyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 성공: 실패 횟수 초기화와 접속 기록 후, 로그인 전에 가려던 화면(없으면 메인)으로 이동한다.
 */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginPolicyService loginPolicyService;

    public LoginSuccessHandler(LoginPolicyService loginPolicyService) {
        this.loginPolicyService = loginPolicyService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        loginPolicyService.insertLoginSuccess(authentication.getName(), request.getRemoteAddr());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
