package kr.go.daegu.pms.application.service.impl;

import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApplicationMapper {
    List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO);
    int selectApplicationListTotCnt(ApplicationSearchVO searchVO);
    Optional<ApplicationVO> selectApplication(@Param("applicationId") Long applicationId);

    /** 행 잠금 조회 (SELECT ... FOR UPDATE). 같은 과제에 대한 동시 정산 등록 시 예산 초과를 막기 위해 사용 */
    Optional<ApplicationVO> selectApplicationForUpdate(@Param("applicationId") Long applicationId);

    int selectApplicationCountByCompany(@Param("announcementId") Long announcementId,
                                        @Param("companyUserId") String companyUserId);
    int insertApplication(ApplicationVO application);
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("status") String status);
    int selectTotalApplicationCount();
    int selectSelectedApplicationCount();
}
