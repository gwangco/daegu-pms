package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Settlement;
import kr.go.daegu.pms.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementMapper settlementMapper;

    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsByApplication(Long applicationId) {
        return settlementMapper.selectSettlementsByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getAllSettlements(String status) {
        return settlementMapper.selectAllSettlements(status);
    }

    @Transactional
    public void registerExpense(Settlement settlement) {
        if (settlement.getSupplyAmount() == null || settlement.getSupplyAmount() <= 0) {
            throw new IllegalArgumentException("공급가액은 0원보다 커야 합니다.");
        }
        if (settlement.getVatAmount() == null) {
            settlement.setVatAmount((long) (settlement.getSupplyAmount() * 0.1));
        }
        settlement.setTotalAmount(settlement.getSupplyAmount() + settlement.getVatAmount());
        settlement.setStatus("REQUESTED");
        settlementMapper.insertSettlement(settlement);
    }

    @Transactional
    public void approveOrReject(Long settlementId, String status, String rejectReason) {
        settlementMapper.updateSettlementStatus(settlementId, status, rejectReason);
    }

    @Transactional(readOnly = true)
    public Long getTotalApprovedAmount(Long applicationId) {
        return settlementMapper.calculateTotalExecutedAmount(applicationId);
    }
}
