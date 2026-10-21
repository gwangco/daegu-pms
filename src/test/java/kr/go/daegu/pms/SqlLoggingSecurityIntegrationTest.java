package kr.go.daegu.pms;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 MyBatis INSERT/UPDATE를 실행하고, SQL 인자와 업무 내용이 기본 로그로 나오지 않는지 검증. */
class SqlLoggingSecurityIntegrationTest extends IntegrationTestSupport {

    private final List<String> events = new CopyOnWriteArrayList<>();
    private LoggerContext context;
    private LoggerConfig applicationLogger;
    private AbstractAppender capture;

    @BeforeEach
    void captureApplicationLogsWithoutChangingLevel() {
        context = (LoggerContext) LogManager.getContext(false);
        applicationLogger = context.getConfiguration().getLoggerConfig("kr.go.daegu.pms");
        capture = new AbstractAppender("sensitive-data-test", null,
                PatternLayout.newBuilder().withPattern("%m %ex").build(), false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                events.add(getLayout().toSerializable(event).toString());
            }
        };
        capture.start();
        applicationLogger.addAppender(capture, Level.ALL, null);
        context.updateLoggers();
    }

    @AfterEach
    void removeCapture() {
        applicationLogger.removeAppender(capture.getName());
        capture.stop();
        context.updateLoggers();
    }

    @Test
    @DisplayName("SQL DEBUG/TRACE 기본 비활성화: 신청 요약·평가 의견·정산 적요·반려 사유 비노출")
    void sensitiveInputsDoNotAppearInDefaultLogs() throws Exception {
        for (String mapper : List.of("application.service.impl.ApplicationMapper",
                "evaluation.service.impl.EvaluationMapper", "settlement.service.impl.SettlementMapper",
                "user.service.impl.UserMapper", "cmm.service.impl.FileMapper")) {
            assertThat(LogManager.getLogger("kr.go.daegu.pms." + mapper).isDebugEnabled())
                    .as(mapper + " must not log SQL parameters").isFalse();
        }

        mockMvc.perform(multipart("/applications/apply").file(pdf("planFile", "PRIVATE_FILE_CANARY.pdf"))
                        .with(csrf()).with(user("company1").roles("COMPANY"))
                        .param("announcementId", "15").param("projectName", "PRIVATE_PROJECT_CANARY")
                        .param("totalBudget", "50000000").param("requestGrant", "40000000")
                        .param("summary", "PRIVATE_SUMMARY_CANARY"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(post("/evaluator/evaluations/3").with(csrf())
                        .with(user("evaluator1").roles("EVALUATOR"))
                        .param("quantitativeScore", "30").param("qualitativeScore", "50")
                        .param("evalComment", "PRIVATE_EVALUATION_CANARY"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "receipt.pdf"))
                        .with(csrf()).with(user("company1").roles("COMPANY"))
                        .param("applicationId", "1").param("itemCode", "EXP02")
                        .param("expenseDate", "2026-10-20").param("supplyAmount", "1000")
                        .param("vendorName", "PRIVATE_VENDOR_CANARY").param("description", "PRIVATE_EXPENSE_CANARY"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(post("/settlements/2/review").with(csrf())
                        .with(user("manager1").roles("MANAGER"))
                        .param("applicationId", "1").param("status", "REJECTED")
                        .param("rejectReason", "PRIVATE_REJECTION_CANARY"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));

        String output = String.join("\n", events);
        // INFO 이벤트는 남아야 한다. 로그를 모두 끄는 것으로 이 테스트를 통과시킬 수 없다.
        assertThat(output).contains("[과제 신청]", "[사업비 집행 등록]", "[정산 검수]")
                .doesNotContain("PRIVATE_", "Parameters:", "Preparing:");
    }
}
