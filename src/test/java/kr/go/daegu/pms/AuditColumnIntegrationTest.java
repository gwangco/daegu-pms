package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

/** MyBatis 인터셉터(AuditParameterInterceptor)가 감사 컬럼을 로그인 사용자·고정 Clock 기준으로 채우는지 검증 */
class AuditColumnIntegrationTest extends IntegrationTestSupport {

    /** IntegrationTestSupport 의 고정 Clock(2026-10-21T01:00Z)을 서울 시간으로 본 값 */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("심사 상태 변경: 수정자는 로그인한 담당자, 화면에서 보낸 updatedBy 는 무시")
    @WithUserDetails("manager1")
    void statusChangeRecordsManager() throws Exception {
        mockMvc.perform(post("/applications/3/status").with(csrf())
                        .param("status", "DOCUMENT_PASSED").param("updatedBy", "hacker"))
                .andExpect(flash().attributeExists("successMessage"));

        Map<String, Object> row = jdbc.queryForMap("SELECT updated_by, updated_at FROM application WHERE application_id = 3");
        assertThat(row.get("updated_by")).isEqualTo("manager1");
        assertThat(row.get("updated_at")).isEqualTo(NOW);   // MySQL 드라이버는 DATETIME 을 LocalDateTime 으로 준다
    }

    @Test
    @DisplayName("평가위원 배정은 배정한 담당자를, 채점은 평가위원을 기록한다")
    void evaluationAssignAndScore() throws Exception {
        mockMvc.perform(post("/applications/3/status").with(csrf()).with(user("manager1").roles("MANAGER"))
                .param("status", "DOCUMENT_PASSED"));
        mockMvc.perform(post("/applications/3/evaluators").with(csrf()).with(user("manager1").roles("MANAGER"))
                        .param("evaluatorUserId", "evaluator2"))
                .andExpect(flash().attributeExists("successMessage"));
        Long evaluationId = jdbc.queryForObject(
                "SELECT evaluation_id FROM evaluation WHERE application_id = 3 AND evaluator_user_id = 'evaluator2'", Long.class);
        assertThat(jdbc.queryForObject("SELECT created_by FROM evaluation WHERE evaluation_id = ?", String.class, evaluationId))
                .isEqualTo("manager1");

        mockMvc.perform(post("/evaluator/evaluations/" + evaluationId).with(csrf()).with(user("evaluator2").roles("EVALUATOR"))
                        .param("quantitativeScore", "30").param("qualitativeScore", "40").param("evalComment", "보완 필요"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(jdbc.queryForObject("SELECT updated_by FROM evaluation WHERE evaluation_id = ?", String.class, evaluationId))
                .isEqualTo("evaluator2");
    }

    @Test
    @DisplayName("정산 등록은 등록한 기업, 검수는 처리한 담당자를 기록한다")
    void settlementCreateAndReview() throws Exception {
        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "audit.pdf"))
                        .with(csrf()).with(user("company1").roles("COMPANY"))
                        .param("applicationId", "1").param("itemCode", "EXP02").param("expenseDate", "2026-10-20")
                        .param("vendorName", "감사 거래처").param("supplyAmount", "1000").param("description", "감사 컬럼 확인"))
                .andExpect(flash().attributeExists("successMessage"));
        Long settlementId = jdbc.queryForObject("SELECT settlement_id FROM settlement WHERE description = '감사 컬럼 확인'", Long.class);
        assertThat(jdbc.queryForObject("SELECT created_by FROM settlement WHERE settlement_id = ?", String.class, settlementId))
                .isEqualTo("company1");

        mockMvc.perform(post("/settlements/" + settlementId + "/review").with(csrf()).with(user("manager1").roles("MANAGER"))
                        .param("applicationId", "1").param("status", "APPROVED"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(jdbc.queryForObject("SELECT updated_by FROM settlement WHERE settlement_id = ?", String.class, settlementId))
                .isEqualTo("manager1");
    }
}
