package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EvaluationMapper {
    List<Evaluation> selectEvaluationsByEvaluator(@Param("evaluatorUserId") String evaluatorUserId);
    Optional<Evaluation> selectEvaluationById(@Param("evaluationId") Long evaluationId);
    Optional<Evaluation> selectEvaluationByAppAndEvaluator(
            @Param("applicationId") Long applicationId,
            @Param("evaluatorUserId") String evaluatorUserId);

    int insertEvaluation(Evaluation evaluation);
    int updateEvaluation(Evaluation evaluation);
}
