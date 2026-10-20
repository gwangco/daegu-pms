package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SettlementMapper {
    List<Settlement> selectSettlementsByApplicationId(@Param("applicationId") Long applicationId);
    List<Settlement> selectAllSettlements(@Param("status") String status);
    Optional<Settlement> selectSettlementById(@Param("settlementId") Long settlementId);
    int insertSettlement(Settlement settlement);
    int updateSettlementStatus(
            @Param("settlementId") Long settlementId,
            @Param("status") String status,
            @Param("rejectReason") String rejectReason);
    Long calculateTotalExecutedAmount(@Param("applicationId") Long applicationId);
}
