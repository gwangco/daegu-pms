package kr.go.daegu.pms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    private Long announcementId;
    private String title;
    private String fieldCode;
    private String fieldName; // 조인용
    private Long totalBudget;
    private Long maxGrant;
    private LocalDate startDate;
    private LocalDate endDate;
    private String content;
    private String status; // DRAFT, OPEN, CLOSED
    private int viewCount;
    private String writerId;
    private String writerName; // 조인용
    private LocalDateTime createdAt;
}
