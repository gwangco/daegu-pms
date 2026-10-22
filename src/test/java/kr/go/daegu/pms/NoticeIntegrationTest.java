package kr.go.daegu.pms;

import kr.go.daegu.pms.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공지사항 (전자정부 공통컴포넌트 게시판 cop.bbs 로 전환) 검증.
 * 공통컴포넌트 원본에서 고친 부분(검색 조건 오류, 작성자 위조, 삭제 시 제목 덮어쓰기)이 다시 생기지 않는지도 확인한다.
 */
class NoticeIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("누구나 목록/상세 조회 가능, 중요 공지가 먼저 나오고 상세 조회 시 조회수 증가")
    void readable() throws Exception {
        String html = mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("정산 증빙 제출 기한")).isLessThan(html.indexOf("[필독] D-PMS 지원사업 포털 이용 안내"));
        assertThat(html.indexOf("[필독] D-PMS 지원사업 포털 이용 안내")).isLessThan(html.indexOf("시스템 정기 점검 안내"));

        int before = viewCount(3);
        mockMvc.perform(get("/notices/3")).andExpect(status().isOk());
        assertThat(viewCount(3)).isEqualTo(before + 1);

        mockMvc.perform(get("/notices/9999")).andExpect(status().isNotFound());
        // 공통컴포넌트 전환 전 주소(NTC_...)나 숫자가 아닌 값은 컨트롤러까지 가지 않고 404
        mockMvc.perform(get("/notices/NTC_0000000003")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공통컴포넌트 원본은 searchCnd 를 숫자와 비교해 숫자가 아닌 값이면 500 이었다 → 이제 정상 조회, 작성자 검색도 동작")
    void searchConditionIsSafe() throws Exception {
        mockMvc.perform(get("/notices").param("searchCnd", "abc").param("searchWrd", "정기 점검"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("시스템 정기 점검 안내")))
                .andExpect(content().string(not(containsString("정산 증빙 제출 기한"))));

        mockMvc.perform(get("/notices").param("searchCnd", "2").param("searchWrd", "총괄관리자"))
                .andExpect(content().string(containsString("시스템 정기 점검 안내")))
                .andExpect(content().string(not(containsString("[필독] D-PMS 지원사업 포털 이용 안내"))));
    }

    @Test
    @DisplayName("기업회원/비로그인은 공지 등록·수정·삭제 불가")
    @WithUserDetails("company1")
    void companyCannotWrite() throws Exception {
        mockMvc.perform(get("/notices/new")).andExpect(status().isForbidden());
        mockMvc.perform(post("/notices/new").with(csrf()).param("nttSj", "t").param("nttCn", "c"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/notices/1/delete").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("담당자 등록: ID 는 공통컴포넌트 채번(NTT_ID), 작성자·조회수·게시판·비밀글 변조 무시, 본문 스크립트는 이스케이프되어 출력")
    @WithUserDetails("manager1")
    void createByManager() throws Exception {
        MvcResult result = mockMvc.perform(post("/notices/new").with(csrf())
                        .param("nttSj", "신규 공지")
                        .param("nttCn", "<script>alert('xss')</script>\n둘째 줄")
                        .param("noticeAt", "Y")
                        .param("frstRegisterId", "admin").param("inqireCo", "9999").param("nttId", "777")
                        .param("bbsId", "BBSMSTR_999999999999").param("secretAt", "Y").param("password", "1234"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        long nttId = Long.parseLong(result.getResponse().getRedirectedUrl().replace("/notices/", ""));
        assertThat(nttId).isGreaterThan(3).isNotEqualTo(777);

        Map<String, Object> saved = jdbc.queryForMap(
                "SELECT BBS_ID, FRST_REGISTER_ID, NTCR_NM, RDCNT, NOTICE_AT, SECRET_AT, PASSWORD, FRST_REGIST_PNTTM FROM COMTNBBS WHERE NTT_ID = ?", nttId);
        assertThat(saved.get("BBS_ID")).isEqualTo(NoticeService.BBS_ID);
        assertThat(saved.get("FRST_REGISTER_ID")).isEqualTo("manager1");
        assertThat(saved.get("NTCR_NM")).isEqualTo("김담당");
        assertThat(((Number) saved.get("RDCNT")).intValue()).isZero();
        assertThat(saved.get("NOTICE_AT")).isEqualTo("Y");
        assertThat(saved.get("SECRET_AT")).isEqualTo("N");
        assertThat(saved.get("PASSWORD")).isNull();
        assertThat(saved.get("FRST_REGIST_PNTTM")).isEqualTo(NOW);   // 고정 Clock(KST)

        mockMvc.perform(get("/notices/" + nttId))
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert('xss')</script>"))));
    }

    @Test
    @DisplayName("필수값 누락 시 같은 화면에 오류 표시")
    @WithUserDetails("manager1")
    void validation() throws Exception {
        mockMvc.perform(post("/notices/new").with(csrf()).param("nttSj", "").param("nttCn", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("notice/form"))
                .andExpect(model().attributeHasFieldErrors("notice", "nttSj", "nttCn"))
                .andExpect(content().string(containsString("제목을 입력하세요.")));
    }

    @Test
    @DisplayName("수정은 제목·내용·중요여부만 바뀌고 작성자는 유지, 수정자/수정일시 기록")
    @WithUserDetails("admin")
    void update() throws Exception {
        mockMvc.perform(post("/notices/3/edit").with(csrf())
                        .param("nttSj", "점검 일정 변경").param("nttCn", "변경된 내용").param("frstRegisterId", "company1"))
                .andExpect(redirectedUrl("/notices/3"));

        Map<String, Object> updated = jdbc.queryForMap(
                "SELECT NTT_SJ, NOTICE_AT, FRST_REGISTER_ID, LAST_UPDUSR_ID, LAST_UPDT_PNTTM FROM COMTNBBS WHERE NTT_ID = 3");
        assertThat(updated.get("NTT_SJ")).isEqualTo("점검 일정 변경");
        assertThat(updated.get("NOTICE_AT")).isEqualTo("N");            // 체크 해제 → N
        assertThat(updated.get("FRST_REGISTER_ID")).isEqualTo("admin"); // seed 작성자 유지 (변조 무시)
        assertThat(updated.get("LAST_UPDUSR_ID")).isEqualTo("admin");
        assertThat(updated.get("LAST_UPDT_PNTTM")).isEqualTo(NOW);
    }

    @Test
    @DisplayName("삭제는 논리 삭제: 목록·상세에서 사라지지만 제목을 포함한 데이터는 그대로 남는다, 재삭제는 404")
    @WithUserDetails("manager1")
    void softDelete() throws Exception {
        mockMvc.perform(post("/notices/2/delete").with(csrf()))
                .andExpect(redirectedUrl("/notices"));

        mockMvc.perform(get("/notices/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/notices")).andExpect(content().string(not(containsString("정산 증빙 제출 기한"))));
        mockMvc.perform(post("/notices/2/delete").with(csrf())).andExpect(status().isNotFound());

        // 공통컴포넌트 원본은 삭제할 때 제목을 "이 글은 작성자에 의해서 삭제되었습니다." 로 덮어썼다
        Map<String, Object> row = jdbc.queryForMap("SELECT USE_AT, NTT_SJ FROM COMTNBBS WHERE NTT_ID = 2");
        assertThat(row.get("USE_AT")).isEqualTo("N");
        assertThat((String) row.get("NTT_SJ")).contains("정산 증빙 제출 기한");
    }

    private int viewCount(long nttId) {
        Integer count = jdbc.queryForObject("SELECT RDCNT FROM COMTNBBS WHERE NTT_ID = ?", Integer.class, nttId);
        return count == null ? 0 : count;
    }
}
