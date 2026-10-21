package kr.go.daegu.pms.announcement.service;


import java.util.List;

/**
 * 지원사업 공고 서비스.
 * 전자정부 표준: Controller 는 이 인터페이스에만 의존하고, 실제 구현은 impl 패키지의 AnnouncementServiceImpl 이 담당한다.
 */
public interface AnnouncementService {

    List<AnnouncementVO> selectAnnouncementList(AnnouncementSearchVO searchVO);

    int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO);

    /** 공고 조회 (조회수 증가 없음 - 신청서 작성 화면 등 내부 용도) */
    AnnouncementVO selectAnnouncement(Long announcementId);

    /** 공고 상세 조회 (조회수 1 증가) */
    AnnouncementVO selectAnnouncementWithViewCount(Long announcementId);

    Long insertAnnouncement(AnnouncementVO announcement);

    int selectActiveAnnouncementCount();

    List<FieldBudgetStatVO> selectFieldBudgetStatList();

    /** 접수 마감일이 지난 공고를 마감 처리하고 처리 건수를 반환 (배치) */
    int closeExpiredAnnouncements();
}
