package kr.go.daegu.pms.settlement.service.impl;

import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.settlement.service.SettlementHistoryVO;
import kr.go.daegu.pms.settlement.service.SettlementSearchVO;
import kr.go.daegu.pms.settlement.service.SettlementService;
import kr.go.daegu.pms.settlement.service.SettlementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("settlementService")
@RequiredArgsConstructor
public class SettlementServiceImpl extends EgovAbstractServiceImpl implements SettlementService {

    private static final int MAX_REJECT_REASON_LENGTH = 255;

    private final SettlementMapper settlementMapper;
    private final ApplicationMapper applicationMapper;
    private final ApplicationAccessChecker accessChecker;
    private final CommonCodeService commonCodeService;
    private final FileService fileService;
    private final Clock clock;
    private final AppMessages messages;

    @Override
    public ApplicationVO selectSettlementApplication(Long applicationId) {
        ApplicationVO application = accessChecker.getSettlementAccessibleApplication(applicationId);
        if (!ApplicationStatus.SELECTED.name().equals(application.getStatus())) {
            throw messages.business("settlement.selection.required");
        }
        return application;
    }

    @Override
    public List<SettlementVO> selectSettlementList(SettlementSearchVO searchVO) {
        accessChecker.getSettlementAccessibleApplication(searchVO.getApplicationId());
        return settlementMapper.selectSettlementList(searchVO);
    }

    @Override
    public int selectSettlementListTotCnt(SettlementSearchVO searchVO) {
        accessChecker.getSettlementAccessibleApplication(searchVO.getApplicationId());
        return settlementMapper.selectSettlementListTotCnt(searchVO);
    }

