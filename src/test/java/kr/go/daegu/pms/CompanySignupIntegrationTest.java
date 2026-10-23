package kr.go.daegu.pms;

import kr.go.daegu.pms.cmm.crypto.PersonalInfoCipher;
import kr.go.daegu.pms.cmm.util.BusinessNoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기업회원 가입 → 승인 대기 → 총괄관리자 승인 → 로그인 (기준 시각은 IntegrationTestSupport 의 고정 Clock) */
class CompanySignupIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final String PASSWORD = "Dpms!2026";
    /** 검증번호가 맞는 가상의 번호 (계산은 BusinessNoUtilTest) */
    private static final String NEW_BUSINESS_NO = "123-45-67891";

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PersonalInfoCipher cipher;

    private static MockHttpServletRequestBuilder signup(String userId, String businessNo) {
        return post("/signup").with(csrf())
                .param("userId", userId)
                .param("newPassword", PASSWORD).param("newPasswordConfirm", PASSWORD)
                .param("companyName", "(주)달서테스트로보틱스")
                .param("businessNo", businessNo)
                .param("userName", "홍담당")
                .param("email", "ceo@newcorp.example.com")
                .param("phone", "01098765432")
                .param("agreeTerms", "true").param("agreePrivacy", "true");
    }

    private ResultActions login(String userId, String password) throws Exception {
        return mockMvc.perform(post("/login-process").with(csrf()).param("userId", userId).param("password", password));
    }

    private int users(String userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE user_id = ?", Integer.class, userId);
    }

    private Map<String, Object> row(String userId) {
        return jdbc.queryForMap("SELECT * FROM users WHERE user_id = ?", userId);
    }

    private static String task(String label, int count) {
        return "(?s).*" + label + "</span>\\s*<span class=\"task-count\"><strong>" + count + "</strong>.*";
    }

    @Test
    @DisplayName("가입 화면은 비로그인만: 필수 약관 두 가지와 입력칸을 보여 주고, 로그인한 사용자는 메인으로 보낸다")
    void signupPageForAnonymousOnly() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이용약관에 동의합니다. (필수)")))
                .andExpect(content().string(containsString("개인정보 수집·이용에 동의합니다. (필수)")))
                .andExpect(content().string(containsString("name=\"newPassword\"")));
        mockMvc.perform(get("/login")).andExpect(content().string(containsString("href=\"/signup\"")));

        mockMvc.perform(get("/signup").with(user("company1").roles("COMPANY"))).andExpect(redirectedUrl("/"));
        mockMvc.perform(signup("newcorp1", NEW_BUSINESS_NO).with(user("company1").roles("COMPANY")))
                .andExpect(redirectedUrl("/"));
        assertThat(users("newcorp1")).isZero();
    }

    @Test
    @DisplayName("가입: 기업회원·승인 대기로 저장, 비밀번호는 BCrypt, 이메일·연락처는 ARIA, 번호는 형식을 맞추고 동의 일시를 남긴다")
    void signupStoresPendingCompanyMember() throws Exception {
        mockMvc.perform(signup("newcorp1", "1234567891"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("signupDone", containsString("newcorp1")));

        Map<String, Object> member = row("newcorp1");
        assertThat(member.get("role")).isEqualTo("ROLE_COMPANY");
        assertThat(member.get("approval_status")).isEqualTo("REQUESTED");
        assertThat(member.get("enabled")).isEqualTo(true);
        assertThat(member.get("business_no")).isEqualTo(NEW_BUSINESS_NO);
        assertThat(member.get("company_business_no")).isEqualTo(NEW_BUSINESS_NO);   // 기업회원이라 생성 컬럼에 값이 생김
        assertThat(passwordEncoder.matches(PASSWORD, (String) member.get("password"))).isTrue();
        assertThat((String) member.get("password")).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        assertThat((String) member.get("email")).startsWith(PersonalInfoCipher.PREFIX);
        assertThat(cipher.decrypt((String) member.get("email"))).isEqualTo("ceo@newcorp.example.com");
        assertThat(cipher.decrypt((String) member.get("phone"))).isEqualTo("010-9876-5432");
        assertThat(member.get("terms_agreed_at")).isEqualTo(NOW);
        assertThat(member.get("privacy_agreed_at")).isEqualTo(NOW);
        assertThat(member.get("created_at")).isEqualTo(NOW);
        assertThat(member.get("approved_at")).isNull();

        mockMvc.perform(get("/login").flashAttr("signupDone", "기업회원 가입 신청을 받았습니다."))
                .andExpect(content().string(containsString("기업회원 가입 신청을 받았습니다.")));
    }

    @Test
    @DisplayName("승인 전에는 맞는 비밀번호로도 로그인할 수 없고(승인 대기 안내·기록), 총괄관리자가 승인하면 로그인된다")
    void pendingMemberLogsInOnlyAfterApproval() throws Exception {
        mockMvc.perform(signup("newcorp1", NEW_BUSINESS_NO)).andExpect(redirectedUrl("/login"));

        login("newcorp1", PASSWORD).andExpect(redirectedUrl("/login?error=pending")).andExpect(unauthenticated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM login_history WHERE user_id = 'newcorp1' AND result_code = 'PENDING'",
                Integer.class)).isEqualTo(1);
        assertThat(row("newcorp1").get("login_fail_count")).isEqualTo(0);   // 승인 대기는 비밀번호 실패로 세지 않는다
        mockMvc.perform(get("/login").param("error", "pending"))
                .andExpect(content().string(containsString("가입 승인을 기다리는 계정입니다.")));

        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(matchesPattern(task("가입 승인 대기", 1))));
        mockMvc.perform(post("/admin/users/newcorp1/approve").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users?pendingOnly=Y"))
                .andExpect(flash().attribute("successMessage", "newcorp1 계정의 가입을 승인했습니다."));

        Map<String, Object> member = row("newcorp1");
        assertThat(member.get("approval_status")).isEqualTo("APPROVED");
        assertThat(member.get("approved_by")).isEqualTo("admin");
        assertThat(member.get("approved_at")).isEqualTo(NOW);
        login("newcorp1", PASSWORD).andExpect(authenticated().withUsername("newcorp1").withRoles("COMPANY"));

        mockMvc.perform(get("/mypage").with(user("newcorp1").roles("COMPANY")))
                .andExpect(content().string(containsString("약관 동의")))
                .andExpect(content().string(containsString("123-45-67891")));
    }

    @Test
    @DisplayName("권한·승인 상태·동의 일시를 요청에 끼워 넣어도 기업회원·승인 대기로만 저장된다 (대량 할당)")
    void serverDecidesRoleAndApproval() throws Exception {
        mockMvc.perform(signup("newcorp2", NEW_BUSINESS_NO)
                        .param("role", "ROLE_ADMIN").param("approvalStatus", "APPROVED")
                        .param("enabled", "true").param("termsAgreedAt", "2020-01-01T00:00"))
                .andExpect(redirectedUrl("/login"));

        Map<String, Object> member = row("newcorp2");
        assertThat(member.get("role")).isEqualTo("ROLE_COMPANY");
        assertThat(member.get("approval_status")).isEqualTo("REQUESTED");
        assertThat(member.get("terms_agreed_at")).isEqualTo(NOW);
    }

    @Test
    @DisplayName("입력 규칙: 어긴 칸마다 안내하고 아무것도 저장하지 않으며, 입력한 비밀번호는 화면에 다시 내보내지 않는다")
    void rejectsInvalidInputPerField() throws Exception {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        String html = mockMvc.perform(post("/signup").with(csrf())
                        .param("userId", "Corp9")                                   // 대문자
                        .param("newPassword", "dpms!1234x").param("newPasswordConfirm", "dpms!1234x")   // 연속된 문자
                        .param("companyName", " ")
                        .param("businessNo", "123-45-67890")                         // 검증번호 오류
                        .param("userName", "홍담당")
                        .param("email", "ceo@example")
                        .param("phone", "12345")
                        .param("agreeTerms", "true"))                                // 개인정보 동의 없음
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(
                "아이디는 영문 소문자로 시작하는 영문 소문자·숫자 4~20자로 입력하세요.",
                "연속된 문자를 4자 이상 이어서 쓸 수 없습니다.",
                "기업명을 100자 이내로 입력하세요.",
                "사업자등록번호가 올바르지 않습니다.",
                "이메일을 영문 주소 형식으로",
                "연락처를 숫자로 입력하세요.",
                "개인정보 수집·이용에 동의해야 가입할 수 있습니다.");
        assertThat(html).doesNotContain("dpms!1234x", "이용약관에 동의해야 가입할 수 있습니다.");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(before);
    }

    @Test
    @DisplayName("아이디: 이미 있는 아이디와 관리자·담당자처럼 보이는 아이디는 쓸 수 없다")
    void userIdRules() throws Exception {
        mockMvc.perform(signup("company1", NEW_BUSINESS_NO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 사용 중인 아이디입니다.")));
        mockMvc.perform(signup("manager9", NEW_BUSINESS_NO))
                .andExpect(content().string(containsString("관리자·담당자 계정으로 보이는 아이디는 쓸 수 없습니다.")));
        assertThat(users("manager9")).isZero();
    }

    @Test
    @DisplayName("비밀번호: 아이디가 들어간 비밀번호, 확인 칸 불일치를 칸별로 알려 준다")
    void passwordRules() throws Exception {
        mockMvc.perform(signup("newcorp3", NEW_BUSINESS_NO)
                        .param("newPassword", "Newcorp3!x").param("newPasswordConfirm", "Newcorp3!x"))
                .andExpect(content().string(containsString("비밀번호에 아이디를 넣을 수 없습니다.")));
        mockMvc.perform(signup("newcorp3", NEW_BUSINESS_NO).param("newPasswordConfirm", "Dpms!2027"))
                .andExpect(content().string(containsString("비밀번호 확인이 비밀번호와 다릅니다.")));
        assertThat(users("newcorp3")).isZero();
    }

    @Test
    @DisplayName("한 기업은 기업회원 하나: 이미 가입된 번호는 하이픈 없이 넣어도 같은 번호로 보고 거부한다")
    void oneAccountPerBusinessNo() throws Exception {
        for (String businessNo : List.of("504-81-98761", "5048198761")) {
            mockMvc.perform(signup("newcorp4", businessNo))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("이미 가입된 사업자등록번호입니다.")));
        }
        assertThat(users("newcorp4")).isZero();
    }

    @Test
    @DisplayName("DB 도 기업회원끼리만 사업자등록번호 중복을 막는다 (같은 기관의 담당자는 번호가 같아도 된다)")
    void databaseUniqueOnlyAmongCompanies() {
        String insert = "INSERT INTO users (user_id, password, user_name, email, role, business_no) VALUES (?, 'x', '중복확인', 'a@example.com', ?, ?)";

        assertThatThrownBy(() -> jdbc.update(insert, "dupcorp", "ROLE_COMPANY", "504-81-98761"))
                .isInstanceOf(DuplicateKeyException.class);
        jdbc.update(insert, "staff91", "ROLE_MANAGER", "504-81-98761");
        jdbc.update(insert, "staff92", "ROLE_MANAGER", "504-81-98761");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE business_no = '504-81-98761'", Integer.class))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("승인: 총괄관리자만, 승인 대기만 골라 보고, 이미 승인된 계정은 다시 승인하지 않는다")
    void adminApproval() throws Exception {
        mockMvc.perform(signup("newcorp5", NEW_BUSINESS_NO)).andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/users").param("pendingOnly", "Y").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("newcorp5")))
                .andExpect(content().string(containsString("가입 승인</button>")))
                .andExpect(content().string(not(containsString(">company1<"))));
        mockMvc.perform(post("/admin/users/newcorp5/approve").with(user("manager1").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/company1/approve").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "승인 대기 중인 계정이 아닙니다."));
        assertThat(row("newcorp5").get("approval_status")).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("데모 기업회원 번호(V4_1)도 가입 화면과 같은 검증번호 규칙을 통과한다")
    void seedCompanyBusinessNumbersAreValid() {
        List<String> numbers = jdbc.queryForList("SELECT business_no FROM users WHERE role = 'ROLE_COMPANY'", String.class);
        assertThat(numbers).isNotEmpty().allMatch(BusinessNoUtil::isValid);
    }
}
