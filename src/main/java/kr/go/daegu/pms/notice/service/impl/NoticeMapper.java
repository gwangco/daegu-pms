package kr.go.daegu.pms.notice.service.impl;

import kr.go.daegu.pms.notice.service.NoticeVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Optional;

/**
 * 공지사항 Mapper (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정)
 */
@EgovMapper("noticeMapper")
public interface NoticeMapper {

    List<NoticeVO> selectNoticeList(NoticeVO searchVO);

    int selectNoticeListTotCnt(NoticeVO searchVO);

    Optional<NoticeVO> selectNotice(@Param("noticeId") String noticeId);

    int insertNotice(NoticeVO notice);

    /** 제목·내용·중요여부·수정자·수정일시만 갱신 (작성자/작성일/조회수는 바꿀 수 없음) */
    int updateNotice(NoticeVO notice);

    int updateNoticeViewCount(@Param("noticeId") String noticeId);

    /** 논리 삭제 (use_yn = 'N') */
    int deleteNotice(@Param("noticeId") String noticeId, @Param("updatedBy") String updatedBy);
}
