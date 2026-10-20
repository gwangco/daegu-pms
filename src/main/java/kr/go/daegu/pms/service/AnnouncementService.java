package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncements(String fieldCode, String status, String keyword) {
        return announcementMapper.selectAllAnnouncements(fieldCode, status, keyword);
    }

    @Transactional
    public Announcement getAnnouncementDetail(Long announcementId) {
        announcementMapper.incrementViewCount(announcementId);
        return announcementMapper.selectAnnouncementById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다. ID: " + announcementId));
    }

    @Transactional
    public void createAnnouncement(Announcement announcement) {
        announcementMapper.insertAnnouncement(announcement);
    }

    @Transactional(readOnly = true)
    public int getActiveCount() {
        return announcementMapper.countActiveAnnouncements();
    }
}
