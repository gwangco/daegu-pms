package kr.go.daegu.pms.settlement.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 정산 처리 이력 (settlement_history) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementHistoryVO {

    public static final String ACTION_REQUESTED = "REQUESTED";
    public static final String ACTION_APPROVED = "APPROVED";
    public static final String ACTION_REJECTED = "REJECTED";
    public static final String ACTION_RESUBMITTED = "RESUBMITTED";

    private Long historyId;
    private Long settlementId;
    private String actionCode;
    private long totalAmount;
    private String memo;
    private String actorId;
    private String actorName;     // 조인용
    private LocalDateTime actedAt;

    public String getActionLabel() {
        if (actionCode == null) {
            return "";
        }
        return switch (actionCode) {
            case ACTION_REQUESTED -> "검수 요청";
            case ACTION_APPROVED -> "승인";
            case ACTION_REJECTED -> "보완 요청(반려)";
            case ACTION_RESUBMITTED -> "보완 후 재요청";
            default -> actionCode;
        };
    }
}
