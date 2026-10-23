package kr.go.daegu.pms.notice.service.impl;

import egovframework.com.cop.bbs.service.BoardVO;
import kr.go.daegu.pms.notice.service.NoticeAttachPolicyVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

/**
 * 공지 첨부에 필요한 조회 (공통컴포넌트 게시판 SQL 에 없는 것만).
 * 게시물 등록·수정·조회는 공통컴포넌트 EgovArticleService 가 담당한다.
 */
@EgovMapper
public interface NoticeMapper {

    /** 게시판 속성(COMTNBBSMASTER)의 첨부 규칙. 원본은 게시판 속성관리(EgovBBSMasterService)로 읽는 값이다 */
    NoticeAttachPolicyVO selectAttachPolicy(@Param("bbsId") String bbsId);

    /**
     * 공지 수정 전에 게시물 행을 잠그고 지금 연결된 첨부 ID 를 읽는다 (SELECT … FOR UPDATE).
     * 담당자 두 명이 같은 공지에 동시에 파일을 올려도 순번(FILE_SN)이 겹치지 않는다. 없거나 삭제된 글이면 null
     */
    BoardVO selectNoticeForUpdate(@Param("bbsId") String bbsId, @Param("nttId") long nttId);
}
