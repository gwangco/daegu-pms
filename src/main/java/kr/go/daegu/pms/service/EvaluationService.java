package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Evaluation;
import kr.go.daegu.pms.domain.User;

import java.util.List;

public interface EvaluationService {

    /** 로그인한 평가위원에게 배정된 평가 목록 */
    List<Evaluation> selectMyEvaluationList();

    /** 로그인한 평가위원 본인의 평가 건 (타인 평가 건이면 접근 거부) */
    Evaluation selectMyEvaluation(Long evaluationId);

    /** 채점 제출 (정량 40 + 정성 60). 제출 후에는 수정 불가 */
    void submitScore(Long evaluationId, int quantitativeScore, int qualitativeScore, String evalComment);

    /** 과제별 평가 현황 (담당자용) */
    List<Evaluation> selectEvaluationListByApplication(Long applicationId);

    /** 해당 과제에 아직 배정되지 않은 평가위원 목록 (담당자용) */
    List<User> selectAssignableEvaluatorList(Long applicationId);

    /** 평가위원 배정. 서류적격통과 과제는 배정 시 전문가심사중으로 자동 전환 */
    void assignEvaluator(Long applicationId, String evaluatorUserId);
}
