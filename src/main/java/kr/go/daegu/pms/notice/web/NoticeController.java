package kr.go.daegu.pms.notice.web;

import kr.go.daegu.pms.cmm.message.AppMessages;

import jakarta.validation.Valid;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
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

/**
 * 공지사항 컨트롤러 (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정).
 *
 * 생성기 원본 대비:
 *  - URL: /notice/noticeList.do 등 *.do + POST 화면 이동 → 프로젝트 규칙(/notices, /notices/{id}, GET 조회)으로 통일
 *  - 등록/수정/삭제에 @PreAuthorize 권한 추가 (서비스에서도 한 번 더 검사)
 *  - 필드 주입(@Resource) → 생성자 주입
 *  - 페이징 준비 코드는 PagingHelper(eGov PaginationInfo + EgovPropertyService)로 공통화
 */
@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final AppMessages messages;

    private final NoticeService noticeService;
    private final PagingHelper pagingHelper;

    @GetMapping
    public String selectNoticeList(@ModelAttribute("searchVO") NoticeVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("resultList", noticeService.selectNoticeList(searchVO));
        paginationInfo.setTotalRecordCount(noticeService.selectNoticeListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        return "notice/list";
    }

    @GetMapping("/{noticeId}")
    public String selectNotice(@PathVariable("noticeId") String noticeId, Model model) {
        NoticeVO notice = noticeService.selectNotice(noticeId);   // 없으면 404
        noticeService.updateNoticeViewCount(noticeId);
        notice.setViewCount(notice.getViewCount() + 1);
        model.addAttribute("notice", notice);
        return "notice/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addNoticeView(Model model) {
        NoticeVO notice = new NoticeVO();
        notice.setImportantYn(NoticeVO.NO);
        model.addAttribute("notice", notice);
        return "notice/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addNotice(@Valid @ModelAttribute("notice") NoticeVO notice, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "notice/form";
        }
        String noticeId = noticeService.insertNotice(notice);
        redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.created"));
        return "redirect:/notices/" + noticeId;
    }

    @GetMapping("/{noticeId}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateNoticeView(@PathVariable("noticeId") String noticeId, Model model) {
        model.addAttribute("notice", noticeService.selectNotice(noticeId));
        return "notice/form";
    }

    @PostMapping("/{noticeId}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateNotice(@PathVariable("noticeId") String noticeId,
                               @Valid @ModelAttribute("notice") NoticeVO notice, BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        notice.setNoticeId(noticeId);   // 수정 대상은 URL 의 ID 로만 결정 (폼 값으로 다른 글을 수정하는 것 방지)
        if (bindingResult.hasErrors()) {
            return "notice/form";
        }
        noticeService.updateNotice(notice);
        redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.updated"));
        return "redirect:/notices/" + noticeId;
    }

    @PostMapping("/{noticeId}/delete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String deleteNotice(@PathVariable("noticeId") String noticeId, RedirectAttributes redirectAttributes) {
        noticeService.deleteNotice(noticeId);
        redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.deleted"));
        return "redirect:/notices";
    }
}
