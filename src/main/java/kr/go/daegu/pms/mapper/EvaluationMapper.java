package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EvaluationMapper {
    List<Evaluation> selectEvaluationListByEvaluator(@Param("evaluatorUserId") String evaluatorUserId);
    List<Evaluation> selectEvaluationListByApplication(@Param("applicationId") Long applicationId);
    Optional<Evaluation> selectEvaluation(@Param("evaluationId") Long evaluationId);
    int selectEvaluationCount(@Param("applicationId") Long applicationId,
                              @Param("evaluatorUserId") String evaluatorUserId);
    int selectIncompleteEvaluationCount(@Param("applicationId") Long applicationId);
    int insertEvaluation(Evaluation evaluation);

    /** 채점 저장. ASSIGNED 상태이고 본인 배정 건일 때만 갱신되므로 반환값 0 이면 중복 제출/권한 없음 */
    int updateEvaluationScore(Evaluation evaluation);
}
