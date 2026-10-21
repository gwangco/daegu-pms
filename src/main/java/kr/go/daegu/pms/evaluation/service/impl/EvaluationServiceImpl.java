package kr.go.daegu.pms.evaluation.service.impl;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.evaluation.service.EvaluationVO;
import kr.go.daegu.pms.user.service.UserVO;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.user.service.impl.UserMapper;
import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("evaluationService")
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final EvaluationMapper evaluationMapper;
    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final ApplicationAccessChecker accessChecker;

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationVO> selectMyEvaluationList() {
        return evaluationMapper.selectEvaluationListByEvaluator(requireEvaluatorId());
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationVO selectMyEvaluation(Long evaluationId) {
        String evaluatorId = requireEvaluatorId();
        EvaluationVO evaluation = evaluationMapper.selectEvaluation(evaluationId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 평가 건입니다."));
        if (!evaluatorId.equals(evaluation.getEvaluatorUserId())) {
            throw new AccessDeniedException("본인에게 배정된 평가 건만 조회할 수 있습니다.");
        }
        return evaluation;
    }

    @Override
    @Transactional
    public void submitScore(Long evaluationId, int quantitativeScore, int qualitativeScore, String evalComment) {
        EvaluationVO evaluation = selectMyEvaluation(evaluationId);

        if (EvaluationVO.STATUS_COMPLETED.equals(evaluation.getStatus())) {
            throw new BusinessException("이미 제출된 평가입니다. 제출 후에는 수정할 수 없습니다.");
        }
        if (!ApplicationStatus.EVALUATING.name().equals(evaluation.getApplicationStatus())) {
            throw new BusinessException("전문가 심사 중인 과제만 채점할 수 있습니다.");
        }
        if (quantitativeScore < 0 || quantitativeScore > EvaluationVO.MAX_QUANTITATIVE) {
            throw new BusinessException("정량 점수는 0~" + EvaluationVO.MAX_QUANTITATIVE + "점 사이로 입력하세요.");
        }
        if (qualitativeScore < 0 || qualitativeScore > EvaluationVO.MAX_QUALITATIVE) {
            throw new BusinessException("정성 점수는 0~" + EvaluationVO.MAX_QUALITATIVE + "점 사이로 입력하세요.");
        }
        if (evalComment == null || evalComment.isBlank()) {
            throw new BusinessException("종합 의견을 입력하세요.");
        }
        if (evalComment.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException("종합 의견은 " + MAX_COMMENT_LENGTH + "자 이내로 입력하세요.");
        }

        evaluation.setQuantitativeScore(quantitativeScore);
        evaluation.setQualitativeScore(qualitativeScore);
        evaluation.setTotalScore(quantitativeScore + qualitativeScore);
        evaluation.setEvalComment(evalComment.trim());

        // UPDATE ... WHERE status='ASSIGNED' AND evaluator_user_id=본인 → 동시에 두 번 제출해도 한 번만 반영
        if (evaluationMapper.updateEvaluationScore(evaluation) == 0) {
            throw new BusinessException("이미 제출된 평가입니다.");
        }
        log.info("[평가 제출] evaluationId={}, applicationId={}, total={}",
                evaluationId, evaluation.getApplicationId(), evaluation.getTotalScore());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationVO> selectEvaluationListByApplication(Long applicationId) {
        requireStaff();
        return evaluationMapper.selectEvaluationListByApplication(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVO> selectAssignableEvaluatorList(Long applicationId) {
        requireStaff();
        Set<String> assigned = evaluationMapper.selectEvaluationListByApplication(applicationId).stream()
                .map(EvaluationVO::getEvaluatorUserId)
                .collect(Collectors.toSet());
        return userMapper.findByRole(SecurityUtil.ROLE_EVALUATOR).stream()
                .filter(UserVO::isEnabled)
                .filter(u -> !assigned.contains(u.getUserId()))
                .toList();
    }

    @Override
    @Transactional
    public void assignEvaluator(Long applicationId, String evaluatorUserId) {
        requireStaff();
        ApplicationVO application = accessChecker.getReadableApplication(applicationId);
        ApplicationStatus status = ApplicationStatus.fromCode(application.getStatus())
                .orElseThrow(() -> new IllegalStateException("알 수 없는 저장 상태: " + application.getStatus()));
        if (!status.isEvaluatorAssignable()) {
            throw new BusinessException("서류적격통과 또는 전문가심사중 상태의 과제에만 평가위원을 배정할 수 있습니다.");
        }

        UserVO evaluator = userMapper.findByUserId(evaluatorUserId)
                .filter(u -> SecurityUtil.ROLE_EVALUATOR.equals(u.getRole()) && u.isEnabled())
                .orElseThrow(() -> new BusinessException("올바르지 않은 평가위원입니다."));

        if (evaluationMapper.selectEvaluationCount(applicationId, evaluator.getUserId()) > 0) {
            throw new BusinessException("이미 배정된 평가위원입니다.");
        }
        try {
            evaluationMapper.insertEvaluation(EvaluationVO.builder()
                    .applicationId(applicationId)
                    .evaluatorUserId(evaluator.getUserId())
                    .build());
        } catch (DuplicateKeyException e) {
            throw new BusinessException("이미 배정된 평가위원입니다.");
        }

        if (status == ApplicationStatus.DOCUMENT_PASSED) {
            applicationMapper.updateApplicationStatus(applicationId, ApplicationStatus.EVALUATING.name());
        }
        log.info("[평가위원 배정] applicationId={}, evaluator={}, by={}",
                applicationId, evaluator.getUserId(), SecurityUtil.getCurrentUserId());
    }

    private String requireEvaluatorId() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            throw new AccessDeniedException("평가위원만 이용할 수 있습니다.");
        }
        return SecurityUtil.getCurrentUserId();
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw new AccessDeniedException("사업담당자만 이용할 수 있습니다.");
        }
    }
}
