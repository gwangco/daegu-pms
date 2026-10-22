package kr.go.daegu.pms.evaluation.service.impl;

import kr.go.daegu.pms.evaluation.service.EvaluationVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Optional;

@EgovMapper
public interface EvaluationMapper {
    List<EvaluationVO> selectEvaluationListByEvaluator(@Param("evaluatorUserId") String evaluatorUserId);
    List<EvaluationVO> selectEvaluationListByApplication(@Param("applicationId") Long applicationId);
    Optional<EvaluationVO> selectEvaluation(@Param("evaluationId") Long evaluationId);
    int selectEvaluationCount(@Param("applicationId") Long applicationId,
                              @Param("evaluatorUserId") String evaluatorUserId);
    int selectIncompleteEvaluationCount(@Param("applicationId") Long applicationId);
    int insertEvaluation(EvaluationVO evaluation);

    /** 채점 저장. ASSIGNED 상태이고 본인 배정 건일 때만 갱신되므로 반환값 0 이면 중복 제출/권한 없음 */
    int updateEvaluationScore(EvaluationVO evaluation);
}
