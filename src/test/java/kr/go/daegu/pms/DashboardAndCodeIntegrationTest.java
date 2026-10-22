package kr.go.daegu.pms;

import kr.go.daegu.pms.cmm.service.CommonCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/** 메인 화면 역할별 "처리할 일"과 공통코드 사용 여부(use_yn) 검증 */
class DashboardAndCodeIntegrationTest extends IntegrationTestSupport {

    @Autowired JdbcTemplate jdbc;
    @Autowired CommonCodeService commonCodeService;

    private static String task(String label, int count) {
        // 카드 안에서 업무 이름 뒤에 건수가 나온다
        return "(?s).*" + label + "</div>\\s*<div><span class=\"count\">" + count + "</span>.*";
    }

    @Test
    @DisplayName("담당자: 서류 검토 대기·선정 판단 대기·정산 검수 대기 건수")
    void managerTasks() throws Exception {
        mockMvc.perform(get("/").with(user("manager1").roles("MANAGER")))
                .andExpect(content().string(matchesPattern(task("서류 검토 대기", 1))))
                .andExpect(content().string(matchesPattern(task("채점 완료 · 선정 판단 대기", 0))))
                .andExpect(content().string(matchesPattern(task("정산 검수 대기", 1))));
    }

    @Test
    @DisplayName("평가위원 채점이 모두 끝나면 선정 판단 대기로 넘어간다")
    @WithUserDetails("manager1")
    void readyToSelectAfterScoring() throws Exception {
        jdbc.update("UPDATE evaluation SET status = 'COMPLETED', total_score = 80 WHERE application_id = 2");
        mockMvc.perform(get("/"))
                .andExpect(content().string(matchesPattern(task("채점 완료 · 선정 판단 대기", 1))));
    }

    @Test
    @DisplayName("평가위원·기업은 본인 기준 건수만 본다")
    void evaluatorAndCompanyTasks() throws Exception {
        mockMvc.perform(get("/").with(user("evaluator1").roles("EVALUATOR")))
                .andExpect(content().string(matchesPattern(task("채점 대기", 1))))
                .andExpect(content().string(not(containsString("서류 검토 대기"))));
        mockMvc.perform(get("/").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(matchesPattern(task("심사 진행 중인 신청", 1))))
                .andExpect(content().string(matchesPattern(task("선정된 과제 \\(e-정산\\)", 1))))
                .andExpect(content().string(matchesPattern(task("보완 요청된 정산", 0))));
    }

    @Test
    @DisplayName("총괄관리자는 잠긴 계정·오늘 로그인 실패도 본다, 비로그인은 처리할 일이 없다")
    void adminTasksAndAnonymous() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/login-process").with(csrf()).param("userId", "company2").param("password", "wrong"));
        }
        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(matchesPattern(task("잠긴 계정", 0))))
                .andExpect(content().string(matchesPattern(task("오늘 로그인 실패", 2))));
        mockMvc.perform(get("/"))
                .andExpect(model().attribute("myTasks", org.hamcrest.Matchers.empty()))
                .andExpect(content().string(not(containsString("처리할 일"))));
    }

    @Test
    @DisplayName("[1부 실습] 공통코드를 사용 안 함(use_yn='N')으로 바꾸면 선택 목록에서 빠지고 서버 검증도 거부한다")
    @WithUserDetails("manager1")
    void unusedCommonCodeDisappears() throws Exception {
        assertThat(commonCodeService.isValidCode(CommonCodeService.GROUP_FIELD, "HEALTH")).isTrue();
        jdbc.update("UPDATE com_code_detail SET use_yn = 'N' WHERE group_id = 'FIELD' AND code_id = 'HEALTH'");

        mockMvc.perform(get("/announcements/new"))
                .andExpect(content().string(not(containsString("value=\"HEALTH\""))))
                .andExpect(content().string(containsString("value=\"ROBOT\"")));
        assertThat(commonCodeService.isValidCode(CommonCodeService.GROUP_FIELD, "HEALTH")).isFalse();
        // 화면을 우회해 사용 안 하는 코드로 등록해도 서비스가 거부한다
        mockMvc.perform(post("/announcements/new").with(csrf())
                        .param("title", "중지 분야 공고").param("fieldCode", "HEALTH")
                        .param("totalBudget", "1000").param("maxGrant", "100")
                        .param("startDate", "2026-10-21").param("endDate", "2026-11-30").param("content", "c"))
                .andExpect(content().string(containsString("올바르지 않은 지원 분야")));
    }
}
