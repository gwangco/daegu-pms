package kr.go.daegu.pms.settlement.service.impl;

import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.service.FileVO;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.settlement.service.SettlementService;
import kr.go.daegu.pms.settlement.service.SettlementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

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
    public List<SettlementVO> selectSettlementList(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectSettlementList(applicationId);
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
        ApplicationVO locked = applicationMapper.selectApplicationForUpdate(settlement.getApplicationId())
                .orElseThrow(() -> messages.notFound("settlement.application.notFound"));
        if (!SecurityUtil.getCurrentUserId().equals(locked.getCompanyUserId())) {
            throw messages.denied("settlement.owner.denied");
        }
        if (!ApplicationStatus.SELECTED.name().equals(locked.getStatus())) {
            throw messages.business("settlement.expense.selection.required");
        }

        // 2) 입력값 업무 검증
        if (!commonCodeService.isValidCode(CommonCodeService.GROUP_EXP_ITEM, settlement.getItemCode())) {
            throw messages.business("settlement.item.invalid");
        }
        if (settlement.getExpenseDate().isAfter(LocalDate.now(clock))) {
            throw messages.business("settlement.date.future");
        }

        // 3) 금액은 화면 값을 믿지 않고 서버에서 재계산 (부가세 10%, 원 단위 미만 절사)
        // @Valid를 거치지 않는 서비스 호출도 방어하며, long 오버플로로 음수 집행액이 저장되지 않게 한다.
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

        // 4) 예산 초과 검증: (승인 + 검수대기 금액) + 이번 등록 금액 <= 협약 지원금
        long used = settlementMapper.selectUsedAmount(locked.getApplicationId());
        long remaining = locked.getRequestGrant() - used;
        if (total > remaining) {
            throw messages.business("settlement.balance.exceeded", remaining, total);
        }

        // 5) 증빙 파일 저장 후 등록
        FileVO file = fileService.storeFile(receiptFile);
        settlement.setReceiptFileId(file.getFileId());
        settlement.setStatus(SettlementVO.STATUS_REQUESTED);
        settlementMapper.insertSettlement(settlement);
        log.info("[사업비 집행 등록] settlementId={}, applicationId={}, total={}",
                settlement.getSettlementId(), settlement.getApplicationId(), total);
        return settlement.getSettlementId();
    }

    @Override
    public Long reviewSettlement(Long settlementId, String statusCode, String rejectReason) {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("settlement.review.denied");
        }
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
        log.info("[정산 검수] settlementId={}, status={}, by={}", settlementId, statusCode, SecurityUtil.getCurrentUserId());
        return settlement.getApplicationId();
    }
}
