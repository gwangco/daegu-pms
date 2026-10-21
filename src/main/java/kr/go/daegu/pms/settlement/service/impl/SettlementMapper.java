package kr.go.daegu.pms.settlement.service.impl;

import kr.go.daegu.pms.settlement.service.SettlementVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@EgovMapper
public interface SettlementMapper {
    List<SettlementVO> selectSettlementList(@Param("applicationId") Long applicationId);
    Optional<SettlementVO> selectSettlement(@Param("settlementId") Long settlementId);
    int insertSettlement(SettlementVO settlement);

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
