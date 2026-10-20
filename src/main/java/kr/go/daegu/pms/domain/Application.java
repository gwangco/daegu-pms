package kr.go.daegu.pms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    private Long applicationId;
    private Long announcementId;
    private String announcementTitle; // 조인용
    private String companyUserId;
    private String companyName;       // 조인용
    private String businessNo;        // 조인용
    private String projectName;
    private Long totalBudget;
    private Long requestGrant;
    private Long selfFunding;
    private String summary;
    private String planFileName;
    private String status; // TEMP_SAVED, SUBMITTED, DOCUMENT_PASSED, EVALUATING, SELECTED, REJECTED
    private String statusName; // 조인용
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;

    // 평가 점수 (있을 경우)
    private Integer totalScore;
}
