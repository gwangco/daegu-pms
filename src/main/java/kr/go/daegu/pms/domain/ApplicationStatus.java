package kr.go.daegu.pms.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * 사업신청서 진행상태 상태머신.
 *
 *  SUBMITTED ──서류검토──▶ DOCUMENT_PASSED ──평가위원 배정──▶ EVALUATING ──▶ SELECTED
 *      │                       │                               │
 *      └──────────────────────┴───────────────────────────────┴──▶ REJECTED
 *
 * - 각 상태에서 이동 가능한 "다음 상태"만 허용하여 "탈락 → 선정" 같은 역행을 막는다.
 * - EVALUATING 은 담당자가 직접 선택하지 않고 "평가위원 배정" 시 시스템이 자동 전환한다.
 * - SELECTED / REJECTED 는 종료 상태(더 이상 변경 불가)이다.
 * - TEMP_SAVED(임시저장)는 공통코드에만 정의되어 있고 이번 단계에서는 사용하지 않는다.
 */
public enum ApplicationStatus {

    TEMP_SAVED("임시저장"),
    SUBMITTED("접수완료"),
    DOCUMENT_PASSED("서류적격통과"),
    EVALUATING("전문가심사중"),
    SELECTED("최종선정(협약)"),
    REJECTED("심사탈락");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 이 상태에서 이동 가능한 다음 상태 목록 */
    public Set<ApplicationStatus> nextStatuses() {
        return switch (this) {
            case TEMP_SAVED -> EnumSet.of(SUBMITTED);
            case SUBMITTED -> EnumSet.of(DOCUMENT_PASSED, REJECTED);
            case DOCUMENT_PASSED -> EnumSet.of(EVALUATING, REJECTED);
            case EVALUATING -> EnumSet.of(SELECTED, REJECTED);
            case SELECTED, REJECTED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canTransitionTo(ApplicationStatus next) {
        return nextStatuses().contains(next);
    }

    /** 담당자가 화면에서 "직접" 선택할 수 있는 다음 상태 (EVALUATING 은 평가위원 배정으로만 전환) */
    public Set<ApplicationStatus> manualNextStatuses() {
        Set<ApplicationStatus> result = EnumSet.noneOf(ApplicationStatus.class);
        result.addAll(nextStatuses());
        result.remove(EVALUATING);
        return result;
    }

    /** 평가위원을 배정할 수 있는 상태인가 */
    public boolean isEvaluatorAssignable() {
        return this == DOCUMENT_PASSED || this == EVALUATING;
    }

    /** 문자열 코드 → enum 변환. 존재하지 않는 코드면 빈 Optional (예외 대신 호출 측에서 업무 메시지 처리) */
    public static Optional<ApplicationStatus> fromCode(String code) {
        return Arrays.stream(values()).filter(s -> s.name().equals(code)).findFirst();
    }
}
