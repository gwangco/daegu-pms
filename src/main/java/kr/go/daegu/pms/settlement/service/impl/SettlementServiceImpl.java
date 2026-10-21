package kr.go.daegu.pms.settlement.service.impl;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.cmm.service.FileVO;
import kr.go.daegu.pms.settlement.service.SettlementVO;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service("settlementService")
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    private static final int MAX_REJECT_REASON_LENGTH = 255;

    private final SettlementMapper settlementMapper;
    private final ApplicationMapper applicationMapper;
    private final ApplicationAccessChecker accessChecker;
    private final CommonCodeService commonCodeService;
    private final FileService fileService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApplicationVO selectSettlementApplication(Long applicationId) {
        ApplicationVO application = accessChecker.getSettlementAccessibleApplication(applicationId);
        if (!ApplicationStatus.SELECTED.name().equals(application.getStatus())) {
            throw new BusinessException("최종 선정(협약)된 과제만 사업비 정산을 이용할 수 있습니다.");
        }
        return application;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementVO> selectSettlementList(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectSettlementList(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long selectApprovedAmount(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectApprovedAmount(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long selectUsedAmount(Long applicationId) {
        accessChecker.getSettlementAccessibleApplication(applicationId);
        return settlementMapper.selectUsedAmount(applicationId);
    }

    @Override
    @Transactional
    public Long insertSettlement(SettlementVO settlement, MultipartFile receiptFile) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            throw new AccessDeniedException("과제 수행 기업만 사업비를 집행 등록할 수 있습니다.");
        }

        // 1) 과제 행을 잠그고(FOR UPDATE) 조회 → 같은 과제에 동시 등록이 들어와도 한 건씩 순서대로 예산 검증
        ApplicationVO locked = applicationMapper.selectApplicationForUpdate(settlement.getApplicationId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 과제입니다."));
        if (!SecurityUtil.getCurrentUserId().equals(locked.getCompanyUserId())) {
            throw new AccessDeniedException("본인 과제에만 사업비를 집행 등록할 수 있습니다.");
        }
        if (!ApplicationStatus.SELECTED.name().equals(locked.getStatus())) {
            throw new BusinessException("최종 선정(협약)된 과제만 사업비를 집행할 수 있습니다.");
        }

        // 2) 입력값 업무 검증
        if (!commonCodeService.isValidCode(CommonCodeService.GROUP_EXP_ITEM, settlement.getItemCode())) {
            throw new BusinessException("올바르지 않은 비목입니다.");
        }
        if (settlement.getExpenseDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException("집행일자는 오늘 이후로 등록할 수 없습니다.");
        }

        // 3) 금액은 화면 값을 믿지 않고 서버에서 재계산 (부가세 10%, 원 단위 미만 절사)
        long supply = settlement.getSupplyAmount();
        long vat = supply / 10;
        long total = supply + vat;
        settlement.setVatAmount(vat);
        settlement.setTotalAmount(total);

        // 4) 예산 초과 검증: (승인 + 검수대기 금액) + 이번 등록 금액 <= 협약 지원금
        long used = settlementMapper.selectUsedAmount(locked.getApplicationId());
        long remaining = locked.getRequestGrant() - used;
        if (total > remaining) {
            throw new BusinessException(String.format(
                    "협약 지원금 잔액을 초과합니다. (등록 가능 잔액: %,d원 / 이번 집행액: %,d원)", remaining, total));
        }

        // 5) 증빙 파일 저장 후 등록
        FileVO file = fileService.storeFile(receiptFile);
        settlement.setReceiptFileId(file.getFileId());
        settlement.setStatus(STATUS_REQUESTED);
        settlementMapper.insertSettlement(settlement);
        log.info("[사업비 집행 등록] settlementId={}, applicationId={}, total={}",
                settlement.getSettlementId(), settlement.getApplicationId(), total);
        return settlement.getSettlementId();
    }

    @Override
    @Transactional
    public Long reviewSettlement(Long settlementId, String statusCode, String rejectReason) {
        if (!SecurityUtil.isStaff()) {
            throw new AccessDeniedException("사업담당자만 정산을 검수할 수 있습니다.");
        }
        if (!STATUS_APPROVED.equals(statusCode) && !STATUS_REJECTED.equals(statusCode)) {
            throw new BusinessException("올바르지 않은 검수 처리 값입니다.");
        }
        SettlementVO settlement = settlementMapper.selectSettlement(settlementId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 정산 건입니다."));

        String reason = null;
        if (STATUS_REJECTED.equals(statusCode)) {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new BusinessException("반려 시에는 보완 요청 사유를 반드시 입력해야 합니다.");
            }
            reason = rejectReason.trim();
            if (reason.length() > MAX_REJECT_REASON_LENGTH) {
                throw new BusinessException("반려 사유는 " + MAX_REJECT_REASON_LENGTH + "자 이내로 입력하세요.");
            }
        }

        // UPDATE ... WHERE status='REQUESTED' → 이미 처리된 건은 0건 갱신 (중복 처리 방지)
        int updated = settlementMapper.updateSettlementReview(settlementId, statusCode, reason, SecurityUtil.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException("이미 검수 처리된 정산 건입니다.");
        }
        log.info("[정산 검수] settlementId={}, status={}, by={}", settlementId, statusCode, SecurityUtil.getCurrentUserId());
        return settlement.getApplicationId();
    }
}
