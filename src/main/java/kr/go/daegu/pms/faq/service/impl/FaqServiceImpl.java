package kr.go.daegu.pms.faq.service.impl;

import egovframework.com.uss.olh.faq.service.EgovFaqService;
import egovframework.com.uss.olh.faq.service.FaqVO;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.faq.service.FaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 자주 묻는 질문 서비스 구현.
 *
 * 공통컴포넌트 FAQ 서비스(EgovFaqService)를 호출하기 전에 D-PMS 업무 규칙을 적용한다.
 *  1. 등록·수정·삭제는 담당자/관리자만 (컨트롤러 @PreAuthorize 와 별도로 서비스에서도 검사)
 *  2. 분류가 사용 중인 공통코드(DPM005)인지 서버에서 다시 확인 (화면 선택지를 거치지 않은 요청 대비)
 *  3. 없거나 이미 삭제된 글은 NotFoundException (404)
 * FAQ ID 채번, 작성자·수정자·일시 기록은 공통컴포넌트 서비스와 SQL(감사 값 #{audit.*})이 맡는다.
 */
@Slf4j
@Service("faqService")
@RequiredArgsConstructor
public class FaqServiceImpl extends EgovAbstractServiceImpl implements FaqService {

    /** 공통컴포넌트 FAQ 서비스 */
    private final EgovFaqService egovFaqService;
    private final CommonCodeService commonCodeService;
    private final AppMessages messages;

    @Override
    public List<FaqVO> selectFaqList(FaqVO searchVO) {
        return egovFaqService.selectFaqList(searchVO);
    }

    @Override
    public int selectFaqListTotCnt(FaqVO searchVO) {
        return egovFaqService.selectFaqListCnt(searchVO);
    }

    @Override
    public FaqVO selectFaq(String faqId) {
        FaqVO key = new FaqVO();
        key.setFaqId(faqId);
        FaqVO faq = egovFaqService.selectFaqDetailNoCount(key);
        if (faq == null) {
            throw messages.notFound("faq.notFound");
        }
        return faq;
    }

    @Override
    public String insertFaq(FaqVO faq) {
        requireStaff();
        requireValidCategory(faq.getFaqClCode());
        egovFaqService.insertFaq(faq);
        log.info("[FAQ 등록] faqId={}, by={}", faq.getFaqId(), SecurityUtil.getCurrentUserId());
        return faq.getFaqId();
    }

    @Override
    public void updateFaq(FaqVO faq) {
        requireStaff();
        requireValidCategory(faq.getFaqClCode());
        if (egovFaqService.updateFaq(faq) == 0) {
            throw messages.notFound("faq.notFound");
        }
        log.info("[FAQ 수정] faqId={}, by={}", faq.getFaqId(), SecurityUtil.getCurrentUserId());
    }

    @Override
    public void deleteFaq(String faqId) {
        requireStaff();
        FaqVO key = new FaqVO();
        key.setFaqId(faqId);
        if (egovFaqService.deleteFaq(key) == 0) {
            throw messages.notFound("faq.delete.notFound");
        }
        log.info("[FAQ 삭제] faqId={}, by={}", faqId, SecurityUtil.getCurrentUserId());
    }

    private void requireValidCategory(String faqClCode) {
        if (!commonCodeService.isValidCode(CommonCodeService.FAQ_CATEGORY, faqClCode)) {
            throw messages.business("faq.category.invalid");
        }
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("faq.write.denied");
        }
    }
}
