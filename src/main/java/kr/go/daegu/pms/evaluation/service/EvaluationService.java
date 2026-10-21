package kr.go.daegu.pms.evaluation.service;

import kr.go.daegu.pms.user.service.UserVO;

import java.util.List;

public interface EvaluationService {

    /** 로그인한 평가위원에게 배정된 평가 목록 */
    List<EvaluationVO> selectMyEvaluationList();

    /** 로그인한 평가위원 본인의 평가 건 (타인 평가 건이면 접근 거부) */
    EvaluationVO selectMyEvaluation(Long evaluationId);

    /** 채점 제출 (정량 40 + 정성 60). 제출 후에는 수정 불가 */
    void submitScore(Long evaluationId, int quantitativeScore, int qualitativeScore, String evalComment);

    /** 과제별 평가 현황 (담당자용) */
    List<EvaluationVO> selectEvaluationListByApplication(Long applicationId);

    /** 해당 과제에 아직 배정되지 않은 평가위원 목록 (담당자용) */
    List<UserVO> selectAssignableEvaluatorList(Long applicationId);

    /** 평가위원 배정. 서류적격통과 과제는 배정 시 전문가심사중으로 자동 전환 */
    void assignEvaluator(Long applicationId, String evaluatorUserId);
}
