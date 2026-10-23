package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 신청서 임시저장 → 이어서 작성 → 제출, 제출 취소, 임시저장 삭제 (기준 날짜는 IntegrationTestSupport 의 고정 Clock 2026-10-21).
 * seed: 공고 15(헬스, 접수중, 최대 4,000만원)는 company1 이 아직 신청하지 않았고, 신청서 3 은 company1 의 공고 14 접수완료 건이다.
 */
class ApplicationDraftIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;

    private Map<String, Object> row(long applicationId) {
        return jdbc.queryForMap("SELECT * FROM application WHERE application_id = ?", applicationId);
    }

    private long draftCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM application WHERE status = 'TEMP_SAVED'", Long.class);
    }

    private static MockMultipartHttpServletRequestBuilder draftRequest(String url) {
        MockMultipartHttpServletRequestBuilder request = multipart(url);
        request.param("draft", "true").with(csrf());
        return request;
    }

    /** 공고 15 에 과제명만 적은 임시저장을 만들고 신청서 번호를 돌려준다 */
    private long saveDraft() throws Exception {
        String location = mockMvc.perform(draftRequest("/applications/apply")
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치"))
                .andExpect(redirectedUrlPattern("/applications/*/edit"))
                .andExpect(flash().attribute("successMessage", containsString("임시저장했습니다")))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.replace("/applications/", "").replace("/edit", ""));
    }

    @Test
    @WithUserDetails("company1")
    @DisplayName("임시저장: 과제명만 있으면 저장되고, 접수번호·제출 일시는 없다. 상태·접수번호를 보내도 무시")
    void draftNeedsOnlyProjectName() throws Exception {
        String location = mockMvc.perform(draftRequest("/applications/apply")
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치")
                        .param("status", "SELECTED").param("receiptNo", "DPMS-99999999"))
                .andExpect(redirectedUrlPattern("/applications/*/edit"))
                .andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(location.replace("/applications/", "").replace("/edit", ""));

        Map<String, Object> draft = row(id);
        assertThat(draft.get("status")).isEqualTo("TEMP_SAVED");
        assertThat(draft.get("receipt_no")).isNull();
        assertThat(draft.get("submitted_at")).isNull();
        assertThat(draft.get("total_budget")).isNull();
        assertThat(draft.get("created_at")).isEqualTo(NOW);

        // 과제명이 없으면 임시저장도 안 된다
        mockMvc.perform(draftRequest("/applications/apply").param("announcementId", "16").param("summary", "요약만"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("임시저장하려면 과제명을 입력하세요.")));
    }

    @Test
    @WithUserDetails("company1")
    @DisplayName("이어서 작성해 제출: 필수 항목이 비면 거부, 모두 채우면 접수번호·제출 일시가 생기고 자부담금은 서버가 계산")
    void continueAndSubmit() throws Exception {
        long id = saveDraft();
        mockMvc.perform(get("/applications/" + id + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("임시저장한 신청서입니다")))
                .andExpect(content().string(containsString("value=\"웨어러블 심전도 패치\"")));

        // 요약·금액·사업계획서 없이 제출 → 칸 검증으로 거부, 여전히 임시저장
        mockMvc.perform(multipart("/applications/" + id + "/edit")
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치").with(csrf()))
                .andExpect(status().isOk());
        assertThat(row(id).get("status")).isEqualTo("TEMP_SAVED");

        // 금액·요약을 채워도 사업계획서가 없으면 거부
        mockMvc.perform(multipart("/applications/" + id + "/edit")
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치").param("summary", "요약")
                        .param("totalBudget", "50000000").param("requestGrant", "40000000").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("첨부파일을 선택하세요.")));

        mockMvc.perform(multipart("/applications/" + id + "/edit").file(pdf("planFile", "사업계획서.pdf"))
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치").param("summary", "요약")
                        .param("totalBudget", "50000000").param("requestGrant", "40000000")
                        .param("selfFunding", "1").with(csrf()))
                .andExpect(redirectedUrl("/applications/" + id))
                .andExpect(flash().attribute("successMessage", "사업계획서가 정상적으로 제출되었습니다."));

        Map<String, Object> submitted = row(id);
        assertThat(submitted.get("status")).isEqualTo("SUBMITTED");
        assertThat((String) submitted.get("receipt_no")).matches("DPMS-\\d{8}");
        assertThat(submitted.get("submitted_at")).isEqualTo(NOW);
        assertThat(submitted.get("self_funding")).isEqualTo(10_000_000L);
        assertThat(submitted.get("plan_atch_file_id")).isNotNull();
        // 제출한 신청서는 이어서 작성 화면으로 고칠 수 없다
        mockMvc.perform(get("/applications/" + id + "/edit")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("임시저장은 신청한 기업만 본다: 담당자 목록·상세·처리할 일, 다른 기업 모두에서 보이지 않는다")
    void draftIsPrivateToOwner() throws Exception {
        long id;
        {
            String location = mockMvc.perform(draftRequest("/applications/apply").with(user("company1").roles("COMPANY"))
                            .param("announcementId", "15").param("projectName", "아무도 몰라야 할 초안"))
                    .andReturn().getResponse().getRedirectedUrl();
            id = Long.parseLong(location.replace("/applications/", "").replace("/edit", ""));
        }
        mockMvc.perform(get("/applications").with(user("manager1").roles("MANAGER")))
                .andExpect(content().string(not(containsString("아무도 몰라야 할 초안"))))
                .andExpect(content().string(not(containsString("value=\"TEMP_SAVED\""))));
        mockMvc.perform(get("/applications/" + id).with(user("manager1").roles("MANAGER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/applications/" + id).with(user("company2").roles("COMPANY")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/applications/" + id + "/edit").with(user("company2").roles("COMPANY")))
                .andExpect(status().isNotFound());

        // 본인 목록·메인 처리할 일에는 보인다
        mockMvc.perform(get("/applications").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(containsString("아무도 몰라야 할 초안")))
                .andExpect(content().string(containsString("작성 중")));
        mockMvc.perform(get("/").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(matchesPattern("(?s).*작성 중인 신청서</span>\\s*<span class=\"task-count\"><strong>1</strong>.*")));
    }

    @Test
    @WithUserDetails("company1")
    @DisplayName("한 공고에 신청서 하나: 임시저장이 있으면 신청 화면 대신 이어서 작성으로, 제출한 공고는 상세로 보낸다")
    void oneApplicationPerAnnouncement() throws Exception {
        long id = saveDraft();
        mockMvc.perform(get("/applications/apply").param("announcementId", "15"))
                .andExpect(redirectedUrl("/applications/" + id + "/edit"));
        mockMvc.perform(get("/applications/apply").param("announcementId", "14"))
                .andExpect(redirectedUrl("/applications/3"))
                .andExpect(flash().attribute("errorMessage", containsString("이미 신청한 공고입니다")));
        mockMvc.perform(draftRequest("/applications/apply").param("announcementId", "15").param("projectName", "두 번째 초안"))
                .andExpect(content().string(containsString("이미 신청한 공고입니다")));
    }

    @Test
    @WithUserDetails("company1")
    @DisplayName("제출 취소: 서류 검토 전이면 임시저장으로 돌아가고, 다시 내면 처음 받은 접수번호를 그대로 쓴다")
    void withdrawAndResubmitKeepsReceiptNo() throws Exception {
        String receiptNo = (String) row(3).get("receipt_no");
        mockMvc.perform(post("/applications/3/withdraw").with(csrf()))
                .andExpect(redirectedUrl("/applications/3"))
                .andExpect(flash().attribute("successMessage", containsString("제출을 취소했습니다")));
        Map<String, Object> withdrawn = row(3);
        assertThat(withdrawn.get("status")).isEqualTo("TEMP_SAVED");
        assertThat(withdrawn.get("submitted_at")).isNull();
        assertThat(withdrawn.get("receipt_no")).isEqualTo(receiptNo);
        assertThat(withdrawn.get("updated_by")).isEqualTo("company1");

        // 화면이 보낸 공고 번호(마감된 11)는 쓰지 않고, 저장된 신청서의 공고(14, 접수중)로 확인한다
        mockMvc.perform(multipart("/applications/3/edit").file(pdf("planFile", "고친 계획서.pdf"))
                        .param("announcementId", "11").param("projectName", "고친 과제명").param("summary", "고친 요약")
                        .param("totalBudget", "60000000").param("requestGrant", "45000000").with(csrf()))
                .andExpect(redirectedUrl("/applications/3"));
        Map<String, Object> resubmitted = row(3);
        assertThat(resubmitted.get("status")).isEqualTo("SUBMITTED");
        assertThat(resubmitted.get("receipt_no")).isEqualTo(receiptNo);
        assertThat(resubmitted.get("project_name")).isEqualTo("고친 과제명");
        assertThat(resubmitted.get("announcement_id")).isEqualTo(14L);
    }

    @Test
    @DisplayName("제출 취소는 본인 접수완료 건만: 서류 검토가 시작됐거나 남의 신청서면 거부")
    void withdrawRules() throws Exception {
        // 신청서 2 는 company2 의 전문가심사중 건
        mockMvc.perform(post("/applications/2/withdraw").with(user("company2").roles("COMPANY")).with(csrf()))
                .andExpect(flash().attribute("errorMessage", containsString("서류 검토가 시작되기 전")));
        assertThat(row(2).get("status")).isEqualTo("EVALUATING");
        mockMvc.perform(post("/applications/3/withdraw").with(user("company2").roles("COMPANY")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/applications/3/withdraw").with(user("manager1").roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(row(3).get("status")).isEqualTo("SUBMITTED");
    }

    @Test
    @WithUserDetails("company1")
    @DisplayName("임시저장 삭제: 본인 임시저장만 지워지고, 제출한 신청서는 지울 수 없다")
    void deleteDraftOnly() throws Exception {
        long id = saveDraft();
        long drafts = draftCount();
        mockMvc.perform(post("/applications/" + id + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/applications"))
                .andExpect(flash().attribute("successMessage", "작성 중이던 신청서를 삭제했습니다."));
        assertThat(draftCount()).isEqualTo(drafts - 1);

        mockMvc.perform(post("/applications/3/delete").with(csrf()))
                .andExpect(redirectedUrl("/applications/3"))
                .andExpect(flash().attribute("errorMessage", containsString("임시저장 중인 신청서만")));
        assertThat(row(3).get("status")).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("DB 도 지킨다: 필수 항목이 빈 신청서를 접수 상태로 바꾸려 하면 CHECK 제약이 거부한다")
    void checkConstraintRejectsIncompleteSubmission() {
        jdbc.update("INSERT INTO application (announcement_id, company_user_id, project_name, status, created_at) "
                + "VALUES (16, 'company1', '빈 신청서', 'TEMP_SAVED', ?)", NOW);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE application SET status = 'SUBMITTED' WHERE company_user_id = 'company1' AND announcement_id = 16"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_application_submitted_complete");
    }
}
