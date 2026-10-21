package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.domain.AnnouncementSearchVO;
import kr.go.daegu.pms.domain.FieldBudgetStat;

import java.util.List;

/**
 * 지원사업 공고 서비스.
 * 전자정부 표준: Controller 는 이 인터페이스에만 의존하고, 실제 구현은 impl 패키지의 AnnouncementServiceImpl 이 담당한다.
 */
public interface AnnouncementService {

    List<Announcement> selectAnnouncementList(AnnouncementSearchVO searchVO);

    int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO);

    /** 공고 조회 (조회수 증가 없음 - 신청서 작성 화면 등 내부 용도) */
    Announcement selectAnnouncement(Long announcementId);

    /** 공고 상세 조회 (조회수 1 증가) */
    Announcement selectAnnouncementWithViewCount(Long announcementId);

    Long insertAnnouncement(Announcement announcement);

    int selectActiveAnnouncementCount();

    List<FieldBudgetStat> selectFieldBudgetStatList();

    /** 접수 마감일이 지난 공고를 마감 처리하고 처리 건수를 반환 (배치) */
    int closeExpiredAnnouncements();
}
