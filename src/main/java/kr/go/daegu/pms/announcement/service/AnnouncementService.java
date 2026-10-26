package kr.go.daegu.pms.announcement.service;


import java.util.List;

/**
 * 지원사업 공고 서비스.
 * 전자정부 표준: Controller 는 이 인터페이스에만 의존하고, 실제 구현은 impl 패키지의 AnnouncementServiceImpl 이 담당한다.
 */
public interface AnnouncementService {

    List<AnnouncementVO> selectAnnouncementList(AnnouncementSearchVO searchVO);

    int selectAnnouncementListTotCnt(AnnouncementSearchVO searchVO);

    /** 공고 조회 (존재하지 않으면 NotFoundException) */
    AnnouncementVO selectAnnouncement(Long announcementId);

    /**
     * 조회수 1 증가.
     * eGov 트랜잭션 규칙상 select* 메서드는 읽기 전용이므로, 데이터를 바꾸는 조회수 증가는 update* 로 분리한다.
     * (eGov 게시판 공통컴포넌트의 updateInqireCo + selectBoardArticle 구성과 동일)
     */
    void updateViewCount(Long announcementId);

    Long insertAnnouncement(AnnouncementVO announcement);

    int selectActiveAnnouncementCount();

    List<FieldBudgetStatVO> selectFieldBudgetStatList();

    /** 접수 마감일이 지난 공고를 마감 처리하고 처리 건수를 반환 (배치) */
}
