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
public class Settlement {
    private Long settlementId;
    private Long applicationId;
    private String itemCode;
    private String itemName;         // 조인용 (인건비, 재료비 등)
    private LocalDate expenseDate;
    private String vendorName;
    private Long supplyAmount;
    private Long vatAmount;
    private Long totalAmount;
    private String description;
    private String receiptFileName;
    private String status;           // REQUESTED, APPROVED, REJECTED
    private String rejectReason;
    private LocalDateTime createdAt;
}
