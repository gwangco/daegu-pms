package kr.go.daegu.pms;

import kr.go.daegu.pms.notice.service.NoticeVO;
import kr.go.daegu.pms.notice.service.impl.NoticeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공지사항 (eGovFrame CRUD 코드 생성기로 만든 뒤 보완한 기능) 검증
 */
class NoticeIntegrationTest extends IntegrationTestSupport {

    @Autowired NoticeMapper noticeMapper;

    @Test
    @DisplayName("누구나 목록/상세 조회 가능, 중요 공지가 먼저 나오고 상세 조회 시 조회수 증가")
    void readable() throws Exception {
        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("[필독] D-PMS 지원사업 포털 이용 안내")));

        int before = noticeMapper.selectNotice("NTC_0000000003").orElseThrow().getViewCount();
        mockMvc.perform(get("/notices/NTC_0000000003")).andExpect(status().isOk());
        assertThat(noticeMapper.selectNotice("NTC_0000000003").orElseThrow().getViewCount()).isEqualTo(before + 1);

        mockMvc.perform(get("/notices/NTC_9999999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("기업회원/비로그인은 공지 등록·수정·삭제 불가")
    @WithUserDetails("company1")
    void companyCannotWrite() throws Exception {
        mockMvc.perform(get("/notices/new")).andExpect(status().isForbidden());
        mockMvc.perform(post("/notices/new").with(csrf()).param("title", "t").param("content", "c"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/notices/NTC_0000000001/delete").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("담당자 등록: ID 는 eGov IdGnr(NTC_ + 10자리), 작성자·조회수 변조 무시, 본문 스크립트는 이스케이프되어 출력")
    @WithUserDetails("manager1")
    void createByManager() throws Exception {
        MvcResult result = mockMvc.perform(post("/notices/new").with(csrf())
                        .param("title", "신규 공지")
                        .param("content", "<script>alert('xss')</script>\n둘째 줄")
                        .param("importantYn", "Y")
                        .param("writerId", "admin").param("viewCount", "9999").param("noticeId", "NTC_HACKED"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String noticeId = result.getResponse().getRedirectedUrl().replace("/notices/", "");
        assertThat(noticeId).matches("NTC_\\d{10}");

        NoticeVO saved = noticeMapper.selectNotice(noticeId).orElseThrow();
        assertThat(saved.getWriterId()).isEqualTo("manager1");
        assertThat(saved.getViewCount()).isZero();
        assertThat(saved.getImportantYn()).isEqualTo("Y");

        mockMvc.perform(get("/notices/" + noticeId))
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert('xss')</script>"))));
    }

    @Test
    @DisplayName("필수값 누락 시 같은 화면에 오류 표시")
    @WithUserDetails("manager1")
    void validation() throws Exception {
        mockMvc.perform(post("/notices/new").with(csrf()).param("title", "").param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("notice/form"))
                .andExpect(model().attributeHasFieldErrors("notice", "title", "content"));
    }

    @Test
    @DisplayName("수정은 제목·내용·중요여부만 바뀌고 작성자는 유지, 수정자/수정일시 기록")
    @WithUserDetails("admin")
    void update() throws Exception {
        mockMvc.perform(post("/notices/NTC_0000000003/edit").with(csrf())
                        .param("title", "점검 일정 변경").param("content", "변경된 내용").param("writerId", "company1"))
                .andExpect(redirectedUrl("/notices/NTC_0000000003"));

        NoticeVO updated = noticeMapper.selectNotice("NTC_0000000003").orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("점검 일정 변경");
        assertThat(updated.getImportantYn()).isEqualTo("N");      // 체크 해제 → N
        assertThat(updated.getWriterId()).isEqualTo("admin");     // seed 작성자 유지 (변조 무시)
        assertThat(updated.getUpdatedBy()).isEqualTo("admin");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("삭제는 논리 삭제: 목록·상세에서 사라지지만 데이터는 남는다, 재삭제는 404")
    @WithUserDetails("manager1")
    void softDelete() throws Exception {
        mockMvc.perform(post("/notices/NTC_0000000002/delete").with(csrf()))
                .andExpect(redirectedUrl("/notices"));

        mockMvc.perform(get("/notices/NTC_0000000002")).andExpect(status().isNotFound());
        mockMvc.perform(get("/notices")).andExpect(content().string(not(containsString("정산 증빙 제출 기한"))));
        mockMvc.perform(post("/notices/NTC_0000000002/delete").with(csrf())).andExpect(status().isNotFound());
    }
}
