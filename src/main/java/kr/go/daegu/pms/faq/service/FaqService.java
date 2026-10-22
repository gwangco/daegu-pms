package kr.go.daegu.pms.faq.service;

import egovframework.com.uss.olh.faq.service.FaqVO;

import java.util.List;

/**
 * 자주 묻는 질문 서비스.
 *
 * FAQ 저장·조회는 전자정부 공통컴포넌트 FAQ 서비스(EgovFaqService, COMTNFAQINFO)에 맡기고,
 * 여기서는 담당자 권한 검사, 분류 코드 검증, 없는 글 404 처리 같은 업무 규칙만 담당한다.
 */
public interface FaqService {

    /** FAQ 목록 (분류 순서 → 표시 순서, 삭제글 제외) */
    List<FaqVO> selectFaqList(FaqVO searchVO);

    int selectFaqListTotCnt(FaqVO searchVO);

    /** FAQ 한 건 (없거나 삭제된 글이면 NotFoundException) */
    FaqVO selectFaq(String faqId);

    /** 등록 (담당자/관리자). 발급된 FAQ ID 반환 */
    String insertFaq(FaqVO faq);

    /** 수정 (담당자/관리자). 분류·질문·답변·표시 순서만 변경 가능 */
    void updateFaq(FaqVO faq);

    /** 삭제 (담당자/관리자). 이력 보존을 위해 논리 삭제 */
    void deleteFaq(String faqId);
}
