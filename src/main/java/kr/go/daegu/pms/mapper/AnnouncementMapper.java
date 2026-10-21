package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.domain.AnnouncementSearchVO;
import kr.go.daegu.pms.domain.FieldBudgetStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AnnouncementMapper {
    List<Announcement> selectAnnouncementList(AnnouncementSearchVO searchVO);
    int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO);
    Optional<Announcement> selectAnnouncement(@Param("announcementId") Long announcementId);
    int insertAnnouncement(Announcement announcement);
    int updateViewCount(@Param("announcementId") Long announcementId);
    int selectActiveAnnouncementCount();
    List<FieldBudgetStat> selectFieldBudgetStatList();

    /** 접수 마감일이 지난 OPEN 공고를 CLOSED 로 일괄 전환 (배치) */
    int updateExpiredAnnouncementsToClosed(@Param("today") LocalDate today);
}
