package kr.go.daegu.pms.faq.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import kr.go.daegu.pms.faq.service.FaqVO;

/**
* @Class Name : FaqMapper.java
* @Description : Faq에 관한 데이터처리 매퍼 클래스
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
@EgovMapper("faqMapper")
public interface FaqMapper {

	/**
	 * 글을 등록한다.
	 * @param vo - 등록할 정보가 담긴 FaqVO
	 * @return 등록 결과
	 * @exception Exception
	 */
	void insertFaq(FaqVO vo) throws Exception;

	/**
	 * 글을 수정한다.
	 * @param vo - 수정할 정보가 담긴 FaqVO
	 * @return void형
	 * @exception Exception
	 */
	void updateFaq(FaqVO vo) throws Exception;

	/**
	 * 글을 삭제한다.
	 * @param vo - 삭제할 정보가 담긴 FaqVO
	 * @return void형
	 * @exception Exception
	 */
	void deleteFaq(FaqVO vo) throws Exception;

	/**
	 * 글을 조회한다.
	 * @param vo - 조회할 정보가 담긴 FaqVO
	 * @return 조회한 글
	 * @exception Exception
	 */
	FaqVO selectFaq(FaqVO vo) throws Exception;

	/**
	 * 글 목록을 조회한다.
	 * @param vo - 조회할 정보가 담긴 VO
	 * @return 글 목록
	 * @exception Exception
	 */
	List<?> selectFaqList(FaqVO vo) throws Exception;

	/**
	 * 글 총 갯수를 조회한다.
	 * @param vo - 조회할 정보가 담긴 VO
	 * @return 글 총 갯수
	 * @exception
	 */
	int selectFaqListTotCnt(FaqVO vo);

}
