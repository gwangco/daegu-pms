package kr.go.daegu.pms.faq.service.impl;

import jakarta.annotation.Resource;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.faq.service.FaqService;
import kr.go.daegu.pms.faq.service.FaqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 자주 묻는 질문 서비스 구현 (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정).
 *
 * 생성기 원본 대비 보완 사항 (원본: docs/egov-codegen/faq-generated)
 *  1. 쓰기 작업에 권한 검사 추가 (원본은 누구나 등록/수정/삭제 가능)
 *  2. 조회 결과가 없으면 NotFoundException (원본은 null 반환 → NPE)
 *  3. 분류가 사용 중인 공통코드인지 서버에서 다시 확인 (원본은 아무 문자열이나 저장)
 *  4. 채번 Bean 을 이름으로 지정 (원본은 egovIdGnrService 하나만 있다고 가정)
 *  5. 수정·삭제는 대상 행이 없으면(이미 삭제됨) 404. 물리 삭제 → 논리 삭제
 * 작성자·작성일·수정자·수정일시는 SQL 에서 감사 값(#{audit.*})으로만 넣는다.
 */
@Slf4j
@Service("faqService")
@RequiredArgsConstructor
public class FaqServiceImpl extends EgovAbstractServiceImpl implements FaqService {

    private final FaqMapper faqMapper;
    private final CommonCodeService commonCodeService;
    private final AppMessages messages;

    /** FAQ ID 채번 (EgovConfigIdGeneration 의 faqIdGnrService). 같은 타입 Bean 이 여러 개라 이름으로 주입 */
    @Resource(name = "faqIdGnrService")
    private EgovIdGnrService faqIdGnrService;

    @Override
    public List<FaqVO> selectFaqList(FaqVO searchVO) {
        return faqMapper.selectFaqList(searchVO);
    }

    @Override
    public int selectFaqListTotCnt(FaqVO searchVO) {
        return faqMapper.selectFaqListTotCnt(searchVO);
    }

    @Override
    public FaqVO selectFaq(String faqId) {
        return faqMapper.selectFaq(faqId)
                .orElseThrow(() -> messages.notFound("faq.notFound"));
    }

    @Override
    public String insertFaq(FaqVO faq) {
        requireStaff();
        requireValidCategory(faq.getCategoryCode());
        try {
            faq.setFaqId(faqIdGnrService.getNextStringId());
        } catch (FdlException e) {
            throw new BaseRuntimeException("FAQ ID 채번에 실패했습니다.", e);
        }
        faqMapper.insertFaq(faq);
        log.info("[FAQ 등록] faqId={}, by={}", faq.getFaqId(), SecurityUtil.getCurrentUserId());
        return faq.getFaqId();
    }

    @Override
    public void updateFaq(FaqVO faq) {
        requireStaff();
        requireValidCategory(faq.getCategoryCode());
        if (faqMapper.updateFaq(faq) == 0) {
            throw messages.notFound("faq.notFound");
        }
        log.info("[FAQ 수정] faqId={}, by={}", faq.getFaqId(), SecurityUtil.getCurrentUserId());
    }

    @Override
    public void deleteFaq(String faqId) {
        requireStaff();
        if (faqMapper.deleteFaq(faqId) == 0) {
            throw messages.notFound("faq.delete.notFound");
        }
        log.info("[FAQ 삭제] faqId={}, by={}", faqId, SecurityUtil.getCurrentUserId());
    }

    private void requireValidCategory(String categoryCode) {
        if (!commonCodeService.isValidCode(CommonCodeService.GROUP_FAQ_CATEGORY, categoryCode)) {
            throw messages.business("faq.category.invalid");
        }
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("faq.write.denied");
        }
    }
}
