package kr.go.daegu.pms.notice.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import jakarta.annotation.Resource;

/**
 * @Class Name : NoticeServiceImpl.java
 * @Description : Notice Business Implement Class
 * @Modification Information
 * @
 * @ 수정일 수정자 수정내용
 * @ --------- --------- -------------------------------
 * @ 2026-10-21 author 최초생성
 *
 * @author author
 * @since 2026-10-21
 * @version 1.0.0
 * @see
 *
 * Copyright (C) All right reserved.
 */
@Service("noticeService")
public class NoticeServiceImpl extends EgovAbstractServiceImpl implements NoticeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoticeServiceImpl.class);

	/** NoticeMapper */
	@Resource(name = "noticeMapper")
	private NoticeMapper noticeMapper;

	/** ID Generation */
	@Resource(name = "egovIdGnrService")
	private EgovIdGnrService egovIdGnrService;

	/**
	 * 글을 등록한다.
	 * @param vo - 등록할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	@Override
	public void insertNotice(NoticeVO vo) throws Exception {
		LOGGER.debug(vo.toString());

		/** ID Generation Service */
		String id = egovIdGnrService.getNextStringId();
		vo.setNoticeId(id);
		LOGGER.debug(vo.toString());

		noticeMapper.insertNotice(vo);
	}

	/**
	 * 글을 수정한다.
	 * @param vo - 수정할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	@Override
	public void updateNotice(NoticeVO vo) throws Exception {
		noticeMapper.updateNotice(vo);
	}

	/**
	 * 글을 삭제한다.
	 * @param vo - 삭제할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	@Override
	public void deleteNotice(NoticeVO vo) throws Exception {
		noticeMapper.deleteNotice(vo);
	}

	/**
	 * 글을 조회한다.
	 * @param vo - 조회할 정보가 담긴 NoticeVO
	 * @return 조회한 글
	 * @exception Exception
	 */
	@Override
	public NoticeVO selectNotice(NoticeVO vo) throws Exception {
		NoticeVO resultVO = noticeMapper.selectNotice(vo);
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
	public List<?> selectNoticeList(NoticeVO vo) throws Exception {
		return noticeMapper.selectNoticeList(vo);
	}

	/**
	 * 글 총 갯수를 조회한다.
	 * @param vo - 조회할 정보가 담긴 VO
	 * @return 글 총 갯수
	 * @exception
	 */
	@Override
	public int selectNoticeListTotCnt(NoticeVO vo) {
		return noticeMapper.selectNoticeListTotCnt(vo);
	}

}
