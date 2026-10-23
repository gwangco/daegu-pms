package kr.go.daegu.pms.notice.service;

import egovframework.com.cop.bbs.service.BoardVO;
import kr.go.daegu.pms.cmm.service.AttachedFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 공지사항 서비스.
 *
 * 게시물 저장·조회는 전자정부 공통컴포넌트 게시판 서비스(EgovArticleService)에 맡기고,
 * 여기서는 공지 게시판 고정, 담당자 권한 검사, 없는 글 404 처리 같은 업무 규칙만 담당한다.
 * 첨부파일은 공통컴포넌트 첨부파일 묶음(ATCH_FILE_ID)으로 저장하고, 개수·크기는 게시판 속성으로 검사한다.
 */
public interface NoticeService {

    /** 공지사항 게시판 ID (COMTNBBSMASTER). 화면 값이 아니라 서버가 항상 이 값으로 고정한다 */
    String BBS_ID = "BBSMSTR_000000000001";

    /**
     * 공지 목록 (중요 공지 우선, 삭제글 제외).
     * 공통컴포넌트와 같은 형식으로 resultList(목록), resultCnt(전체 건수 문자열)를 담아 돌려준다.
     */
    Map<String, Object> selectNoticeList(BoardVO searchVO);

    /** 메인 화면용 최근 공지 */
    List<BoardVO> selectRecentNoticeList(int count);

    /** 공지 상세 (없거나 삭제된 글이면 NotFoundException) */
    BoardVO selectNotice(long nttId);

    /** 공지에 첨부된 파일 목록 (첨부가 없으면 빈 목록) */
    List<AttachedFile> selectNoticeFiles(BoardVO notice);

    /** 공지 게시판의 첨부 규칙 (게시판 속성) */
    NoticeAttachPolicyVO selectAttachPolicy();

    /** 조회수 1 증가 (select* 는 읽기 전용 트랜잭션이므로 update* 로 분리) */
    void updateNoticeViewCount(long nttId);

    /**
     * 공지 등록 (담당자/관리자). 발급된 게시물 ID 반환.
     * 첨부파일은 게시판 속성의 개수·크기 안에서만 받고, 하나라도 어긋나면 글도 파일도 저장하지 않는다.
     */
    long insertNotice(NoticeVO notice, List<MultipartFile> files);

    /**
     * 공지 수정 (담당자/관리자). 제목·내용·중요여부와 첨부(삭제·추가)만 바꿀 수 있다.
     *
     * @param deleteFileSns 지울 첨부 순번. 이 공지에 실제로 있는 순번만 지운다
     */
    void updateNotice(NoticeVO notice, List<MultipartFile> files, List<Integer> deleteFileSns);

    /** 공지 삭제 (담당자/관리자). 이력 보존을 위해 논리 삭제 (첨부파일도 남지만 내려받을 수 없게 된다) */
    void deleteNotice(long nttId);
}
