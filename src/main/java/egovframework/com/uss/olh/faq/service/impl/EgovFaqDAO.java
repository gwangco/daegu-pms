package egovframework.com.uss.olh.faq.service.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import egovframework.com.cmm.service.impl.EgovComAbstractDAO;
import egovframework.com.uss.olh.faq.service.FaqVO;

/**
 * FAQ 데이터 접근 클래스
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.10.22  gwangco         D-PMS 적용: 조회수 증가 SQL 제거, 삭제는 물리 삭제 대신 논리 삭제(USE_AT = 'N'),
 *                               수정·삭제는 처리 건수를 돌려줌
 *
 * </pre>
 */
@Repository("EgovFaqDAO")
public class EgovFaqDAO extends EgovComAbstractDAO {

	public List<FaqVO> selectFaqList(FaqVO searchVO) {
		return selectList("FaqManage.selectFaqList", searchVO);
	}

	public int selectFaqListCnt(FaqVO searchVO) {
		return (Integer) selectOne("FaqManage.selectFaqListCnt", searchVO);
	}

	public FaqVO selectFaqDetail(FaqVO searchVO) {
		return (FaqVO) selectOne("FaqManage.selectFaqDetail", searchVO);
	}

	public void insertFaq(FaqVO faqVO) {
		insert("FaqManage.insertFaq", faqVO);
	}

	public int updateFaq(FaqVO faqVO) {
		return update("FaqManage.updateFaq", faqVO);
	}

	public int deleteFaq(FaqVO faqVO) {
		return update("FaqManage.deleteFaq", faqVO);
	}
}
