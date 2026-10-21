package kr.go.daegu.pms.settlement.service;

import kr.go.daegu.pms.application.service.ApplicationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SettlementService {

    /** 정산 접근 권한(담당자 또는 과제 수행 기업)을 검사한 뒤 과제 정보 반환 */
    ApplicationVO selectSettlementApplication(Long applicationId);

    List<SettlementVO> selectSettlementList(Long applicationId);

    /** 승인 완료 누적 집행액 */
    long selectApprovedAmount(Long applicationId);

    /** 예산 소진액 (승인 + 검수대기) */
    long selectUsedAmount(Long applicationId);

    /** 사업비 집행 등록 (기업). 협약 지원금 초과 집행 차단 */
    Long insertSettlement(SettlementVO settlement, MultipartFile receiptFile);

    /** 검수 승인/반려 (담당자). 처리된 정산 건의 과제 ID 반환 */
    Long reviewSettlement(Long settlementId, String statusCode, String rejectReason);
}
