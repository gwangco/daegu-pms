package kr.go.daegu.pms.cmm.service;

import lombok.Data;

import java.time.LocalDateTime;

/** 배치 실행 이력 한 건 (Spring Batch 메타데이터 BATCH_JOB_EXECUTION + 파라미터 + 스텝 건수) */
@Data
public class BatchExecutionVO {
    private Long executionId;
    private String jobName;
    private String targetDate;      // 잡 파라미터 (기준일)
    private String requestedBy;     // 잡 파라미터 (수동 실행한 관리자, 정기 실행은 비어 있음)
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;          // COMPLETED, FAILED, STARTED …
    private String exitCode;
    private long readCount;
    private long writeCount;

    public String getStatusLabel() {
        return statusLabel(status);
    }

    /** Spring Batch 실행 상태(BatchStatus 이름)를 화면용 한글로 */
    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "COMPLETED" -> "완료";
            case "FAILED" -> "실패";
            case "STARTED", "STARTING" -> "실행 중";
            case "STOPPED", "STOPPING" -> "중지";
            default -> status;
        };
    }

    public String getJobLabel() {
        return "announcementCloseJob".equals(jobName) ? "공고 접수 마감" : jobName;
    }
}
