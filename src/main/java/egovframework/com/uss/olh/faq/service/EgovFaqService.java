package egovframework.com.uss.olh.faq.service;

import java.util.List;

/**
 * FAQ 관리 서비스
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.10.22  gwangco         D-PMS 적용
 *                               - 체크 예외(throws Exception, FdlException) 제거: D-PMS 는 서비스 밖으로 체크 예외를 던지지 않는다
 *                               - selectFaqDetail(조회수 증가) 제거: select* 는 읽기 전용 트랜잭션이라 UPDATE 를 함께 할 수 없고,
 *                                 D-PMS FAQ 는 목록에서 답변을 펼쳐 보여 주므로 상세 화면이 없다
 *                               - 단건 조회는 없으면 null, 수정·삭제는 처리 건수를 돌려줌 (없거나 이미 삭제된 글이면 0)
 *
 * </pre>
 */
public interface EgovFaqService {

	List<FaqVO> selectFaqList(FaqVO searchVO);

	int selectFaqListCnt(FaqVO searchVO);

	FaqVO selectFaqDetailNoCount(FaqVO searchVO);

	void insertFaq(FaqVO faqVO);

	int updateFaq(FaqVO faqVO);

	int deleteFaq(FaqVO faqVO);
}
