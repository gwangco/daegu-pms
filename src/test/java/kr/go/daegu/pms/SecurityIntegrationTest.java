package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증/인가 및 IDOR(파라미터 변조) 방어 검증
 */
class SecurityIntegrationTest extends IntegrationTestSupport {

    // ───────────── 인증 ─────────────

    @Test
    @DisplayName("BCrypt 로 저장된 비밀번호로 로그인 성공, 틀리면 실패")
    void loginWithBcrypt() throws Exception {
        mockMvc.perform(post("/login-process").with(csrf()).param("userId", "company1").param("password", "company1234"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/login-process").with(csrf()).param("userId", "company1").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("CSRF 토큰 없는 POST 는 403")
    @WithUserDetails("manager1")
    void csrfRequired() throws Exception {
        mockMvc.perform(post("/applications/3/status").param("status", "DOCUMENT_PASSED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비로그인: 공고 조회는 허용, 신청현황/공고등록은 로그인 화면으로")
    void anonymousAccess() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/announcements")).andExpect(status().isOk());
        mockMvc.perform(get("/announcements/13")).andExpect(status().isOk());
        mockMvc.perform(get("/applications")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/announcements/new")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("H2 콘솔은 총괄관리자 외 접근 불가")
    @WithUserDetails("manager1")
    void h2ConsoleAdminOnly() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isForbidden());
    }

    // ───────────── IDOR: 신청서 ─────────────

    @Test
    @DisplayName("기업은 본인 신청서만 열람 가능 (company2 → company1 신청서 403)")
    @WithUserDetails("company2")
    void companyCannotReadOthersApplication() throws Exception {
        mockMvc.perform(get("/applications/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/applications/2")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("기업 목록에는 본인 신청서만 표시 (companyUserId 파라미터 변조 무시)")
    @WithUserDetails("company2")
    void companyListScope() throws Exception {
        mockMvc.perform(get("/applications").param("companyUserId", "company1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("applications", hasSize(1)))
                .andExpect(content().string(containsString("북성로 공구골목")))
                .andExpect(content().string(not(containsString("도심형 PBV"))));
    }

    @Test
    @DisplayName("평가위원은 배정된 과제만 열람, 정산 화면은 접근 불가")
    @WithUserDetails("evaluator1")
    void evaluatorScope() throws Exception {
        mockMvc.perform(get("/applications/2")).andExpect(status().isOk());          // 배정됨
        mockMvc.perform(get("/applications/3")).andExpect(status().isForbidden());   // 미배정
        mockMvc.perform(get("/settlements/application/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("다른 기업의 정산 화면 접근 / 정산 등록 시도는 403")
    @WithUserDetails("company2")
    void settlementIdor() throws Exception {
        mockMvc.perform(get("/settlements/application/1")).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/settlements/register")
                        .file(pdf("receiptFile", "invoice.pdf"))
                        .param("applicationId", "1").param("itemCode", "EXP02").param("expenseDate", "2026-10-20")
                        .param("vendorName", "공격자상사").param("supplyAmount", "1000").param("description", "변조 시도")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("기업/평가위원은 엑셀 다운로드 불가, 담당자는 가능")
    @WithUserDetails("company1")
    void excelForbiddenForCompany() throws Exception {
        mockMvc.perform(get("/excel/applications")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("평가위원은 다른 평가위원의 평가 건에 접근할 수 없음")
    @WithUserDetails("evaluator1")
    void evaluatorCannotReadOthersEvaluation() throws Exception {
        mockMvc.perform(get("/evaluator/evaluations/4")).andExpect(status().isForbidden()); // evaluator2 의 건
        mockMvc.perform(get("/evaluator/evaluations/3")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("기업은 평가위원 화면에 접근할 수 없음")
    @WithUserDetails("company1")
    void companyCannotAccessEvaluatorPages() throws Exception {
        mockMvc.perform(get("/evaluator/evaluations")).andExpect(status().isForbidden());
    }

    // ───────────── 페이징 우회 차단 ─────────────

    @Test
    @DisplayName("recordCountPerPage=0 을 넣어도 페이징이 우회되지 않음 (한 페이지 10건)")
    void pagingCannotBeBypassed() throws Exception {
        mockMvc.perform(get("/announcements").param("recordCountPerPage", "0").param("firstIndex", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("announcements", hasSize(10)));
        mockMvc.perform(get("/announcements").param("pageIndex", "2"))
                .andExpect(model().attribute("announcements", hasSize(6)));
    }

    @Test
    @DisplayName("존재하지 않는 공고는 404, 잘못된 형식의 ID 는 400")
    void notFoundAndBadRequest() throws Exception {
        mockMvc.perform(get("/announcements/99999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/announcements/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시드 데이터의 접수중 공고는 4건")
    void seedCount() throws Exception {
        mockMvc.perform(get("/announcements").param("status", "OPEN"))
                .andExpect(model().attribute("announcements", hasSize(4)));
    }
}
