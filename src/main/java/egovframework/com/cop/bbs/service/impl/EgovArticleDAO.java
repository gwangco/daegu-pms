package egovframework.com.cop.bbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import egovframework.com.cmm.service.impl.EgovComAbstractDAO;
import egovframework.com.cop.bbs.service.Board;
import egovframework.com.cop.bbs.service.BoardVO;

/**
 * 게시물 데이터 접근 클래스
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.10.22  gwangco         D-PMS 적용: 공지 게시판에 쓰는 SQL 만 남김, 수정·삭제는 처리 건수를 돌려줌
 *
 * </pre>
 */
@Repository("EgovArticleDAO")
public class EgovArticleDAO extends EgovComAbstractDAO {

	public List<BoardVO> selectArticleList(BoardVO boardVO) {
		return selectList("BBSArticle.selectArticleList", boardVO);
	}

	public int selectArticleListCnt(BoardVO boardVO) {
		return (Integer)selectOne("BBSArticle.selectArticleListCnt", boardVO);
	}

	public void updateInqireCo(BoardVO boardVO) {
		update("BBSArticle.updateInqireCo", boardVO);
	}

	public BoardVO selectArticleDetail(BoardVO boardVO) {
		return (BoardVO) selectOne("BBSArticle.selectArticleDetail", boardVO);
	}

	public void insertArticle(Board board) {
		insert("BBSArticle.insertArticle", board);
	}

	public int updateArticle(Board board) {
		return update("BBSArticle.updateArticle", board);
	}

	public int deleteArticle(Board board) {
		return update("BBSArticle.deleteArticle", board);
	}
}
