package kr.go.daegu.pms.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static kr.go.daegu.pms.application.service.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    @DisplayName("정방향 전이만 허용된다")
    void forwardTransitions() {
        assertThat(SUBMITTED.canTransitionTo(DOCUMENT_PASSED)).isTrue();
        assertThat(DOCUMENT_PASSED.canTransitionTo(EVALUATING)).isTrue();
        assertThat(EVALUATING.canTransitionTo(SELECTED)).isTrue();
    }

    @Test
    @DisplayName("단계 건너뛰기, 역행, 종료 상태에서의 변경은 금지된다")
    void forbiddenTransitions() {
        assertThat(SUBMITTED.canTransitionTo(SELECTED)).isFalse();    // 심사 건너뛰고 선정
        assertThat(REJECTED.canTransitionTo(SELECTED)).isFalse();     // 탈락 → 선정 (역행)
        assertThat(SELECTED.nextStatuses()).isEmpty();                // 종료 상태
        assertThat(REJECTED.nextStatuses()).isEmpty();
    }

    @Test
    @DisplayName("EVALUATING 은 담당자가 직접 선택할 수 없고 평가위원 배정으로만 전환된다")
    void evaluatingIsNotManual() {
        assertThat(DOCUMENT_PASSED.manualNextStatuses()).containsExactly(REJECTED);
        assertThat(DOCUMENT_PASSED.isEvaluatorAssignable()).isTrue();
        assertThat(SUBMITTED.isEvaluatorAssignable()).isFalse();
    }

    @Test
    @DisplayName("임시저장은 기업이 제출하는 작성 중 상태: 담당자가 고를 다음 상태가 없고, 제출 취소는 서류 검토 전(접수완료)만")
    void draftAndWithdraw() {
        assertThat(TEMP_SAVED.canTransitionTo(SUBMITTED)).isTrue();
        assertThat(TEMP_SAVED.manualNextStatuses()).isEmpty();
        assertThat(SUBMITTED.manualNextStatuses()).doesNotContain(TEMP_SAVED);
        assertThat(TEMP_SAVED.isDraft()).isTrue();
        assertThat(SUBMITTED.isWithdrawable()).isTrue();
        assertThat(DOCUMENT_PASSED.isWithdrawable()).isFalse();
        assertThat(SELECTED.isWithdrawable()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 코드는 빈 Optional")
    void convertFromCode() {
        assertThat(fromCode("SELECTED")).contains(SELECTED);
        assertThat(fromCode("HACKED")).isEmpty();
        assertThat(fromCode(null)).isEmpty();
    }
}
