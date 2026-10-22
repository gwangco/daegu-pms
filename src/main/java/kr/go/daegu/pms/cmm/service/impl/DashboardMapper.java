package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.DashboardCountVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.time.LocalDateTime;

/** 역할별 처리 대기 건수. 한 번의 조회로 여러 건수를 가져오도록 스칼라 서브쿼리를 쓴다 */
@EgovMapper
public interface DashboardMapper {

    DashboardCountVO selectCompanyTaskCounts(@Param("userId") String userId);

    int selectEvaluatorPendingCount(@Param("userId") String userId);

    DashboardCountVO selectStaffTaskCounts();

    DashboardCountVO selectAdminTaskCounts(@Param("lockedSince") LocalDateTime lockedSince,
                                           @Param("todayStart") LocalDateTime todayStart);
}
