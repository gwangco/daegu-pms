package kr.go.daegu.pms.notice.service.impl;

import egovframework.com.cop.bbs.service.Board;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovArticleService;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 공지사항 서비스 구현.
 *
 * 공통컴포넌트 게시판(EgovArticleService)을 호출하기 전에 D-PMS 업무 규칙을 적용한다.
 *  1. 게시판 ID 는 항상 공지사항 게시판으로 고정 (검색 파라미터로 다른 게시판 글을 조회·수정하는 것 방지)
 *  2. 등록·수정·삭제는 담당자/관리자만 (컨트롤러 @PreAuthorize 와 별도로 서비스에서도 검사)
 *  3. 화면 값 중 제목·내용·중요여부만 옮겨 담는다 (작성자·조회수·비밀글 같은 컬럼은 서버가 정함)
 *  4. 없거나 이미 삭제된 글은 NotFoundException (404)
 */
@Slf4j
@Service("noticeService")
@RequiredArgsConstructor
public class NoticeServiceImpl extends EgovAbstractServiceImpl implements NoticeService {

    /** 공통컴포넌트 게시판 서비스 */
    private final EgovArticleService articleService;
    private final AppMessages messages;

    @Override
    public Map<String, Object> selectNoticeList(BoardVO searchVO) {
        searchVO.setBbsId(BBS_ID);
        return articleService.selectArticleList(searchVO);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BoardVO> selectRecentNoticeList(int count) {
        BoardVO searchVO = new BoardVO();
        searchVO.setBbsId(BBS_ID);
        searchVO.setFirstIndex(0);
        searchVO.setRecordCountPerPage(count);
        return (List<BoardVO>) articleService.selectArticleList(searchVO).get("resultList");
    }

    @Override
    public BoardVO selectNotice(long nttId) {
        BoardVO notice = articleService.selectArticleDetailNoCount(key(nttId));
        if (notice == null) {
            throw messages.notFound("notice.notFound");
        }
        return notice;
    }

    @Override
    public void updateNoticeViewCount(long nttId) {
        articleService.increaseInqireCo(key(nttId));
    }

    @Override
    public long insertNotice(NoticeVO notice) {
        requireStaff();
        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttSj(notice.getNttSj());
        board.setNttCn(notice.getNttCn());
        board.setNoticeAt(normalizeYn(notice.getNoticeAt()));
        articleService.insertArticle(board);
        log.info("[공지 등록] nttId={}, by={}", board.getNttId(), SecurityUtil.getCurrentUserId());
        return board.getNttId();
    }

    @Override
    public void updateNotice(NoticeVO notice) {
        requireStaff();
        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttId(notice.getNttId());
        board.setNttSj(notice.getNttSj());
        board.setNttCn(notice.getNttCn());
        board.setNoticeAt(normalizeYn(notice.getNoticeAt()));
        if (articleService.updateArticle(board) == 0) {
            throw messages.notFound("notice.notFound");
        }
        log.info("[공지 수정] nttId={}, by={}", notice.getNttId(), SecurityUtil.getCurrentUserId());
    }

    @Override
    public void deleteNotice(long nttId) {
        requireStaff();
        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttId(nttId);
        if (articleService.deleteArticle(board) == 0) {
            throw messages.notFound("notice.delete.notFound");
        }
        log.info("[공지 삭제] nttId={}, by={}", nttId, SecurityUtil.getCurrentUserId());
    }

    private static BoardVO key(long nttId) {
        BoardVO key = new BoardVO();
        key.setBbsId(BBS_ID);
        key.setNttId(nttId);
        return key;
    }

    private static String normalizeYn(String value) {
        return NoticeVO.YES.equals(value) ? NoticeVO.YES : NoticeVO.NO;
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("notice.write.denied");
        }
    }
}
