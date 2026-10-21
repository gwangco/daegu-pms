package kr.go.daegu.pms.announcement.web;

import jakarta.validation.Valid;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.announcement.service.AnnouncementSearchVO;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final CommonCodeService commonCodeService;
    private final PagingHelper pagingHelper;

    @GetMapping
    public String list(@ModelAttribute("searchVO") AnnouncementSearchVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("announcements", announcementService.selectAnnouncementList(searchVO));
        paginationInfo.setTotalRecordCount(announcementService.selectAnnouncementListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("fieldCodes", commonCodeService.selectCodeList(CommonCodeService.GROUP_FIELD));
        return "announcement/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        // 존재 확인(없으면 404) → 조회수 증가 → 화면 표시
        AnnouncementVO announcement = announcementService.selectAnnouncement(id);
        announcementService.updateViewCount(id);
        announcement.setViewCount(announcement.getViewCount() + 1);
        model.addAttribute("announcement", announcement);
        return "announcement/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("announcement", new AnnouncementVO());
        model.addAttribute("fieldCodes", commonCodeService.selectCodeList(CommonCodeService.GROUP_FIELD));
        return "announcement/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String create(@Valid @ModelAttribute("announcement") AnnouncementVO announcement,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long id = announcementService.insertAnnouncement(announcement);
                redirectAttributes.addFlashAttribute("successMessage", "공고가 등록되었습니다.");
                return "redirect:/announcements/" + id;
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("fieldCodes", commonCodeService.selectCodeList(CommonCodeService.GROUP_FIELD));
        return "announcement/form";
    }
}
