package kr.go.daegu.pms;

import kr.go.daegu.pms.faq.service.FaqService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 자주 묻는 질문 (전자정부 공통컴포넌트 FAQ uss.olh.faq 로 전환) 검증.
 * 코드 생성기 원본과 공통컴포넌트 원본에서 고친 부분(권한 없음, 작성자 위조, 첨부파일 ID 연결, 물리 삭제,
 * 숫자 검색조건 오류, 분류 미검증)이 다시 생기지 않는지 확인한다.
 */
class FaqIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);
    /** seed FAQ ID (공통컴포넌트 형식: FAQ_ + 16자리) */
    private static final String FAQ_1 = "FAQ_0000000000000001";
    private static final String FAQ_2 = "FAQ_0000000000000002";
    private static final String FAQ_7 = "FAQ_0000000000000007";

    @Autowired FaqService faqService;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("누구나 목록 조회: 분류 순서 → 표시 순서로 나오고 분류 탭과 검색어(질문·답변)로 좁힌다")
    void readableAndFilterable() throws Exception {
        String html = mockMvc.perform(get("/faqs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("한 공고에 여러 과제를")).isLessThan(html.indexOf("접수번호는 언제"));
        assertThat(html.indexOf("접수번호는 언제")).isLessThan(html.indexOf("심사 진행 상황은"));
        assertThat(html.indexOf("부가세는 어떻게")).isLessThan(html.indexOf("비밀번호를 여러 번"));

        mockMvc.perform(get("/faqs").param("faqClCode", "SETTLE"))
                .andExpect(content().string(containsString("부가세는 어떻게 계산되나요?")))
                .andExpect(content().string(not(containsString("접수번호는 언제 발급되나요?"))))
                .andExpect(content().string(containsString("aria-current=\"page\" class=\"active\">사업비 정산</a>")));

        // "30분"은 답변에만 있는 말이다
        mockMvc.perform(get("/faqs").param("searchWrd", "30분"))
                .andExpect(content().string(containsString("비밀번호를 여러 번 틀려")))
                .andExpect(content().string(not(containsString("부가세는 어떻게"))));
    }

    @Test
    @DisplayName("원본(생성기·공통컴포넌트)은 검색 조건을 숫자와 비교해 숫자가 아닌 값이면 500 이었다 → 이제 정상 조회")
    void textSearchConditionDoesNotBreakQuery() throws Exception {
        mockMvc.perform(get("/faqs").param("searchCnd", "title").param("searchWrd", "접수번호"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("접수번호는 언제 발급되나요?")));
    }

    @Test
    @DisplayName("비로그인·기업회원은 등록·삭제 화면과 요청이 막힌다")
    void nonStaffCannotWrite() throws Exception {
        mockMvc.perform(get("/faqs/new")).andExpect(status().is3xxRedirection());   // 로그인 화면으로
        mockMvc.perform(get("/faqs/new").with(user("company1").roles("COMPANY"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/faqs/" + FAQ_1 + "/delete").with(csrf()).with(user("company1").roles("COMPANY")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/faqs/" + FAQ_1 + "/delete").with(user("manager1").roles("MANAGER")))
                .andExpect(status().isForbidden());   // CSRF 토큰 없는 POST
    }

    @Test
    @WithMockUser(username = "company1", roles = "COMPANY")
    @DisplayName("화면을 거치지 않고 서비스를 직접 호출해도 담당자가 아니면 거부한다")
    void serviceRejectsNonStaff() {
        assertThatThrownBy(() -> faqService.deleteFaq(FAQ_1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("FAQ는 사업담당자만 등록·수정·삭제할 수 있습니다.");
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("담당자 등록: ID 는 공통컴포넌트 채번(FAQ_ + 16자리), ID·작성자·사용여부·첨부파일 변조는 무시, 저장한 질문을 펼쳐 보여 준다")
    void createByManager() throws Exception {
        MvcResult result = mockMvc.perform(post("/faqs/new").with(csrf())
                        .param("faqClCode", "ACCOUNT").param("sortOrdr", "2")
                        .param("qestnSj", "비밀번호는 어떻게 바꾸나요?")
                        .param("answerCn", "<script>alert('xss')</script>\n둘째 줄")
                        .param("faqId", "FAQ_HACKED").param("frstRegisterId", "admin")
                        .param("useAt", "N").param("atchFileId", "FILE_000000000000001"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String faqId = (String) result.getFlashMap().get("openFaqId");
        assertThat(faqId).matches("FAQ_\\d{16}");
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/faqs?faqClCode=ACCOUNT#faq-" + faqId);

        Map<String, Object> saved = jdbc.queryForMap(
                "SELECT FRST_REGISTER_ID, FRST_REGIST_PNTTM, USE_AT, ATCH_FILE_ID FROM COMTNFAQINFO WHERE FAQ_ID = ?", faqId);
        assertThat(saved.get("FRST_REGISTER_ID")).isEqualTo("manager1");
        assertThat(saved.get("FRST_REGIST_PNTTM")).isEqualTo(NOW);   // 고정 Clock(KST)
        assertThat(saved.get("USE_AT")).isEqualTo("Y");
        assertThat(saved.get("ATCH_FILE_ID")).isNull();              // 남의 첨부파일을 FAQ 에 연결할 수 없다

        mockMvc.perform(get("/faqs").param("faqClCode", "ACCOUNT").flashAttr("openFaqId", faqId))
                .andExpect(content().string(containsString("id=\"faq-" + faqId + "\" class=\"faq-item\" open=\"open\"")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert('xss')</script>"))));
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("수정: 분류·질문·답변·표시 순서만 바뀌고 작성자는 유지, 수정자·수정일시 기록, 대상은 URL 의 ID")
    void update() throws Exception {
        mockMvc.perform(post("/faqs/" + FAQ_7 + "/edit").with(csrf())
                        .param("faqId", FAQ_1)
                        .param("faqClCode", "SETTLE").param("sortOrdr", "5")
                        .param("qestnSj", "부가세 계산 방법").param("answerCn", "공급가액의 10%입니다.")
                        .param("frstRegisterId", "company1"))
                .andExpect(redirectedUrl("/faqs?faqClCode=SETTLE#faq-" + FAQ_7));

        Map<String, Object> updated = jdbc.queryForMap(
                "SELECT QESTN_SJ, SORT_ORDR, FRST_REGISTER_ID, LAST_UPDUSR_ID, LAST_UPDT_PNTTM FROM COMTNFAQINFO WHERE FAQ_ID = ?", FAQ_7);
        assertThat(updated.get("QESTN_SJ")).isEqualTo("부가세 계산 방법");
        assertThat(((Number) updated.get("SORT_ORDR")).intValue()).isEqualTo(5);
        assertThat(updated.get("FRST_REGISTER_ID")).isEqualTo("manager1");
        assertThat(updated.get("LAST_UPDUSR_ID")).isEqualTo("admin");
        assertThat(updated.get("LAST_UPDT_PNTTM")).isEqualTo(NOW);
        assertThat(faqService.selectFaq(FAQ_1).getQestnSj()).isEqualTo("한 공고에 여러 과제를 신청할 수 있나요?");
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("사용 안 함으로 바꾼 분류는 탭에서 빠지고, 그 분류나 없는 분류로는 저장할 수 없다 (입력값은 유지)")
    void invalidCategoryRejected() throws Exception {
        jdbc.update("UPDATE COMTCCMMNDETAILCODE SET USE_AT = 'N' WHERE CODE_ID = 'DPM005' AND CODE = 'ACCOUNT'");
        mockMvc.perform(get("/faqs")).andExpect(content().string(not(containsString(">회원·계정</a>"))));

        for (String code : new String[] {"ACCOUNT", "HACKED"}) {
            mockMvc.perform(post("/faqs/new").with(csrf())
                            .param("faqClCode", code).param("sortOrdr", "1")
                            .param("qestnSj", "분류 검증 질문").param("answerCn", "a"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("faq/form"))
                    .andExpect(content().string(containsString("올바르지 않은 FAQ 분류입니다.")))
                    .andExpect(content().string(containsString("value=\"분류 검증 질문\"")));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMTNFAQINFO WHERE QESTN_SJ = '분류 검증 질문'", Integer.class)).isZero();
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("필수값·범위 오류는 같은 화면에 표시")
    void validation() throws Exception {
        mockMvc.perform(post("/faqs/new").with(csrf())
                        .param("faqClCode", "").param("qestnSj", "").param("answerCn", "").param("sortOrdr", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("faq/form"))
                .andExpect(model().attributeHasFieldErrors("faq", "faqClCode", "qestnSj", "answerCn", "sortOrdr"));
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("삭제는 논리 삭제: 목록·수정 화면에서 사라지지만 데이터는 남는다, 재삭제는 404")
    void softDelete() throws Exception {
        mockMvc.perform(post("/faqs/" + FAQ_2 + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/faqs"));

        mockMvc.perform(get("/faqs")).andExpect(content().string(not(containsString("접수번호는 언제 발급되나요?"))));
        mockMvc.perform(get("/faqs/" + FAQ_2 + "/edit")).andExpect(status().isNotFound());
        mockMvc.perform(post("/faqs/" + FAQ_2 + "/delete").with(csrf())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT USE_AT FROM COMTNFAQINFO WHERE FAQ_ID = ?", String.class, FAQ_2))
                .isEqualTo("N");
    }
}
