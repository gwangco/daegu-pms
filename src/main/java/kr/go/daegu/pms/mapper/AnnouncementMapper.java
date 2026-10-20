package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AnnouncementMapper {
    List<Announcement> selectAllAnnouncements(@Param("fieldCode") String fieldCode, @Param("status") String status, @Param("keyword") String keyword);
    Optional<Announcement> selectAnnouncementById(@Param("announcementId") Long announcementId);
    int insertAnnouncement(Announcement announcement);
    int updateAnnouncement(Announcement announcement);
    int incrementViewCount(@Param("announcementId") Long announcementId);
    int countActiveAnnouncements();
}
