package kr.go.daegu.pms.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    private Long announcementId;

    @NotBlank(message = "공고명을 입력하세요.")
    @Size(max = 200, message = "공고명은 200자 이내로 입력하세요.")
    private String title;

    @NotBlank(message = "지원 분야를 선택하세요.")
    private String fieldCode;
    private String fieldName; // 조인용

    @NotNull(message = "총 지원 예산을 입력하세요.")
    @Positive(message = "총 지원 예산은 0원보다 커야 합니다.")
    private Long totalBudget;

    @NotNull(message = "기업당 최대 지원금을 입력하세요.")
    @Positive(message = "기업당 최대 지원금은 0원보다 커야 합니다.")
    private Long maxGrant;

    @NotNull(message = "접수 시작일을 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "접수 마감일을 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank(message = "공고 내용을 입력하세요.")
    @Size(max = 4000, message = "공고 내용은 4000자 이내로 입력하세요.")
    private String content;

    private String status; // DRAFT, OPEN, CLOSED
    private int viewCount;
    private String writerId;
    private String writerName; // 조인용
    private LocalDateTime createdAt;
}
