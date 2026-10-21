package kr.go.daegu.pms.evaluation.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationVO {

    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final int MAX_QUANTITATIVE = 40;
    public static final int MAX_QUALITATIVE = 60;

    private Long evaluationId;
    private Long applicationId;
    private String projectName;      // 조인용
    private String companyName;      // 조인용
    private String announcementTitle; // 조인용
    private String applicationStatus; // 조인용 (과제 진행상태)
    private String evaluatorUserId;
    private String evaluatorName;    // 조인용
    private int quantitativeScore;   // 40점 만점
    private int qualitativeScore;    // 60점 만점
    private int totalScore;          // 100점 만점
    private String evalComment;
    private String status;           // ASSIGNED, COMPLETED
    private LocalDateTime assignedAt;
    private LocalDateTime evaluatedAt;
}
