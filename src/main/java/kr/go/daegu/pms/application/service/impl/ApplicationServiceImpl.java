package kr.go.daegu.pms.application.service.impl;

import egovframework.com.cmm.service.FileVO;
import jakarta.annotation.Resource;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.util.MaskingUtil;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.evaluation.service.impl.EvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service("applicationService")
@RequiredArgsConstructor
public class ApplicationServiceImpl extends EgovAbstractServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final EvaluationMapper evaluationMapper;
    private final AnnouncementService announcementService;
    private final FileService fileService;
    private final ApplicationAccessChecker accessChecker;
    private final Clock clock;
    private final AppMessages messages;

    /**
     * 접수번호 채번 서비스 (EgovConfigIdGeneration 의 receiptNoIdGnrService).
     * 같은 타입(EgovIdGnrService)의 Bean 이 여러 개일 수 있으므로 eGov 관례대로 @Resource(name) 으로 이름을 지정해 주입한다.
     */
    @Resource(name = "receiptNoIdGnrService")
    private EgovIdGnrService receiptNoIdGnrService;

    @Override
    public List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO) {
        applyDataScope(searchVO);
        return applicationMapper.selectApplicationList(searchVO).stream()
                .map(this::maskForEvaluator)
                .toList();
    }

    @Override
    public int selectApplicationListTotCnt(ApplicationSearchVO searchVO) {
        applyDataScope(searchVO);
        return applicationMapper.selectApplicationListTotCnt(searchVO);
    }

    /**
     * 로그인 사용자 권한에 따라 조회 범위를 강제한다. 화면에서 넘어온 값은 무시하고 항상 덮어쓴다.
     */
    private void applyDataScope(ApplicationSearchVO searchVO) {
        searchVO.setCompanyUserId(null);
        searchVO.setEvaluatorUserId(null);
        if (SecurityUtil.isStaff()) {
            return;
        }
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            searchVO.setCompanyUserId(currentUserId);
        } else if (SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            searchVO.setEvaluatorUserId(currentUserId);
        } else {
            throw messages.denied("application.list.denied");
        }
    }

    @Override
    public ApplicationVO selectApplication(Long applicationId) {
        return maskForEvaluator(accessChecker.getReadableApplication(applicationId));
    }

    /**
     * 평가위원은 심사에 사업자등록번호 전체가 필요하지 않으므로 가린 값만 준다 (개인정보 최소 처리).
     * 화면에서 가리지 않고 서버에서 바꿔 내려야 HTML 소스에도 원본이 남지 않는다.
     *
     * 조회 결과 객체를 직접 고치지 않고 복사본을 만든다. MyBatis 는 같은 트랜잭션(SqlSession) 안의 같은 조회에
     * 1차 캐시의 같은 객체를 돌려주므로, 원본을 고치면 뒤이은 조회도 가려진 값을 받게 된다.
     */
    private ApplicationVO maskForEvaluator(ApplicationVO application) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            return application;
        }
        return application.toBuilder().businessNo(MaskingUtil.businessNo(application.getBusinessNo())).build();
    }

    @Override
    public Long insertApplication(ApplicationVO application, MultipartFile planFile) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            throw messages.denied("application.create.denied");
        }
        String companyUserId = SecurityUtil.getCurrentUserId();
        AnnouncementVO announcement = announcementService.selectAnnouncement(application.getAnnouncementId());

        // 1) 접수 가능한 공고인지 (상태 + 접수기간)
        LocalDate today = LocalDate.now(clock);
        if (!AnnouncementVO.STATUS_OPEN.equals(announcement.getStatus())
                || today.isBefore(announcement.getStartDate())
                || today.isAfter(announcement.getEndDate())) {
            throw messages.business("application.period.closed", announcement.getStartDate(), announcement.getEndDate());
        }

        // 2) 금액 검증 (화면의 max 속성은 우회 가능하므로 서버에서 다시 검증)
        if (application.getRequestGrant() > application.getTotalBudget()) {
            throw messages.business("application.budget.exceeded");
        }
        if (application.getRequestGrant() > announcement.getMaxGrant()) {
            throw messages.business("application.grant.exceeded", announcement.getMaxGrant());
        }

        // 3) 중복 신청 (DB 유니크 제약도 함께 걸려 있어 동시 요청도 차단됨)
        if (applicationMapper.selectApplicationCountByCompany(announcement.getAnnouncementId(), companyUserId) > 0) {
            throw messages.business("application.duplicate");
        }

        // 4) 첨부파일 저장 후 신청서 등록 (등록 실패 시 트랜잭션 롤백 → 저장된 파일도 삭제됨)
        FileVO file = fileService.storeFile(planFile);

        application.setCompanyUserId(companyUserId);
        application.setSelfFunding(application.getTotalBudget() - application.getRequestGrant());
        application.setPlanAtchFileId(file.getAtchFileId());
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        application.setReceiptNo(nextReceiptNo());
        try {
            applicationMapper.insertApplication(application);
        } catch (DuplicateKeyException e) {
            throw messages.business("application.duplicate");
        }
        log.info("[과제 신청] applicationId={}, company={}", application.getApplicationId(), companyUserId);
        return application.getApplicationId();
    }

    @Override
    public void updateApplicationStatus(Long applicationId, String statusCode) {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("application.status.denied");
        }
        ApplicationStatus next = ApplicationStatus.fromCode(statusCode)
                .orElseThrow(() -> messages.business("application.status.invalid"));
        // 심사·배정·채점은 같은 신청서 행을 먼저 잠근다. 검증 이후 다른 요청이 상태를 바꾸지 못하게 한다.
        ApplicationVO application = applicationMapper.selectApplicationForUpdate(applicationId)
                .orElseThrow(() -> messages.notFound("application.notFound"));
        ApplicationStatus current = ApplicationStatus.fromCode(application.getStatus())
                .orElseThrow(() -> new IllegalStateException("알 수 없는 저장 상태: " + application.getStatus()));

        if (!current.manualNextStatuses().contains(next)) {
            throw messages.business("application.status.transitionDenied", current.getLabel(), next.getLabel());
        }
        if (next == ApplicationStatus.SELECTED) {
            if (evaluationMapper.selectEvaluationCount(applicationId, null) == 0
                    || evaluationMapper.selectIncompleteEvaluationCount(applicationId) > 0) {
                throw messages.business("application.evaluation.incomplete");
            }
        }
        applicationMapper.updateApplicationStatus(applicationId, next.name());
        log.info("[심사 상태 변경] applicationId={}, {} -> {}, by={}", applicationId, current, next, SecurityUtil.getCurrentUserId());
    }

    @Override
    public int selectTotalApplicationCount() {
        return applicationMapper.selectTotalApplicationCount();
    }

    @Override
    public int selectSelectedApplicationCount() {
        return applicationMapper.selectSelectedApplicationCount();
    }

    /** 접수번호 발급 (예: DPMS-00000004). 채번 실패는 시스템 장애이므로 eGov 예외 후처리 대상이 된다 */
    private String nextReceiptNo() {
        try {
            return receiptNoIdGnrService.getNextStringId();
        } catch (FdlException e) {
            throw new BaseRuntimeException("접수번호 채번에 실패했습니다.", e);
        }
    }
}
