package kr.go.daegu.pms.announcement.service.impl;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.announcement.service.AnnouncementSearchVO;
import kr.go.daegu.pms.announcement.service.FieldBudgetStatVO;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service("announcementService")
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CommonCodeService commonCodeService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementVO> selectAnnouncementList(AnnouncementSearchVO searchVO) {
        return announcementMapper.selectAnnouncementList(searchVO);
    }

    @Override
    @Transactional(readOnly = true)
    public int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO) {
        return announcementMapper.selectAnnouncementListTotCnt(searchVO);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementVO selectAnnouncement(Long announcementId) {
        return announcementMapper.selectAnnouncement(announcementId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 공고입니다."));
    }

    @Override
    @Transactional
    public AnnouncementVO selectAnnouncementWithViewCount(Long announcementId) {
        AnnouncementVO announcement = selectAnnouncement(announcementId);
        announcementMapper.updateViewCount(announcementId);
        announcement.setViewCount(announcement.getViewCount() + 1);
        return announcement;
    }

    @Override
    @Transactional
    public Long insertAnnouncement(AnnouncementVO announcement) {
        if (!commonCodeService.isValidCode(CommonCodeService.GROUP_FIELD, announcement.getFieldCode())) {
            throw new BusinessException("올바르지 않은 지원 분야입니다.");
        }
        if (announcement.getEndDate().isBefore(announcement.getStartDate())) {
            throw new BusinessException("접수 마감일은 시작일 이후여야 합니다.");
        }
        if (announcement.getEndDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("접수 마감일이 이미 지난 공고는 등록할 수 없습니다.");
        }
        if (announcement.getMaxGrant() > announcement.getTotalBudget()) {
            throw new BusinessException("기업당 최대 지원금은 총 지원 예산을 초과할 수 없습니다.");
        }

        // 작성자와 상태는 화면 입력값이 아니라 서버가 결정
        announcement.setWriterId(SecurityUtil.getCurrentUserId());
        announcement.setStatus(AnnouncementVO.STATUS_OPEN);
        announcementMapper.insertAnnouncement(announcement);
        return announcement.getAnnouncementId();
    }

    @Override
    @Transactional(readOnly = true)
    public int selectActiveAnnouncementCount() {
        return announcementMapper.selectActiveAnnouncementCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldBudgetStatVO> selectFieldBudgetStatList() {
        return announcementMapper.selectFieldBudgetStatList();
    }

    @Override
    @Transactional
    public int closeExpiredAnnouncements() {
        return announcementMapper.updateExpiredAnnouncementsToClosed(LocalDate.now(clock));
    }
}
