package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 개인정보 처리 기록: 남의 개인정보를 조회·다운로드하면 남고, 본인 정보는 남지 않는다 (일시는 고정 Clock) */
class PrivacyAccessLogIntegrationTest extends IntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;

    private List<Map<String, Object>> logs(String userId) {
        return jdbc.queryForList("SELECT * FROM privacy_access_log WHERE user_id = ? ORDER BY log_id", userId);
    }

    @Test
    @DisplayName("담당자가 기업 신청서를 열면 처리자·대상·정보주체·접속지·일시가 남고, 기업이 자기 신청서를 열면 남지 않는다")
    void applicationViewIsLogged() throws Exception {
        mockMvc.perform(get("/applications/1").with(user("manager1").roles("MANAGER"))
                        .with(request -> { request.setRemoteAddr("203.0.113.9"); return request; }))
                .andExpect(status().isOk());
        List<Map<String, Object>> manager = logs("manager1");
        assertThat(manager).hasSize(1);
        assertThat(manager.get(0))
                .containsEntry("action", "VIEW").containsEntry("target_type", "APPLICATION")
                .containsEntry("target_id", "1").containsEntry("subject_id", "company1")
                .containsEntry("client_ip", "203.0.113.9").containsEntry("accessed_at", NOW);

        mockMvc.perform(get("/applications/1").with(user("company1").roles("COMPANY"))).andExpect(status().isOk());
        assertThat(logs("company1")).isEmpty();
    }

    @Test
    @DisplayName("평가위원이 배정 과제를 열어도 남는다 (사업자등록번호는 가려서 보지만 기업 정보를 처리한 것)")
    void evaluatorViewIsLogged() throws Exception {
        mockMvc.perform(get("/applications/2").with(user("evaluator1").roles("EVALUATOR"))).andExpect(status().isOk());
        assertThat(logs("evaluator1")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("subject_id", "company2"));
    }

    @Test
    @DisplayName("엑셀·사용자 목록은 여러 사람 정보라 정보주체는 비우고 건수를 남긴다")
    void bulkAccessKeepsCount() throws Exception {
        int applications = jdbc.queryForObject("SELECT COUNT(*) FROM application WHERE status <> 'TEMP_SAVED'", Integer.class);
        mockMvc.perform(get("/excel/applications").with(user("manager1").roles("MANAGER"))).andExpect(status().isOk());
        assertThat(logs("manager1")).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("action", "DOWNLOAD").containsEntry("target_type", "EXCEL")
                .containsEntry("record_count", applications).containsEntry("subject_id", null));

        mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        assertThat(logs("admin")).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("action", "VIEW").containsEntry("target_type", "USER_LIST"));
        assertThat((Integer) logs("admin").get(0).get("record_count")).isPositive();
    }

    @Test
    @DisplayName("기업이 낸 사업계획서를 담당자가 내려받으면 남고, 기업 본인이 받으면 남지 않는다")
    void fileDownloadIsLogged() throws Exception {
        String location = mockMvc.perform(multipart("/applications/apply").file(pdf("planFile", "사업계획서.pdf"))
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치")
                        .param("totalBudget", "50000000").param("requestGrant", "40000000").param("summary", "요약")
                        .with(user("company1").roles("COMPANY")).with(csrf()))
                .andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(location.replace("/applications/", ""));
        String atchFileId = jdbc.queryForObject("SELECT plan_atch_file_id FROM application WHERE application_id = ?", String.class, id);

        mockMvc.perform(get("/files/" + atchFileId + "/0").with(user("company1").roles("COMPANY"))).andExpect(status().isOk());
        assertThat(logs("company1")).isEmpty();
        mockMvc.perform(get("/files/" + atchFileId + "/0").with(user("manager1").roles("MANAGER"))).andExpect(status().isOk());
        assertThat(logs("manager1")).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("action", "DOWNLOAD").containsEntry("target_type", "FILE")
                .containsEntry("target_id", atchFileId + "/0"));
    }

    @Test
    @DisplayName("처리 기록 화면은 총괄관리자만 본다")
    void adminOnlyScreen() throws Exception {
        mockMvc.perform(get("/applications/1").with(user("manager1").roles("MANAGER")));
        mockMvc.perform(get("/admin/privacy-log").param("searchKeyword", "manager1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("신청서 상세")))
                .andExpect(content().string(containsString("company1")));
        mockMvc.perform(get("/admin/privacy-log").with(user("manager1").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }
}
