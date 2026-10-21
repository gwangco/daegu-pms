package kr.go.daegu.pms;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.domain.Evaluation;
import kr.go.daegu.pms.domain.Settlement;
import kr.go.daegu.pms.mapper.AnnouncementMapper;
import kr.go.daegu.pms.mapper.ApplicationMapper;
import kr.go.daegu.pms.mapper.EvaluationMapper;
import kr.go.daegu.pms.mapper.SettlementMapper;
import kr.go.daegu.pms.service.AnnouncementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 업무 흐름(공고 → 신청 → 심사 → 평가 → 선정 → 정산) 규칙 검증
 */
class WorkflowIntegrationTest extends IntegrationTestSupport {

    @Autowired AnnouncementMapper announcementMapper;
    @Autowired ApplicationMapper applicationMapper;
    @Autowired EvaluationMapper evaluationMapper;
    @Autowired SettlementMapper settlementMapper;
    @Autowired AnnouncementService announcementService;

    // ───────────── 공고 등록 ─────────────

    @Test
    @DisplayName("담당자 공고 등록 성공 (seed 와 PK 충돌 없음) / 필수값 누락 시 같은 화면에 오류 표시")
    @WithUserDetails("manager1")
    void createAnnouncement() throws Exception {
        mockMvc.perform(post("/announcements/new").with(csrf())
                        .param("title", "테스트 공고").param("fieldCode", "ROBOT")
                        .param("totalBudget", "100000000").param("maxGrant", "10000000")
                        .param("startDate", "2026-10-21").param("endDate", "2026-11-30")
                        .param("content", "내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/announcements/*"));

        mockMvc.perform(post("/announcements/new").with(csrf()).param("title", "").param("totalBudget", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("announcement/form"))
                .andExpect(model().attributeHasFieldErrors("announcement", "title", "totalBudget", "fieldCode"));
    }

    @Test
    @DisplayName("공고 등록 업무규칙: 최대지원금 > 총예산이면 거부")
    @WithUserDetails("manager1")
    void createAnnouncementBusinessRule() throws Exception {
        mockMvc.perform(post("/announcements/new").with(csrf())
                        .param("title", "규칙 위반 공고").param("fieldCode", "ROBOT")
                        .param("totalBudget", "1000").param("maxGrant", "2000")
                        .param("startDate", "2026-10-21").param("endDate", "2026-11-30")
                        .param("content", "내용"))
                .andExpect(view().name("announcement/form"))
                .andExpect(content().string(containsString("총 지원 예산을 초과")));
    }

    // ───────────── 과제 신청 ─────────────

    @Test
    @DisplayName("기업 신청 성공: 파일 저장, 자부담금 서버 재계산, status 변조 무시")
    @WithUserDetails("company1")
    void submitApplication() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/applications/apply")
                        .file(pdf("planFile", "사업계획서.pdf"))
                        .param("announcementId", "15").param("projectName", "웨어러블 심전도 패치")
                        .param("totalBudget", "50000000").param("requestGrant", "40000000")
                        .param("selfFunding", "999").param("status", "SELECTED")   // 변조 시도
                        .param("summary", "요약").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Long id = Long.valueOf(result.getResponse().getRedirectedUrl().replace("/applications/", ""));
        Application saved = applicationMapper.selectApplication(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("SUBMITTED");
        assertThat(saved.getSelfFunding()).isEqualTo(10_000_000L);
        assertThat(saved.getCompanyUserId()).isEqualTo("company1");
        assertThat(saved.getPlanFileName()).isEqualTo("사업계획서.pdf");

        // 본인은 업로드한 파일 다운로드 가능, 타 기업은 불가, 담당자는 가능
        mockMvc.perform(get("/files/" + saved.getPlanFileId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/octet-stream")))
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")));
        mockMvc.perform(get("/files/" + saved.getPlanFileId()).with(user("company2").roles("COMPANY")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/files/" + saved.getPlanFileId()).with(user("manager1").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("확장자 위조(내용은 JSP) 및 허용되지 않은 확장자 업로드 차단")
    @WithUserDetails("company1")
    void rejectMaliciousFile() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile("planFile", "plan.pdf", "application/pdf",
                "<%@ page import=\"java.io.*\" %>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/applications/apply").file(fakePdf)
                        .param("announcementId", "15").param("projectName", "p")
                        .param("totalBudget", "100").param("requestGrant", "50").param("summary", "s").with(csrf()))
                .andExpect(view().name("application/form"))
                .andExpect(content().string(containsString("확장자(.pdf)와 일치하지 않습니다")));

        MockMultipartFile jsp = new MockMultipartFile("planFile", "shell.jsp", "text/plain", PDF_BYTES);
        mockMvc.perform(multipart("/applications/apply").file(jsp)
                        .param("announcementId", "15").param("projectName", "p")
                        .param("totalBudget", "100").param("requestGrant", "50").param("summary", "s").with(csrf()))
                .andExpect(content().string(containsString("허용되지 않는 파일 형식")));
    }

    @Test
    @DisplayName("신청 업무규칙: 중복 신청 / 최대지원금 초과 / 마감 공고 신청 차단")
    @WithUserDetails("company1")
    void submitApplicationRules() throws Exception {
        // 이미 신청한 공고(13)
        mockMvc.perform(multipart("/applications/apply").file(pdf("planFile", "p.pdf"))
                        .param("announcementId", "13").param("projectName", "p")
                        .param("totalBudget", "100").param("requestGrant", "50").param("summary", "s").with(csrf()))
                .andExpect(content().string(containsString("이미 신청한 공고")));
        // 공고 15 최대지원금 4천만원 초과
        mockMvc.perform(multipart("/applications/apply").file(pdf("planFile", "p.pdf"))
                        .param("announcementId", "15").param("projectName", "p")
                        .param("totalBudget", "90000000").param("requestGrant", "45000000").param("summary", "s").with(csrf()))
                .andExpect(content().string(containsString("최대 지원금")));
        // 마감 공고(12)
        mockMvc.perform(multipart("/applications/apply").file(pdf("planFile", "p.pdf"))
                        .param("announcementId", "12").param("projectName", "p")
                        .param("totalBudget", "100").param("requestGrant", "50").param("summary", "s").with(csrf()))
                .andExpect(content().string(containsString("접수 기간이 아닌 공고")));
    }

    // ───────────── 심사 상태머신 + 평가 ─────────────

    @Test
    @DisplayName("상태머신: 접수완료 → 선정 직행 불가, 서류통과 → 평가위원 배정 시 자동으로 심사중")
    @WithUserDetails("manager1")
    void statusMachine() throws Exception {
        mockMvc.perform(post("/applications/3/status").with(csrf()).param("status", "SELECTED"))
                .andExpect(flash().attribute("errorMessage", containsString("변경할 수 없습니다")));
        assertThat(applicationMapper.selectApplication(3L).orElseThrow().getStatus()).isEqualTo("SUBMITTED");

        mockMvc.perform(post("/applications/3/status").with(csrf()).param("status", "DOCUMENT_PASSED"))
                .andExpect(flash().attributeExists("successMessage"));
        mockMvc.perform(post("/applications/3/evaluators").with(csrf()).param("evaluatorUserId", "evaluator1"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(applicationMapper.selectApplication(3L).orElseThrow().getStatus()).isEqualTo("EVALUATING");

        // 같은 평가위원 중복 배정 / 평가위원이 아닌 사용자 배정 차단
        mockMvc.perform(post("/applications/3/evaluators").with(csrf()).param("evaluatorUserId", "evaluator1"))
                .andExpect(flash().attribute("errorMessage", containsString("이미 배정")));
        mockMvc.perform(post("/applications/3/evaluators").with(csrf()).param("evaluatorUserId", "company2"))
                .andExpect(flash().attribute("errorMessage", containsString("올바르지 않은 평가위원")));
    }

    @Test
    @DisplayName("채점 미완료 과제는 최종 선정 불가")
    @WithUserDetails("manager1")
    void cannotSelectBeforeEvaluationCompleted() throws Exception {
        mockMvc.perform(post("/applications/2/status").with(csrf()).param("status", "SELECTED"))
                .andExpect(flash().attribute("errorMessage", containsString("채점이 모두 완료")));
    }

    @Test
    @DisplayName("평가위원 채점: 범위 초과 거부, 정상 제출, 재제출 거부")
    @WithUserDetails("evaluator1")
    void scoreEvaluation() throws Exception {
        mockMvc.perform(post("/evaluator/evaluations/3").with(csrf())
                        .param("quantitativeScore", "41").param("qualitativeScore", "50").param("evalComment", "의견"))
                .andExpect(flash().attribute("errorMessage", containsString("정량 점수")));

        mockMvc.perform(post("/evaluator/evaluations/3").with(csrf())
                        .param("quantitativeScore", "35").param("qualitativeScore", "50").param("evalComment", "우수"))
                .andExpect(redirectedUrl("/evaluator/evaluations"));
        Evaluation saved = evaluationMapper.selectEvaluation(3L).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getTotalScore()).isEqualTo(85);

        mockMvc.perform(post("/evaluator/evaluations/3").with(csrf())
                        .param("quantitativeScore", "40").param("qualitativeScore", "60").param("evalComment", "수정 시도"))
                .andExpect(flash().attribute("errorMessage", containsString("이미 제출")));
        assertThat(evaluationMapper.selectEvaluation(3L).orElseThrow().getTotalScore()).isEqualTo(85);
    }

    // ───────────── 사업비 정산 ─────────────

    @Test
    @DisplayName("정산 등록: 잔액 초과 거부 / 정상 등록 시 부가세 서버 재계산")
    @WithUserDetails("company1")
    void registerSettlement() throws Exception {
        // 잔액 = 1억 - (승인 1,650만 + 대기 2,420만) = 5,930만. 공급가 6천만(+부가세) → 초과
        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "invoice.pdf"))
                        .param("applicationId", "1").param("itemCode", "EXP02").param("expenseDate", "2026-10-20")
                        .param("vendorName", "거래처").param("supplyAmount", "60000000").param("description", "초과")
                        .with(csrf()))
                .andExpect(flash().attribute("errorMessage", containsString("잔액을 초과")));

        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "invoice.pdf"))
                        .param("applicationId", "1").param("itemCode", "EXP02").param("expenseDate", "2026-10-20")
                        .param("vendorName", "거래처").param("supplyAmount", "1000005").param("vatAmount", "1")
                        .param("totalAmount", "1").param("description", "정상").with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));

        List<Settlement> list = settlementMapper.selectSettlementList(1L);
        Settlement created = list.stream().filter(s -> "정상".equals(s.getDescription())).findFirst().orElseThrow();
        assertThat(created.getVatAmount()).isEqualTo(100_000L);         // 원 미만 절사
        assertThat(created.getTotalAmount()).isEqualTo(1_100_005L);
        assertThat(created.getStatus()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("정산 등록: 미래 일자 / 존재하지 않는 비목 거부")
    @WithUserDetails("company1")
    void registerSettlementRules() throws Exception {
        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "i.pdf"))
                        .param("applicationId", "1").param("itemCode", "EXP02").param("expenseDate", "2026-12-31")
                        .param("vendorName", "v").param("supplyAmount", "1000").param("description", "d").with(csrf()))
                .andExpect(flash().attribute("errorMessage", containsString("오늘 이후")));
        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "i.pdf"))
                        .param("applicationId", "1").param("itemCode", "EXP99").param("expenseDate", "2026-10-20")
                        .param("vendorName", "v").param("supplyAmount", "1000").param("description", "d").with(csrf()))
                .andExpect(flash().attribute("errorMessage", containsString("비목")));
    }

    @Test
    @DisplayName("정산 검수: 반려는 사유 필수, 처리 완료 건은 재처리 불가")
    @WithUserDetails("manager1")
    void reviewSettlement() throws Exception {
        mockMvc.perform(post("/settlements/2/review").with(csrf())
                        .param("applicationId", "1").param("status", "REJECTED").param("rejectReason", " "))
                .andExpect(flash().attribute("errorMessage", containsString("사유")));

        mockMvc.perform(post("/settlements/2/review").with(csrf())
                        .param("applicationId", "999").param("status", "REJECTED").param("rejectReason", "세금계산서 금액 불일치"))
                .andExpect(redirectedUrl("/settlements/application/1"));  // 화면 값(999)이 아닌 실제 과제로 이동
        Settlement reviewed = settlementMapper.selectSettlement(2L).orElseThrow();
        assertThat(reviewed.getStatus()).isEqualTo("REJECTED");
        assertThat(reviewed.getReviewedBy()).isEqualTo("manager1");

        mockMvc.perform(post("/settlements/2/review").with(csrf())
                        .param("applicationId", "1").param("status", "APPROVED"))
                .andExpect(flash().attribute("errorMessage", containsString("이미 검수")));
    }

    // ───────────── 배치 ─────────────

    @Test
    @DisplayName("마감 배치: 마감일이 지난 OPEN 공고만 CLOSED 로 전환")
    void closeExpiredAnnouncements() {
        Announcement expired = Announcement.builder()
                .title("어제 마감된 공고").fieldCode("AI_SW").totalBudget(1L).maxGrant(1L)
                .startDate(LocalDate.of(2026, 10, 1)).endDate(LocalDate.of(2026, 10, 20))
                .content("c").status("OPEN").writerId("manager1").build();
        announcementMapper.insertAnnouncement(expired);

        int closed = announcementService.closeExpiredAnnouncements();

        assertThat(closed).isEqualTo(1);
        assertThat(announcementMapper.selectAnnouncement(expired.getAnnouncementId()).orElseThrow().getStatus()).isEqualTo("CLOSED");
        assertThat(announcementMapper.selectAnnouncement(13L).orElseThrow().getStatus()).isEqualTo("OPEN");
    }

    // ───────────── 화면 렌더링 ─────────────

    @Test
    @DisplayName("상세/정산/채점 화면에 실제 데이터가 렌더링된다 (Thymeleaf 예약어 ${application} 오용 회귀 방지)")
    void detailPagesRenderData() throws Exception {
        mockMvc.perform(get("/applications/1").with(user("manager1").roles("MANAGER")))
                .andExpect(content().string(containsString("도심형 PBV 자율주행용 고정밀 조향센서")))
                .andExpect(content().string(containsString("125,000,000원")))
                .andExpect(content().string(containsString("88.5")));          // 평균 점수 (92 + 85) / 2
        mockMvc.perform(get("/settlements/application/1").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(containsString("(주)대구모빌리티솔루션")))
                .andExpect(content().string(containsString("59,300,000원")));  // 등록 가능 잔액
        mockMvc.perform(get("/evaluator/evaluations/3").with(user("evaluator1").roles("EVALUATOR")))
                .andExpect(content().string(containsString("북성로 공구골목")));
        mockMvc.perform(get("/applications/apply").param("announcementId", "15").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(containsString("name=\"announcementId\" value=\"15\"")));
    }

    @Test
    @DisplayName("엑셀 다운로드 (담당자)")
    @WithUserDetails("manager1")
    void excelDownload() throws Exception {
        mockMvc.perform(get("/excel/applications"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }
}
