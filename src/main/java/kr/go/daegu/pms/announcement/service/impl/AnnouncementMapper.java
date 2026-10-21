package kr.go.daegu.pms.announcement.service.impl;

import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.announcement.service.AnnouncementSearchVO;
import kr.go.daegu.pms.announcement.service.FieldBudgetStatVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@EgovMapper
public interface AnnouncementMapper {
    List<AnnouncementVO> selectAnnouncementList(AnnouncementSearchVO searchVO);
    int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO);
    Optional<AnnouncementVO> selectAnnouncement(@Param("announcementId") Long announcementId);
    int insertAnnouncement(AnnouncementVO announcement);
    int updateViewCount(@Param("announcementId") Long announcementId);
    int selectActiveAnnouncementCount();
    List<FieldBudgetStatVO> selectFieldBudgetStatList();

    /** 접수 마감일이 지난 OPEN 공고를 CLOSED 로 일괄 전환 (배치) */
    int updateExpiredAnnouncementsToClosed(@Param("today") LocalDate today);
}
