package kr.go.daegu.pms.faq.service.impl;

import kr.go.daegu.pms.faq.service.FaqVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Optional;

/**
 * 자주 묻는 질문 Mapper (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정)
 */
@EgovMapper
public interface FaqMapper {

    List<FaqVO> selectFaqList(FaqVO searchVO);

    int selectFaqListTotCnt(FaqVO searchVO);

    Optional<FaqVO> selectFaq(@Param("faqId") String faqId);

    int insertFaq(FaqVO faq);

    /** 분류·질문·답변·표시 순서만 갱신 (작성자/작성일/사용여부는 바꿀 수 없음). 수정자·수정일시는 인터셉터가 채운다 */
    int updateFaq(FaqVO faq);

    /** 논리 삭제 (use_yn = 'N') */
    int deleteFaq(@Param("faqId") String faqId);
}
