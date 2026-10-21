package kr.go.daegu.pms.application.service.impl;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.cmm.service.FileVO;
import kr.go.daegu.pms.evaluation.service.impl.EvaluationMapper;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.cmm.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
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

    @Override
    public List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO) {
        applyDataScope(searchVO);
        return applicationMapper.selectApplicationList(searchVO);
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
            throw new AccessDeniedException("신청서 목록 조회 권한이 없습니다.");
        }
    }

    @Override
    public ApplicationVO selectApplication(Long applicationId) {
        return accessChecker.getReadableApplication(applicationId);
    }

    @Override
    public Long insertApplication(ApplicationVO application, MultipartFile planFile) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            throw new AccessDeniedException("기업회원만 사업을 신청할 수 있습니다.");
        }
        String companyUserId = SecurityUtil.getCurrentUserId();
        AnnouncementVO announcement = announcementService.selectAnnouncement(application.getAnnouncementId());

        // 1) 접수 가능한 공고인지 (상태 + 접수기간)
        LocalDate today = LocalDate.now(clock);
        if (!AnnouncementVO.STATUS_OPEN.equals(announcement.getStatus())
                || today.isBefore(announcement.getStartDate())
                || today.isAfter(announcement.getEndDate())) {
            throw new BusinessException("접수 기간이 아닌 공고입니다. (접수기간: "
                    + announcement.getStartDate() + " ~ " + announcement.getEndDate() + ")");
        }

        // 2) 금액 검증 (화면의 max 속성은 우회 가능하므로 서버에서 다시 검증)
        if (application.getRequestGrant() > application.getTotalBudget()) {
            throw new BusinessException("신청 지원금은 총 소요 예산을 초과할 수 없습니다.");
        }
        if (application.getRequestGrant() > announcement.getMaxGrant()) {
            throw new BusinessException("신청 지원금이 공고의 기업당 최대 지원금("
                    + String.format("%,d", announcement.getMaxGrant()) + "원)을 초과합니다.");
        }

        // 3) 중복 신청 (DB 유니크 제약도 함께 걸려 있어 동시 요청도 차단됨)
        if (applicationMapper.selectApplicationCountByCompany(announcement.getAnnouncementId(), companyUserId) > 0) {
            throw new BusinessException("이미 신청한 공고입니다. 한 공고에는 1회만 신청할 수 있습니다.");
        }

        // 4) 첨부파일 저장 후 신청서 등록 (등록 실패 시 트랜잭션 롤백 → 저장된 파일도 삭제됨)
        FileVO file = fileService.storeFile(planFile);

        application.setCompanyUserId(companyUserId);
        application.setSelfFunding(application.getTotalBudget() - application.getRequestGrant());
        application.setPlanFileId(file.getFileId());
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        try {
            applicationMapper.insertApplication(application);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("이미 신청한 공고입니다. 한 공고에는 1회만 신청할 수 있습니다.");
        }
        log.info("[과제 신청] applicationId={}, company={}", application.getApplicationId(), companyUserId);
        return application.getApplicationId();
    }

    @Override
    public void updateApplicationStatus(Long applicationId, String statusCode) {
        if (!SecurityUtil.isStaff()) {
            throw new AccessDeniedException("심사 상태 변경 권한이 없습니다.");
        }
        ApplicationStatus next = ApplicationStatus.fromCode(statusCode)
                .orElseThrow(() -> new BusinessException("올바르지 않은 진행상태 값입니다."));
        ApplicationVO application = accessChecker.getReadableApplication(applicationId);
        ApplicationStatus current = ApplicationStatus.fromCode(application.getStatus())
                .orElseThrow(() -> new IllegalStateException("알 수 없는 저장 상태: " + application.getStatus()));

        if (!current.manualNextStatuses().contains(next)) {
            throw new BusinessException("'" + current.getLabel() + "' 상태에서 '" + next.getLabel() + "' 상태로 변경할 수 없습니다.");
        }
        if (next == ApplicationStatus.SELECTED) {
            if (evaluationMapper.selectEvaluationCount(applicationId, null) == 0
                    || evaluationMapper.selectIncompleteEvaluationCount(applicationId) > 0) {
                throw new BusinessException("배정된 평가위원의 채점이 모두 완료되어야 최종 선정할 수 있습니다.");
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
}
