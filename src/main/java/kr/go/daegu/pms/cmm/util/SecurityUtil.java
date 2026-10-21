package kr.go.daegu.pms.cmm.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 로그인 사용자 정보를 조회하는 정적 유틸리티.
 * (전자정부 프레임워크의 EgovUserDetailsHelper.getAuthenticatedUser() 와 같은 역할)
 *
 * 서비스 계층에서 "이 데이터의 소유자가 현재 로그인 사용자인가?"를 재검증할 때 사용한다.
 * 화면(버튼 숨김)이나 URL 권한만으로는 파라미터 변조(IDOR)를 막을 수 없기 때문이다.
 */
public final class SecurityUtil {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_EVALUATOR = "ROLE_EVALUATOR";
    public static final String ROLE_COMPANY = "ROLE_COMPANY";

    private SecurityUtil() {
    }

    /** 로그인 사용자 ID. 비로그인이면 null */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    /** 사업담당자 또는 총괄관리자 (모든 과제 열람 가능한 내부 직원) */
    public static boolean isStaff() {
        return hasRole(ROLE_MANAGER) || hasRole(ROLE_ADMIN);
    }
}
