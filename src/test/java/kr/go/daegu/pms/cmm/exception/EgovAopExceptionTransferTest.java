package kr.go.daegu.pms.cmm.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class EgovAopExceptionTransferTest {

    @Test
    @DisplayName("업무 흐름상 예상된 예외는 장애 후처리 대상이 아니다")
    void expectedBusinessFlow() {
        assertThat(EgovAopExceptionTransfer.isExpectedBusinessFlow(new BusinessException("잔액 초과"))).isTrue();
        assertThat(EgovAopExceptionTransfer.isExpectedBusinessFlow(new NotFoundException("없음"))).isTrue();
        assertThat(EgovAopExceptionTransfer.isExpectedBusinessFlow(new AccessDeniedException("권한 없음"))).isTrue();
    }

    @Test
    @DisplayName("DB 오류·NPE 같은 예상치 못한 예외는 eGov ExceptionTransfer 로 넘겨 장애 로그를 남긴다")
    void unexpectedFailure() {
        assertThat(EgovAopExceptionTransfer.isExpectedBusinessFlow(new DataIntegrityViolationException("x"))).isFalse();
        assertThat(EgovAopExceptionTransfer.isExpectedBusinessFlow(new NullPointerException())).isFalse();
    }
}
