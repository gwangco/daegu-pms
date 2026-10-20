package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApplicationMapper {
    List<Application> selectApplications(
            @Param("announcementId") Long announcementId,
            @Param("companyUserId") String companyUserId,
            @Param("status") String status);

    Optional<Application> selectApplicationById(@Param("applicationId") Long applicationId);
    int insertApplication(Application application);
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("status") String status);
    int updateApplication(Application application);
    int countTotalApplications();
    int countSelectedApplications();
}
