package kr.go.daegu.pms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        // {bcrypt}, {noop} 등 접두어로 알고리즘을 구분하는 위임형 인코더. 신규 인코딩은 BCrypt 로 수행
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 및 오류 페이지 (오류 페이지를 막으면 403/404 대신 로그인 화면으로 튕긴다)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                // H2 웹 콘솔: 임의 SQL 실행이 가능한 관리 도구이므로 총괄관리자만 (local 프로필에서만 활성화됨)
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                // 공고 등록은 담당자/관리자 (아래 공고 조회 허용 규칙보다 먼저 선언해야 함 - 위에서부터 순서대로 매칭)
                .requestMatchers("/announcements/new").hasAnyRole("MANAGER", "ADMIN")
                // 공지사항 등록/수정/삭제는 담당자/관리자 (아래 공지 조회 허용 규칙보다 먼저)
                .requestMatchers("/notices/new", "/notices/*/edit", "/notices/*/delete").hasAnyRole("MANAGER", "ADMIN")
                // 공개 페이지 (메인, 로그인, 공고 목록/상세 조회)
                .requestMatchers("/", "/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/announcements", "/announcements/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/notices", "/notices/*").permitAll()
                // 평가위원 전용 채점 화면
                .requestMatchers("/evaluator/**").hasRole("EVALUATOR")
                // 엑셀 다운로드 (개인정보·사업자번호 포함) 는 담당자/관리자만
                .requestMatchers("/excel/**").hasAnyRole("MANAGER", "ADMIN")
                // 그 외 모든 요청은 인증 필요 (세부 권한은 @PreAuthorize + 서비스 계층에서 검사)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login-process")
                .usernameParameter("userId")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                // H2 콘솔은 자체 폼을 사용해 CSRF 토큰을 보낼 수 없으므로 예외 (ADMIN 전용 + local 전용)
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers
                // 기본값 DENY → 같은 출처 iframe 만 허용 (H2 콘솔 화면이 iframe 구조)
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
