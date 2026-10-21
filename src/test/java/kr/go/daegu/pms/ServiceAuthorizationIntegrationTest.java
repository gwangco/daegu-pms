package kr.go.daegu.pms;

import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** URL/컨트롤러를 거치지 않는 호출에서도 쓰기 권한을 보장한다. */
class ServiceAuthorizationIntegrationTest extends IntegrationTestSupport {

    @Autowired AnnouncementService announcementService;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("기업이 공고 등록 서비스를 직접 호출해도 저장 전에 차단된다")
    @WithMockUser(username = "company1", roles = "COMPANY")
    void companyCannotInsertAnnouncementDirectly() {
        assertAnnouncementInsertDenied();
    }

    @Test
    @DisplayName("평가위원이 공고 등록 서비스를 직접 호출해도 저장 전에 차단된다")
    @WithMockUser(username = "evaluator1", roles = "EVALUATOR")
    void evaluatorCannotInsertAnnouncementDirectly() {
        assertAnnouncementInsertDenied();
    }

    @Test
    @DisplayName("비로그인 공고 등록 서비스 호출은 DB 오류 대신 접근 거부로 처리한다")
    void anonymousCannotInsertAnnouncementDirectly() {
        assertAnnouncementInsertDenied();
    }

    @Test
    @DisplayName("담당자가 공고를 등록하면 서버가 로그인 계정을 작성자로 지정한다")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void managerCanInsertAnnouncement() {
        AnnouncementVO announcement = validAnnouncement();
        announcement.setWriterId("company1");
        Long id = announcementService.insertAnnouncement(announcement);
        assertThat(jdbc.queryForObject("SELECT writer_id FROM announcement WHERE announcement_id = ?", String.class, id))
                .isEqualTo("manager1");
    }

    private void assertAnnouncementInsertDenied() {
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM announcement", Integer.class);
        assertThatThrownBy(() -> announcementService.insertAnnouncement(validAnnouncement()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM announcement", Integer.class)).isEqualTo(before);
    }

    private AnnouncementVO validAnnouncement() {
        return AnnouncementVO.builder()
                .title("서비스 권한 검증 공고").fieldCode("AI_SW")
                .totalBudget(10_000_000L).maxGrant(1_000_000L)
                .startDate(LocalDate.of(2026, 10, 21)).endDate(LocalDate.of(2026, 11, 20))
                .content("서비스 계층 직접 호출 검증").build();
    }
}
