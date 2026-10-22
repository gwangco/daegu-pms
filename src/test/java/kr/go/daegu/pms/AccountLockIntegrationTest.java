package kr.go.daegu.pms;

import kr.go.daegu.pms.user.service.UserManageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그인 5회 실패 잠금, 잠금 해제, 접속 기록 검증 (기준 시각은 IntegrationTestSupport 의 고정 Clock) */
class AccountLockIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired UserManageService userManageService;

    private ResultActions login(String userId, String password) throws Exception {
        return mockMvc.perform(post("/login-process").with(csrf())
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                })
                .param("userId", userId).param("password", password));
    }

    private int history(String userId, String resultCode) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM login_history WHERE user_id = ? AND result_code = ?",
                Integer.class, userId, resultCode);
    }

    private Map<String, Object> lockState(String userId) {
        return jdbc.queryForMap("SELECT login_fail_count, locked_at, last_login_at, updated_by FROM users WHERE user_id = ?", userId);
    }

    @Test
    @DisplayName("비밀번호를 5회 연속 틀리면 잠기고, 잠긴 동안은 맞는 비밀번호도 거부한다")
    void lockAfterFiveFailures() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login("company2", "wrong-" + i).andExpect(redirectedUrl("/login?error=true"));
        }
        login("company2", "wrong-5").andExpect(redirectedUrl("/login?error=locked"));
        login("company2", "company1234").andExpect(redirectedUrl("/login?error=locked"));

        Map<String, Object> state = lockState("company2");
        assertThat(state.get("login_fail_count")).isEqualTo(5);
        assertThat(state.get("locked_at")).isEqualTo(NOW);   // MySQL 드라이버는 DATETIME 을 LocalDateTime 으로 준다
        assertThat(history("company2", "FAIL_PASSWORD")).isEqualTo(5);
        assertThat(history("company2", "LOCKED")).isEqualTo(1);
        assertThat(history("company2", "SUCCESS")).isZero();

        mockMvc.perform(get("/login").param("error", "locked"))
                .andExpect(content().string(containsString("5회 연속 잘못 입력해 계정이 잠겼습니다. 30분 뒤")));
    }

    @Test
    @DisplayName("한도 전에 로그인에 성공하면 실패 횟수를 0으로 되돌리고 최근 로그인·접속 IP 를 남긴다")
    void successResetsFailureCount() throws Exception {
        login("company2", "wrong").andExpect(redirectedUrl("/login?error=true"));
        login("company2", "wrong").andExpect(redirectedUrl("/login?error=true"));
        login("company2", "company1234").andExpect(redirectedUrl("/"));

        Map<String, Object> state = lockState("company2");
        assertThat(state.get("login_fail_count")).isEqualTo(0);
        assertThat(state.get("last_login_at")).isEqualTo(NOW);
        assertThat(jdbc.queryForObject("SELECT client_ip FROM login_history WHERE user_id = 'company2' AND result_code = 'SUCCESS'", String.class))
                .isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("잠금 시간(30분)이 지나면 다시 로그인할 수 있고, 다음 실패는 1회부터 센다")
    void lockExpires() throws Exception {
        jdbc.update("UPDATE users SET login_fail_count = 5, locked_at = ? WHERE user_id = 'company2'",
                Timestamp.valueOf(NOW.minusMinutes(31)));

        login("company2", "wrong").andExpect(redirectedUrl("/login?error=true"));
        assertThat(lockState("company2").get("login_fail_count")).isEqualTo(1);
        assertThat(lockState("company2").get("locked_at")).isNull();

        login("company2", "company1234").andExpect(redirectedUrl("/"));
        assertThat(lockState("company2").get("login_fail_count")).isEqualTo(0);
    }

    @Test
    @DisplayName("없는 아이디는 같은 안내 문구로 처리하고, 입력한 아이디 원문은 접속 기록에 남기지 않는다")
    void unknownIdIsNotStored() throws Exception {
        String typedByMistake = "MyP@ssw0rd!";
        login(typedByMistake, "whatever").andExpect(redirectedUrl("/login?error=true"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_history WHERE result_code = 'FAIL_UNKNOWN_ID' AND user_id IS NULL", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_history WHERE user_id = ?", Integer.class, typedByMistake))
                .isZero();
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(content().string(containsString("아이디 또는 비밀번호가 일치하지 않습니다.")));
    }

    @Test
    @DisplayName("총괄관리자는 잠긴 계정을 찾아 해제할 수 있고, 해제한 사람이 감사 컬럼에 남는다")
    void adminUnlocks() throws Exception {
        jdbc.update("UPDATE users SET login_fail_count = 5, locked_at = ? WHERE user_id = 'company2'", Timestamp.valueOf(NOW));

        mockMvc.perform(get("/admin/users").param("lockedOnly", "Y").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("company2")))
                .andExpect(content().string(containsString("10:30</span> 해제 예정")))
                .andExpect(content().string(not(containsString(">company1<"))));

        mockMvc.perform(post("/admin/users/company2/unlock").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage", "계정 잠금을 해제했습니다."));

        Map<String, Object> state = lockState("company2");
        assertThat(state.get("locked_at")).isNull();
        assertThat(state.get("login_fail_count")).isEqualTo(0);
        assertThat(state.get("updated_by")).isEqualTo("admin");
        login("company2", "company1234").andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("사용자 관리는 총괄관리자 전용 (URL·서비스 모두 차단)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void adminOnly() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/login-history")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/company2/unlock").with(csrf())).andExpect(status().isForbidden());
        assertThatThrownBy(() -> userManageService.updateUserUnlock("company2")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("사용자 목록의 연락처·이메일은 서버에서 가린 값만 내려간다")
    void contactsAreMasked() throws Exception {
        mockMvc.perform(get("/admin/users").param("searchKeyword", "company1").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("010-****-4001")))
                .andExpect(content().string(containsString("ce*@mobility.example.com")))
                .andExpect(content().string(not(containsString("010-0000-4001"))))
                .andExpect(content().string(not(containsString("ceo@mobility.example.com"))));
    }

    @Test
    @DisplayName("접속 기록 화면에서 로그인·로그아웃 이력을 조회한다")
    void loginHistoryPage() throws Exception {
        login("company1", "company1234").andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/logout").with(csrf()).with(user("company1").roles("COMPANY")));
        assertThat(history("company1", "LOGOUT")).isEqualTo(1);

        mockMvc.perform(get("/admin/login-history").param("searchKeyword", "company1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("203.0.113.7")))
                .andExpect(content().string(containsString("로그아웃")));
    }
}
