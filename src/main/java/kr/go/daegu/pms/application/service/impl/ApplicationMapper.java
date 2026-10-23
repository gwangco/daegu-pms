package kr.go.daegu.pms.application.service.impl;

import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

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

    /** 임시저장 새로 쓰기 (접수번호·제출 일시 없음) */
    int insertApplicationDraft(ApplicationVO application);

    /** 임시저장 내용 고치기 (임시저장 상태일 때만) */
    int updateApplicationDraft(ApplicationVO application);

    /** 임시저장 → 접수완료 (접수번호·제출 일시 기록, 임시저장 상태일 때만) */
    int updateApplicationSubmit(ApplicationVO application);

    /** 접수완료 → 임시저장 (제출 일시 비움, 접수번호는 유지. 접수완료 상태일 때만) */
    int updateApplicationWithdraw(@Param("applicationId") Long applicationId);

    int deleteApplicationDraft(@Param("applicationId") Long applicationId);

    /** 이 공고에 낸 기업의 신청서 번호·상태 */
    Optional<ApplicationVO> selectApplicationByCompany(@Param("announcementId") Long announcementId,
                                                     @Param("companyUserId") String companyUserId);
    int updateApplicationStatus(@Param("applicationId") Long applicationId, @Param("status") String status);
    int selectTotalApplicationCount();
    int selectSelectedApplicationCount();
}
