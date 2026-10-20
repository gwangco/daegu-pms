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
public class Evaluation {
    private Long evaluationId;
    private Long applicationId;
    private String projectName;      // 조인용
    private String companyName;      // 조인용
    private String evaluatorUserId;
    private String evaluatorName;    // 조인용
    private int quantitativeScore;   // 40점 만점
    private int qualitativeScore;    // 60점 만점
    private int totalScore;          // 100점 만점
    private String evalComment;
    private String status;           // ASSIGNED, COMPLETED
    private LocalDateTime evaluatedAt;
}
