package kr.go.daegu.pms.application.service;

import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.evaluation.service.impl.EvaluationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 신청서(과제) 단위 데이터 접근 권한 검사기.
 *
 * 시큐어코딩 - 부적절한 인가(IDOR) 방지:
 * URL 의 과제번호만 바꿔서(/applications/1 → /applications/2) 남의 사업계획서/정산내역/첨부파일에
 * 접근하는 것을 막기 위해, 데이터를 조회한 뒤 "로그인 사용자와 데이터의 관계"를 서버에서 다시 검사한다.
 *
 * 신청서·정산·파일 서비스가 공통으로 사용하므로 별도 컴포넌트로 분리했다.
 * (각 서비스가 서로를 참조하면 순환 의존이 생기기 때문)
 */
@Component
@RequiredArgsConstructor
public class ApplicationAccessChecker {

    private final ApplicationMapper applicationMapper;
    private final EvaluationMapper evaluationMapper;

    /**
     * 신청서 열람 권한: 담당자/관리자 = 전체, 기업 = 본인 신청서, 평가위원 = 배정받은 과제
     */
    public ApplicationVO getReadableApplication(Long applicationId) {
        ApplicationVO application = findApplication(applicationId);
        if (SecurityUtil.isStaff() || isOwner(application) || isAssignedEvaluator(applicationId)) {
            return application;
        }
        throw new AccessDeniedException("해당 과제에 대한 열람 권한이 없습니다.");
    }

    /**
     * 사업비 정산 접근 권한: 담당자/관리자 또는 과제 수행 기업 본인 (평가위원은 정산 정보 열람 불가)
     */
    public ApplicationVO getSettlementAccessibleApplication(Long applicationId) {
        ApplicationVO application = findApplication(applicationId);
        if (SecurityUtil.isStaff() || isOwner(application)) {
            return application;
        }
        throw new AccessDeniedException("해당 과제의 사업비 정산에 대한 접근 권한이 없습니다.");
    }

    public boolean isOwner(ApplicationVO application) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)
                && currentUserId != null
                && currentUserId.equals(application.getCompanyUserId());
    }

    private boolean isAssignedEvaluator(Long applicationId) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)
                && currentUserId != null
                && evaluationMapper.selectEvaluationCount(applicationId, currentUserId) > 0;
    }

    private ApplicationVO findApplication(Long applicationId) {
        return applicationMapper.selectApplication(applicationId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 신청서입니다."));
    }
}
