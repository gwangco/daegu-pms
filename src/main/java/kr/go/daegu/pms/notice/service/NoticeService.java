package kr.go.daegu.pms.notice.service;

import java.util.List;

/**
 * 공지사항 서비스 (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정)
 */
public interface NoticeService {

    /** 공지 목록 (중요 공지 우선, 삭제글 제외) */
    List<NoticeVO> selectNoticeList(NoticeVO searchVO);

    int selectNoticeListTotCnt(NoticeVO searchVO);

    /** 공지 상세 (없거나 삭제된 글이면 NotFoundException) */
    NoticeVO selectNotice(String noticeId);

    /** 조회수 1 증가 (select* 는 읽기 전용 트랜잭션이므로 update* 로 분리) */
    void updateNoticeViewCount(String noticeId);

    /** 공지 등록 (담당자/관리자). 발급된 공지 ID 반환 */
    String insertNotice(NoticeVO notice);

    /** 공지 수정 (담당자/관리자). 제목·내용·중요여부만 변경 가능 */
    void updateNotice(NoticeVO notice);

    /** 공지 삭제 (담당자/관리자). 이력 보존을 위해 논리 삭제 */
    void deleteNotice(String noticeId);
}
