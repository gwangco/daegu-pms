package kr.go.daegu.pms.settlement.service;

import kr.go.daegu.pms.application.service.ApplicationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SettlementService {

    /** 정산 접근 권한(담당자 또는 과제 수행 기업)을 검사한 뒤 과제 정보 반환 */
    ApplicationVO selectSettlementApplication(Long applicationId);

    /** 과제별 정산 목록 (searchVO.applicationId 필수, eGov 페이징) */
    List<SettlementVO> selectSettlementList(SettlementSearchVO searchVO);

    int selectSettlementListTotCnt(SettlementSearchVO searchVO);

    /** 화면에 보이는 정산 건들의 처리 이력 (정산 ID → 이력 목록) */
    Map<Long, List<SettlementHistoryVO>> selectSettlementHistoryMap(Long applicationId, List<SettlementVO> settlements);

    /** 담당자 검수함: 전체 과제의 정산 목록 */
    List<SettlementVO> selectSettlementReviewList(SettlementSearchVO searchVO);

    int selectSettlementReviewListTotCnt(SettlementSearchVO searchVO);

    /** 승인 완료 누적 집행액 */
    long selectApprovedAmount(Long applicationId);

    /** 예산 소진액 (승인 + 검수대기) */
    long selectUsedAmount(Long applicationId);

    /** 사업비 집행 등록 (기업). 협약 지원금 초과 집행 차단 */
    Long insertSettlement(SettlementVO settlement, MultipartFile receiptFile);

    /** 검수 승인/반려 (담당자). 처리된 정산 건의 과제 ID 반환 */
    Long reviewSettlement(Long settlementId, String statusCode, String rejectReason);

    /**
     * 반려(보완 요청)된 정산을 고쳐서 다시 검수 요청 (기업). 새 증빙을 올리지 않으면 기존 증빙을 유지한다.
     * @return 정산 건의 과제 ID
     */
    Long updateSettlementResubmit(Long settlementId, SettlementVO input, MultipartFile newReceiptFile);
}
