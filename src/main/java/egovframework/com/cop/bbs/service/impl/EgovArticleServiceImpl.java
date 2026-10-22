package egovframework.com.cop.bbs.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;

import egovframework.com.cop.bbs.service.Board;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovArticleService;
import jakarta.annotation.Resource;

/**
 * 게시물 관리를 위한 ServiceImpl 클래스
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2024.10.29  inganyoyo     Transaction 처리 오류 수정(Article)
 *   2025.06.03  이백행          PMD로 소프트웨어 보안약점 진단하고 제거하기-AvoidReassigningParameters(매개변수 재할당 방지), LocalVariableNamingConventions(지역 변수 명명 규칙)
 *   2026.10.22  gwangco         D-PMS 적용: 공지 게시판 기능만 남김 (EgovArticleService 개정이력 참고)
 *                               첨부 업로드(EgovFileMngUtil) 의존 제거, 조회수는 SQL 에서 1 증가 (동시 조회 시 누락 방지)
 *
 * </pre>
 */

@Service("EgovArticleService")
public class EgovArticleServiceImpl extends EgovAbstractServiceImpl implements EgovArticleService {

	@Resource(name = "EgovArticleDAO")
	private EgovArticleDAO egovArticleDao;

	@Resource(name = "egovNttIdGnrService")
	private EgovIdGnrService nttIdgenService;

	@Override
	public Map<String, Object> selectArticleList(BoardVO boardVO) {
		List<BoardVO> list = egovArticleDao.selectArticleList(boardVO);
		int cnt = egovArticleDao.selectArticleListCnt(boardVO);

		Map<String, Object> map = new HashMap<>();

		map.put("resultList", list);
		map.put("resultCnt", Integer.toString(cnt));

		return map;
	}

	@Override
	public BoardVO selectArticleDetailNoCount(BoardVO boardVO) {
		return egovArticleDao.selectArticleDetail(boardVO);
	}

	@Override
	public void increaseInqireCo(BoardVO boardVO) {
		// 원본: 최대 조회수를 읽어 +1 한 값을 다시 저장 → 동시에 열면 증가분이 사라짐. SQL 한 번으로 1 증가
		egovArticleDao.updateInqireCo(boardVO);
	}

	@Override
	public void insertArticle(Board board) {
		// 답글이 아닌 새 글: 부모글 0, 답글 위치 0 (정렬 순서는 SQL 에서 NTT_ID 로 지정)
		board.setParnts("0");
		board.setReplyLc("0");
		board.setReplyAt("N");
		try {
			board.setNttId((long) nttIdgenService.getNextIntegerId());
		} catch (FdlException e) {
			throw new BaseRuntimeException(e);
		}
		egovArticleDao.insertArticle(board);
	}

	@Override
	public int updateArticle(Board board) {
		return egovArticleDao.updateArticle(board);
	}

	@Override
	public int deleteArticle(Board board) {
		return egovArticleDao.deleteArticle(board);
	}
}
