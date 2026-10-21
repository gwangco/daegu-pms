package kr.go.daegu.pms.application.service.impl;

import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@EgovMapper
public interface ApplicationMapper {
    List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO);
    int selectApplicationListTotCnt(ApplicationSearchVO searchVO);
    Optional<ApplicationVO> selectApplication(@Param("applicationId") Long applicationId);

    /** 행 잠금 조회. 상태 변경·평가 배정·채점·정산은 신청서 행을 먼저 잠가 업무 검증과 저장을 직렬화한다. */
    Optional<ApplicationVO> selectApplicationForUpdate(@Param("applicationId") Long applicationId);

    int selectApplicationCountByCompany(@Param("announcementId") Long announcementId,
                                        @Param("companyUserId") String companyUserId);
    int insertApplication(ApplicationVO application);
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("status") String status);
    int selectTotalApplicationCount();
    int selectSelectedApplicationCount();
}
