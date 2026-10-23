package kr.go.daegu.pms.config;

import kr.go.daegu.pms.user.web.LoginFailureHandler;
import kr.go.daegu.pms.user.web.LoginSuccessHandler;
import kr.go.daegu.pms.user.web.LogoutHistoryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 설정 (URL 단위 1차 인가).
 *
 * 인가는 3단계로 겹쳐서 건다 (Defense in Depth):
 *  1) URL 패턴 (이 클래스)
 *  2) 컨트롤러 메서드 @PreAuthorize
 *  3) 서비스 계층의 데이터 소유자 검사 (ApplicationAccessChecker) - IDOR 방지
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 콘텐츠 보안 정책(CSP). 스크립트·스타일·글꼴을 같은 출처(/webjars, /js, /css, /fonts)에서만 받는다.
     * 인라인 스크립트와 onclick 같은 인라인 이벤트도 막히므로, XSS 로 HTML 이 끼어들어도 스크립트가 실행되지 않는다.
     * img-src 의 data: 는 Bootstrap CSS 가 아이콘(select 화살표 등)을 data URI 로 쓰기 때문에 허용한다.
     */
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            // 다른 사이트(같은 출처 포함)가 이 화면을 iframe 으로 감싸지 못하게 한다 (클릭재킹 방지)
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder passwordEncoder() {
        // {bcrypt}, {noop} 등 접두어로 알고리즘을 구분하는 위임형 인코더. 신규 인코딩은 BCrypt 로 수행
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler loginSuccessHandler,
                                           LoginFailureHandler loginFailureHandler,
                                           LogoutHistoryHandler logoutHistoryHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 및 오류 페이지 (오류 페이지를 막으면 403/404 대신 로그인 화면으로 튕긴다)
                .requestMatchers("/css/**", "/js/**", "/fonts/**", "/images/**", "/webjars/**", "/favicon.svg", "/favicon.ico", "/error").permitAll()
                // 공고 등록은 담당자/관리자 (아래 공고 조회 허용 규칙보다 먼저 선언해야 함 - 위에서부터 순서대로 매칭)
                .requestMatchers("/announcements/new").hasAnyRole("MANAGER", "ADMIN")
                // 공지사항 등록/수정/삭제는 담당자/관리자 (아래 공지 조회 허용 규칙보다 먼저)
                .requestMatchers("/notices/new", "/notices/*/edit", "/notices/*/delete").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/faqs/new", "/faqs/*/edit", "/faqs/*/delete").hasAnyRole("MANAGER", "ADMIN")
                // 공개 페이지 (메인, 로그인, 기업회원 가입, 공고 목록/상세 조회)
                // 가입 화면은 로그인한 사용자가 열면 컨트롤러가 메인으로 돌려보낸다
                .requestMatchers("/", "/login", "/privacy", "/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/announcements", "/announcements/*").permitAll()
                // 공지 첨부는 공지처럼 누구나 받는다. 삭제된 공지의 파일인지는 FileService 가 다시 확인한다
                .requestMatchers(HttpMethod.GET, "/notices", "/notices/*", "/notices/*/files/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/faqs").permitAll()
                // 평가위원 전용 채점 화면
                .requestMatchers("/evaluator/**").hasRole("EVALUATOR")
                // 엑셀 다운로드 (개인정보·사업자번호 포함) 는 담당자/관리자만
                .requestMatchers("/excel/**").hasAnyRole("MANAGER", "ADMIN")
                // 사용자 관리·접속 기록은 총괄관리자만
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 그 외 모든 요청은 인증 필요 (세부 권한은 @PreAuthorize + 서비스 계층에서 검사)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login-process")
                .usernameParameter("userId")
                .passwordParameter("password")
                // 성공: 실패 횟수 초기화·접속 기록 후 원래 가려던 화면으로 / 실패: 횟수 증가·잠금 판단
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(logoutHistoryHandler)
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // CSRF 는 Spring Security 기본값(모든 POST 에 토큰 필수)을 그대로 쓴다.
            // MySQL 전환으로 H2 웹 콘솔을 없애면서 콘솔용 CSRF 예외·iframe 허용·CSP 예외도 함께 없앴다.
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
            );

        return http.build();
    }
}
