package kr.go.daegu.pms.cmm.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BatchExecutionVOTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 26, 0, 0);

    @ParameterizedTest(name = "{0}ms → {1}")
    @CsvSource({
            "80, 1초 미만",
            "1000, 1초",
            "59999, 59초",
            "60000, 1분 0초",
            "125000, 2분 5초"
    })
    @DisplayName("소요 시간은 1초 미만, 초, 분·초로 보여 준다")
    void durationLabel(long millis, String expected) {
        BatchExecutionVO execution = new BatchExecutionVO();
        execution.setStartTime(START);
        execution.setEndTime(START.plusNanos(millis * 1_000_000));
        assertThat(execution.getDurationLabel()).isEqualTo(expected);
    }

    @Test
    @DisplayName("끝나지 않은 실행은 소요 시간이 없고, 상태는 한글로 바꾼다")
    void runningExecution() {
        BatchExecutionVO execution = new BatchExecutionVO();
        execution.setStartTime(START);
        execution.setStatus("STARTED");
        assertThat(execution.getDurationLabel()).isNull();
        assertThat(execution.getStatusLabel()).isEqualTo("실행 중");
        assertThat(BatchExecutionVO.statusLabel("FAILED")).isEqualTo("실패");
        assertThat(BatchExecutionVO.statusLabel("ABANDONED")).isEqualTo("ABANDONED");
    }
}
