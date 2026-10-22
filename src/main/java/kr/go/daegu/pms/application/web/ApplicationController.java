package kr.go.daegu.pms.application.web;

import jakarta.validation.Valid;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AnnouncementService announcementService;
    private final EvaluationService evaluationService;
    private final PagingHelper pagingHelper;
    private final AppMessages messages;

    // 기업: 본인 신청 내역 / 평가위원: 배정 과제 / 담당자·관리자: 전체 (범위는 서비스에서 강제)
    @GetMapping
    public String list(@ModelAttribute("searchVO") ApplicationSearchVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("applications", applicationService.selectApplicationList(searchVO));
        paginationInfo.setTotalRecordCount(applicationService.selectApplicationListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("statuses", ApplicationStatus.values());
        return "application/list";
    }

    // 사업 신청서 작성 화면
    @GetMapping("/apply")
    @PreAuthorize("hasRole('COMPANY')")
    public String applyForm(@RequestParam("announcementId") Long announcementId, Model model) {
        AnnouncementVO announcement = announcementService.selectAnnouncement(announcementId);
        model.addAttribute("announcement", announcement);
        model.addAttribute("app", ApplicationVO.builder().announcementId(announcementId).build());
        return "application/form";
    }

    // 사업 신청서 제출 (사업계획서 파일 첨부)
    @PostMapping("/apply")
    @PreAuthorize("hasRole('COMPANY')")
    public String submit(@Valid @ModelAttribute("app") ApplicationVO application,
                         BindingResult bindingResult,
                         @RequestParam(value = "planFile", required = false) MultipartFile planFile,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long id = applicationService.insertApplication(application, planFile);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("application.submitted"));
                return "redirect:/applications/" + id;
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("announcement", announcementService.selectAnnouncement(application.getAnnouncementId()));
        return "application/form";
    }

    // 신청서 상세 조회 (열람 권한은 서비스에서 검사)
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        ApplicationVO application = applicationService.selectApplication(id);
        model.addAttribute("app", application);

        if (SecurityUtil.isStaff()) {
            ApplicationStatus current = ApplicationStatus.fromCode(application.getStatus()).orElse(null);
            model.addAttribute("nextStatuses", current == null ? List.of() : current.manualNextStatuses());
            model.addAttribute("evaluatorAssignable", current != null && current.isEvaluatorAssignable());
            model.addAttribute("evaluations", evaluationService.selectEvaluationListByApplication(id));
            model.addAttribute("assignableEvaluators", evaluationService.selectAssignableEvaluatorList(id));
        }
        return "application/detail";
    }

    // 담당자: 심사 상태 변경 (서류적격, 선정, 탈락)
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            applicationService.updateApplicationStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("application.status.updated"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    // 담당자: 평가위원 배정
    @PostMapping("/{id}/evaluators")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String assignEvaluator(@PathVariable("id") Long id,
                                  @RequestParam("evaluatorUserId") String evaluatorUserId,
                                  RedirectAttributes redirectAttributes) {
        try {
            evaluationService.assignEvaluator(id, evaluatorUserId);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("evaluation.assigned"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }
}
