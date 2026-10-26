package kr.go.daegu.pms;

import kr.go.daegu.pms.announcement.batch.AnnouncementCloseScheduler;
import kr.go.daegu.pms.cmm.service.BatchHistoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공고 마감 배치 (eGov 배치): 청크로 나눠 빠짐없이 마감하는지, 같은 기준일은 한 번만 도는지, 관리자 수동 실행과 이력 화면.
 *
 * Spring Batch 는 이미 열린 트랜잭션 안에서 잡을 실행하지 않는다 (JobRepository 가 거부).
 * 그래서 이 클래스만 테스트 트랜잭션을 끄고(NOT_SUPPORTED), 만든 공고는 테스트가 끝날 때 직접 지운다.
 * 기동 직후 실행이 기준일 2026-10-21(고정 Clock)을 이미 썼으므로 정기 실행 시험은 다른 날짜를 쓴다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AnnouncementCloseJobIntegrationTest extends IntegrationTestSupport {

    private static final String TITLE_PREFIX = "[배치시험] ";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired AnnouncementCloseScheduler scheduler;
    @Autowired BatchHistoryService batchHistoryService;

    @AfterEach
    void deleteTestAnnouncements() {
        jdbc.update("DELETE FROM announcement WHERE title LIKE ?", TITLE_PREFIX + "%");
    }

    private void insertOpenAnnouncements(List<Object[]> titleAndEndDate) {
        jdbc.batchUpdate("""
                INSERT INTO announcement (title, field_code, total_budget, max_grant, start_date, end_date, content, status, writer_id)
                VALUES (?, 'AI_SW', 1, 1, '2026-10-01', ?, 'c', 'OPEN', 'manager1')
                """, titleAndEndDate);
    }

    private Map<String, Object> announcement(String title) {
        return jdbc.queryForMap("SELECT status, updated_by, updated_at FROM announcement WHERE title = ?", TITLE_PREFIX + title);
    }

    @Test
    @DisplayName("기준일보다 마감일이 이른 접수중 공고 250건을 100건씩 세 번에 나눠 모두 마감하고, 수정자는 SYSTEM")
    void closesEveryExpiredAnnouncementInChunks() {
        insertOpenAnnouncements(IntStream.rangeClosed(1, 250)
                .mapToObj(i -> new Object[]{TITLE_PREFIX + "지난 공고 " + i, "2026-10-21"})
                .toList());
        insertOpenAnnouncements(List.<Object[]>of(new Object[]{TITLE_PREFIX + "기준일 마감", "2026-10-22"}));

        JobExecution execution = scheduler.runDaily(LocalDate.of(2026, 10, 22)).orElseThrow();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(250);
        assertThat(step.getWriteCount()).isEqualTo(250);
        assertThat(step.getCommitCount()).isEqualTo(3);   // 100 + 100 + 50: OFFSET 을 쓰면 여기서 건너뛴 공고가 생긴다

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM announcement WHERE title LIKE ? AND status = 'CLOSED'",
                Integer.class, TITLE_PREFIX + "지난 공고%")).isEqualTo(250);
        Map<String, Object> closed = announcement("지난 공고 250");
        assertThat(closed.get("updated_by")).isEqualTo("SYSTEM");
        assertThat(closed.get("updated_at")).isEqualTo(NOW);
        assertThat(announcement("기준일 마감").get("status")).isEqualTo("OPEN");   // 마감일 당일까지는 접수
        assertThat(jdbc.queryForObject("SELECT status FROM announcement WHERE announcement_id = 13", String.class))
                .isEqualTo("OPEN");
    }

    @Test
    @DisplayName("같은 기준일로 다시 실행하면 이미 끝난 실행(JobInstance)이라 건너뛴다")
    void sameTargetDateRunsOnce() {
        LocalDate targetDate = LocalDate.of(2026, 10, 23);

        assertThat(scheduler.runDaily(targetDate))
                .hasValueSatisfying(execution -> assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED));
        assertThat(scheduler.runDaily(targetDate)).isEmpty();

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM BATCH_JOB_EXECUTION_PARAMS
                WHERE PARAMETER_NAME = 'targetDate' AND PARAMETER_VALUE = '2026-10-23'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자가 지금 실행하면 수정자는 그 관리자로 남고, 이력 화면에 수동 실행으로 보인다")
    void adminRunsNowAndSeesHistory() throws Exception {
        insertOpenAnnouncements(List.<Object[]>of(new Object[]{TITLE_PREFIX + "어제 마감", "2026-10-20"}));

        mockMvc.perform(post("/admin/batch/announcement-close").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/batch"))
                .andExpect(flash().attribute("successMessage", containsString("결과 완료, 마감 1건")));

        Map<String, Object> closed = announcement("어제 마감");
        assertThat(closed.get("status")).isEqualTo("CLOSED");
        assertThat(closed.get("updated_by")).isEqualTo("admin");

        mockMvc.perform(get("/admin/batch").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin (수동)")))
                .andExpect(content().string(containsString("공고 접수 마감")));
    }

    @Test
    @DisplayName("담당자는 배치 이력 화면·수동 실행에 들어올 수 없고, 서비스를 직접 불러도 막힌다")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void managerIsDenied() throws Exception {
        mockMvc.perform(get("/admin/batch")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/batch/announcement-close").with(csrf())).andExpect(status().isForbidden());

        assertThatThrownBy(() -> batchHistoryService.selectBatchExecutionList(20)).isInstanceOf(AccessDeniedException.class);
    }
}
