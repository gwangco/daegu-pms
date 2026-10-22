package kr.go.daegu.pms.settlement.service;

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
public class SettlementVO {

    /** 검수 상태: 검수요청 / 승인 / 반려(보완 요청) */
    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private Long settlementId;

    @NotNull(message = "과제 정보가 없습니다.")
    private Long applicationId;

    @NotBlank(message = "비목을 선택하세요.")
    private String itemCode;
    private String itemName;         // 조인용 (인건비, 재료비 등)

    @NotNull(message = "집행일자를 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    @NotBlank(message = "거래처명을 입력하세요.")
    @Size(max = 100, message = "거래처명은 100자 이내로 입력하세요.")
    private String vendorName;

    @NotNull(message = "공급가액을 입력하세요.")
    @Positive(message = "공급가액은 0원보다 커야 합니다.")
    private Long supplyAmount;
    private Long vatAmount;          // 서버에서 공급가액의 10%로 재계산
    private Long totalAmount;        // 서버에서 공급가액 + 부가세로 재계산

    @NotBlank(message = "집행 내용을 입력하세요.")
    @Size(max = 255, message = "집행 내용은 255자 이내로 입력하세요.")
    private String description;

    private Long receiptFileId;      // 증빙 첨부파일 ID
    private String receiptFileName;  // 조인용 (첨부파일 원본명)
    private String status;           // REQUESTED, APPROVED, REJECTED
    private String rejectReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
