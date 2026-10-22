package kr.go.daegu.pms.announcement.service.impl;

import kr.go.daegu.pms.announcement.service.AnnouncementSearchVO;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.announcement.service.FieldBudgetStatVO;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service("announcementService")
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends EgovAbstractServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CommonCodeService commonCodeService;
    private final Clock clock;
    private final AppMessages messages;

    @Override
    public List<AnnouncementVO> selectAnnouncementList(AnnouncementSearchVO searchVO) {
        return announcementMapper.selectAnnouncementList(searchVO);
    }

    @Override
    public int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO) {
        return announcementMapper.selectAnnouncementListTotCnt(searchVO);
    }

    @Override
    public AnnouncementVO selectAnnouncement(Long announcementId) {
        return announcementMapper.selectAnnouncement(announcementId)
                .orElseThrow(() -> messages.notFound("announcement.notFound"));
    }

    @Override
    public void updateViewCount(Long announcementId) {
        announcementMapper.updateViewCount(announcementId);
    }

    @Override
    public Long insertAnnouncement(AnnouncementVO announcement) {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("announcement.write.denied");
        }
        if (!commonCodeService.isValidCode(CommonCodeService.FIELD, announcement.getFieldCode())) {
            throw messages.business("announcement.field.invalid");
        }
        if (announcement.getEndDate().isBefore(announcement.getStartDate())) {
            throw messages.business("announcement.period.invalid");
        }
        if (announcement.getEndDate().isBefore(LocalDate.now(clock))) {
            throw messages.business("announcement.closed");
        }
        if (announcement.getMaxGrant() > announcement.getTotalBudget()) {
            throw messages.business("announcement.grant.exceeded");
        }

        // 작성자와 상태는 화면 입력값이 아니라 서버가 결정
        announcement.setWriterId(SecurityUtil.getCurrentUserId());
        announcement.setStatus(AnnouncementVO.STATUS_OPEN);
        announcementMapper.insertAnnouncement(announcement);
        return announcement.getAnnouncementId();
    }

    @Override
    public int selectActiveAnnouncementCount() {
        return announcementMapper.selectActiveAnnouncementCount();
    }

    @Override
    public List<FieldBudgetStatVO> selectFieldBudgetStatList() {
        return announcementMapper.selectFieldBudgetStatList();
    }

    @Override
    public int closeExpiredAnnouncements() {
        return announcementMapper.updateExpiredAnnouncementsToClosed(LocalDate.now(clock));
    }
}
