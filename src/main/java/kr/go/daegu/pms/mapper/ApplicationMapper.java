package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.domain.ApplicationSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApplicationMapper {
    List<Application> selectApplicationList(ApplicationSearchVO searchVO);
    int selectApplicationListTotCnt(ApplicationSearchVO searchVO);
    Optional<Application> selectApplication(@Param("applicationId") Long applicationId);

    /** 행 잠금 조회 (SELECT ... FOR UPDATE). 같은 과제에 대한 동시 정산 등록 시 예산 초과를 막기 위해 사용 */
    Optional<Application> selectApplicationForUpdate(@Param("applicationId") Long applicationId);

    int selectApplicationCountByCompany(@Param("announcementId") Long announcementId,
                                        @Param("companyUserId") String companyUserId);
    int insertApplication(Application application);
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("status") String status);
    int selectTotalApplicationCount();
    int selectSelectedApplicationCount();
}
