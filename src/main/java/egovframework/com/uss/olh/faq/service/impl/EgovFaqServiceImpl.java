package egovframework.com.uss.olh.faq.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;

import egovframework.com.uss.olh.faq.service.EgovFaqService;
import egovframework.com.uss.olh.faq.service.FaqVO;
import jakarta.annotation.Resource;

/**
 * FAQ 관리 서비스 구현
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.10.22  gwangco         D-PMS 적용 (EgovFaqService 개정이력 참고). 채번 실패는 BaseRuntimeException 으로 감싼다
 *
 * </pre>
 */
@Service("EgovFaqService")
public class EgovFaqServiceImpl extends EgovAbstractServiceImpl implements EgovFaqService {

	@Resource(name = "EgovFaqDAO")
	private EgovFaqDAO egovFaqDao;

	/** ID Generation */
	@Resource(name = "egovFaqManageIdGnrService")
	private EgovIdGnrService idgenService;

	@Override
	public List<FaqVO> selectFaqList(FaqVO searchVO) {
		return egovFaqDao.selectFaqList(searchVO);
	}

	@Override
	public int selectFaqListCnt(FaqVO searchVO) {
		return egovFaqDao.selectFaqListCnt(searchVO);
	}

	@Override
	public FaqVO selectFaqDetailNoCount(FaqVO searchVO) {
		return egovFaqDao.selectFaqDetail(searchVO);
	}

	@Override
	public void insertFaq(FaqVO faqVO) {
		try {
			faqVO.setFaqId(idgenService.getNextStringId());
		} catch (FdlException e) {
			throw new BaseRuntimeException(e);
		}
		egovFaqDao.insertFaq(faqVO);
	}

	@Override
	public int updateFaq(FaqVO faqVO) {
		return egovFaqDao.updateFaq(faqVO);
	}

	@Override
	public int deleteFaq(FaqVO faqVO) {
		return egovFaqDao.deleteFaq(faqVO);
	}
}
