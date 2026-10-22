package kr.go.daegu.pms.evaluation.service.impl;

import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.impl.ApplicationMapper;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import kr.go.daegu.pms.evaluation.service.EvaluationVO;
import kr.go.daegu.pms.user.service.UserVO;
import kr.go.daegu.pms.user.service.impl.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("evaluationService")
@RequiredArgsConstructor
public class EvaluationServiceImpl extends EgovAbstractServiceImpl implements EvaluationService {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final EvaluationMapper evaluationMapper;
    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final AppMessages messages;

    @Override
    public List<EvaluationVO> selectMyEvaluationList() {
        return evaluationMapper.selectEvaluationListByEvaluator(requireEvaluatorId());
    }

    @Override
    public EvaluationVO selectMyEvaluation(Long evaluationId) {
        String evaluatorId = requireEvaluatorId();
        EvaluationVO evaluation = evaluationMapper.selectEvaluation(evaluationId)
                .orElseThrow(() -> messages.notFound("evaluation.notFound"));
        if (!evaluatorId.equals(evaluation.getEvaluatorUserId())) {
            throw messages.denied("evaluation.read.denied");
        }
        return evaluation;
    }

    @Override
    public void submitScore(Long evaluationId, int quantitativeScore, int qualitativeScore, String evalComment) {
        EvaluationVO evaluation = selectMyEvaluation(evaluationId);

        // 인가 후 신청서 → 평가표 순서로 잠근다. 탈락/선정과 채점이 동시에 진행되지 않게 한다.
        ApplicationVO application = applicationMapper.selectApplicationForUpdate(evaluation.getApplicationId())
                .orElseThrow(() -> messages.notFound("application.notFound"));

        if (EvaluationVO.STATUS_COMPLETED.equals(evaluation.getStatus())) {
            throw messages.business("evaluation.completed");
        }
        if (!ApplicationStatus.EVALUATING.name().equals(application.getStatus())) {
            throw messages.business("evaluation.status.invalid");
        }
        if (quantitativeScore < 0 || quantitativeScore > EvaluationVO.MAX_QUANTITATIVE) {
            throw messages.business("evaluation.quantitative.range", EvaluationVO.MAX_QUANTITATIVE);
        }
        if (qualitativeScore < 0 || qualitativeScore > EvaluationVO.MAX_QUALITATIVE) {
            throw messages.business("evaluation.qualitative.range", EvaluationVO.MAX_QUALITATIVE);
        }
        if (evalComment == null || evalComment.isBlank()) {
            throw messages.business("evaluation.comment.required");
        }
        if (evalComment.length() > MAX_COMMENT_LENGTH) {
            throw messages.business("evaluation.comment.tooLong", MAX_COMMENT_LENGTH);
        }

        evaluation.setQuantitativeScore(quantitativeScore);
        evaluation.setQualitativeScore(qualitativeScore);
        evaluation.setTotalScore(quantitativeScore + qualitativeScore);
        evaluation.setEvalComment(evalComment.trim());

        // UPDATE ... WHERE status='ASSIGNED' AND evaluator_user_id=본인 → 동시에 두 번 제출해도 한 번만 반영
        if (evaluationMapper.updateEvaluationScore(evaluation) == 0) {
            throw messages.business("evaluation.duplicateSubmission");
        }
        log.info("[평가 제출] evaluationId={}, applicationId={}, total={}",
                evaluationId, evaluation.getApplicationId(), evaluation.getTotalScore());
    }

    @Override
    public List<EvaluationVO> selectEvaluationListByApplication(Long applicationId) {
        requireStaff();
        return evaluationMapper.selectEvaluationListByApplication(applicationId);
    }

    @Override
    public List<UserVO> selectAssignableEvaluatorList(Long applicationId) {
        requireStaff();
        Set<String> assigned = evaluationMapper.selectEvaluationListByApplication(applicationId).stream()
                .map(EvaluationVO::getEvaluatorUserId)
                .collect(Collectors.toSet());
        return userMapper.selectUserListByRole(SecurityUtil.ROLE_EVALUATOR).stream()
                .filter(UserVO::isEnabled)
                .filter(u -> !assigned.contains(u.getUserId()))
                .toList();
    }

    @Override
    public void assignEvaluator(Long applicationId, String evaluatorUserId) {
        requireStaff();
        // 최종 선정의 미완료 평가 검사와 신규 배정이 교차하지 않도록 동일한 신청서 행을 잠근다.
        ApplicationVO application = applicationMapper.selectApplicationForUpdate(applicationId)
                .orElseThrow(() -> messages.notFound("application.notFound"));
        ApplicationStatus status = ApplicationStatus.fromCode(application.getStatus())
                .orElseThrow(() -> new IllegalStateException("알 수 없는 저장 상태: " + application.getStatus()));
        if (!status.isEvaluatorAssignable()) {
            throw messages.business("evaluation.assignment.invalidStatus");
        }

        UserVO evaluator = userMapper.selectUser(evaluatorUserId)
                .filter(u -> SecurityUtil.ROLE_EVALUATOR.equals(u.getRole()) && u.isEnabled())
                .orElseThrow(() -> messages.business("evaluation.evaluator.invalid"));

        if (evaluationMapper.selectEvaluationCount(applicationId, evaluator.getUserId()) > 0) {
            throw messages.business("evaluation.assignment.duplicate");
        }
        try {
            evaluationMapper.insertEvaluation(EvaluationVO.builder()
                    .applicationId(applicationId)
                    .evaluatorUserId(evaluator.getUserId())
                    .build());
        } catch (DuplicateKeyException e) {
            throw messages.business("evaluation.assignment.duplicate");
        }

        if (status == ApplicationStatus.DOCUMENT_PASSED) {
            applicationMapper.updateApplicationStatus(applicationId, ApplicationStatus.EVALUATING.name());
        }
        log.info("[평가위원 배정] applicationId={}, evaluator={}, by={}",
                applicationId, evaluator.getUserId(), SecurityUtil.getCurrentUserId());
    }

    private String requireEvaluatorId() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            throw messages.denied("evaluation.evaluator.denied");
        }
        return SecurityUtil.getCurrentUserId();
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("evaluation.staff.denied");
        }
    }
}
