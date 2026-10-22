package kr.go.daegu.pms.faq.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kr.go.daegu.pms.faq.service.FaqService;
import kr.go.daegu.pms.faq.service.FaqVO;
import jakarta.annotation.Resource;

/**
 * @Class Name : FaqServiceImpl.java
 * @Description : Faq Business Implement Class
 * @Modification Information
 * @
 * @ 수정일 수정자 수정내용
 * @ --------- --------- -------------------------------
 * @ 2026-10-22 author 최초생성
 *
 * @author author
 * @since 2026-10-22
 * @version 1.0.0
 * @see
 *
 * Copyright (C) All right reserved.
 */
@Service("faqService")
public class FaqServiceImpl extends EgovAbstractServiceImpl implements FaqService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FaqServiceImpl.class);

	/** FaqMapper */
	@Resource(name = "faqMapper")
	private FaqMapper faqMapper;

	/** ID Generation */
	@Resource(name = "egovIdGnrService")
	private EgovIdGnrService egovIdGnrService;

	/**
	 * 글을 등록한다.
	 * @param vo - 등록할 정보가 담긴 FaqVO
	 * @exception Exception
	 */
	@Override
	public void insertFaq(FaqVO vo) throws Exception {
		LOGGER.debug(vo.toString());

		/** ID Generation Service */
		String id = egovIdGnrService.getNextStringId();
		vo.setFaqId(id);
		LOGGER.debug(vo.toString());

		faqMapper.insertFaq(vo);
	}

	/**
	 * 글을 수정한다.
	 * @param vo - 수정할 정보가 담긴 FaqVO
	 * @exception Exception
	 */
	@Override
	public void updateFaq(FaqVO vo) throws Exception {
		faqMapper.updateFaq(vo);
	}

	/**
	 * 글을 삭제한다.
	 * @param vo - 삭제할 정보가 담긴 FaqVO
	 * @exception Exception
	 */
	@Override
	public void deleteFaq(FaqVO vo) throws Exception {
		faqMapper.deleteFaq(vo);
	}

	/**
	 * 글을 조회한다.
	 * @param vo - 조회할 정보가 담긴 FaqVO
	 * @return 조회한 글
	 * @exception Exception
	 */
	@Override
	public FaqVO selectFaq(FaqVO vo) throws Exception {
		FaqVO resultVO = faqMapper.selectFaq(vo);
		// if (resultVO == null)
		//  	throw processException("info.nodata.msg");
		return resultVO;
	}

	/**
	 * 글 목록을 조회한다.
	 * @param vo - 조회할 정보가 담긴 VO
	 * @return 글 목록
	 * @exception Exception
	 */
	@Override
	public List<?> selectFaqList(FaqVO vo) throws Exception {
		return faqMapper.selectFaqList(vo);
	}

	/**
	 * 글 총 갯수를 조회한다.
	 * @param vo - 조회할 정보가 담긴 VO
	 * @return 글 총 갯수
	 * @exception
	 */
	@Override
	public int selectFaqListTotCnt(FaqVO vo) {
		return faqMapper.selectFaqListTotCnt(vo);
	}

}