    @Override
    public Map<Long, List<SettlementHistoryVO>> selectSettlementHistoryMap(Long applicationId, List<SettlementVO> settlements) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        // 다른 과제의 정산 ID 가 섞여 들어와도 이 과제의 정산 이력만 조회한다
        List<Long> ids = settlements.stream()
                .filter(s -> applicationId.equals(s.getApplicationId()))
                .map(SettlementVO::getSettlementId)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return settlementMapper.selectSettlementHistoryList(ids).stream()
                .collect(Collectors.groupingBy(SettlementHistoryVO::getSettlementId, LinkedHashMap::new, Collectors.toList()));
    }

    @Override
    public List<SettlementVO> selectSettlementReviewList(SettlementSearchVO searchVO) {
        requireStaff();
        searchVO.setApplicationId(null);
        return settlementMapper.selectSettlementReviewList(searchVO);
    }

    @Override
    public int selectSettlementReviewListTotCnt(SettlementSearchVO searchVO) {
        requireStaff();
        searchVO.setApplicationId(null);
        return settlementMapper.selectSettlementListTotCnt(searchVO);
    }

    @Override
    public long selectApprovedAmount(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectApprovedAmount(applicationId);
    }

    @Override
    public long selectUsedAmount(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectUsedAmount(applicationId);
    }

    @Override
    public Long insertSettlement(SettlementVO settlement, MultipartFile receiptFile) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            throw messages.denied("settlement.create.denied");
        }

        // 1) 과제 행을 잠그고(FOR UPDATE) 조회 → 같은 과제에 동시 등록이 들어와도 한 건씩 순서대로 예산 검증
        ApplicationVO locked = lockOwnSelectedApplication(settlement.getApplicationId());

        // 2) 입력값 검증과 금액 서버 재계산, 3) 예산 초과 검증
        validateAndCalculate(settlement);
        checkBudget(locked, settlement.getTotalAmount());

        // 4) 증빙 파일 저장 후 등록
        settlement.setReceiptAtchFileId(fileService.storeFile(receiptFile).getAtchFileId());
        settlement.setStatus(SettlementVO.STATUS_REQUESTED);
        settlementMapper.insertSettlement(settlement);
        recordHistory(settlement.getSettlementId(), SettlementHistoryVO.ACTION_REQUESTED, settlement.getTotalAmount(), null);
        log.info("[사업비 집행 등록] settlementId={}, applicationId={}, total={}",
                settlement.getSettlementId(), settlement.getApplicationId(), settlement.getTotalAmount());
        return settlement.getSettlementId();
    }

    @Override
    public Long reviewSettlement(Long settlementId, String statusCode, String rejectReason) {
        requireStaff();
        if (!SettlementVO.STATUS_APPROVED.equals(statusCode) && !SettlementVO.STATUS_REJECTED.equals(statusCode)) {
            throw messages.business("settlement.review.invalid");
        }
        SettlementVO settlement = settlementMapper.selectSettlement(settlementId)
                .orElseThrow(() -> messages.notFound("settlement.notFound"));

        String reason = null;
        if (SettlementVO.STATUS_REJECTED.equals(statusCode)) {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw messages.business("settlement.rejectReason.required");
            }
            reason = rejectReason.trim();
            if (reason.length() > MAX_REJECT_REASON_LENGTH) {
                throw messages.business("settlement.rejectReason.tooLong", MAX_REJECT_REASON_LENGTH);
            }
        }

        // UPDATE ... WHERE status='REQUESTED' → 이미 처리된 건은 0건 갱신 (중복 처리 방지)
        int updated = settlementMapper.updateSettlementReview(settlementId, statusCode, reason, SecurityUtil.getCurrentUserId());
        if (updated == 0) {
            throw messages.business("settlement.review.completed");
        }
        recordHistory(settlementId, statusCode, settlement.getTotalAmount(), reason);
        log.info("[정산 검수] settlementId={}, status={}, by={}", settlementId, statusCode, SecurityUtil.getCurrentUserId());
        return settlement.getApplicationId();
    }

    @Override
    public Long updateSettlementResubmit(Long settlementId, SettlementVO input, MultipartFile newReceiptFile) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            throw messages.denied("settlement.create.denied");
        }
        Long applicationId = settlementMapper.selectSettlement(settlementId)
                .orElseThrow(() -> messages.notFound("settlement.notFound"))
                .getApplicationId();

        // 등록과 같은 과제 행 잠금 → 잠근 뒤 정산 건을 다시 읽어 최신 상태로 판단한다
        ApplicationVO locked = lockOwnSelectedApplication(applicationId);
        SettlementVO current = settlementMapper.selectSettlement(settlementId)
                .orElseThrow(() -> messages.notFound("settlement.notFound"));
        if (!SettlementVO.STATUS_REJECTED.equals(current.getStatus())) {
            throw messages.business("settlement.resubmit.invalidStatus");
        }

        input.setSettlementId(settlementId);
        input.setApplicationId(applicationId);
        validateAndCalculate(input);
        // 반려 건은 소진액에 포함되지 않으므로 재요청 금액 전체를 잔액과 비교한다
        checkBudget(locked, input.getTotalAmount());

        boolean hasNewFile = newReceiptFile != null && !newReceiptFile.isEmpty();
        input.setReceiptAtchFileId(hasNewFile ? fileService.storeFile(newReceiptFile).getAtchFileId() : current.getReceiptAtchFileId());
        if (settlementMapper.updateSettlementResubmit(input) == 0) {
            throw messages.business("settlement.resubmit.invalidStatus");
        }
        recordHistory(settlementId, SettlementHistoryVO.ACTION_RESUBMITTED, input.getTotalAmount(), null);
        log.info("[정산 보완 재요청] settlementId={}, applicationId={}, total={}, newFile={}",
                settlementId, applicationId, input.getTotalAmount(), hasNewFile);
        return applicationId;
    }

    /** 과제 행을 잠그고 본인 과제·최종선정 상태인지 확인 (등록·재요청 공통) */
    private ApplicationVO lockOwnSelectedApplication(Long applicationId) {
        ApplicationVO locked = applicationMapper.selectApplicationForUpdate(applicationId)
                .orElseThrow(() -> messages.notFound("settlement.application.notFound"));
        if (!SecurityUtil.getCurrentUserId().equals(locked.getCompanyUserId())) {
            throw messages.denied("settlement.owner.denied");
        }
        if (!ApplicationStatus.SELECTED.name().equals(locked.getStatus())) {
            throw messages.business("settlement.expense.selection.required");
        }
        return locked;
    }

    /**
     * 비목·집행일자 검증과 금액 재계산 (부가세 10%, 원 단위 미만 절사).
     * 화면 값을 믿지 않고 @Valid 를 거치지 않는 서비스 호출도 방어하며, long 오버플로로 음수 집행액이 저장되지 않게 한다.
     */
    private void validateAndCalculate(SettlementVO settlement) {
        if (!commonCodeService.isValidCode(CommonCodeService.EXP_ITEM, settlement.getItemCode())) {
            throw messages.business("settlement.item.invalid");
        }
        if (settlement.getExpenseDate() == null) {
            throw messages.business("settlement.date.required");
        }
        if (settlement.getExpenseDate().isAfter(LocalDate.now(clock))) {
            throw messages.business("settlement.date.future");
        }
        if (settlement.getSupplyAmount() == null || settlement.getSupplyAmount() <= 0) {
            throw messages.business("settlement.supply.positive");
        }
        long supply = settlement.getSupplyAmount();
        long vat = supply / 10;
        long total;
        try {
            total = Math.addExact(supply, vat);
        } catch (ArithmeticException e) {
            throw messages.business("settlement.amount.tooLarge");
        }
        settlement.setVatAmount(vat);
        settlement.setTotalAmount(total);
    }

    /** (승인 + 검수대기 금액) + 이번 금액 <= 협약 지원금 */
    private void checkBudget(ApplicationVO locked, long total) {
        long used = settlementMapper.selectUsedAmount(locked.getApplicationId());
        long remaining = locked.getRequestGrant() - used;
        if (total > remaining) {
            throw messages.business("settlement.balance.exceeded", remaining, total);
        }
    }

    private void recordHistory(Long settlementId, String actionCode, long totalAmount, String memo) {
        settlementMapper.insertSettlementHistory(SettlementHistoryVO.builder()
                .settlementId(settlementId)
                .actionCode(actionCode)
                .totalAmount(totalAmount)
                .memo(memo)
                .build());
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("settlement.review.denied");
        }
    }
}
