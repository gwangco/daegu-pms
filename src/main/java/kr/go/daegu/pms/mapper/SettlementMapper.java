package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SettlementMapper {
    List<Settlement> selectSettlementList(@Param("applicationId") Long applicationId);
    Optional<Settlement> selectSettlement(@Param("settlementId") Long settlementId);
    int insertSettlement(Settlement settlement);

    /** 검수 처리. REQUESTED 상태일 때만 갱신되므로 반환값 0 이면 이미 처리된 건(중복 클릭/동시 처리) */
    int updateSettlementReview(@Param("settlementId") Long settlementId,
                               @Param("status") String status,
                               @Param("rejectReason") String rejectReason,
                               @Param("reviewedBy") String reviewedBy);

    /** 승인 완료된 누적 집행액 */
    Long selectApprovedAmount(@Param("applicationId") Long applicationId);

    /** 예산 소진액 (승인 + 검수대기). 반려 건은 제외 */
    Long selectUsedAmount(@Param("applicationId") Long applicationId);
}
