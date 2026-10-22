package kr.go.daegu.pms.application.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationVO {
    private Long applicationId;
    private String receiptNo;          // 대외 공개용 접수번호 (eGov IdGnr 채번, 서버에서만 설정)

    @NotNull(message = "신청할 공고 정보가 없습니다.")
    private Long announcementId;
    private String announcementTitle; // 조인용
    private String companyUserId;
    private String companyName;       // 조인용
    private String businessNo;        // 조인용

    @NotBlank(message = "과제명을 입력하세요.")
    @Size(max = 200, message = "과제명은 200자 이내로 입력하세요.")
    private String projectName;

    @NotNull(message = "총 소요 예산을 입력하세요.")
    @Positive(message = "총 소요 예산은 0원보다 커야 합니다.")
    private Long totalBudget;

    @NotNull(message = "신청 지원금을 입력하세요.")
    @Positive(message = "신청 지원금은 0원보다 커야 합니다.")
    private Long requestGrant;

    private Long selfFunding;         // 서버에서 (총예산 - 지원금)으로 재계산

    @NotBlank(message = "사업계획 요약을 입력하세요.")
    @Size(max = 4000, message = "사업계획 요약은 4000자 이내로 입력하세요.")
    private String summary;

    private String planAtchFileId;    // 사업계획서 첨부파일 ID (공통컴포넌트 COMTNFILE)
    private String planFileName;      // 조인용 (첨부파일 원본명)
    private String status; // TEMP_SAVED, SUBMITTED, DOCUMENT_PASSED, EVALUATING, SELECTED, REJECTED
    private String statusName; // 조인용
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;

    // 평가 완료된 점수의 평균 (평가 전이면 null)
    private Double avgScore;
}
