package kr.go.daegu.pms;

import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서로 다른 스레드·DB 커넥션·트랜잭션의 경쟁을 검증한다.
 * 상위 테스트의 자동 롤백을 해제하고 전용 과제를 생성/삭제하므로 시드 데이터는 변경하지 않는다.
 * 실제 잠금 대기 확인은 DatabaseLockWait 가 맡는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@WithMockUser(username = "manager1", roles = "MANAGER")
class WorkflowConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Autowired ApplicationService applicationService;
    @Autowired EvaluationService evaluationService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private ExecutorService executor;
    private Long applicationId;
    private Long evaluationId;

    @BeforeEach
    void createDedicatedApplication() {
        executor = Executors.newSingleThreadExecutor();
        jdbc.update("""
                INSERT INTO application (receipt_no, announcement_id, company_user_id, project_name,
                    total_budget, request_grant, self_funding, summary, status)
                VALUES ('TEST-CONCURRENCY', 15, 'company2', '경쟁 상태 회귀 테스트',
                    1000000, 900000, 100000, '테스트 전용 과제', 'EVALUATING')
                """);
        applicationId = jdbc.queryForObject("SELECT application_id FROM application WHERE receipt_no = 'TEST-CONCURRENCY'", Long.class);
        jdbc.update("""
                INSERT INTO evaluation (application_id, evaluator_user_id, quantitative_score,
                    qualitative_score, total_score, status)
                VALUES (?, 'evaluator1', 40, 60, 100, 'COMPLETED')
                """, applicationId);
        evaluationId = jdbc.queryForObject("SELECT evaluation_id FROM evaluation WHERE application_id = ?", Long.class, applicationId);
    }

    @AfterEach
    void removeDedicatedApplication() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        if (applicationId != null) {
            jdbc.update("DELETE FROM evaluation WHERE application_id = ?", applicationId);
            jdbc.update("DELETE FROM application WHERE application_id = ?", applicationId);
        }
    }

    @Test
    @DisplayName("탈락과 선정이 겹쳐도 먼저 확정된 종료 상태를 덮어쓰지 않는다")
    void selectedCannotOverwriteConcurrentRejection() throws Exception {
        assertConflictRejected(
                () -> applicationService.updateApplicationStatus(applicationId, "REJECTED"),
                "manager1", "ROLE_MANAGER",
                () -> applicationService.updateApplicationStatus(applicationId, "SELECTED"));
        assertThat(applicationStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("최종 선정 중 들어온 신규 평가위원 배정은 확정 상태 재검사 후 차단한다")
    void cannotAssignEvaluatorAfterConcurrentSelection() throws Exception {
        assertConflictRejected(
                () -> applicationService.updateApplicationStatus(applicationId, "SELECTED"),
                "manager1", "ROLE_MANAGER",
                () -> evaluationService.assignEvaluator(applicationId, "evaluator2"));
        assertThat(applicationStatus()).isEqualTo("SELECTED");
        assertThat(evaluationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("신규 평가위원 배정 중 들어온 최종 선정은 미완료 평가를 확인해 차단한다")
    void cannotSelectWhileNewEvaluationIsPending() throws Exception {
        assertConflictRejected(
                () -> evaluationService.assignEvaluator(applicationId, "evaluator2"),
                "manager1", "ROLE_MANAGER",
                () -> applicationService.updateApplicationStatus(applicationId, "SELECTED"));
        assertThat(applicationStatus()).isEqualTo("EVALUATING");
        assertThat(evaluationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("탈락 처리 중 들어온 채점은 신청서 최신 상태를 확인하고 점수를 저장하지 않는다")
    void cannotScoreAfterConcurrentRejection() throws Exception {
        jdbc.update("UPDATE evaluation SET status = 'ASSIGNED', quantitative_score = 0, qualitative_score = 0, total_score = 0 WHERE evaluation_id = ?", evaluationId);
        assertConflictRejected(
                () -> applicationService.updateApplicationStatus(applicationId, "REJECTED"),
                "evaluator1", "ROLE_EVALUATOR",
                () -> evaluationService.submitScore(evaluationId, 40, 60, "동시 채점 시도"));
        assertThat(applicationStatus()).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("SELECT status FROM evaluation WHERE evaluation_id = ?", String.class, evaluationId))
                .isEqualTo("ASSIGNED");
        assertThat(jdbc.queryForObject("SELECT total_score FROM evaluation WHERE evaluation_id = ?", Integer.class, evaluationId))
                .isZero();
    }

    private void assertConflictRejected(Runnable firstChange, String user, String role, Runnable competingChange) throws Exception {
        AtomicBoolean waitedOnDatabaseLock = new AtomicBoolean();
        Future<Throwable> competing = new TransactionTemplate(txManager).execute(status -> {
            // 첫 업무 변경은 같은 트랜잭션 안에서 실행되고 이 콜백 종료까지 커밋하지 않는다.
            firstChange.run();
            Future<Throwable> future = DatabaseLockWait.submitAs(executor, user, role, competingChange);
            waitedOnDatabaseLock.set(DatabaseLockWait.awaitLockWaitOrCompletion(jdbc, future));
            return future;
        });
        assertThat(competing).isNotNull();
        Throwable outcome = competing.get(15, TimeUnit.SECONDS);
        assertThat(waitedOnDatabaseLock).as("두 번째 요청이 첫 요청의 신청서 행 잠금을 실제로 기다림").isTrue();
        assertThat(outcome).as("먼저 확정된 상태를 다시 읽고 업무 오류로 거부함")
                .isInstanceOf(BusinessException.class);
    }

    private String applicationStatus() {
        return jdbc.queryForObject("SELECT status FROM application WHERE application_id = ?", String.class, applicationId);
    }

    private Integer evaluationCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM evaluation WHERE application_id = ?", Integer.class, applicationId);
    }
}
