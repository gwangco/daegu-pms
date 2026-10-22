package egovframework.com.cop.bbs.service;

import java.util.Map;

/**
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일			수정자		수정내용
 *  -------			--------	---------------------------
 *   2024.10.29		inganyoyo	Transaction 처리 오류 수정(Article)
 *   2026.10.22		gwangco		D-PMS 적용: 공지 게시판에 쓰는 기능만 남김 (답글·방명록·블로그·첨부 업로드 제외)
 *   				        	조회수를 함께 올리던 selectArticleDetail 제거 → selectArticleDetailNoCount + increaseInqireCo 로 나눠 호출
 *   				        	(D-PMS 트랜잭션 규칙에서 select* 는 읽기 전용이라 같은 메서드에서 UPDATE 할 수 없음)
 *   				        	insertArticleAndFiles → insertArticle, 수정·삭제는 처리 건수를 돌려줌 (없는 글이면 0)
 * </pre>
 */

public interface EgovArticleService {

	Map<String, Object> selectArticleList(BoardVO boardVO);

	// 조회수를 올리지 않는 단건 조회 — 권한 확인·수정·삭제용
	BoardVO selectArticleDetailNoCount(BoardVO boardVO);

	void increaseInqireCo(BoardVO boardVO);

	void insertArticle(Board board);

	int updateArticle(Board board);

	int deleteArticle(Board board);
}
