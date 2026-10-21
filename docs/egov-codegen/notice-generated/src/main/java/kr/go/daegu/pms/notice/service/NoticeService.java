package kr.go.daegu.pms.notice.service;

import java.util.List;

/**
 * @Class Name : NoticeService.java
 * @Description : NoticeService Class
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
public interface NoticeService {

	/**
	 * 글을 등록한다.
	 * @param vo - 등록할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	void insertNotice(NoticeVO vo) throws Exception;

	/**
	 * 글을 수정한다.
	 * @param vo - 수정할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	void updateNotice(NoticeVO vo) throws Exception;

	/**
	 * 글을 삭제한다.
	 * @param vo - 삭제할 정보가 담긴 NoticeVO
	 * @exception Exception
	 */
	void deleteNotice(NoticeVO vo) throws Exception;

	/**
	 * 글을 조회한다.
	 * @param vo - 조회할 정보가 담긴 NoticeVO
	 * @return 조회한 글
	 * @exception Exception
	 */
	NoticeVO selectNotice(NoticeVO vo) throws Exception;

	/**
	 * 글 목록을 조회한다.
	 * @param vo - 조회할 정보가 담긴 NoticeVO
	 * @return 글 목록
	 * @exception Exception
	 */
	List<?> selectNoticeList(NoticeVO vo) throws Exception;

	/**
	 * 글 총 갯수를 조회한다.
	 * @param vo - 조회할 정보가 담긴 NoticeVO
	 * @return 글 총 갯수
	 * @exception
	 */
	int selectNoticeListTotCnt(NoticeVO vo);

}
