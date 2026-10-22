package kr.go.daegu.pms.faq.web;

import jakarta.validation.Valid;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import kr.go.daegu.pms.faq.service.FaqService;
import kr.go.daegu.pms.faq.service.FaqVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 자주 묻는 질문 컨트롤러 (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정).
 *
 * 생성기 원본 대비:
 *  - URL: /faq/faqList.do, 등록·수정 화면을 POST 로 여는 방식 → /faqs, /faqs/new, /faqs/{faqId}/edit (화면 조회는 GET)
 *  - 등록/수정/삭제에 @PreAuthorize 권한 추가 (서비스에서도 한 번 더 검사)
 *  - 수정·삭제 대상은 URL 의 ID 로만 결정 (원본은 폼의 hidden faqId 값을 그대로 사용)
 *  - 필드 주입(@Resource) → 생성자 주입, throws Exception 과 쓰지 않는 SessionStatus 제거
 *  - 페이징 준비 코드는 PagingHelper(eGov PaginationInfo + EgovPropertyService)로 공통화
 */
@Controller
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;
    private final CommonCodeService commonCodeService;
    private final PagingHelper pagingHelper;
    private final AppMessages messages;

    /** 분류 탭과 등록 화면 선택지 (사용 중인 공통코드만) */
    @ModelAttribute("categories")
    public List<CommonCodeVO> categories() {
        return commonCodeService.selectCodeList(CommonCodeService.GROUP_FAQ_CATEGORY);
    }

    @GetMapping
    public String selectFaqList(@ModelAttribute("searchVO") FaqVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("resultList", faqService.selectFaqList(searchVO));
        paginationInfo.setTotalRecordCount(faqService.selectFaqListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        return "faq/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addFaqView(Model model) {
        FaqVO faq = new FaqVO();
        faq.setSortOrder(1);
        model.addAttribute("faq", faq);
        return "faq/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addFaq(@Valid @ModelAttribute("faq") FaqVO faq, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                String faqId = faqService.insertFaq(faq);
                return redirectToSaved(faqId, faq.getCategoryCode(), messages.text("faq.created"), redirectAttributes);
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());   // 입력값을 유지한 채 폼에 안내
            }
        }
        return "faq/form";
    }

    @GetMapping("/{faqId}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateFaqView(@PathVariable("faqId") String faqId, Model model) {
        model.addAttribute("faq", faqService.selectFaq(faqId));
        return "faq/form";
    }

    @PostMapping("/{faqId}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateFaq(@PathVariable("faqId") String faqId,
                            @Valid @ModelAttribute("faq") FaqVO faq, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        faq.setFaqId(faqId);   // 수정 대상은 URL 의 ID 로만 결정
        if (!bindingResult.hasErrors()) {
            try {
                faqService.updateFaq(faq);
                return redirectToSaved(faqId, faq.getCategoryCode(), messages.text("faq.updated"), redirectAttributes);
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        return "faq/form";
    }

    @PostMapping("/{faqId}/delete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String deleteFaq(@PathVariable("faqId") String faqId, RedirectAttributes redirectAttributes) {
        faqService.deleteFaq(faqId);
        redirectAttributes.addFlashAttribute("successMessage", messages.text("faq.deleted"));
        return "redirect:/faqs";
    }

    /** 저장한 FAQ 의 분류 목록으로 돌아가 해당 질문을 펼친 상태로 보여 준다 */
    private String redirectToSaved(String faqId, String categoryCode, String successMessage,
                                   RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("categoryCode", categoryCode);
        redirectAttributes.addFlashAttribute("openFaqId", faqId);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/faqs#faq-" + faqId;
    }
}
